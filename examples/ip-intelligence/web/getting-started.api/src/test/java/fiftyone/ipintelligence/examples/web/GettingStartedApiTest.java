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
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class GettingStartedApiTest {
    private static Server SERVER;
    private static int PORT;

    @BeforeClass
    public static void startJetty() throws Exception {
        // Use the enterprise data file when present, otherwise fall back to
        // the free Lite or ASN file so the test can run from a fresh clone.
        String dataFilePath = DataFileHelper.findAvailableDataFile();
        assumeTrue("Skipping test, no IP Intelligence data file found",
                dataFilePath != null);

        Map<String, String> initParams = new HashMap<>();
        initParams.put(GettingStartedApi.DATA_FILE_INIT_PARAM, dataFilePath);
        // Bind an OS-assigned ephemeral port (0) to avoid intermittent
        // "Address already in use" failures from a fixed port not yet released.
        SERVER = EmbedJetty.startServlet("/*", 0, GettingStartedApi.class, initParams);
        PORT = EmbedJetty.boundPort(SERVER);
    }

    private static String get(String path, int expectedCode) throws Exception {
        HttpURLConnection connection = (HttpURLConnection)
                new URL("http://localhost:" + PORT + path).openConnection();
        int code = connection.getResponseCode();
        InputStream response = code >= 200 && code < 400 ?
                connection.getInputStream() : connection.getErrorStream();
        String responseBody = "";
        if (response != null) {
            try (Scanner scanner = new Scanner(response)) {
                scanner.useDelimiter("\\A");
                if (scanner.hasNext()) {
                    responseBody = scanner.next();
                }
            }
        }
        connection.disconnect();
        assertEquals("Unexpected response code for " + path, expectedCode, code);
        return responseBody;
    }

    @Test
    public void testEvidenceKeys() throws Exception {
        String body = get("/evidencekeys", 200);
        JSONArray keys = new JSONArray(body);
        assertTrue("There should be at least one evidence key", keys.length() > 0);
    }

    @Test
    public void testAccessibleProperties() throws Exception {
        String body = get("/accessibleproperties", 200);
        JSONObject result = new JSONObject(body);
        JSONObject products = result.getJSONObject("Products");
        assertTrue("There should be at least one product", products.length() > 0);
    }

    @Test
    public void testJson() throws Exception {
        String body = get("/json?client-ip=8.8.8.8", 200);
        JSONObject result = new JSONObject(body);
        assertFalse("The JSON response should not be empty", result.isEmpty());
    }

    @Test
    public void testUnknownPathReturns404() throws Exception {
        get("/nosuchendpoint", 404);
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        EmbedJetty.stopAndJoin(SERVER);
    }
}
