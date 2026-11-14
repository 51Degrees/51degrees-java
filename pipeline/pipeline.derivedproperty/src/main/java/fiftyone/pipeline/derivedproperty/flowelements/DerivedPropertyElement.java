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

package fiftyone.pipeline.derivedproperty.flowelements;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.derivedproperty.data.IDerivedPropertyData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The Derived Property Element, which produces new property values from
 * property values other Flow Elements have already produced, following
 * rules written in a script, and writes them into the Element Data under
 * the key {@code derived}, which every instance in a Pipeline shares.
 * <p>
 * The element holds no data file, makes no request over the network and
 * reads no evidence. Everything it holds is fixed when it is built, so
 * concurrent requests need nothing further, and per request it reads each
 * source property once, then runs each script.
 * <p>
 * See the
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/pipeline-elements/derived-property-element.md">Derived
 * Property Element specification</a>.
 */
public final class DerivedPropertyElement
    extends FlowElementBase<IDerivedPropertyData, ElementPropertyMetaData>
    implements IDerivedPropertyElement {

    /**
     * The Element Data key every Derived Property Element in a Pipeline
     * writes under.
     */
    public static final String ELEMENT_DATA_KEY = "derived";

    /**
     * The sentence that closes every message for a derived property with no
     * value, being USUAL_CAUSES in tools/evaluate.mjs.
     */
    static final String USUAL_CAUSES =
        "Usual causes are the element that supplies the property not being " +
        "in the pipeline, or being added after this element rather than " +
        "before it, the property being excluded in the engine " +
        "configuration, the property not being included in the resource " +
        "key, or JavaScript that populates the property not having run yet.";

    private static final TypedKey<IDerivedPropertyData> DATA_KEY =
        new TypedKeyDefault<>(ELEMENT_DATA_KEY, IDerivedPropertyData.class);

    private final CompiledScript[] scripts;
    private final SourceRead[] reads;
    private final List<TypedKey<ElementData>> dataKeys;
    private final List<ElementPropertyMetaData> properties;
    private final List<DerivedPropertyMetaData> metadata;
    private final EvidenceKeyFilter evidenceKeyFilter =
        new EvidenceKeyFilterWhitelist(Collections.<String>emptyList());

    /**
     * Construct the element from validated scripts. Use
     * {@link DerivedPropertyElementBuilder} to build one.
     * @param logger used for logging
     * @param dataFactory makes the Element Data
     * @param models the validated scripts
     */
    DerivedPropertyElement(
        Logger logger,
        ElementDataFactory<IDerivedPropertyData> dataFactory,
        List<DerivedScript> models) {
        super(logger, dataFactory);
        Map<String, Integer> keyIndex = new HashMap<>();
        Map<String, Integer> readIndex = new HashMap<>();
        List<TypedKey<ElementData>> keys = new ArrayList<>();
        List<SourceRead> allReads = new ArrayList<>();
        List<CompiledScript> compiled = new ArrayList<>();
        List<ElementPropertyMetaData> outputs = new ArrayList<>();
        List<DerivedPropertyMetaData> scriptData = new ArrayList<>();
        for (DerivedScript model : models) {
            int[] scriptReads = new int[model.getProperties().size()];
            for (int i = 0; i < scriptReads.length; i++) {
                SourceProperty property = model.getProperties().get(i);
                String id = property.getKey() + "|" + property.getType();
                Integer index = readIndex.get(id);
                if (index == null) {
                    int key = -1;
                    if (property.getElementKey().equals(ELEMENT_DATA_KEY) ==
                        false) {
                        Integer known = keyIndex.get(property.getElementKey());
                        if (known == null) {
                            known = keys.size();
                            keyIndex.put(property.getElementKey(), known);
                            keys.add(new TypedKeyDefault<ElementData>(
                                property.getElementKey(), ElementData.class));
                        }
                        key = known;
                    }
                    index = allReads.size();
                    readIndex.put(id, index);
                    allReads.add(new SourceRead(property, key));
                }
                scriptReads[i] = index;
            }
            compiled.add(new CompiledScript(model, scriptReads));
            scriptData.add(new DerivedPropertyMetaData(model));
            OutputModel output = model.getOutput();
            outputs.add(new ElementPropertyMetaDataDefault(
                output.getName(),
                this,
                output.getCategory() == null ? "" : output.getCategory(),
                output.getValueType().getValueClass(),
                true));
        }
        this.scripts = compiled.toArray(new CompiledScript[0]);
        this.reads = allReads.toArray(new SourceRead[0]);
        this.dataKeys = Collections.unmodifiableList(keys);
        this.properties = Collections.unmodifiableList(outputs);
        this.metadata = Collections.unmodifiableList(scriptData);
    }

    @Override
    public String getElementDataKey() {
        return ELEMENT_DATA_KEY;
    }

    @Override
    public TypedKey<IDerivedPropertyData> getTypedDataKey() {
        return DATA_KEY;
    }

    /**
     * The element reads properties other elements produce and nothing from
     * the request itself, so it accepts no evidence.
     * @return a filter that includes no key
     */
    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return evidenceKeyFilter;
    }

    /**
     * @return one entry per script, with the output property's name, the
     * Java type of its value, the category from the script's output block,
     * and available true
     */
    @Override
    public List<ElementPropertyMetaData> getProperties() {
        return properties;
    }

    @Override
    public List<DerivedPropertyMetaData> getScripts() {
        return metadata;
    }

    /**
     * Every source property the element reads from another element's
     * Element Data, once each.
     */
    List<SourceProperty> sourceProperties() {
        List<SourceProperty> result = new ArrayList<>();
        for (SourceRead read : reads) {
            if (read.getKey() >= 0) {
                result.add(read.getProperty());
            }
        }
        return result;
    }

    /**
     * Read each source property once, then run each script, writing one
     * value, or one value with no value carrying a message, per script.
     */
    @Override
    protected void processInternal(FlowData data) {
        IDerivedPropertyData out = data.getOrAdd(DATA_KEY, getDataFactory());
        ElementData[] sources = new ElementData[dataKeys.size()];
        for (int i = 0; i < sources.length; i++) {
            TryGetResult<ElementData> source =
                data.tryGetValue(dataKeys.get(i));
            if (source.hasValue()) {
                sources[i] = source.getValue();
            }
        }
        Slot[] slots = new Slot[reads.length];
        for (int i = 0; i < reads.length; i++) {
            if (reads[i].getKey() >= 0) {
                slots[i] = SourceValues.read(
                    sources[reads[i].getKey()], reads[i].getProperty());
            }
        }
        for (CompiledScript script : scripts) {
            processScript(script, slots, sources, out);
        }
    }

    private void processScript(
        CompiledScript script,
        Slot[] slots,
        ElementData[] sources,
        IDerivedPropertyData out) {
        DerivedScript model = script.getModel();
        Slot[] scriptSlots = new Slot[script.readCount()];
        List<String> missing = null;
        List<String> reasons = null;
        SourceRead noData = null;
        for (int i = 0; i < scriptSlots.length; i++) {
            SourceRead read = reads[script.read(i)];
            if (read.getKey() < 0) {
                scriptSlots[i] = SourceValues.read(out, read.getProperty());
            } else {
                scriptSlots[i] = slots[script.read(i)];
                if (sources[read.getKey()] == null && noData == null) {
                    noData = read;
                }
            }
            if (scriptSlots[i].isAvailable() == false) {
                if (missing == null) {
                    missing = new ArrayList<>();
                    reasons = new ArrayList<>();
                }
                missing.add(model.getProperties().get(i).getName());
                reasons.add(scriptSlots[i].getReason());
            }
        }
        OutputModel output = model.getOutput();
        if (missing != null) {
            if (noData != null && script.firstMissingData()) {
                String key = noData.getProperty().getElementKey();
                logger.error("Derived property script '" + model.getName() +
                    "' reads '" + noData.getProperty().getName() + "', and a " +
                    "request carried no element data under '" + key + "'. " +
                    "Add this element to the pipeline after the element " +
                    "that supplies '" + key + "'. This error is reported " +
                    "once for this script.");
            }
            AspectPropertyValueDefault<Object> value =
                new AspectPropertyValueDefault<>();
            value.setNoValueMessage(
                missingMessage(output.getName(), missing, reasons));
            out.put(output.getName(), value);
            return;
        }
        out.put(output.getName(),
            valueOf(output.getValueType(), model.evaluate(scriptSlots)));
    }

    private static AspectPropertyValue<?> valueOf(
        ValueType type,
        Literal literal) {
        switch (type) {
            case BOOL:
                return new AspectPropertyValueDefault<>(literal.getBool());
            case INT:
                return new AspectPropertyValueDefault<>(
                    (int) literal.getNumber());
            case DOUBLE:
                return new AspectPropertyValueDefault<>(literal.getNumber());
            default:
                return new AspectPropertyValueDefault<>(literal.getText());
        }
    }

    /**
     * Say why a script has no value, naming every source property that
     * could not be read, in the wording of missingMessage in
     * tools/evaluate.mjs.
     */
    static String missingMessage(
        String outputName,
        List<String> missing,
        List<String> reasons) {
        StringBuilder message = new StringBuilder()
            .append("Derived property '").append(outputName)
            .append("' has no value because ");
        if (missing.size() == 1) {
            message.append("1 source property was not available.");
        } else {
            message.append(missing.size())
                .append(" source properties were not available.");
        }
        for (int i = 0; i < missing.size(); i++) {
            message.append(" '").append(missing.get(i)).append("' (")
                .append(reasons.get(i)).append(").");
        }
        return message.append(' ').append(USUAL_CAUSES).toString();
    }

    /**
     * Check that some element in the Pipeline supplies every source
     * property a script names, and that no two Derived Property Elements
     * produce the same property. Where an element sits is not judged,
     * because a Pipeline lists the members of a parallel group after the
     * elements at the top level, so a position does not say what ran
     * first.
     * <p>
     * See the
     * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/pipeline-elements/derived-property-element.md#the-pipeline-check">Pipeline
     * check</a>.
     * @param pipeline the Pipeline the element is being added to
     * @throws PipelineConfigurationException where the check fails
     */
    @Override
    public void addPipeline(Pipeline pipeline) {
        if (pipeline != null) {
            checkPipeline(pipeline);
        }
        super.addPipeline(pipeline);
    }

    private void checkPipeline(Pipeline pipeline) {
        Map<String, Map<String, ElementPropertyMetaData>> available =
            availableProperties(pipeline);
        List<FlowElement<?, ?>> elements = elementsOf(pipeline, available);
        Set<String> keys = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        keys.addAll(available.keySet());
        boolean allLoaded = allLoaded(pipeline);
        List<String> problems = new ArrayList<>();
        for (CompiledScript script : scripts) {
            for (SourceProperty property :
                script.getModel().getProperties()) {
                String problem = checkSupplied(elements, keys, allLoaded,
                    script.getModel().getName(), property);
                if (problem != null) {
                    problems.add(problem);
                }
            }
        }
        // Each name counts the distinct Derived Property Elements that
        // produce it.
        Map<String, Integer> producers = new HashMap<>();
        for (FlowElement<?, ?> element : elements) {
            if (element instanceof DerivedPropertyElement) {
                for (CompiledScript script :
                    ((DerivedPropertyElement) element).scripts) {
                    String name = script.getModel().getOutput().getName()
                        .toLowerCase(Locale.ROOT);
                    Integer count = producers.get(name);
                    producers.put(name, count == null ? 1 : count + 1);
                }
            }
        }
        for (CompiledScript script : scripts) {
            String name = script.getModel().getOutput().getName();
            Integer count = producers.get(name.toLowerCase(Locale.ROOT));
            if (count != null && count > 1) {
                problems.add("two Derived Property Elements in the pipeline " +
                    "produce the derived property '" + name + "'");
            }
        }
        if (problems.isEmpty() == false) {
            throw new PipelineConfigurationException(
                String.join(". ", problems));
        }
    }

    /**
     * Why no element supplies a source property, or null where one does.
     * An element whose properties are not known yet, such as a cloud engine
     * that could not reach the service, is taken to supply the properties
     * under its key, with a warning, so that a passing failure does not
     * stop the Pipeline being built.
     */
    private String checkSupplied(
        List<FlowElement<?, ?>> elements,
        Set<String> keys,
        boolean allLoaded,
        String script,
        SourceProperty property) {
        String key = property.getElementKey();
        String name = property.getPropertyName();
        boolean inspected = false;
        for (FlowElement<?, ?> element : elements) {
            if (element.getElementDataKey().equalsIgnoreCase(key) == false) {
                continue;
            }
            inspected = true;
            if (lists(element, name)) {
                return null;
            }
            if (element.hasLoadedProperties() == false) {
                warnNotLoaded(script, property);
                return null;
            }
        }
        if (inspected == false && keys.contains(key) == false) {
            return "derived property script '" + script + "' reads '" +
                property.getName() + "', and no element in the pipeline has " +
                "the element data key '" + key + "'";
        }
        if (inspected == false && allLoaded == false) {
            warnNotLoaded(script, property);
            return null;
        }
        return "derived property script '" + script + "' reads '" +
            property.getName() + "', and no element under '" + key +
            "' lists the property '" + name + "'";
    }

    private void warnNotLoaded(String script, SourceProperty property) {
        logger.warn("Derived property script '" + script + "' reads '" +
            property.getName() + "', and the element under '" +
            property.getElementKey() + "' has not loaded its properties " +
            "yet, so whether it supplies '" + property.getPropertyName() +
            "' cannot be checked.");
    }

    /**
     * Whether an element lists a property in its metadata, matched without
     * regard to case.
     */
    private static boolean lists(FlowElement<?, ?> element, String name) {
        try {
            for (ElementPropertyMetaData property : element.getProperties()) {
                if (property.getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        } catch (RuntimeException e) {
            return false;
        }
        return false;
    }

    /**
     * The Pipeline's available properties by Element Data key, which covers
     * the members of every parallel group as well as the elements at the
     * top level.
     */
    private static Map<String, Map<String, ElementPropertyMetaData>>
    availableProperties(Pipeline pipeline) {
        try {
            return pipeline.getElementAvailableProperties();
        } catch (RuntimeException e) {
            return Collections.emptyMap();
        }
    }

    /**
     * Every element of the Pipeline that has an Element Data key, with the
     * members of a parallel group found through the available properties,
     * whose metadata names the element that owns each property.
     */
    private static List<FlowElement<?, ?>> elementsOf(
        Pipeline pipeline,
        Map<String, Map<String, ElementPropertyMetaData>> available) {
        Map<FlowElement<?, ?>, Boolean> seen = new IdentityHashMap<>();
        List<FlowElement<?, ?>> elements = new ArrayList<>();
        for (Object top : pipeline.getFlowElements()) {
            addElement((FlowElement<?, ?>) top, seen, elements);
        }
        for (Map<String, ElementPropertyMetaData> byName :
            available.values()) {
            for (ElementPropertyMetaData property : byName.values()) {
                FlowElement<?, ?> owner = property.getElement();
                if (owner != null) {
                    addElement(owner, seen, elements);
                }
            }
        }
        return elements;
    }

    /**
     * Add an element that has an Element Data key. A parallel group has
     * none, and is passed over.
     */
    private static void addElement(
        FlowElement<?, ?> element,
        Map<FlowElement<?, ?>, Boolean> seen,
        List<FlowElement<?, ?>> elements) {
        if (seen.containsKey(element)) {
            return;
        }
        seen.put(element, Boolean.TRUE);
        try {
            element.getElementDataKey();
        } catch (UnsupportedOperationException e) {
            return;
        }
        elements.add(element);
    }

    private static boolean allLoaded(Pipeline pipeline) {
        for (Object element : pipeline.getFlowElements()) {
            if (((FlowElement<?, ?>) element).hasLoadedProperties() == false) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void managedResourcesCleanup() {
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }

    /**
     * Write the build log for each script, being one information line
     * giving the name, the version, the format, the source and the output
     * property, the compiled model as canonical JSON at debug level, and a
     * warning carrying the note of a deprecated script.
     */
    void logScripts() {
        for (CompiledScript script : scripts) {
            DerivedScript model = script.getModel();
            logger.info("Derived property script '" + model.getName() +
                "' version " + model.getVersion() + ", format " +
                model.getFormat() + ", from " + model.getSource() +
                ", produces '" + ELEMENT_DATA_KEY + "." +
                model.getOutput().getName() + "'");
            if (logger.isDebugEnabled()) {
                logger.debug("Derived property script '" + model.getName() +
                    "' compiled to:\n" + CanonicalJson.print(model));
            }
            if (model.isDeprecated()) {
                logger.warn("Derived property script '" + model.getName() +
                    "' is deprecated. " + model.getDeprecationNote());
            }
        }
    }
}
