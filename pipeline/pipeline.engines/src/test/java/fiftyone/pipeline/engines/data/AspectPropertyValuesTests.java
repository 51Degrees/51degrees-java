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

import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.WeightedValue;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the checked conversions of {@link AspectPropertyValues}, which
 * element data that is not an {@link AspectDataBase} uses.
 */
public class AspectPropertyValuesTests {

    @Test
    public void checkedValue_Value() {
        AspectPropertyValue<String> result = AspectPropertyValues.checkedValue(
            "p", new AspectPropertyValueDefault<>("value"), String.class);
        assertEquals("value", result.getValue());
    }

    @Test
    public void checkedValue_NullStored() {
        assertNull(AspectPropertyValues.checkedValue(
            "p", null, String.class));
    }

    @Test
    public void checkedValue_NoValue() {
        AspectPropertyValue<String> stored = new AspectPropertyValueDefault<>();
        stored.setNoValueMessage("none");
        AspectPropertyValue<String> result =
            AspectPropertyValues.checkedValue("p", stored, String.class);
        assertFalse(result.hasValue());
        assertEquals("none", result.getNoValueMessage());
    }

    @Test
    public void checkedList_Values() {
        AspectPropertyValue<List<String>> result =
            AspectPropertyValues.checkedList(
                "p",
                new AspectPropertyValueDefault<>(Arrays.asList("a", "b")),
                String.class);
        assertEquals(Arrays.asList("a", "b"), result.getValue());
    }

    @Test
    public void checkedWeightedList_Values() {
        List<IWeightedValue<String>> stored =
            Collections.singletonList(new WeightedValue<>(7, "GB"));
        AspectPropertyValue<List<IWeightedValue<String>>> result =
            AspectPropertyValues.checkedWeightedList(
                "p",
                new AspectPropertyValueDefault<>(stored),
                String.class);
        assertTrue(result.hasValue());
        assertEquals("GB", result.getValue().get(0).getValue());
        assertEquals(7, result.getValue().get(0).getRawWeighting());
    }

    @Test
    public void checkedValue_NotAnAspectPropertyValue() {
        try {
            AspectPropertyValues.checkedValue("p", "plain", String.class);
            fail("A stored value that is not an AspectPropertyValue " +
                "should throw.");
        } catch (ClassCastException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("'p'"));
        }
    }

    @Test
    public void checkedList_WrongElementType() {
        try {
            AspectPropertyValues.checkedList(
                "p",
                new AspectPropertyValueDefault<>(Arrays.asList(1, 2)),
                String.class);
            fail("A list element of the wrong type should throw.");
        } catch (ClassCastException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("'p'"));
        }
    }
}
