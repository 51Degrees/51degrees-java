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

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.Evidence;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The Cloud Request Engine's json-response is a lazy property whose task is
 * the HTTP request. The Engine builds the request while it processes, starts
 * it and returns without waiting for the response. Every reader waits on the
 * task, and the task never reads the Flow Data. A failed request met by Cloud
 * Aspect Engines during processing is recorded once, as the Cloud Request
 * Engine's error, and one met after processing reaches only the reader.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/cloud-request-engine.md#processing">Specification</a>
 */
public class CloudRequestEngineLazyTaskTests
    extends CloudRequestEngineTestsBase {

    private static final long WAIT_SECONDS = 10;

    private final CountDownLatch release = new CountDownLatch(1);

    private final ExecutorService threads = Executors.newCachedThreadPool();

    private volatile boolean failRequest = false;

    public CloudRequestEngineLazyTaskTests() throws Exception {
        super();
    }

    @AfterEach
    public void cleanup() {
        release.countDown();
        threads.shutdownNow();
    }

    /**
     * Answer discovery at once, and hold the data request until
     * {@link #release} is counted down.
     */
    private void configureHeldClient() throws IOException {
        configureMockedClient();
        doAnswer(invocation -> {
            URL url = ((HttpURLConnection) invocation.getArgument(0))
                .getURL();
            if (url.getPath().endsWith("properties")) {
                return accessiblePropertiesResponse;
            }
            if (url.getPath().endsWith("evidencekeys")) {
                return evidenceKeysResponse;
            }
            assertTrue(release.await(WAIT_SECONDS, TimeUnit.SECONDS));
            if (failRequest) {
                throw new IOException("The request failed");
            }
            return jsonResponse;
        })
            .when(httpClient)
            .postData(
                any(HttpURLConnection.class),
                ArgumentMatchers.<String, String>anyMap(),
                (byte[]) any());
    }

    private CloudRequestEngine buildEngine() throws Exception {
        return new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey("resource_key")
            .build();
    }

    @Test
    public void process_ReturnsBeforeTheResponseAndEveryReaderWaits()
        throws Exception {
        configureHeldClient();
        CloudRequestEngine engine = buildEngine();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");

            Future<?> processing = threads.submit(() -> {
                data.process();
                return null;
            });
            // The response is held, so process returns only if it does not
            // wait for it.
            processing.get(WAIT_SECONDS, TimeUnit.SECONDS);

            CloudRequestData requestData = data.getFromElement(engine);
            List<Future<String>> readers = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                readers.add(threads.submit(() -> requestData.getJsonResponse()));
            }
            for (Future<String> reader : readers) {
                assertFalse(reader.isDone(),
                    "A reader returned before the response arrived");
            }
            release.countDown();
            for (Future<String> reader : readers) {
                assertEquals(
                    jsonResponse,
                    reader.get(WAIT_SECONDS, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void process_TheRequestsTaskDoesNotReadTheFlowData()
        throws Exception {
        configureHeldClient();
        CloudRequestEngine engine = buildEngine();

        Map<String, Object> evidenceMap = new HashMap<>();
        evidenceMap.put("query.User-Agent", "iPhone");
        Evidence evidence = mock(Evidence.class);
        when(evidence.asKeyMap()).thenReturn(evidenceMap);
        FlowData data = mock(FlowData.class);
        when(data.getEvidence()).thenReturn(evidence);
        CloudRequestData[] made = {null};
        when(data.getOrAdd(
            any(TypedKey.class),
            any(FlowElement.DataFactory.class))).thenAnswer(invocation -> {
                if (made[0] == null) {
                    made[0] = ((FlowElement.DataFactory<CloudRequestData>)
                        invocation.getArgument(1)).create(data);
                }
                return made[0];
            });

        threads.submit(() -> {
            engine.process(data);
            return null;
        }).get(WAIT_SECONDS, TimeUnit.SECONDS);
        assertNotNull(made[0]);

        clearInvocations(data, evidence);
        release.countDown();
        assertEquals(jsonResponse, made[0].getJsonResponse());
        verifyNoInteractions(data, evidence);
        engine.close();
    }

    @Test
    public void process_FailureMetByAspectEngines_RecordedOnceAsTheRequests()
        throws Exception {
        failRequest = true;
        configureHeldClient();
        release.countDown();
        CloudRequestEngine engine = buildEngine();
        TestAspectEngine first = new TestAspectEngine("first");
        TestAspectEngine second = new TestAspectEngine("second");
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(engine)
                .addFlowElement(first)
                .addFlowElement(second)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();

            List<FlowError> errors = new ArrayList<>(data.getErrors());
            assertEquals(1, errors.size(), errors.toString());
            assertSame(engine, errors.get(0).getFlowElement());
            assertTrue(
                errors.get(0).getThrowable() instanceof IOException,
                String.valueOf(errors.get(0).getThrowable()));
        }
    }

    @Test
    public void read_FailureAfterProcessing_ReachesOnlyTheReader()
        throws Exception {
        failRequest = true;
        configureHeldClient();
        CloudRequestEngine engine = buildEngine();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();
            release.countDown();

            CloudRequestData requestData = data.getFromElement(engine);
            RuntimeException read = assertThrows(
                RuntimeException.class,
                requestData::getJsonResponse);
            assertTrue(read.getCause() instanceof IOException,
                String.valueOf(read));
            assertTrue(data.getErrors().isEmpty(),
                String.valueOf(data.getErrors()));
        }
    }

    public static class TestData extends AspectDataBase {
        public TestData(
            org.slf4j.Logger logger,
            FlowData flowData,
            AspectEngine<? extends AspectData,
                ? extends AspectPropertyMetaData> engine) {
            super(
                logger,
                flowData,
                engine,
                MissingPropertyServiceDefault.getInstance());
        }
    }

    private class TestAspectEngine extends CloudAspectEngineBase<TestData> {
        private final String key;

        @SuppressWarnings("unchecked")
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
}
