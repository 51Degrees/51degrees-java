/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package fiftyone.pipeline.core.flowelements;

import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.exceptions.PipelineDataException;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.core.typed.TypedKeyMap;
import fiftyone.pipeline.core.typed.TypedKeyMapBuilder;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static fiftyone.pipeline.util.Check.getNotNull;

/**
 * Default implementation of the {@link FlowData} interface. This class is
 * package private, and should only be created by the
 * {@link FlowDataFactoryDefault} class.
 * <p>
 * It takes no lock apart from the one on errors that the members of a
 * parallel group add while the group runs. Its Element Data is kept in a
 * linked list, which a parallel group's members add to with one compare and
 * swap each, and which readers follow with no lock. It is not safe to use
 * from several threads at once, and a caller that does so is responsible
 * for any locking.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#flow-data">Specification</a>
 */
class FlowDataDefault implements FlowData {

    private final Logger logger;

    private final PipelineInternal pipeline;

    private final List<FlowError> errors = new ArrayList<>();

    /**
     * True while a parallel group runs, when its members add errors under
     * the lock on {@link #errors}.
     */
    private volatile boolean groupRunning = false;

    private final TypedKeyMap data;

    private final Evidence evidence;

    private boolean stop = false;

    private final AtomicBoolean processed = new AtomicBoolean(false);

    FlowDataDefault(Logger logger, Pipeline pipeline, Evidence evidence) {
        this.logger = logger;
        this.pipeline = (PipelineInternal) pipeline;
        this.evidence = evidence;
        // With a parallel group, its members add Element Data at the same
        // time, each with one compare and swap. Otherwise a plain write is
        // enough.
        data = new TypedKeyMapBuilder(pipeline.isConcurrent()).build();
        logger.debug("FlowData '" + hashCode() + "' created.");

    }

    @Override
    public void process() {
        if (processed.getAndSet(true)) {
            throw new IllegalStateException("Flow data has already been processed");
        } else if (pipeline.isClosed()) {
            throw new IllegalStateException("Pipeline has been closed.");
        } else {
            pipeline.process(this);
        }
    }

    @Override
    public boolean isStopped() {
        return stop;
    }

    @Override
    public void stop() {
        stop = true;
    }

    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }

    /**
     * Mark the start and end of a parallel group, between which its members
     * add errors under a lock.
     * @param running true when the group starts, false when it has finished
     */
    void setGroupRunning(boolean running) {
        groupRunning = running;
    }

    @Override
    public Collection<FlowError> getErrors() {
        if (groupRunning) {
            synchronized (errors) {
                return new ArrayList<>(errors);
            }
        }
        return new ArrayList<>(errors);
    }

    @Override
    public void addError(FlowError error) {
        if (groupRunning) {
            synchronized (errors) {
                errors.add(error);
            }
        } else {
            errors.add(error);
        }

        if (logger.isErrorEnabled()) {
            String message = "Error occurred during processing";
            if (error.getFlowElement() != null) {
                message += " of " +
                    error.getFlowElement().getClass().getSimpleName() +
                    "-" + error.getFlowElement().hashCode();
            }
            logger.error(message);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void addError(Throwable e, FlowElement element) {
        addError(new FlowError.Default(e, element));
    }

    @Override
    public Evidence getEvidence() {
        return evidence;
    }

    @Override
    public <T> TryGetResult<T> tryGetEvidence(String key, Class<T> type) {
        TryGetResult<T> result = new TryGetResult<>();
        if (evidence.asKeyMap().containsKey(key)) {
            Object obj;
            try {
                obj = evidence.get(key);
                T value = type.cast(obj);
                result.setValue(value);
            } catch (ClassCastException e) {
                logger.debug("Evidence for the key '" + key + "' was found, " +
                    "but was the wrong type (should have been " +
                    type.getSimpleName() + ")",
                    e);
            }
        }
        return result;
    }


    @Override
    public List<String> getDataKeys() {
        return keysInPipelineOrder();
    }

    /**
     * The keys of the Element Data held, in Pipeline order, being the order
     * the Flow Elements were added to the Pipeline, with the members of a
     * parallel group in the group's place. The list holds the newest first,
     * so it is sorted here. Keys of no Flow Element come last, in the order
     * they were added.
     */
    private List<String> keysInPipelineOrder() {
        List<String> keys = data.getKeys();
        Collections.reverse(keys);
        final Map<String, Integer> rank = new HashMap<>();
        addRanks(pipeline.getFlowElements(), rank);
        keys.sort(Comparator.comparingInt(key -> rank.getOrDefault(
            key.toLowerCase(Locale.ROOT),
            Integer.MAX_VALUE)));
        return keys;
    }

    @SuppressWarnings("rawtypes")
    private static void addRanks(
        List<FlowElement> elements,
        Map<String, Integer> rank) {
        for (FlowElement element : elements) {
            if (element instanceof ParallelElements) {
                addRanks(((ParallelElements) element).getFlowElements(), rank);
            } else {
                rank.putIfAbsent(
                    element.getElementDataKey().toLowerCase(Locale.ROOT),
                    rank.size());
            }
        }
    }

    @Override
    public FlowData addEvidence(String key, Object value) {
        evidence.put(key, value);
        if (logger.isDebugEnabled()) {
            logger.debug("FlowData '" + hashCode() + "' set evidence " +
                "'" + key + "' to '" + value.toString() + "'.");
        }
        return this;
    }

    @Override
    public FlowData addEvidence(Map<String, ?> values) {
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            addEvidence(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public <T extends ElementData> T get(TypedKey<T> key) {
        if (processed.get() == false) {
            throw new IllegalStateException("This instance has not yet been processed.");
        }
        getNotNull(key, "Element data key cannot be null.");
        return data.get(key);
    }

    @Override
    public ElementData get(String key) {
        if (processed.get() == false) {
            throw new IllegalStateException("This instance has not yet been processed");
        }

        return data.get(new TypedKeyDefault<ElementData>(key, ElementData.class));
    }

    @Override
    public <T extends ElementData> T get(Class<T> type) {
        return data.get(type);
    }

    @Override
    public <T extends ElementData> T getFromElement(FlowElement<T, ?> element) {
        getNotNull(element, "Element cannot be null");
        return get(element.getTypedDataKey());
    }

    @Override
    public <T> T getAs(String key, Class<T> type) {

        T result;

        // Throw an error if the instance has not yet been processed.
        if (processed.get() == false) {
            String message = "Flow data has not yet been processed";
            logger.error(message);
            throw new PipelineDataException(message);
        }

        ElementPropertyMetaData property = pipeline.getMetaDataForProperty(key);

        try {
            result = type.cast(
                get(property.getElement().getElementDataKey())
                    .get(property.getName()));
        } catch (ClassCastException e) {
            String message = "Failed to cast property '" + key +
                "' to '" + type.getSimpleName() + "'. Expected property type is '" +
                property.getType().getSimpleName() + "'.";
            logger.error(message);
            throw e;
        }

        return result;
    }

    @Override
    public <T extends ElementData> TryGetResult<T> tryGetValue(TypedKey<T> key) {
        return data.tryGet(key);
    }

    @Override
    public String getAsString(String propertyName) {
        return getAs(propertyName, String.class);
    }

    @Override
    public Boolean getAsBool(String propertyName) {
        return getAs(propertyName, Boolean.class);
    }

    @Override
    public Integer getAsInt(String propertyName) {
        return getAs(propertyName, Integer.class);
    }

    @Override
    public Long getAsLong(String propertyName) {
        return getAs(propertyName, Long.class);
    }

    @Override
    public Float getAsFloat(String propertyName) {
        return getAs(propertyName, Float.class);
    }

    @Override
    public Double getAsDouble(String propertyName) {
        return getAs(propertyName, Double.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ElementData> T getOrAdd(
        String elementDataKey,
        FlowElement.DataFactory<T> dataFactory) {

        TypedKey<ElementData> typedKey = new TypedKeyDefault<>(
            elementDataKey,
            ElementData.class);
        if (data.containsKey(elementDataKey) == false) {
            data.asStringKeyMap().putIfAbsent(
                elementDataKey,
                dataFactory.create(this));
        }
        return (T) data.get(typedKey);
    }

    @Override
    public <T extends ElementData> T getOrAdd(
        TypedKey<T> key,
        FlowElement.DataFactory<T> dataFactory) {
        T result;

        if (data.containsKey(key) == false) {
            // One step, so that members of a parallel group adding at the
            // same time never replace each other's Element Data.
            data.asStringKeyMap().putIfAbsent(
                key.getName(),
                dataFactory.create(this));
        }
        try {
            result = data.get(key);
        } catch (ClassCastException e) {
            String message = "Failed to cast data '" + key.getName() + "'. " +
                "Expected data type is '" + key.getType().getSimpleName() + "'";
            logger.error(message);
            throw e;
        }
        return result;
    }

    @Override
    public Map<String, Object> elementDataAsMap() {
        return data.asStringKeyMap();
    }

    @Override
    public Iterable<ElementData> elementDataAsIterable() {
        List<ElementData> result = new ArrayList<>();
        for (String key : keysInPipelineOrder()) {
            result.add((ElementData) data.asStringKeyMap().get(key));
        }
        return result;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, String> getWhere(PropertyMatcher matcher) {
        Map<String, String> map = new HashMap<>();
        for (FlowElement element : pipeline.getFlowElements()) {
            // We know the property has extends ElementPropertyMetaData as it
            // is a constraint of the FlowElement interface, so don't check this
            // cast
            for (Object propertyObject : element.getProperties()) {
                ElementPropertyMetaData property =
                    (ElementPropertyMetaData)propertyObject;
                if (property.isAvailable() && matcher.isMatch(property)) {
                    map.put(
                        element.getElementDataKey() + "." + property.getName(),
                        get(element.getElementDataKey())
                            .get(property.getName().toLowerCase()).toString());
                }
            }
        }
        return map;
    }

    @Override
    public DataKey generateKey(EvidenceKeyFilter filter) {
        Map<String, Object> evidenceMap = evidence.asKeyMap();
        DataKeyBuilder builder = new DataKeyBuilderDefault();
        for (String key : evidenceMap.keySet()) {
            if (filter.include(key)) {
                builder.add(
                    filter.order(key),
                    key,
                    evidenceMap.get(key));
            }
        }
        return builder.build();
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return pipeline.getEvidenceKeyFilter();
    }

    @Override
    public void close() throws Exception {
        for (Object elementData : data.asStringKeyMap().values()) {
            if (AutoCloseable.class.isAssignableFrom(elementData.getClass())) {
                ((AutoCloseable) elementData).close();
            }
        }
    }
}
