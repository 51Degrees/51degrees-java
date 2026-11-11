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

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

public class KeyHelperTest {
    /**
     * Values of the form KeyUtils accepts as a key. Neither is a real key.
     */
    private static final String KEY_A = "AQAAAAAAAAAAAAAAAAAAAAAA";
    private static final String KEY_B = "AgAAAAAAAAAAAAAAAAAAAAAA";

    /**
     * The names getOrSetTestResourceKey reads, in its order.
     */
    private static final List<String> EXAMPLE_NAMES = Arrays.asList(
        KeyHelper.RESOURCE_KEY_ENV_VAR, KeyHelper.TEST_RESOURCE_KEY);

    /**
     * With no key anywhere, the lookup logs that there is none and stores
     * nothing, rather than passing null to System.setProperty.
     */
    @Test
    public void getOrSetTestResourceKey_NoKey_StoresNothing() {
        // A test can clear a system property but not an environment
        // variable.
        for (String name : EXAMPLE_NAMES) {
            assumeTrue(name + " is set in the environment",
                System.getenv(name) == null &&
                    System.getenv(name.toUpperCase()) == null);
        }
        Map<String, String> saved = new HashMap<>();
        for (String name : EXAMPLE_NAMES) {
            saved.put(name, System.clearProperty(name));
        }
        try {
            assertNull(KeyHelper.getOrSetTestResourceKey(false));
            for (String name : EXAMPLE_NAMES) {
                assertNull(name, System.getProperty(name));
            }
        } finally {
            for (Map.Entry<String, String> entry : saved.entrySet()) {
                if (entry.getValue() != null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    @Test
    public void requireResourceKey_NoKey_FailsNamingEveryName() {
        AssertionError error = assertThrows(AssertionError.class, () ->
            KeyHelper.requireResourceKey(name -> null, EXAMPLE_NAMES,
                "the network and location of an IP address, and whether a " +
                    "device is mobile",
                "ip", "device"));
        assertEquals("This test calls the live 51Degrees cloud service, so " +
                "it needs a resource key and there is nothing to fall back " +
                "on. None of _51DEGREES_RESOURCE_KEY, " +
                "_51DEGREES_RESOURCE_KEY_BESPOKE, _51DEGREES_RESOURCE_KEY, " +
                "TestResourceKey is set, as an environment variable or as a " +
                "system property, tried in that order. The key has to carry " +
                "the ip and device products, because the test asks the " +
                "service for the network and location of an IP address, and " +
                "whether a device is mobile. To see the products a key " +
                "carries, open " +
                "https://cloud.51degrees.com/api/v4/accessibleproperties?resource=<key>. " +
                "Create a key at " + KeyHelper.CREATE_KEY_URL + ".",
            error.getMessage());
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
            EXAMPLE_NAMES, "every IP Intelligence property", "ip"));
    }

    @Test
    public void requireResourceKey_ExampleNamesInTheirOrder() {
        Map<String, String> values = new HashMap<>();
        values.put(KeyHelper.RESOURCE_KEY_ENV_VAR, KEY_A);
        values.put(KeyHelper.TEST_RESOURCE_KEY, KEY_B);
        assertEquals(KEY_A, KeyHelper.requireResourceKey(values::get,
            EXAMPLE_NAMES, "every IP Intelligence property", "ip"));
        values.remove(KeyHelper.RESOURCE_KEY_ENV_VAR);
        assertEquals(KEY_B, KeyHelper.requireResourceKey(values::get,
            EXAMPLE_NAMES, "every IP Intelligence property", "ip"));
    }
}
