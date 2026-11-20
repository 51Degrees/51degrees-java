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

import fiftyone.pipeline.derivedproperty.DerivedScriptFault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a script's text is read. Plain scalars follow the YAML 1.2 core
 * schema, as the reference tools do, quoted scalars are strings, keys are
 * written once without regard to case, and JSON is read by the same
 * reader.
 * <p>
 * See <a href="https://github.com/51Degrees/derived-properties/blob/cc30638632050795d9acc80d5118a290ffaba085/docs/format-1.md#writing-yaml-that-reads-the-way-you-meant">writing
 * YAML that reads the way you meant</a>.
 */
public class DerivedScriptParserTests {

    private static ScriptNode value(String yaml) throws Exception {
        return DerivedScriptParser.parse("Value: " + yaml).get("Value");
    }

    /**
     * A script comparing device.Flag with the literal given.
     */
    private static String flagScript(String literal) {
        return "Format: 1\nName: Flag\nVersion: \"1.0.0\"\n" +
            "Output: { Name: Flag, Description: A flag., ValueType: bool, " +
            "IsList: false }\n" +
            "Rules:\n" +
            "  - When: { Property: device.Flag, Eq: " + literal + " }\n" +
            "    Then: true\n" +
            "  - Else: false\n";
    }

    private static ValueType inferred(String literal) {
        return DerivedPropertyElementTests.mustValidate(flagScript(literal))
            .getProperties().get(0).getType();
    }

    @Test
    public void onlyTrueAndFalseAreBooleans() throws Exception {
        for (String text : new String[]{"true", "True", "TRUE"}) {
            assertSame(ScriptNode.Kind.BOOL, value(text).getKind(), text);
            assertTrue(value(text).getBool(), text);
        }
        for (String text : new String[]{"false", "False", "FALSE"}) {
            assertSame(ScriptNode.Kind.BOOL, value(text).getKind(), text);
        }
        for (String text : new String[]{"yes", "no", "on", "off", "y", "n",
            "NO", "Yes", "tRUE"}) {
            assertSame(ScriptNode.Kind.STRING, value(text).getKind(), text);
            assertEquals(text, value(text).getText());
        }
    }

    @Test
    public void yesInfersStringAndTrueInfersBool() {
        assertSame(ValueType.STRING, inferred("yes"));
        assertSame(ValueType.STRING, inferred("\"true\""));
        assertSame(ValueType.STRING, inferred("'false'"));
        assertSame(ValueType.BOOL, inferred("true"));
    }

    @Test
    public void numbersFollowTheCoreSchema() throws Exception {
        assertEquals(8, value("8").getNumber());
        assertEquals(-8, value("-8").getNumber());
        assertEquals(8, value("+8").getNumber());
        assertEquals(8.5, value("8.5").getNumber());
        assertEquals(800, value("8e2").getNumber());
        assertEquals(0.5, value(".5").getNumber());
        assertEquals(16, value("0x10").getNumber());
        assertEquals(15, value("0o17").getNumber());
        assertEquals(Double.POSITIVE_INFINITY, value(".inf").getNumber());
        assertEquals(Double.NEGATIVE_INFINITY, value("-.Inf").getNumber());
        assertTrue(Double.isNaN(value(".NaN").getNumber()));
        for (String text : new String[]{"1_000", "0b101", "1:30", "8.5.1",
            "0x", "1e", "0o8"}) {
            assertSame(ScriptNode.Kind.STRING, value(text).getKind(), text);
        }
        assertSame(ScriptNode.Kind.STRING, value("\"8\"").getKind());
    }

    @Test
    public void wholeNumbersInferIntWhateverTheirForm() {
        assertSame(ValueType.INT, inferred("8"));
        assertSame(ValueType.INT, inferred("8.0"));
        assertSame(ValueType.INT, inferred("8e0"));
        assertSame(ValueType.DOUBLE, inferred("8.5"));
        // Outside the range of int a whole number infers double.
        assertSame(ValueType.DOUBLE, inferred("3000000000"));
    }

    @Test
    public void nullForms() throws Exception {
        for (String text : new String[]{"~", "null", "Null", "NULL", ""}) {
            assertSame(ScriptNode.Kind.NULL, value(text).getKind(), text);
        }
        assertSame(ScriptNode.Kind.STRING, value("'null'").getKind());
    }

    @Test
    public void aKeyWrittenTwiceIsRefused() {
        ScriptParseException exact = assertThrows(ScriptParseException.class,
            () -> DerivedScriptParser.parse("Name: A\nName: B\n"));
        assertTrue(exact.getMessage().contains(
            "the key 'Name' is written twice in one mapping"),
            exact.getMessage());
        ScriptParseException cased = assertThrows(ScriptParseException.class,
            () -> DerivedScriptParser.parse(
                "Output:\n  Name: A\n  name: B\n"));
        assertTrue(cased.getMessage().contains(
            "the keys 'Name' and 'name' differ only in case"),
            cased.getMessage());
        assertEquals(2, cased.getLine());
        ScriptParseException json = assertThrows(ScriptParseException.class,
            () -> DerivedScriptParser.parse("{\"Name\": \"A\", \"NAME\": 1}"));
        assertTrue(json.getMessage().contains("differ only in case"),
            json.getMessage());
    }

    @Test
    public void textThatIsNotAScript() {
        assertEquals("the script is empty", assertThrows(
            ScriptParseException.class,
            () -> DerivedScriptParser.parse("")).getMessage());
        assertEquals("the script is empty", assertThrows(
            ScriptParseException.class,
            () -> DerivedScriptParser.parse("# only a comment\n"))
            .getMessage());
        assertEquals("the script must be a mapping of keys to values at the " +
            "top level", assertThrows(ScriptParseException.class,
            () -> DerivedScriptParser.parse("- 1\n- 2\n")).getMessage());
        ScriptParseException yaml = assertThrows(ScriptParseException.class,
            () -> DerivedScriptParser.parse("Name: A\n  Bad: [\n"));
        assertTrue(yaml.getMessage().startsWith("the text is not valid YAML: "),
            yaml.getMessage());
        assertTrue(yaml.getLine() > 0);
        ScriptParseException json = assertThrows(ScriptParseException.class,
            () -> DerivedScriptParser.parse("  {\"Name\": \"A\""));
        assertTrue(json.getMessage().startsWith("the text is not valid JSON: "),
            json.getMessage());
    }

    @Test
    public void anAliasThatRefersToItselfIsRefused() {
        ScriptParseException e = assertThrows(ScriptParseException.class,
            () -> DerivedScriptParser.parse("Rules: &a [ *a ]\n"));
        assertTrue(e.getMessage().startsWith("the text is not valid YAML"),
            e.getMessage());
    }

    @Test
    public void aliasesAreFollowed() throws Exception {
        ScriptNode document = DerivedScriptParser.parse(
            "A: &x { Property: device.X, Eq: 1 }\nB: *x\n");
        assertEquals(1, document.get("B").get("Eq").getNumber());
    }

    @Test
    public void faultsCarryTheLineOfTheMapping() {
        DerivedScriptValidator.Result result = DerivedScriptValidator.validate(
            DerivedPropertyElementTests.BAND_YAML.replace(
                "{ Property: device.IsCrawler, Eq: false }",
                "{ Property: device.IsCrawler, Equals: false }"),
            "Band", "code");
        assertNull(result.getScript());
        assertEquals(1, result.getFaults().size(), result.getFaults()
            .toString());
        DerivedScriptFault fault = result.getFaults().get(0);
        assertEquals("Rules[0].When.All[1]", fault.getPath());
        assertEquals(15, fault.getLine());
        assertEquals("Band (code) line 15 at Rules[0].When.All[1]: unknown " +
            "operator 'Equals', expected one of Eq, Ne, Gt, Ge, Lt, Le, In, " +
            "NotIn, StartsWith, EndsWith, Contains", fault.toString());
    }

    @Test
    public void inAndNotInAreRefusedOnACount() {
        DerivedScriptValidator.Result result = DerivedScriptValidator.validate(
            DerivedPropertyElementTests.LIKELY_YAML.replace(
                "{ Failed: Checks, Eq: 0 }", "{ Failed: Checks, In: 0 }"),
            "Likely", "code");
        assertNull(result.getScript());
        assertTrue(result.getFaults().get(0).getMessage().contains(
            "operator 'In' is not allowed on a count"),
            result.getFaults().toString());
    }
}
