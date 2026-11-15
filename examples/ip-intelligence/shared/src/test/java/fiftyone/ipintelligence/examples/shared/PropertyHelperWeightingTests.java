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

package fiftyone.ipintelligence.examples.shared;

import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * "Examples MUST demonstrate accessing both the value and its associated
 * weighting" (ip-intelligence-specification/required-examples.md line 107).
 * The examples read each value of a property with its weighting through
 * {@link PropertyHelper#weightedValues}.
 */
public class PropertyHelperWeightingTests {

    private static IWeightedValue<Object> weighted(
        final int rawWeighting,
        final Object value) {
        return new IWeightedValue<Object>() {
            @Override
            public int getRawWeighting() {
                return rawWeighting;
            }

            @Override
            public Object getValue() {
                return value;
            }
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void weightedValues_ListOfWeightedValues_KeepsEachWeighting() {
        List<IWeightedValue<Object>> list = new ArrayList<>();
        list.add(weighted(49151, "gb"));
        list.add(weighted(16384, "ie"));
        AspectPropertyValue<String> property =
            new AspectPropertyValueDefault(list);

        List<IWeightedValue<?>> values =
            PropertyHelper.weightedValues(property);

        assertEquals(2, values.size());
        assertEquals("gb", values.get(0).getValue());
        assertEquals(49151 / 65535f, values.get(0).getWeighting(), 0.0001);
        assertEquals("ie", values.get(1).getValue());
        assertEquals(16384 / 65535f, values.get(1).getWeighting(), 0.0001);
    }

    /**
     * "a property with a single answer returns it with a weighting of 1"
     * (required-examples.md line 107)
     */
    @Test
    public void weightedValues_SingleValue_HasWeightingOfOne() {
        AspectPropertyValue<String> property =
            new AspectPropertyValueDefault<>("gb");

        List<IWeightedValue<?>> values =
            PropertyHelper.weightedValues(property);

        assertEquals(1, values.size());
        assertEquals("gb", values.get(0).getValue());
        assertEquals(1f, values.get(0).getWeighting(), 0.0001);
    }

    @Test
    public void weightedValues_NoValue_IsEmpty() {
        assertTrue(PropertyHelper.weightedValues(
            new AspectPropertyValueDefault<String>()).isEmpty());
        assertTrue(PropertyHelper.weightedValues(null).isEmpty());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void asWeightedString_ShowsValueAndWeighting() {
        List<IWeightedValue<Object>> list = new ArrayList<>();
        list.add(weighted(65535, "gb"));
        assertEquals("gb (weighting 1.00)", PropertyHelper.asWeightedString(
            new AspectPropertyValueDefault(list)));
    }
}
