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

package fiftyone.ipintelligence.engine.onpremise.flowelements;

import fiftyone.ipintelligence.engine.onpremise.interop.swig.ResultsIpiSwig;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Reads of finished IP Intelligence on-premise results are safe from
 * several threads, because only one thread at a time reads from the native
 * results. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/thread-safety.md#L57">thread-safety.md line 57</a>
 * and
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/thread-safety.md#L61">line 61</a>:
 * "Such Element Data MUST let only one thread at a time read from its
 * results".
 * <p>
 * As in the Device Detection test, the native results are a mock that
 * holds the first thread's read, and the second thread is checked from its
 * state, so nothing depends on timing.
 */
public class IPIntelligenceDataHashThreadSafetyTests {

    private static final long DEADLINE_MILLIS = 60_000;

    private static final Class<?>[] WEIGHTED_STRINGS =
        new Class<?>[] { List.class, IWeightedValue.class, String.class };

    private static class Gate implements Answer<Object> {
        final AtomicInteger inside = new AtomicInteger();
        final AtomicInteger most = new AtomicInteger();
        final CountDownLatch firstInside = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        volatile Thread first;

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            String name = invocation.getMethod().getName();
            if (name.equals("close") || name.equals("delete") ||
                name.equals("finalize")) {
                return null;
            }
            int now = inside.incrementAndGet();
            most.accumulateAndGet(now, Math::max);
            try {
                if (Thread.currentThread() == first &&
                    firstInside.getCount() > 0) {
                    firstInside.countDown();
                    assertTrue(release.await(
                        DEADLINE_MILLIS, TimeUnit.MILLISECONDS));
                }
                Class<?> type = invocation.getMethod().getReturnType();
                if (type == boolean.class) {
                    return true;
                }
                if (type == int.class || type == long.class) {
                    return 0;
                }
                if (type == String.class) {
                    return "";
                }
                if (type == void.class) {
                    return null;
                }
                return mock(type);
            } finally {
                inside.decrementAndGet();
            }
        }
    }

    private IPIntelligenceDataHashDefault newData(Gate gate) {
        FlowData flowData = mock(FlowData.class);
        when(flowData.getPipeline()).thenReturn(mock(Pipeline.class));
        IPIntelligenceDataHashDefault data = new IPIntelligenceDataHashDefault(
            mock(Logger.class),
            flowData,
            mock(IPIntelligenceOnPremiseEngine.class),
            mock(MissingPropertyService.class));
        data.setResults(mock(ResultsIpiSwig.class, gate));
        return data;
    }

    private void assertOneReaderAtATime(
        Consumer<IPIntelligenceDataHashDefault> firstRead,
        Consumer<IPIntelligenceDataHashDefault> secondRead) throws Exception {
        Gate gate = new Gate();
        IPIntelligenceDataHashDefault data = newData(gate);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread first = new Thread(() -> run(firstRead, data, failure));
        gate.first = first;
        first.start();
        assertTrue("The first read did not reach the native results.",
            gate.firstInside.await(DEADLINE_MILLIS, TimeUnit.MILLISECONDS));

        Thread second = new Thread(() -> run(secondRead, data, failure));
        second.start();
        long deadline = System.currentTimeMillis() + DEADLINE_MILLIS;
        while (gate.most.get() < 2 &&
            second.isAlive() &&
            waitingForTheLock(second) == false) {
            assertTrue("The second read neither waited nor read.",
                System.currentTimeMillis() < deadline);
            Thread.sleep(1);
        }

        gate.release.countDown();
        first.join(DEADLINE_MILLIS);
        second.join(DEADLINE_MILLIS);
        assertNull("A read failed: " + failure.get(), failure.get());
        assertEquals(
            "Two threads read from the native results at once.",
            1,
            gate.most.get());
    }

    private static void run(
        Consumer<IPIntelligenceDataHashDefault> read,
        IPIntelligenceDataHashDefault data,
        AtomicReference<Throwable> failure) {
        try {
            read.accept(data);
        } catch (Throwable t) {
            failure.compareAndSet(null, t);
        }
    }

    private static boolean waitingForTheLock(Thread thread) {
        if (thread.getState() != Thread.State.BLOCKED) {
            return false;
        }
        StackTraceElement[] stack = thread.getStackTrace();
        return stack.length > 0 &&
            stack[0].getClassName().startsWith("fiftyone.ipintelligence.");
    }

    @Test
    public void getValues_WaitsForAnotherGetValues() throws Exception {
        assertOneReaderAtATime(
            d -> d.getValues("RegisteredName", WEIGHTED_STRINGS),
            d -> d.getValues("RegisteredName", WEIGHTED_STRINGS));
    }

    @Test
    public void getValues_WaitsForAPropertyGetter() throws Exception {
        assertOneReaderAtATime(
            d -> d.getCountryCodesGeographical(),
            d -> d.getValues("RegisteredName", WEIGHTED_STRINGS));
    }

    @Test
    public void propertyGetter_WaitsForGetValues() throws Exception {
        assertOneReaderAtATime(
            d -> d.getValues("RegisteredName", WEIGHTED_STRINGS),
            d -> d.getCountryCodesGeographical());
    }
}
