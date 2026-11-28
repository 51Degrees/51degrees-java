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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
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
    /** The JSON call's answer where the sealed value redeems. */
    private static final String REDEEMED =
            "{\"derived\":{"
            + "\"humanconfidencesource\":\"Active\","
            + "\"humanconfidence\":\"High\"}}";
    /** The JSON call's answer where the sealed value was redeemed before. */
    private static final String REPLAYED =
            "{\"derived\":{"
            + "\"humanconfidencesource\":\"Active\","
            + "\"humanconfidence\":\"Medium\"}}";
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
    private final List<Map<String, String>> sealedForms =
            new CopyOnWriteArrayList<>();
    private final AtomicInteger hcRedeemCalls = new AtomicInteger();
    private final AtomicInteger propertiesCalls = new AtomicInteger();
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

    private String start(boolean hasProperty) throws IOException {
        return start(hasProperty, LICENSE);
    }

    /**
     * Starts the fake cloud and the example that uses it. The fake cloud's
     * JSON call redeems a sealed value the first time it is sent, and
     * answers Medium after that. It counts any call to hc/redeem, which
     * the example must never make.
     * @param hasProperty whether the key's properties include
     *                    HumanConfidence
     * @param license the License Key the example is given, or null
     * @return the example's base URL
     */
    private String start(boolean hasProperty, String license)
            throws IOException {
        cloud = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        cloud.createContext("/api/v4/evidencekeys", exchange -> reply(
                exchange, 200,
                "[\"header.user-agent\",\"header.sec-ch-ua\",\"query.x\"]"));
        final String properties = hasProperty
                ? "{\"Products\":{\"derived\":{\"DataTier\":\"Bespoke\","
                  + "\"Properties\":["
                  + "{\"Name\":\"HumanConfidence\",\"Type\":\"String\","
                  + "\"Category\":\"Bot Detection\"},"
                  + "{\"Name\":\"HumanConfidenceSource\",\"Type\":\"String\","
                  + "\"Category\":\"Bot Detection\"}]}}}"
                : "{\"Products\":{\"device\":{\"DataTier\":\"Free\","
                  + "\"Properties\":["
                  + "{\"Name\":\"IsMobile\",\"Type\":\"Bool\","
                  + "\"Category\":\"Device\"}]}}}";
        cloud.createContext("/api/v4/accessibleproperties", exchange -> {
            propertiesCalls.incrementAndGet();
            reply(exchange, 200, properties);
        });
        cloud.createContext("/api/v4/" + RESOURCE + ".json", exchange -> {
            Map<String, String> form = HumanConfidence.parseForm(
                    body(exchange));
            String sealed = form.get("humanconfidence");
            if (sealed != null) {
                boolean first = true;
                for (Map<String, String> earlier : sealedForms) {
                    first &= sealed.equals(earlier.get("humanconfidence"))
                            == false;
                }
                sealedForms.add(form);
                reply(exchange, 200, first && form.containsKey("license")
                        ? REDEEMED : REPLAYED);
                return;
            }
            passiveForm = form;
            String values = passiveForm.get("values");
            reply(exchange, 200, values != null
                    && values.contains("derived.humanconfidencesource")
                    ? passiveAnswer : LEVEL_ONLY);
        });
        cloud.createContext("/api/v4/hc/redeem", exchange -> {
            hcRedeemCalls.incrementAndGet();
            reply(exchange, 404, "Not found");
        });
        cloud.start();
        String api = "http://127.0.0.1:" + cloud.getAddress().getPort()
                + "/api/v4/";
        example = new HumanConfidence(api, RESOURCE, license);
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
    public void pageHasScriptAndPageIdAndIsNotCached() throws IOException {
        String base = start(true);
        HttpURLConnection connection = open(base);
        String page = read(connection);
        assertEquals(200, connection.getResponseCode());
        assertEquals("no-store", connection.getHeaderField("Cache-Control"));
        assertTrue(page, page.contains(
                "/api/v4/" + RESOURCE + ".js?transaction="));
        assertFalse(pageId(page).isEmpty());
        assertFalse("Each page gets its own id.",
                pageId(page).equals(pageId(read(open(base)))));
        assertFalse(page.contains("__SCRIPT__"));
        assertFalse(page.contains("__PAGE__"));
        assertFalse(page.contains("contact-us"));
        assertFalse(page.contains(LICENSE));
    }

    @Test
    public void passiveSendsOnlyCloudHeadersAndReadsDerived()
            throws IOException {
        String base = start(true);
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
        String base = start(true);
        String response = rawGet(base, "/passive", "User-Agent: Test Agent");
        JSONObject answer = new JSONObject(
                response.substring(response.indexOf("\r\n\r\n") + 4));
        assertFalse(answer.toString(), answer.has("error"));
        assertFalse("No source is made up: " + answer, answer.has("source"));
        assertFalse("No level is made up: " + answer,
                answer.has("humanConfidence"));
    }

    /**
     * The page sends the sealed value to the server as humanconfidence.
     * The server adds it to the Evidence, and the Pipeline redeems it
     * through the JSON call with the License Key. The server reads the two
     * properties and makes no request to hc/redeem.
     */
    @Test
    public void activeReadsThePropertiesThePipelineRedeemed()
            throws IOException {
        String base = start(true);
        String id = pageId(read(open(base)));
        HttpURLConnection connection = post(base + "active",
                "humanconfidence=sealed%2Bvalue&humanconfidencepage=" + id);
        assertEquals(200, connection.getResponseCode());
        JSONObject answer = new JSONObject(read(connection));
        assertEquals("Active", answer.optString("source"));
        assertEquals("High", answer.optString("humanConfidence"));

        assertEquals(1, sealedForms.size());
        Map<String, String> form = sealedForms.get(0);
        assertEquals("sealed+value", form.get("humanconfidence"));
        assertEquals(LICENSE, form.get("license"));
        assertFalse(form.get("transaction").isEmpty());
        assertFalse(form.containsKey("humanconfidencepage"));
        assertFalse(form.containsKey("value"));
        assertFalse(form.containsKey("sealedResult"));
        assertEquals(0, hcRedeemCalls.get());
    }

    /**
     * A page's transaction is used once, so the same value sent again is
     * not sent to the cloud, and the second answer is Medium, still Active.
     */
    @Test
    public void activeSentTwiceIsMediumTheSecondTime() throws IOException {
        String base = start(true);
        String form = "humanconfidence=v&humanconfidencepage="
                + pageId(read(open(base)));
        post(base + "active", form).getResponseCode();
        JSONObject second = new JSONObject(read(post(base + "active", form)));
        assertEquals("Active", second.optString("source"));
        assertEquals("Medium", second.optString("humanConfidence"));
        assertEquals(1, sealedForms.size());
        assertEquals(0, hcRedeemCalls.get());
    }

    /** Without a License Key the sealed value is not redeemed. */
    @Test
    public void activeWithoutALicenseKeyIsMedium() throws IOException {
        String base = start(true, null);
        JSONObject answer = new JSONObject(read(post(base + "active",
                "humanconfidence=v&humanconfidencepage="
                + pageId(read(open(base))))));
        assertEquals("Active", answer.optString("source"));
        assertEquals("Medium", answer.optString("humanConfidence"));
        assertEquals(0, hcRedeemCalls.get());
    }

    /**
     * The page sends the sealed value to the server's active route as
     * humanconfidence, and reads only the two properties the server
     * answers with.
     */
    @Test
    public void pageSendsTheSealedValueToTheServer() throws IOException {
        String page;
        try (InputStream in = HumanConfidence.class.getResourceAsStream(
                "/human-confidence/page.html")) {
            page = readAll(in);
        }
        assertTrue(page.contains("fetch(\"active\""));
        assertTrue(page.contains("body.set(\"humanconfidence\", value)"));
        assertTrue(page.contains("body.set(\"humanconfidencepage\", PAGE)"));
        assertFalse(page.contains("fetch(\"redeem\""));
        assertFalse(page.contains("sealedResult"));
        assertFalse(page.contains("answer.status"));
    }

    /**
     * A failed read gives the page a fixed text, never the exception's,
     * which can quote the cloud's answer.
     */
    @Test
    public void failedReadGivesThePageAFixedText() throws IOException {
        start(true);
        JSONObject answer = example.failed(new IllegalStateException(
                "The cloud said: key " + LICENSE + " is not valid."));
        assertEquals(HumanConfidence.READ_FAILED, answer.getString("error"));
        assertFalse(answer.toString(), answer.toString().contains("cloud said"));
        assertFalse(answer.toString(), answer.toString().contains("-key"));
    }

    /**
     * The example reads the properties the Cloud Request Engine found, so
     * the only accessibleproperties call is the engine's own.
     */
    @Test
    public void onlyTheEngineAsksForTheProperties() throws IOException {
        start(true);
        assertTrue(example.hasHumanConfidence());
        assertEquals(1, propertiesCalls.get());
    }

    @Test
    public void missingPropertyLinksToContactUs() throws IOException {
        String base = start(false);
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
        String base = start(true);
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

    /** The page id the example put in a page. */
    private static String pageId(String page) {
        Matcher id = Pattern.compile("var PAGE = \"([^\"]*)\"")
                .matcher(page);
        assertTrue(page, id.find());
        return id.group(1);
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
