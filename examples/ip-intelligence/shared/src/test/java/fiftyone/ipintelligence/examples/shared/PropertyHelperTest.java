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
import fiftyone.pipeline.core.data.WeightedValue;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class PropertyHelperTest {

    @Test
    public void testAsStringBoolean() {
        AspectPropertyValue<Boolean> test = new AspectPropertyValueDefault<>(true);
        assertEquals("true", PropertyHelper.asString(test));
    }

    @Test
    public void testAsStringStringArray() {
        List<String> strings = new ArrayList<>();
        strings.add("one"); strings.add("two");
        AspectPropertyValue<List<String>> test = new AspectPropertyValueDefault<>(strings);
        assertEquals("one, two", PropertyHelper.asString(test));
    }

    @Test
    public void testAsBooleanArray() {
        List<Boolean> bools = new ArrayList<>();
        bools.add(true); bools.add(false);
        AspectPropertyValue<List<Boolean>> test = new AspectPropertyValueDefault<>(bools);
        assertEquals("true, false", PropertyHelper.asString(test));
    }

    @Test
    public void testAsNoValue() {
        AspectPropertyValue<List<Boolean>> test = new AspectPropertyValueDefault<>();
        assertTrue(PropertyHelper.asString(test).startsWith("Unknown"));
    }

    /**
     * Each weighted value is shown with its weighting, as the IP
     * Intelligence examples must show both. A raw weighting of 65535 is a
     * weighting of 1.
     */
    @Test
    public void testWeightedValuesShowTheirWeighting() {
        AspectPropertyValue<List<IWeightedValue<String>>> one =
            new AspectPropertyValueDefault<>(Collections.singletonList(
                new WeightedValue<>(65535, "Japan")));
        assertEquals("Japan (weighting 1.00)", PropertyHelper.asString(one));

        AspectPropertyValue<List<IWeightedValue<String>>> two =
            new AspectPropertyValueDefault<>(Arrays.asList(
                new WeightedValue<>(49151, "Tokyo"),
                new WeightedValue<>(16384, "Osaka")));
        assertEquals(
            "Tokyo (weighting 0.75), Osaka (weighting 0.25)",
            PropertyHelper.asString(two));
    }
}