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

import fiftyone.devicedetection.examples.shared.DataFileHelper;
import fiftyone.devicedetection.examples.shared.FindOutMore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Runs each on-premise console example's main with no arguments and no
 * licence key, as someone who has just cloned the repository runs it, and
 * checks that it says what the Enterprise data file adds, with the link to
 * the contact page. The examples default to the free Lite data file. CI
 * gives the other tests the Enterprise data files, so nothing else tests
 * what a fresh clone shows.
 */
public class ExamplesWithoutSettingsTest {

    private static final String CONTACT_US = "https://51degrees.com/contact-us?";

    private static final String LICENSE_KEY =
            UpdateDataFile.UPDATE_EXAMPLE_LICENSE_KEY_NAME;

    /**
     * An example's main method.
     */
    private interface Main {
        void main(String[] args) throws Exception;
    }

    /**
     * Runs the example's main with no arguments and the licence key system
     * property removed, and returns what it wrote to standard output.
     */
    private static String runWithoutSettings(Main main) throws Exception {
        assumeTrue("Skipping, a licence key is set in the environment",
                System.getenv(LICENSE_KEY) == null &&
                        System.getenv(LICENSE_KEY.toUpperCase()) == null);
        String licenseKey = System.clearProperty(LICENSE_KEY);
        PrintStream out = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, "UTF-8"));
        try {
            main.main(new String[0]);
        } finally {
            System.setOut(out);
            if (licenseKey != null) {
                System.setProperty(LICENSE_KEY, licenseKey);
            }
        }
        String output = buffer.toString("UTF-8");
        out.println(output);
        assertFindOutMore(output);
        return output;
    }

    private static void assertContains(String output, String expected) {
        assertTrue("The output should contain '" + expected + "'",
                output.contains(expected));
    }

    /**
     * Checks that the output has the "Find out more" section after the last
     * link to the contact page, and that the section closes with its rule.
     * Log lines may still follow the section.
     */
    private static void assertFindOutMore(String output) {
        int heading = output.lastIndexOf(FindOutMore.HEADING);
        assertTrue("The output should have the '" + FindOutMore.HEADING +
                "' section after the link to the contact page",
                heading > output.lastIndexOf(CONTACT_US));
        assertTrue("The '" + FindOutMore.HEADING +
                "' section should end with its rule",
                output.indexOf(FindOutMore.RULE, heading) > heading);
    }

    private static void assertOffer(String output) {
        assertContains(output, "This example used the free Lite data file.");
        assertContains(output, CONTACT_US);
    }

    @Test
    public void gettingStarted() throws Exception {
        String output = runWithoutSettings(GettingStartedOnPrem::main);
        assertContains(output, "Hardware Vendor: " + DataFileHelper.NEEDS_ENTERPRISE);
        assertContains(output, "Some of these properties need the Enterprise data file.");
        assertContains(output, CONTACT_US);
    }

    @Test
    public void matchMetrics() throws Exception {
        assertOffer(runWithoutSettings(MatchMetrics::main));
    }

    @Test
    public void metadata() throws Exception {
        assertOffer(runWithoutSettings(MetadataOnPrem::main));
    }

    @Test
    public void offlineProcessing() throws Exception {
        String output = runWithoutSettings(OfflineProcessing::main);
        assertContains(output, "device.hardwarevendor: " + DataFileHelper.NEEDS_ENTERPRISE);
        assertContains(output, "# Some of these properties need the Enterprise " +
                "data file. Contact us for a copy at " + CONTACT_US);
    }

    @Test
    public void performance() throws Exception {
        assertOffer(runWithoutSettings(PerformanceBenchmark::main));
    }

    @Test
    public void updateDataFile() throws Exception {
        String output = runWithoutSettings(UpdateDataFile::main);
        assertContains(output, "The data file in use is the 'Lite' file");
        assertContains(output, "which needs a licence key");
        assertContains(output, CONTACT_US);
    }
}
