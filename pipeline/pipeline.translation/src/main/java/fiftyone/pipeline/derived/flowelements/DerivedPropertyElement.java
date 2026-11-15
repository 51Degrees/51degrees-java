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

package fiftyone.pipeline.derived.flowelements;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.derived.data.DerivedPropertyMetaData;
import fiftyone.pipeline.derived.data.IDerivedPropertyData;
import fiftyone.pipeline.derived.script.DerivedScript;
import fiftyone.pipeline.derived.script.ValueType;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Produces new property values from property values that other flow
 * elements in the pipeline have already produced, following rules written
 * in scripts in format 1, and stores the results under the element data
 * key {@code derived}. The element holds no data file, makes no request
 * over the network, uses no resource key and reads no evidence.
 * <p>
 * Where every source property a script names is available, the script
 * chooses a value. Where any one of them is not, the script's property has
 * no value, and its no-value message names every property that was
 * missing and why.
 */
public class DerivedPropertyElement
    extends FlowElementBase<IDerivedPropertyData, ElementPropertyMetaData> {

    /**
     * The element data key every Derived Property Element shares.
     */
    public static final String ELEMENT_DATA_KEY = "derived";

    private final List<DerivedScript> scripts;
    private final List<DerivedPropertyMetaData> metaData;
    private volatile List<ElementPropertyMetaData> properties;
    private final EvidenceKeyFilter evidenceKeyFilter =
        new EvidenceKeyFilterWhitelist(Collections.<String>emptyList());

    /**
     * The flag per script that records the missing element data error has
     * been logged, which is the only state shared between requests.
     */
    private final AtomicBoolean[] logged;

    /**
     * Construct a new instance. Use {@link DerivedPropertyElementBuilder}.
     * @param logger used for logging
     * @param elementDataFactory creates the element data
     * @param scripts the compiled scripts
     */
    public DerivedPropertyElement(
        Logger logger,
        ElementDataFactory<IDerivedPropertyData> elementDataFactory,
        List<DerivedScript> scripts) {
        super(logger, elementDataFactory);
        this.scripts = Collections.unmodifiableList(new ArrayList<>(scripts));
        List<DerivedPropertyMetaData> meta = new ArrayList<>();
        logged = new AtomicBoolean[scripts.size()];
        for (int i = 0; i < scripts.size(); i++) {
            DerivedScript script = scripts.get(i);
            logged[i] = new AtomicBoolean(false);
            meta.add(new DerivedPropertyMetaData(script.getName(),
                script.getVersion(), script.getOutput()));
            logger.info("Derived property script '{}' version {}, format 1, " +
                    "from {}, produces '{}'.", script.getName(),
                script.getVersion(), script.getSource(),
                script.getOutputName());
            if (script.isDeprecated()) {
                logger.warn("Derived property script '{}' is deprecated. {}",
                    script.getName(), script.getDeprecationNote());
            }
        }
        this.metaData = Collections.unmodifiableList(meta);
    }

    @Override
    public String getElementDataKey() {
        return ELEMENT_DATA_KEY;
    }

    @Override
    public TypedKey<IDerivedPropertyData> getTypedDataKey() {
        if (typedKey == null) {
            typedKey = new TypedKeyDefault<>(getElementDataKey(),
                IDerivedPropertyData.class);
        }
        return typedKey;
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        // Every input is a property another element has produced.
        return evidenceKeyFilter;
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        List<ElementPropertyMetaData> result = properties;
        if (result == null) {
            List<ElementPropertyMetaData> props = new ArrayList<>();
            for (DerivedScript script : scripts) {
                props.add(new ElementPropertyMetaDataDefault(
                    script.getOutputName(), this, script.getCategory(),
                    script.getValueType().getJavaType(), true));
            }
            result = Collections.unmodifiableList(props);
            properties = result;
        }
        return result;
    }

    /**
     * The full property metadata of every script the element holds.
     * @return one entry per script
     */
    public List<DerivedPropertyMetaData> getScripts() {
        return metaData;
    }

    /**
     * Checks, when a pipeline adds the element, that some element in the
     * pipeline supplies every source property, and that no other Derived
     * Property Element produces the same property. Where an element sits
     * in the pipeline is not judged.
     */
    @Override
    public void addPipeline(Pipeline pipeline) {
        super.addPipeline(pipeline);
        Map<String, Map<String, ElementPropertyMetaData>> available =
            pipeline.getElementAvailableProperties();
        List<String> missing = new ArrayList<>();
        for (DerivedScript script : scripts) {
            for (DerivedScript.SourceProperty source : script.getSources()) {
                Map<String, ElementPropertyMetaData> element =
                    available.get(source.getElementDataKey());
                if (element == null ||
                    element.containsKey(source.getProperty()) == false) {
                    missing.add("Derived property script '" + script.getName() +
                        "' reads '" + source.getName() + "', which no " +
                        "element in the pipeline supplies.");
                }
            }
        }
        Set<String> mine = new HashSet<>();
        for (DerivedScript script : scripts) {
            mine.add(script.getOutputName().toLowerCase());
        }
        for (FlowElement<?, ?> element : pipeline.getFlowElements()) {
            if (element != this && element instanceof DerivedPropertyElement) {
                for (DerivedScript script :
                    ((DerivedPropertyElement) element).scripts) {
                    if (mine.contains(script.getOutputName().toLowerCase())) {
                        missing.add("Two Derived Property Elements produce " +
                            "the derived property '" + script.getOutputName() +
                            "'.");
                    }
                }
            }
        }
        if (missing.isEmpty() == false) {
            throw new PipelineConfigurationException(
                String.join(System.lineSeparator(), missing));
        }
    }

    @Override
    protected void processInternal(FlowData data) {
        IDerivedPropertyData result =
            data.getOrAdd(getTypedDataKey(), getDataFactory());
        // Each distinct source element data is got once.
        Map<String, ElementData> elements = new LinkedHashMap<>();
        for (int i = 0; i < scripts.size(); i++) {
            DerivedScript script = scripts.get(i);
            List<DerivedScript.SourceProperty> sources = script.getSources();
            Object[] values = new Object[sources.size()];
            String[] reasons = new String[sources.size()];
            boolean absent = false;
            for (int j = 0; j < sources.size(); j++) {
                DerivedScript.SourceProperty source = sources.get(j);
                String key = source.getElementDataKey().toLowerCase();
                ElementData element;
                if (elements.containsKey(key)) {
                    element = elements.get(key);
                } else {
                    element = elementData(data, source.getElementDataKey());
                    elements.put(key, element);
                }
                if (element == null) {
                    logMissingElementData(i, script, source);
                }
                reasons[j] = read(element, source, values, j);
                absent |= reasons[j] != null;
            }
            AspectPropertyValueDefault<Object> value =
                new AspectPropertyValueDefault<>();
            if (absent) {
                value.setNoValueMessage(script.noValueMessage(reasons));
            } else {
                value.setValue(script.evaluate(values));
            }
            result.put(script.getOutputName(), value);
        }
    }

    /**
     * The element data under the key, or null where the request carries
     * none.
     */
    private static ElementData elementData(FlowData data, String key) {
        try {
            return data.get(key);
        } catch (RuntimeException notThere) {
            return null;
        }
    }

    /**
     * Reads one source property into the values, and returns null, or the
     * reason the property is not available.
     */
    private static String read(ElementData element,
                               DerivedScript.SourceProperty source,
                               Object[] values,
                               int index) {
        String notPresent = "element '" + source.getElementDataKey() +
            "' has no value for '" + source.getProperty() +
            "': property not present on this request";
        if (element == null) {
            return notPresent;
        }
        Object raw;
        try {
            raw = element.get(source.getProperty());
        } catch (RuntimeException missing) {
            return notPresent;
        }
        if (raw instanceof AspectPropertyValue) {
            AspectPropertyValue<?> value = (AspectPropertyValue<?>) raw;
            if (value.hasValue() == false) {
                String message = value.getNoValueMessage();
                return message == null || message.isEmpty() ? notPresent :
                    "element '" + source.getElementDataKey() +
                        "' has no value for '" + source.getProperty() + "': " +
                        message;
            }
            raw = value.getValue();
        }
        if (raw == null) {
            return notPresent;
        }
        raw = heaviest(raw);
        ValueType.Read read = source.getType().read(raw);
        if (read.isAvailable() == false) {
            return read.failure();
        }
        values[index] = read.value();
        return null;
    }

    /**
     * The value with the highest weight of a list of weighted values, the
     * first where two weights are equal, or the value unchanged.
     */
    private static Object heaviest(Object raw) {
        if (raw instanceof List && ((List<?>) raw).isEmpty() == false) {
            IWeightedValue<?> best = null;
            for (Object member : (List<?>) raw) {
                if (member instanceof IWeightedValue == false) {
                    return raw;
                }
                IWeightedValue<?> weighted = (IWeightedValue<?>) member;
                if (best == null ||
                    weighted.getRawWeighting() > best.getRawWeighting()) {
                    best = weighted;
                }
            }
            return best.getValue();
        }
        return raw;
    }

    /**
     * Logs, once for each script, that a request carries no element data
     * under a key a script reads, which means the element that supplies it
     * ran after this one or failed before adding its data.
     */
    private void logMissingElementData(int index, DerivedScript script,
                                       DerivedScript.SourceProperty source) {
        if (logged[index].compareAndSet(false, true)) {
            logger.error("Derived property script '{}' reads '{}', but the " +
                    "request has no element data under '{}'. Add this " +
                    "element after the element that supplies '{}'. This " +
                    "error is reported once.", script.getName(),
                source.getName(), source.getElementDataKey(),
                source.getElementDataKey());
        }
    }

    @Override
    protected void managedResourcesCleanup() {
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }
}
