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

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * A lazy loading Cloud Request Engine with a cache returns from process
 * without waiting for the cloud service, and still never serves a response
 * with a 51Did from the cache. Lazy loading means "the Engine returns from process
 * at once and the work is done on another thread", which the check of
 * <a href="https://github.com/51Degrees/specifications/blob/d79bbe3b2aef86273570742f6b660f29bfe72816/pipeline-specification/features/caching.md#L151-L156">caching.md</a>
 * ("A cache MUST NOT store a result that holds such a value, and MUST NOT
 * serve one that it finds.") must not undo.
 * <p>
 * No result depends on timing: the cloud service's answer is held back until
 * the test releases it, and the test reads the processing thread's stack to
 * see whether it is waiting for that answer.
 */
public class CloudRequestEngineLazyCacheTests
    extends CloudRequestEngineTestsBase {

    /**
     * Long enough that a process which waits for the held back answer is
     * still waiting when the test looks.
     */
    private static final int LAZY_TIMEOUT_MILLIS = 600000;

    public CloudRequestEngineLazyCacheTests() throws MalformedURLException {
        super();
    }

    /**
     * Mock the cloud service so that each data request takes the next
     * response in the list, and waits for the gate first.
     */
    private void mockService(
        final CountDownLatch called,
        final CountDownLatch gate,
        String... responses) throws Exception {
        evidenceKeysResponse =
            new JSONArray(Arrays.asList("query.User-Agent")).toString();
        configureMockedClient();
        final List<String> remaining = new ArrayList<>(Arrays.asList(responses));
        doAnswer(invocation -> {
            HttpURLConnection connection = invocation.getArgument(0);
            String path = connection.getURL().getPath();
            if (path.endsWith("properties")) {
                return accessiblePropertiesResponse;
            }
            if (path.endsWith("evidencekeys")) {
                return evidenceKeysResponse;
            }
            called.countDown();
            gate.await();
            return remaining.remove(0);
        }).when(httpClient).postData(any(), anyMap(), (byte[]) any());
    }

    private CloudRequestEngine buildLazyCachedEngine() throws Exception {
        return new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey("resourcekey")
            .setCacheSize(10)
            .setLazyLoading(new LazyLoadingConfiguration(LAZY_TIMEOUT_MILLIS))
            .build();
    }

    private static boolean waitingForProcessing(Thread thread) {
        for (StackTraceElement frame : thread.getStackTrace()) {
            if (frame.getMethodName().equals("waitOnAllProcessFutures") ||
                frame.getClassName().endsWith("FutureTask") &&
                    frame.getMethodName().equals("get")) {
                return true;
            }
        }
        return false;
    }

    // Combined run only: rc's process_LazyCachedEngineDoesNotWaitForTheService
    // is removed here. It checks that a lazy loading engine returns before
    // the cloud service answers, which #202 ("make lazy loading have no
    // effect") removes on purpose: process now runs on the calling thread,
    // so the test's unbounded loop never ends. #202 must drop or rewrite it
    // when it merges this rc.

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
     * gets its own. The result is stored before it is processed, so the
     * check before serving is the one that keeps it out.
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
     * With lazy loading, the result is offered to the cache on the thread
     * that called process, so the processing task never reads the Flow Data
     * to make the cache key once process has returned.
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
            // Waits for the processing task to finish.
            assertEquals(jsonResponse,
                data.get(CloudRequestData.class).getJsonResponse());
        }

        assertEquals(1, putThreads.size(),
            "The result was not offered to the cache once.");
        assertSame(Thread.currentThread(), putThreads.get(0),
            "The processing task read the Flow Data to cache the result.");
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
