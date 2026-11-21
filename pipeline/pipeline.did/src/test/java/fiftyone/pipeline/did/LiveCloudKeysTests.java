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

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * Each key the live 51Did tests use is read under its one _51DEGREES_
 * name, and no other name is read in its place.
 */
public class LiveCloudKeysTests {

    @Test
    public void resourceKey_ReadUnderItsOwnNameOnly() {
        assertArrayEquals(new String[]{"_51DEGREES_RESOURCE_KEY"},
            LiveCloudKeys.RESOURCE_KEY_NAMES);
    }

    /**
     * A sealed verdict is redeemed with the 51Did license.
     * _51DEGREES_LICENSE_KEY holds the data file license, so it is not
     * read here.
     */
    @Test
    public void licenceKey_IsThe51DidLicence() {
        assertArrayEquals(new String[]{"_51DEGREES_LICENSE_KEY_51DID"},
            LiveCloudKeys.LICENCE_KEY_NAMES);
    }

    @Test
    public void noResourceKey_NamesTheVariable() {
        assertTrue(LiveCloudKeys.noResourceKey(),
            LiveCloudKeys.noResourceKey().contains(
                "_51DEGREES_RESOURCE_KEY is not set"));
    }
}
