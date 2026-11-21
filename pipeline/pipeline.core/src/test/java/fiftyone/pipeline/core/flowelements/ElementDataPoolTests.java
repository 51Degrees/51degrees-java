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

package fiftyone.pipeline.core.flowelements;

import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Element Data that can be pooled is given out again once the Flow Data that
 * held it is closed, so a Flow Element allocates one instance for each
 * request running at once, not one for every request. A pooled instance
 * holds nothing of the request it served before.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Specification</a>
 */
public class ElementDataPoolTests {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(ElementDataPoolTests.class);

    private static final int REQUESTS = 1000;

    private static final int THREADS = 8;

    private static final long WAIT_SECONDS = 60;

    /**
     * Element Data whose only state is its map, so it can be pooled.
     */
    public static class PooledData extends ElementDataBase {
        public PooledData(Logger logger, FlowData flowData) {
            super(logger, flowData);
        }

        protected boolean isPoolable() {
            return true;
        }
    }

    /**
     * Counts the Element Data it allocates, and writes the request's value
     * into its own, with a mark on the first request only.
     */
    private static class Counter
        extends FlowElementBase<PooledData, ElementPropertyMetaData> {
        private final AtomicInteger allocated;

        Counter(AtomicInteger allocated) {
            super(LOGGER, (flowData, element) -> {
                allocated.incrementAndGet();
                return new PooledData(LOGGER, flowData);
            });
            this.allocated = allocated;
        }

        @Override
        protected void processInternal(FlowData data) {
            PooledData own = data.getOrAdd(getTypedDataKey(), getDataFactory());
            Object request = data.getEvidence().get("query.request");
            assertNull("A pooled instance kept the last request's value",
                own.get("request"));
            own.put("request", request);
            if ("0".equals(request)) {
                own.put("first", "only");
            }
        }

        @Override
        public String getElementDataKey() {
            return "counter";
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return new EvidenceKeyFilterWhitelist(
                Collections.singletonList("query.request"));
        }

        @Override
        public List<ElementPropertyMetaData> getProperties() {
            return Collections.emptyList();
        }

        @Override
        protected void managedResourcesCleanup() {
        }

        @Override
        protected void unmanagedResourcesCleanup() {
        }
    }

    /**
     * Element Data that can be pooled and that a lazy task writes, until the
     * task the test gives it has finished.
     */
    public static class TaskData extends ElementDataBase {
        private volatile CompletableFuture<Void> task = null;

        public TaskData(Logger logger, FlowData flowData) {
            super(logger, flowData);
        }

        protected boolean isPoolable() {
            return true;
        }

        protected boolean hasLazyTaskRunning() {
            return task != null && task.isDone() == false;
        }
    }

    /**
     * Gives its Element Data the next task the test sets.
     */
    private static class TaskStarter
        extends FlowElementBase<TaskData, ElementPropertyMetaData> {
        private volatile CompletableFuture<Void> next = null;

        TaskStarter() {
            super(LOGGER,
                (flowData, element) -> new TaskData(LOGGER, flowData));
        }

        @Override
        protected void processInternal(FlowData data) {
            data.getOrAdd(getTypedDataKey(), getDataFactory()).task = next;
        }

        @Override
        public String getElementDataKey() {
            return "task";
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return new EvidenceKeyFilterWhitelist(Collections.emptyList());
        }

        @Override
        public List<ElementPropertyMetaData> getProperties() {
            return Collections.emptyList();
        }

        @Override
        protected void managedResourcesCleanup() {
        }

        @Override
        protected void unmanagedResourcesCleanup() {
        }
    }

    private static TaskData process(Pipeline pipeline) throws Exception {
        try (FlowData data = pipeline.createFlowData()) {
            data.process();
            return (TaskData) data.get("task");
        }
    }

    private static void request(Pipeline pipeline, int i) throws Exception {
        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.request", Integer.toString(i));
            data.process();
            ElementDataBase own = (ElementDataBase) data.get("counter");
            assertEquals(Integer.toString(i), own.get("request"));
            if (i != 0) {
                assertNull("A pooled instance kept the first request's mark",
                    own.get("first"));
            }
        }
    }

    @Test
    public void oneThread_AllocatesOneInstance() throws Exception {
        AtomicInteger allocated = new AtomicInteger();
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .addFlowElement(new Counter(allocated))
                .setAutoCloseElements(true)
                .build()) {
            for (int i = 0; i < REQUESTS; i++) {
                request(pipeline, i);
            }
        }
        assertEquals(
            "Element Data allocated for " + REQUESTS + " requests one after " +
                "another",
            1,
            allocated.get());
    }

    @Test
    public void manyThreads_AllocatesNoMoreThanRunAtOnce() throws Exception {
        AtomicInteger allocated = new AtomicInteger();
        ExecutorService threads = Executors.newFixedThreadPool(THREADS);
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .addFlowElement(new Counter(allocated))
                .setAutoCloseElements(true)
                .build()) {
            List<Future<?>> done = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                final int first = t * REQUESTS + 1;
                done.add(threads.submit(() -> {
                    for (int i = first; i < first + REQUESTS; i++) {
                        request(pipeline, i);
                    }
                    return null;
                }));
            }
            for (Future<?> each : done) {
                each.get(WAIT_SECONDS, TimeUnit.SECONDS);
            }
        } finally {
            threads.shutdownNow();
        }
        assertTrue(
            "Element Data allocated for " + (THREADS * REQUESTS) +
                " requests on " + THREADS + " threads: " + allocated.get(),
            allocated.get() <= THREADS);
    }

    @Test
    public void runningLazyTask_KeepsTheInstanceFromItsPool() throws Exception {
        TaskStarter starter = new TaskStarter();
        CompletableFuture<Void> running = new CompletableFuture<>();
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .addFlowElement(starter)
                .setAutoCloseElements(true)
                .build()) {
            starter.next = running;
            TaskData first = process(pipeline);

            starter.next = CompletableFuture.completedFuture(null);
            TaskData second = process(pipeline);
            assertNotSame(
                "An instance went back to its pool while its lazy task ran",
                first,
                second);

            TaskData third = process(pipeline);
            assertSame(
                "An instance whose lazy task had finished was not pooled",
                second,
                third);
        } finally {
            running.complete(null);
        }
    }

    /**
     * Closing a Flow Data a second time, as a try-with-resources around one
     * that a web filter also closes does, must not give back an instance
     * that the pool has since given to another request.
     */
    @Test
    public void closingTwice_GivesNoInstanceToTwoRequests() throws Exception {
        AtomicInteger allocated = new AtomicInteger();
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .addFlowElement(new Counter(allocated))
                .setAutoCloseElements(true)
                .build();
             FlowData first = pipeline.createFlowData()) {
            first.addEvidence("query.request", "first");
            first.process();
            Object pooled = first.get("counter");
            first.close();

            try (FlowData second = pipeline.createFlowData()) {
                second.addEvidence("query.request", "second");
                second.process();
                assertSame(
                    "The instance was not reused, so this proves nothing",
                    pooled,
                    second.get("counter"));

                first.close();

                try (FlowData third = pipeline.createFlowData()) {
                    third.addEvidence("query.request", "third");
                    third.process();
                    assertNotSame(
                        "A second close gave an instance that another " +
                            "request holds back to its pool",
                        pooled,
                        third.get("counter"));
                }
                assertEquals(
                    "The second request's value was overwritten",
                    "second",
                    ((ElementDataBase) second.get("counter")).get("request"));
            }
        }
        assertEquals(
            "Element Data allocated for three requests, two at once",
            2,
            allocated.get());
    }
}
