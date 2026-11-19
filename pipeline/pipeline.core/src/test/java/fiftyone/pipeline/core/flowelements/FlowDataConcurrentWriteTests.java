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

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByFiveElement;
import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByTenElement;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Flow Data of a Pipeline with parallel elements takes Element Data and
 * errors from several threads, with no lock to add Element Data and errors
 * kept whole. See
 * <a href="https://github.com/51Degrees/specifications/blob/d79bbe3b2aef86273570742f6b660f29bfe72816/pipeline-specification/features/thread-safety.md#L88-L115">thread-safety.md lines 88 to 115</a>:
 * a Flow Element adds its Element Data with one compare and swap, readers
 * take no lock, and errors that members add while a group runs are added
 * under a lock.
 * <p>
 * The first writer is held inside its write until the test lets it go,
 * and the second is checked from what it has done, so nothing depends on
 * how long anything takes. The deadline only stops a broken run from
 * hanging.
 */
public class FlowDataConcurrentWriteTests {

    private static final long DEADLINE_MILLIS = 60_000;

    /**
     * Loggers that write nothing, as the errors test logs thousands.
     */
    private final ILoggerFactory loggerFactory = name -> mock(Logger.class);

    private Pipeline pipeline(boolean parallel) throws Exception {
        PipelineBuilder builder = new PipelineBuilder(loggerFactory);
        if (parallel) {
            builder.addFlowElementsParallel(new FlowElement<?, ?>[] {
                new MultiplyByFiveElement(mock(Logger.class)),
                new MultiplyByTenElement(mock(Logger.class)) });
        } else {
            builder.addFlowElement(
                new MultiplyByFiveElement(mock(Logger.class)));
        }
        return builder.build();
    }

    /**
     * Two threads that add Element Data under the same key at once do not
     * wait for each other, and both get the one instance that was kept.
     */
    @Test
    public void getOrAdd_SameKeyFromTwoThreads_NeitherWaits()
        throws Exception {
        try (Pipeline pipeline = pipeline(true);
             FlowData flowData = pipeline.createFlowData()) {
            assertTrue(pipeline.isConcurrent());
            AtomicInteger created = new AtomicInteger();
            CountDownLatch bothCreating = new CountDownLatch(2);
            CountDownLatch release = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            List<ElementData> results = new ArrayList<>();

            FlowElement.DataFactory<ElementData> factory = data -> {
                created.incrementAndGet();
                bothCreating.countDown();
                try {
                    release.await(DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return mock(ElementData.class);
            };
            Runnable add = () -> {
                try {
                    ElementData result = flowData.getOrAdd("shared", factory);
                    synchronized (results) {
                        results.add(result);
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            };

            Thread first = new Thread(add);
            Thread second = new Thread(add);
            first.start();
            second.start();
            // Both writers reach the factory while neither has finished, so
            // neither waited for the other.
            boolean neitherWaited = bothCreating.await(
                DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
            release.countDown();
            first.join(DEADLINE_MILLIS);
            second.join(DEADLINE_MILLIS);

            assertNull("A write failed: " + failure.get(), failure.get());
            assertTrue("One writer waited for the other.", neitherWaited);
            assertEquals(2, created.get());
            assertEquals(2, results.size());
            assertSame(results.get(0), results.get(1));
        }
    }

    /**
     * Errors added from several threads at once are all kept.
     */
    @Test
    public void addError_FromSeveralThreads_KeepsEveryError()
        throws Exception {
        final int threads = 8;
        final int perThread = 1000;
        try (Pipeline pipeline = pipeline(true);
             FlowData flowData = pipeline.createFlowData()) {
            CountDownLatch start = new CountDownLatch(1);
            FlowElement<?, ?> element = mock(FlowElement.class);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            List<Thread> workers = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                Thread worker = new Thread(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        return;
                    }
                    try {
                        for (int i = 0; i < perThread; i++) {
                            flowData.addError(new FlowError.Default(
                                new Exception("error"), element));
                        }
                    } catch (Throwable e) {
                        failure.compareAndSet(null, e);
                    }
                });
                workers.add(worker);
                worker.start();
            }
            start.countDown();
            for (Thread worker : workers) {
                worker.join(DEADLINE_MILLIS);
                assertFalse(worker.isAlive());
            }
            assertNull("A write failed: " + failure.get(), failure.get());
            assertEquals(threads * perThread, flowData.getErrors().size());
        }
    }

    /**
     * Without parallel elements, adding Element Data takes no lock, so a
     * second writer for another key is not held up by the first.
     */
    @Test
    public void getOrAdd_WithoutParallelElements_TakesNoLock()
        throws Exception {
        try (Pipeline pipeline = pipeline(false);
             FlowData flowData = pipeline.createFlowData()) {
            assertFalse(pipeline.isConcurrent());
            CountDownLatch firstCreating = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();

            Thread first = new Thread(() -> {
                try {
                    flowData.getOrAdd("first", data -> {
                        firstCreating.countDown();
                        try {
                            release.await(
                                DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return mock(ElementData.class);
                    });
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            });
            first.start();
            assertTrue(firstCreating.await(
                DEADLINE_MILLIS, TimeUnit.MILLISECONDS));
            Thread second = new Thread(() -> {
                try {
                    flowData.getOrAdd("second",
                        data -> mock(ElementData.class));
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            });
            second.start();
            second.join(DEADLINE_MILLIS);
            boolean secondFinished = second.isAlive() == false;
            release.countDown();
            first.join(DEADLINE_MILLIS);

            assertNull("A write failed: " + failure.get(), failure.get());
            assertTrue("The second writer waited for the first.",
                secondFinished);
        }
    }
}
