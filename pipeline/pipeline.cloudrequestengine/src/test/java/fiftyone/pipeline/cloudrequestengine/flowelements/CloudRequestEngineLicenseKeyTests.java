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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * The credentials a Cloud Request Engine accepts, what it sends for each
 * and that a license key stays out of URLs, logs and messages.
 */
public class CloudRequestEngineLicenseKeyTests {

    private static final String BASE = "https://cloud.example/api/v4/";
    private static final String LICENSE_KEY = "abcdefghijkl1234";
    private static final String RESOURCE_KEY = "resourcekey";
    private static final String PROPERTIES = "device.ismobile,device.iscrawler";
    private static final List<String> PROPERTY_LIST =
        Arrays.asList("device.ismobile", "device.iscrawler");

    /** A request the engine made. */
    private static class Call {
        final String method;
        final URL url;
        final String body;

        Call(String method, URL url, String body) {
            this.method = method;
            this.url = url;
            this.body = body;
        }
    }

    /** Records every request and answers from fixed responses. */
    private static class FakeCloud implements HttpClient {
        final List<Call> calls = new ArrayList<>();
        String dataResponse = "{\"device\":{\"ismobile\":true," +
            "\"iscrawler\":false}}";
        int dataStatus = 200;
        String propertiesResponse =
            "{\"Products\":{\"device\":{\"DataTier\":\"t\",\"Properties\":" +
            "[{\"Name\":\"IsMobile\",\"Type\":\"Boolean\"," +
            "\"Category\":\"Device\"}]}}}";
        String evidenceKeysResponse = "[\"query.User-Agent\"]";
        private final Map<HttpURLConnection, URL> urls = new HashMap<>();

        @Override
        public HttpURLConnection connect(URL url) {
            HttpURLConnection connection = mock(HttpURLConnection.class);
            try {
                when(connection.getURL()).thenReturn(url);
                when(connection.getResponseCode()).thenReturn(
                    url.getPath().endsWith("json") ? dataStatus : 200);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            when(connection.getHeaderFields()).thenReturn(
                Collections.emptyMap());
            urls.put(connection, url);
            return connection;
        }

        @Override
        public String postData(
            HttpURLConnection connection,
            Map<String, String> headers,
            byte[] data) {
            URL url = urls.get(connection);
            calls.add(new Call(
                "POST",
                url,
                data == null ? "" :
                    new String(data, StandardCharsets.UTF_8)));
            return answer(url);
        }

        @Override
        public String getResponseString(HttpURLConnection connection) {
            return getResponseString(connection, null);
        }

        @Override
        public String getResponseString(
            HttpURLConnection connection,
            Map<String, String> headers) {
            URL url = urls.get(connection);
            calls.add(new Call("GET", url, ""));
            return answer(url);
        }

        private String answer(URL url) {
            if (url.getPath().endsWith("accessibleproperties")) {
                return propertiesResponse;
            }
            if (url.getPath().endsWith("evidencekeys")) {
                return evidenceKeysResponse;
            }
            return dataResponse;
        }

        List<Call> callsTo(String pathEnd) {
            List<Call> found = new ArrayList<>();
            for (Call call : calls) {
                if (call.url.getPath().endsWith(pathEnd)) {
                    found.add(call);
                }
            }
            return found;
        }
    }

    private FakeCloud cloud;
    private Logger logger;
    private ILoggerFactory loggerFactory;

    @BeforeEach
    public void setUp() {
        cloud = new FakeCloud();
        logger = mock(Logger.class);
        loggerFactory = mock(ILoggerFactory.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);
    }

    private CloudRequestEngineBuilder builder() {
        return new CloudRequestEngineBuilder(loggerFactory, cloud);
    }

    /**
     * Build with the test endpoint set last, as the default endpoint is when
     * no endpoint is set, so the data endpoint's route reflects the keys.
     */
    private CloudRequestEngine build(CloudRequestEngineBuilder builder)
        throws Exception {
        return builder.setEndpoint(BASE).build();
    }

    private String process(CloudRequestEngine engine) throws Exception {
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();
            StringBuilder errors = new StringBuilder();
            for (FlowError error : data.getErrors()) {
                for (Throwable t = error.getThrowable(); t != null;
                     t = t.getCause()) {
                    errors.append(t).append('\n');
                }
            }
            return errors.toString();
        }
    }

    private static Map<String, String> form(String body)
        throws UnsupportedEncodingException {
        Map<String, String> fields = new HashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            String[] parts = pair.split("=", 2);
            fields.put(
                URLDecoder.decode(parts[0], "UTF-8"),
                URLDecoder.decode(parts.length > 1 ? parts[1] : "", "UTF-8"));
        }
        return fields;
    }

    private Map<String, String> dataRequest(String pathEnd)
        throws UnsupportedEncodingException {
        List<Call> posts = cloud.callsTo(pathEnd);
        assertEquals(1, posts.size());
        assertEquals("POST", posts.get(0).method);
        return form(posts.get(0).body);
    }

    private void assertRefused(
        CloudRequestEngineBuilder builder,
        String... expectedInMessage) {
        PipelineConfigurationException e = assertThrows(
            PipelineConfigurationException.class,
            () -> build(builder));
        for (String expected : expectedInMessage) {
            assertTrue(
                e.getMessage().contains(expected),
                "Message should contain '" + expected + "' but was: " +
                    e.getMessage());
        }
        assertTrue(cloud.calls.isEmpty(), "A refused build sends nothing.");
    }

    // The rows of the specification's table of accepted combinations.

    @Test
    public void Build_ResourceKeyOnly_Accepted() throws Exception {
        assertNotNull(build(builder().setResourceKey(RESOURCE_KEY)));
    }

    @Test
    public void Build_ResourceKeyAndLicenseKey_Accepted() throws Exception {
        assertNotNull(build(builder()
            .setResourceKey(RESOURCE_KEY)
            .setLicenseKey(LICENSE_KEY)));
    }

    @Test
    public void Build_AllThree_Accepted() throws Exception {
        assertNotNull(build(builder()
            .setResourceKey(RESOURCE_KEY)
            .setLicenseKey(LICENSE_KEY)
            .setProperties(PROPERTY_LIST)));
    }

    @Test
    public void Build_LicenseKeyAndProperties_Accepted() throws Exception {
        assertNotNull(build(builder()
            .setLicenseKey(LICENSE_KEY)
            .setProperties(PROPERTY_LIST)));
    }

    @Test
    public void Build_LicenseKeyWithoutProperties_Refused() {
        assertRefused(
            builder().setLicenseKey(LICENSE_KEY),
            "answers 400",
            "setProperty");
    }

    @Test
    public void Build_ResourceKeyWithProperties_Accepted() throws Exception {
        assertNotNull(build(builder()
            .setResourceKey(RESOURCE_KEY)
            .setProperty("device.hardwarevendor")));
    }

    @Test
    public void Build_NoCredential_Refused() {
        assertRefused(builder(), "401", "resource key");
        assertRefused(builder().setProperties(PROPERTY_LIST), "401");
    }

    @Test
    public void Build_BlankValuesCountAsAbsent() throws Exception {
        assertRefused(
            builder().setResourceKey("  ").setLicenseKey("\n"),
            "401");
        assertRefused(
            builder()
                .setLicenseKey(LICENSE_KEY)
                .setProperties(Arrays.asList(" ", "")),
            "answers 400");
        assertNotNull(build(builder()
            .setResourceKey(RESOURCE_KEY)
            .setLicenseKey(" ")
            .setProperty(" ")));
    }

    @Test
    public void Build_RefusalsDoNotContainTheLicenseKey() {
        PipelineConfigurationException e = assertThrows(
            PipelineConfigurationException.class,
            () -> build(builder().setLicenseKey(LICENSE_KEY)));
        assertFalse(e.getMessage().contains(LICENSE_KEY));
    }

    // Requests.

    @Test
    public void Process_LicenseKeyAlone_SendsLicenseAndValues()
        throws Exception {
        CloudRequestEngine engine = build(builder()
            .setLicenseKey(LICENSE_KEY + "\n")
            .setProperties(
                Arrays.asList(" Device.IsMobile", "device.iscrawler ")));

        assertEquals("", process(engine));

        Map<String, String> fields = dataRequest("/api/v4/json");
        assertEquals(LICENSE_KEY, fields.get("license"));
        assertEquals(PROPERTIES, fields.get("values"));
        assertFalse(fields.containsKey("resource"));
        assertFalse(fields.containsKey("licence"));
        assertEquals("iPhone", fields.get("User-Agent"));
    }

    @Test
    public void Process_ResourceKey_SendsResourceOnly() throws Exception {
        CloudRequestEngine engine = build(builder()
            .setResourceKey(RESOURCE_KEY));

        assertEquals("", process(engine));

        Map<String, String> fields = dataRequest(RESOURCE_KEY + ".json");
        assertEquals(RESOURCE_KEY, fields.get("resource"));
        assertFalse(fields.containsKey("license"));
        assertFalse(fields.containsKey("values"));
    }

    @Test
    public void Process_ResourceKeyAndLicenseKey_SendsBoth()
        throws Exception {
        CloudRequestEngine engine = build(builder()
            .setResourceKey(RESOURCE_KEY)
            .setLicenseKey(LICENSE_KEY));

        assertEquals("", process(engine));

        Map<String, String> fields = dataRequest(RESOURCE_KEY + ".json");
        assertEquals(RESOURCE_KEY, fields.get("resource"));
        assertEquals(LICENSE_KEY, fields.get("license"));
        assertFalse(fields.containsKey("values"));
    }

    @Test
    public void Process_ResourceKeyAndProperties_SendsResourceAndValues()
        throws Exception {
        cloud.dataResponse = "{\"device\":{\"ismobile\":true," +
            "\"hardwarevendor\":null," +
            "\"hardwarevendornullreason\":\"Paid feature\"}}";
        CloudRequestEngine engine = build(builder()
            .setResourceKey(RESOURCE_KEY)
            .setProperty("Device.HardwareVendor"));

        assertEquals("", process(engine));

        Map<String, String> fields = dataRequest(RESOURCE_KEY + ".json");
        assertEquals(RESOURCE_KEY, fields.get("resource"));
        assertEquals("device.hardwarevendor", fields.get("values"));
        assertFalse(fields.containsKey("license"));
        verify(logger, never()).warn(anyString());
    }

    @Test
    public void Process_AllThree_SendsResourceLicenseAndValues()
        throws Exception {
        CloudRequestEngine engine = build(builder()
            .setResourceKey(RESOURCE_KEY)
            .setLicenseKey(LICENSE_KEY)
            .setProperty("IP.RegisteredName"));
        cloud.dataResponse = "{\"device\":{\"ismobile\":true}," +
            "\"ip\":{\"registeredname\":\"Example\"}}";

        assertEquals("", process(engine));

        Map<String, String> fields = dataRequest(RESOURCE_KEY + ".json");
        assertEquals(RESOURCE_KEY, fields.get("resource"));
        assertEquals(LICENSE_KEY, fields.get("license"));
        assertEquals("ip.registeredname", fields.get("values"));
        for (Call call : cloud.calls) {
            assertFalse(call.url.toString().contains(LICENSE_KEY));
        }
    }

    // Discovery.

    @Test
    public void Discovery_LicenseKeyAlone_PostsCredentialsInTheBody()
        throws Exception {
        CloudRequestEngine engine = build(builder()
            .setLicenseKey(LICENSE_KEY)
            .setProperties(PROPERTY_LIST));

        Map<String, AccessiblePropertyMetaData.ProductMetaData> products =
            engine.getPublicProperties();
        assertTrue(products.containsKey("device"));
        assertNotNull(engine.getEvidenceKeyFilter());

        List<Call> properties = cloud.callsTo("accessibleproperties");
        assertEquals(1, properties.size());
        assertEquals("POST", properties.get(0).method);
        assertNull(properties.get(0).url.getQuery());
        assertFalse(properties.get(0).url.toString().contains(LICENSE_KEY));
        Map<String, String> fields = form(properties.get(0).body);
        assertEquals(LICENSE_KEY, fields.get("license"));
        assertFalse(fields.containsKey("resource"));

        List<Call> evidence = cloud.callsTo("evidencekeys");
        assertEquals(1, evidence.size());
        assertEquals("GET", evidence.get(0).method);
        assertNull(evidence.get(0).url.getQuery());
    }

    @Test
    public void Discovery_ResourceKeyAndProperties_GetsWithTheKeyInTheQuery()
        throws Exception {
        CloudRequestEngine engine = build(builder()
            .setResourceKey(RESOURCE_KEY)
            .setProperty("device.hardwarevendor"));

        engine.getPublicProperties();

        List<Call> properties = cloud.callsTo("accessibleproperties");
        assertEquals(1, properties.size());
        assertEquals("GET", properties.get(0).method);
        assertEquals(
            "Resource=" + RESOURCE_KEY,
            properties.get(0).url.getQuery());
    }

    @Test
    public void Discovery_BothKeys_PostsBothInTheBody() throws Exception {
        CloudRequestEngine engine = build(builder()
            .setResourceKey(RESOURCE_KEY)
            .setLicenseKey(LICENSE_KEY));

        engine.getPublicProperties();

        Call call = cloud.callsTo("accessibleproperties").get(0);
        assertEquals("POST", call.method);
        assertNull(call.url.getQuery());
        Map<String, String> fields = form(call.body);
        assertEquals(RESOURCE_KEY, fields.get("resource"));
        assertEquals(LICENSE_KEY, fields.get("license"));
    }

    // Keys in URLs, logs and messages.

    @Test
    public void Process_FailedRequest_LicenseKeyInNoUrlLogOrMessage()
        throws Exception {
        CloudRequestEngine engine = build(builder()
            .setLicenseKey(LICENSE_KEY)
            .setProperties(Arrays.asList(
                "device.ismobile", "device.other")));
        assertEquals("", process(engine));

        cloud.dataStatus = 500;
        cloud.dataResponse = "{\"status\":\"failure\"}";
        String errors = process(engine);

        assertTrue(errors.contains("500"), errors);
        assertFalse(errors.contains(LICENSE_KEY), errors);
        for (Call call : cloud.calls) {
            assertFalse(call.url.toString().contains(LICENSE_KEY));
        }
        assertFalse(mockingDetails(logger).getInvocations().isEmpty());
        mockingDetails(logger).getInvocations().forEach(i ->
            assertFalse(
                Arrays.deepToString(i.getArguments()).contains(LICENSE_KEY),
                i.toString()));
    }

    // Properties the service did not return.

    @Test
    public void Process_PropertyDropped_ReportedOnceAndRequestSucceeds()
        throws Exception {
        cloud.dataResponse = "{\"device\":{\"ismobile\":true}}";
        CloudRequestEngine engine = build(builder()
            .setLicenseKey(LICENSE_KEY)
            .setProperties(
                Arrays.asList("Device.IsMobile", "Device.IsCrawler")));

        assertEquals("", process(engine));
        assertEquals("", process(engine));

        verify(logger, times(1)).warn(argThat(
            (String m) -> m.contains("device.iscrawler") &&
                m.contains("not covered") &&
                !m.contains("device.ismobile")));
        verify(logger, times(1)).warn(anyString());
    }

    @Test
    public void Process_PropertyPresentButNull_CountsAsAnswered()
        throws Exception {
        cloud.dataResponse = "{\"device\":{\"ismobile\":true," +
            "\"iscrawler\":null,\"iscrawlernullreason\":\"No data\"}}";
        CloudRequestEngine engine = build(builder()
            .setLicenseKey(LICENSE_KEY)
            .setProperties(PROPERTY_LIST));

        assertEquals("", process(engine));

        verify(logger, never()).warn(anyString());
    }

    @Test
    public void Process_ResourceKey_NothingToCompare() throws Exception {
        cloud.dataResponse = "{\"device\":{\"ismobile\":true}}";
        CloudRequestEngine engine = build(builder()
            .setResourceKey(RESOURCE_KEY));

        assertEquals("", process(engine));

        verify(logger, never()).warn(anyString());
    }
}
