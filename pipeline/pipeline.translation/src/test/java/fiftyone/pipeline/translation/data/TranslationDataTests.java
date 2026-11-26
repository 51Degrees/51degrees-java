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

package fiftyone.pipeline.translation.data;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.WeightedValue;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Tests the typed value accessors of {@link TranslationData}.
 */
public class TranslationDataTests {

    private final TestData data =
        new TestData(mock(Logger.class), mock(FlowData.class));

    @Test
    public void getValue_String() {
        data.put("p", new AspectPropertyValueDefault<>("value"));
        assertEquals("value", data.value("p", String.class).getValue());
    }

    @Test
    public void getList_String() {
        data.put("p", new AspectPropertyValueDefault<>(
            Arrays.asList("a", "b")));
        assertEquals(
            Arrays.asList("a", "b"),
            data.list("p", String.class).getValue());
    }

    @Test
    public void getWeightedList_String() {
        List<IWeightedValue<String>> stored =
            Collections.singletonList(new WeightedValue<>(3, "France"));
        data.put("p", new AspectPropertyValueDefault<>(stored));
        IWeightedValue<String> first =
            data.weightedList("p", String.class).getValue().get(0);
        assertEquals("France", first.getValue());
        assertEquals(3, first.getRawWeighting());
    }

    @Test
    public void getValue_WrongType() {
        data.put("p", new AspectPropertyValueDefault<>("value"));
        assertThrows(
            ClassCastException.class,
            () -> data.value("p", Integer.class));
    }

    private static class TestData extends TranslationData {
        TestData(Logger logger, FlowData flowData) {
            super(logger, flowData);
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
