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

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.caching.FlowCache;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/**
 * The transaction the Cloud Request Engine issues and keeps for each page,
 * and the cases HumanConfidenceElementTests does not cover, with a
 * stand-in for the cloud service. See the required tests in
 * <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/human-confidence.md#required-tests">human-confidence.md</a>.
 */
public class HumanConfidenceTransactionTests
    extends CloudRequestEngineTestsBase {

    /** A sealed result, with characters a form must encode. */
    private static final String SEALED = "SEALEDxRESULTx0123+456/789=";
    private static final String LICENSE = "LICENSExKEYxABCD";
    private static final String REDEEMED =
        "{'derived':{'humanconfidence':'High'," +
        "'humanconfidencesource':'Active'}}";

    /** The bodies of the requests to the json endpoint, in order. */
    private final List<String> calls = new CopyOnWriteArrayList<>();

    /** Each logger made, by name, which records every call made to it. */
    private final Map<String, Logger> loggers = new TreeMap<>();

    /** The answer to the next request to the json endpoint. */
    private volatile String answer = REDEEMED;

    /** Thrown in place of an answer to the json endpoint, where set. */
    private volatile IOException failure = null;

    private ILoggerFactory recordingLoggers;
    private CloudRequestEngineDefault engine;
    private HumanConfidenceElement element;

    public HumanConfidenceTransactionTests() throws MalformedURLException {
        super();
    }

    @BeforeEach
    public void setUp() throws IOException {
        evidenceKeysResponse = "['query.User-Agent','header.user-agent']";
        recordingLoggers = name -> {
            synchronized (loggers) {
                return loggers.computeIfAbsent(name, n -> mock(
                    Logger.class,
                    invocation -> invocation.getMethod().getReturnType() ==
                        boolean.class ? Boolean.TRUE : null));
            }
        };
        configureMockedClient();
        doAnswer(invocation -> {
            HttpURLConnection connection = invocation.getArgument(0);
            String path = connection.getURL().getPath();
            if (path.endsWith("properties")) {
                return accessiblePropertiesResponse;
            }
            if (path.endsWith("evidencekeys")) {
                return evidenceKeysResponse;
            }
            calls.add(new String(
                (byte[]) invocation.getArgument(2), "UTF-8"));
            if (failure != null) {
                throw failure;
            }
            return answer;
        }).when(httpClient).postData(any(), anyMap(), (byte[]) any());
    }

    private CloudRequestEngineBuilder requestEngine(String license) {
        CloudRequestEngineBuilder builder =
            new CloudRequestEngineBuilder(recordingLoggers, httpClient)
                .setResourceKey("resourcekey")
                .setRequestedProperties(Arrays.asList(
                    "derived.HumanConfidence",
                    "derived.HumanConfidenceSource"));
        return license == null ? builder : builder.setLicenseKey(license);
    }

    private Pipeline pipeline(CloudRequestEngineBuilder builder)
        throws Exception {
        engine = (CloudRequestEngineDefault) builder.build();
        element = new HumanConfidenceElementBuilder(recordingLoggers)
            .setRequestEngine(engine)
            .build();
        return new PipelineBuilder(recordingLoggers)
            .setSuppressProcessException(true)
            .addFlowElement(engine)
            .addFlowElement(element)
            .build();
    }

    private Pipeline pipeline() throws Exception {
        return pipeline(requestEngine(LICENSE));
    }

    /**
     * Process a request that carries the sealed result and a page id. The
     * caller closes the Flow Data, or this does where it fails.
     */
    private FlowData process(Pipeline pipeline, String pageId) {
        FlowData data = pipeline.createFlowData();
        boolean handedOver = false;
        try {
            data.addEvidence("header.user-agent", "Mozilla/5.0");
            data.addEvidence("query.humanconfidence", SEALED);
            if (pageId != null) {
                data.addEvidence("query.humanconfidencepage", pageId);
            }
            data.process();
            handedOver = true;
            return data;
        } finally {
            if (handedOver == false) {
                data.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static AspectPropertyValue<String> property(
        FlowData data,
        String name) {
        ElementData derived = data.get(HumanConfidenceElement.ELEMENT_DATA_KEY);
        return (AspectPropertyValue<String>) derived.get(name);
    }

    /** The fields of a form, by name, each name once. */
    private static Map<String, List<String>> form(String body)
        throws IOException {
        Map<String, List<String>> fields =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String pair : body.split("&")) {
            int equals = pair.indexOf('=');
            String name = equals < 0 ? pair : pair.substring(0, equals);
            String value = equals < 0 ? "" : pair.substring(equals + 1);
            fields.computeIfAbsent(
                URLDecoder.decode(name, "UTF-8"), k -> new ArrayList<>())
                .add(URLDecoder.decode(value, "UTF-8"));
        }
        return fields;
    }

    /**
     * Medium and Active, read without an exception, and no error from the
     * Human Confidence element.
     */
    private void assertNotRedeemed(FlowData data) {
        assertEquals("Medium",
            property(data, HumanConfidenceElement.HUMAN_CONFIDENCE)
                .getValue());
        assertEquals("Active",
            property(data, HumanConfidenceElement.HUMAN_CONFIDENCE_SOURCE)
                .getValue());
        if (data.getErrors() != null) {
            for (FlowError error : data.getErrors()) {
                assertNotSame(element, error.getFlowElement(),
                    "The read added an error of its own.");
            }
        }
    }

    // The transaction kept for each page.

    @Test
    public void redeemed_CallHoldsTheKeptTransactionAndNoPageId()
        throws Exception {
        try (Pipeline pipeline = pipeline()) {
            HumanConfidencePage page = engine.newHumanConfidencePage();
            try (FlowData data = process(pipeline, page.getId())) {
                assertEquals("High",
                    property(data, HumanConfidenceElement.HUMAN_CONFIDENCE)
                        .getValue());
            }
            Map<String, List<String>> call = form(calls.get(0));
            assertEquals(Collections.singletonList(SEALED),
                call.get("humanconfidence"),
                "The sealed result was not sent exactly as it arrived.");
            assertEquals(Collections.singletonList(page.getTransaction()),
                call.get("transaction"));
            assertNull(call.get("humanconfidencepage"),
                "The page id was sent to the cloud service.");
        }
    }

    /**
     * A request from a page the engine did not serve, or with no page id,
     * has no kept transaction, so the sealed result is not sent and the
     * Value is Medium, whatever the cloud would answer.
     */
    @Test
    public void notRedeemed_PageNotServed() throws Exception {
        try (Pipeline pipeline = pipeline()) {
            engine.newHumanConfidencePage();
            for (String pageId : Arrays.asList("not-a-page-id", null)) {
                try (FlowData data = process(pipeline, pageId)) {
                    assertNotRedeemed(data);
                }
            }
            for (String call : calls) {
                assertNull(form(call).get("humanconfidence"));
                assertNull(form(call).get("transaction"));
            }
        }
    }

    /** A page's transaction is used once. */
    @Test
    public void notRedeemed_TransactionUsedBefore() throws Exception {
        try (Pipeline pipeline = pipeline()) {
            HumanConfidencePage page = engine.newHumanConfidencePage();
            try (FlowData first = process(pipeline, page.getId())) {
                assertEquals("High",
                    property(first, HumanConfidenceElement.HUMAN_CONFIDENCE)
                        .getValue());
            }
            try (FlowData second = process(pipeline, page.getId())) {
                assertNotRedeemed(second);
            }
            assertNull(form(calls.get(1)).get("humanconfidence"));
            assertNull(form(calls.get(1)).get("transaction"));
        }
    }

    /** Two pages each redeem with their own transaction. */
    @Test
    public void pages_EachRedeemsWithItsOwnTransaction() throws Exception {
        try (Pipeline pipeline = pipeline()) {
            HumanConfidencePage first = engine.newHumanConfidencePage();
            HumanConfidencePage second = engine.newHumanConfidencePage();
            process(pipeline, second.getId()).close();
            process(pipeline, first.getId()).close();
            assertEquals(Collections.singletonList(second.getTransaction()),
                form(calls.get(0)).get("transaction"));
            assertEquals(Collections.singletonList(first.getTransaction()),
                form(calls.get(1)).get("transaction"));
        }
    }

    @Test
    public void transaction_OnlyTheKeptOneIsSent() throws Exception {
        try (Pipeline pipeline = pipeline()) {
            HumanConfidencePage page = engine.newHumanConfidencePage();
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("query.humanconfidence", SEALED);
                data.addEvidence("query.humanconfidencepage", page.getId());
                data.addEvidence("query.transaction", "from-the-request");
                data.addEvidence("header.Transaction", "from-a-header");
                data.addEvidence("query.challenge", "older-name");
                data.addEvidence("server.transaction", "from-the-server");
                data.process();
            }
            Map<String, List<String>> call = form(calls.get(0));
            assertEquals(Collections.singletonList(page.getTransaction()),
                call.get("transaction"));
            assertNull(call.get("challenge"));
            for (String other : Arrays.asList("from-the-request",
                "from-a-header", "older-name", "from-the-server")) {
                assertFalse(calls.get(0).contains(other),
                    "The call holds '" + other + "'.");
            }
        }
    }

    // More cases where the sealed result is not redeemed.

    /**
     * A License Key in the Evidence is never used, so the sealed result is
     * not redeemed by an engine that has none of its own.
     */
    @Test
    public void notRedeemed_LicenseKeyOnlyInEvidence() throws Exception {
        try (Pipeline pipeline = pipeline(requestEngine(null))) {
            HumanConfidencePage page = engine.newHumanConfidencePage();
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("query.humanconfidence", SEALED);
                data.addEvidence("query.humanconfidencepage", page.getId());
                data.addEvidence("query.license", LICENSE);
                data.addEvidence("header.licence", LICENSE);
                data.process();
                assertNotRedeemed(data);
            }
            Map<String, List<String>> call = form(calls.get(0));
            assertNull(call.get("license"));
            assertNull(call.get("licence"));
            assertNull(call.get("humanconfidence"));
        }
    }

    /**
     * The Cloud Request Engine makes no request, here because it still
     * lacks the accepted Evidence keys when it processes.
     */
    @Test
    public void notRedeemed_NoRequestMade() throws Exception {
        doThrow(new IOException("unreachable"))
            .when(httpClient)
            .getResponseString(
                argThat(c -> c.getURL().getPath().endsWith("evidencekeys")),
                anyMap());
        try (Pipeline pipeline = pipeline()) {
            HumanConfidencePage page = engine.newHumanConfidencePage();
            try (FlowData data = process(pipeline, page.getId())) {
                assertNotRedeemed(data);
            }
        }
        assertTrue(calls.isEmpty());
    }

    @Test
    public void notRedeemed_RequestTimedOut() throws Exception {
        failure = new SocketTimeoutException("Read timed out");
        try (Pipeline pipeline = pipeline()) {
            HumanConfidencePage page = engine.newHumanConfidencePage();
            try (FlowData data = process(pipeline, page.getId())) {
                assertNotRedeemed(data);
                assertEquals(1, data.getErrors().size());
                assertSame(engine,
                    data.getErrors().iterator().next().getFlowElement());
            }
        }
    }

    // No cache.

    /**
     * The same Evidence twice, with a cache, makes two requests, because
     * the first result is not stored.
     */
    @Test
    public void cache_SealedResultIsNotStored() throws Exception {
        try (Pipeline pipeline = pipeline(requestEngine(LICENSE)
                .setCacheSize(10))) {
            for (int i = 0; i < 2; i++) {
                process(pipeline, "fixed").close();
            }
        }
        assertEquals(2, calls.size());
    }

    /**
     * A result the cache holds for the same request without the sealed
     * result is not served to a request that carries one.
     */
    @Test
    public void cache_PassiveResultIsNotServed() throws Exception {
        answer = "{'derived':{'humanconfidence':'High'," +
            "'humanconfidencesource':'Passive'}}";
        try (Pipeline pipeline = pipeline(requestEngine(LICENSE)
                .setCacheSize(10))) {
            try (FlowData passive = pipeline.createFlowData()) {
                passive.addEvidence("header.user-agent", "Mozilla/5.0");
                passive.process();
            }
            answer = REDEEMED;
            HumanConfidencePage page = engine.newHumanConfidencePage();
            try (FlowData data = process(pipeline, page.getId())) {
                assertEquals("Active", property(data,
                    HumanConfidenceElement.HUMAN_CONFIDENCE_SOURCE)
                    .getValue());
            }
        }
        assertEquals(2, calls.size());
    }

    /** A cache is never asked or offered a result for a sealed request. */
    @Test
    public void cache_NeverUsed() throws Exception {
        FlowCache cache = mock(FlowCache.class);
        try (Pipeline pipeline = pipeline()) {
            engine.setCache(cache);
            HumanConfidencePage page = engine.newHumanConfidencePage();
            process(pipeline, page.getId()).close();
        }
        verify(cache, never()).get(any());
        verify(cache, never()).put(any(), any());
    }

    // Nothing logged.

    /**
     * Every call to every logger, at every level, and every error message,
     * from a request whose answer and Evidence repeat the sealed result,
     * the License Key and the transaction.
     */
    @Test
    public void logs_HoldNoSealedResultKeyOrTransaction() throws Exception {
        try (Pipeline pipeline = pipeline()) {
            HumanConfidencePage page = engine.newHumanConfidencePage();
            String transaction = page.getTransaction();
            answer = "{'errors':['Bad " + SEALED + " for " + LICENSE +
                " with " + transaction + "']," +
                "'warnings':['Seen " + SEALED + " " + LICENSE + " " +
                transaction + "']}";
            List<String> messages;
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("query.humanconfidence", SEALED);
                data.addEvidence("query.humanconfidencepage", page.getId());
                // Two other prefixes give the same name, which is logged
                // as a conflict.
                data.addEvidence("cookie.humanconfidence", SEALED);
                data.addEvidence("header.humanconfidence", SEALED + "2");
                data.process();
                assertNotRedeemed(data);
                messages = logged();
                for (FlowError error : data.getErrors()) {
                    for (Throwable t = error.getThrowable(); t != null;
                         t = t.getCause()) {
                        messages.add(t.toString());
                        for (Throwable s : t.getSuppressed()) {
                            messages.add(s.toString());
                        }
                    }
                }
            }
            assertFalse(messages.isEmpty());
            for (String message : messages) {
                for (String secret : Arrays.asList(SEALED,
                    SEALED.substring(SEALED.length() - 6),
                    LICENSE,
                    LICENSE.substring(LICENSE.length() - 4),
                    transaction,
                    transaction.substring(transaction.length() - 6))) {
                    assertFalse(message.contains(secret),
                        "A message holds a secret value: " + message);
                }
            }
        }
    }

    /** Every argument of every call to every logger, as text. */
    private List<String> logged() {
        List<String> messages = new ArrayList<>();
        synchronized (loggers) {
            for (Logger logger : loggers.values()) {
                for (Invocation invocation :
                    mockingDetails(logger).getInvocations()) {
                    for (Object argument : invocation.getArguments()) {
                        if (argument instanceof Throwable) {
                            for (Throwable t = (Throwable) argument;
                                 t != null; t = t.getCause()) {
                                messages.add(t.toString());
                            }
                        } else if (argument instanceof Object[]) {
                            messages.add(Arrays.deepToString(
                                (Object[]) argument));
                        } else {
                            messages.add(String.valueOf(argument));
                        }
                    }
                }
            }
        }
        return messages;
    }

    // The form.

    /**
     * An Evidence name holding '&' or '=' is encoded with its value, so it
     * cannot add a second license, transaction or humanconfidence field.
     */
    @Test
    public void form_EvidenceNameCannotAddAField() throws Exception {
        try (Pipeline pipeline = pipeline()) {
            HumanConfidencePage page = engine.newHumanConfidencePage();
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("query.humanconfidence", SEALED);
                data.addEvidence("query.humanconfidencepage", page.getId());
                data.addEvidence("query.51D_a&license", "EVIL");
                data.addEvidence("query.51D_b&transaction", "EVIL");
                data.addEvidence("query.51D_c&humanconfidence=EVIL&x", "y");
                data.process();
            }
            Map<String, List<String>> call = form(calls.get(0));
            assertEquals(Collections.singletonList(LICENSE),
                call.get("license"));
            assertEquals(Collections.singletonList(page.getTransaction()),
                call.get("transaction"));
            assertEquals(Collections.singletonList(SEALED),
                call.get("humanconfidence"));
            assertEquals(Collections.singletonList("EVIL"),
                call.get("51D_a&license"));
            assertEquals(Collections.singletonList("y"),
                call.get("51D_c&humanconfidence=EVIL&x"));
        }
    }

    // A sealed result under any key but query.humanconfidence.

    /**
     * A sealed result in a header, a cookie, server or with no prefix is
     * not sent, and nor is a transaction or challenge from the request,
     * whether query.humanconfidence is missing or empty.
     */
    @Test
    public void notSent_SealedResultUnderAnotherKey() throws Exception {
        try (Pipeline pipeline = pipeline()) {
            for (String key : Arrays.asList("header.humanconfidence",
                "cookie.HumanConfidence", "server.humanconfidence",
                "humanconfidence", "other.humanconfidence")) {
                for (String query : Arrays.asList(null, "")) {
                    HumanConfidencePage page = engine.newHumanConfidencePage();
                    try (FlowData data = pipeline.createFlowData()) {
                        data.addEvidence("header.user-agent", "Mozilla/5.0");
                        if (query != null) {
                            data.addEvidence("query.humanconfidence", query);
                        }
                        data.addEvidence(
                            "query.humanconfidencepage", page.getId());
                        data.addEvidence(key, SEALED);
                        data.addEvidence("query.transaction", "from-the-page");
                        data.addEvidence("header.transaction", "from-a-header");
                        data.addEvidence("cookie.challenge", "from-a-cookie");
                        data.process();
                    }
                    String body = calls.get(calls.size() - 1);
                    Map<String, List<String>> call = form(body);
                    for (String name : Arrays.asList(
                        "humanconfidence", "transaction", "challenge")) {
                        assertNull(call.get(name), key + " sent " + name);
                    }
                    assertFalse(body.contains(
                        URLEncoder.encode(SEALED, "UTF-8")), key);
                }
            }
        }
    }

    /** No cache is asked or offered a result for one either. */
    @Test
    public void cache_NotUsedForASealedResultUnderAnotherKey()
        throws Exception {
        FlowCache cache = mock(FlowCache.class);
        try (Pipeline pipeline = pipeline()) {
            engine.setCache(cache);
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("header.user-agent", "Mozilla/5.0");
                data.addEvidence("header.humanconfidence", SEALED);
                data.process();
            }
        }
        verify(cache, never()).get(any());
        verify(cache, never()).put(any(), any());
    }

    /** An empty humanconfidence is no sealed result and changes nothing. */
    @Test
    public void form_EmptyHumanConfidenceChangesNothing() throws Exception {
        try (Pipeline pipeline = pipeline()) {
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("query.humanconfidence", "");
                data.addEvidence("header.humanconfidence", "");
                data.addEvidence("query.transaction", "from-the-page");
                data.process();
            }
            assertEquals(Collections.singletonList("from-the-page"),
                form(calls.get(0)).get("transaction"));
        }
    }

    // Messages on any request.

    /**
     * No part of the License Key is in a log entry or error on a request
     * without a sealed result, whether the cloud names the key as it is
     * or URL encoded.
     */
    @Test
    public void messages_NoPartOfTheLicenseKeyOnAnyRequest() throws Exception {
        answer = "{'errors':['The key " + LICENSE + " is not valid.']," +
            "'warnings':['Seen license=" + LICENSE + "']}";
        List<String> messages;
        try (Pipeline pipeline = pipeline();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", "Mozilla/5.0");
            data.process();
            messages = logged();
            addErrors(messages, data);
        }
        assertNoPartOfTheLicenseKey(messages);
    }

    /** Nor in the error a refused discovery request gives. */
    @Test
    public void discovery_NoPartOfTheLicenseKeyInTheError() {
        accessiblePropertiesResponse =
            "{'errors':['The license key " + LICENSE + " is not valid.']}";
        accessiblePropertiesResponseCode = 403;
        Exception refused = assertThrows(Exception.class,
            () -> requestEngine(LICENSE).build());
        List<String> messages = logged();
        addThrowable(messages, refused);
        assertNoPartOfTheLicenseKey(messages);
    }

    private static void assertNoPartOfTheLicenseKey(List<String> messages) {
        for (String message : messages) {
            for (int i = 0; i + 4 <= LICENSE.length(); i++) {
                assertFalse(message.contains(LICENSE.substring(i, i + 4)),
                    "A message holds part of the License Key: " + message);
            }
        }
        assertTrue(messages.stream().anyMatch(m -> m.contains("[hidden]")),
            "The cloud's text was not given: " + messages);
    }

    /** The text of each error, its causes and the ones it suppressed. */
    private static void addErrors(List<String> messages, FlowData data) {
        if (data.getErrors() != null) {
            for (FlowError error : data.getErrors()) {
                addThrowable(messages, error.getThrowable());
            }
        }
    }

    private static void addThrowable(List<String> messages, Throwable t) {
        for (; t != null; t = t.getCause()) {
            messages.add(t.toString());
            for (Throwable s : t.getSuppressed()) {
                addThrowable(messages, s);
            }
        }
    }

    /**
     * A transaction or challenge the request gives is not in a log entry
     * or error, on a request without a sealed result too.
     */
    @Test
    public void messages_HoldNoTransactionOnAnyRequest() throws Exception {
        answer = "{'errors':['No match for TXN-FROM-THE-REQUEST.']," +
            "'warnings':['Seen CHALLENGE-FROM-A-COOKIE']}";
        List<String> messages;
        try (Pipeline pipeline = pipeline();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", "Mozilla/5.0");
            data.addEvidence("query.transaction", "TXN-FROM-THE-REQUEST");
            data.addEvidence("cookie.challenge", "CHALLENGE-FROM-A-COOKIE");
            data.process();
            messages = logged();
            addErrors(messages, data);
        }
        for (String message : messages) {
            assertFalse(message.contains("TXN-FROM"), message);
            assertFalse(message.contains("CHALLENGE-FROM"), message);
        }
        assertTrue(messages.stream().anyMatch(m -> m.contains("[hidden]")),
            "The cloud's text was not given: " + messages);
    }

    // A credential that leaves the Properties out.

    @Test
    public void notCarried_LeftOutGivesNoValueAndTheReason()
        throws Exception {
        answer = "{'derived':{}}";
        try (Pipeline pipeline = pipeline();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", "Mozilla/5.0");
            data.process();
            for (String name : Arrays.asList(
                HumanConfidenceElement.HUMAN_CONFIDENCE,
                HumanConfidenceElement.HUMAN_CONFIDENCE_SOURCE)) {
                AspectPropertyValue<String> value = property(data, name);
                assertFalse(value.hasValue());
                assertTrue(value.getNoValueMessage().contains(name),
                    value.getNoValueMessage());
            }
        }
    }

    /**
     * A response without a derived member leaves both Properties out, so
     * each gives the missing Property reason rather than the reason for no
     * response.
     */
    @Test
    public void notCarried_NoDerivedMemberGivesTheMissingReason()
        throws Exception {
        answer = "{'device':{'ismobile':true}}";
        try (Pipeline pipeline = pipeline();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", "Mozilla/5.0");
            data.process();
            for (String name : Arrays.asList(
                HumanConfidenceElement.HUMAN_CONFIDENCE,
                HumanConfidenceElement.HUMAN_CONFIDENCE_SOURCE)) {
                AspectPropertyValue<String> value = property(data, name);
                assertFalse(value.hasValue());
                assertEquals(
                    String.format(HumanConfidenceElement.MISSING, name),
                    value.getNoValueMessage());
            }
        }
    }

    // The Evidence the web integration adds.

    @Test
    public void evidenceKeys_IncludeTheSealedResultAndPageId()
        throws Exception {
        try (Pipeline pipeline = pipeline()) {
            assertTrue(pipeline.getEvidenceKeyFilter()
                .include("query.humanconfidence"));
            assertTrue(pipeline.getEvidenceKeyFilter()
                .include("query.humanconfidencepage"));
        }
    }

    // The pages kept.

    @Test
    public void pages_EachIsNewAndUsedOnce() {
        HumanConfidencePages pages = new HumanConfidencePages();
        HumanConfidencePage first = pages.issue();
        HumanConfidencePage second = pages.issue();
        assertNotEquals(first.getId(), second.getId());
        assertNotEquals(first.getTransaction(), second.getTransaction());
        assertNotEquals(first.getId(), first.getTransaction());
        assertEquals(first.getTransaction(), pages.take(first.getId()));
        assertNull(pages.take(first.getId()));
        assertNull(pages.take(null));
    }

    @Test
    public void pages_ExpireAfterTheirLifetime() {
        AtomicLong now = new AtomicLong(0);
        HumanConfidencePages pages = new HumanConfidencePages(now::get);
        HumanConfidencePage page = pages.issue();
        now.addAndGet(TimeUnit.MINUTES.toNanos(
            HumanConfidencePages.PAGE_LIFETIME_MINUTES) + 1);
        assertNull(pages.take(page.getId()));
    }

    @Test
    public void pages_OldestDroppedWhenFull() {
        HumanConfidencePages pages = new HumanConfidencePages(() -> 0L);
        HumanConfidencePage oldest = pages.issue();
        HumanConfidencePage next = pages.issue();
        for (int i = 2; i < HumanConfidencePages.MAX_PAGES + 1; i++) {
            pages.issue();
        }
        assertNull(pages.take(oldest.getId()));
        assertEquals(next.getTransaction(), pages.take(next.getId()));
    }

    @Test
    public void page_ScriptAddressCarriesTheTransaction() {
        HumanConfidencePage page = new HumanConfidencePage("id", "a+b/c");
        assertEquals("https://x/api/v4/r.js?transaction=a%2Bb%2Fc",
            page.scriptAddress("https://x/api/v4/r.js"));
        assertEquals("https://x/r.js?v=1&transaction=a%2Bb%2Fc",
            page.scriptAddress("https://x/r.js?v=1"));
    }
}
