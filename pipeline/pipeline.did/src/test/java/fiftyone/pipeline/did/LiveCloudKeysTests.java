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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The live 51Did tests run once for each resource key, and each key is read
 * under its own _51DEGREES_ name and no other.
 */
public class LiveCloudKeysTests {

    /**
     * A name nothing else sets, for the tests of the read.
     */
    private static final String SAMPLE = "_51DEGREES_LIVE_CLOUD_KEYS_SAMPLE";

    /**
     * A sealed verdict is redeemed only with the license key of the account
     * the resource key belongs to. So the 51Did resource key runs with the
     * 51Did license key, and the Bespoke resource key runs with none.
     * _51DEGREES_LICENSE_KEY holds the data file license, so it is not read.
     */
    @Test
    public void passes_PairEachResourceKeyWithItsOwnLicenseOrNone() {
        assertEquals(2, LiveCloudKeys.PASSES.length);
        assertArrayEquals(new String[]{
            "_51DEGREES_RESOURCE_KEY_51DID",
            "_51DEGREES_LICENSE_KEY_51DID"}, LiveCloudKeys.PASSES[0]);
        assertArrayEquals(new String[]{
            "_51DEGREES_RESOURCE_KEY_BESPOKE",
            null}, LiveCloudKeys.PASSES[1]);
    }

    @Test
    public void read_GivesTheTrimmedSystemProperty() {
        System.setProperty(SAMPLE, " value ");
        try {
            assertEquals("value", LiveCloudKeys.read(SAMPLE));
        } finally {
            System.clearProperty(SAMPLE);
        }
    }

    @Test
    public void read_GivesNullForABlankValue() {
        System.setProperty(SAMPLE, "  ");
        try {
            assertNull(LiveCloudKeys.read(SAMPLE));
        } finally {
            System.clearProperty(SAMPLE);
        }
    }

    @Test
    public void read_GivesNullWhenNothingIsSet() {
        assertNull(LiveCloudKeys.read(SAMPLE));
    }

    @Test
    public void read_GivesNullForNoName() {
        assertNull(LiveCloudKeys.read(null));
    }

    @Test
    public void noResourceKey_NamesTheVariable() {
        String message = LiveCloudKeys.noResourceKey(SAMPLE);
        assertTrue(message, message.contains(SAMPLE + " is not set"));
    }

    /**
     * A license key with no resource key of its own account means a key is
     * missing, and the tests that redeem would never run.
     */
    @Test
    public void missingResourceKey_NamesBothVariables() {
        String message = LiveCloudKeys.missingResourceKey(
            SAMPLE, "_51DEGREES_LIVE_CLOUD_KEYS_LICENSE");
        assertTrue(message, message.contains(SAMPLE + " is not set"));
        assertTrue(message,
            message.contains("_51DEGREES_LIVE_CLOUD_KEYS_LICENSE"));
    }

    @Test
    public void noLicenseKey_NamesTheVariableToSet() {
        String message = LiveCloudKeys.noLicenseKey(SAMPLE);
        assertTrue(message, message.contains("Set " + SAMPLE));
    }

    @Test
    public void noLicenseKey_SaysWhenThePassHasNone() {
        String message = LiveCloudKeys.noLicenseKey(null);
        assertTrue(message, message.contains("no license key"));
    }
}
