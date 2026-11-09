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
    public void requireResourceKey_NoKey_FailsNamingEveryName() {
        AssertionError error = assertThrows(AssertionError.class, () ->
            KeyHelper.requireResourceKey(name -> null,
                Collections.singletonList(KeyHelper.TEST_RESOURCE_KEY),
                "whether a device is mobile", "device"));
        assertEquals("This test calls the live 51Degrees cloud service, so " +
                "it needs a resource key and there is nothing to fall back " +
                "on. None of _51DEGREES_RESOURCE_KEY, " +
                "_51DEGREES_RESOURCE_KEY_BESPOKE, TestResourceKey is set, as " +
                "an environment variable or as a system property, tried in " +
                "that order. The key has to carry the device product, " +
                "because the test asks the service for whether a device is " +
                "mobile. To see the products a key carries, open " +
                "https://cloud.51degrees.com/api/v4/accessibleproperties?resource=<key>. " +
                "Create a key at " + KeyHelper.CREATE_KEY_URL + ".",
            error.getMessage());
    }

    @Test
    public void requireResourceKey_TwoProducts_NamesBoth() {
        AssertionError error = assertThrows(AssertionError.class, () ->
            KeyHelper.requireResourceKey(name -> null,
                Collections.singletonList("SuperResourceKey"),
                "the devices that match a TAC", "hardware", "device"));
        assertTrue(error.getMessage(), error.getMessage().contains(
            "None of _51DEGREES_RESOURCE_KEY, " +
                "_51DEGREES_RESOURCE_KEY_BESPOKE, SuperResourceKey is set"));
        assertTrue(error.getMessage(), error.getMessage().contains(
            "carry the hardware and device products, because"));
    }

    /**
     * A name holding something that is not a key, such as the empty
     * string the example poms pass for a key that is not set, is passed
     * over.
     */
    @Test
    public void requireResourceKey_FirstValidNameWins() {
        Map<String, String> values = new HashMap<>();
        values.put("_51DEGREES_RESOURCE_KEY", "");
        values.put("_51DEGREES_RESOURCE_KEY_BESPOKE", KEY_A);
        values.put(KeyHelper.TEST_RESOURCE_KEY, KEY_B);
        assertEquals(KEY_A, KeyHelper.requireResourceKey(values::get,
            Collections.singletonList(KeyHelper.TEST_RESOURCE_KEY),
            "whether a device is mobile", "device"));
    }

    @Test
    public void requireResourceKey_ExampleNameAfterSharedNames() {
        Map<String, String> values = new HashMap<>();
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
