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

package fiftyone.ipintelligence.shared.testhelpers;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class KeyUtilsTest {
    private static final String NAME = "Key_Utils_Test_Name";

    /**
     * An environment that holds a value under the upper case form of
     * {@link #NAME} only.
     */
    private static final Map<String, String> UPPER_CASE_ONLY =
        Collections.singletonMap("KEY_UTILS_TEST_NAME", "from-environment");

    @Test
    public void getNamedKey_ReadsTheEnvironmentUnderTheNameGiven() {
        assertEquals("from-environment", KeyUtils.getNamedKey(
            "KEY_UTILS_TEST_NAME", UPPER_CASE_ONLY::get));
        assertNull(KeyUtils.getNamedKey(NAME, UPPER_CASE_ONLY::get));
    }

    @Test
    public void getNamedKey_NotInTheEnvironment_ReadsTheSystemProperty() {
        System.setProperty(NAME, "from-property");
        try {
            assertEquals("from-property",
                KeyUtils.getNamedKey(NAME, UPPER_CASE_ONLY::get));
        } finally {
            System.clearProperty(NAME);
        }
    }

    private static final String KEY_NAME = "_51DEGREES_RESOURCE_KEY";

    /**
     * System properties stand in for the environment variables, because
     * the lookup reads either and a test cannot set an environment
     * variable.
     */
    @Test
    public void keyNameIsRead() {
        assumeTrue(System.getenv(KEY_NAME) == null);
        String saved = System.clearProperty(KEY_NAME);
        try {
            System.setProperty(KEY_NAME, "name-key");
            assertEquals("name-key",
                KeyUtils.getResourceKey());
        } finally {
            restore(KEY_NAME, saved);
        }
    }

    private static void restore(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
