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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.configuration.ExecutorServiceFactory;
import fiftyone.pipeline.engines.configuration.LazyLoadingConfiguration;
import fiftyone.pipeline.engines.testhelpers.data.EmptyEngineData;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngine;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngineBuilder;
import org.junit.After;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The deprecated lazy loading option has no effect. An Engine with it set
 * processes on the thread that processes the Flow Data, uses no executor,
 * and gives the same values as one without it, whatever the timeout.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/advanced-features/lazy-loading.md">Specification</a>
 */
public class LazyLoadingHasNoEffectTests {

    private static final long PROCESS_COST_MILLIS = 200;

    /**
     * How much sooner than the process cost process may seem to return. On
     * Windows, Thread.sleep can wake a millisecond or two early by the
     * System.nanoTime clock. Processing in the background would return
     * almost at once, far below this.
     */
    private static final long SLEEP_EARLY_TOLERANCE_MILLIS = 20;

    private static final int LONG_TIMEOUT_MILLIS = 5000;

    private static final int SHORT_TIMEOUT_MILLIS = 10;

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    private final AtomicInteger executorsCreated = new AtomicInteger();

    private final AtomicInteger threadsStarted = new AtomicInteger();

    private ExecutorService executor;

    @After
    public void cleanup() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private ExecutorServiceFactory countingFactory() {
        return new ExecutorServiceFactory() {
            @Override
            public ExecutorService create() {
                executorsCreated.incrementAndGet();
                executor = Executors.newSingleThreadExecutor(runnable -> {
                    threadsStarted.incrementAndGet();
                    return new Thread(runnable);
                });
                return executor;
            }
        };
    }

    private void processAndCheck(EmptyEngine engine) throws Exception {
        engine.setProcessCost(PROCESS_COST_MILLIS);
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("test.value", "1");
            long start = System.nanoTime();
            data.process();
            long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

            EmptyEngineData result = data.get(EmptyEngineData.class);
            assertTrue(
                "Processing should be finished when process returns",
                result.getProcessFuture().isDone());
            assertTrue(
                "process should return only once the Engine has processed, " +
                    "but returned after " + elapsedMillis + " ms",
                elapsedMillis >=
                    PROCESS_COST_MILLIS - SLEEP_EARLY_TOLERANCE_MILLIS);
            assertEquals(1, result.getValueOne());
            assertEquals(2, result.getValueTwo());
        }
        assertEquals(
            "No thread should be started for lazy loading",
            0,
            threadsStarted.get());
    }

    @Test
    public void lazyLoading_NotSet_ProcessesOnCallingThread()
        throws Exception {
        processAndCheck(new EmptyEngineBuilder(loggerFactory).build());
    }

    @Test
    public void lazyLoading_LongTimeout_ProcessesOnCallingThread()
        throws Exception {
        processAndCheck(new EmptyEngineBuilder(loggerFactory)
            .setLazyLoading(new LazyLoadingConfiguration(
                LONG_TIMEOUT_MILLIS,
                countingFactory()))
            .build());
    }

    @Test
    public void lazyLoading_TimeoutShorterThanProcessing_GivesValues()
        throws Exception {
        processAndCheck(new EmptyEngineBuilder(loggerFactory)
            .setLazyLoading(new LazyLoadingConfiguration(
                SHORT_TIMEOUT_MILLIS,
                countingFactory()))
            .build());
    }

    @Test
    public void lazyLoading_Set_CreatesNoExecutor() throws Exception {
        EmptyEngine engine = new EmptyEngineBuilder(loggerFactory)
            .setLazyLoading(new LazyLoadingConfiguration(
                LONG_TIMEOUT_MILLIS,
                countingFactory()))
            .build();
        try {
            assertEquals(
                "Setting lazy loading should create no executor",
                0,
                executorsCreated.get());
        } finally {
            engine.close();
        }
    }
}
