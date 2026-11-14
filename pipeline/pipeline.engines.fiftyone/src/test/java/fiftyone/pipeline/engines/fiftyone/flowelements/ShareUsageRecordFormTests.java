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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.DataUploader;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.trackers.Tracker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fiftyone.pipeline.core.Constants.EVIDENCE_CLIENTIP_KEY;
import static fiftyone.pipeline.core.Constants.EVIDENCE_HEADER_USERAGENT_KEY;
import static fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The usage sharing record is written as the specification's Processing
 * section sets it out. Each test sends one record through a Pipeline that
 * holds a Sequence Element and the Usage Sharing Element, and reads what
 * the element wrote.
 */
public class ShareUsageRecordFormTests {

    /** Keeps what the element writes. */
    private static class Uploader implements DataUploader {
        final ByteArrayOutputStream written = new ByteArrayOutputStream();

        @Override
        public OutputStream getOutputStream() {
            return written;
        }

        @Override
        public int getResponseCode() {
            return 200;
        }
    }

    /** Keeps the headers the element sends through an HTTP client. */
    private static class HeaderClient implements HttpClient {
        Map<String, String> sent;

        @Override
        public HttpURLConnection connect(URL url) {
            return new HttpURLConnection(url) {
                @Override
                public int getResponseCode() {
                    return 200;
                }

                @Override
                public String getResponseMessage() {
                    return "";
                }

                @Override
                public void disconnect() {
                }

                @Override
                public boolean usingProxy() {
                    return false;
                }

                @Override
                public void connect() {
                }
            };
        }

        @Override
        public String postData(
            HttpURLConnection connection,
            Map<String, String> headers,
            byte[] data) {
            sent = new HashMap<>(headers);
            return "";
        }

        @Override
        public String getResponseString(HttpURLConnection connection) {
            return "";
        }

        @Override
        public String getResponseString(
            HttpURLConnection connection,
            Map<String, String> headers) {
            return "";
        }
    }

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    private Pipeline pipeline;
    private ShareUsageElement element;
    private Uploader uploader;

    @AfterEach
    public void cleanUp() throws Exception {
        if (pipeline != null) {
            pipeline.close();
        }
    }

    /**
     * Build a Pipeline of a Sequence Element and a Usage Sharing Element
     * that sends every record as soon as it has it.
     */
    private void build() throws Exception {
        SequenceElement sequence = new SequenceElement(
            LoggerFactory.getLogger(SequenceElement.class));
        Tracker tracker = new Tracker() {
            @Override
            public boolean track(FlowData flowData) {
                return true;
            }
        };
        element = new ShareUsageElement(
            LoggerFactory.getLogger(ShareUsageElement.class),
            1,
            1,
            2,
            100,
            100,
            1,
            true,
            "http://51Degrees.com/test",
            new ArrayList<String>(),
            new ArrayList<String>(),
            new ArrayList<Map.Entry<String, String>>(),
            DEFAULT_SESSION_COOKIE_NAME,
            tracker);
        uploader = new Uploader();
        element.dataUploader = uploader;
        pipeline = new PipelineBuilder()
            .addFlowElement(sequence)
            .addFlowElement(element)
            .build();
    }

    /** Send one record and return the text the element wrote. */
    private String send() throws Exception {
        Map<String, Object> evidence = new HashMap<>();
        evidence.put(EVIDENCE_HEADER_USERAGENT_KEY, "<iPhone> & co");
        evidence.put(EVIDENCE_CLIENTIP_KEY, "192.0.2.1");
        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidence);
            data.process();
            assertNotNull(element.getSendDataFuture());
            element.getSendDataFuture().get();
        }
        return new String(
            uploader.written.toByteArray(), StandardCharsets.UTF_8);
    }

    private static Document parse(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String text(Document doc, String name) {
        NodeList nodes = doc.getElementsByTagName(name);
        assertEquals(1, nodes.getLength(), "One " + name + " expected");
        return nodes.item(0).getTextContent();
    }

    /** The root is Devices alone, with no attribute. */
    @Test
    public void Root_IsDevicesWithNoAttribute() throws Exception {
        build();
        String xml = send();

        assertTrue(xml.startsWith("<Devices><Device>"), xml);
        assertTrue(xml.endsWith("</Device></Devices>"), xml);
        assertEquals(0,
            parse(xml).getDocumentElement().getAttributes().getLength());
    }

    /**
     * DateSent is the time in UTC, whatever time zone the JVM runs in.
     * {@code DATE_FMT} keeps the zone that was the default when the class
     * loaded, so the test gives it the zone a JVM started in that zone
     * has.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void DateSent_IsUtc_WhenTheDefaultTimeZoneIsNot() throws Exception {
        TimeZone defaultBefore = TimeZone.getDefault();
        TimeZone formatBefore = ShareUsageBase.DATE_FMT.getTimeZone();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
            ShareUsageBase.DATE_FMT.setTimeZone(TimeZone.getDefault());
            build();

            long before = System.currentTimeMillis();
            String sent = text(parse(send()), "DateSent");
            long after = System.currentTimeMillis();

            SimpleDateFormat utc = new SimpleDateFormat(DATE_PATTERN);
            utc.setTimeZone(TimeZone.getTimeZone("UTC"));
            long written = utc.parse(sent).getTime();
            // DateSent has no part of a second, so allow one either side.
            assertTrue(
                written >= before - 1000 && written <= after + 1000,
                "DateSent was " + sent + ", which is not the time in UTC, "
                + utc.format(before));
        } finally {
            TimeZone.setDefault(defaultBefore);
            ShareUsageBase.DATE_FMT.setTimeZone(formatBefore);
        }
    }

    /** Each FlowElement holds the fully qualified name of the class. */
    @Test
    public void FlowElement_IsTheFullyQualifiedClassName() throws Exception {
        build();
        NodeList names = parse(send()).getElementsByTagName("FlowElement");

        assertEquals(2, names.getLength());
        assertEquals(
            "fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElement",
            names.item(0).getTextContent());
        assertEquals(
            "fiftyone.pipeline.engines.fiftyone.flowelements."
            + "ShareUsageElement",
            names.item(1).getTextContent());
    }

    /** Platform holds the name of the operating system and its version. */
    @Test
    public void Platform_IsTheNameAndTheVersion() throws Exception {
        String name = System.getProperty("os.name");
        String version = System.getProperty("os.version");
        assertNotNull(version, "This JVM gives no os.version to test with");
        build();

        assertEquals(
            name + " " + version, text(parse(send()), "Platform"));
    }

    /** The message is sent as text/xml, compressed with gzip. */
    @Test
    public void ContentType_IsTextXml() throws Exception {
        build();

        assertEquals("text/xml", element.headers.get("Content-Type"));
        assertEquals("gzip", element.headers.get("Content-Encoding"));

        // The same headers go to an HTTP client the element is given.
        HeaderClient client = new HeaderClient();
        element.httpClient = client;
        send();
        assertEquals("text/xml", client.sent.get("Content-Type"));
        assertEquals("gzip", client.sent.get("Content-Encoding"));
    }

    /**
     * The whole record, compared as text. DateSent and ServerIP are read
     * from the record, as they differ from run to run, and the values that
     * name the runtime are the ones this JVM gives.
     */
    @Test
    public void WholeRecord_MatchesTheSpecificationExample() throws Exception {
        build();
        String xml = send();

        Matcher date = Pattern.compile(
            "<DateSent>(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})</DateSent>")
            .matcher(xml);
        assertTrue(date.find(), xml);
        Matcher session = Pattern.compile(
            "<SessionId>([^<]+)</SessionId>").matcher(xml);
        assertTrue(session.find(), xml);
        Matcher server = Pattern.compile(
            "<ServerIP>([^<]*)</ServerIP>").matcher(xml);
        assertTrue(server.find(), xml);
        String version = Pipeline.class.getPackage()
            .getImplementationVersion();

        StringBuilder expected = new StringBuilder();
        expected.append("<Devices><Device>")
            .append("<SessionId>").append(session.group(1))
            .append("</SessionId>")
            .append("<Sequence>1</Sequence>")
            .append("<DateSent>").append(date.group(1)).append("</DateSent>");
        if (version != null) {
            expected.append("<Version>").append(version).append("</Version>");
        }
        expected.append("<Product>Pipeline</Product>")
            .append("<FlowElement>fiftyone.pipeline.engines.fiftyone."
                + "flowelements.SequenceElement</FlowElement>")
            .append("<FlowElement>fiftyone.pipeline.engines.fiftyone."
                + "flowelements.ShareUsageElement</FlowElement>")
            .append("<Language>java</Language>")
            .append("<LanguageVersion>")
            .append(System.getProperty("java.version"))
            .append("</LanguageVersion>")
            .append("<ClientIP>192.0.2.1</ClientIP>")
            .append("<ServerIP>").append(server.group(1))
            .append("</ServerIP>")
            .append("<Platform>").append(System.getProperty("os.name"))
            .append(" ").append(System.getProperty("os.version"))
            .append("</Platform>")
            .append("<header Name=\"user-agent\">")
            .append("&lt;iPhone&gt; &amp; co</header>")
            .append("</Device></Devices>");

        assertEquals(expected.toString(), xml);
    }
}
