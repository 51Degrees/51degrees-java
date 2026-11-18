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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
}
