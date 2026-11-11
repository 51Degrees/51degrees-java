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

import fiftyone.common.testhelpers.TestLogger;
import fiftyone.pipeline.cloudrequestengine.CloudRequestException;
import fiftyone.pipeline.cloudrequestengine.Constants;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.exceptions.PropertyNotLoadedException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CloudRequestEngineTests extends CloudRequestEngineTestsBase {
    public CloudRequestEngineTests() throws MalformedURLException {
        super();
    }

    /**
     * Test cloud request engine adds correct information to post request
     * and returns the response in the ElementData
     */
    @Test
    public void Process() throws Exception {
        final String resourceKey = "resource_key";
        final String userAgent = "iPhone";

        configureMockedClient();

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey(resourceKey)
                .build();


        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", userAgent);
            data.process();

            String result = data.getFromElement(engine).getJsonResponse();
            assertEquals("{'device':{'value':'1'}}", result);

            JSONObject jsonObj = new JSONObject(result);
            assertEquals(1, jsonObj.getJSONObject("device").getInt("value"));

            verify(httpClient, times(1)) // we expected a single external POST request
                    .postData(
                            argThat(c -> {
                                return c.getURL().equals(expectedUrl); // to this uri
                            }),
                            ArgumentMatchers.anyMap(),
                            argThat(bytes -> {
                                String string = new String(bytes);
                                return string.contains("resource=" + resourceKey) && // content contains resource key
                                        string.contains("User-Agent=" + userAgent); // content contains user agent
                            }));
        }
    }

    /**
     * The cloud answers a refused license key with an error that names it.
     * The exception the engine raises, and every log entry, show the key
     * only by its last 4 characters.
     */
    @Test
    public void Process_LicenseKeyInCloudError_Masked() throws Exception {
        final String licenseKey = "TESTKEYNOTREALABCDEFGHIJ1234";
        final String masked = "************************1234";
        jsonResponse = "{\"errors\":[\"Invalid license key(s) supplied: " +
            licenseKey + "\"]}";
        configureMockedClient();
        Logger logger = mock(Logger.class);
        when(logger.isDebugEnabled()).thenReturn(true);
        when(logger.isInfoEnabled()).thenReturn(true);
        when(logger.isWarnEnabled()).thenReturn(true);
        when(logger.isErrorEnabled()).thenReturn(true);
        ILoggerFactory capturingLoggerFactory = mock(ILoggerFactory.class);
        when(capturingLoggerFactory.getLogger(anyString())).thenReturn(logger);

        CloudRequestEngine engine = new CloudRequestEngineBuilder(
                capturingLoggerFactory, httpClient)
                .setResourceKey("resource_key")
                .setLicenseKey(licenseKey)
                .build();

        try (Pipeline pipeline = new PipelineBuilder(capturingLoggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();

            assertFalse(data.getErrors().isEmpty());
            StringBuilder reported = new StringBuilder();
            for (FlowError error : data.getErrors()) {
                reported.append(printed(error.getThrowable()));
            }
            for (org.mockito.invocation.Invocation invocation :
                    mockingDetails(logger).getInvocations()) {
                for (Object argument : invocation.getArguments()) {
                    reported.append(argument instanceof Throwable ?
                            printed((Throwable) argument) :
                            String.valueOf(argument));
                }
            }
            assertFalse(reported.toString().contains(licenseKey),
                    reported.toString());
            assertTrue(reported.toString().contains(masked),
                    reported.toString());
        }
    }

    private static String printed(Throwable throwable) {
        java.io.StringWriter text = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(text));
        return text.toString();
    }

    /**
     * Test errors thrown by cloud request engine are added to flow.errors when SuppressProcessExceptions == true
     */
    @Test
    public void Process_SuppressProcessExceptions_Exceptions_Added_To_Errors() throws Exception {
        final String resourceKey = "resourcekey";
        final String userAgent = "iPhone";
        configureFailingMockClient();

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey(resourceKey)
                .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {

            data.addEvidence("query.User-Agent", userAgent);
            data.process();

            assertFalse(data.getErrors().isEmpty());
            ArrayList<FlowError> errors = (ArrayList<FlowError>) data.getErrors();
            Throwable throwable = errors.get(0).getThrowable();
            assertInstanceOf(IOException.class, throwable);
        }
    }

    /**
     * Test cloud request engine adds correct information to post request
     * and returns the response in the ElementData
     */
    @Test
    public void Process_LicenseKey() throws Exception {
        final String resourceKey = "resource_key";
        final String userAgent = "iPhone";
        final String licenseKey = "ABCDEFG";

        configureMockedClient();

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey(resourceKey)
                .setLicenseKey(licenseKey)
                .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", userAgent);

            data.process();

            String result = data.getFromElement(engine).getJsonResponse();
            assertEquals("{'device':{'value':'1'}}", result);

            JSONObject jsonObj = new JSONObject(result);
            assertEquals(1, jsonObj.getJSONObject("device").getInt("value"));
        }

        verify(httpClient, times(1)) // we expected a single external POST request
                .postData(
                        argThat(c -> {
                            return c.getURL().equals(expectedUrl); // to this uri
                        }),
                        ArgumentMatchers.anyMap(),
                        argThat(bytes -> {
                            String string = new String(bytes);
                            return string.contains("resource=" + resourceKey) && // content contains resource key
                                    string.contains("license=" + licenseKey) && // content contains license key
                                    string.contains("User-Agent=" + userAgent); // content contains user agent
                        }));
    }


    /**
     * Verify that the CloudRequestEngine can correctly parse a
     * response from the accessible properties endpoint that contains
     * meta-data for sub-properties.
     */
    @Test
    public void subProperties() throws Exception {
        accessiblePropertiesResponse =
                "{\n" +
                        "    \"Products\": {\n" +
                        "        \"device\": {\n" +
                        "            \"DataTier\": \"CloudV4TAC\",\n" +
                        "            \"Properties\": [\n" +
                        "                {\n" +
                        "                    \"Name\": \"IsMobile\",\n" +
                        "                        \"Type\": \"Boolean\",\n" +
                        "                        \"Category\": \"Device\"\n" +
                        "                },\n" +
                        "                {\n" +
                        "                    \"Name\": \"IsTablet\",\n" +
                        "                        \"Type\": \"Boolean\",\n" +
                        "                        \"Category\": \"Device\"\n" +
                        "                }\n" +
                        "            ]\n" +
                        "        },\n" +
                        "        \"devices\": {\n" +
                        "            \"DataTier\": \"CloudV4TAC\",\n" +
                        "            \"Properties\": [\n" +
                        "                {\n" +
                        "                    \"Name\": \"Devices\",\n" +
                        "                    \"Type\": \"Array\",\n" +
                        "                    \"Category\": \"Unspecified\",\n" +
                        "                    \"ItemProperties\": [\n" +
                        "                        {\n" +
                        "                            \"Name\": \"IsMobile\",\n" +
                        "                            \"Type\": \"Boolean\",\n" +
                        "                            \"Category\": \"Device\"\n" +
                        "                        },\n" +
                        "                        {\n" +
                        "                            \"Name\": \"IsTablet\",\n" +
                        "                            \"Type\": \"Boolean\",\n" +
                        "                            \"Category\": \"Device\"\n" +
                        "                        }\n" +
                        "                    ]\n" +
                        "                }\n" +
                        "            ]\n" +
                        "        }\n" +
                        "    }\n" +
                        "}";
        configureMockedClient();

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("key")
                .build();

        assertEquals(2, engine.getPublicProperties().size());
        AccessiblePropertyMetaData.ProductMetaData deviceProperties = engine.getPublicProperties().get("device");
        assertEquals(2, deviceProperties.properties.size());
        assertTrue(propertiesContainName(deviceProperties.properties, "IsMobile"));
        assertTrue(propertiesContainName(deviceProperties.properties, "IsTablet"));
        AccessiblePropertyMetaData.ProductMetaData devicesProperties = engine.getPublicProperties().get("devices");
        assertEquals(1, devicesProperties.properties.size());
        assertEquals("Devices", devicesProperties.properties.get(0).name);
        assertTrue(propertiesContainName(devicesProperties.properties.get(0).itemProperties, "IsMobile"));
        assertTrue(propertiesContainName(devicesProperties.properties.get(0).itemProperties, "IsTablet"));
    }

    private boolean propertiesContainName(
            List<AccessiblePropertyMetaData.PropertyMetaData> properties,
            String name) {
        for (AccessiblePropertyMetaData.PropertyMetaData property : properties) {
            if (property.name.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static Stream<Arguments> getValidateErrorArgs() {
        return Stream.of(
                Arguments.of("include message", 400, true),
                Arguments.of("no message", 400, false),
                Arguments.of("include message", 200, true));
    }

    /**
     * Errors from the cloud service when the engine is built. A 4xx answer
     * is a definitive configuration error, which fails the build with a
     * CloudRequestException that carries the cloud's message. Any other
     * answer that holds errors is logged as one warning, the build
     * completes, and the accessible properties are unknown until a retry.
     */
    @ParameterizedTest(name = "response code {1} - {0}")
    @MethodSource("getValidateErrorArgs")
    public void validateErrorHandling(String name, int responseCode, boolean includeMessage) throws Exception {
        final String resourceKey = "resource_key";
        String errorMessage = "some error message";
        if (includeMessage) {
            accessiblePropertiesResponse = "{ \"errors\":[\"" + errorMessage + "\"]}";
        }
        accessiblePropertiesResponseCode = responseCode;

        configureMockedClient();

        if (responseCode >= 400 && responseCode < 500) {
            CloudRequestException cloudEx = assertThrows(
                    CloudRequestException.class,
                    () -> new CloudRequestEngineBuilder(loggerFactory, httpClient)
                            .setResourceKey(resourceKey)
                            .build());
            assertEquals(responseCode, cloudEx.getHttpStatusCode());
            if (includeMessage) {
                assertTrue(cloudEx.getMessage().contains(errorMessage),
                        "Exception message did not contain the expected text.");
            }
        } else {
            CloudRequestEngine engine =
                    new CloudRequestEngineBuilder(loggerFactory, httpClient)
                            .setResourceKey(resourceKey)
                            .build();
            assertNull(engine.getPublicProperties());
            assertEquals(1, warningCount());
        }
    }

    private int warningCount() {
        int count = 0;
        for (TestLogger logger : loggerFactory.loggers) {
            count += logger.warningsLogged.size();
        }
        return count;
    }

    private static Stream<Arguments> getPrecidenceArgs() {
        return Stream.of(
                Arguments.of("query+header (no conflict)", false, "query.User-Agent=iPhone", "header.User-Agent=iPhone"),
                Arguments.of("query+cookie (no conflict)", false, "query.User-Agent=iPhone", "cookie.User-Agent=iPhone"),
                Arguments.of("header+cookie (conflict)", true, "header.User-Agent=iPhone", "cookie.User-Agent=iPhone"),
                Arguments.of("query+a (no conflict)", false, "query.value=1", "a.value=1"),
                Arguments.of("a+b (conflict)", true, "a.value=1", "b.value=1"),
                Arguments.of("e+f (conflict)", true, "e.value=1", "f.value=1"));
    }

    /**
     * Test cloud request engine adds correct information to post request
     * following the order of precedence when processing evidence and
     * returns the response in the ElementData. Evidence parameters
     * should be added in descending order of precedence.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getPrecidenceArgs")
    public void evidencePrecidence(String name, boolean shouldWarn, String evidence1, String evidence2) throws Exception {
        loggerFactory.loggers.clear();
        String[] evidence1Parts = evidence1.split("=");
        String[] evidence2Parts = evidence2.split("=");
        acceptEvidence(Arrays.asList(evidence1Parts[0], evidence2Parts[0]));
        configureMockedClient();

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();
             FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence(evidence1Parts[0], evidence1Parts[1]);

            flowData.addEvidence(evidence2Parts[0], evidence2Parts[1]);

            flowData.process();

            if (shouldWarn) {
                // If warn is expected then check for warnings from cloud request
                // engine.
                loggerFactory.assertMaxWarnings(1);
                String warning = "";
                for (TestLogger logger : loggerFactory.loggers) {
                    if (logger.warningsLogged.size() == 1) {
                        warning = logger.warningsLogged.get(0);
                        break;
                    }
                }
                assertEquals(
                        String.format(Constants.Messages.EvidenceConflict,
                                evidence1Parts[0],
                                evidence1Parts[1],
                                String.format("%s:%s", evidence2Parts[0], evidence2Parts[1])),
                        warning);
            } else {
                loggerFactory.assertMaxWarnings(0);
            }
        }
    }

    @SuppressWarnings("serial")
    private static Stream<Arguments> getSelectedEvidenceArgs() {
        return Stream.of(
                Arguments.of(
                        "query",
                        new HashMap<String, Object>() {{
                            put("query.User-Agent", "iPhone");
                            put("header.User-Agent", "iPhone");
                        }},
                        "query",
                        new HashMap<String, Object>() {{
                            put("query.User-Agent", "iPhone");
                        }}),
                Arguments.of(
                        "other",
                        new HashMap<String, Object>() {{
                            put("header.User-Agent", "iPhone");
                            put("a.User-Agent", "iPhone");
                            put("z.User-Agent", "iPhone");
                        }},
                        "other",
                        new HashMap<String, Object>() {{
                            put("z.User-Agent", "iPhone");
                            put("a.User-Agent", "iPhone");
                        }}));
    }

    /**
     * Test evidence of specific type is returned from all
     * the evidence passed, if type is not from query, header
     * or cookie then evidences are returned sorted in descensing order
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getSelectedEvidenceArgs")
    public void getSelectedEvidence(String name, Map<String, Object> evidence, String type, Map<String, Object> expectedValue) throws Exception {
        configureMockedClient();
        CloudRequestEngineDefault engine = (CloudRequestEngineDefault) new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();

        Map<String, Object> result = engine.getSelectedEvidence(evidence, type);

        assertEquals(expectedValue, result);
    }

    @SuppressWarnings("serial")
    private static Stream<Arguments> getFormDataArgs() {
        return Stream.of(
                Arguments.of(
                        "query > header",
                        new HashMap<String, Object>() {{
                            put("query.User-Agent", "query-iPhone");
                            put("header.User-Agent", "header-iPhone");
                        }},
                        "query-iPhone"),
                Arguments.of(
                        "header > cookie",
                        new HashMap<String, Object>() {{
                            put("header.User-Agent", "header-iPhone");
                            put("cookie.User-Agent", "cookie-iPhone");
                        }},
                        "header-iPhone"),
                Arguments.of(
                        "a > b > z",
                        new HashMap<String, Object>() {{
                            put("a.User-Agent", "a-iPhone");
                            put("b.User-Agent", "b-iPhone");
                            put("z.User-Agent", "z-iPhone");
                        }},
                        "a-iPhone"));
    }

    /**
     * Test Content to send in the POST request is generated as
     * per the precedence rule of The evidence keys. These are
     * added to the evidence in reverse order, if there is conflict then
     * the queryData value is overwritten.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getFormDataArgs")
    public void getFormData(String name, Map<String, Object> evidence, String expectedValue) throws Exception {
        acceptEvidence(evidence.keySet());
        configureMockedClient();
        CloudRequestEngineDefault engine = (CloudRequestEngineDefault) new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            for (Map.Entry<String, Object> entry : evidence.entrySet()) {
                data.addEvidence(entry.getKey(), entry.getValue());
            }

            Map<String, Object> result = engine.getFormData(data);
            assertEquals(expectedValue, result.get("User-Agent"));
        }
    }

    /**
     * The web integration adds the whole web request as server.http-request
     * for usage sharing to read. It holds every header and cookie, so it is
     * never sent to the cloud service, even where the service lists its key.
     */
    @Test
    public void getFormData_LeavesOutTheWebRequest() throws Exception {
        acceptEvidence(Arrays.asList(
            "header.User-Agent", "server.client-ip", "server.http-request"));
        configureMockedClient();
        CloudRequestEngineDefault engine = (CloudRequestEngineDefault) new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();

        Map<String, Object> request = new HashMap<>();
        request.put("header.Cookie", "session=secret");
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.User-Agent", "iPhone");
            data.addEvidence("server.client-ip", "1.2.3.4");
            data.addEvidence("server.http-request", request);

            Map<String, Object> result = engine.getFormData(data);
            assertEquals("iPhone", result.get("User-Agent"));
            assertEquals("1.2.3.4", result.get("client-ip"));
            assertFalse(result.containsKey("http-request"),
                "The web request was sent to the cloud service.");
        }
    }

    /**
     * An evidence key whose name itself holds a dot, such as
     * {@code query.id.usage}, must reach the cloud service under its whole
     * name, {@code id.usage}. The cloud service creates no 51Did unless
     * {@code id.usage} is sent, so cutting the name at its second dot
     * (sending {@code id} instead) means a server never gets a 51Did.
     */
    @Test
    public void getFormData_KeepsDotsInEvidenceName() throws Exception {
        acceptEvidence(Arrays.asList("query.id.usage", "header.id.email"));
        configureMockedClient();
        CloudRequestEngineDefault engine = (CloudRequestEngineDefault)
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.id.usage", "non-marketing");
            data.addEvidence("header.id.email", "someone@example.com");

            Map<String, Object> result = engine.getFormData(data);
            assertEquals("non-marketing", result.get("id.usage"));
            assertEquals("someone@example.com", result.get("id.email"));
            assertFalse(result.containsKey("id"),
                "The evidence name was cut at its second dot: " + result);
        }
    }

    /**
     * The body posted to the cloud service carries {@code id.usage} under
     * its whole name.
     */
    @Test
    public void process_PostsWholeEvidenceName() throws Exception {
        acceptEvidence(Arrays.asList("query.id.usage"));
        configureMockedClient();
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.id.usage", "non-marketing");
            data.process();
        }

        ArgumentCaptor<byte[]> body = ArgumentCaptor.forClass(byte[].class);
        verify(httpClient, times(1)).postData(
            argThat(c -> c.getURL().getPath().endsWith("properties") == false),
            any(),
            body.capture());
        List<String> items = Arrays.asList(
            new String(body.getValue(), StandardCharsets.UTF_8).split("&"));
        assertTrue(items.contains("id.usage=non-marketing"),
            "The posted body does not carry id.usage: " + items);
    }

    /**
     * Every Evidence value is sent, including Evidence the cloud service
     * does not list as accepted, apart from the reserved names under any
     * prefix.
     * See the Processing section of the cloud request engine specification.
     */
    @Test
    public void process_SendsEveryEvidenceValueButTheReservedNames()
        throws Exception {
        acceptEvidence(Arrays.asList("query.User-Agent"));
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("query.User-Agent", "iPhone");
        evidence.put("header.accept-language", "en-GB");
        evidence.put("header.X-Other", "other");
        evidence.put("cookie.session", "abc");
        evidence.put("server.client-ip", "1.2.3.4");
        evidence.put("other.VALUES", "ismobile");
        evidence.put("server.licence", "evidence-licence");

        List<String> items = postedItems(null, evidence);

        assertEquals(
            new HashSet<>(Arrays.asList(
                "resource=resourcekey",
                "User-Agent=iPhone",
                "accept-language=en-GB",
                "X-Other=other",
                "session=abc",
                "client-ip=1.2.3.4")),
            new HashSet<>(items),
            "The request did not carry every Evidence value apart from " +
                "the reserved names: " + items);
    }

    /**
     * Evidence whose key has no prefix is sent under its whole key, unless
     * the key is a reserved name in any letter case. Keys only usually
     * carry a prefix, as the Overview of the Evidence specification says.
     */
    @Test
    public void process_SendsEvidenceWithNoPrefixUnderItsWholeKey()
        throws Exception {
        acceptEvidence(Arrays.asList("query.User-Agent"));
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("query.User-Agent", "iPhone");
        evidence.put("bare", "value");
        evidence.put("Values", "ismobile");
        evidence.put("license", "evidence-license");
        evidence.put("RESOURCE", "evidence-resource");
        evidence.put("licence", "evidence-licence");

        List<String> items = postedItems(null, evidence);

        assertEquals(
            new HashSet<>(Arrays.asList(
                "resource=resourcekey",
                "User-Agent=iPhone",
                "bare=value")),
            new HashSet<>(items),
            "The request did not carry the Evidence with no prefix, or " +
                "carried a reserved name: " + items);
    }

    /**
     * Prefixes are compared without regard to case, so query Evidence
     * takes precedence over header Evidence of the same name, with no
     * warning, whatever the case of its prefix. Keys are case-insensitive,
     * as the Overview of the Evidence specification says.
     */
    @Test
    public void process_ComparesPrefixesWithoutRegardToCase()
        throws Exception {
        loggerFactory.loggers.clear();
        acceptEvidence(Arrays.asList("query.User-Agent"));
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("Query.User-Agent", "query-value");
        evidence.put("header.User-Agent", "header-value");

        List<String> items = postedItems(null, evidence);

        assertEquals(
            new HashSet<>(Arrays.asList(
                "resource=resourcekey",
                "User-Agent=query-value")),
            new HashSet<>(items),
            "The query Evidence did not take precedence: " + items);
        loggerFactory.assertMaxWarnings(0);
    }

    /**
     * Evidence with no prefix ranks with Evidence of any prefix other than
     * query, header and cookie, so it has the lowest precedence, unless the
     * whole key is one of those prefixes, which puts it with that prefix's
     * Evidence. The precedence order of the Evidence specification orders
     * prefixes only, so this follows .NET.
     */
    @Test
    public void process_RanksEvidenceWithNoPrefix() throws Exception {
        acceptEvidence(Arrays.asList("query.User-Agent"));
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("X-Thing", "bare");
        evidence.put("header.X-Thing", "header");
        evidence.put("query", "bare-query");
        evidence.put("header.query", "header-query");

        List<String> items = postedItems(null, evidence);

        assertEquals(
            new HashSet<>(Arrays.asList(
                "resource=resourcekey",
                "X-Thing=header",
                "query=bare-query")),
            new HashSet<>(items),
            "Evidence with no prefix was not ranked as .NET ranks it: " +
                items);
    }

    /**
     * Evidence whose key strips to resource, license or values is never
     * sent, whatever its case and even where the cloud service lists the
     * key, so the request carries the configured resource key, and the
     * license key where one is configured, once each.
     * See the Processing section of the cloud request engine specification.
     */
    @ParameterizedTest(name = "license key {0}")
    @NullSource
    @ValueSource(strings = {"ABCDEFG"})
    public void process_LeavesOutEvidenceNamedResourceLicenseOrValues(
        String licenseKey) throws Exception {
        acceptEvidence(Arrays.asList(
            "query.User-Agent",
            "query.resource",
            "header.Resource",
            "query.license",
            "cookie.LICENSE",
            "query.values"));
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("query.User-Agent", "iPhone");
        evidence.put("query.resource", "evidence-resource");
        evidence.put("header.Resource", "header-resource");
        evidence.put("query.license", "evidence-license");
        evidence.put("cookie.LICENSE", "cookie-license");
        evidence.put("query.values", "ismobile");

        List<String> items = postedItems(licenseKey, evidence);

        List<String> expected = new ArrayList<>();
        expected.add("resource=resourcekey");
        if (licenseKey != null) {
            expected.add("license=" + licenseKey);
        }
        expected.add("User-Agent=iPhone");
        assertEquals(expected, items);
    }

    /**
     * Evidence whose key strips to licence, whatever its case, is never
     * sent, because the cloud service reads the license key from licence
     * where license is absent.
     * See the Processing section of the cloud request engine specification.
     */
    @ParameterizedTest(name = "license key {0}")
    @NullSource
    @ValueSource(strings = {"ABCDEFG"})
    public void process_LeavesOutEvidenceNamedLicence(
        String licenseKey) throws Exception {
        acceptEvidence(Arrays.asList(
            "query.User-Agent",
            "query.licence",
            "cookie.LICENCE"));
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("query.User-Agent", "iPhone");
        evidence.put("query.licence", "evidence-licence");
        evidence.put("cookie.LICENCE", "cookie-licence");

        List<String> items = postedItems(licenseKey, evidence);

        List<String> expected = new ArrayList<>();
        expected.add("resource=resourcekey");
        if (licenseKey != null) {
            expected.add("license=" + licenseKey);
        }
        expected.add("User-Agent=iPhone");
        assertEquals(expected, items);
    }

    /**
     * A cache key is built from the Evidence the engine advertises, which is
     * still the keys the cloud service lists, so a second request that
     * differs only in Evidence the service does not list finds the entry
     * the first one wrote, and one request is posted. That request carries
     * the Evidence the service does not list all the same.
     * See the Generation of keys section of the caching specification.
     */
    @Test
    public void process_RepeatedRequestHitsTheCache() throws Exception {
        acceptEvidence(Arrays.asList("query.User-Agent"));
        configureMockedClient();
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .setCacheSize(10)
                .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build()) {
            for (String platform : Arrays.asList("Windows", "macOS")) {
                try (FlowData data = pipeline.createFlowData()) {
                    data.addEvidence("query.User-Agent", "iPhone");
                    data.addEvidence("header.sec-ch-ua-platform", platform);
                    data.process();
                }
            }
        }

        assertFalse(
            engine.getEvidenceKeyFilter().include("header.sec-ch-ua-platform"),
            "The engine advertises a key the cloud service does not list.");
        ArgumentCaptor<byte[]> body = ArgumentCaptor.forClass(byte[].class);
        verify(httpClient, times(1)).postData(
            argThat(c -> c.getURL().getPath().endsWith("properties") == false),
            any(),
            body.capture());
        List<String> items = Arrays.asList(
            new String(body.getValue(), StandardCharsets.UTF_8).split("&"));
        assertTrue(items.contains("sec-ch-ua-platform=Windows"),
            "The request did not carry the Evidence the cloud service does " +
                "not list: " + items);
    }

    /**
     * Answer the evidencekeys request with the given keys.
     * @param keys the Evidence keys the cloud service accepts
     */
    private void acceptEvidence(Collection<String> keys) {
        evidenceKeysResponse = new JSONArray(keys).toString();
    }

    /**
     * Process the Evidence with an engine on the resource key
     * {@code resourcekey} and return the fields of the one request it
     * posted, in order.
     * @param licenseKey the license key, or null for none
     * @param evidence the Evidence to process
     * @return the posted fields
     */
    private List<String> postedItems(
        String licenseKey,
        Map<String, Object> evidence) throws Exception {
        configureMockedClient();
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .setLicenseKey(licenseKey)
                .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidence);
            data.process();
        }

        ArgumentCaptor<byte[]> body = ArgumentCaptor.forClass(byte[].class);
        verify(httpClient, times(1)).postData(
            argThat(c -> c.getURL().getPath().endsWith("properties") == false),
            any(),
            body.capture());
        return Arrays.asList(
            new String(body.getValue(), StandardCharsets.UTF_8).split("&"));
    }

    /**
     * Verify that the request to the cloud service will contain
     * the configured origin header value.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void OriginHeader() throws Exception {
        final String resourceKey = "resource_key";
        final String userAgent = "iPhone";
        final String origin = "51degrees.com";

        configureMockedClient();

        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey(resourceKey)
                .setCloudRequestOrigin(origin)
                .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", userAgent);

            data.process();
        }

        ArgumentCaptor<Map<String, String>> argumentsCaptured = ArgumentCaptor.forClass(Map.class);

        verify(httpClient, times(1)) // we expected a single external POST request
                .postData(
                        argThat(c -> {
                            return c.getURL().equals(expectedUrl); // to this uri
                        }),
                        argumentsCaptured.capture(),
                        argThat(bytes -> {
                            String string = new String(bytes);
                            return string.contains("resource=" + resourceKey) && // content contains resource key
                                    string.contains("User-Agent=" + userAgent); // content contains user agent
                        }));

        // Verify that the origin header has the expected value.
        Map<String, String> headers = argumentsCaptured.getValue();
        assertTrue(headers.containsKey(Constants.OriginHeaderName));
        assertEquals(origin, headers.get(Constants.OriginHeaderName));
    }

    /**
     * Verify that the request to the cloud service will contain
     * the configured origin header value.
     */
    @Test
    public void HttpDataSetInException() throws Exception {
        final String resourceKey = "resource_key";

        try {
            new CloudRequestEngineBuilder(loggerFactory, new HttpClientDefault())
                    .setResourceKey(resourceKey)
                    .build()
                    .getPublicProperties();
            fail("Expected exception was not thrown");
        } catch (CloudRequestException ex) {
            assertTrue(ex.getHttpStatusCode() > 0, "Status code should not be 0");
            assertNotNull(ex.getResponseHeaders(), "Response headers not populated");
            assertTrue(ex.getResponseHeaders().size() > 0, "Response headers not populated");
        }
    }

    /**
     * Verify that resource key is set in propertiesEndPoint when
     * requested.
     */
    @Test
    public void getPublicProperties_Set_Resource_Key() {
        final String resourceKey = "resource_key";

        try {
            configureMockedClient();
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                    .setResourceKey(resourceKey)
                    .build()
                    .getPublicProperties();

            assertTrue(propertiesEndPoint.contains(resourceKey),
                    "Resource key is not set in properties endpoint.");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception was thrown");
        }
    }

    /**
     * Verify that getPublicProperties contains test data
     */
    @Test
    public void getPublicProperties() throws Exception {
        configureMockedClient();
        final String resourceKey = "resource_key";
        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey(resourceKey)
                .build();
        Map<String, AccessiblePropertyMetaData.ProductMetaData> properties = engine.getPublicProperties();
        assertTrue(properties.containsKey("device"));
    }

    /**
     * An unreachable server when the engine is built is a failure that can
     * pass. The build completes with one warning, and the accessible
     * properties are unknown until processing fetches them.
     */
    @Test
    public void getPublicProperties_Server_Unavailable_At_Build() throws Exception {
        final String resourceKey = "resource_key";
        configureFailingMockClient();

        CloudRequestEngine engine =
                new CloudRequestEngineBuilder(loggerFactory, httpClient)
                        .setResourceKey(resourceKey)
                        .build();

        assertNull(engine.getPublicProperties());
        assertEquals(1, warningCount());
    }

    /**
     * Verify that getEvidenceKeyFilter returns test data
     */
    @Test
    public void getEvidenceKeyFilter() throws Exception {
        configureMockedClient();
        final String resourceKey = "resource_key";
        CloudRequestEngine engine = new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey(resourceKey)
                .build();
        EvidenceKeyFilter evidenceKeyFilter = engine.getEvidenceKeyFilter();
        assertTrue(evidenceKeyFilter.include("query.User-Agent"));
    }

    /**
     * An unreachable server when the engine is built leaves the accepted
     * Evidence empty, rather than failing whatever reads it, until
     * processing fetches it.
     */
    @Test
    public void getEvidenceKeyFilter_Server_Unavailable_At_Build() throws Exception {
        final String resourceKey = "resource_key";
        configureFailingMockClient();

        EvidenceKeyFilter filter =
                new CloudRequestEngineBuilder(loggerFactory, httpClient)
                        .setResourceKey(resourceKey)
                        .build()
                        .getEvidenceKeyFilter();

        assertNotNull(filter);
        assertFalse(filter.include("query.User-Agent"));
        assertEquals(1, warningCount());
    }
}
