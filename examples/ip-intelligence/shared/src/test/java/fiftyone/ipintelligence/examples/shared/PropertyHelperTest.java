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

import java.net.InetAddress;
import java.util.ArrayList;
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

    @Test
    public void testAsIPAddressProperty() throws Exception {
        AspectPropertyValue<InetAddress> test = new AspectPropertyValueDefault<>(
                InetAddress.getByAddress(new byte[]{8, 8, 8, 0}));
        assertEquals("8.8.8.0", PropertyHelper.asIPAddressProperty(test));
    }

    @Test
    public void testAsStringWeightedIPAddress() throws Exception {
        List<IWeightedValue<InetAddress>> addresses = new ArrayList<>();
        addresses.add(new WeightedValue<>(65535,
                InetAddress.getByAddress(new byte[]{8, 8, 8, (byte) 255})));
        AspectPropertyValue<List<IWeightedValue<InetAddress>>> test =
                new AspectPropertyValueDefault<>(addresses);
        assertEquals("8.8.8.255", PropertyHelper.asString(test));
    }
}