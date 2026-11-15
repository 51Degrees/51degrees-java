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

package fiftyone.devicedetection.hash.engine.onpremise.flowelements;

import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.ResultsHashSwig;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;

/**
 * Finished Element Data MUST be safe to read from several threads at once,
 * so Aspect Data that reads its values from a ResultsHash instance MUST let
 * only one thread at a time read from its results
 * (device-detection-specification/pipeline-elements/device-detection-on-premise.md
 * line 327 at 706de1f0). Each test reads the same data from two threads, and
 * the mock results record the most threads that were ever inside them at
 * once. A thread that gets in waits for the other to join it, up to a
 * limit. With a lock the other cannot join, so the wait runs out and one
 * thread was inside at a time. Without one, both are inside together. The
 * limit only makes a test with a lock take longer, never fail.
 */
public class ResultsOneThreadAtATimeTests {

    private static final long JOIN_WAIT_MILLIS = 500;

    /**
     * Answer for every native results call that counts the threads inside the
     * results, and gives a value of the type the call returns.
     */
    private static class CountingAnswer implements Answer<Object> {
        private final AtomicInteger inside = new AtomicInteger();
        private final AtomicInteger most = new AtomicInteger();
        private final CountDownLatch bothInside = new CountDownLatch(2);

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            String name = invocation.getMethod().getName();
            if (name.equals("close") || name.equals("delete") ||
                name.equals("finalize")) {
                return null;
            }
            int now = inside.incrementAndGet();
            most.accumulateAndGet(now, Math::max);
            bothInside.countDown();
            try {
                bothInside.await(JOIN_WAIT_MILLIS, TimeUnit.MILLISECONDS);
                Class<?> returns = invocation.getMethod().getReturnType();
                if (name.equals("getDeviceId")) {
                    return "0-0-0-0";
                }
                if (name.equals("containsProperty")) {
                    return true;
                }
                if (returns.getName().contains(".interop.swig.")) {
                    // A value with no value, as the native code gives for a
                    // property with none.
                    return mock(returns);
                }
                return RETURNS_DEFAULTS.answer(invocation);
            }
            finally {
                inside.decrementAndGet();
            }
        }
    }

    private static int mostThreadsInsideResults(
        Consumer<DeviceDataHashDefault> read) throws Exception {
        CountingAnswer answer = new CountingAnswer();
        ResultsHashSwig results = mock(ResultsHashSwig.class, answer);
        FlowData flowData = mock(FlowData.class);
        DeviceDataHashDefault data = new DeviceDataHashDefault(
            mock(Logger.class),
            flowData,
            mock(DeviceDetectionHashEngine.class),
            mock(MissingPropertyService.class));
        data.setResults(results);

        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> errors = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    read.accept(data);
                } catch (Throwable t) {
                    synchronized (errors) {
                        errors.add(t);
                    }
                }
            });
            thread.start();
            threads.add(thread);
        }
        start.countDown();
        for (Thread thread : threads) {
            thread.join();
        }
        if (errors.isEmpty() == false) {
            throw new AssertionError("A read failed", errors.get(0));
        }
        return answer.most.get();
    }

    /**
     * A match metric, such as DeviceId, reads the results too, and walks every
     * graph not yet walked.
     */
    @Test
    public void Results_MatchMetric_OneThreadAtATime() throws Exception {
        assertEquals(
            "Two threads read the native results at once for DeviceId",
            1,
            mostThreadsInsideResults(d -> d.getDeviceId()));
    }

    /**
     * A typed property read, which first asks the results whether they have
     * the property.
     */
    @Test
    public void Results_PropertyRead_OneThreadAtATime() throws Exception {
        assertEquals(
            "Two threads read the native results at once for IsMobile",
            1,
            mostThreadsInsideResults(d -> d.getIsMobile()));
    }

    /**
     * The public list read.
     */
    @Test
    public void Results_GetValues_OneThreadAtATime() throws Exception {
        assertEquals(
            "Two threads read the native results at once for getValues",
            1,
            mostThreadsInsideResults(d -> d.getValues("HardwareName")));
    }
}
