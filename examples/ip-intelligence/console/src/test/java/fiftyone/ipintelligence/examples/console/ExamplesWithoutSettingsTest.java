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

package fiftyone.ipintelligence.examples.console;

import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Runs each on-premise console example's main with no arguments, no data
 * file setting and no licence key, as someone who has just cloned the
 * repository runs it, and checks that it says what the Enterprise data file
 * adds, with the link to the contact page. The examples default to the free
 * Lite data file, or the free Asn file for performance and updates. CI gives
 * the other tests the Enterprise data file, so nothing else tests what a
 * fresh clone shows.
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
     * Runs the example's main with no arguments and the data file and
     * licence key system properties removed, and returns what it wrote to
     * standard output. The deprecated name of the data file variable is
     * read when the current one is not set, so both are checked and
     * removed.
     */
    @SuppressWarnings("deprecation")
    private static String runWithoutSettings(Main main) throws Exception {
        assumeTrue("Skipping, a data file or licence key is set in the environment",
                System.getenv(DataFileHelper.IPI_PATH_VARIABLE) == null &&
                        System.getenv(DataFileHelper.IPI_PATH_ENV_VAR) == null &&
                        System.getenv(LICENSE_KEY) == null &&
                        System.getenv(LICENSE_KEY.toUpperCase()) == null);
        String dataFile = System.clearProperty(DataFileHelper.IPI_PATH_VARIABLE);
        String oldNameDataFile =
                System.clearProperty(DataFileHelper.IPI_PATH_ENV_VAR);
        String licenseKey = System.clearProperty(LICENSE_KEY);
        PrintStream out = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, "UTF-8"));
        try {
            main.main(new String[0]);
        } finally {
            System.setOut(out);
            if (dataFile != null) {
                System.setProperty(DataFileHelper.IPI_PATH_VARIABLE, dataFile);
            }
            if (oldNameDataFile != null) {
                System.setProperty(DataFileHelper.IPI_PATH_ENV_VAR,
                        oldNameDataFile);
            }
            if (licenseKey != null) {
                System.setProperty(LICENSE_KEY, licenseKey);
            }
        }
        String output = buffer.toString("UTF-8");
        out.println(output);
        return output;
    }

    private static void assertContains(String output, String expected) {
        assertTrue("The output should contain '" + expected + "'",
                output.contains(expected));
    }

    private static void assertStops(String output, String purpose) {
        assertContains(output, purpose + " needs the ");
        assertContains(output, "so the example stops here.");
        assertContains(output, CONTACT_US);
    }

    private static void assertOffer(String output) {
        assertContains(output, "This example used a free data file.");
        assertContains(output, CONTACT_US);
    }

    @Test
    public void gettingStarted() throws Exception {
        String output = runWithoutSettings(GettingStartedOnPrem::main);
        assertContains(output, "RegisteredName: ");
        assertContains(output, "Country: " + DataFileHelper.NEEDS_ENTERPRISE);
        assertContains(output, "Some of these properties need the Enterprise data file.");
        assertContains(output, CONTACT_US);
    }

    @Test
    public void suspicious() throws Exception {
        assertStops(runWithoutSettings(SuspiciousOnPrem::main),
                "Judging whether an IP address is suspicious");
    }

    @Test
    public void compare() throws Exception {
        assertStops(runWithoutSettings(CompareOnPrem::main),
                "Comparing known locations with IP Intelligence");
    }

    @Test
    public void metrics() throws Exception {
        assertStops(runWithoutSettings(MetricsOnPrem::main),
                "Measuring the location areas");
    }

    @Test
    public void offlineProcessing() throws Exception {
        String output = runWithoutSettings(OfflineProcessing::main);
        assertContains(output, "ip.Country: " + DataFileHelper.NEEDS_ENTERPRISE);
        assertContains(output, "# Some of these properties need the Enterprise " +
                "data file. Contact us for a copy at " + CONTACT_US);
    }

    @Test
    public void metadata() throws Exception {
        assertOffer(runWithoutSettings(MetadataOnPrem::main));
    }

    @Test
    public void performance() throws Exception {
        assertOffer(runWithoutSettings(PerformanceBenchmark::main));
    }

    @Test
    public void updateDataFile() throws Exception {
        String output = runWithoutSettings(UpdateDataFile::main);
        assertContains(output, "The data file in use is the ");
        assertContains(output, "which needs a licence key");
        assertContains(output, CONTACT_US);
    }

    @Test
    public void mixed() throws Exception {
        String output = runWithoutSettings(GettingStartedMixed::main);
        assertContains(output, "Hardware Vendor: " + DataFileHelper.NEEDS_ENTERPRISE);
        assertContains(output, "Country: " + DataFileHelper.NEEDS_ENTERPRISE);
        assertContains(output, CONTACT_US);
    }
}
