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
import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.derived.data.IDerivedPropertyData;
import fiftyone.pipeline.derived.script.DerivedScriptException;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Derived Property Element against the behaviour format 1 sets out.
 */
public class DerivedPropertyElementTests {

    private static final ILoggerFactory LOGGERS =
        LoggerFactory.getILoggerFactory();

    private static final String IS_GDPR =
        "Format: 1\n" +
        "Name: IsGdpr\n" +
        "Version: 0.1.0\n" +
        "Output:\n" +
        "  Name: IsGdpr\n" +
        "  Description: Whether the regulation is likely to apply.\n" +
        "  ValueType: bool\n" +
        "  IsList: false\n" +
        "Checks:\n" +
        "  InScope: { Property: ip.CountryCode, In: [\"FR\", \"GB\", \"NO\"] }\n" +
        "Rules:\n" +
        "  - When: { Check: InScope }\n" +
        "    Then: true\n" +
        "  - Else: false\n";

    private static final String BAND =
        "{ \"Format\": 1, \"Name\": \"Band\", \"Version\": \"1.0.0\",\n" +
        "  \"Output\": { \"Name\": \"Band\", \"Description\": \"A band.\",\n" +
        "    \"ValueType\": \"string\", \"IsList\": false,\n" +
        "    \"Values\": [ { \"Name\": \"High\" }, { \"Name\": \"Low\" } ] },\n" +
        "  \"Checks\": {\n" +
        "    \"Big\": { \"Property\": \"ip.Score\", \"Ge\": 8 },\n" +
        "    \"Human\": { \"Property\": \"device.IsCrawler\", \"Eq\": false } },\n" +
        "  \"Rules\": [\n" +
        "    { \"When\": { \"Failed\": \"Checks\", \"Eq\": 0 }, \"Then\": \"High\" },\n" +
        "    { \"Else\": \"Low\" } ] }\n";

    /**
     * Stands in for an engine, giving the values it is made with under its
     * element data key, and listing the properties it is made with.
     */
    static class SourceElement
        extends FlowElementBase<ElementData, ElementPropertyMetaData> {
        private final String key;
        private final Map<String, Object> values;
        private final List<ElementPropertyMetaData> properties =
            new ArrayList<>();

        SourceElement(String key, Map<String, Object> values,
                      String... properties) {
            super(LoggerFactory.getLogger(SourceElement.class),
                (flowData, element) -> new ElementDataBase(
                    LoggerFactory.getLogger(SourceElement.class), flowData) {
                });
            this.key = key;
            this.values = values;
            for (String property : properties) {
                this.properties.add(new ElementPropertyMetaDataDefault(
                    property, this, "", Object.class, true));
            }
        }

        @Override
        public String getElementDataKey() {
            return key;
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return new EvidenceKeyFilterWhitelist(Collections.<String>emptyList());
        }

        @Override
        public List<ElementPropertyMetaData> getProperties() {
            return properties;
        }

        @Override
        protected void processInternal(FlowData data) {
            ElementData elementData =
                data.getOrAdd(getTypedDataKey(), getDataFactory());
            for (Map.Entry<String, Object> value : values.entrySet()) {
                elementData.put(value.getKey(), value.getValue());
            }
        }

        @Override
        protected void managedResourcesCleanup() {
        }

        @Override
        protected void unmanagedResourcesCleanup() {
        }
    }

    private static Map<String, Object> values(Object... pairs) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private static AspectPropertyValue<Object> run(
        DerivedPropertyElement derived, String property,
        SourceElement... sources) throws Exception {
        PipelineBuilder builder = new PipelineBuilder(LOGGERS);
        for (SourceElement source : sources) {
            builder.addFlowElement(source);
        }
        try (Pipeline pipeline = builder.addFlowElement(derived).build();
             FlowData data = pipeline.createFlowData()) {
            data.process();
            return data.get(IDerivedPropertyData.class)
                .getValue(property, Object.class);
        }
    }

    @Test
    public void scriptInCode_ChoosesAValue() throws Exception {
        DerivedPropertyElement element = new DerivedPropertyElementBuilder(LOGGERS)
            .addScript("IsGdpr", IS_GDPR).build();
        assertEquals(true, run(element, "IsGdpr",
            new SourceElement("ip", values("CountryCode", "FR"),
                "CountryCode")).getValue());
        // YAML 1.2 keeps "NO" as the string, so Norway is in scope.
        element = new DerivedPropertyElementBuilder(LOGGERS)
            .addScript("IsGdpr", IS_GDPR).build();
        assertEquals(true, run(element, "IsGdpr",
            new SourceElement("ip", values("CountryCode", "NO"),
                "CountryCode")).getValue());
        element = new DerivedPropertyElementBuilder(LOGGERS)
            .addScript("IsGdpr", IS_GDPR).build();
        assertEquals(false, run(element, "IsGdpr",
            new SourceElement("ip", values("CountryCode", "US"),
                "CountryCode")).getValue());
    }

    @Test
    public void json_AggregateOfChecks() throws Exception {
        DerivedPropertyElement element = new DerivedPropertyElementBuilder(LOGGERS)
            .addScript("Band", BAND).build();
        assertEquals("High", run(element, "Band",
            new SourceElement("ip", values("Score", "9"), "Score"),
            new SourceElement("device", values("IsCrawler", "False"),
                "IsCrawler")).getValue());
        element = new DerivedPropertyElementBuilder(LOGGERS)
            .addScript("Band", BAND).build();
        assertEquals("Low", run(element, "Band",
            new SourceElement("ip", values("Score", 3), "Score"),
            new SourceElement("device", values("IsCrawler", false),
                "IsCrawler")).getValue());
    }

    @Test
    public void absentSource_NoValueNamingEveryProperty() throws Exception {
        DerivedPropertyElement element = new DerivedPropertyElementBuilder(LOGGERS)
            .addScript("Band", BAND).build();
        AspectPropertyValue<Object> value = run(element, "Band",
            new SourceElement("ip", values("Score", "Unknown"), "Score"),
            new SourceElement("device", values(), "IsCrawler"));
        assertFalse(value.hasValue());
        assertEquals("Derived property 'Band' has no value because 2 " +
            "source properties were not available. 'ip.Score' (held " +
            "'Unknown' which cannot be read as int). 'device.IsCrawler' " +
            "(element 'device' has no value for 'IsCrawler': property not " +
            "present on this request). " +
            fiftyone.pipeline.derived.script.DerivedScript.USUAL_CAUSES,
            value.getNoValueMessage());
    }

    @Test
    public void sourceNoElementSupplies_FailsThePipelineBuild() {
        DerivedPropertyElement element = new DerivedPropertyElementBuilder(LOGGERS)
            .addScript("IsGdpr", IS_GDPR).build();
        PipelineConfigurationException e = assertThrows(
            PipelineConfigurationException.class,
            () -> new PipelineBuilder(LOGGERS).addFlowElement(element).build());
        assertTrue(e.getMessage().contains("'ip.CountryCode'"), e.getMessage());
    }

    @Test
    public void faults_AllReportedTogether() {
        String broken =
            "Format: 2\n" +
            "Name: Broken\n" +
            "Version: 1.0.0\n" +
            "Colour: blue\n" +
            "Output: { Name: Broken, Description: x, ValueType: wkb, IsList: true }\n" +
            "Checks:\n" +
            "  A: { Property: device.IsCrawler, Eq: true, Ne: false }\n" +
            "Rules:\n" +
            "  - Else: x\n" +
            "  - When: { Check: Missing }\n" +
            "    Then: y\n";
        DerivedScriptException e = assertThrows(DerivedScriptException.class,
            () -> new DerivedPropertyElementBuilder(LOGGERS)
                .addScript("Broken", broken).build());
        String message = e.getMessage();
        assertTrue(e.getFaults().size() >= 5, message);
        assertTrue(message.contains("Colour"), message);
        assertTrue(message.contains("wkb"), message);
        assertTrue(message.contains("Rules[0]"), message);
        assertTrue(message.contains("Missing"), message);
    }

    @Test
    public void metadata_OnePropertyPerScript() {
        DerivedPropertyElement element = new DerivedPropertyElementBuilder(LOGGERS)
            .addScript("IsGdpr", IS_GDPR).build();
        assertEquals("derived", element.getElementDataKey());
        assertEquals(1, element.getProperties().size());
        assertEquals("IsGdpr", element.getProperties().get(0).getName());
        assertEquals(Boolean.class, element.getProperties().get(0).getType());
        assertEquals(Collections.singletonList("ip.CountryCode"),
            element.getScripts().get(0).getOutput().get("Dependencies"));
    }
}
