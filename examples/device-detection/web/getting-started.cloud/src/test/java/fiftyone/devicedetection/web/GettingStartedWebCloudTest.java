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

package fiftyone.devicedetection.web;

import fiftyone.devicedetection.examples.shared.FindOutMore;
import fiftyone.devicedetection.examples.shared.KeyHelper;
import fiftyone.devicedetection.examples.web.EmbedJetty;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fiftyone.devicedetection.examples.web.GettingStartedWebCloud.resourceBase;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Requests the example's page with a known User-Agent and checks that the
 * page shows what the cloud detected for it.
 */
public class GettingStartedWebCloudTest {
    /**
     * Chrome 98 on Windows 10.
     */
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36";

    /**
     * What the page shows in place of a value the cloud did not return,
     * followed by the reason.
     */
    private static final String UNKNOWN = "Unknown.";

    private static Server SERVER = null;

    @BeforeClass
    public static void startJetty() throws Exception {
        String resourceKey = KeyHelper.requireTestResourceKey(
            "the browser and platform of a known User-Agent", "device");
        // The example's configuration file reads the key from here.
        System.setProperty(KeyHelper.TEST_RESOURCE_KEY, resourceKey);
        SERVER = EmbedJetty.startWebApp(getFilePath(resourceBase).getAbsolutePath(), 8081);
    }

    @Test
    public void testWebCloud() throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL("http://localhost:8081/").openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);

        int code = connection.getResponseCode();
        String body = read(code < 400 ?
            connection.getInputStream() : connection.getErrorStream());
        connection.disconnect();

        assertEquals("The page answered " + code + " with " + body,
            200, code);
        assertTrue("The page does not say the cloud engine detected the " +
                "values it shows. " + body,
            body.contains("<strong>cloud device detection engine</strong>"));
        assertDetected(body, "Browser Name", "BrowserName", "Chrome");
        assertDetected(body, "Platform Name", "PlatformName", "Windows");
        assertTrue("The page should end with the '" + FindOutMore.HEADING +
                "' section. " + body, body.contains(
                        "<h2>" + FindOutMore.HEADING + "</h2>"));
    }

    /**
     * Checks the value in the page's row for a property, which is the
     * reason when the cloud returned no value.
     */
    private static void assertDetected(String body, String label,
                                       String property, String expected) {
        Matcher row = Pattern.compile(Pattern.quote(label +
            "</td><td class=\"c-eg-table__cell\">") + "(.*?)</td>")
            .matcher(body);
        assertTrue("The page has no " + label + " row. " + body, row.find());
        String value = row.group(1).trim();
        if (value.startsWith(UNKNOWN)) {
            String reason = value.substring(UNKNOWN.length()).trim();
            fail(KeyHelper.answeredWithout(property,
                "null".equals(reason) ? null : reason, "device"));
        }
        assertEquals(label + " on the page", expected, value);
    }

    private static String read(InputStream stream) {
        if (stream == null) {
            return "";
        }
        try (Scanner scanner = new Scanner(stream,
                StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        if (SERVER != null) {
            SERVER.stop();
        }
    }
}