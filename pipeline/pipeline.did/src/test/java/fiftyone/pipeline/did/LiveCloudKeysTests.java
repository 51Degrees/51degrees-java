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

package fiftyone.pipeline.did;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

/**
 * Tests which names {@link LiveCloudKeys} reads a key from. System
 * properties stand in for the environment variables, because the lookup
 * reads either and a test cannot set an environment variable.
 */
public class LiveCloudKeysTests {

    private static final String RESOURCE_KEY_NAME = "_51DEGREES_RESOURCE_KEY";
    private static final String OLD_RESOURCE_KEY_NAME = "RESOURCE_KEY";
    private static final String LICENCE_KEY_NAME = "_51DEGREES_LICENSE_KEY";
    private static final String OLD_LICENCE_KEY_NAME = "LICENSE_KEY";
    private static final String OTHER_NAME = "TestResourceKey";

    private static final String[] NAMES = {
        RESOURCE_KEY_NAME, OLD_RESOURCE_KEY_NAME,
        LICENCE_KEY_NAME, OLD_LICENCE_KEY_NAME, OTHER_NAME,
    };

    private final String[] saved = new String[NAMES.length];

    @Before
    public void clearNames() {
        for (int i = 0; i < NAMES.length; i++) {
            // An environment variable would answer before the system
            // properties set here, so the test cannot run.
            assumeTrue(System.getenv(NAMES[i]) == null);
            saved[i] = System.clearProperty(NAMES[i]);
        }
    }

    @After
    public void restoreNames() {
        for (int i = 0; i < NAMES.length; i++) {
            if (saved[i] == null) {
                System.clearProperty(NAMES[i]);
            } else {
                System.setProperty(NAMES[i], saved[i]);
            }
        }
    }

    @Test
    public void resourceKey_OldNameWhenNewIsNotSet() {
        System.setProperty(OLD_RESOURCE_KEY_NAME, "old-name-key");

        assertEquals("old-name-key",
            LiveCloudKeys.find(new String[] {RESOURCE_KEY_NAME}));
    }

    @Test
    public void licenceKey_OldNameWhenNewIsNotSet() {
        System.setProperty(OLD_LICENCE_KEY_NAME, "old-name-licence");

        assertEquals("old-name-licence",
            LiveCloudKeys.find(new String[] {LICENCE_KEY_NAME}));
    }

    @Test
    public void newNameWinsOverOld() {
        System.setProperty(RESOURCE_KEY_NAME, "new-name-key");
        System.setProperty(OLD_RESOURCE_KEY_NAME, "old-name-key");

        assertEquals("new-name-key",
            LiveCloudKeys.find(new String[] {RESOURCE_KEY_NAME}));
    }

    @Test
    public void oldNameIsReadOnlyForItsNewName() {
        System.setProperty(OLD_RESOURCE_KEY_NAME, "old-name-key");

        assertNull(LiveCloudKeys.find(new String[] {OTHER_NAME}));
    }
}
