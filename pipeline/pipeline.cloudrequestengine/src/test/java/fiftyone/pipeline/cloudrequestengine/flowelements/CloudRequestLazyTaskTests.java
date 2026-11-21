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

import fiftyone.pipeline.cloudrequestengine.CloudRequestException;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.MissingPropertyReason;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The JSON response is a lazy property. The engine builds the request while
 * it processes, starts it as a task, and returns without waiting. Readers
 * wait on the task, and a failure met during processing is recorded once as
 * the engine's own error.
 */
public class CloudRequestLazyTaskTests extends CloudRequestEngineTestsBase {

    public CloudRequestLazyTaskTests() throws MalformedURLException {
        super();
    }

    public static class TestData extends AspectDataBase {
        public TestData(
            Logger logger,
            FlowData flowData,
            AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
            super(
                logger,
                flowData,
                engine,
                MissingPropertyServiceDefault.getInstance());
        }
    }

    private class TestAspectEngine extends CloudAspectEngineBase<TestData> {
        private final String key;

        TestAspectEngine(String key) {
            super(
                loggerFactory.getLogger("TestAspectEngine"),
                new ElementDataFactory<TestData>() {
                    @Override
                    public TestData create(
                        FlowData flowData,
                        FlowElement<TestData, ?> flowElement) {
                        return new TestData(
                            loggerFactory.getLogger("TestData"),
                            flowData,
                            (AspectEngine<TestData, ?>) flowElement);
                    }
                });
            this.key = key;
        }

        @Override
        public String getElementDataKey() {
            return key;
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return new EvidenceKeyFilterWhitelist(new ArrayList<String>());
        }

        @Override
        protected void unmanagedResourcesCleanup() {
        }

        @Override
        protected void processCloudEngine(
            FlowData data,
            TestData aspectData,
            String json) {
        }
    }

    private static final long WAIT_SECONDS = 10;

    /**
     * How long a call that should wait is given to return anyway.
     */
    private static final long HELD_MILLISECONDS = 300;

    private static final int READERS = 8;

    private static boolean await(CountDownLatch latch) {
        try {
            return latch.await(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Meets the failed request first, then holds its add of the failure to
     * the errors until the second reader has finished processing, as two
     * members of a parallel group can.
     */
    private class FirstReader extends TestAspectEngine {
        private final CountDownLatch adding;
        private final CountDownLatch release;

        FirstReader(CountDownLatch adding, CountDownLatch release) {
            super("first");
            this.adding = adding;
            this.release = release;
        }

        @Override
        protected void processEngine(FlowData data, TestData aspectData) {
            FlowData holding = mock(
                FlowData.class,
                AdditionalAnswers.delegatesTo(data));
            doAnswer(invocation -> {
                adding.countDown();
                await(release);
                data.addError(
                    invocation.<Throwable>getArgument(0),
                    invocation.<FlowElement<?, ?>>getArgument(1));
                return null;
            }).when(holding).addError(any(Throwable.class), any());
            super.processEngine(holding, aspectData);
        }
    }

    /**
     * Reads the response once the first reader is adding the failure.
     */
    private class SecondReader extends TestAspectEngine {
        private final CountDownLatch adding;
        private final CountDownLatch release;

        SecondReader(CountDownLatch adding, CountDownLatch release) {
            super("second");
            this.adding = adding;
            this.release = release;
        }

        @Override
        protected void processEngine(FlowData data, TestData aspectData) {
            try {
                await(adding);
                super.processEngine(data, aspectData);
            } finally {
                release.countDown();
            }
        }
    }

    /**
     * Hold the data request until processing has returned, and record
     * whether it did. The wait is bounded so an engine that waits for the
     * response fails the test rather than hanging.
     */
    private void holdDataRequest(
        CountDownLatch processReturned,
        AtomicBoolean answeredAfterProcess) throws Exception {
        doAnswer(invocation -> {
            URL url = ((HttpURLConnection) invocation.getArgument(0)).getURL();
            if (url.getPath().endsWith("properties")) {
                return accessiblePropertiesResponse;
            }
            if (url.getPath().endsWith("evidencekeys")) {
                return evidenceKeysResponse;
            }
            answeredAfterProcess.set(
                processReturned.await(10, TimeUnit.SECONDS));
            return jsonResponse;
        })
            .when(httpClient)
            .postData(
                any(HttpURLConnection.class),
                ArgumentMatchers.<String, String>anyMap(),
                (byte[]) any());
    }

    @Test
    public void Process_ReturnsBeforeTheResponse() throws Exception {
        configureMockedClient();
        CountDownLatch processReturned = new CountDownLatch(1);
        AtomicBoolean answeredAfterProcess = new AtomicBoolean(false);
        holdDataRequest(processReturned, answeredAfterProcess);
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resource_key")
                .build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");

            data.process();
            processReturned.countDown();

            assertEquals(jsonResponse,
                data.getFromElement(engine).getJsonResponse());
            assertTrue(answeredAfterProcess.get(),
                "Processing waited for the cloud response.");
        } finally {
            processReturned.countDown();
        }
    }

    @Test
    public void FailedRequest_AfterProcessing_ReachesOnlyTheReader()
        throws Exception {
        jsonResponse = "{\"errors\":[\"The request failed.\"]}";
        configureMockedClient();
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resource_key")
                .build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();

            assertThrows(CloudRequestException.class,
                () -> data.getFromElement(engine).getJsonResponse());
            assertThrows(CloudRequestException.class,
                () -> data.getFromElement(engine).getJsonResponse(),
                "Every reader gets the failure.");
            assertTrue(data.getErrors() == null || data.getErrors().isEmpty(),
                "A failure met after processing is not recorded.");
        }
    }

    @Test
    public void FailedRequest_MetWhileProcessing_RecordedOnceAsEngineError()
        throws Exception {
        jsonResponse = "{\"errors\":[\"The request failed.\"]}";
        configureMockedClient();
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resource_key")
                .build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(engine)
                .addFlowElement(new TestAspectEngine("first"))
                .addFlowElement(new TestAspectEngine("second"))
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();

            List<FlowError> errors = new ArrayList<>(data.getErrors());
            assertEquals(1, errors.size(), errors.toString());
            assertSame(engine, errors.get(0).getFlowElement());
            assertInstanceOf(CloudRequestException.class,
                errors.get(0).getThrowable());
        }
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void FailedRequest_EveryParallelReaderMarkedFailed()
        throws Exception {
        jsonResponse = "{\"errors\":[\"The request failed.\"]}";
        configureMockedClient();
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resource_key")
                .build();
        CountDownLatch adding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        FirstReader first = new FirstReader(adding, release);
        SecondReader second = new SecondReader(adding, release);
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(engine)
                .addFlowElementsParallel(new FlowElement[]{first, second})
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();

            assertEquals(1, data.getErrors().size(),
                data.getErrors().toString());
            for (TestAspectEngine reader : Arrays.asList(first, second)) {
                PropertyMissingException e = assertThrows(
                    PropertyMissingException.class,
                    () -> data.getFromElement(reader).get("value"));
                assertEquals(
                    MissingPropertyReason.CloudRequestFailed,
                    e.getReason(),
                    reader.getElementDataKey() + " was not marked as " +
                        "failed before the failure was recorded");
            }
        } finally {
            release.countDown();
        }
    }

    @Test
    public void ConcurrentReaders_OneRequestAndTheSameResponse()
        throws Exception {
        configureMockedClient();
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger dataRequests = new AtomicInteger();
        doAnswer(invocation -> {
            URL url = ((HttpURLConnection) invocation.getArgument(0)).getURL();
            if (url.getPath().endsWith("properties")) {
                return accessiblePropertiesResponse;
            }
            if (url.getPath().endsWith("evidencekeys")) {
                return evidenceKeysResponse;
            }
            dataRequests.incrementAndGet();
            await(release);
            return jsonResponse;
        })
            .when(httpClient)
            .postData(
                any(HttpURLConnection.class),
                ArgumentMatchers.<String, String>anyMap(),
                (byte[]) any());
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resource_key")
                .build();
        ExecutorService readers = Executors.newFixedThreadPool(READERS);
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();
            CloudRequestData requestData = data.getFromElement(engine);

            List<Future<String>> reads = new ArrayList<>();
            for (int i = 0; i < READERS; i++) {
                reads.add(readers.submit(requestData::getJsonResponse));
            }
            Thread.sleep(HELD_MILLISECONDS);
            for (Future<String> read : reads) {
                assertFalse(read.isDone(),
                    "A reader did not wait for the response");
            }
            release.countDown();
            for (Future<String> read : reads) {
                assertEquals(jsonResponse,
                    read.get(WAIT_SECONDS, TimeUnit.SECONDS));
            }
            assertEquals(1, dataRequests.get(),
                "The request was made more than once");
        } finally {
            release.countDown();
            readers.shutdownNow();
        }
    }

    @Test
    public void Task_DoesNotReadTheFlowData() throws Exception {
        configureMockedClient();
        CountDownLatch processReturned = new CountDownLatch(1);
        AtomicBoolean answeredAfterProcess = new AtomicBoolean(false);
        holdDataRequest(processReturned, answeredAfterProcess);
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resource_key")
                .build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            FlowData watched = mock(
                FlowData.class,
                AdditionalAnswers.delegatesTo(data));
            engine.process(watched);
            CloudRequestData requestData = data.getOrAdd(
                engine.getTypedDataKey(),
                flowData -> null);
            clearInvocations(watched);

            processReturned.countDown();
            assertEquals(jsonResponse, requestData.getJsonResponse());
            assertTrue(answeredAfterProcess.get());
            verifyNoInteractions(watched);
        } finally {
            processReturned.countDown();
        }
    }

    @Test
    public void Close_WaitsForARequestStillRunning() throws Exception {
        configureMockedClient();
        CountDownLatch processReturned = new CountDownLatch(1);
        AtomicBoolean answeredAfterProcess = new AtomicBoolean(false);
        holdDataRequest(processReturned, answeredAfterProcess);
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resource_key")
                .build();
        ExecutorService closer = Executors.newSingleThreadExecutor();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();
            CloudRequestData requestData = data.getFromElement(engine);

            Future<?> closed = closer.submit(() -> {
                engine.close();
                return null;
            });
            try {
                assertThrows(TimeoutException.class,
                    () -> closed.get(
                        HELD_MILLISECONDS,
                        TimeUnit.MILLISECONDS),
                    "Closing the engine did not wait for its request");
            } finally {
                processReturned.countDown();
            }
            closed.get(WAIT_SECONDS, TimeUnit.SECONDS);
            assertEquals(jsonResponse, requestData.getJsonResponse());
        } finally {
            processReturned.countDown();
            closer.shutdownNow();
        }
    }
}
