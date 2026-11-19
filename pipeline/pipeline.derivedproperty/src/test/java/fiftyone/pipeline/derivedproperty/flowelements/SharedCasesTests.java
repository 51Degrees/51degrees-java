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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.WeightedValue;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.derivedproperty.BuiltInScript;
import fiftyone.pipeline.derivedproperty.DerivedScriptFault;
import fiftyone.pipeline.derivedproperty.data.IDerivedPropertyData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Runs the case files of the derived-properties repository, which every
 * language runs, so the answers and the faults are checked to match case
 * for case. The repository is the submodule pipeline/derived-properties.
 * <p>
 * See <a href="https://github.com/51Degrees/derived-properties/blob/cc30638632050795d9acc80d5118a290ffaba085/docs/testing.md">writing
 * and running cases</a>.
 */
public class SharedCasesTests {

    /**
     * The largest raw weighting a weighted value carries.
     */
    private static final int MAX_RAW_WEIGHTING = 65535;

    /**
     * The derived-properties submodule, which a test fails without.
     */
    static Path sharedFolder() {
        Path folder = Paths.get("..", "derived-properties");
        if (Files.isDirectory(folder.resolve("tests")) == false) {
            fail("The derived-properties submodule is missing from " +
                folder.toAbsolutePath() + ". Run 'git submodule update " +
                "--init pipeline/derived-properties'.");
        }
        return folder;
    }

    /**
     * The files in a folder whose names end with the text given, in name
     * order.
     */
    static List<Path> files(Path folder, String glob) throws IOException {
        List<Path> found = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                 Files.newDirectoryStream(folder, glob)) {
            for (Path path : stream) {
                found.add(path);
            }
        }
        found.sort((a, b) -> a.getFileName().toString()
            .compareTo(b.getFileName().toString()));
        return found;
    }

    static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @TestFactory
    public List<DynamicTest> sharedCases() throws Exception {
        List<Path> files = files(
            sharedFolder().resolve("tests"), "*.cases.yaml");
        assertFalse(files.isEmpty(),
            "There are no case files in the derived-properties submodule.");
        List<DynamicTest> tests = new ArrayList<>();
        for (Path file : files) {
            ScriptNode cases = DerivedScriptParser.parse(read(file));
            String script = cases.get("Script").getText();
            BuiltInScript builtIn = BuiltInScript.fromName(script);
            if (builtIn == null) {
                fail(file + " proves the script " + script + ", which has " +
                    "no BuiltInScript.");
            }
            DerivedPropertyElement element =
                new DerivedPropertyElementBuilder(
                    LoggerFactory.getILoggerFactory())
                    .addScript(builtIn)
                    .build();
            String output = element.getScripts().get(0).getOutput().getName();
            for (boolean wrap : new boolean[]{false, true}) {
                for (ScriptNode c : cases.get("Cases").getItems()) {
                    String name = script + "/" + c.get("Name").getText() +
                        "/wrapped=" + wrap;
                    tests.add(DynamicTest.dynamicTest(name, () ->
                        runCase(element, output, c, wrap)));
                }
            }
        }
        return tests;
    }

    private static void runCase(
        DerivedPropertyElement element,
        String output,
        ScriptNode c,
        boolean wrap) throws Exception {
        Map<String, Map<String, Object>> values = new LinkedHashMap<>();
        ScriptNode properties = c.get("Properties");
        for (int i = 0; i < properties.getKeys().size(); i++) {
            String property = properties.getKeys().get(i);
            int dot = property.indexOf('.');
            String key = property.substring(0, dot).toLowerCase(Locale.ROOT);
            Map<String, Object> forKey = values.get(key);
            if (forKey == null) {
                forKey = new LinkedHashMap<>();
                values.put(key, forKey);
            }
            forKey.put(property.substring(dot + 1),
                caseValue(properties.getValues().get(i), wrap));
        }
        AspectPropertyValue<?> got = process(element, values, output);
        ScriptNode expect = c.get("Expect");
        ScriptNode value = expect.get("Value");
        if (value != null) {
            assertTrue(got.hasValue(), "Expected '" + expectedText(value) +
                "' but there was no value. " + got.getNoValueMessage());
            assertEquals(expectedText(value), valueText(got.getValue()));
            return;
        }
        List<String> missing = new ArrayList<>();
        for (ScriptNode name : expect.get("Missing").getItems()) {
            missing.add(name.getText());
        }
        checkMissing(got, missing, output);
    }

    /**
     * Run one request through stubs holding the values and the element,
     * and give what the element wrote for the output property.
     */
    static AspectPropertyValue<?> process(
        DerivedPropertyElement element,
        Map<String, Map<String, Object>> values,
        String output) throws Exception {
        TestPipelineBuilder builder = new TestPipelineBuilder();
        for (StubSourceElement stub : stubsFor(element, values)) {
            builder.addFlowElement(stub);
        }
        try (Pipeline pipeline = builder.addFlowElement(element).build();
             FlowData data = pipeline.createFlowData()) {
            data.process();
            Object value = data.get(IDerivedPropertyData.class).get(output);
            assertTrue(value instanceof AspectPropertyValue<?>,
                output + " is " + value + ", not an AspectPropertyValue");
            return (AspectPropertyValue<?>) value;
        }
    }

    /**
     * A stub for each key the element reads, listing every property read
     * under it and holding the values given for it.
     */
    static List<StubSourceElement> stubsFor(
        DerivedPropertyElement element,
        Map<String, Map<String, Object>> values) {
        Map<String, List<String>> names = new LinkedHashMap<>();
        for (SourceProperty property : element.sourceProperties()) {
            List<String> forKey = names.get(property.getElementKey());
            if (forKey == null) {
                forKey = new ArrayList<>();
                names.put(property.getElementKey(), forKey);
            }
            forKey.add(property.getPropertyName());
        }
        List<StubSourceElement> stubs = new ArrayList<>();
        for (Map.Entry<String, List<String>> key : names.entrySet()) {
            stubs.add(new StubSourceElement(
                key.getKey(), key.getValue(), values.get(key.getKey())));
        }
        return stubs;
    }

    /**
     * A value from a case file as a source element holds it. The String
     * form is the string, NoValue is a value with no value, and a list of
     * Value and Weight pairs is a list of weighted values.
     */
    private static Object caseValue(ScriptNode node, boolean wrap) {
        switch (node.getKind()) {
            case NULL:
                return null;
            case BOOL:
            case NUMBER:
            case STRING:
                Object value = scalar(node);
                return wrap ? new AspectPropertyValueDefault<>(value) : value;
            case MAP:
                ScriptNode noValue = node.get("NoValue");
                if (noValue != null) {
                    AspectPropertyValueDefault<Object> missing =
                        new AspectPropertyValueDefault<>();
                    missing.setNoValueMessage(Literal.of(noValue).toString());
                    return missing;
                }
                ScriptNode text = node.get("String");
                if (text != null) {
                    if (text.getKind() == ScriptNode.Kind.NULL) {
                        return null;
                    }
                    String string = Literal.of(text).toString();
                    return wrap ?
                        new AspectPropertyValueDefault<>(string) : string;
                }
                return new LinkedHashMap<>();
            default:
                return weighted(node);
        }
    }

    /**
     * A list whose members are not all weighted values cannot be read
     * where one value is needed, whatever it holds. A case gives weights
     * of any size, and a raw weighting is 16 bits, so each weight is scaled
     * against the largest, which keeps the order.
     */
    private static List<Object> weighted(ScriptNode node) {
        List<Object> values = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double largest = 0;
        for (ScriptNode item : node.getItems()) {
            if (item.getKind() != ScriptNode.Kind.MAP ||
                item.get("Value") == null) {
                List<Object> nulls = new ArrayList<>();
                for (int i = 0; i < node.getItems().size(); i++) {
                    nulls.add(null);
                }
                return nulls;
            }
            ScriptNode weight = item.get("Weight");
            double w = weight == null ? 0 : weight.getNumber();
            values.add(caseValue(item.get("Value"), false));
            weights.add(w);
            largest = Math.max(largest, w);
        }
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            int raw = largest > 0 && weights.get(i) > 0 ?
                (int) Math.round(weights.get(i) / largest * MAX_RAW_WEIGHTING) :
                0;
            IWeightedValue<Object> member =
                new WeightedValue<>(raw, values.get(i));
            list.add(member);
        }
        return list;
    }

    /**
     * A scalar with the type as written, a whole number being an Integer
     * where it fits and a Long where it does not.
     */
    private static Object scalar(ScriptNode node) {
        switch (node.getKind()) {
            case BOOL:
                return node.getBool();
            case NUMBER:
                double number = node.getNumber();
                if (DerivedScriptValidator.isWhole(number) &&
                    number >= Integer.MIN_VALUE &&
                    number <= Integer.MAX_VALUE) {
                    return (int) number;
                }
                if (DerivedScriptValidator.isWhole(number) &&
                    Math.abs(number) < Long.MAX_VALUE) {
                    return (long) number;
                }
                return number;
            default:
                return node.getText();
        }
    }

    /**
     * The text of a value, as the case files compare values.
     */
    static String valueText(Object value) {
        if (value instanceof Double) {
            return JsText.number((Double) value);
        }
        return String.valueOf(value);
    }

    /**
     * The text of an expected value in a case file.
     */
    private static String expectedText(ScriptNode node) {
        return Literal.of(node).toString();
    }

    /**
     * A missing value names every property that could not be read, so the
     * message gives the count and each name.
     */
    static void checkMissing(
        AspectPropertyValue<?> got,
        List<String> missing,
        String output) {
        assertFalse(got.hasValue(), () -> "Expected no value naming " +
            missing + " but got '" + got.getValue() + "'");
        String message = got.getNoValueMessage();
        String noun = missing.size() == 1 ?
            "1 source property was not available" :
            missing.size() + " source properties were not available";
        String prefix = "Derived property '" + output +
            "' has no value because " + noun + ". ";
        assertTrue(message.startsWith(prefix),
            "The message '" + message + "' does not start '" + prefix + "'");
        String lower = message.toLowerCase(Locale.ROOT);
        for (String name : missing) {
            assertTrue(
                lower.contains("'" + name.toLowerCase(Locale.ROOT) + "' ("),
                "The message '" + message + "' does not name " + name);
        }
        assertTrue(message.endsWith(DerivedPropertyElement.USUAL_CAUSES),
            "The message '" + message + "' does not end with the usual " +
                "causes");
    }

    @TestFactory
    public List<DynamicTest> rejectedScripts() throws Exception {
        List<Path> files = files(
            sharedFolder().resolve("tests").resolve("invalid"), "*.yaml");
        assertFalse(files.isEmpty(), "There are no rejection cases in the " +
            "derived-properties submodule.");
        List<DynamicTest> tests = new ArrayList<>();
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            ScriptNode c = DerivedScriptParser.parse(read(file));
            tests.add(DynamicTest.dynamicTest(fileName, () ->
                checkRejected(c, fileName)));
        }
        return tests;
    }

    private static void checkRejected(ScriptNode c, String fileName) {
        ScriptNode name = c.get("Name");
        DerivedScriptValidator.Result result = DerivedScriptValidator.validate(
            c.get("Script").getText(),
            name == null ? "" : name.getText(),
            fileName);
        assertNull(result.getScript(),
            "The script validated, and the case says it must not.");
        assertFalse(result.getFaults().isEmpty(),
            "The script validated, and the case says it must not.");
        StringBuilder all = new StringBuilder();
        for (DerivedScriptFault fault : result.getFaults()) {
            all.append('\n').append(fault);
        }
        ScriptNode expect = c.get("Expect");
        ScriptNode paths = expect.get("Paths");
        if (paths != null) {
            for (ScriptNode path : paths.getItems()) {
                boolean found = false;
                for (DerivedScriptFault fault : result.getFaults()) {
                    found = found || fault.getPath().equals(path.getText());
                }
                assertTrue(found, "Expected a fault at '" + path.getText() +
                    "'. Faults were:" + all);
            }
        }
        ScriptNode mentions = expect.get("Mentions");
        if (mentions != null) {
            for (ScriptNode mention : mentions.getItems()) {
                boolean found = false;
                for (DerivedScriptFault fault : result.getFaults()) {
                    found = found ||
                        fault.getMessage().contains(mention.getText());
                }
                assertTrue(found, "Expected a fault mentioning '" +
                    mention.getText() + "'. Faults were:" + all);
            }
        }
    }
}
