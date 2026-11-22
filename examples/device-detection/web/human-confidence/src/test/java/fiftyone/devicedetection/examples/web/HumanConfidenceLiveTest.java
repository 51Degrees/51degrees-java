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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * The passive call the example makes, against the live cloud service with
 * keys from the environment. The request names HumanConfidence and
 * HumanConfidenceSource, so the answer carries both whatever the key's own
 * property list holds. A test with no key is skipped and names the
 * variable it needs. Keys are never printed.
 */
public class HumanConfidenceLiveTest {

    /** A Resource Key with the Bespoke product, which gets the source. */
    private static final String BESPOKE_KEY = "_51DEGREES_RESOURCE_KEY_BESPOKE";

    /** A Resource Key outside the Bespoke product, which gets no source. */
    private static final String BASIC_KEY = "_51DEGREES_RESOURCE_KEY_BASIC";

    private static final String ENDPOINT = "_51DEGREES_CLOUD_ENDPOINT";

    private static final String VALUES =
            "derived.humanconfidence,derived.humanconfidencesource";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36";

    private static final List<String> LEVELS =
            Arrays.asList("High", "Medium", "Low");

    private static final int TIMEOUT_MS = 20000;

    @Test
    public void bespokeKeyGetsSourcePassive() throws IOException {
        String key = key(BESPOKE_KEY);
        JSONObject derived = passive(key);
        String text = redact(derived.toString(), key);
        assertEquals("A request with no browser tests is passive: " + text,
                "Passive", derived.optString("humanconfidencesource", null));
        assertTrue("A passive level is one of the plain levels: " + text,
                LEVELS.contains(derived.optString("humanconfidence", null)));
    }

    @Test
    public void keyOutsideBespokeGetsNoSource() throws IOException {
        String key = key(BASIC_KEY);
        JSONObject derived = passive(key);
        assertNull("A key outside Bespoke gets no source: "
                        + redact(derived.toString(), key),
                derived.optString("humanconfidencesource", null));
    }

    /** The key in the named variable, or a skip that names it. */
    private static String key(String name) {
        String key = System.getenv(name);
        assumeTrue(name + " is not set, so this live cloud test is "
                        + "skipped. " + ENDPOINT + " names the service.",
                key != null && key.trim().isEmpty() == false);
        return key.trim();
    }

    /** The API base the example would call, ending in one slash. */
    private static String api() {
        String endpoint = System.getenv(ENDPOINT);
        if (endpoint == null || endpoint.trim().isEmpty()) {
            endpoint = HumanConfidence.DEFAULT_ENDPOINT;
        }
        return endpoint.trim().replaceAll("/+$", "") + "/";
    }

    /**
     * Posts a browser's User-Agent to the key's JSON path, naming the two
     * properties, and returns the answer's derived object, empty when the
     * answer has none.
     */
    private static JSONObject passive(String key) throws IOException {
        String form = "user-agent=" + encode(USER_AGENT)
                + "&values=" + encode(VALUES);
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
        assertEquals("The passive call did not succeed: "
                + redact(body, key), 200, status);
        JSONObject derived = new JSONObject(body).optJSONObject("derived");
        return derived == null ? new JSONObject() : derived;
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
