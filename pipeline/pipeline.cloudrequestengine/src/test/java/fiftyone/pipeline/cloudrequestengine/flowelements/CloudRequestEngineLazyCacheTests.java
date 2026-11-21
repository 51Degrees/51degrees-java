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

import fiftyone.caching.Cache;
import fiftyone.caching.CacheBuilder;
import fiftyone.caching.LruPutCache;
import fiftyone.caching.PutCache;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.configuration.LazyLoadingConfiguration;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * A Cloud Request Engine with a cache returns from process without waiting
 * for the cloud service, because its request is a lazy task, and serves a
 * result from the cache only once its request has finished with a response
 * that may be served. See
 * <a href="https://github.com/51Degrees/specifications/blob/9d12262654fe8914a5497bdacd85ca733898dd8b/pipeline-specification/pipeline-elements/cloud-request-engine.md#L364-L371">cloud-request-engine.md</a>
 * ("It then starts the HTTP request, adds its Element Data with
 * json-response as a lazy property whose task is that request, and returns
 * without waiting for the response.") and
 * <a href="https://github.com/51Degrees/specifications/blob/9d12262654fe8914a5497bdacd85ca733898dd8b/pipeline-specification/features/caching.md#L151-L157">caching.md</a>
 * ("A cache MUST NOT store a result that holds such a value, and MUST NOT
 * serve one that it finds."). The deprecated lazy loading option is set in
 * these tests and has no effect.
 * <p>
 * No result depends on timing: the cloud service's answer is held back until
 * the test releases it, and the test reads the processing thread's stack to
 * see whether it is waiting for that answer.
 */
public class CloudRequestEngineLazyCacheTests
    extends CloudRequestEngineTestsBase {

    /**
     * The timeout given with the lazy loading option, which has no effect.
     */
    private static final int LAZY_TIMEOUT_MILLIS = 600000;

    public CloudRequestEngineLazyCacheTests() throws MalformedURLException {
        super();
    }

    /**
     * Mock the cloud service so that the nth data request gets the nth
     * response in the list, and waits for the gate first.
     */
    private void mockService(
        final CountDownLatch called,
        final CountDownLatch gate,
        String... responses) throws Exception {
        evidenceKeysResponse =
            new JSONArray(Arrays.asList("query.User-Agent")).toString();
        configureMockedClient();
        final AtomicInteger next = new AtomicInteger();
        doAnswer(invocation -> {
            HttpURLConnection connection = invocation.getArgument(0);
            String path = connection.getURL().getPath();
            if (path.endsWith("properties")) {
                return accessiblePropertiesResponse;
            }
            if (path.endsWith("evidencekeys")) {
                return evidenceKeysResponse;
            }
            String response = responses[next.getAndIncrement()];
            called.countDown();
            gate.await();
            if (response == null) {
                throw new IOException("The cloud service did not answer.");
            }
            return response;
        }).when(httpClient).postData(any(), anyMap(), (byte[]) any());
    }

    private CloudRequestEngine buildLazyCachedEngine() throws Exception {
        return new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey("resourcekey")
            .setCacheSize(10)
            .setLazyLoading(new LazyLoadingConfiguration(LAZY_TIMEOUT_MILLIS))
            .build();
    }

    /**
     * Whether the thread is waiting for a cloud request or a process task.
     */
    private static boolean waitingForTheRequest(Thread thread) {
        for (StackTraceElement frame : thread.getStackTrace()) {
            String type = frame.getClassName();
            String method = frame.getMethodName();
            if (method.equals("waitOnAllProcessFutures") ||
                type.equals("java.util.concurrent.CompletableFuture") &&
                    (method.equals("join") || method.equals("get") ||
                        method.equals("waitingGet") ||
                        method.equals("timedGet")) ||
                type.endsWith("FutureTask") && method.equals("get")) {
                return true;
            }
        }
        return false;
    }

    /**
     * process returns while the cloud service has not answered, so the
     * check made before the result is stored does not wait for the
     * response.
     */
    @Test
    public void process_CachedEngineDoesNotWaitForTheService()
        throws Exception {
        CountDownLatch called = new CountDownLatch(1);
        CountDownLatch gate = new CountDownLatch(1);
        mockService(called, gate, jsonResponse);
        CloudRequestEngine engine = buildLazyCachedEngine();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            final AtomicBoolean returned = new AtomicBoolean(false);
            final AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread processing = new Thread(() -> {
                try {
                    data.process();
                    returned.set(true);
                } catch (Throwable t) {
                    failure.set(t);
                }
            });
            processing.start();
            called.await();
            // Look until process has returned, or is seen waiting for the
            // answer that is held back.
            boolean waited = false;
            while (returned.get() == false && failure.get() == null &&
                processing.isAlive()) {
                if (waitingForTheRequest(processing)) {
                    waited = true;
                    break;
                }
                Thread.yield();
            }
            gate.countDown();
            processing.join();
            assertNull(failure.get());
            assertFalse(waited,
                "process waited for the cloud service's answer.");
            assertTrue(returned.get());
            assertEquals(jsonResponse,
                data.get(CloudRequestData.class).getJsonResponse());
        }
    }

    /**
     * A failed request is not served from the cache, so the next identical
     * request goes to the cloud service and gets its response.
     */
    @Test
    public void process_FailedRequestIsNotServed() throws Exception {
        String second = "{'device':{'value':'1'}}";
        mockService(new CountDownLatch(2), new CountDownLatch(0),
            null, second);
        CloudRequestEngine engine = buildLazyCachedEngine();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build()) {
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("query.User-Agent", "iPhone");
                data.process();
                CloudRequestData result = data.get(CloudRequestData.class);
                assertThrows(RuntimeException.class, result::getJsonResponse);
            }
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("query.User-Agent", "iPhone");
                data.process();
                assertEquals(second,
                    data.get(CloudRequestData.class).getJsonResponse(),
                    "The failed request was served from the cache.");
            }
        }
        verify(httpClient, times(2)).postData(
            argThat(c -> c.getURL().getPath().endsWith("properties") == false &&
                c.getURL().getPath().endsWith("evidencekeys") == false),
            any(),
            any());
    }

    /**
     * A request still running for one Flow Data is not served from the cache
     * to another, so the second gets its own request and its own response,
     * which matters when the response holds a 51Did.
     */
    @Test
    public void process_RunningRequestIsNotShared() throws Exception {
        String first = "{'device':{'value':'1'},'fodid':'first'}";
        String second = "{'device':{'value':'1'},'fodid':'second'}";
        CountDownLatch firstCalled = new CountDownLatch(1);
        CountDownLatch gate = new CountDownLatch(1);
        mockService(firstCalled, gate, first, second);
        CloudRequestEngine engine = buildLazyCachedEngine();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build();
             FlowData one = pipeline.createFlowData();
             FlowData two = pipeline.createFlowData()) {
            one.addEvidence("query.User-Agent", "iPhone");
            one.process();
            // The first request has started and is held back, so its result
            // is in the cache with its request still running.
            firstCalled.await();
            two.addEvidence("query.User-Agent", "iPhone");
            two.process();
            gate.countDown();
            assertEquals(first,
                one.get(CloudRequestData.class).getJsonResponse());
            assertEquals(second,
                two.get(CloudRequestData.class).getJsonResponse(),
                "A request still running was served from the cache.");
        }
    }
    /**
     * With lazy loading, a response without a 51Did is still cached, so the
     * second identical request is served from the cache.
     */
    @Test
    public void process_LazyResponseWithoutFodIdIsCached() throws Exception {
        CountDownLatch gate = new CountDownLatch(0);
        String first = "{'device':{'value':'1'}}";
        mockService(new CountDownLatch(1), gate, first);
        CloudRequestEngine engine = buildLazyCachedEngine();

        List<String> answers = processTwice(engine);

        assertEquals(Arrays.asList(first, first), answers);
        verify(httpClient, times(1)).postData(
            argThat(c -> c.getURL().getPath().endsWith("properties") == false &&
                c.getURL().getPath().endsWith("evidencekeys") == false),
            any(),
            any());
    }

    /**
     * With lazy loading, a response with a 51Did is not served from the
     * cache, so the second identical request goes to the cloud service and
     * gets its own.
     */
    @Test
    public void process_LazyFodIdResponseIsNotServed() throws Exception {
        CountDownLatch gate = new CountDownLatch(0);
        String first = "{'device':{'value':'1'},'fodid':'first'}";
        String second = "{'device':{'value':'1'},'fodid':'second'}";
        mockService(new CountDownLatch(2), gate, first, second);
        CloudRequestEngine engine = buildLazyCachedEngine();

        List<String> answers = processTwice(engine);

        assertEquals(Arrays.asList(first, second), answers,
            "A response with a 51Did was served from the cache.");
    }

    /**
     * With lazy loading, the result is offered to the cache once, on the
     * thread that called process.
     */
    @Test
    public void process_LazyResultIsStoredOnTheProcessingThread()
        throws Exception {
        mockService(new CountDownLatch(1), new CountDownLatch(0),
            jsonResponse);
        final List<Thread> putThreads =
            Collections.synchronizedList(new ArrayList<>());
        CacheBuilder recording = new CacheBuilder() {
            @Override
            public <K, V> Cache<K, V> build(Cache<K, V> c, int cacheSize) {
                final PutCache<K, V> inner =
                    new LruPutCache.Builder().build(c, cacheSize);
                return new PutCache<K, V>() {
                    @Override
                    public void put(K key, V value) {
                        putThreads.add(Thread.currentThread());
                        inner.put(key, value);
                    }

                    @Override
                    public V get(K key) {
                        return inner.get(key);
                    }

                    @Override
                    public void close() throws java.io.IOException {
                        inner.close();
                    }
                };
            }
        };
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .setCache(new CacheConfiguration(recording, 10))
                .setLazyLoading(
                    new LazyLoadingConfiguration(LAZY_TIMEOUT_MILLIS))
                .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();
            assertEquals(jsonResponse,
                data.get(CloudRequestData.class).getJsonResponse());
        }

        assertEquals(1, putThreads.size(),
            "The result was not offered to the cache once.");
        assertSame(Thread.currentThread(), putThreads.get(0),
            "The processing task read the Flow Data to cache the result.");
    }

    /**
     * After a failed request, the next result for the same Evidence takes
     * the failed entry's place, so the request after that is served from the
     * cache.
     */
    @Test
    public void process_ResultAfterAFailureIsCached() throws Exception {
        String second = "{'device':{'value':'1'}}";
        CountDownLatch gate = new CountDownLatch(1);
        mockService(new CountDownLatch(2), gate, null, second);
        CloudRequestEngine engine = buildLazyCachedEngine();

        assertEquals(Arrays.asList(FAILED, second, second),
            processThreeTimes(engine, gate));
        verifyDataRequests(2);
    }

    /**
     * After a response with a 51Did, the next result for the same Evidence
     * takes its place, so the request after that is served from the cache.
     * The cache is configured as the cloud pipeline builders configure it.
     */
    @Test
    public void process_ResultAfterAFodIdIsCached() throws Exception {
        String first = "{'device':{'value':'1'},'fodid':'first'}";
        String second = "{'device':{'value':'1'}}";
        CountDownLatch gate = new CountDownLatch(1);
        mockService(new CountDownLatch(2), gate, first, second);
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .setCache(new CacheConfiguration(10))
                .build();

        assertEquals(Arrays.asList(first, second, second),
            processThreeTimes(engine, gate));
        verifyDataRequests(2);
    }

    /**
     * What processThreeTimes records for a request whose response could not
     * be read.
     */
    private static final String FAILED = "failed";

    /**
     * Process three Flow Data with the same Evidence and read each response.
     * The first request is held back until its process has returned, so its
     * result is offered to the cache while the request is still running.
     */
    private List<String> processThreeTimes(
        CloudRequestEngine engine,
        CountDownLatch firstGate) throws Exception {
        List<String> answers = new ArrayList<>();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build()) {
            for (int i = 0; i < 3; i++) {
                try (FlowData data = pipeline.createFlowData()) {
                    data.addEvidence("query.User-Agent", "iPhone");
                    data.process();
                    firstGate.countDown();
                    try {
                        answers.add(data.get(CloudRequestData.class)
                            .getJsonResponse());
                    } catch (RuntimeException e) {
                        answers.add(FAILED);
                    }
                }
            }
        }
        return answers;
    }

    private void verifyDataRequests(int count) throws Exception {
        verify(httpClient, times(count)).postData(
            argThat(c -> c.getURL().getPath().endsWith("properties") == false &&
                c.getURL().getPath().endsWith("evidencekeys") == false),
            any(),
            any());
    }

    private List<String> processTwice(CloudRequestEngine engine)
        throws Exception {
        List<String> answers = new ArrayList<>();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build()) {
            for (int i = 0; i < 2; i++) {
                try (FlowData data = pipeline.createFlowData()) {
                    data.addEvidence("query.User-Agent", "iPhone");
                    data.process();
                    answers.add(data.get(CloudRequestData.class)
                        .getJsonResponse());
                }
            }
        }
        return answers;
    }
}
