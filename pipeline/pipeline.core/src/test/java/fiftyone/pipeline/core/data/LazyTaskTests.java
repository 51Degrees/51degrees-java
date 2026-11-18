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

package fiftyone.pipeline.core.data;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A lazy property's task runs once, every reader waits for it, and a failed
 * task gives every waiting reader its error.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#lazy-properties">Specification</a>
 */
public class LazyTaskTests {

    private static final int READERS = 8;

    private static final long WAIT_SECONDS = 10;

    private final ExecutorService readers =
        Executors.newFixedThreadPool(READERS);

    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @After
    public void cleanup() {
        readers.shutdownNow();
        worker.shutdownNow();
    }

    private List<Future<Object>> readAll(LazyTask<?> task) {
        List<Future<Object>> results = new ArrayList<>();
        for (int i = 0; i < READERS; i++) {
            results.add(readers.submit(() -> (Object) task.get()));
        }
        return results;
    }

    private static void await(CountDownLatch latch) throws Exception {
        assertTrue(latch.await(WAIT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void get_ManyReaders_WorkRunsOnceAndEveryReaderWaits()
        throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        LazyTask<String> task = new LazyTask<>(() -> {
            runs.incrementAndGet();
            started.countDown();
            await(release);
            return "value";
        });

        List<Future<Object>> results = readAll(task);
        await(started);
        for (Future<Object> result : results) {
            assertFalse("A reader returned before the work finished",
                result.isDone());
        }
        release.countDown();

        for (Future<Object> result : results) {
            assertEquals("value", result.get(WAIT_SECONDS, TimeUnit.SECONDS));
        }
        assertEquals(1, runs.get());
        assertTrue(task.isDone());
    }

    @Test
    public void start_ReturnsBeforeTheWorkFinishes() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        LazyTask<String> task = new LazyTask<>(() -> {
            runs.incrementAndGet();
            started.countDown();
            await(release);
            return "value";
        }).start(worker);

        await(started);
        assertFalse(task.isDone());
        List<Future<Object>> results = readAll(task);
        release.countDown();

        for (Future<Object> result : results) {
            assertEquals("value", result.get(WAIT_SECONDS, TimeUnit.SECONDS));
        }
        assertEquals(1, runs.get());
    }

    @Test
    public void get_WorkFails_EveryReaderGetsTheError() throws Exception {
        IllegalStateException failure = new IllegalStateException("failed");
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        LazyTask<String> task = new LazyTask<>(() -> {
            started.countDown();
            await(release);
            throw failure;
        });

        List<Future<Object>> results = readAll(task);
        await(started);
        release.countDown();

        for (Future<Object> result : results) {
            try {
                result.get(WAIT_SECONDS, TimeUnit.SECONDS);
                fail("A reader got a value from failed work");
            } catch (java.util.concurrent.ExecutionException e) {
                assertSame(failure, e.getCause());
            }
        }
    }

    @Test
    public void get_WorkFailsWithCheckedException_ThrownAsCause() {
        IOException failure = new IOException("failed");
        LazyTask<String> task = new LazyTask<>(() -> {
            throw failure;
        });
        for (int i = 0; i < 2; i++) {
            try {
                task.get();
                fail("Failed work gave a value");
            } catch (CompletionException e) {
                assertSame(failure, e.getCause());
            }
        }
    }
}
