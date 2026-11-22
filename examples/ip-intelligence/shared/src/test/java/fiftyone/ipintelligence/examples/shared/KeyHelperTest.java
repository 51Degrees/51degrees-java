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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
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

    /**
     * The name getOrSetTestResourceKey reads.
     */
    private static final String EXAMPLE_NAME =
        KeyHelper.RESOURCE_KEY_VARIABLE;

    /**
     * With no key anywhere, the lookup logs that there is none and stores
     * nothing, rather than passing null to System.setProperty.
     */
    @Test
    public void getOrSetTestResourceKey_NoKey_StoresNothing() {
        // A test can clear a system property but not an environment
        // variable.
        assumeTrue(EXAMPLE_NAME + " is set in the environment",
            System.getenv(EXAMPLE_NAME) == null);
        String saved = System.clearProperty(EXAMPLE_NAME);
        try {
            assertNull(KeyHelper.getOrSetTestResourceKey(false));
            assertNull(System.getProperty(EXAMPLE_NAME));
        } finally {
            if (saved != null) {
                System.setProperty(EXAMPLE_NAME, saved);
            }
        }
    }

    /**
     * A key is read from the environment under the name given, and from
     * the system property of that name when the environment has none.
     */
    @Test
    public void readKey_ReadsTheEnvironmentThenTheSystemProperty() {
        String name = "Key_Helper_Test_Name";
        Map<String, String> environment = new HashMap<>();
        environment.put("KEY_HELPER_TEST_NAME", KEY_A);
        assertNull(KeyHelper.readKey(name, environment::get));
        System.setProperty(name, KEY_B);
        try {
            assertEquals(KEY_B, KeyHelper.readKey(name, environment::get));
            environment.put(name, KEY_A);
            assertEquals(KEY_A, KeyHelper.readKey(name, environment::get));
        } finally {
            System.clearProperty(name);
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
                KeyHelper.requireResourceKey(name -> null, EXAMPLE_NAME,
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
     * Only the name the test gives is read, and a name holding something
     * that is not a key, such as an empty string, is passed over.
     */
    @Test
    public void requireResourceKey_ReadsOnlyTheNameGiven() {
        Map<String, String> values = new HashMap<>();
        values.put(NEW_KEY_NAME, "");
        values.put("_51DEGREES_RESOURCE_KEY_BESPOKE", KEY_A);
        assertThrows(AssumptionViolatedException.class, () ->
            KeyHelper.requireResourceKey(values::get, EXAMPLE_NAME,
                "every IP Intelligence property", "ip"));
        values.put(NEW_KEY_NAME, KEY_B);
        assertEquals(KEY_B, KeyHelper.requireResourceKey(values::get,
            EXAMPLE_NAME, "every IP Intelligence property", "ip"));
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

    /**
     * A cloud example runs as an application, with no test library on its
     * class path, and reads its key with KeyHelper. So KeyHelper has to
     * load, and find a key, where JUnit cannot be found.
     */
    @Test
    public void findResourceKey_NoJUnitOnTheClassPath_FindsTheKey()
        throws Exception {
        try (URLClassLoader loader = withoutJUnit()) {
            Class<?> helper = Class.forName(KeyHelper.class.getName(), true,
                loader);
            assertNotSame(KeyHelper.class, helper);
            String key = (String) helper
                .getMethod("findResourceKey", String.class)
                .invoke(null, KEY_A);
            assertEquals(KEY_A, key);
        }
    }

    /**
     * A class loader with KeyHelper's own class files, that cannot see
     * JUnit.
     */
    private static URLClassLoader withoutJUnit() {
        String helpers = KeyHelper.class.getPackage().getName() + ".";
        ClassLoader parent = new ClassLoader(
            KeyHelper.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
                if (name.startsWith("org.junit.") ||
                    name.startsWith(helpers)) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        };
        return new URLClassLoader(new URL[]{KeyHelper.class
            .getProtectionDomain().getCodeSource().getLocation()}, parent);
    }
}
