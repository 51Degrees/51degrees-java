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

import fiftyone.ipintelligence.engine.onpremise.TestsBase;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.EngineIpiSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.EvidenceIpiSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.ResultsIpiSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.StringValueSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.WeightedUTF8StringListValueSwig;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.ipintelligence.shared.IPIntelligenceDataBaseOnPremise;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.engines.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Several threads read one finished Element Data at once, through the
 * typed getters and through getValues, and only one of them at a time reads
 * from the native results. A stand-in for the native engine gives results
 * that count the threads inside them, and each read of them takes a moment
 * so that two reads at once would overlap.
 */
public class ResultsReadByOneThreadTests extends TestsBase {

    private static final int THREADS = 4;

    private static final int READS = 10;

    private static final long READ_HOLD_MILLIS = 2;

    private static final long WAIT_SECONDS = 60;

    private static final Class<?>[] WEIGHTED_STRINGS =
        { List.class, IWeightedValue.class, String.class };

    private final AtomicInteger inside = new AtomicInteger();

    private final AtomicInteger most = new AtomicInteger();

    @Before
    public void init() throws Exception {
        testInitialize(Constants.PerformanceProfiles.LowMemory);
    }

    @After
    public void cleanup() {
        testCleanup();
    }

    @Test
    public void SeveralThreads_OneReadsTheResultsAtATime() throws Exception {
        IPIntelligenceOnPremiseEngine engine = getWrapper().getEngine();
        EngineIpiSwig nativeEngine = mock(EngineIpiSwig.class);
        ResultsIpiSwig results = countingResults();
        when(nativeEngine.process(any(EvidenceIpiSwig.class)))
            .thenReturn(results);

        Field field = IPIntelligenceOnPremiseEngine.class
            .getDeclaredField("engine");
        field.setAccessible(true);
        Object real = field.get(engine);
        field.set(engine, nativeEngine);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try (FlowData data = getWrapper().getPipeline().createFlowData()) {
            data.addEvidence("query.client-ip", "116.154.188.222");
            data.process();
            IPIntelligenceData ipi = data.get(IPIntelligenceData.class);

            CountDownLatch start = new CountDownLatch(1);
            List<Future<Integer>> readers = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                boolean getter = t % 2 == 0;
                readers.add(executor.submit(read(ipi, getter, start)));
            }
            start.countDown();
            for (Future<Integer> reader : readers) {
                assertEquals(Integer.valueOf(READS),
                    reader.get(WAIT_SECONDS, TimeUnit.SECONDS));
            }
            assertTrue("Expected the stand-in results to be read.",
                most.get() > 0);
            assertEquals(
                "More than one thread read the native results at once.",
                1,
                most.get());
        } finally {
            executor.shutdownNow();
            field.set(engine, real);
        }
    }

    private Callable<Integer> read(
        IPIntelligenceData ipi,
        boolean getter,
        CountDownLatch start) {
        return () -> {
            start.await();
            int done = 0;
            for (int i = 0; i < READS; i++) {
                if (getter) {
                    assertEquals("Example",
                        ipi.getRegisteredName().getValue());
                }
                else {
                    ((IPIntelligenceDataBaseOnPremise) ipi).getValues(
                        "RegisteredName", WEIGHTED_STRINGS);
                }
                done++;
            }
            return done;
        };
    }

    /**
     * Results that count the threads inside any of their methods, holding
     * each call for a moment.
     */
    private ResultsIpiSwig countingResults() {
        StringValueSwig value = mock(StringValueSwig.class);
        when(value.hasValue()).thenReturn(true);
        when(value.getValue()).thenReturn("Example");
        WeightedUTF8StringListValueSwig list =
            mock(WeightedUTF8StringListValueSwig.class);
        when(list.hasValue()).thenReturn(false);
        when(list.getNoValueMessage()).thenReturn("No value");
        return mock(ResultsIpiSwig.class, invocation -> {
            int now = inside.incrementAndGet();
            most.accumulateAndGet(now, Math::max);
            try {
                Thread.sleep(READ_HOLD_MILLIS);
                switch (invocation.getMethod().getName()) {
                    case "containsProperty":
                        return true;
                    case "getValueAsString":
                        return value;
                    case "getValuesAsWeightedUTF8StringList":
                        return list;
                    default:
                        return RETURNS_DEFAULTS.answer(invocation);
                }
            }
            finally {
                inside.decrementAndGet();
            }
        });
    }
}
