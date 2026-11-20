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

package fiftyone.devicedetection.examples.shared;

import org.junit.AssumptionViolatedException;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class KeyHelperTest {
    /**
     * Values of the form KeyUtils accepts as a key. Neither is a real key.
     */
    private static final String KEY_A = "AQAAAAAAAAAAAAAAAAAAAAAA";
    private static final String KEY_B = "AgAAAAAAAAAAAAAAAAAAAAAA";

    /**
     * With no key anywhere, the lookup logs that there is none and stores
     * nothing, rather than passing null to System.setProperty.
     */
    @Test
    public void getOrSetTestResourceKey_NoKey_StoresNothing() {
        String name = KeyHelper.TEST_RESOURCE_KEY;
        // A test can clear a system property but not an environment
        // variable.
        assumeTrue(name + " is set in the environment",
            System.getenv(name) == null &&
                System.getenv(name.toUpperCase()) == null);
        String saved = System.clearProperty(name);
        try {
            assertNull(KeyHelper.getOrSetTestResourceKey(false));
            assertNull(System.getProperty(name));
        } finally {
            if (saved != null) {
                System.setProperty(name, saved);
            }
        }
    }

    @Test
    public void testResourceKey_IsTheSharedName() {
        assertEquals("_51DEGREES_RESOURCE_KEY", KeyHelper.TEST_RESOURCE_KEY);
    }

    /**
     * A test with no key is skipped, naming the variable to set, rather
     * than failed.
     */
    @Test
    public void requireResourceKey_NoKey_SkipsNamingTheVariable() {
        AssumptionViolatedException skipped = assertThrows(
            AssumptionViolatedException.class, () ->
                KeyHelper.requireResourceKey(name -> null,
                    Collections.singletonList(KeyHelper.TEST_RESOURCE_KEY),
                    "whether a device is mobile", "device"));
        assertEquals("This test calls the live 51Degrees cloud service, so " +
                "it needs a resource key, and it is skipped because " +
                "_51DEGREES_RESOURCE_KEY is not set as an environment " +
                "variable or as a system property. The key has to carry the " +
                "device product, because the test asks the service for " +
                "whether a device is mobile. To see the products a key " +
                "carries, open " +
                "https://cloud.51degrees.com/api/v4/accessibleproperties?resource=<key>. " +
                "Create a key at " + KeyHelper.CREATE_KEY_URL + ".",
            skipped.getMessage());
    }

    @Test
    public void requireResourceKey_TwoProducts_NamesBoth() {
        AssumptionViolatedException skipped = assertThrows(
            AssumptionViolatedException.class, () ->
                KeyHelper.requireResourceKey(name -> null,
                    Collections.singletonList("_51DEGREES_RESOURCE_KEY_SUPER"),
                    "the devices that match a TAC", "hardware", "device"));
        assertTrue(skipped.getMessage(), skipped.getMessage().contains(
            "skipped because _51DEGREES_RESOURCE_KEY_SUPER is not set"));
        assertTrue(skipped.getMessage(), skipped.getMessage().contains(
            "carry the hardware and device products, because"));
    }

    /**
     * Only the names the test gives are read. A key under another name,
     * such as the one every cloud example reads, does not stand in for the
     * key with every property.
     */
    @Test
    public void requireResourceKey_ReadsOnlyTheNamesGiven() {
        Map<String, String> values = new HashMap<>();
        values.put(KeyHelper.TEST_RESOURCE_KEY, KEY_A);
        assertThrows(AssumptionViolatedException.class, () ->
            KeyHelper.requireResourceKey(values::get,
                Collections.singletonList("_51DEGREES_RESOURCE_KEY_SUPER"),
                "the devices that match a TAC", "hardware", "device"));
    }

    /**
     * A name holding something that is not a key, such as the empty
     * string a pom passes for a key that is not set, is passed over.
     */
    @Test
    public void requireResourceKey_ValueThatIsNotAKey_IsPassedOver() {
        Map<String, String> values = new HashMap<>();
        values.put(KeyHelper.TEST_RESOURCE_KEY, "");
        assertThrows(AssumptionViolatedException.class, () ->
            KeyHelper.requireResourceKey(values::get,
                Collections.singletonList(KeyHelper.TEST_RESOURCE_KEY),
                "whether a device is mobile", "device"));
        values.put(KeyHelper.TEST_RESOURCE_KEY, KEY_B);
        assertEquals(KEY_B, KeyHelper.requireResourceKey(values::get,
            Collections.singletonList(KeyHelper.TEST_RESOURCE_KEY),
            "whether a device is mobile", "device"));
    }

    @Test
    public void answeredWithout_NoReason_SaysSo() {
        assertEquals("The cloud answered without BrowserName (no reason " +
                "given), which is what a key that does not carry the device " +
                "product gets.",
            KeyHelper.answeredWithout("BrowserName", null, "device"));
        assertEquals("The cloud answered without BrowserName (BrowserName " +
                "is a paid feature), which is what a key that does not carry " +
                "the device product gets.",
            KeyHelper.answeredWithout("BrowserName",
                "BrowserName is a paid feature", "device"));
    }
}
