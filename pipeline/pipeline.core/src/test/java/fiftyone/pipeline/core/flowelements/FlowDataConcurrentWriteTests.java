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
 * Flow Data of a Pipeline with parallel elements takes writes from several
 * threads safely, and Flow Data of a Pipeline without them does not pay for
 * it. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/advanced-features/parallel-processing.md#L22">parallel-processing.md line 22</a>:
 * "Flow Data MUST be capable of enforcing thread-safe writes to data
 * structures while also maintaining performance when it is not needed."
 * <p>
 * The first writer is held inside its write until the test lets it go,
 * and the second is checked from its thread's state, so nothing depends
 * on how long anything takes. The deadline only stops a broken run from
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

    private static boolean blockedInFlowData(Thread thread) {
        if (thread.getState() != Thread.State.BLOCKED) {
            return false;
        }
        StackTraceElement[] stack = thread.getStackTrace();
        return stack.length > 0 &&
            stack[0].getClassName().equals(FlowDataDefault.class.getName());
    }

    /**
     * Two threads that add Element Data under the same key at once create
     * it once, and both get the same instance. The second waits while the
     * first creates it.
     */
    @Test
    public void getOrAdd_SameKeyFromTwoThreads_CreatesOnce() throws Exception {
        try (Pipeline pipeline = pipeline(true);
             FlowData flowData = pipeline.createFlowData()) {
            assertTrue(pipeline.isConcurrent());
            AtomicInteger created = new AtomicInteger();
            CountDownLatch firstCreating = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            List<ElementData> results = new ArrayList<>();

            FlowElement.DataFactory<ElementData> factory = data -> {
                created.incrementAndGet();
                firstCreating.countDown();
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
            first.start();
            assertTrue(firstCreating.await(
                DEADLINE_MILLIS, TimeUnit.MILLISECONDS));
            Thread second = new Thread(add);
            second.start();
            long deadline = System.currentTimeMillis() + DEADLINE_MILLIS;
            while (created.get() < 2 && second.isAlive() &&
                blockedInFlowData(second) == false) {
                assertTrue("The second writer neither waited nor wrote.",
                    System.currentTimeMillis() < deadline);
                Thread.sleep(1);
            }
            release.countDown();
            first.join(DEADLINE_MILLIS);
            second.join(DEADLINE_MILLIS);

            assertNull("A write failed: " + failure.get(), failure.get());
            assertEquals("The Element Data was created twice.",
                1, created.get());
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
