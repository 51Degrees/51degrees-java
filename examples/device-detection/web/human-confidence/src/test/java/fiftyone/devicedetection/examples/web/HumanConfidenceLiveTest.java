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

import org.json.JSONArray;
import org.json.JSONException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Runs the example against the staging cloud service that
 * CLOUD_STAGING_ENDPOINT names, and asks its /passive path as the page does.
 * The production cloud does not serve HumanConfidenceSource yet, so the
 * tests ask the staging cloud.
 * <p>
 * Each test reads its own key. The Bespoke key in
 * RESOURCE_KEY_BESPOKE_STAGING, which the staging cloud resolves, has
 * products that offer HumanConfidenceSource, so its answer carries the source
 * Passive and a level. The key in _51DEGREES_RESOURCE_KEY_BASIC has products
 * that don't, so the cloud answers the source as null, with the reason, and
 * the example's answer carries no source. The example is given the key the
 * test checks, as a person running it would set it in
 * _51DEGREES_RESOURCE_KEY. A test is skipped when its key or
 * CLOUD_STAGING_ENDPOINT isn't set, naming the variable, and fails when the
 * answer lacks what it checks. The Bespoke test first fails, naming the
 * variable and the property, when the service's list of the key's
 * properties doesn't include HumanConfidence, then when the service, asked
 * for HumanConfidenceSource with the key, doesn't answer it as Passive. The
 * list isn't asked about the source, as a key can be served the source when
 * a request names it without the list holding it. Keys are never printed.
 */
public class HumanConfidenceLiveTest {

    /**
     * A Bespoke key, whose products offer HumanConfidenceSource, that the
     * staging cloud resolves. Only CI sets it.
     */
    private static final String BESPOKE_KEY = "RESOURCE_KEY_BESPOKE_STAGING";

    /** The key whose products don't offer HumanConfidenceSource. */
    private static final String BASIC_KEY = "_51DEGREES_RESOURCE_KEY_BASIC";

    /** The staging cloud's API base. Only CI sets it. */
    private static final String ENDPOINT = "CLOUD_STAGING_ENDPOINT";

    private static final String SOURCE = "humanconfidencesource";

    private static final String SOURCE_REASON =
            "humanconfidencesourcenullreason";

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
        requireProperties(BESPOKE_KEY, key, "HumanConfidence");
        JSONObject served = servedSource(key);
        assertEquals("The staging cloud didn't answer HumanConfidenceSource "
                        + "as " + PASSIVE + " for the key in " + BESPOKE_KEY
                        + ": " + redact(served.toString(), key)
                        + ". Set the variable to a key whose products "
                        + "include it.",
                PASSIVE, served.optString(SOURCE, null));
        JSONObject answer = passive(key);
        String text = redact(answer.toString(), key);

        assertFalse("The example's passive call failed: " + text,
                answer.has("error"));
        assertEquals("The answer for the key in " + BESPOKE_KEY
                        + " carries the source " + PASSIVE + ": " + text
                        + ". The service says: "
                        + redact(served.toString(), key),
                PASSIVE, answer.optString("source", null));
        assertTrue("The passive level for the key in " + BESPOKE_KEY
                        + " is one of the plain levels: " + text,
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
        // A cloud that serves the source answers it for this key as null,
        // with the reason. A cloud that does not serve it leaves it out.
        String said = redact(served.toString(), key);
        assertTrue("The service answers this key's source as null: " + said,
                served.has(SOURCE) && served.isNull(SOURCE));
        assertTrue("The service says why the source is null: " + said,
                served.opt(SOURCE_REASON) instanceof String);
        assertFalse("This key's answer carries no source: " + text
                        + ". The service says: " + said,
                answer.has("source"));
    }

    @Test
    public void missingPropertiesNamesOnlyThePropertiesNoProductLists() {
        JSONObject accessible = new JSONObject(
                "{\"Products\":{"
                + "\"device\":{\"Properties\":[{\"Name\":\"IsMobile\"}]},"
                + "\"derived\":{\"Properties\":["
                + "{\"Name\":\"HumanConfidence\"}]}}}");

        assertEquals(Arrays.asList("HumanConfidenceSource"),
                missingProperties(accessible,
                        "HumanConfidence", "HumanConfidenceSource"));
        assertEquals(Arrays.asList("HumanConfidence"),
                missingProperties(new JSONObject("{}"), "HumanConfidence"));
    }

    @Test
    public void missingPropertiesIgnoresTheCaseOfTheNames() {
        JSONObject accessible = new JSONObject(
                "{\"Products\":{\"derived\":{\"Properties\":["
                + "{\"Name\":\"humanconfidence\"}]}}}");

        assertEquals(new ArrayList<String>(),
                missingProperties(accessible, "HumanConfidence"));
    }

    @Test
    public void missingMessageNamesEveryMissingPropertyAndTheVariable() {
        assertEquals("The key in " + BESPOKE_KEY + " does not include the "
                        + "HumanConfidence property. Set the variable to a "
                        + "key that includes it.",
                missingMessage(BESPOKE_KEY,
                        Arrays.asList("HumanConfidence")));
        assertEquals("The key in " + BESPOKE_KEY + " does not include the "
                        + "HumanConfidence and HumanConfidenceSource "
                        + "properties. Set the variable to a key that "
                        + "includes them.",
                missingMessage(BESPOKE_KEY, Arrays.asList(
                        "HumanConfidence", "HumanConfidenceSource")));
    }

    /** The key in the variable name, or a skip that names it. */
    private static String key(String name) {
        String key = System.getenv(name);
        assumeTrue(name + " is not set, so this live cloud test is "
                        + "skipped.",
                key != null && key.trim().isEmpty() == false);
        return key.trim();
    }

    /**
     * Fails, naming the variable and every property it lacks, when the
     * service's list of the properties the key can read lacks any of them.
     */
    private static void requireProperties(String name, String key,
            String... properties) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)
                new URL(api() + "accessibleproperties?resource=" + key)
                        .openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        int status = connection.getResponseCode();
        String body = read(status >= 400
                ? connection.getErrorStream() : connection.getInputStream());
        assertEquals("The service didn't list the properties of the key in "
                + name + ": " + redact(body, key), 200, status);
        JSONObject accessible;
        try {
            accessible = new JSONObject(body);
        } catch (JSONException e) {
            fail("The service's property list for the key in " + name
                    + " isn't JSON: " + redact(body, key));
            return;
        }
        List<String> missing = missingProperties(accessible, properties);
        if (missing.isEmpty() == false) {
            fail(missingMessage(name, missing));
        }
    }

    /**
     * The failure for a key whose list lacks the properties, naming the
     * variable and each property.
     */
    static String missingMessage(String name, List<String> missing) {
        boolean one = missing.size() == 1;
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < missing.size(); i++) {
            if (i > 0) {
                names.append(i == missing.size() - 1
                        ? (missing.size() > 2 ? ", and " : " and ") : ", ");
            }
            names.append(missing.get(i));
        }
        return "The key in " + name + " does not include the " + names
                + (one ? " property" : " properties")
                + ". Set the variable to a key that includes "
                + (one ? "it." : "them.");
    }

    /**
     * The properties, of those given, that no product in the service's list
     * of accessible properties names, ignoring case.
     */
    static List<String> missingProperties(JSONObject accessible,
            String... properties) {
        Set<String> names = new HashSet<>();
        JSONObject products = accessible.optJSONObject("Products");
        if (products != null) {
            for (String product : products.keySet()) {
                JSONObject entry = products.optJSONObject(product);
                JSONArray list = entry == null
                        ? null : entry.optJSONArray("Properties");
                for (int i = 0; list != null && i < list.length(); i++) {
                    JSONObject property = list.optJSONObject(i);
                    if (property != null) {
                        names.add(property.optString("Name")
                                .toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        List<String> missing = new ArrayList<>();
        for (String property : properties) {
            if (names.contains(property.toLowerCase(Locale.ROOT)) == false) {
                missing.add(property);
            }
        }
        return missing;
    }

    /** Starts the example with the key and asks it for /passive. */
    private JSONObject passive(String key) throws IOException {
        example = new HumanConfidence(api(), key, null);
        return passive(example.start(0));
    }

    /**
     * The API base the example calls, ending in one slash, or a skip that
     * names the variable. There is no default address.
     */
    private static String api() {
        String endpoint = System.getenv(ENDPOINT);
        assumeTrue(ENDPOINT + " is not set, so this live cloud test is "
                        + "skipped.",
                endpoint != null && endpoint.trim().isEmpty() == false);
        return endpoint.trim().replaceAll("/+$", "") + "/";
    }

    /**
     * Asks the service for HumanConfidenceSource with the key, and returns
     * the source's value and null reason from the answer, as the service
     * gave them.
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
                    SOURCE, SOURCE_REASON }) {
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
