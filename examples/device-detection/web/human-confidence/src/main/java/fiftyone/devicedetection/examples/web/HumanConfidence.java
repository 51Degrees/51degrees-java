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
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngineBuilder;
import fiftyone.pipeline.cloudrequestengine.flowelements.HumanConfidenceElement;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngineDefault;
import fiftyone.pipeline.cloudrequestengine.flowelements.HumanConfidenceElementBuilder;
import fiftyone.pipeline.cloudrequestengine.flowelements.HumanConfidencePage;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Human Confidence demo server. It uses the JDK's own HTTP server, and a
 * 51Degrees Pipeline with the Cloud Request Engine and the Human Confidence
 * element to get the HumanConfidence and HumanConfidenceSource Properties.
 * <p>
 * Human Confidence says how likely it is that a person is looking at the
 * page. The demo shows it in three steps.
 * <ol>
 * <li>Passive. This server processes the visitor's request headers and IP
 * address with the Pipeline, and reads HumanConfidenceSource and
 * HumanConfidence. The Pipeline names both properties, so the answer
 * carries them whatever the Resource Key's saved list of properties holds.
 * The server reads them rather than reading a level from the page, because
 * a level a browser sends can be forged.</li>
 * <li>Active. The page loads the 51Degrees client script from the address
 * the package gives the page, which carries the page's transaction. When
 * the script
 * completes, HumanConfidenceSource is Active and HumanConfidence holds a
 * sealed value the browser can't read or change.</li>
 * <li>Read by the server. The page sends the sealed value to this server
 * as humanconfidence, with the page id the package gave it as
 * humanconfidencepage. The server adds both to the Evidence as query
 * values and processes the request with the Pipeline. The
 * Pipeline redeems the sealed value with the License Key, which stays on
 * this server, and the server reads the same two Properties. A sealed
 * value redeems once, within ten seconds. Where it does not redeem,
 * HumanConfidence is Medium.</li>
 * </ol>
 * Environment variables:
 * <ul>
 * <li>_51DEGREES_RESOURCE_KEY, the Resource Key, required</li>
 * <li>_51DEGREES_LICENSE_KEY, a License Key of the same account, needed to
 * redeem</li>
 * <li>_51DEGREES_CLOUD_ENDPOINT, the API base including /api/v4/, which
 * defaults to https://cloud.51degrees.com/api/v4/</li>
 * <li>_51DEGREES_EXAMPLE_HTTP_PORTS, the port to listen on, which defaults
 * to 5151. It may hold a comma separated list, and the example listens on
 * the first port in it</li>
 * </ul>
 * See the README beside this example's pom.xml for the steps.
 */
public class HumanConfidence {

    /** The environment variable that names the port to listen on. */
    public static final String HTTP_PORTS_ENV_VAR =
            "_51DEGREES_EXAMPLE_HTTP_PORTS";

    /** The port the example listens on when no port is set. */
    public static final int DEFAULT_PORT = 5151;

    /** The cloud API base used when _51DEGREES_CLOUD_ENDPOINT isn't set. */
    public static final String DEFAULT_ENDPOINT =
            "https://cloud.51degrees.com/api/v4/";

    /** Where to ask for a Resource Key that includes HumanConfidence. */
    public static final String CONTACT_URL = "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=web-human-confidence-src-main-java-fiftyone-devicedetection-examples-web-humanconfidence.java&utm_term=contact-us";

    /** The properties the passive call reads, named in its request. */
    public static final String PASSIVE_VALUES =
            "derived.humanconfidence,derived.humanconfidencesource";

    /** A short limit, so a slow cloud can't hang the demo. */
    private static final int TIMEOUT_MS = 10000;

    /** The page's parameter that carries the sealed value. */
    public static final String SEALED_PARAMETER = "humanconfidence";

    /** The parameter the page sends its page id as. */
    public static final String PAGE_PARAMETER = "humanconfidencepage";

    private static final String PAGE = "/human-confidence/page.html";
    private static final String CSS = "/human-confidence/examples-main.min.css";

    private final String api;
    private final String resource;
    private final String license;
    private boolean hasHumanConfidence = true;
    private HttpServer server;
    private CloudRequestEngine engine;
    private Pipeline pipeline;

    /**
     * @param api the cloud API base ending in /api/v4/
     * @param resource the Resource Key
     * @param license the License Key, or null when there is none
     */
    public HumanConfidence(String api, String resource, String license) {
        this.api = api.trim().replaceAll("/+$", "") + "/";
        this.resource = resource;
        this.license = license;
    }

    /**
     * Runs the demo until the process is stopped.
     * @param args not used
     * @throws IOException when the server can't start
     */
    public static void main(String[] args) throws IOException {
        Map<String, String> env = System.getenv();
        String resource = env.get("_51DEGREES_RESOURCE_KEY");
        if (isBlank(resource)) {
            System.err.println(
                    "Set _51DEGREES_RESOURCE_KEY to the Resource Key of the page.");
            System.exit(1);
            return;
        }
        String license = env.get("_51DEGREES_LICENSE_KEY");
        if (isBlank(license)) {
            System.out.println("No _51DEGREES_LICENSE_KEY set. The page still "
                    + "shows Passive and Active. The sealed value redeems "
                    + "only with a License Key, so the third row shows "
                    + "Medium.");
            license = null;
        }
        String endpoint = env.get("_51DEGREES_CLOUD_ENDPOINT");
        HumanConfidence example = new HumanConfidence(
                isBlank(endpoint) ? DEFAULT_ENDPOINT : endpoint,
                resource, license);
        int port = example.start(port(env));
        System.out.println("Human Confidence demo on http://localhost:"
                + port + "/, using " + example.api);
    }

    /**
     * Reads the first port {@link #HTTP_PORTS_ENV_VAR} lists, or uses
     * {@link #DEFAULT_PORT}.
     * @param env the environment variables
     * @return the port to listen on
     */
    public static int port(Map<String, String> env) {
        String value = env.get(HTTP_PORTS_ENV_VAR);
        String first = isBlank(value) ? "" : value.split(",", -1)[0].trim();
        return first.isEmpty() ? DEFAULT_PORT : Integer.parseInt(first);
    }

    /**
     * Checks the Resource Key's properties, then starts the server.
     * @param port the port, or 0 for any free port
     * @return the port the server listens on
     * @throws IOException when the server can't start
     */
    public int start(int port) throws IOException {
        checkProperties();
        buildPipeline();
        server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        return server.getAddress().getPort();
    }

    /** Stops the server and closes the Pipeline. */
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (pipeline != null) {
            try {
                pipeline.close();
            } catch (Exception e) {
                System.out.println("Couldn't close the Pipeline. "
                        + e.getMessage());
            }
        }
    }

    /**
     * Builds the Pipeline: the Cloud Request Engine with the Resource Key,
     * the License Key where there is one, and the two properties named,
     * then the Human Confidence element that gives them.
     */
    private void buildPipeline() throws IOException {
        ILoggerFactory loggers = LoggerFactory.getILoggerFactory();
        try {
            CloudRequestEngineBuilder builder =
                    new CloudRequestEngineBuilder(loggers)
                            .setResourceKey(resource);
            if (license != null) {
                builder.setLicenseKey(license);
            }
            engine = builder
                    .setEndpoint(api)
                    .setRequestedProperties(PASSIVE_VALUES)
                    .setTimeOutSeconds(TIMEOUT_MS / 1000)
                    .build();
            pipeline = new PipelineBuilder(loggers)
                    .setSuppressProcessException(true)
                    .addFlowElement(engine)
                    .addFlowElement(new HumanConfidenceElementBuilder(loggers)
                            .setRequestEngine(engine)
                            .build())
                    .build();
        } catch (Exception e) {
            throw new IOException("Couldn't build the Pipeline. "
                    + e.getMessage(), e);
        }
    }

    /**
     * @return false when the Resource Key's properties don't include
     * HumanConfidence
     */
    public boolean hasHumanConfidence() {
        return hasHumanConfidence;
    }

    /**
     * Asks the cloud which properties the Resource Key has. When
     * HumanConfidence isn't one of them, the console and the page say so.
     */
    private void checkProperties() {
        try {
            Answer answer = send("GET", api + "accessibleproperties?resource="
                    + encode(resource), null);
            if (answer.status == 200) {
                hasHumanConfidence = hasProperty(
                        new JSONObject(answer.body), "humanconfidence");
            }
        } catch (IOException | JSONException e) {
            System.out.println("Couldn't check the Resource Key's properties. "
                    + e.getMessage());
        }
        if (hasHumanConfidence == false) {
            System.out.println("This Resource Key doesn't include "
                    + "HumanConfidence. Contact us to add it. " + CONTACT_URL);
        }
    }

    /** Looks for a property Name anywhere in the answer. */
    private static boolean hasProperty(Object node, String name) {
        if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            Object value = object.opt("Name");
            if (value instanceof String
                    && name.equalsIgnoreCase((String) value)) {
                return true;
            }
            for (String key : object.keySet()) {
                if (hasProperty(object.get(key), name)) {
                    return true;
                }
            }
        } else if (node instanceof JSONArray) {
            for (Object item : (JSONArray) node) {
                if (hasProperty(item, name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            if ("/".equals(path) && "GET".equals(method)) {
                page(exchange);
            } else if ("/examples-main.min.css".equals(path)) {
                reply(exchange, 200, "text/css", resourceBytes(CSS));
            } else if ("/passive".equals(path) && "GET".equals(method)) {
                properties(exchange, null, null);
            } else if ("/active".equals(path) && "POST".equals(method)) {
                Map<String, String> form =
                        parseForm(readAll(exchange.getRequestBody()));
                String sealed = form.get(SEALED_PARAMETER);
                properties(exchange, sealed == null ? "" : sealed,
                        form.get(PAGE_PARAMETER));
            } else {
                reply(exchange, 404, "text/plain", bytes("Not found"));
            }
        } finally {
            exchange.close();
        }
    }

    /**
     * Serves the page, read on each request so an edit shows at once. The
     * package gives each page its id and the client script's address,
     * which carries the transaction it keeps for the page.
     */
    private void page(HttpExchange exchange) throws IOException {
        HumanConfidencePage humanConfidencePage =
                ((CloudRequestEngineDefault) engine).newHumanConfidencePage();
        String notice = hasHumanConfidence ? ""
                : "  <p class=\"c-eg-page__lead\" id=\"notice\">This Resource "
                  + "Key doesn't include HumanConfidence. <a href=\""
                  + CONTACT_URL.replace("&", "&amp;")
                  + "\">Contact us</a> to add it.</p>";
        String html = new String(resourceBytes(PAGE), StandardCharsets.UTF_8)
                .replace("__NOTICE__", notice)
                .replace("__SCRIPT__", humanConfidencePage.scriptAddress(
                        api + resource + ".js"))
                .replace("__PAGE__", humanConfidencePage.getId());
        // Each load runs the browser tests, so never serve it from a cache.
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        reply(exchange, 200, "text/html; charset=utf-8", bytes(html));
    }

    /**
     * Processes the visitor's request with the Pipeline, and returns source
     * and humanConfidence from the HumanConfidenceSource and
     * HumanConfidence Properties. The Evidence is the visitor's headers
     * that the cloud reads, the visitor's IP address, and the sealed value
     * where the page sent one. The Pipeline redeems a sealed value itself,
     * so this server only reads the two Properties. A Property with no
     * value is left out, and its reason is given instead.
     * @param sealed the sealed value the page sent, or null for the
     *               passive step
     * @param pageId the page id the page sent, or null
     */
    private void properties(HttpExchange exchange, String sealed,
            String pageId) throws IOException {
        JSONObject result = new JSONObject();
        try (FlowData data = pipeline.createFlowData()) {
            for (Map.Entry<String, List<String>> header
                    : exchange.getRequestHeaders().entrySet()) {
                String key = "header."
                        + header.getKey().toLowerCase(Locale.ROOT);
                // Only the headers the cloud reads, so headers such as
                // Authorization stay on this server.
                if (engine.getEvidenceKeyFilter().include(key)) {
                    data.addEvidence(key, String.join(",", header.getValue()));
                }
            }
            // Behind a load balancer or CDN, use the visitor's address it
            // passes on.
            data.addEvidence("server.client-ip",
                    exchange.getRemoteAddress().getAddress().getHostAddress());
            if (sealed != null) {
                data.addEvidence("query." + SEALED_PARAMETER, sealed);
            }
            if (pageId != null) {
                data.addEvidence("query." + PAGE_PARAMETER, pageId);
            }
            data.process();
            ElementData derived = data.get(
                    HumanConfidenceElement.ELEMENT_DATA_KEY);
            put(result, "source", derived.get(
                    HumanConfidenceElement.HUMAN_CONFIDENCE_SOURCE));
            put(result, "humanConfidence", derived.get(
                    HumanConfidenceElement.HUMAN_CONFIDENCE));
        } catch (Exception e) {
            result = failed(e);
        }
        reply(exchange, 200, "application/json", bytes(result.toString()));
    }

    /** What the page is told when the properties can't be read. */
    static final String READ_FAILED =
            "The properties could not be read. See the server's console.";

    /**
     * The answer the page gets when reading the properties fails. It holds
     * a fixed text and never the exception's, which can quote what the
     * cloud sent back. The details go to the console instead.
     */
    JSONObject failed(Exception e) {
        System.out.println("Couldn't read the properties. "
                + withoutLicenseKey(String.valueOf(e)));
        return new JSONObject().put("error", READ_FAILED);
    }

    /** The text with the License Key taken out, for the console. */
    private String withoutLicenseKey(String text) {
        return license == null ? text : text.replace(license, "[hidden]");
    }

    /**
     * Puts a Property's value in the answer under the name, or its reason
     * under the name followed by Reason where it has no value.
     */
    private static void put(JSONObject result, String name, Object property) {
        if (property instanceof AspectPropertyValue) {
            AspectPropertyValue<?> value = (AspectPropertyValue<?>) property;
            if (value.hasValue()) {
                result.put(name, String.valueOf(value.getValue()));
            } else {
                result.put(name + "Reason", value.getNoValueMessage());
            }
        }
    }

    /** A status code and body from the cloud. */
    private static class Answer {
        final int status;
        final String body;

        Answer(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    private static Answer send(String method, String url, String form)
            throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod(method);
            if (form != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(bytes(form));
                }
            }
            int status = connection.getResponseCode();
            InputStream in = status >= 400
                    ? connection.getErrorStream() : connection.getInputStream();
            return new Answer(status, in == null ? "" : readAll(in));
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Reads a URL encoded form.
     * @param body the form
     * @return the fields by name
     */
    public static Map<String, String> parseForm(String body) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String name = equals < 0 ? pair : pair.substring(0, equals);
            String value = equals < 0 ? "" : pair.substring(equals + 1);
            fields.put(decode(name), decode(value));
        }
        return fields;
    }

    private static String form(Map<String, String> fields) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            if (text.length() > 0) {
                text.append('&');
            }
            text.append(encode(field.getKey())).append('=')
                    .append(encode(field.getValue()));
        }
        return text.toString();
    }

    private static String encode(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String decode(String text) {
        try {
            return URLDecoder.decode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] resourceBytes(String name) throws IOException {
        InputStream in = HumanConfidence.class.getResourceAsStream(name);
        if (in == null) {
            throw new IOException("Missing resource " + name);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void copy(InputStream in, OutputStream out)
            throws IOException {
        try {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) > 0) {
                out.write(buffer, 0, count);
            }
        } finally {
            in.close();
        }
    }

    private static void reply(HttpExchange exchange, int status,
            String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String trim(String text) {
        return text.length() > 200 ? text.substring(0, 200) : text;
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
