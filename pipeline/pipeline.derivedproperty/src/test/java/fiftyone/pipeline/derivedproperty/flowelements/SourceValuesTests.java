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

import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.WeightedValue;
import fiftyone.pipeline.core.data.WktString;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How a source value is converted to the type a script infers, with the
 * Java types elements hold.
 * <p>
 * See <a href="https://github.com/51Degrees/derived-properties/blob/cc30638632050795d9acc80d5118a290ffaba085/docs/format-1.md#conversion">format
 * 1, conversion</a>.
 */
public class SourceValuesTests {

    private static Slot read(Object raw, ValueType type) {
        return SourceValues.readValue(
            raw, new SourceProperty("device.Value", type));
    }

    private static void assertAbsent(Object raw, ValueType type, String why) {
        Slot slot = read(raw, type);
        assertFalse(slot.isAvailable(), raw + " was read as " + type);
        assertEquals(why, slot.getReason());
    }

    @Test
    public void booleans() {
        assertTrue(read(true, ValueType.BOOL).getBool());
        assertTrue(read(" TRUE\t", ValueType.BOOL).getBool());
        assertFalse(read("False", ValueType.BOOL).getBool());
        assertEquals("True", read(true, ValueType.STRING).getString());
        assertEquals("False", read(false, ValueType.STRING).getString());
        for (Object raw : new Object[]{"N/A", "Unknown", "", "1", "yes", 1}) {
            assertFalse(read(raw, ValueType.BOOL).isAvailable(),
                raw + " became a boolean");
        }
        assertAbsent("yes", ValueType.BOOL,
            "held 'yes' which cannot be read as bool");
    }

    @Test
    public void wholeNumbersHaveTheRangeOfInt() {
        assertEquals(5, read(5, ValueType.INT).getInt());
        assertEquals(5, read(5L, ValueType.INT).getInt());
        assertEquals(5, read((short) 5, ValueType.INT).getInt());
        assertEquals(5, read((byte) 5, ValueType.INT).getInt());
        assertEquals(5, read(BigInteger.valueOf(5), ValueType.INT).getInt());
        assertEquals(12, read(" +12 ", ValueType.INT).getInt());
        assertEquals(2, read(2.0, ValueType.INT).getInt());
        assertEquals(Integer.MIN_VALUE,
            read((long) Integer.MIN_VALUE, ValueType.INT).getInt());
        assertAbsent(3000000000L, ValueType.INT,
            "held '3000000000' which cannot be read as int");
        assertAbsent("3000000000", ValueType.INT,
            "held '3000000000' which cannot be read as int");
        assertAbsent("99999999999999999999", ValueType.INT,
            "held '99999999999999999999' which cannot be read as int");
        assertAbsent(2.5, ValueType.INT,
            "held '2.5' which cannot be read as int");
        for (Object raw : new Object[]{"1.0", "Unknown", "", "1e3", "0x10"}) {
            assertFalse(read(raw, ValueType.INT).isAvailable(),
                raw + " became an int");
        }
    }

    @Test
    public void doubles() {
        assertEquals(1.5, read(1.5f, ValueType.DOUBLE).getDouble());
        assertEquals(3000000000.0,
            read(3000000000L, ValueType.DOUBLE).getDouble());
        assertEquals(1000, read("1e3", ValueType.DOUBLE).getDouble());
        assertEquals(0.5, read(".5", ValueType.DOUBLE).getDouble());
        assertEquals(5, read("5.", ValueType.DOUBLE).getDouble());
        assertEquals(-2.5, read(" -2.5E0 ", ValueType.DOUBLE).getDouble());
        for (Object raw : new Object[]{"Unknown", "", "1e400", "NaN",
            "Infinity", "0x10", "1,5", Double.NaN,
            Double.POSITIVE_INFINITY}) {
            assertFalse(read(raw, ValueType.DOUBLE).isAvailable(),
                raw + " became a double");
        }
    }

    @Test
    public void strings() {
        assertEquals(" as written ",
            read(" as written ", ValueType.STRING).getString());
        assertEquals("8", read(8, ValueType.STRING).getString());
        assertEquals("8", read(8.0, ValueType.STRING).getString());
        assertEquals("8.5", read(8.5, ValueType.STRING).getString());
        assertEquals("1e+21", read(1e21, ValueType.STRING).getString());
        assertEquals("POINT (1 2)",
            read(new WktString("POINT (1 2)"), ValueType.STRING).getString());
    }

    @Test
    public void aValueWithNoValueCarriesItsMessage() {
        AspectPropertyValueDefault<Boolean> noValue =
            new AspectPropertyValueDefault<>();
        noValue.setNoValueMessage("The JavaScript has not run yet.");
        assertAbsent(noValue, ValueType.BOOL, "element 'device' has no " +
            "value for 'Value': The JavaScript has not run yet.");
        noValue.setNoValueMessage("");
        assertAbsent(noValue, ValueType.BOOL, "element 'device' has no " +
            "value for 'Value': property not present on this request");
        assertTrue(read(new AspectPropertyValueDefault<>("True"),
            ValueType.BOOL).getBool());
    }

    @Test
    public void aWeightedListGivesTheHeaviestValue() {
        List<IWeightedValue<Integer>> weighted =
            Arrays.<IWeightedValue<Integer>>asList(
                new WeightedValue<>(100, 2),
                new WeightedValue<>(900, 9),
                new WeightedValue<>(900, 7));
        assertEquals(9, read(weighted, ValueType.INT).getInt());
        assertEquals(9, read(new AspectPropertyValueDefault<>(weighted),
            ValueType.INT).getInt());
        assertEquals(4, read(new WeightedValue<>(1, 4), ValueType.INT)
            .getInt());
        assertAbsent(Arrays.asList("a", "b"), ValueType.STRING,
            "held a list where a single value is needed");
        assertAbsent(Collections.emptyList(), ValueType.STRING,
            "held a list where a single value is needed");
    }

    @Test
    public void aMissingPropertyIsNotPresent() {
        Slot slot = SourceValues.read(null,
            new SourceProperty("Device.Value", ValueType.BOOL));
        assertFalse(slot.isAvailable());
        assertEquals("element 'Device' has no value for 'Value': property " +
            "not present on this request", slot.getReason());
    }

    @Test
    public void numbersPrintAsJavaScriptPrintsThem() {
        assertEquals("0", JsText.number(-0.0));
        assertEquals("1", JsText.number(1.0));
        assertEquals("0.1", JsText.number(0.1));
        assertEquals("123456789012345680000",
            JsText.number(1.2345678901234568E20));
        assertEquals("1e+21", JsText.number(1e21));
        assertEquals("1.5e-7", JsText.number(1.5e-7));
        assertEquals("0.000001", JsText.number(1e-6));
        assertEquals("-2.5", JsText.number(-2.5));
        assertEquals("NaN", JsText.number(Double.NaN));
        assertEquals("-Infinity", JsText.number(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void stringsQuoteAsJsonStringifyQuotesThem() {
        assertEquals("\"a\\\"b\\\\c\\n\\u0001\u00e9\"",
            JsText.quote("a\"b\\c\n\u0001\u00e9"));
        assertEquals("\"\\ud800\"", JsText.quote("\ud800"));
        assertEquals("\"\ud83d\ude00\"", JsText.quote("\ud83d\ude00"));
    }
}
