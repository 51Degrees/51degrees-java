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
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Refreshing the engine's data from several threads at once, with the free
 * Lite data file. The engine lets only one refresh run at a time, so every
 * refresh completes and the engine still works afterwards.
 */
public class DataRefreshTests extends TestsBase {

    private static final int THREADS = 4;

    private static final int REFRESHES = 5;

    private static final long WAIT_SECONDS = 300;

    private static final String ADDRESS = "116.154.188.222";

    private ExecutorService executor;

    @Before
    public void init() throws Exception {
        testInitialize(Constants.PerformanceProfiles.LowMemory);
        executor = Executors.newFixedThreadPool(THREADS);
    }

    @After
    public void cleanup() {
        executor.shutdownNow();
        testCleanup();
    }

    /**
     * Several threads refresh the engine from its data file at once, and
     * the engine then gives the values it gave before.
     */
    @Test
    public void Refresh_FromSeveralThreads_EngineStillWorks()
        throws Exception {
        String expected = registeredCountry();
        IPIntelligenceOnPremiseEngine engine = getWrapper().getEngine();
        String identifier = engine.getDataFileMetaData().getIdentifier();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> refreshers = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            refreshers.add(executor.submit(() -> {
                start.await();
                for (int i = 0; i < REFRESHES; i++) {
                    engine.refreshData(identifier);
                }
                return REFRESHES;
            }));
        }
        start.countDown();
        for (Future<Integer> refresher : refreshers) {
            assertEquals(Integer.valueOf(REFRESHES),
                refresher.get(WAIT_SECONDS, TimeUnit.SECONDS));
        }
        assertEquals(expected, registeredCountry());
    }

    /**
     * The registered country of one address.
     */
    private String registeredCountry() throws Exception {
        try (FlowData data = getWrapper().getPipeline().createFlowData()) {
            data.addEvidence("query.client-ip", ADDRESS);
            data.process();
            return data.get(IPIntelligenceData.class)
                .getRegisteredCountry().getValue();
        }
    }
}
