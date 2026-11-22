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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
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
            System.getenv(name) == null);
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
        assertNull(KeyHelper.getNamedKey(name, environment::get));
        System.setProperty(name, KEY_B);
        try {
            assertEquals(KEY_B, KeyHelper.readKey(name, environment::get));
            assertEquals(KEY_B,
                KeyHelper.getNamedKey(name, environment::get));
            environment.put(name, KEY_A);
            assertEquals(KEY_A, KeyHelper.readKey(name, environment::get));
            assertEquals(KEY_A,
                KeyHelper.getNamedKey(name, environment::get));
        } finally {
            System.clearProperty(name);
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
                    KeyHelper.TEST_RESOURCE_KEY,
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
    public void bespokeResourceKeyVariable_IsTheEveryPropertyName() {
        assertEquals("_51DEGREES_RESOURCE_KEY_BESPOKE",
            KeyHelper.BESPOKE_RESOURCE_KEY_VARIABLE);
    }

    @Test
    public void requireResourceKey_TwoProducts_NamesBoth() {
        AssumptionViolatedException skipped = assertThrows(
            AssumptionViolatedException.class, () ->
                KeyHelper.requireResourceKey(name -> null,
                    KeyHelper.BESPOKE_RESOURCE_KEY_VARIABLE,
                    "the devices that match a TAC", "hardware", "device"));
        assertTrue(skipped.getMessage(), skipped.getMessage().contains(
            "skipped because _51DEGREES_RESOURCE_KEY_BESPOKE is not set"));
        assertTrue(skipped.getMessage(), skipped.getMessage().contains(
            "carry the hardware and device products, because"));
    }

    /**
     * Only the name the test gives is read. A key under another name,
     * such as the one every cloud example reads, does not stand in for the
     * key with every property.
     */
    @Test
    public void requireResourceKey_ReadsOnlyTheNameGiven() {
        Map<String, String> values = new HashMap<>();
        values.put(KeyHelper.TEST_RESOURCE_KEY, KEY_A);
        assertThrows(AssumptionViolatedException.class, () ->
            KeyHelper.requireResourceKey(values::get,
                KeyHelper.BESPOKE_RESOURCE_KEY_VARIABLE,
                "the devices that match a TAC", "hardware", "device"));
    }

    /**
     * A name holding something that is not a key, such as an empty string,
     * is passed over.
     */
    @Test
    public void requireResourceKey_ValueThatIsNotAKey_IsPassedOver() {
        Map<String, String> values = new HashMap<>();
        values.put(KeyHelper.TEST_RESOURCE_KEY, "");
        assertThrows(AssumptionViolatedException.class, () ->
            KeyHelper.requireResourceKey(values::get,
                KeyHelper.TEST_RESOURCE_KEY,
                "whether a device is mobile", "device"));
        values.put(KeyHelper.TEST_RESOURCE_KEY, KEY_B);
        assertEquals(KEY_B, KeyHelper.requireResourceKey(values::get,
            KeyHelper.TEST_RESOURCE_KEY,
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

    /**
     * A test of a cloud example reads the key with every property. A key
     * under the name the example reads does not stand in for it.
     */
    @Test
    public void requireTestResourceKey_ReadsTheKeyWithEveryProperty() {
        String example = "_51DEGREES_RESOURCE_KEY";
        String bespoke = "_51DEGREES_RESOURCE_KEY_BESPOKE";
        // A test can set a system property but not an environment
        // variable.
        assumeTrue(System.getenv(example) == null &&
            System.getenv(bespoke) == null);
        String savedExample = System.getProperty(example);
        String savedBespoke = System.clearProperty(bespoke);
        try {
            System.setProperty(example, KEY_A);
            AssumptionViolatedException skipped = assertThrows(
                AssumptionViolatedException.class, () ->
                    KeyHelper.requireTestResourceKey(
                        "whether a device is mobile", "device"));
            assertTrue(skipped.getMessage(),
                skipped.getMessage().contains(bespoke + " is not set"));
            System.setProperty(bespoke, KEY_B);
            assertEquals(KEY_B, KeyHelper.requireTestResourceKey(
                "whether a device is mobile", "device"));
        } finally {
            putBack(example, savedExample);
            putBack(bespoke, savedBespoke);
        }
    }

    private static void putBack(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
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
                .getMethod("findResourceKey", String.class, String.class)
                .invoke(null, KEY_A, KeyHelper.TEST_RESOURCE_KEY);
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
