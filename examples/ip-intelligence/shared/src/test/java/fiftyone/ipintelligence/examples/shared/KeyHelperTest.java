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

import org.junit.AssumptionViolatedException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
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

    private static final String NEW_KEY_NAME = "_51DEGREES_RESOURCE_KEY";
    private static final String OLD_KEY_NAME = "51DEGREES_RESOURCE_KEY";

    /**
     * The names getOrSetTestResourceKey reads, in its order.
     */
    private static final List<String> EXAMPLE_NAMES =
        Collections.singletonList(KeyHelper.RESOURCE_KEY_VARIABLE);

    /**
     * With no key anywhere, the lookup logs that there is none and stores
     * nothing, rather than passing null to System.setProperty.
     */
    @Test
    public void getOrSetTestResourceKey_NoKey_StoresNothing() {
        List<String> namesRead = new ArrayList<>(EXAMPLE_NAMES);
        // A test can clear a system property but not an environment
        // variable.
        for (String name : namesRead) {
            assumeTrue(name + " is set in the environment",
                System.getenv(name) == null &&
                    System.getenv(name.toUpperCase()) == null);
        }
        Map<String, String> saved = new HashMap<>();
        for (String name : namesRead) {
            saved.put(name, System.clearProperty(name));
        }
        try {
            assertNull(KeyHelper.getOrSetTestResourceKey(false));
            for (String name : namesRead) {
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
    public void testResourceKey_IsTheSharedName() {
        assertEquals(NEW_KEY_NAME, KeyHelper.TEST_RESOURCE_KEY);
    }

    /**
     * A test with no key is skipped, naming the variable to set, rather
     * than failed.
     */
    @Test
    public void requireResourceKey_NoKey_SkipsNamingTheVariable() {
        AssumptionViolatedException skipped = assertThrows(
            AssumptionViolatedException.class, () ->
                KeyHelper.requireResourceKey(name -> null, EXAMPLE_NAMES,
                    "the network and location of an IP address, and " +
                        "whether a device is mobile",
                    "ip", "device"));
        assertEquals("This test calls the live 51Degrees cloud service, so " +
                "it needs a resource key, and it is skipped because " +
                "_51DEGREES_RESOURCE_KEY is not set as an environment " +
                "variable or as a system property. The key has to carry the " +
                "ip and device products, because the test asks the service " +
                "for the network and location of an IP address, and whether " +
                "a device is mobile. To see the products a key carries, open " +
                "https://cloud.51degrees.com/api/v4/accessibleproperties?resource=<key>. " +
                "Create a key at " + KeyHelper.CREATE_KEY_URL + ".",
            skipped.getMessage());
    }

    /**
     * Only the names the test gives are read, and a name holding something
     * that is not a key, such as the empty string a pom passes for a key
     * that is not set, is passed over.
     */
    @Test
    public void requireResourceKey_ReadsOnlyTheNamesGiven() {
        Map<String, String> values = new HashMap<>();
        values.put(NEW_KEY_NAME, "");
        values.put("_51DEGREES_RESOURCE_KEY_BESPOKE", KEY_A);
        assertThrows(AssumptionViolatedException.class, () ->
            KeyHelper.requireResourceKey(values::get, EXAMPLE_NAMES,
                "every IP Intelligence property", "ip"));
        values.put(NEW_KEY_NAME, KEY_B);
        assertEquals(KEY_B, KeyHelper.requireResourceKey(values::get,
            EXAMPLE_NAMES, "every IP Intelligence property", "ip"));
    }

    /**
     * A cloud example reads its key from _51DEGREES_RESOURCE_KEY alone.
     */
    @Test
    public void findResourceKey_ReadsOnlyTheSharedName() {
        assumeTrue(System.getenv(NEW_KEY_NAME) == null);
        String other = "AnyOtherResourceKeyName";
        String saved = System.clearProperty(NEW_KEY_NAME);
        try {
            System.setProperty(other, KEY_A);
            assertNull(KeyHelper.findResourceKey(null));
            System.setProperty(NEW_KEY_NAME, KEY_B);
            assertEquals(KEY_B, KeyHelper.findResourceKey(null));
        } finally {
            System.clearProperty(other);
            if (saved == null) {
                System.clearProperty(NEW_KEY_NAME);
            } else {
                System.setProperty(NEW_KEY_NAME, saved);
            }
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void oldKeyConstantKeepsItsValue() throws Exception {
        assertEquals(OLD_KEY_NAME, KeyHelper.RESOURCE_KEY_ENV_VAR);
        assertEquals(true, KeyHelper.class.getField("RESOURCE_KEY_ENV_VAR")
            .isAnnotationPresent(Deprecated.class));
        assertEquals(NEW_KEY_NAME,
            KeyHelper.class.getField("RESOURCE_KEY_VARIABLE").get(null));
    }
}
