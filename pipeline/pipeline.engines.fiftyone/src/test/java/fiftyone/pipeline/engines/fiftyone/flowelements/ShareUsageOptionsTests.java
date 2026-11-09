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

import fiftyone.caching.Cache;
import fiftyone.caching.CacheBuilder;
import fiftyone.caching.LruPutCache;
import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.fiftyone.data.EvidenceKeyFilterShareUsage;
import fiftyone.pipeline.engines.fiftyone.trackers.ShareUsageTracker;
import fiftyone.pipeline.engines.services.DataUploader;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the usage sharing options "Share all Evidence" and "Share all query
 * string parameters" from a configuration file, the case of the keys the
 * element reads and the number of caches a tracker holds.
 * @see <a href="https://github.com/51Degrees/specifications/blob/rc/pipeline-specification/pipeline-elements/usage-sharing-element.md#configuration-options">Specification</a>
 */
public class ShareUsageOptionsTests {

    /**
     * Keeps what the element sends so a test can read it.
     */
    private static class CapturingUploader implements DataUploader {
        private final ByteArrayOutputStream sent = new ByteArrayOutputStream();

        @Override
        public OutputStream getOutputStream() {
            return sent;
        }

        @Override
        public int getResponseCode() {
            return 200;
        }

        Document document() throws Exception {
            return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(
                    new ByteArrayInputStream(sent.toByteArray())));
        }
    }

    /**
     * Counts the caches the tracker asks for.
     */
    private static class CountingCacheBuilder implements CacheBuilder {
        final AtomicInteger built = new AtomicInteger();
        private final LruPutCache.Builder inner = new LruPutCache.Builder();

        @Override
        public <K, V> Cache<K, V> build(Cache<K, V> c, int cacheSize) {
            built.incrementAndGet();
            return inner.build(c, cacheSize);
        }
    }

    /**
     * Options for a usage sharing element that sends every request at once,
     * as a configuration file holds them.
     */
    private static PipelineOptions options(String... nameAndValue) {
        ElementOptions element = new ElementOptions();
        element.builderName = "ShareUsage";
        element.buildParameters.put("SharePercentage", "1");
        element.buildParameters.put("MinimumEntriesPerMessage", "1");
        element.buildParameters.put("RepeatEvidenceIntervalMinutes", "0");
        element.buildParameters.put("BlockedHttpHeader", "x-blocked");
        for (int i = 0; i < nameAndValue.length; i += 2) {
            element.buildParameters.put(nameAndValue[i], nameAndValue[i + 1]);
        }
        PipelineOptions options = new PipelineOptions();
        options.elements.add(element);
        return options;
    }

    private static Map<String, Object> evidence() {
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("header.user-agent", "agent");
        evidence.put("header.x-blocked", "blocked");
        evidence.put("header.cookie", "a=b");
        evidence.put("cookie.plain", "cookie");
        evidence.put("session.plain", "session");
        evidence.put("query.plain", "query");
        evidence.put("query.id.email", "a@example.com");
        evidence.put("id.email", "a@example.com");
        evidence.put("id.salt", "salt");
        evidence.put("server.request-path", "/path");
        evidence.put("server.request-query", "?plain=query");
        return evidence;
    }

    /**
     * Builds the element from the options, gives it the Evidence and returns
     * what it sends.
     */
    private static Document send(
        PipelineOptions options,
        Map<String, Object> evidence) throws Exception {
        CapturingUploader uploader = new CapturingUploader();
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .buildFromConfiguration(options)) {
            ShareUsageElement element =
                pipeline.getElement(ShareUsageElement.class);
            assertNotNull(element);
            element.dataUploader = uploader;
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence(evidence);
                element.process(data);
                assertNotNull(element.getSendDataFuture());
                element.getSendDataFuture().get();
            }
        }
        return uploader.document();
    }

    private static boolean shared(
        Document sent,
        String category,
        String name) throws Exception {
        return !XPathFactory.newInstance().newXPath().evaluate(
            "//Device/" + category + "[@Name='" + name + "']",
            sent).isEmpty();
    }

    @Test
    public void defaults_ShareOnlyWhatTheRulesAllow() throws Exception {
        Document sent = send(options(), evidence());

        assertTrue(shared(sent, "header", "user-agent"));
        assertFalse(shared(sent, "header", "x-blocked"));
        assertFalse(shared(sent, "cookie", "plain"));
        assertFalse(shared(sent, "session", "plain"));
        assertFalse(shared(sent, "query", "plain"));
    }

    @Test
    public void shareAllEvidence_FromConfiguration_SharesEverythingButNeverShared()
        throws Exception {
        Document sent = send(options("ShareAllEvidence", "true"), evidence());

        assertTrue(shared(sent, "header", "user-agent"));
        assertTrue(shared(sent, "header", "x-blocked"));
        assertTrue(shared(sent, "cookie", "plain"));
        assertTrue(shared(sent, "session", "plain"));
        assertTrue(shared(sent, "query", "plain"));
        assertFalse(shared(sent, "query", "id.email"));
        assertFalse(shared(sent, "id", "email"));
        assertFalse(shared(sent, "id", "salt"));
        assertFalse(shared(sent, "header", "cookie"));
        assertFalse(shared(sent, "server", "request-path"));
        assertFalse(shared(sent, "server", "request-query"));
    }

    @Test
    public void shareAllEvidence_False_FromConfiguration_KeepsTheRules()
        throws Exception {
        Document sent = send(options("ShareAllEvidence", "false"), evidence());

        assertFalse(shared(sent, "header", "x-blocked"));
        assertFalse(shared(sent, "cookie", "plain"));
        assertFalse(shared(sent, "query", "plain"));
    }

    @Test
    public void shareAllQueryStringParameters_FromConfiguration_SharesEveryParameter()
        throws Exception {
        Document sent = send(
            options("ShareAllQueryStringParameters", "true"),
            evidence());

        assertTrue(shared(sent, "query", "plain"));
        assertFalse(shared(sent, "query", "id.email"));
        assertFalse(shared(sent, "server", "request-query"));
        // The other rules still apply.
        assertFalse(shared(sent, "header", "x-blocked"));
        assertFalse(shared(sent, "cookie", "plain"));
        assertFalse(shared(sent, "session", "plain"));
        assertTrue(shared(sent, "header", "user-agent"));
    }

    @Test
    public void shareAllQueryStringParameters_False_FromConfiguration_KeepsTheRules()
        throws Exception {
        Document sent = send(
            options("ShareAllQueryStringParameters", "false"),
            evidence());

        assertFalse(shared(sent, "query", "plain"));
    }

    @Test
    public void keysAreReadWithoutRegardToCase() throws Exception {
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("Server.Client-IP", "1.2.3.4");
        evidence.put("Query.Session-ID", "session-1");
        evidence.put("Query.Sequence", "5");

        Document sent = send(options(), evidence);

        assertEquals("1.2.3.4",
            sent.getElementsByTagName("ClientIP").item(0).getTextContent());
        assertEquals("session-1",
            sent.getElementsByTagName("SessionId").item(0).getTextContent());
        assertEquals("5",
            sent.getElementsByTagName("Sequence").item(0).getTextContent());
    }

    @Test
    public void tracker_BuildsOneCache() {
        CountingCacheBuilder builder = new CountingCacheBuilder();

        try (ShareUsageTracker tracker = new ShareUsageTracker(
            new CacheConfiguration(builder, 10),
            1000,
            new EvidenceKeyFilterShareUsage(
                new ArrayList<String>(),
                new ArrayList<String>(),
                false,
                DEFAULT_SESSION_COOKIE_NAME))) {
            assertEquals(1, builder.built.get());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
