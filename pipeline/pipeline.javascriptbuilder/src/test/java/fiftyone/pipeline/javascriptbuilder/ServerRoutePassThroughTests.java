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

package fiftyone.pipeline.javascriptbuilder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudPassThroughEngineBuilder;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngineBuilder;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.javascriptbuilder.data.JavaScriptBuilderData;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The server route: a publisher's server makes the cloud call for a page,
 * passes the cloud's page script on to the browser, and passes the
 * browser's answers back to the cloud on the next iteration, supplying the
 * visitor's address, JA4 and H2 itself. A local stand-in for the cloud
 * service holds the 51Did back until a call carries the answers, as the
 * cloud service does where a call requests HumanConfidence, and records
 * every call it is sent.
 */
public class ServerRoutePassThroughTests {

    /** The browser tests' script, as the stand-in hands it out. */
    private static final String SCRIPT =
        "window.__tests={token:'t'};"
        + "// 51D replace this comment with callback function.";

    /** The name the script's answers come back under. */
    private static final String ANSWER = "51D_x7Qa";

    private static final String JA4 = "t13d1516h2_8daaf6152771_02713d6af862";
    private static final String H2 =
        "1:65536;2:0;4:6291456;6:262144|15663105|0|m,a,s,p";
    private static final String VISITOR = "192.0.2.10";
    private static final String DID = "AzUxZC5lcwCNZzcAQgAAAAOP";

    private TestLoggerFactory loggerFactory;
    private HttpServer cloud;
    private final List<Map<String, String>> calls =
        Collections.synchronizedList(new ArrayList<Map<String, String>>());

    @BeforeEach
    public void start() throws IOException {
        loggerFactory = new TestLoggerFactory(null);
        cloud = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        cloud.createContext("/", this::answer);
        cloud.start();
    }

    @AfterEach
    public void stop() {
        cloud.stop(0);
    }

    @Test
    public void serverRoute_PassesTheScriptOnAndTheAnswersBack()
            throws Exception {
        String endpoint = "http://127.0.0.1:"
            + cloud.getAddress().getPort() + "/api/v4/";
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(new SequenceElementBuilder(loggerFactory)
                    .build())
                .addFlowElement(new CloudRequestEngineBuilder(loggerFactory)
                    .setResourceKey("rk")
                    .setEndpoint(endpoint)
                    .build())
                .addFlowElement(new CloudPassThroughEngineBuilder(
                    loggerFactory, "browserderived").build())
                .addFlowElement(new CloudPassThroughEngineBuilder(
                    loggerFactory, "derived").build())
                .addFlowElement(new CloudPassThroughEngineBuilder(
                    loggerFactory, "fodid").build())
                .addFlowElement(new JsonBuilderElementBuilder(loggerFactory)
                    .build())
                .addFlowElement(new JavaScriptBuilderElementBuilder(
                    loggerFactory).setEndpoint("/json").build())
                .setSuppressProcessException(true)
                .build()) {

            // Iteration 1, the page's own request. The server supplies the
            // visitor's address, JA4 and H2 as query evidence.
            String sessionId;
            try (FlowData page = pipeline.createFlowData()) {
                page.addEvidence("header.user-agent", "Mozilla/5.0");
                supply(page);
                page.process();
                assertNoErrors(page);

                Map<String, String> first = calls.get(calls.size() - 1);
                assertSupplied(first);
                assertEquals("1", first.get("sequence"), first.toString());
                sessionId = first.get("session-id");
                assertNotNull(sessionId, first.toString());
                assertFalse(first.containsKey(ANSWER), first.toString());

                JSONObject json = new JSONObject(
                    page.get(JsonBuilderData.class).getJson());
                assertTrue(json.getJSONArray("javascriptProperties")
                    .toList().contains(
                        "browserderived.browsertestsjavascript"),
                    json.toString());
                assertEquals(SCRIPT, json.getJSONObject("browserderived")
                    .getString("browsertestsjavascript"));
                assertTrue(json.getJSONObject("fodid")
                    .isNull("idprobglobal"), json.toString());
                assertTrue(json.getJSONObject("fodid")
                    .getString("idprobglobalnullreason")
                    .contains("browser test answers"), json.toString());

                String script =
                    page.get(JavaScriptBuilderData.class).getJavaScript();
                assertTrue(script.contains(sessionId),
                    "The page's script carries the session id.");
                assertTrue(script.contains("window.__tests"),
                    "The page's script carries the browser tests.");
            }

            // Iteration 2, the page's call back to the server. It posts
            // the answers, the session id and its sequence, and the server
            // takes from it what the Pipeline's filter accepts, as the web
            // integration does.
            Map<String, String> posted = new LinkedHashMap<>();
            posted.put(ANSWER, "3z|answers");
            posted.put("session-id", sessionId);
            posted.put("sequence", "1");
            try (FlowData callback = pipeline.createFlowData()) {
                for (Map.Entry<String, String> field : posted.entrySet()) {
                    String key = "query." + field.getKey();
                    assertTrue(pipeline.getEvidenceKeyFilter().include(key),
                        "The web integration would drop " + key);
                    callback.addEvidence(key, field.getValue());
                }
                callback.addEvidence("header.user-agent", "Mozilla/5.0");
                supply(callback);
                callback.process();
                assertNoErrors(callback);

                Map<String, String> second = calls.get(calls.size() - 1);
                assertSupplied(second);
                assertEquals("3z|answers", second.get(ANSWER),
                    second.toString());
                assertEquals(sessionId, second.get("session-id"));
                assertEquals("2", second.get("sequence"), second.toString());

                JSONObject json = new JSONObject(
                    callback.get(JsonBuilderData.class).getJson());
                assertEquals(DID, json.getJSONObject("fodid")
                    .getString("idprobglobal"), json.toString());
            }
        }
    }

    /** Fails with every error the Flow Data recorded, if there are any. */
    private static void assertNoErrors(FlowData data) {
        StringBuilder errors = new StringBuilder();
        for (FlowError error : data.getErrors()) {
            java.io.StringWriter trace = new java.io.StringWriter();
            error.getThrowable().printStackTrace(
                new java.io.PrintWriter(trace));
            errors.append(error.getFlowElement() == null
                ? "" : error.getFlowElement().getClass().getSimpleName())
                .append(": ").append(trace).append(System.lineSeparator());
        }
        assertEquals("", errors.toString());
    }

    /** The values a server supplies for its visitor. */
    private static void supply(FlowData data) {
        data.addEvidence("query.client-ip", VISITOR);
        data.addEvidence("query.ja4", JA4);
        data.addEvidence("query.h2", H2);
    }

    private static void assertSupplied(Map<String, String> call) {
        assertEquals(VISITOR, call.get("client-ip"), call.toString());
        assertEquals(JA4, call.get("ja4"), call.toString());
        assertEquals(H2, call.get("h2"), call.toString());
    }

    /**
     * The stand-in for the cloud service: the discovery answers, and a
     * data answer that holds the 51Did back until a call carries answers.
     */
    private void answer(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String body;
        if (path.endsWith("evidencekeys")) {
            body = new JSONArray(Arrays.asList(
                "header.User-Agent", "query.client-ip", "query.ja4",
                "query.h2", "query.transaction")).toString();
        } else if (path.endsWith("accessibleproperties")) {
            body = "{\"Products\":{"
                + "\"browserderived\":{\"DataTier\":\"t\",\"Properties\":["
                + "{\"Name\":\"BrowserTestsJavaScript\","
                + "\"Type\":\"JavaScript\",\"Category\":\"Browser\"}]},"
                + "\"derived\":{\"DataTier\":\"t\",\"Properties\":["
                + "{\"Name\":\"HumanConfidence\",\"Type\":\"String\","
                + "\"Category\":\"Derived\"}]},"
                + "\"FODid\":{\"DataTier\":\"t\",\"Properties\":["
                + "{\"Name\":\"IdProbGlobal\",\"Type\":\"String\","
                + "\"Category\":\"Id\"}]}}}";
        } else {
            Map<String, String> form = parse(readAll(
                exchange.getRequestBody()));
            calls.add(form);
            JSONObject json = new JSONObject();
            JSONObject fodid = new JSONObject();
            if (form.containsKey(ANSWER)) {
                fodid.put("idprobglobal", DID);
                json.put("derived", new JSONObject()
                    .put("humanconfidence", "sealed"));
                json.put("javascriptProperties", new JSONArray());
            } else {
                fodid.put("idprobglobal", JSONObject.NULL);
                fodid.put("idprobglobalnullreason", "Can not form "
                    + "IdProbGlobal. The browser test answers have not come "
                    + "back yet.");
                json.put("browserderived", new JSONObject()
                    .put("browsertestsjavascript", SCRIPT));
                json.put("derived", new JSONObject()
                    .put("humanconfidence", "Low"));
                json.put("javascriptProperties", new JSONArray()
                    .put("browserderived.browsertestsjavascript"));
            }
            json.put("fodid", fodid);
            body = json.toString();
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static Map<String, String> parse(String form) throws IOException {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String pair : form.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String name = equals < 0 ? pair : pair.substring(0, equals);
            String value = equals < 0 ? "" : pair.substring(equals + 1);
            fields.put(URLDecoder.decode(name, "UTF-8"),
                URLDecoder.decode(value, "UTF-8"));
        }
        return fields;
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
