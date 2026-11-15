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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.common.testhelpers.LogbackHelper;
import fiftyone.common.testhelpers.TestLogger;
import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.DataUploader;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.trackers.Tracker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static fiftyone.pipeline.core.Constants.*;
import static fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
import static fiftyone.pipeline.engines.Constants.FIFTYONE_COOKIE_PREFIX;
import static org.junit.jupiter.api.Assertions.*;

public class ShareUsageElementTests {
    /**
     * Test implementation that outputs to byte array rather than HTTP
     */
    private static class TestConnector implements DataUploader {
        ByteArrayOutputStream baos= new ByteArrayOutputStream();
        private int code = 200;
        private int delay;

        @Override
        public OutputStream getOutputStream() {
            return baos;
        }
        @Override
        public int getResponseCode() {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {
            }
            return code;
        }

        public ByteArrayOutputStream getBaos() {
            return baos;
        }

        public void setResponseCode(int code) {
            this.code = code;
        }

        public void setResponseDelay(int delay) {
            this.delay = delay;
        }
    }

    /**
     * Mock HttpClient writes zipped data to a buffer
     */
    private static class TestHttpClient implements HttpClient {
        @Override
        public HttpURLConnection connect(URL url) {
            // mock HttpUrlConnection
            return new HttpURLConnection(url) {
                @Override
                public int getResponseCode()  {
                    return 200;
                }

                @Override
                public String getResponseMessage()  {
                    return "";
                }

                @Override
                public void disconnect() { }

                @Override
                public boolean usingProxy() {
                    return false;
                }

                @Override
                public void connect() {}
            };
        }
        byte[] buffer;
        @Override
        public String postData(HttpURLConnection connection, Map<String, String> headers, byte[] data)  {
            buffer = data;
            return "";
        }
        @Override
        public String getResponseString(HttpURLConnection connection)  {
            return "";
        }
        @Override
        public String getResponseString(HttpURLConnection connection, Map<String, String> headers)  {
            return "Happy Days";
        }

        public byte[] getBuffer(){
            return buffer;
        }
    }
    private static class TestTracker implements Tracker {
        @Override
        public boolean track(FlowData flowData) {
            return true;
        }
    }

    /**
     * How long a test consumer will linger before giving up waiting for the
     * element to be closed. Only reached if a test fails early.
     */
    private static final long LINGER_TIMEOUT_MILLIS = 30000;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    // Share usage instance that is being tested
    private ShareUsageElement shareUsageElement;

    // XML builder factory used to parse XML for validation.
    private final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    private final XPath xpath = XPathFactory.newInstance().newXPath();

    // Dependencies
    private Pipeline pipeline;
    private Tracker tracker;

    TestConnector connector;
    // Test instance data.
    private SequenceElement sequenceElement;

    @BeforeEach
    public void Init() throws Exception {

        // Configure the pipeline to return an empty list of flow elements
        pipeline = new PipelineBuilder().build();

        // Configure the tracker to always allow sharing.
        tracker = new TestTracker();
    }

    /**
     * The element runs its own sending thread, which closing it stops.
     * Closing an element a test has already closed does nothing more.
     */
    @AfterEach
    public void cleanUp() throws Exception {
        if (shareUsageElement != null) {
            shareUsageElement.close();
        }
        pipeline.close();
    }

    private void createShareUsage(
            double sharePercentage,
            int minimumEntriesPerMessage,
            @SuppressWarnings("SameParameterValue") int interval,
            List<String> blockedHeaders,
            List<String> includedQueryStringParams,
            List<Entry<String, String>> ignoreDataEvidenceFiler) {
        createShareUsage(
            sharePercentage,
            minimumEntriesPerMessage,
            minimumEntriesPerMessage * 2,
            interval,
            blockedHeaders,
            includedQueryStringParams,
            ignoreDataEvidenceFiler);
    }

    private void createShareUsage(
            double sharePercentage,
            int minimumEntriesPerMessage,
            int maximumQueueSize,
            @SuppressWarnings("SameParameterValue") int interval,
            List<String> blockedHeaders,
            List<String> includedQueryStringParams,
            List<Entry<String, String>> ignoreDataEvidenceFiler) {
        sequenceElement = new SequenceElement(LoggerFactory.getLogger(SequenceElement.class));
        sequenceElement.addPipeline(pipeline);
        shareUsageElement = new ShareUsageElement(
            LoggerFactory.getLogger(ShareUsageElement.class),
            sharePercentage,
            minimumEntriesPerMessage,
            maximumQueueSize,
            100,
            100,
            interval,
            true,
            "http://51Degrees.com/test",
            blockedHeaders,
            includedQueryStringParams,
            ignoreDataEvidenceFiler,
            DEFAULT_SESSION_COOKIE_NAME,
            tracker);
        shareUsageElement.addPipeline(pipeline);
        connector= new TestConnector();
        shareUsageElement.dataUploader = connector;
    }

    @Test
    public void ShareUsageElement_SingleEvent_ClientIPAndHeader() throws Exception {
        // Arrange
        createShareUsage(
                1,
                1,
                1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "x-forwarded-for", "5.6.7.8");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "forwarded-for", "2001::");
        evidenceData.put(EVIDENCE_COOKIE_PREFIX + EVIDENCE_SEPERATOR + FIFTYONE_COOKIE_PREFIX + "Profile", "123456");
        evidenceData.put(EVIDENCE_COOKIE_PREFIX + EVIDENCE_SEPERATOR + "RemoveMe", "123456");

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);
            // Act
            shareUsageElement.process(data);
            // Wait for the consumer task to finish.
            assertNotNull(shareUsageElement.getSendDataFuture());
            shareUsageElement.getSendDataFuture().get();

            ByteArrayOutputStream baos = connector.getBaos();
            byte[] output = baos.toByteArray();

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(output));

            NodeList devices = (NodeList) xpath.compile("//Devices/Device").evaluate(doc, XPathConstants.NODESET);
            assertEquals(1, devices.getLength(), "One device expected");

            NodeList headers = (NodeList) xpath.compile(EVIDENCE_HTTPHEADER_PREFIX).evaluate(devices.item(0), XPathConstants.NODESET);
            assertEquals(2, headers.getLength(), "2 headers expected");
            NodeList header1 = (NodeList) xpath.compile(EVIDENCE_HTTPHEADER_PREFIX+"[@Name='x-forwarded-for']").evaluate(devices.item(0), XPathConstants.NODESET);
            assertEquals("5.6.7.8",header1.item(0).getTextContent());
            NodeList header2 = (NodeList) xpath.compile(EVIDENCE_HTTPHEADER_PREFIX+"[@Name='forwarded-for']").evaluate(devices.item(0), XPathConstants.NODESET);
            assertEquals("2001::",header2.item(0).getTextContent());

            NodeList cookies = (NodeList) xpath.compile(EVIDENCE_COOKIE_PREFIX).evaluate(devices.item(0), XPathConstants.NODESET);
            assertEquals(1, cookies.getLength(), "1 cookie expected");
            assertEquals("51d_Profile", cookies.item(0).getAttributes().getNamedItem("Name").getTextContent());
            assertEquals("123456", cookies.item(0).getFirstChild().getTextContent());

            NodeList clientIp = (NodeList) xpath.compile("ClientIP").evaluate(devices.item(0), XPathConstants.NODESET);
            assertEquals(1, clientIp.getLength(), "1 client IP expected");
            assertEquals("1.2.3.4", clientIp.item(0).getTextContent());


        }
    }

    /**
     * Java 8 needs a utility to read from InputStream to OutputStream
     * @param source an input stream
     * @param target an output stream
     */
    private void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
    }

    // duplicate above test, only using legacy HttpClient approach
    @Test
    public void ShareUsageElement_SingleEvent_ClientIPAndHeader_HttpClient() throws Exception {
        // Arrange
        createShareUsage(
                1,
                1,
                1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        shareUsageElement.httpClient = new TestHttpClient();
        shareUsageElement.dataUploader = null;

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "x-forwarded-for", "5.6.7.8");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "forwarded-for", "2001::");
        evidenceData.put(EVIDENCE_COOKIE_PREFIX + EVIDENCE_SEPERATOR + FIFTYONE_COOKIE_PREFIX + "Profile", "123456");
        evidenceData.put(EVIDENCE_COOKIE_PREFIX + EVIDENCE_SEPERATOR + "RemoveMe", "123456");

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);
            // Act
            shareUsageElement.process(data);
            // Wait for the consumer task to finish.
            assertNotNull(shareUsageElement.getSendDataFuture());
            shareUsageElement.getSendDataFuture().get();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] zip = ((TestHttpClient)shareUsageElement.httpClient).getBuffer();
            // unzip the buffer
            copy(new GZIPInputStream(new ByteArrayInputStream(zip)), baos);
            byte[] output = baos.toByteArray();

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(output));

            NodeList devices = (NodeList) xpath.compile("//Devices/Device").evaluate(doc, XPathConstants.NODESET);
            assertEquals(1, devices.getLength(), "One device expected");

            NodeList headers = (NodeList) xpath.compile(EVIDENCE_HTTPHEADER_PREFIX).evaluate(devices.item(0), XPathConstants.NODESET);
            assertEquals(2, headers.getLength(), "2 headers expected");
            NodeList header1 = (NodeList) xpath.compile(EVIDENCE_HTTPHEADER_PREFIX+"[@Name='x-forwarded-for']").evaluate(devices.item(0), XPathConstants.NODESET);
            assertEquals("5.6.7.8",header1.item(0).getTextContent());
            NodeList header2 = (NodeList) xpath.compile(EVIDENCE_HTTPHEADER_PREFIX+"[@Name='forwarded-for']").evaluate(devices.item(0), XPathConstants.NODESET);
            assertEquals("2001::",header2.item(0).getTextContent());

            NodeList cookies = (NodeList) xpath.compile(EVIDENCE_COOKIE_PREFIX).evaluate(devices.item(0), XPathConstants.NODESET);
            assertEquals(1, cookies.getLength(), "1 cookie expected");
            assertEquals("51d_Profile", cookies.item(0).getAttributes().getNamedItem("Name").getTextContent());
            assertEquals("123456", cookies.item(0).getFirstChild().getTextContent());

            NodeList clientIp = (NodeList) xpath.compile("ClientIP").evaluate(devices.item(0), XPathConstants.NODESET);
            assertEquals(1, clientIp.getLength(), "1 client IP expected");
            assertEquals("1.2.3.4", clientIp.item(0).getTextContent());


        }
    }

    @Test
    public void ShareUsageElement_TwoEvents_FirstEvent() throws Exception {
        // Arrange
        createShareUsage(
            1,
            2,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            // Act
            shareUsageElement.process(data);

            // sending of data never gets triggered
            assertNull(shareUsageElement.getSendDataFuture());
        }
    }

    @Test
    public void ShareUsageElement_TwoEvents_SecondEvent() throws Exception {
        // Arrange
        createShareUsage(
            1,
            2,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            // Act
            shareUsageElement.process(data);
            shareUsageElement.process(data);

            // Wait for the consumer task to finish.
            assertNotNull(shareUsageElement.getSendDataFuture());
            shareUsageElement.getSendDataFuture().get();

            ByteArrayOutputStream baos = connector.getBaos();
            byte[] output = baos.toByteArray();

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(output));

            // Verify that there is only one devices element
            NodeList devices = (NodeList) xpath.compile("//Devices").evaluate(doc, XPathConstants.NODESET);
            assertEquals(1, devices.getLength(), "1 devices node expected");

            // Make sure there are 2 'Device' nodes
            NodeList device = (NodeList) xpath.compile("//Device").evaluate(devices.item(0), XPathConstants.NODESET);
            assertEquals(2, device.getLength(), "2 device nodes expected");
        }
    }

    @Test
    public void ShareUsageElement_RestrictedHeaders() throws Exception {
        // Arrange
        createShareUsage(
            1,
            1,
            1,
            new ArrayList<>(Arrays.asList("x-forwarded-for", "forwarded-for")),
                new ArrayList<>(),
                new ArrayList<>());

        String useragent = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0 Mozilla/5.0 (Macintosh; Intel Mac OS X x.y; rv:42.0) Gecko/20100101 Firefox/42.0.";
        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "x-forwarded-for", "5.6.7.8");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "forwarded-for", "2001::");
        evidenceData.put(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "user-agent", useragent);

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);
            // Act
            shareUsageElement.process(data);
            // Wait for the consumer task to finish.
            assertNotNull(shareUsageElement.getSendDataFuture());
            shareUsageElement.getSendDataFuture().get();

            ByteArrayOutputStream baos = connector.getBaos();
            byte[] output = baos.toByteArray();

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(output));

            // Verify that there is only one clientIP element
            NodeList clientIP = (NodeList) xpath.compile("//ClientIP").evaluate(doc, XPathConstants.NODESET);
            assertEquals(1, clientIP.getLength(), "1 ClientIP node expected");

            // Make sure there is a header node
            NodeList device = (NodeList) xpath.compile("//header[@Name='user-agent']").evaluate(doc, XPathConstants.NODESET);
            assertEquals(1, device.getLength(), "1 user-agent header expected");
            // Make sure no x-forwarded-for
            NodeList xf = (NodeList) xpath.compile("//header[@Name='x-forwarded-for']").evaluate(doc, XPathConstants.NODESET);
            assertEquals(0, xf.getLength(), "0 x-forwarded-for header expected");
            // Make sure no forwarded-for
            NodeList f = (NodeList) xpath.compile("//header[@Name='forwarded-for']").evaluate(doc, XPathConstants.NODESET);
            assertEquals(0, f.getLength(), "0 forwarded-for header expected");
        }
    }

    /**
     * The element accepts no Evidence, so a header no other element accepts
     * reaches it only through the web request the web integration adds as
     * server.http-request. It shares that header under the key it would have
     * as Evidence, keeps the Evidence value for a key in both places, and
     * still leaves out the cookie header, a blocked header and the web
     * request itself.
     */
    @Test
    public void ShareUsageElement_WebRequest_SharesWhatNoOtherElementAccepts()
        throws Exception {
        createShareUsage(
            1,
            1,
            1,
            new ArrayList<>(Collections.singletonList("x-blocked")),
            new ArrayList<>(),
            new ArrayList<>());

        Map<String, Object> request = new HashMap<>();
        request.put("header.User-Agent", "request-ua");
        request.put("header.X-Test", "shared");
        request.put("header.Cookie", "a=1; 51D_Profile=123456");
        request.put("header.X-Blocked", "blocked");
        request.put("cookie.51D_Profile", "123456");
        request.put("cookie.other", "not-shared");
        request.put("query.secret", "not-shared");
        request.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        request.put("server.request-method", "GET");
        request.put("server.request-path", "/a%2Fb/c");

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(
                EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR + "user-agent",
                "evidence-ua");
            data.addEvidence("server.http-request",
                Collections.unmodifiableMap(request));
            shareUsageElement.process(data);
            assertNotNull(shareUsageElement.getSendDataFuture());
            shareUsageElement.getSendDataFuture().get();

            Document doc = docBuilderFactory.newDocumentBuilder().parse(
                new ByteArrayInputStream(connector.getBaos().toByteArray()));

            assertEquals("shared", xpath.evaluate(
                "//header[@Name='X-Test']", doc),
                "A header only the web request holds was not shared.");
            NodeList userAgents = (NodeList) xpath.evaluate(
                "//header[translate(@Name,'USERAGNT','useragnt')='user-agent']",
                doc, XPathConstants.NODESET);
            assertEquals(1, userAgents.getLength(),
                "The User-Agent in both places was not shared once.");
            assertEquals("evidence-ua", userAgents.item(0).getTextContent(),
                "The User-Agent in both places did not keep the Evidence value.");
            assertEquals(0, ((NodeList) xpath.evaluate(
                "//header[@Name='Cookie' or @Name='cookie']", doc,
                XPathConstants.NODESET)).getLength(),
                "The cookie header was shared.");
            assertEquals(0, ((NodeList) xpath.evaluate(
                "//header[@Name='X-Blocked']", doc,
                XPathConstants.NODESET)).getLength(),
                "A blocked header was shared.");
            assertEquals("123456", xpath.evaluate(
                "//cookie[@Name='51D_Profile']", doc),
                "A 51D_ cookie from the web request was not shared.");
            assertEquals(0, ((NodeList) xpath.evaluate(
                "//cookie[@Name='other'] | //query[@Name='secret']", doc,
                XPathConstants.NODESET)).getLength(),
                "A cookie or query string parameter the rules leave out was shared.");
            assertEquals("1.2.3.4", xpath.evaluate("//ClientIP", doc),
                "The client IP from the web request was not shared.");
            assertEquals("GET", xpath.evaluate(
                "//server[@Name='request-method']", doc),
                "The method of the request line was not shared.");
            assertEquals(0, ((NodeList) xpath.evaluate(
                "//*[@Name='request-path']", doc,
                XPathConstants.NODESET)).getLength(),
                "The path of the request line was shared.");
            assertEquals(0, ((NodeList) xpath.evaluate(
                "//*[@Name='http-request']", doc,
                XPathConstants.NODESET)).getLength(),
                "The web request itself was shared.");
        }
    }

    @Test
    public void ShareUsageElement_LowPercentage() throws Exception {
        // Arrange
        createShareUsage(
            0.001,
            100,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            // Act
            int requiredEvents = 0;
            while (connector.getBaos().size()==0 && requiredEvents <= 1000000) {
                shareUsageElement.process(data);
                requiredEvents++;
            }
            // Wait for the consumer task to finish.
            assertNotNull(shareUsageElement.getSendDataFuture());
            shareUsageElement.getSendDataFuture().get();

            logger.info("Required events was {}", requiredEvents);

            // Assert
            // On average, the number of required events should be around
            // 100,000. However, as it's chance based it can vary
            // significantly. We only want to catch any gross errors so just
            // make sure the value is of the expected order of magnitude.
            assertTrue(requiredEvents > 10000,
                    "Expected the number of required events to be at least " +
                            "10,000, but was actually '" + requiredEvents + "'");
            assertTrue(requiredEvents < 1000000,
                    "Expected the number of required events to be less than " +
                            "1,000,000, but was actually '" + requiredEvents + "'");
        }
    }
    @Test
    public void ShareUsageElement_SendOnCleanup() throws Exception {
        // Arrange
        createShareUsage(
            1,
            2,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            // Act
            shareUsageElement.process(data);

            // No data should be being sending yet.
            assertNull(shareUsageElement.getSendDataFuture());

            // Dispose of the element.
            shareUsageElement.close();

            // Assert
            // data should have been sent
            assertNotNull(shareUsageElement.getSendDataFuture());
            assertTrue(shareUsageElement.isClosed());

            ByteArrayOutputStream baos = connector.getBaos();
            byte[] output = baos.toByteArray();

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(output));

            // Check that the expected values are populated.
            NodeList f = (NodeList) xpath.compile("//Device").evaluate(doc, XPathConstants.NODESET);
            assertEquals(1, f.getLength(), "1 device expected");

        }
    }
    @Test
    public void ShareUsageElement_CancelOnServerError() throws Exception {
        logger.info("Test intentionally creates errors");
        LogbackHelper.intentionalErrorConfig();
        // Arrange
        createShareUsage(
            1,
            1,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        ((TestConnector)shareUsageElement.dataUploader).setResponseCode(500);
        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            shareUsageElement.process(data);
            // Wait for the consumer task to finish.
            assertNotNull(shareUsageElement.getSendDataFuture());
            shareUsageElement.getSendDataFuture().get();

            // Assert
            assertFalse(shareUsageElement.isCanceled());
            assertEquals(0, shareUsageElement.evidenceCollection.size());
        }
    }

    @Test
    public void ShareUsageElement_IgnoreOnEvidence() throws Exception {
        // Arrange
        createShareUsage(
            1,
            1,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                Collections.singletonList(
                        new AbstractMap.SimpleEntry<>("header.User-Agent", "Azure Traffic Manager Endpoint Monitor")
                ));

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put("header.User-Agent", "Azure Traffic Manager Endpoint Monitor");

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            // Act
            shareUsageElement.process(data);
            // Check that the consumer task did not start.
            assertNull(shareUsageElement.getSendDataFuture());

            assertEquals(0, connector.getBaos().size());
        }
    }

    @Test  @Disabled // we now throw exceptions for invalid data
    public void ShareUsageBuilder_IgnoreData_InvalidFilter() throws IOException {
        logger.info("The following warning are part of the test:");
        for (String config : new String[]{"user-agent=iPhone", "user-agent,iPhone", "test,iPhone,block"}) {
            ILoggerFactory internalLoggerFactory = LoggerFactory.getILoggerFactory();
            TestLoggerFactory loggerFactory = new TestLoggerFactory(internalLoggerFactory);
            TestLogger logger = new TestLogger("test", LoggerFactory.getLogger("test"));

            ShareUsageBuilder builder = new ShareUsageBuilder(loggerFactory, logger);
            ShareUsageElement element = builder
                .setSharePercentage(1)
                .setMinimumEntriesPerMessage(1)
                .setRepeatEvidenceIntervalMinutes(1)
                .setIgnoreFlowDataEvidenceFilter(config)
                .build();
            assertNotNull(element);

            assertTrue(logger.warningsLogged.size() > 0);
            assertEquals(0, logger.errorsLogged.size());
        }

    }

    @Test
    public void ShareUsageBuilder_IgnoreData_ValidFilter() throws IOException {
        for (String config : new String[]{"user-agent:iPhone", "user-agent:iPhone,host:bacon.com", "user-agent:iPhone,host:bacon.com,license:ABCDEF"}) {
            ILoggerFactory internalLoggerFactory = LoggerFactory.getILoggerFactory();
            TestLoggerFactory loggerFactory = new TestLoggerFactory(internalLoggerFactory);
            TestLogger logger = new TestLogger("test", LoggerFactory.getLogger("test"));


            ShareUsageBuilder builder = new ShareUsageBuilder(loggerFactory, logger);
            ShareUsageElement element = builder
                .setSharePercentage(1)
                .setMinimumEntriesPerMessage(1)
                .setRepeatEvidenceIntervalMinutes(1)
                .setIgnoreFlowDataEvidenceFilter(config)
                .build();
            assertNotNull(element);

            assertEquals(0, logger.warningsLogged.size());
            assertEquals(0, logger.errorsLogged.size());
        }
    }



    /**
     * Check that the usage element can handle invalid xml chars.
     */

    @Test
    public void ShareUsageElement_BadSchema() throws Exception {
        // Arrange
        createShareUsage(1, 1, 1, new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();

        // Contains XML characters and illegal character.
        // (0x0018) - Cancel control character
        evidenceData.put(EVIDENCE_HEADER_USERAGENT_KEY, "<iPhone\u0018>");

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            // Act
            shareUsageElement.process(data);
            // Wait for the consumer task to finish.
            assertNotNull(shareUsageElement.getSendDataFuture());
            shareUsageElement.getSendDataFuture().get();

            ByteArrayOutputStream baos = connector.getBaos();
            byte[] output = baos.toByteArray();

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(output));

            // Check that the expected values are populated.
            NodeList f = (NodeList) xpath.compile("//header[@Name='user-agent']").evaluate(doc, XPathConstants.NODESET);
            assertEquals(1, f.getLength(), "1 user-agent header expected");
            assertEquals("<iPhone\\x0018>", f.item(0).getTextContent());
            assertEquals("true", f.item(0).getAttributes().getNamedItem("escaped").getTextContent());
            NodeList b = (NodeList) xpath.compile("//BadSchema").evaluate(doc, XPathConstants.NODESET);
            assertEquals(0, b.getLength());
        }
    }
    
    /**
     * Check that the invalid characters in a value are marked with the
     * attribute the specification names, and that a value that is not too
     * long is not marked as truncated.
     */
    @Test
    public void ShareUsageElement_InvalidCharacter_EscapedAttributeOnly()
        throws Exception {
        Document doc = shareUserAgent("<iPhone" + (char) 0x18 + ">");

        Node userAgent = doc.getElementsByTagName("header").item(0);
        assertEquals("true",
            userAgent.getAttributes().getNamedItem("escaped").getTextContent());
        assertNull(userAgent.getAttributes().getNamedItem("replaced"));
        assertNull(userAgent.getAttributes().getNamedItem("truncated"));
        assertEquals("<iPhone\\x0018>", userAgent.getTextContent());
    }

    /**
     * Check that a value that is too long is cut and marked as truncated,
     * without the escaped attribute.
     */
    @Test
    public void ShareUsageElement_LongValue_TruncatedAttributeOnly()
        throws Exception {
        StringBuilder longValue = new StringBuilder();
        for (int i = 0; i < 10001; i++) {
            longValue.append('a');
        }
        Document doc = shareUserAgent(longValue.toString());

        Node userAgent = doc.getElementsByTagName("header").item(0);
        assertEquals("true",
            userAgent.getAttributes().getNamedItem("truncated").getTextContent());
        assertNull(userAgent.getAttributes().getNamedItem("escaped"));
        assertEquals(10000, userAgent.getTextContent().length());
    }

    /**
     * Check that the values are written in the order the specification's
     * example gives, and that no CDATA section or BadSchema element is used.
     */
    @Test
    public void ShareUsageElement_RecordFollowsTheSpecificationExample()
        throws Exception {
        Document doc = shareUserAgent("<iPhone>");

        Node device = doc.getElementsByTagName("Device").item(0);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < device.getChildNodes().getLength(); i++) {
            Node child = device.getChildNodes().item(i);
            assertNotEquals(Node.CDATA_SECTION_NODE, child.getNodeType());
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                names.add(child.getNodeName());
            }
        }
        String[] expected = {"SessionId", "Sequence", "DateSent", "Version",
            "Product", "FlowElement", "Language", "LanguageVersion",
            "ClientIP", "ServerIP", "Platform"};
        List<String> required = Arrays.asList("SessionId", "Sequence",
            "DateSent", "Product", "Language", "LanguageVersion");
        int last = -1;
        for (String name : expected) {
            int index = names.indexOf(name);
            if (!required.contains(name)) {
                // Only present when the Pipeline, the request and the
                // platform give one.
                if (index < 0) {
                    continue;
                }
            }
            assertTrue(index >= 0, name + " missing from " + names);
            assertTrue(index > last, name + " out of order in " + names);
            last = index;
        }
        assertEquals(0, doc.getElementsByTagName("BadSchema").getLength());
        assertEquals(1, doc.getElementsByTagName("header").getLength());
    }

    private Document shareUserAgent(String userAgent) throws Exception {
        createShareUsage(1, 1, 1, new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());
        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_HEADER_USERAGENT_KEY, userAgent);
        evidenceData.put("query.session-id", "session");
        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);
            shareUsageElement.process(data);
            assertNotNull(shareUsageElement.getSendDataFuture());
            shareUsageElement.getSendDataFuture().get();
            byte[] output = connector.getBaos().toByteArray();
            return docBuilderFactory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(output));
        }
    }

/**
     * Test that the ShareUsageElement generates a session id if one is not
     * contained in the evidence and adds it to the results.
     */

    @Test
    public void ShareUsageElement_SessionIdAndSequence_None() throws Exception {
        // Arrange
        createShareUsage(
            1,
            1,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            // Act
            sequenceElement.process(data);
            shareUsageElement.process(data);

            // Wait for the consumer task to finish.
            assertNotNull(shareUsageElement.getSendDataFuture());
            shareUsageElement.getSendDataFuture().get();

            ByteArrayOutputStream baos = connector.getBaos();
            byte[] output = baos.toByteArray();

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(output));

            NodeList f = (NodeList) xpath.compile("//SessionId").evaluate(doc, XPathConstants.NODESET);
            assertEquals(1, f.getLength(), "Expecting 1 session ID");
            NodeList ff = (NodeList) xpath.compile("//Sequence").evaluate(doc, XPathConstants.NODESET);
            assertEquals("1", ff.item(0).getTextContent(), "Expecting Sequence ID 1");
        }
    }
    
/**
     * Test that if a session id and sequence exists in the evidence the 
     * ShareUsageElement persists the session id and increments the 
     * sequence.
     */

    @Test
    public void ShareUsageElement_SessionIdAndSequence_Existing() throws Exception {
        // Arrange
        createShareUsage(
            1,
            1,
            1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put(Constants.EVIDENCE_SESSIONID, "abcdefg-hijklmn-opqrst-uvwxyz");
        evidenceData.put(Constants.EVIDENCE_SEQUENCE, 2);

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            // Act
            sequenceElement.process(data);
            shareUsageElement.process(data);

            // Wait for the consumer task to finish.
            assertNotNull(shareUsageElement.getSendDataFuture());
            shareUsageElement.getSendDataFuture().get();

            ByteArrayOutputStream baos = connector.getBaos();
            byte[] output = baos.toByteArray();

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(output));

            NodeList f = (NodeList) xpath.compile("//SessionId").evaluate(doc, XPathConstants.NODESET);
            assertEquals(1, f.getLength(), "Expecting 1 session ID");
            assertEquals("abcdefg-hijklmn-opqrst-uvwxyz", f.item(0).getTextContent(), "Expecting session ID to be ...");
            NodeList ff = (NodeList) xpath.compile("//Sequence").evaluate(doc, XPathConstants.NODESET);
            assertEquals("3", ff.item(0).getTextContent(), "Expecting Sequence ID 1");
        }
    }

    /**
     * test for earlier bug where 100 out of 101 usages were shared (with min buffer of 50)
     * plus make sure to send buffer on close down
     */
    @Test
    public void CheckForOffByOneError() throws Exception {
        int count = 45;
        // The queue holds every entry. With the usual 20, a first send slower
        // than the 100ms add timeout - a cold JVM on a slow runner - leaves
        // the queue full, and the element drops what it cannot queue. That is
        // the element shedding load as designed, not the off-by-one this
        // tests, and it failed this test with 40 of 45.
        createShareUsage(
                1,
                10,
                count,
                1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            // collect multiple <Devices> documents
            connector.getBaos().write("<allDevices>".getBytes(StandardCharsets.UTF_8));
            // Act
            for (int i=0; i < count; i++) {
                shareUsageElement.process(data);
            }
            // Wait for the consumer task to finish.
            shareUsageElement.close();

            ByteArrayOutputStream baos = connector.getBaos();
            baos.write("</allDevices>".getBytes(StandardCharsets.UTF_8));
            byte[] output = baos.toByteArray();

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(output));

            NodeList f = (NodeList) xpath.compile("//Device").evaluate(doc, XPathConstants.NODESET);
            assertEquals(count, f.getLength(), "Expecting devices");

        }
    }
    /**
     * A consumer that has finished its drain loop with fewer entries left than
     * the minimum batch size still reports as running until its task completes.
     * close() must send that remainder rather than deciding, from the state of
     * the task, that a consumer is already dealing with it.
     * This is the deterministic form of the race that makes
     * {@link #CheckForOffByOneError()} flaky on slower CI runners.
     */
    @Test
    public void ShareUsageElement_PartialBatchWhileConsumerFinishing_SentOnCleanup() throws Exception {
        final int minimumEntriesPerMessage = 10;
        final int remainder = 5;
        final CountDownLatch consumerHasDrainedQueue = new CountDownLatch(1);

        sequenceElement = new SequenceElement(LoggerFactory.getLogger(SequenceElement.class));
        sequenceElement.addPipeline(pipeline);
        shareUsageElement = new ShareUsageElement(
                LoggerFactory.getLogger(ShareUsageElement.class),
                1,
                minimumEntriesPerMessage,
                minimumEntriesPerMessage * 2,
                100,
                100,
                1,
                true,
                "http://51Degrees.com/test",
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                DEFAULT_SESSION_COOKIE_NAME,
                tracker) {
            @Override
            protected void sendUsageData() {
                super.sendUsageData();
                consumerHasDrainedQueue.countDown();
                // Hold the task open after its drain loop has finished, so that
                // it is still 'running' at the point where close decides
                // whether a final consumer is needed. Releasing on shutdown
                // means no second thread is needed to drive the test. The
                // deadline stops the executor's non daemon thread spinning for
                // ever if the test fails before it reaches close.
                long giveUpTime = System.currentTimeMillis() + LINGER_TIMEOUT_MILLIS;
                while (executor.isShutdown() == false
                        && System.currentTimeMillis() < giveUpTime) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException interruptedException) {
                        break;
                    }
                }
            }
        };
        shareUsageElement.addPipeline(pipeline);
        connector = new TestConnector();
        shareUsageElement.dataUploader = connector;

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            // collect multiple <Devices> documents
            connector.getBaos().write("<allDevices>".getBytes(StandardCharsets.UTF_8));

            // close in a finally as well, so that an assertion failure before the
            // Act step below still releases the lingering consumer.
            try {
                // Send one full batch and wait for the consumer to drain it.
                for (int i = 0; i < minimumEntriesPerMessage; i++) {
                    shareUsageElement.process(data);
                }
                assertTrue(consumerHasDrainedQueue.await(LINGER_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS),
                        "The consumer should have drained the first batch");

                // Queue too few entries to start a consumer of their own, while the
                // previous consumer's task has not completed yet.
                for (int i = 0; i < remainder; i++) {
                    shareUsageElement.process(data);
                }
                assertEquals(remainder, shareUsageElement.evidenceCollection.size());
                assertTrue(shareUsageElement.isRunning(),
                        "The first consumer should still be reported as running");

                // Act - close must flush the remainder.
                shareUsageElement.close();
                assertEquals(0, shareUsageElement.evidenceCollection.size(),
                        "close should leave nothing queued");
            } finally {
                // Swallowed so that a failure above is still the reported one - the
                // close in the Act step is the one under test.
                try {
                    shareUsageElement.close();
                } catch (Exception exception) {
                    logger.warn("Exception closing the element under test", exception);
                }
            }

            ByteArrayOutputStream baos = connector.getBaos();
            baos.write("</allDevices>".getBytes(StandardCharsets.UTF_8));
            byte[] output = baos.toByteArray();

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(output));

            NodeList f = (NodeList) xpath.compile("//Device").evaluate(doc, XPathConstants.NODESET);
            assertEquals(minimumEntriesPerMessage + remainder, f.getLength(), "Expecting devices");
        }
    }

    @Test
    public void CheckForDrainQueueEvenIfError() throws Exception {
        logger.info("Test intentionally creates errors");
        LogbackHelper.intentionalErrorConfig();
        int count = 45;
        // The queue holds every entry, for the reason given in
        // CheckForOffByOneError.
        createShareUsage(
                1,
                10,
                count,
                1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        // each send fails
        ((TestConnector)shareUsageElement.dataUploader).setResponseCode(500);

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            // collect multiple <Devices> documents
            connector.getBaos().write("<allDevices>".getBytes(StandardCharsets.UTF_8));
            // Act
            for (int i=0; i < count; i++) {
                shareUsageElement.process(data);
            }
            // Wait for the consumer task to finish.
            shareUsageElement.close();

            ByteArrayOutputStream baos = connector.getBaos();
            baos.write("</allDevices>".getBytes(StandardCharsets.UTF_8));
            byte[] output = baos.toByteArray();

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(output));

            NodeList f = (NodeList) xpath.compile("//Device").evaluate(doc, XPathConstants.NODESET);
            // this shows that sending is ignoring the failures
            assertEquals(count, f.getLength(), "Expecting devices");

        }
    }
    @Test
    public void CheckForContinueAndResumeWhenQueueFull() throws Exception {
        logger.info("Test intentionally creates WARNs");
        createShareUsage(
                1,
                10,
                1,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>());

        // add delay to the response so queue grows
        ((TestConnector)shareUsageElement.dataUploader).setResponseDelay(3000);

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            // the output document will collect multiple <Devices> documents,
            // so we need to wrap them in a root element
            connector.getBaos().write("<allDevices>".getBytes(StandardCharsets.UTF_8));

            // Overwhelm the queue
            int count = 55;
            for (int i=0; i < count; i++) {
                shareUsageElement.process(data);
            }
            long amountOfLostData;
            assertTrue((amountOfLostData = shareUsageElement.lostData) > 0);
            // wait for queue to drain
            logger.info("Sleeping");
            Thread.sleep(5000);

            // send a new element which should reset the flag
            shareUsageElement.process(data);
            assertEquals(amountOfLostData, shareUsageElement.lostData);

            // overwhelm the queue again
            for (int i=0; i < count; i++) {
                shareUsageElement.process(data);
            }
            assertTrue(amountOfLostData < shareUsageElement.lostData);

            // Wait for the consumer task to finish.
            shareUsageElement.close();

            // get the data output
            ByteArrayOutputStream baos = connector.getBaos();
            baos.write("</allDevices>".getBytes(StandardCharsets.UTF_8));
            byte[] output = baos.toByteArray();

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(output));

            NodeList f = (NodeList) xpath.compile("//Device").evaluate(doc, XPathConstants.NODESET);
            // the count is what we put in minus what we have recorded as lost
            assertEquals(2 * count + 1 - shareUsageElement.lostData, f.getLength(), "Expecting devices");

        }
    }

    /**
     * Default settings share headers other than Cookie, the 51Degrees cookies
     * and query string parameters, and other evidence, and leave out ordinary
     * cookies, other parameters, session values and the evidence that is
     * never shared.
     */
    @Test
    public void ShareUsageElement_DefaultSharesOnlyAcceptedEvidence() throws Exception {
        // Arrange
        createShareUsage(
            1,
            1,
            1,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>());

        Map<String, Object> evidenceData = new HashMap<>();
        evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
        evidenceData.put("header.user-agent", "some user agent");
        evidenceData.put("cookie.51d_screenpixelswidth", "screen width");
        evidenceData.put("query.51D_Test", "fiftyone parameter");
        evidenceData.put("cookie.sessionid", "sessionid cookie");
        evidenceData.put("header.cookie", "cookie header");
        evidenceData.put("query.password", "password parameter");
        evidenceData.put("query.id.email", "email parameter");
        evidenceData.put("header.id.email", "email header");
        evidenceData.put("id.salt", "salt value");
        evidenceData.put("server.request-query", "whole query string");
        evidenceData.put("server.request-path", "request path");
        evidenceData.put("session.org.example.attribute", "session attribute");

        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidenceData);

            // Act
            shareUsageElement.process(data);
            assertNotNull(shareUsageElement.getSendDataFuture());
            shareUsageElement.getSendDataFuture().get();
        }

        // Assert
        String output = new String(
            connector.getBaos().toByteArray(), StandardCharsets.UTF_8);
        for (String shared : Arrays.asList(
            "some user agent", "screen width", "fiftyone parameter")) {
            assertTrue(output.contains(shared), shared);
        }
        for (String withheld : Arrays.asList(
            "sessionid cookie", "cookie header", "password parameter",
            "email parameter", "email header", "salt value",
            "whole query string", "request path", "session attribute")) {
            assertFalse(output.contains(withheld), withheld);
        }
    }

    /**
     * The default tracker does not share again a request that differs from
     * one already shared only in its session id and sequence number.
     */
    @Test
    public void ShareUsageElement_RepeatWithNewSequence_NotShared() throws Exception {
        // Arrange
        tracker = null;
        createShareUsage(
            1,
            3,
            1,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>());

        // Act
        for (int sequence = 1; sequence <= 2; sequence++) {
            Map<String, Object> evidenceData = new HashMap<>();
            evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
            evidenceData.put("header.user-agent", "repeated user agent");
            evidenceData.put(Constants.EVIDENCE_SESSIONID, "session " + sequence);
            evidenceData.put(Constants.EVIDENCE_SEQUENCE, sequence);
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence(evidenceData);
                shareUsageElement.process(data);
            }
        }
        shareUsageElement.close();

        // Assert
        Document doc = docBuilderFactory.newDocumentBuilder().parse(
            new ByteArrayInputStream(connector.getBaos().toByteArray()));
        NodeList devices = (NodeList) xpath.compile("//Device")
            .evaluate(doc, XPathConstants.NODESET);
        assertEquals(1, devices.getLength(), "1 device expected");
    }

    /**
     * Within one session, a request that differs from one already shared
     * only in its sequence number is not shared again, and a later request
     * with a 51D_ value changed is, with the same SessionId and its own
     * Sequence. The session id and the sequence never enter the repeat
     * comparison and are always written to the record.
     */
    @Test
    public void ShareUsageElement_OneSession_ChangedValueSharedWithItsSequence()
        throws Exception {
        // Arrange
        tracker = null;
        createShareUsage(
            1,
            3,
            1,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>());
        String[] screenWidths = { "1920", "1920", "1024" };

        // Act
        for (int sequence = 1; sequence <= screenWidths.length; sequence++) {
            Map<String, Object> evidenceData = new HashMap<>();
            evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
            evidenceData.put("header.user-agent", "one session user agent");
            evidenceData.put(
                "cookie.51D_ScreenPixelsWidth",
                screenWidths[sequence - 1]);
            evidenceData.put(Constants.EVIDENCE_SESSIONID, "one session");
            evidenceData.put(Constants.EVIDENCE_SEQUENCE, sequence);
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence(evidenceData);
                shareUsageElement.process(data);
            }
        }
        shareUsageElement.close();

        // Assert
        Document doc = docBuilderFactory.newDocumentBuilder().parse(
            new ByteArrayInputStream(connector.getBaos().toByteArray()));
        NodeList devices = (NodeList) xpath.compile("//Device")
            .evaluate(doc, XPathConstants.NODESET);
        assertEquals(2, devices.getLength(),
            "Only the first request and the one with a changed value " +
                "should be shared");
        assertEquals("one session", xpath.evaluate("SessionId", devices.item(0)));
        assertEquals("1", xpath.evaluate("Sequence", devices.item(0)));
        assertEquals("1920", cookieValue(devices.item(0), "51D_ScreenPixelsWidth"));
        assertEquals("one session", xpath.evaluate("SessionId", devices.item(1)));
        assertEquals("3", xpath.evaluate("Sequence", devices.item(1)));
        assertEquals("1024", cookieValue(devices.item(1), "51D_ScreenPixelsWidth"));
    }

    /**
     * The value a record gives for the named cookie, or null when it gives
     * none. Names are compared without regard to case.
     */
    private static String cookieValue(Node device, String name) {
        NodeList children = device.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            Node nameAttribute = child.getAttributes() == null ?
                null : child.getAttributes().getNamedItem("Name");
            if ("cookie".equals(child.getNodeName()) &&
                nameAttribute != null &&
                name.equalsIgnoreCase(nameAttribute.getNodeValue())) {
                return child.getTextContent();
            }
        }
        return null;
    }

    /**
     * The session cookie the element names is read by the tracker, whatever
     * the case of its name, and never sent. With sessions tracked, the same
     * evidence from a new session is shared again. The element accepts no
     * Evidence, so the cookie reaches the tracker from what it finds rather
     * than through its accepted Evidence.
     */
    @Test
    public void ShareUsageElement_TrackSession_SessionCookie() throws Exception {
        // Arrange
        tracker = null;
        createShareUsage(
            1,
            3,
            1,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>());
        String sessionCookie = EVIDENCE_COOKIE_PREFIX + EVIDENCE_SEPERATOR +
            DEFAULT_SESSION_COOKIE_NAME;

        // Act
        for (int session = 1; session <= 2; session++) {
            Map<String, Object> evidenceData = new HashMap<>();
            evidenceData.put(EVIDENCE_CLIENTIP_KEY, "1.2.3.4");
            evidenceData.put("header.user-agent", "repeated user agent");
            evidenceData.put(sessionCookie, "session cookie " + session);
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence(evidenceData);
                shareUsageElement.process(data);
            }
        }
        shareUsageElement.close();

        // Assert
        assertFalse(shareUsageElement.getEvidenceKeyFilter().include(sessionCookie));
        byte[] output = connector.getBaos().toByteArray();
        assertFalse(new String(output, StandardCharsets.UTF_8).contains(
            "session cookie"), "the session cookie is never sent");
        Document doc = docBuilderFactory.newDocumentBuilder().parse(
            new ByteArrayInputStream(output));
        NodeList devices = (NodeList) xpath.compile("//Device")
            .evaluate(doc, XPathConstants.NODESET);
        assertEquals(2, devices.getLength(), "1 device expected for each session");
    }
}
