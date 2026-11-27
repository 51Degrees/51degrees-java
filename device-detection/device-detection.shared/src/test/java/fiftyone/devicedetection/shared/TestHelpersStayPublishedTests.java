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

package fiftyone.devicedetection.shared;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * FileUtils and KeyUtils, which earlier releases published, stay in the
 * published jar, deprecated, as a patch release removes no public name.
 */
public class TestHelpersStayPublishedTests {

    /**
     * FileUtils is in the main jar, and deprecated.
     * @deprecated tests the deprecated FileUtils.
     */
    @Deprecated
    @Test
    public void FileUtils_InTheMainJar_Deprecated() {
        assertPublishedAndDeprecated(
            fiftyone.devicedetection.shared.testhelpers.FileUtils.class);
    }

    /**
     * KeyUtils is in the main jar, and deprecated.
     * @deprecated tests the deprecated KeyUtils.
     */
    @Deprecated
    @Test
    public void KeyUtils_InTheMainJar_Deprecated() {
        assertPublishedAndDeprecated(
            fiftyone.devicedetection.shared.testhelpers.KeyUtils.class);
    }

    private static void assertPublishedAndDeprecated(Class<?> type) {
        String name = type.getName();
        String location = type.getProtectionDomain()
            .getCodeSource().getLocation().toString();
        assertFalse(name + " comes from " + location + ", not the main jar",
            location.contains("test-classes") ||
                location.endsWith("-tests.jar"));
        assertTrue(name + " should be deprecated",
            type.isAnnotationPresent(Deprecated.class));
    }
}
