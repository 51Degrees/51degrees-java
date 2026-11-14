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

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.derivedproperty.BuiltInScript;
import fiftyone.pipeline.derivedproperty.DerivedScriptValidationException;
import fiftyone.pipeline.derivedproperty.data.IDerivedPropertyData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.NoValueException;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Derived Property Element against the specification and format 1,
 * using scripts written here so the tests do not depend on the content of
 * the shared scripts.
 */
public class DerivedPropertyElementTests {

    static final String BAND_YAML = "Format: 1\n" +
        "Name: Band\n" +
        "Version: 1.0.0\n" +
        "Output:\n" +
        "  Name: Band\n" +
        "  Description: A band.\n" +
        "  ValueType: string\n" +
        "  IsList: false\n" +
        "  Values:\n" +
        "    - { Name: Old }\n" +
        "    - { Name: New, Description: Recent. }\n" +
        "Checks:\n" +
        "  Recent: { Property: device.BrowserReleaseAge, Lt: 2.0 }\n" +
        "Rules:\n" +
        "  - When: { All: [ { Check: Recent }, " +
        "{ Property: device.IsCrawler, Eq: false } ] }\n" +
        "    Then: New\n" +
        "  - Else: Old\n";

    static final String BAND_JSON = "{\"Format\": 1, \"Name\": \"Band\", " +
        "\"Version\": \"1.0.0\",\n" +
        " \"Output\": {\"Name\": \"Band\", \"Description\": \"A band.\", " +
        "\"ValueType\": \"string\", \"IsList\": false,\n" +
        "  \"Values\": [{\"Name\": \"Old\"}, {\"Name\": \"New\", " +
        "\"Description\": \"Recent.\"}]},\n" +
        " \"Checks\": {\"Recent\": {\"Property\": " +
        "\"device.BrowserReleaseAge\", \"Lt\": 2}},\n" +
        " \"Rules\": [{\"When\": {\"All\": [{\"Check\": \"Recent\"}, " +
        "{\"Property\": \"device.IsCrawler\", \"Eq\": false}]},\n" +
        "  \"Then\": \"New\"}, {\"Else\": \"Old\"}]}";

    /**
     * A script reading one property under device and one under ip.
     */
    static final String LIKELY_YAML = "Format: 1\n" +
        "Name: Likely\n" +
        "Version: 1.0.0\n" +
        "Output:\n" +
        "  Name: Likely\n" +
        "  Description: Whether a person is likely to be viewing the page.\n" +
        "  ValueType: bool\n" +
        "  IsList: false\n" +
        "  Category: Human Detection\n" +
        "Checks:\n" +
        "  NotCrawler: { Property: device.IsCrawler, Eq: false }\n" +
        "  HumanAddress: { Property: ip.HumanProbability, Ge: 7 }\n" +
        "Rules:\n" +
        "  - When: { Failed: Checks, Eq: 0 }\n" +
        "    Then: true\n" +
        "  - Else: false\n";

    private static final String MOBILE_YAML = "Format: 1\n" +
        "Name: Mobile\n" +
        "Version: 1.0.0\n" +
        "Output: { Name: Mobile, Description: Whether the device is " +
        "mobile., ValueType: bool, IsList: false }\n" +
        "Rules:\n" +
        "  - When: { Property: device.IsMobile, Eq: true }\n" +
        "    Then: true\n" +
        "  - Else: false\n";

    private static final String KIND_YAML = "Format: 1\n" +
        "Name: Kind\n" +
        "Version: 1.0.0\n" +
        "Output: { Name: Kind, Description: The kind of device., " +
        "ValueType: string, IsList: false }\n" +
        "Rules:\n" +
        "  - When: { Property: derived.Mobile, Eq: true }\n" +
        "    Then: Phone\n" +
        "  - Else: Other\n";

    /**
     * A request the Likely script reads as true.
     */
    static Map<String, Map<String, Object>> likelyValues() {
        Map<String, Map<String, Object>> values = new LinkedHashMap<>();
        values.put("device", map("IsCrawler", "False"));
        values.put("ip", map("HumanProbability", 9));
        return values;
    }

    static Map<String, Object> map(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    static DerivedScript mustValidate(String text) {
        DerivedScriptValidator.Result result =
            DerivedScriptValidator.validate(text, "", "code");
        assertTrue(result.getFaults().isEmpty(),
            "The script did not validate:\n" + result.getFaults());
        return result.getScript();
    }

    static DerivedPropertyElementBuilder builder() {
        return new DerivedPropertyElementBuilder(
            LoggerFactory.getILoggerFactory());
    }

    static DerivedPropertyElement likely() {
        return builder().addScript("Likely", LIKELY_YAML).build();
    }

    /**
     * A YAML script and the JSON that mirrors it give the same model, and
     * the canonical form adds the inferred types and the computed
     * dependencies.
     */
    @Test
    public void yamlAndJsonGiveTheSameModel() {
        String fromYaml = CanonicalJson.print(mustValidate(BAND_YAML));
        String fromJson = CanonicalJson.print(mustValidate(BAND_JSON));
        assertEquals(fromYaml, fromJson);
        for (String want : new String[]{
            "\"device.BrowserReleaseAge\": {\n      \"Type\": \"int\"\n    }",
            "\"Dependencies\": [\n      \"device.BrowserReleaseAge\",\n" +
                "      \"device.IsCrawler\"\n    ]",
            "\"Lt\": 2\n"}) {
            assertTrue(fromYaml.contains(want),
                "The canonical form lacks " + want + ":\n" + fromYaml);
        }
    }

    /**
     * 2 and 2.0 are both whole numbers, so they infer the same type and do
     * not conflict.
     */
    @Test
    public void wholeNumbersInferInt() {
        DerivedScript model = mustValidate(BAND_YAML.replace("Checks:\n",
            "Checks:\n  Young: { Property: device.BrowserReleaseAge, " +
                "Lt: 2 }\n"));
        assertSame(ValueType.INT, model.getProperties().get(0).getType());
    }

    /**
     * Every fault in every script is reported at once, one line each.
     */
    @Test
    public void buildReportsEveryFault() {
        DerivedScriptValidationException e = assertThrows(
            DerivedScriptValidationException.class,
            () -> builder()
                .addScript("First", BAND_YAML.replace(
                    "Name: Band\nVersion", "Name: First\nVersion: x\nV"))
                .addScript("Second", BAND_YAML.replace(
                    "Format: 1", "Format: 2"))
                .setScripts("NoSuchScript")
                .build());
        String message = e.getMessage();
        for (String want : new String[]{"First (code)", "Second (code)",
            "Format must be 1, found 2",
            "'NoSuchScript' is not a built-in script"}) {
            assertTrue(message.contains(want),
                "The faults lack " + want + ":\n" + message);
        }
        assertEquals(e.getFaults().size(), message.split("\n").length);
    }

    @Test
    public void buildNeedsAScript() {
        assertThrows(IllegalStateException.class, () -> builder().build());
    }

    @Test
    public void twoScriptsWithOneOutputAreRefused() {
        DerivedScriptValidationException e = assertThrows(
            DerivedScriptValidationException.class,
            () -> builder()
                .addScript("Band", BAND_YAML)
                .addScript("Band", BAND_JSON)
                .build());
        assertTrue(e.getMessage().contains("is added more than once"),
            e.getMessage());
        assertTrue(e.getMessage().contains(
            "is also produced by the script 'Band'"), e.getMessage());
    }

    private static void assertPipelineRefused(
        String want,
        FlowElement<?, ?>... elements) {
        TestPipelineBuilder builder = new TestPipelineBuilder();
        for (FlowElement<?, ?> element : elements) {
            builder.addFlowElement(element);
        }
        PipelineConfigurationException e = assertThrows(
            PipelineConfigurationException.class, builder::build);
        assertTrue(e.getMessage().contains(want),
            "The build gave '" + e.getMessage() + "', not '" + want + "'");
    }

    /**
     * The Pipeline check names a key no element has, a source property no
     * element lists, and a derived property two elements produce.
     */
    @Test
    public void pipelineCheckNamesWhatIsMissing() {
        List<String> device = Collections.singletonList("IsCrawler");
        assertPipelineRefused(
            "no element in the pipeline has the element data key 'ip'",
            new StubSourceElement("device", device, null),
            likely());
        assertPipelineRefused(
            "no element under 'ip' lists the property 'HumanProbability'",
            new StubSourceElement("device", device, null),
            new StubSourceElement("ip",
                Collections.singletonList("CountryCode"), null),
            likely());
        assertPipelineRefused(
            "two Derived Property Elements in the pipeline produce the " +
                "derived property 'Likely'",
            new StubSourceElement("device", device, null),
            new StubSourceElement("ip",
                Collections.singletonList("HumanProbability"), null),
            likely(),
            likely());
    }

    /**
     * Where an element sits is not judged, so an element added before the
     * elements it reads builds, and gives no value naming each property.
     */
    @Test
    public void positionIsNotJudged() throws Exception {
        DerivedPropertyElement element = likely();
        List<FlowElement<?, ?>> elements = new ArrayList<>();
        elements.add(element);
        elements.addAll(SharedCasesTests.stubsFor(element, likelyValues()));
        TestPipelineBuilder builder = new TestPipelineBuilder();
        for (FlowElement<?, ?> e : elements) {
            builder.addFlowElement(e);
        }
        try (Pipeline pipeline = builder.build();
             FlowData data = pipeline.createFlowData()) {
            data.process();
            AspectPropertyValue<Boolean> value = data
                .get(IDerivedPropertyData.class)
                .getAs("Likely", Boolean.class);
            SharedCasesTests.checkMissing(value,
                Arrays.asList("device.IsCrawler", "ip.HumanProbability"),
                "Likely");
        }
    }

    /**
     * Sources that run in a parallel group are listed after the elements at
     * the top level, and the check still finds them.
     */
    @Test
    public void pipelineCheckFindsAParallelGroup() throws Exception {
        DerivedPropertyElement element = likely();
        List<StubSourceElement> stubs =
            SharedCasesTests.stubsFor(element, likelyValues());
        try (Pipeline pipeline = new TestPipelineBuilder()
                .addFlowElementsParallel(
                    stubs.toArray(new FlowElement<?, ?>[0]))
                .addFlowElement(element)
                .setAutoCloseElements(true)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.process();
            assertEquals(Boolean.TRUE, data.get(IDerivedPropertyData.class)
                .getAs("Likely", Boolean.class).getValue());
        }
    }

    /**
     * Elements in a parallel group write into the one Element Data under
     * derived at the same time.
     */
    @Test
    public void elementsInAParallelGroupShareTheElementData()
        throws Exception {
        DerivedPropertyElement mobile =
            builder().addScript("Mobile", MOBILE_YAML).build();
        DerivedPropertyElement band =
            builder().addScript("Band", BAND_YAML).build();
        Map<String, Map<String, Object>> values = new LinkedHashMap<>();
        values.put("device", map("IsMobile", true, "IsCrawler", false,
            "BrowserReleaseAge", 1));
        StubSourceElement device = new StubSourceElement("device",
            Arrays.asList("IsMobile", "IsCrawler", "BrowserReleaseAge"),
            values.get("device"));
        try (Pipeline pipeline = new TestPipelineBuilder()
                .addFlowElement(device)
                .addFlowElementsParallel(new FlowElement<?, ?>[]{mobile, band})
                .setAutoCloseElements(true)
                .build()) {
            for (int request = 0; request < 50; request++) {
                try (FlowData data = pipeline.createFlowData()) {
                    data.process();
                    IDerivedPropertyData derived =
                        data.get(IDerivedPropertyData.class);
                    assertEquals(Boolean.TRUE,
                        derived.getAs("Mobile", Boolean.class).getValue());
                    assertEquals("New",
                        derived.getAs("band", String.class).getValue());
                }
            }
        }
    }

    /**
     * A request with no Element Data under a key a script reads gives no
     * value, and the error is logged once for the script.
     */
    @Test
    public void noElementDataIsLoggedOnce() throws Exception {
        RecordingLoggerFactory logs = new RecordingLoggerFactory(Level.INFO);
        DerivedPropertyElement element =
            new DerivedPropertyElementBuilder(logs)
                .addScript("Likely", LIKELY_YAML)
                .build();
        try (Pipeline pipeline = new TestPipelineBuilder()
                .addFlowElement(new StubSourceElement("device",
                    Collections.singletonList("IsCrawler"),
                    map("IsCrawler", false)))
                .addFlowElement(new StubSourceElement("ip",
                    Collections.singletonList("HumanProbability"),
                    map("HumanProbability", 9), false))
                .addFlowElement(element)
                .build()) {
            for (int request = 0; request < 3; request++) {
                try (FlowData data = pipeline.createFlowData()) {
                    data.process();
                    AspectPropertyValue<Boolean> value = data
                        .get(IDerivedPropertyData.class)
                        .getAs("Likely", Boolean.class);
                    assertFalse(value.hasValue());
                    assertTrue(value.getNoValueMessage().contains(
                        "'ip.HumanProbability' (element 'ip' has no value " +
                            "for 'HumanProbability': property not present " +
                            "on this request)"),
                        value.getNoValueMessage());
                }
            }
        }
        List<String> errors = logs.messages(Level.ERROR);
        assertEquals(1, errors.size(), errors.toString());
        assertTrue(errors.get(0).contains("reads 'ip.HumanProbability'"),
            errors.get(0));
        assertTrue(errors.get(0).contains(
            "after the element that supplies 'ip'"), errors.get(0));
        assertTrue(errors.get(0).contains("reported once"), errors.get(0));
    }

    /**
     * A source property absent for any other reason, such as one the
     * JavaScript has not populated yet, is not logged.
     */
    @Test
    public void anAbsentPropertyIsNotLogged() throws Exception {
        RecordingLoggerFactory logs = new RecordingLoggerFactory(Level.INFO);
        DerivedPropertyElement element =
            new DerivedPropertyElementBuilder(logs)
                .addScript("Likely", LIKELY_YAML)
                .build();
        Map<String, Map<String, Object>> values = likelyValues();
        values.get("ip").remove("HumanProbability");
        AspectPropertyValue<?> value =
            SharedCasesTests.process(element, values, "Likely");
        assertFalse(value.hasValue());
        assertTrue(logs.messages(Level.ERROR).isEmpty(),
            logs.messages(Level.ERROR).toString());
    }

    /**
     * A script can read the property another script in the same element
     * produces, under the key derived.
     */
    @Test
    public void derivedFromDerived() throws Exception {
        DerivedPropertyElement element = builder()
            .addScript("Mobile", MOBILE_YAML)
            .addScript("Kind", KIND_YAML)
            .build();
        Map<String, Map<String, Object>> values = new LinkedHashMap<>();
        values.put("device", map("IsMobile", "True"));
        AspectPropertyValue<?> kind =
            SharedCasesTests.process(element, values, "Kind");
        assertTrue(kind.hasValue(), kind.getNoValueMessage());
        assertEquals("Phone", kind.getValue());
    }

    /**
     * The element accepts no evidence, lists one property per script and
     * exposes each script's full metadata.
     */
    @Test
    public void metadata() {
        DerivedPropertyElement element = builder()
            .addScript("Band", BAND_YAML)
            .addScript("Likely", LIKELY_YAML)
            .build();
        assertFalse(element.getEvidenceKeyFilter()
            .include("header.user-agent"));
        assertEquals("derived", element.getElementDataKey());
        List<ElementPropertyMetaData> properties = element.getProperties();
        assertEquals(2, properties.size());
        assertEquals("Band", properties.get(0).getName());
        assertEquals("", properties.get(0).getCategory());
        assertSame(String.class, properties.get(0).getType());
        assertTrue(properties.get(0).isAvailable());
        assertSame(element, properties.get(0).getElement());
        assertEquals("Likely", properties.get(1).getName());
        assertEquals("Human Detection", properties.get(1).getCategory());
        assertSame(Boolean.class, properties.get(1).getType());

        List<DerivedPropertyMetaData> scripts = element.getScripts();
        assertEquals(2, scripts.size());
        DerivedPropertyMetaData band = scripts.get(0);
        assertEquals("Band", band.getName());
        assertEquals("1.0.0", band.getVersion());
        assertEquals(1, band.getFormat());
        assertEquals("code", band.getSource());
        assertFalse(band.isDeprecated());
        DerivedOutputMetaData output = band.getOutput();
        assertEquals("A band.", output.getDescription());
        assertEquals("string", output.getValueType());
        assertSame(String.class, output.getValueClass());
        assertFalse(output.isList());
        assertNull(output.getDefaultValue());
        assertNull(output.getCategory());
        assertEquals(
            Arrays.<Object>asList("device.BrowserReleaseAge",
                "device.IsCrawler"),
            output.getDependencies());
        List<String> names = new ArrayList<>();
        List<String> descriptions = new ArrayList<>();
        for (DerivedValueMetaData value : output.getValues()) {
            names.add(value.getName());
            descriptions.add(value.getDescription());
        }
        assertEquals(Arrays.asList("Old", "New"), names);
        assertEquals(Arrays.asList(null, "Recent."), descriptions);
        assertNull(scripts.get(1).getOutput().getValues());
    }

    /**
     * A built-in script's source is its name.
     */
    @Test
    public void builtInScriptSourceIsItsName() {
        DerivedPropertyElement element = builder()
            .addScript(BuiltInScript.IsGdpr)
            .build();
        assertEquals("IsGdpr", element.getScripts().get(0).getSource());
    }

    /**
     * The build logs one information line per script, the compiled model
     * at debug level and a warning carrying a deprecated script's note.
     */
    @Test
    public void buildLogs() {
        RecordingLoggerFactory logs = new RecordingLoggerFactory(Level.DEBUG);
        String deprecated = BAND_YAML.replace("Version: 1.0.0\n",
            "Version: 1.0.0\nDeprecated: true\n" +
                "DeprecationNote: Use Likely.\n");
        new DerivedPropertyElementBuilder(logs)
            .addScript("Likely", LIKELY_YAML)
            .addScript("Band", deprecated)
            .build();
        List<String> info = logs.messages(Level.INFO, "Derived property");
        assertEquals(2, info.size(), info.toString());
        assertEquals("Derived property script 'Likely' version 1.0.0, " +
            "format 1, from code, produces 'derived.Likely'", info.get(0));
        List<String> debug = logs.messages(Level.DEBUG, "Derived property");
        assertEquals(2, debug.size(), debug.toString());
        assertTrue(debug.get(0).contains("\"Name\": \"Likely\""),
            debug.get(0));
        assertTrue(debug.get(0).contains(
            "\"ip.HumanProbability\": {\n      \"Type\": \"int\"\n    }"),
            debug.get(0));
        List<String> warnings = logs.messages(Level.WARN, "Derived property");
        assertEquals(1, warnings.size(), warnings.toString());
        assertEquals("Derived property script 'Band' is deprecated. " +
            "Use Likely.", warnings.get(0));
    }

    /**
     * The compiled model is printed only where debug is on.
     */
    @Test
    public void canonicalJsonIsNotWrittenWithoutDebug() {
        RecordingLoggerFactory logs = new RecordingLoggerFactory(Level.INFO);
        new DerivedPropertyElementBuilder(logs)
            .addScript("Likely", LIKELY_YAML)
            .build();
        assertTrue(logs.messages(Level.DEBUG).isEmpty());
        assertEquals(1, logs.messages(Level.INFO, "Derived property").size());
    }

    /**
     * The Element Data gives a value of the type the script produces, and
     * refuses another type or a property no script produces.
     */
    @Test
    public void elementDataGetAs() throws Exception {
        DerivedPropertyElement element = likely();
        try (Pipeline pipeline = buildWithStubs(element, likelyValues());
             FlowData data = pipeline.createFlowData()) {
            data.process();
            IDerivedPropertyData derived = data.get(IDerivedPropertyData.class);
            assertEquals(Boolean.TRUE,
                derived.getAs("likely", Boolean.class).getValue());
            assertThrows(ClassCastException.class,
                () -> derived.getAs("Likely", String.class));
            assertThrows(PropertyMissingException.class,
                () -> derived.getAs("Unlikely", Boolean.class));
        }
    }

    /**
     * Reading a value with no value raises the existing no value error,
     * carrying the message.
     */
    @Test
    public void readingNoValueThrowsWithTheMessage() throws Exception {
        Map<String, Map<String, Object>> values = likelyValues();
        values.get("device").put("IsCrawler", "N/A");
        AspectPropertyValue<?> value =
            SharedCasesTests.process(likely(), values, "Likely");
        NoValueException e =
            assertThrows(NoValueException.class, value::getValue);
        assertEquals("Derived property 'Likely' has no value because 1 " +
            "source property was not available. 'device.IsCrawler' (held " +
            "'N/A' which cannot be read as bool). " +
            DerivedPropertyElement.USUAL_CAUSES, e.getMessage());
    }

    static Pipeline buildWithStubs(
        DerivedPropertyElement element,
        Map<String, Map<String, Object>> values) throws Exception {
        TestPipelineBuilder builder = new TestPipelineBuilder();
        for (StubSourceElement stub :
            SharedCasesTests.stubsFor(element, values)) {
            builder.addFlowElement(stub);
        }
        return builder.addFlowElement(element).build();
    }
}
