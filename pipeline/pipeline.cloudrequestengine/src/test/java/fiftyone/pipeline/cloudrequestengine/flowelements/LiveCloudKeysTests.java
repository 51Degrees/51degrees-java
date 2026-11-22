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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each key the live cloud tests use is read under its one _51DEGREES_ name,
 * and no other name is read in its place.
 */
public class LiveCloudKeysTests {

    @Test
    public void resourceKey_ReadUnderItsOwnNameOnly() {
        assertEquals("_51DEGREES_RESOURCE_KEY",
            LiveCloudKeys.RESOURCE_KEY_NAME);
    }

    /**
     * The test on a license key alone needs a license for a cloud product.
     * _51DEGREES_LICENSE_KEY holds the data file license, so it is not
     * read here.
     */
    @Test
    public void licenceKey_IsTheCloudLicence() {
        assertEquals("_51DEGREES_LICENSE_KEY_BESPOKE",
            LiveCloudKeys.LICENCE_KEY_NAME);
    }

    @Test
    public void noResourceKey_NamesTheVariable() {
        assertTrue(LiveCloudKeys.noResourceKey().contains(
            "_51DEGREES_RESOURCE_KEY is not set"),
            LiveCloudKeys.noResourceKey());
    }
}
