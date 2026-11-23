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

import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Runs the example against the live cloud service, with the Resource Key in
 * _51DEGREES_RESOURCE_KEY, and asks its /passive path as the page does. The
 * service is the one _51DEGREES_CLOUD_ENDPOINT names, or production.
 * <p>
 * The system property humanConfidenceSource says what the key's answer
 * carries. Passive, the default, is for a key whose products offer
 * HumanConfidenceSource, and none is for a key whose products don't. The
 * test is skipped when the key isn't set, and when the service doesn't
 * serve HumanConfidenceSource at all. Keys are never printed.
 */
public class HumanConfidenceLiveTest {

    private static final String RESOURCE_KEY = "_51DEGREES_RESOURCE_KEY";

    private static final String ENDPOINT = "_51DEGREES_CLOUD_ENDPOINT";

    /** The system property naming the source the key's answer carries. */
    private static final String EXPECTED_SOURCE = "humanConfidenceSource";

    /** The expected source for a key whose products don't offer it. */
    private static final String NONE = "none";

    private static final String PASSIVE = "Passive";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36";

    private static final List<String> LEVELS =
            Arrays.asList("High", "Medium", "Low");

    private static final int TIMEOUT_MS = 20000;

    private HumanConfidence example;

    @After
    public void stop() {
        if (example != null) {
            example.stop();
        }
    }

    @Test
    public void passiveAnswerCarriesTheKeysSource() throws IOException {
        String expected = expectedSource();
        String key = key();
        JSONObject served = servedSource(key);
        example = new HumanConfidence(api(), key, null);
        int port = example.start(0);
        JSONObject answer = passive(port);
        String text = redact(answer.toString(), key);

        assertFalse("The example's passive call failed: " + text,
                answer.has("error"));
        if (NONE.equals(expected)) {
            assertFalse("This key's answer carries no source: " + text
                            + ". The service says: "
                            + redact(served.toString(), key),
                    answer.has("source"));
        } else {
            assertEquals("This key's answer carries the source " + PASSIVE
                            + ": " + text + ". The service says: "
                            + redact(served.toString(), key),
                    PASSIVE, answer.optString("source", null));
            assertTrue("A passive level is one of the plain levels: "
                            + text,
                    LEVELS.contains(answer.optString(
                            "humanConfidence", null)));
        }
    }

    /** Passive or none, from the system property, or a failure. */
    private static String expectedSource() {
        String expected = System.getProperty(EXPECTED_SOURCE, PASSIVE).trim();
        assertTrue(EXPECTED_SOURCE + " is " + PASSIVE + " or " + NONE
                        + ", not " + expected,
                PASSIVE.equals(expected) || NONE.equals(expected));
        return expected;
    }

    /** The key in _51DEGREES_RESOURCE_KEY, or a skip that names it. */
    private static String key() {
        String key = System.getenv(RESOURCE_KEY);
        assumeTrue(RESOURCE_KEY + " is not set, so this live cloud test is "
                        + "skipped. " + ENDPOINT + " names the service.",
                key != null && key.trim().isEmpty() == false);
        return key.trim();
    }

    /** The API base the example calls, ending in one slash. */
    private static String api() {
        String endpoint = System.getenv(ENDPOINT);
        if (endpoint == null || endpoint.trim().isEmpty()) {
            endpoint = HumanConfidence.DEFAULT_ENDPOINT;
        }
        return endpoint.trim().replaceAll("/+$", "") + "/";
    }

    /**
     * Asks the service for HumanConfidenceSource with the key, and returns
     * the source's value and null reason from the answer. Skips the test
     * when the answer has neither, as the service doesn't serve the
     * property, so no key can get it there.
     */
    private static JSONObject servedSource(String key) throws IOException {
        String form = "user-agent=" + encode(USER_AGENT)
                + "&values=" + encode("derived.humanconfidencesource");
        HttpURLConnection connection = (HttpURLConnection)
                new URL(api() + key + ".json").openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        try (OutputStream out = connection.getOutputStream()) {
            out.write(form.getBytes(StandardCharsets.UTF_8));
        }
        int status = connection.getResponseCode();
        String body = read(status >= 400
                ? connection.getErrorStream() : connection.getInputStream());
        assertEquals("The service didn't answer the key: "
                + redact(body, key), 200, status);
        JSONObject derived = new JSONObject(body).optJSONObject("derived");
        JSONObject served = new JSONObject();
        if (derived != null) {
            for (String name : new String[] {
                    "humanconfidencesource",
                    "humanconfidencesourcenullreason" }) {
                if (derived.has(name)) {
                    served.put(name, derived.get(name));
                }
            }
        }
        assumeTrue("The service " + ENDPOINT + " names, or production when "
                        + "it isn't set, doesn't serve HumanConfidenceSource, "
                        + "so this test is skipped.",
                served.length() > 0);
        return served;
    }

    /** The example's answer to a browser's GET of /passive. */
    private static JSONObject passive(int port) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)
                new URL("http://127.0.0.1:" + port + "/passive")
                        .openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        assertEquals(200, connection.getResponseCode());
        return new JSONObject(read(connection.getInputStream()));
    }

    private static String encode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = in.read(buffer)) > 0) {
            bytes.write(buffer, 0, count);
        }
        in.close();
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    /** The text with the key taken out, as an error can quote the URL. */
    private static String redact(String text, String key) {
        return text.replace(key, "<key>");
    }
}
