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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static fiftyone.pipeline.util.Check.getNotNull;

/**
 * Default implementation of the {@link FlowData} interface. This class is
 * package private, and should only be created by the
 * {@link FlowDataFactoryDefault} class.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#flow-data">Specification</a>
 */
class FlowDataDefault implements FlowData {

    private final Logger logger;


    private final PipelineInternal pipeline;

    /**
     * The errors added. While a parallel group runs, its members add their
     * errors under the lock on this collection, so they appear in the order
     * the members add them. At every other time an add takes no lock.
     */
    private final Collection<FlowError> errors;

    /**
     * The parallel groups running on this Flow Data, so that errors are
     * added under the lock only while one runs.
     */
    private final AtomicInteger groupsRunning = new AtomicInteger(0);

    private final TypedKeyMap data;

    private final Evidence evidence;

    private boolean stop = false;

    private final AtomicBoolean processed = new AtomicBoolean(false);

    /**
     * Set when processing ends, after which the Flow Data is read only. A
     * write tests it, and nothing waits on it.
     */
    private volatile boolean finished = false;

    static final String FINISHED_MESSAGE =
        "This Flow Data has finished processing, so it is read only. " +
        "Add Evidence before processing, and Element Data and errors only " +
        "while a Flow Element processes.";

    FlowDataDefault(Logger logger, Pipeline pipeline, Evidence evidence) {
        this.logger = logger;
        this.pipeline = (PipelineInternal) pipeline;
        this.evidence = evidence;
        if (pipeline.isConcurrent()) {
            // There are parallel elements, so the FlowData's
            // backing map and the errors collection must be thread safe.
            data = new TypedKeyMapBuilder(true).build();
            errors = new ConcurrentLinkedQueue<>();
        } else {
            // There are no parallel elements, so a non thread safe backing
            // map and errors collection will suffice.
            data = new TypedKeyMapBuilder(false).build();
            errors = new LinkedList<>();
        }
        logger.debug("FlowData '" + hashCode() + "' created.");

    }

    @Override
    public void process() {
        if (processed.getAndSet(true)) {
            throw new IllegalStateException("Flow data has already been processed");
        } else if (pipeline.isClosed()) {
            throw new IllegalStateException("Pipeline has been closed.");
        } else {
            try {
                pipeline.process(this);
            } finally {
                finished = true;
                // Evidence made by this package refuses writes from now on
                // too, including through getEvidence().
                if (evidence instanceof EvidenceDefault) {
                    ((EvidenceDefault) evidence).finish();
                }
            }
        }
    }

    /**
     * Fail a write once processing has finished.
     * @throws IllegalStateException if processing has finished
     */
    private void checkNotFinished() {
        if (finished) {
            throw new IllegalStateException(FINISHED_MESSAGE);
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

    /**
     * Called by a parallel group before its members start.
     */
    void groupStarted() {
        groupsRunning.incrementAndGet();
    }

    /**
     * Called by a parallel group once every member has stopped.
     */
    void groupStopped() {
        groupsRunning.decrementAndGet();
    }

    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }

    @Override
    public Collection<FlowError> getErrors() {
        return new ArrayList<>(errors);
    }

    @Override
    public void addError(FlowError error) {
        checkNotFinished();
        if (groupsRunning.get() > 0) {
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
            logger.error(message, error.getThrowable());
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
        return data.getKeys();
    }

    @Override
    public FlowData addEvidence(String key, Object value) {
        checkNotFinished();
        evidence.put(key, value);
        if (logger.isDebugEnabled()) {
            logger.debug("FlowData '" + hashCode() + "' set evidence " +
                "'" + key + "' to '" + value.toString() + "'.");
        }
        return this;
    }

    @Override
    public FlowData addEvidence(Map<String, ?> values) {
        checkNotFinished();
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
        // No lock. Members of a parallel group each add their own Element
        // Data with one compare and swap, and making an instance never
        // waits for another member.
        if (data.containsKey(elementDataKey) == false) {
            checkNotFinished();
            data.putIfAbsent(typedKey, dataFactory.create(this));
        }
        return (T) data.get(typedKey);
    }

    @Override
    public <T extends ElementData> T getOrAdd(
        TypedKey<T> key,
        FlowElement.DataFactory<T> dataFactory) {
        T result;

        TypedKey<Object> untypedKey = new TypedKeyDefault<>(
            key.getName(),
            Object.class);
        // No lock, as above.
        if (data.containsKey(key) == false) {
            checkNotFinished();
            data.putIfAbsent(key, dataFactory.create(this));
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
        return new ElementDataMap(data.asStringKeyMap());
    }

    /**
     * The Element Data as a map, whose writes fail once processing has
     * finished, whenever the map was taken. Reads go straight to the
     * Flow Data's map, which gives copies of its keys, values and entries.
     */
    private final class ElementDataMap extends AbstractMap<String, Object> {
        private final Map<String, Object> live;

        ElementDataMap(Map<String, Object> live) {
            this.live = live;
        }

        @Override
        public int size() {
            return live.size();
        }

        @Override
        public boolean isEmpty() {
            return live.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return live.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return live.containsValue(value);
        }

        @Override
        public Object get(Object key) {
            return live.get(key);
        }

        @Override
        public Set<String> keySet() {
            return live.keySet();
        }

        @Override
        public Collection<Object> values() {
            return live.values();
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            return live.entrySet();
        }

        @Override
        public Object put(String key, Object value) {
            checkNotFinished();
            return live.put(key, value);
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            checkNotFinished();
            live.putAll(m);
        }

        @Override
        public Object remove(Object key) {
            checkNotFinished();
            return live.remove(key);
        }

        @Override
        public void clear() {
            checkNotFinished();
            live.clear();
        }
    }

    @Override
    public Iterable<ElementData> elementDataAsIterable() {
        List<ElementData> result = new ArrayList<>();
        for (Object elementData : data.asStringKeyMap().values()) {
            result.add((ElementData) elementData);
        }
        return result;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, String> getWhere(PropertyMatcher matcher) {
        if (processed.get() == false) {
            throw new IllegalStateException(
                "This instance has not yet been processed.");
        }
        Map<String, String> map = new HashMap<>();
        for (FlowElement element : pipeline.getFlowElements()) {
            // An element that did not run, for example because an earlier
            // one stopped processing, has no data, and a property may have
            // no value. Neither is the caller's mistake, so both are left
            // out rather than thrown. See
            // https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/exception-handling.md#L57
            if (data.containsKey(element.getElementDataKey()) == false) {
                continue;
            }
            ElementData elementData = get(element.getElementDataKey());
            // We know the property has extends ElementPropertyMetaData as it
            // is a constraint of the FlowElement interface, so don't check this
            // cast
            for (Object propertyObject : element.getProperties()) {
                ElementPropertyMetaData property =
                    (ElementPropertyMetaData)propertyObject;
                if (property.isAvailable() && matcher.isMatch(property)) {
                    Object value = elementData.get(
                        property.getName().toLowerCase(Locale.ROOT));
                    if (value != null) {
                        map.put(
                            element.getElementDataKey() + "." +
                                property.getName(),
                            value.toString());
                    }
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
        // Pooled Element Data goes back to its Flow Element's pool. A
        // reference kept after this point is not supported.
        for (Object elementData : data.asStringKeyMap().values()) {
            if (elementData instanceof ElementDataBase) {
                ((ElementDataBase) elementData).release();
            }
        }
    }
}
