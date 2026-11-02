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

package fiftyone.ipintelligence.examples.web;

import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class GettingStartedWebMixedTest {
    private static Server SERVER;
    private static int PORT;

    /**
     * Resolve the device detection data file. It is not part of this
     * repository and is not fetched by CI, so the test is skipped when it
     * is absent.
     */
    static String resolveDdDataFile() {
        Path relative = Paths.get(GettingStartedWebMixed.DD_DATA_FILE_DEFAULT);
        if (Files.exists(relative)) {
            return relative.toAbsolutePath().toString();
        }
        try {
            return getFilePath(GettingStartedWebMixed.DD_DATA_FILE_DEFAULT)
                    .getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    @BeforeClass
    public static void startJetty() throws Exception {
        String ddDataFilePath = resolveDdDataFile();
        assumeTrue("Skipping test, no device detection data file found at " +
                        GettingStartedWebMixed.DD_DATA_FILE_DEFAULT,
                ddDataFilePath != null);
        System.setProperty("TestDeviceDetectionDataFile", ddDataFilePath);

        // Use the enterprise data file when present, otherwise fall back to
        // the free Lite or ASN file so the test can run from a fresh clone.
        String ipiDataFilePath = DataFileHelper.findAvailableDataFile();
        assumeTrue("Skipping test, no IP Intelligence data file found",
                ipiDataFilePath != null);
        System.setProperty("TestDataFile", ipiDataFilePath);

        // Bind an OS-assigned ephemeral port (0) to avoid intermittent
        // "Address already in use" failures from a fixed port not yet released.
        SERVER = EmbedJetty.startWebApp(
                GettingStartedWebMixed.getResourceBase(), 0);
        PORT = EmbedJetty.boundPort(SERVER);
    }

    @Test
    public void testWebMixed() throws Exception {

        HttpURLConnection connection =
                (HttpURLConnection) new URL("http://localhost:" + PORT + "/").openConnection();

        int code = connection.getResponseCode();

        InputStream response;
        if (code >= 200 && code < 400) {
            response = connection.getInputStream();
        } else {
            response = connection.getErrorStream();
        }

        String responseBody = "";
        if (response != null) {
            try (Scanner scanner = new Scanner(response)) {
                scanner.useDelimiter("\\A");
                if (scanner.hasNext()) {
                    responseBody = scanner.next();
                }
            }
        }

        System.out.println("Response code: " + code);
        System.out.println("Response body length: " + responseBody.length());

        connection.disconnect();

        assertEquals("Expected HTTP 200 OK response", 200, code);
        assertTrue("Response should not be empty", responseBody.length() > 0);
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        EmbedJetty.stopAndJoin(SERVER);
    }
}
