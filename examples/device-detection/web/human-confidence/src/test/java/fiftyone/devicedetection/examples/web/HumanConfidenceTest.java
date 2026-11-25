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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Runs the Human Confidence example against a fake cloud on this machine,
 * so no keys and no network are needed.
 */
public class HumanConfidenceTest {

    private static final String RESOURCE = "test-resource-key";
    private static final String LICENSE = "test-license-key";
    private static final String REDEEMED =
            "{\"humanConfidence\":\"High\",\"secondsSinceVerified\":2}";
    private static final String PASSIVE =
            "{\"derived\":{"
            + "\"humanconfidencesource\":\"Passive\","
            + "\"humanconfidence\":\"Medium\"}}";
    /**
     * The answer to a request that doesn't name the source, for a key whose
     * saved list holds HumanConfidence alone.
     */
    private static final String LEVEL_ONLY =
            "{\"derived\":{\"humanconfidence\":\"Medium\"}}";
    /** The answer for a key outside Bespoke, with no source and no level. */
    private static final String NO_SOURCE =
            "{\"derived\":{"
            + "\"humanconfidence\":null,"
            + "\"humanconfidencenullreason\":\"Not in this key.\","
            + "\"humanconfidencesource\":null,"
            + "\"humanconfidencesourcenullreason\":\"Not in this key.\"}}";


    /** A link to 51degrees.com, up to the character that ends it. */
    private static final Pattern LINK = Pattern.compile(
            "https://51degrees[.]com[^ \"<>)]*");

    /** The five UTM parameters, in the order the link check expects. */
    private static final Pattern TAGGED = Pattern.compile(
            "https://51degrees[.]com/[^?#]*[?]"
            + "utm_source=(github|code)"
            + "&utm_medium=(readme|docs|example|comment|package)"
            + "&utm_campaign=51degrees-java"
            + "&utm_content=[a-z0-9][a-z0-9._-]*"
            + "&utm_term=[a-z0-9][a-z0-9._-]{0,59}");

    private HttpServer cloud;
    private HumanConfidence example;
    private volatile Map<String, String> passiveForm;
    private volatile Map<String, String> redeemForm;
    private volatile String passiveAnswer = PASSIVE;

    @After
    public void stop() {
        if (example != null) {
            example.stop();
        }
        if (cloud != null) {
            cloud.stop(0);
        }
    }

    /**
     * Starts the fake cloud and the example that uses it.
     * @param hasProperty whether the key's properties include
     *                    HumanConfidence
     * @param redeems whether the fake cloud answers hc/redeem
     * @return the example's base URL
     */
    private String start(boolean hasProperty, final boolean redeems)
            throws IOException {
        cloud = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        cloud.createContext("/api/v4/evidencekeys", exchange -> reply(
                exchange, 200,
                "[\"header.user-agent\",\"header.sec-ch-ua\",\"query.x\"]"));
        final String properties = hasProperty
                ? "{\"Products\":{\"device\":{\"Properties\":["
                  + "{\"Name\":\"HumanConfidence\"},"
                  + "{\"Name\":\"HumanConfidenceSource\"}]}}}"
                : "{\"Products\":{\"device\":{\"Properties\":["
                  + "{\"Name\":\"IsMobile\"}]}}}";
        cloud.createContext("/api/v4/accessibleproperties",
                exchange -> reply(exchange, 200, properties));
        cloud.createContext("/api/v4/" + RESOURCE + ".json", exchange -> {
            passiveForm = HumanConfidence.parseForm(body(exchange));
            String values = passiveForm.get("values");
            reply(exchange, 200, values != null
                    && values.contains("derived.humanconfidencesource")
                    ? passiveAnswer : LEVEL_ONLY);
        });
        cloud.createContext("/api/v4/hc/redeem", exchange -> {
            redeemForm = HumanConfidence.parseForm(body(exchange));
            if (redeems) {
                reply(exchange, 200, REDEEMED);
            } else {
                reply(exchange, 404, "Not found");
            }
        });
        cloud.start();
        String api = "http://127.0.0.1:" + cloud.getAddress().getPort()
                + "/api/v4/";
        example = new HumanConfidence(api, RESOURCE, LICENSE);
        int port = example.start(0);
        return "http://127.0.0.1:" + port + "/";
    }

    @Test
    public void defaultPortIs5151() {
        assertEquals(5151, HumanConfidence.DEFAULT_PORT);
        assertEquals(5151, HumanConfidence.port(
                Collections.<String, String>emptyMap()));
        Map<String, String> env = new HashMap<>();
        env.put("_51DEGREES_EXAMPLE_HTTP_PORTS", "8100");
        assertEquals(8100, HumanConfidence.port(env));
        env.put("_51DEGREES_EXAMPLE_HTTP_PORTS", " 8101, 8102");
        assertEquals(8101, HumanConfidence.port(env));
        env.put("_51DEGREES_EXAMPLE_HTTP_PORTS", " ");
        assertEquals(5151, HumanConfidence.port(env));
        env.clear();
        env.put("PORT", "8100");
        assertEquals(5151, HumanConfidence.port(env));
    }

    @Test
    public void pageHasKeyAndEndpointAndIsNotCached() throws IOException {
        String base = start(true, true);
        HttpURLConnection connection = open(base);
        String page = read(connection);
        assertEquals(200, connection.getResponseCode());
        assertEquals("no-store", connection.getHeaderField("Cache-Control"));
        assertTrue(page.contains("\"" + RESOURCE + "\""));
        assertTrue(page.contains("/api/v4/\""));
        assertFalse(page.contains("__RESOURCE__"));
        assertFalse(page.contains("__API__"));
        assertFalse(page.contains("contact-us"));
        assertFalse(page.contains(LICENSE));
    }

    @Test
    public void passiveSendsOnlyCloudHeadersAndReadsDerived()
            throws IOException {
        String base = start(true, true);
        // A raw request, so every header arrives exactly as written.
        String response = rawGet(base, "/passive",
                "User-Agent: Test Agent",
                "Sec-CH-UA: \"Test\";v=\"1\"",
                "Authorization: Bearer test",
                "X-Other: other");
        JSONObject answer = new JSONObject(
                response.substring(response.indexOf("\r\n\r\n") + 4));
        assertEquals("Passive", answer.optString("source"));
        assertEquals("Medium", answer.optString("humanConfidence"));

        assertEquals("derived.humanconfidence,derived.humanconfidencesource",
                passiveForm.get("values"));
        assertEquals("Test Agent", passiveForm.get("user-agent"));
        assertEquals("\"Test\";v=\"1\"", passiveForm.get("sec-ch-ua"));
        assertEquals("127.0.0.1", passiveForm.get("client-ip"));
        assertNull(passiveForm.get("authorization"));
        assertNull(passiveForm.get("x-other"));
        assertNull(passiveForm.get("x"));
    }

    @Test
    public void passiveWithoutTheSourceReportsNone() throws IOException {
        passiveAnswer = NO_SOURCE;
        String base = start(true, true);
        String response = rawGet(base, "/passive", "User-Agent: Test Agent");
        JSONObject answer = new JSONObject(
                response.substring(response.indexOf("\r\n\r\n") + 4));
        assertFalse(answer.toString(), answer.has("error"));
        assertFalse("No source is made up: " + answer, answer.has("source"));
        assertFalse("No level is made up: " + answer,
                answer.has("humanConfidence"));
    }

    @Test
    public void redeemPassesTheAnswerOnUnchanged() throws IOException {
        String base = start(true, true);
        HttpURLConnection connection = post(base + "redeem",
                "sealedResult=sealed%2Bvalue");
        assertEquals(200, connection.getResponseCode());
        assertEquals(REDEEMED, read(connection));
        assertEquals("sealed+value", redeemForm.get("sealedResult"));
        assertEquals("sealed+value", redeemForm.get("value"));
        assertEquals(LICENSE, redeemForm.get("license"));
    }

    @Test
    public void redeemSendsTheSealedValueUnderBothNames() throws IOException {
        String base = start(true, true);
        HttpURLConnection connection = post(base + "redeem", "value=v");
        assertEquals(200, connection.getResponseCode());
        assertEquals("v", redeemForm.get("sealedResult"));
        assertEquals("v", redeemForm.get("value"));
        assertFalse(redeemForm.containsKey("challenge"));
    }

    @Test
    public void redeemNotFoundSaysTheServiceDoesNotRedeem()
            throws IOException {
        String base = start(true, false);
        HttpURLConnection connection = post(base + "redeem", "value=v");
        String error = new JSONObject(read(connection)).optString("error");
        assertTrue(error, error.contains("doesn't redeem Human Confidence"));
    }

    @Test
    public void missingPropertyLinksToContactUs() throws IOException {
        String base = start(false, true);
        assertFalse(example.hasHumanConfidence());
        String page = read(open(base));
        assertTrue(HumanConfidence.CONTACT_URL,
                HumanConfidence.CONTACT_URL.startsWith(
                        "https://51degrees.com/contact-us?"));
        assertTrue(HumanConfidence.CONTACT_URL,
                TAGGED.matcher(HumanConfidence.CONTACT_URL).matches());
        assertTrue(page.contains(
                HumanConfidence.CONTACT_URL.replace("&", "&amp;")));
    }

    @Test
    public void stylesheetHasTheStatusColors() throws IOException {
        String base = start(true, true);
        HttpURLConnection connection = open(base + "examples-main.min.css");
        String css = read(connection);
        assertEquals(200, connection.getResponseCode());
        // The page sets these classes on each result, so the served
        // stylesheet must color them.
        String[] rules = {
            ".c-eg-status{",
            ".c-eg-status--pass{color:#9ba90d}",
            ".c-eg-status--fail{color:#cc2b27}",
            ".c-eg-status--part{color:#f5841f}",
            ".c-eg-status--none{color:#646060;"
        };
        for (String rule : rules) {
            assertTrue(rule, css.contains(rule));
        }
    }

    @Test
    public void readmeLinksFollowTheUtmConvention() throws IOException {
        // Maven runs the tests from the folder that holds the README.
        String readme = new String(
                Files.readAllBytes(Paths.get("README.md")),
                StandardCharsets.UTF_8);
        Matcher links = LINK.matcher(readme);
        int count = 0;
        while (links.find()) {
            count++;
            assertTrue(links.group(), TAGGED.matcher(links.group()).matches());
        }
        assertTrue("README links found: " + count, count >= 4);
    }

    @Test
    public void readmeNamesOnlyTheProductionCloud() throws IOException {
        assertEquals("https://cloud.51degrees.com/api/v4/",
                HumanConfidence.DEFAULT_ENDPOINT);
        String readme = new String(
                Files.readAllBytes(Paths.get("README.md")),
                StandardCharsets.UTF_8);
        Matcher hosts = Pattern.compile("[a-z0-9-]+[.]51degrees[.]com")
                .matcher(readme);
        int count = 0;
        while (hosts.find()) {
            count++;
            assertEquals("cloud.51degrees.com", hosts.group());
        }
        assertTrue("README cloud addresses found: " + count, count >= 1);
    }

    /**
     * The page shows a redemption as a success only when it was one. A
     * cloud from before status answers expired and replayed with a level
     * as well, and names the failure in result, so those must read as
     * failures. The page's own function is run in Node with each answer.
     * Where Node is absent the test says so and stops.
     */
    @Test
    public void pageReadsLegacyFailuresAsFailures() throws Exception {
        String page;
        try (InputStream in = HumanConfidence.class.getResourceAsStream(
                "/human-confidence/page.html")) {
            page = readAll(in);
        }
        Matcher function = Pattern.compile(
                "function redeemed\\(answer\\) \\{.*?\\r?\\n  \\}",
                Pattern.DOTALL).matcher(page);
        assertTrue("the page has its redeemed function", function.find());
        String[][] cases = {
            // Answers with status.
            {"{\"status\":\"success\",\"humanConfidence\":\"High\"}", "true"},
            {"{\"status\":\"replayed\",\"humanConfidence\":\"Low\"}", "false"},
            // Answers from a cloud before status.
            {"{\"humanConfidence\":\"High\",\"secondsSinceVerified\":2}",
                "true"},
            {"{\"humanConfidence\":\"High\",\"result\":\"success\"}", "true"},
            {"{\"humanConfidence\":\"Low\",\"result\":\"replayed\"}", "false"},
            {"{\"humanConfidence\":\"Low\",\"result\":\"expired\"}", "false"},
            {"{\"humanConfidence\":\"Low\",\"result\":\"unreadable\"}",
                "false"},
            {"{\"humanConfidence\":\"Low\",\"result\":\"unconfirmed\"}",
                "false"},
            {"{\"humanConfidence\":\"Low\",\"context\":\"replayed\"}",
                "false"},
            {"{\"humanConfidence\":\"Low\",\"result\":\"something-new\"}",
                "false"},
            {"{\"result\":\"unreadable\"}", "false"},
            {"{}", "false"},
        };
        StringBuilder script = new StringBuilder(function.group())
                .append("\nvar answers = [");
        for (String[] answer : cases) {
            script.append(answer[0]).append(",");
        }
        script.append("];\nconsole.log(JSON.stringify("
                + "answers.map(function (a) { return redeemed(a); })));");
        Process process;
        try {
            // The script goes in on standard input, which Node runs when
            // it is given no file, so no quoting of it is needed.
            process = new ProcessBuilder("node")
                    .redirectErrorStream(true).start();
        } catch (IOException notThere) {
            Assume.assumeTrue("node is not on the path, so the page's "
                    + "function was not run: " + notThere.getMessage(), false);
            return;
        }
        try (OutputStream in = process.getOutputStream()) {
            in.write(script.toString().getBytes(StandardCharsets.UTF_8));
        }
        String printed = readAll(process.getInputStream()).trim();
        assertEquals(printed, 0, process.waitFor());
        org.json.JSONArray results = new org.json.JSONArray(printed);
        assertEquals(cases.length, results.length());
        for (int i = 0; i < cases.length; i++) {
            assertEquals(cases[i][0], Boolean.parseBoolean(cases[i][1]),
                    results.getBoolean(i));
        }
    }

    private static String rawGet(String base, String path, String... headers)
            throws IOException {
        URL url = new URL(base);
        try (Socket socket = new Socket(url.getHost(), url.getPort())) {
            StringBuilder request = new StringBuilder("GET " + path
                    + " HTTP/1.1\r\nHost: " + url.getHost() + "\r\n");
            for (String header : headers) {
                request.append(header).append("\r\n");
            }
            request.append("Connection: close\r\n\r\n");
            socket.getOutputStream().write(
                    request.toString().getBytes(StandardCharsets.UTF_8));
            return readAll(socket.getInputStream());
        }
    }

    private static HttpURLConnection open(String url) throws IOException {
        return (HttpURLConnection) new URL(url).openConnection();
    }

    private static HttpURLConnection post(String url, String form)
            throws IOException {
        HttpURLConnection connection = open(url);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        try (OutputStream out = connection.getOutputStream()) {
            out.write(form.getBytes(StandardCharsets.UTF_8));
        }
        return connection;
    }

    private static String read(HttpURLConnection connection)
            throws IOException {
        InputStream in = connection.getResponseCode() >= 400
                ? connection.getErrorStream() : connection.getInputStream();
        return readAll(in);
    }

    private static String body(HttpExchange exchange) throws IOException {
        return readAll(exchange.getRequestBody());
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = in.read(buffer)) > 0) {
            bytes.write(buffer, 0, count);
        }
        in.close();
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void reply(HttpExchange exchange, int status, String text)
            throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
