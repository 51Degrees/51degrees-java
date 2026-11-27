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

package fiftyone.pipeline.engines.data;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.WeightedValue;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Tests the typed value accessors the generated data classes call:
 * {@link AspectDataBase#getValue}, {@link AspectDataBase#getList} and
 * {@link AspectDataBase#getWeightedList}.
 */
public class AspectDataBaseTypedValueTests {

    private TestData data;

    @Before
    public void initialise() {
        AspectEngine<?, ?> engine = mock(AspectEngine.class);
        data = new TestData(
            mock(Logger.class),
            mock(FlowData.class),
            engine);
    }

    @Test
    public void getValue_String() {
        data.put("p", new AspectPropertyValueDefault<>("value"));
        AspectPropertyValue<String> result = data.value("p", String.class);
        assertTrue(result.hasValue());
        assertEquals("value", result.getValue());
    }

    @Test
    public void getValue_Integer() {
        data.put("p", new AspectPropertyValueDefault<>(42));
        AspectPropertyValue<Integer> result = data.value("p", Integer.class);
        assertEquals(Integer.valueOf(42), result.getValue());
    }

    @Test
    public void getValue_Boolean() {
        data.put("p", new AspectPropertyValueDefault<>(true));
        AspectPropertyValue<Boolean> result = data.value("p", Boolean.class);
        assertEquals(Boolean.TRUE, result.getValue());
    }

    @Test
    public void getValue_Double() {
        data.put("p", new AspectPropertyValueDefault<>(1.5d));
        AspectPropertyValue<Double> result = data.value("p", Double.class);
        assertEquals(1.5d, result.getValue(), 0d);
    }

    @Test
    public void getValue_Float() {
        data.put("p", new AspectPropertyValueDefault<>(2.5f));
        AspectPropertyValue<Float> result = data.value("p", Float.class);
        assertEquals(2.5f, result.getValue(), 0f);
    }

    @Test
    public void getValue_Null() {
        data.put("p", new AspectPropertyValueDefault<String>(null));
        AspectPropertyValue<String> result = data.value("p", String.class);
        assertTrue(result.hasValue());
        assertEquals(null, result.getValue());
    }

    @Test
    public void getList_String() {
        data.put("p", new AspectPropertyValueDefault<>(
            Arrays.asList("a", "b")));
        AspectPropertyValue<List<String>> result =
            data.list("p", String.class);
        assertTrue(result.hasValue());
        assertEquals(Arrays.asList("a", "b"), result.getValue());
    }

    @Test
    public void getWeightedList_String() {
        List<IWeightedValue<String>> stored = new ArrayList<>();
        stored.add(new WeightedValue<>(65535, "GB"));
        stored.add(new WeightedValue<>(100, "FR"));
        data.put("p", new AspectPropertyValueDefault<>(stored));
        AspectPropertyValue<List<IWeightedValue<String>>> result =
            data.weightedList("p", String.class);
        assertTrue(result.hasValue());
        List<IWeightedValue<String>> values = result.getValue();
        assertEquals(2, values.size());
        assertEquals("GB", values.get(0).getValue());
        assertEquals(65535, values.get(0).getRawWeighting());
        assertEquals("FR", values.get(1).getValue());
        assertEquals(100, values.get(1).getRawWeighting());
    }

    @Test
    public void getValue_NoValue() {
        AspectPropertyValue<String> stored = new AspectPropertyValueDefault<>();
        stored.setNoValueMessage("no value because of the test");
        data.put("p", stored);
        AspectPropertyValue<String> result = data.value("p", String.class);
        assertFalse(result.hasValue());
        assertEquals(
            "no value because of the test",
            result.getNoValueMessage());
    }

    @Test
    public void getList_NoValue() {
        AspectPropertyValue<List<String>> stored =
            new AspectPropertyValueDefault<>();
        stored.setNoValueMessage("no list");
        data.put("p", stored);
        AspectPropertyValue<List<String>> result =
            data.list("p", String.class);
        assertFalse(result.hasValue());
        assertEquals("no list", result.getNoValueMessage());
    }

    @Test
    public void getWeightedList_NoValue() {
        AspectPropertyValue<List<IWeightedValue<String>>> stored =
            new AspectPropertyValueDefault<>();
        stored.setNoValueMessage("no weighted list");
        data.put("p", stored);
        AspectPropertyValue<List<IWeightedValue<String>>> result =
            data.weightedList("p", String.class);
        assertFalse(result.hasValue());
        assertEquals("no weighted list", result.getNoValueMessage());
    }

    @Test
    public void getValue_WrongType() {
        data.put("p", new AspectPropertyValueDefault<>("not a number"));
        try {
            data.value("p", Integer.class);
            fail("A value of the wrong type should throw.");
        } catch (ClassCastException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("'p'"));
        }
    }

    @Test
    public void getList_WrongElementType() {
        data.put("p", new AspectPropertyValueDefault<>(
            Arrays.asList("a", "b")));
        try {
            data.list("p", Integer.class);
            fail("A list element of the wrong type should throw.");
        } catch (ClassCastException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("'p'"));
        }
    }

    @Test
    public void getWeightedList_WrongValueType() {
        List<IWeightedValue<String>> stored = new ArrayList<>();
        stored.add(new WeightedValue<>(1, "GB"));
        data.put("p", new AspectPropertyValueDefault<>(stored));
        try {
            data.weightedList("p", Integer.class);
            fail("A weighted value of the wrong type should throw.");
        } catch (ClassCastException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("'p'"));
        }
    }

    @Test
    public void getValue_NotAnAspectPropertyValue() {
        data.put("p", "a plain string");
        try {
            data.value("p", String.class);
            fail("A stored value that is not an AspectPropertyValue " +
                "should throw.");
        } catch (ClassCastException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("'p'"));
        }
    }

    @Test
    public void getValue_ReturnsCopy() {
        AspectPropertyValue<String> stored =
            new AspectPropertyValueDefault<>("stored");
        data.put("p", stored);
        data.value("p", String.class).setValue("changed");
        assertEquals("stored", stored.getValue());
    }

    @Test
    public void getList_ReturnsCopy() {
        List<String> stored = new ArrayList<>(Arrays.asList("a", "b"));
        data.put("p", new AspectPropertyValueDefault<>(stored));
        data.list("p", String.class).getValue().add("c");
        assertEquals(Arrays.asList("a", "b"), stored);
    }

    private static class TestData extends AspectDataBase {
        TestData(
            Logger logger,
            FlowData flowData,
            AspectEngine<?, ?> engine) {
            super(logger, flowData, engine);
        }

        <V> AspectPropertyValue<V> value(String key, Class<V> type) {
            return getValue(key, type);
        }

        <V> AspectPropertyValue<List<V>> list(
            String key,
            Class<V> elementType) {
            return getList(key, elementType);
        }

        <V> AspectPropertyValue<List<IWeightedValue<V>>> weightedList(
            String key,
            Class<V> valueType) {
            return getWeightedList(key, valueType);
        }
    }
}
