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

package fiftyone.devicedetection.examples.console;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Checks which lines the live tests of the TAC and native model examples
 * count as a device with a vendor and a model.
 */
public class HardwareLookupOutputTest {

    @Test
    public void deviceWithVendorAndModel() {
        assertTrue(HardwareLookupOutput.isDevice("Apple iPhone 6 (A1586)"));
        assertTrue(HardwareLookupOutput.isDevice(
            "Samsung Galaxy S10 (SC-03L)"));
    }

    @Test
    public void modelHoldingParentheses() {
        assertTrue(HardwareLookupOutput.isDevice(
            "Vendor Name (Model (2020))"));
    }

    @Test
    public void vendorOrModelWithNoValue() {
        assertFalse(HardwareLookupOutput.isDevice(
            "Unknown. No value was found. iPhone 6 (A1586)"));
        assertFalse(HardwareLookupOutput.isDevice(
            "Apple iPhone 6 (Unknown. No value (reason))"));
        assertFalse(HardwareLookupOutput.isDevice("Apple iPhone 6 ()"));
    }

    @Test
    public void otherForms() {
        assertFalse(HardwareLookupOutput.isDevice("Apple; iPhone 6; A1586"));
        assertFalse(HardwareLookupOutput.isDevice("Apple (A1586)"));
        assertFalse(HardwareLookupOutput.isDevice(
            "No devices are associated with this TAC."));
    }
}
