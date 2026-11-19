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

package fiftyone.devicedetection.examples.web;

import fiftyone.devicedetection.examples.shared.FindOutMore;
import org.eclipse.jetty.server.Server;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Starts the on-premise web example as its main method does, with no
 * arguments and no data file set, as someone who has just cloned the
 * repository runs it, so that it uses the free Lite data file. The other
 * test of this example gives it the Enterprise data file when CI has one,
 * which does not show that the example works out of the box
 * (device-detection-specification/required-examples.md, "Those tests MUST
 * include a run of each example with no arguments or settings").
 */
public class WebOnPremWithoutSettingsTest {

    private static final int PORT = 8083;

    private static final String[] DATA_FILE_NAMES = {
            "TestDataFile", "_51DEGREES_DD_PATH"};

    @Test
    public void runsWithoutSettings() throws Exception {
        for (String name : DATA_FILE_NAMES) {
            assumeTrue("Skipping, " + name + " is set in the environment",
                    System.getenv(name) == null);
        }
        String saved = System.clearProperty("TestDataFile");
        Server server = GettingStartedWebOnPrem.start(PORT);
        try {
            HttpURLConnection connection = (HttpURLConnection)
                    new URL("http://localhost:" + PORT + "/").openConnection();
            String body;
            try (InputStream response = connection.getInputStream();
                 Scanner scanner = new Scanner(response, "UTF-8")) {
                body = scanner.useDelimiter("\\A").next();
            }
            int code = connection.getResponseCode();
            connection.disconnect();
            assertEquals(200, code);
            assertTrue("The page should say it used the free Lite data " +
                    "file. " + body, body.contains("Lite"));
            assertTrue("The page should end with the '" +
                    FindOutMore.HEADING + "' section. " + body,
                    body.contains("<h2>" + FindOutMore.HEADING + "</h2>"));
        } finally {
            server.stop();
            if (saved != null) {
                System.setProperty("TestDataFile", saved);
            }
        }
    }
}
