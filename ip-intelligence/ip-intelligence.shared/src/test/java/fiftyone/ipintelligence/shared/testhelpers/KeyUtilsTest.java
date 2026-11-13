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

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class KeyUtilsTest {
    @Test
    public void testGetEnv(){
        assertNotNull(KeyUtils.getNamedKey("path"));
        System.out.println(KeyUtils.getNamedKey("path"));
    }

    private static final String NEW_KEY_NAME = "_51DEGREES_RESOURCE_KEY";
    private static final String OLD_KEY_NAME = "51DEGREES_RESOURCE_KEY";

    /**
     * System properties stand in for the environment variables, because
     * the lookup reads either and a test cannot set an environment
     * variable.
     */
    @Test
    public void oldKeyNameIsReadWhenNewIsNotSet() {
        assumeKeyNamesNotInEnvironment();
        String savedNew = System.clearProperty(NEW_KEY_NAME);
        String savedOld = System.clearProperty(OLD_KEY_NAME);
        try {
            System.setProperty(OLD_KEY_NAME, "old-name-key");
            assertEquals("old-name-key",
                KeyUtils.getResourceKey("NoSuchLegacyName"));
        } finally {
            restore(NEW_KEY_NAME, savedNew);
            restore(OLD_KEY_NAME, savedOld);
        }
    }

    @Test
    public void newKeyNameWinsOverOld() {
        assumeKeyNamesNotInEnvironment();
        String savedNew = System.clearProperty(NEW_KEY_NAME);
        String savedOld = System.clearProperty(OLD_KEY_NAME);
        try {
            System.setProperty(NEW_KEY_NAME, "new-name-key");
            System.setProperty(OLD_KEY_NAME, "old-name-key");
            assertEquals("new-name-key",
                KeyUtils.getResourceKey("NoSuchLegacyName"));
        } finally {
            restore(NEW_KEY_NAME, savedNew);
            restore(OLD_KEY_NAME, savedOld);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void oldKeyConstantKeepsItsValue() throws Exception {
        assertEquals(OLD_KEY_NAME, KeyUtils.RESOURCE_KEY_ENV_VAR);
        assertTrue(KeyUtils.class.getField("RESOURCE_KEY_ENV_VAR")
            .isAnnotationPresent(Deprecated.class));
        assertEquals(NEW_KEY_NAME,
            KeyUtils.class.getField("RESOURCE_KEY_VARIABLE").get(null));
    }

    private static void assumeKeyNamesNotInEnvironment() {
        assumeTrue(System.getenv(NEW_KEY_NAME) == null);
        assumeTrue(System.getenv(OLD_KEY_NAME) == null);
    }

    private static void restore(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
