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
 * Runs the example against the live cloud service and asks its /passive path
 * as the page does. The service is the one _51DEGREES_CLOUD_ENDPOINT names,
 * or production.
 * <p>
 * Each test reads its own key. The key in _51DEGREES_RESOURCE_KEY_BESPOKE has
 * products that offer HumanConfidenceSource, so its answer carries the source
 * Passive and a level. The key in _51DEGREES_RESOURCE_KEY_BASIC has products
 * that don't, so its answer carries no source. The example is given the key
 * the test checks, as a person running it would set it in
 * _51DEGREES_RESOURCE_KEY. A test is skipped when its key isn't set, naming
 * the variable, and fails when the answer lacks what it checks. Keys are
 * never printed.
 */
public class HumanConfidenceLiveTest {

    /** The key whose products offer HumanConfidenceSource. */
    private static final String BESPOKE_KEY =
            "_51DEGREES_RESOURCE_KEY_BESPOKE";

    /** The key whose products don't offer HumanConfidenceSource. */
    private static final String BASIC_KEY = "_51DEGREES_RESOURCE_KEY_BASIC";

    private static final String ENDPOINT = "_51DEGREES_CLOUD_ENDPOINT";

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
    public void bespokeKeysPassiveAnswerCarriesTheSourcePassive()
            throws IOException {
        String key = key(BESPOKE_KEY);
        JSONObject served = servedSource(key);
        JSONObject answer = passive(key);
        String text = redact(answer.toString(), key);

        assertFalse("The example's passive call failed: " + text,
                answer.has("error"));
        assertEquals("This key's answer carries the source " + PASSIVE
                        + ": " + text + ". The service says: "
                        + redact(served.toString(), key),
                PASSIVE, answer.optString("source", null));
        assertTrue("A passive level is one of the plain levels: " + text,
                LEVELS.contains(answer.optString("humanConfidence", null)));
    }

    @Test
    public void basicKeysPassiveAnswerCarriesNoSource() throws IOException {
        String key = key(BASIC_KEY);
        JSONObject served = servedSource(key);
        JSONObject answer = passive(key);
        String text = redact(answer.toString(), key);

        assertFalse("The example's passive call failed: " + text,
                answer.has("error"));
        assertFalse("This key's answer carries no source: " + text
                        + ". The service says: "
                        + redact(served.toString(), key),
                answer.has("source"));
    }

    /** The key in the variable name, or a skip that names it. */
    private static String key(String name) {
        String key = System.getenv(name);
        assumeTrue(name + " is not set, so this live cloud test is "
                        + "skipped. " + ENDPOINT + " names the service.",
                key != null && key.trim().isEmpty() == false);
        return key.trim();
    }

    /** Starts the example with the key and asks it for /passive. */
    private JSONObject passive(String key) throws IOException {
        example = new HumanConfidence(api(), key, null);
        return passive(example.start(0));
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
     * the source's value and null reason from the answer, for a failure to
     * quote.
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
