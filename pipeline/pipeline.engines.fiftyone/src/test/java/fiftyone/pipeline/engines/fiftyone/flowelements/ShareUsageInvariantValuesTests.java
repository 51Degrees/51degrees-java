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
import fiftyone.pipeline.engines.trackers.Tracker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static fiftyone.pipeline.core.Constants.EVIDENCE_CLIENTIP_KEY;
import static fiftyone.pipeline.core.Constants.EVIDENCE_HEADER_USERAGENT_KEY;
import static fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The values that do not change while the element lives are written to its
 * first record and again once an hour, and every record carries the
 * process identifier. The sentences quoted are from
 * pipeline-specification/pipeline-elements/usage-sharing-element.md in the
 * specifications repository at 07e1fcedc2.
 * <p>
 * Each record is sent in a message of its own, and the element is given a
 * clock the test sets, so the hour passes without waiting for it.
 */
public class ShareUsageInvariantValuesTests {

    /** One hour, in milliseconds. */
    private static final long HOUR = 60L * 60L * 1000L;

    /**
     * The elements that hold the invariant values, as lines 138 to 145 list
     * them. Flow Elements are written as FlowElement and Packages as
     * Package.
     */
    private static final String[] INVARIANT = {
        "Version", "Product", "FlowElement", "Package", "Language",
        "LanguageVersion", "ServerIP", "Platform"};

    /**
     * Lines 122 to 125: "32 hexadecimal digits in groups of 8, 4, 4, 4 and
     * 12 separated by hyphens". A version 4 UUID has 4 as the first digit
     * of its third group and 8, 9, a or b as the first of its fourth.
     */
    private static final Pattern UUID_V4 = Pattern.compile(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}"
            + "-[0-9a-fA-F]{12}");

    /** Keeps each message the element sends apart from the others. */
    private static class Uploader implements DataUploader {
        final List<ByteArrayOutputStream> messages = new ArrayList<>();

        @Override
        public OutputStream getOutputStream() {
            ByteArrayOutputStream message = new ByteArrayOutputStream();
            messages.add(message);
            return message;
        }

        @Override
        public int getResponseCode() {
            return 200;
        }
    }

    private final List<Pipeline> pipelines = new ArrayList<>();

    @AfterEach
    public void cleanUp() throws Exception {
        for (Pipeline pipeline : pipelines) {
            pipeline.close();
        }
    }

    /** A Usage Sharing Element, in a Pipeline, and what it has sent. */
    private class Sharing {
        final ShareUsageElement element;
        final Uploader uploader = new Uploader();
        final Pipeline pipeline;
        final AtomicLong now = new AtomicLong(1_000_000_000_000L);

        Sharing() throws Exception {
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
                false,
                "http://51Degrees.com/test",
                new ArrayList<String>(),
                new ArrayList<String>(),
                new ArrayList<Map.Entry<String, String>>(),
                DEFAULT_SESSION_COOKIE_NAME,
                tracker);
            element.dataUploader = uploader;
            element.clock = now::get;
            // Package elements depend on whether the classes are loaded from
            // jars, so the test gives the element one package of its own.
            element.packageFinder = classes -> {
                TreeMap<String, String> packages = new TreeMap<>();
                packages.put("com.51degrees:pipeline.engines.fiftyone", "4.5.0");
                return packages;
            };
            pipeline = new PipelineBuilder()
                .addFlowElement(new SequenceElement(
                    LoggerFactory.getLogger(SequenceElement.class)))
                .addFlowElement(element)
                .build();
            pipelines.add(pipeline);
        }

        /** Send one record and return the Device element written for it. */
        Element send() throws Exception {
            int before = uploader.messages.size();
            Map<String, Object> evidence = new HashMap<>();
            evidence.put(EVIDENCE_HEADER_USERAGENT_KEY, "Mozilla/5.0");
            evidence.put(EVIDENCE_CLIENTIP_KEY, "192.0.2.1");
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence(evidence);
                data.process();
                assertNotNull(element.getSendDataFuture());
                element.getSendDataFuture().get();
            }
            assertEquals(before + 1, uploader.messages.size(),
                "One message for each record");
            Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(
                    uploader.messages.get(before).toByteArray()));
            NodeList devices = doc.getElementsByTagName("Device");
            assertEquals(1, devices.getLength(), "One record in the message");
            return (Element) devices.item(0);
        }

        /** Move the clock the element reads on by the time given. */
        void advance(long millis) {
            now.addAndGet(millis);
        }
    }

    private static int count(Element device, String name) {
        int count = 0;
        for (Node child = device.getFirstChild();
             child != null;
             child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE &&
                child.getNodeName().equals(name)) {
                count++;
            }
        }
        return count;
    }

    private static void assertCarriesInvariantValues(Element device) {
        for (String name : INVARIANT) {
            assertTrue(count(device, name) > 0,
                name + " is an invariant value and the record should carry it");
        }
    }

    private static void assertLeavesOutInvariantValues(Element device) {
        // Lines 207 to 210: "A record that leaves them out has no Version,
        // Product, FlowElement, Package, Language, LanguageVersion, ServerIP
        // or Platform element, and is otherwise the same."
        for (String name : INVARIANT) {
            assertEquals(0, count(device, name),
                name + " is an invariant value and the record should leave it out");
        }
        for (String name : new String[] {
            "ProcessId", "SessionId", "Sequence", "DateSent", "ClientIP"}) {
            assertEquals(1, count(device, name),
                name + " is written to every record");
        }
        assertEquals(1, count(device, "header"), "The Evidence is written");
    }

    private static String processId(Element device) {
        assertEquals(1, count(device, "ProcessId"),
            "Every record carries one ProcessId");
        return device.getElementsByTagName("ProcessId").item(0)
            .getTextContent();
    }

    /**
     * Lines 150 and 151: "The invariant values MUST be written to the first
     * record the element writes." Line 214 puts ProcessId first in the
     * record.
     */
    @Test
    public void FirstRecord_CarriesTheInvariantValues() throws Exception {
        Sharing sharing = new Sharing();
        Element first = sharing.send();

        assertCarriesInvariantValues(first);
        Node firstChild = first.getFirstChild();
        while (firstChild.getNodeType() != Node.ELEMENT_NODE) {
            firstChild = firstChild.getNextSibling();
        }
        assertEquals("ProcessId", firstChild.getNodeName(),
            "ProcessId comes first, as in the specification's example");
    }

    /**
     * Lines 152 and 153: "They MUST NOT be written to any other record." A
     * record written a moment less than an hour after the first leaves
     * them out.
     */
    @Test
    public void RecordsAfterTheFirst_LeaveThemOut_UntilTheHourIsUp()
        throws Exception {
        Sharing sharing = new Sharing();
        assertCarriesInvariantValues(sharing.send());

        sharing.advance(1);
        assertLeavesOutInvariantValues(sharing.send());
        sharing.advance(HOUR / 2);
        assertLeavesOutInvariantValues(sharing.send());
        sharing.advance(HOUR / 2 - 2);
        assertLeavesOutInvariantValues(sharing.send());
    }

    /**
     * Lines 151 and 152: "They MUST be written again to the first record the
     * element writes once an hour or more has passed since it last wrote
     * them." Lines 159 and 160: "The hour is measured from when the values
     * were last written to a record". So the hour starts again from the
     * record that carried them again, not from the first record.
     */
    @Test
    public void HourUp_WritesThemAgain_AndTheNextHourStartsThen()
        throws Exception {
        Sharing sharing = new Sharing();
        assertCarriesInvariantValues(sharing.send());

        // No record is written for an hour and a half.
        sharing.advance(HOUR + HOUR / 2);
        assertCarriesInvariantValues(sharing.send());
        // An hour after the first record, but not after the second.
        sharing.advance(HOUR / 2);
        assertLeavesOutInvariantValues(sharing.send());
        // Exactly an hour after the record that last carried them.
        sharing.advance(HOUR / 2);
        assertCarriesInvariantValues(sharing.send());
        assertLeavesOutInvariantValues(sharing.send());
    }

    /**
     * Lines 115 and 116: "The element MUST create a process identifier when
     * it is built, and MUST write it to every record as the `ProcessId`
     * element." Lines 238 and 239: "the same in every record the element
     * writes."
     */
    @Test
    public void OneElement_KeepsOneProcessId_OnEveryRecord() throws Exception {
        Sharing sharing = new Sharing();
        String first = processId(sharing.send());

        assertTrue(UUID_V4.matcher(first).matches(),
            first + " is not a version 4 UUID in the string format");
        sharing.advance(1);
        assertEquals(first, processId(sharing.send()));
        sharing.advance(HOUR);
        assertEquals(first, processId(sharing.send()),
            "The record that carries the invariant values again keeps it");
        sharing.advance(1);
        assertEquals(first, processId(sharing.send()));
    }

    /**
     * Lines 128 to 131: "The identifier is never stored, so a process that
     * starts again creates a new one. A process that builds more than one
     * Usage Sharing Element, for example one for each of two Pipelines,
     * creates an identifier for each". A process that starts again builds
     * its element afresh, so a fresh element stands for it here.
     */
    @Test
    public void FreshElement_CreatesANewProcessId() throws Exception {
        Sharing before = new Sharing();
        String first = processId(before.send());

        Sharing after = new Sharing();
        Element record = after.send();
        String second = processId(record);

        assertTrue(UUID_V4.matcher(second).matches(),
            second + " is not a version 4 UUID in the string format");
        assertNotEquals(first.toLowerCase(), second.toLowerCase(),
            "A fresh element creates a new identifier");
        assertCarriesInvariantValues(record);
    }
}
