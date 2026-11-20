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

import fiftyone.devicedetection.hash.engine.onpremise.TestsBase;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Refreshing the engine's data, with the free Lite data file. Refreshes
 * from several threads at once leave a working engine, a refresh from data
 * that cannot be used fails and leaves the data the engine had, and
 * processing while the data is refreshed gives the same values.
 */
public class DataRefreshTests extends TestsBase {

    private static final int THREADS = 4;

    private static final int REFRESHES = 5;

    private static final long WAIT_SECONDS = 300;

    private static final List<String> USER_AGENTS = Arrays.asList(
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 " +
            "Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

    private ExecutorService executor;

    @Before
    public void init() throws Exception {
        testInitialize(Constants.PerformanceProfiles.MaxPerformance);
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
        List<Boolean> expected = isMobile();
        DeviceDetectionHashEngine engine = getWrapper().getEngine();
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
        assertEquals(expected, isMobile());
    }

    /**
     * Several threads refresh the engine at once from the same data held in
     * memory, and every refresh completes and the engine then gives the
     * values it gave before. A refresh from memory leaves the native engine
     * without a file path, so this test doesn't mix it with refreshes from
     * the file.
     */
    @Test
    public void Refresh_FromMemoryOnSeveralThreads_EngineStillWorks()
        throws Exception {
        List<Boolean> expected = isMobile();
        DeviceDetectionHashEngine engine = getWrapper().getEngine();
        String identifier = engine.getDataFileMetaData().getIdentifier();
        byte[] data = Files.readAllBytes(Paths.get(
            engine.getDataFileMetaData().getDataFilePath()));
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> refreshers = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            refreshers.add(executor.submit(() -> {
                start.await();
                for (int i = 0; i < REFRESHES; i++) {
                    engine.refreshData(identifier, data);
                }
                return REFRESHES;
            }));
        }
        start.countDown();
        for (Future<Integer> refresher : refreshers) {
            assertEquals(Integer.valueOf(REFRESHES),
                refresher.get(WAIT_SECONDS, TimeUnit.SECONDS));
        }
        assertEquals(expected, isMobile());
    }

    /**
     * A refresh from data that is not a data file fails with an error, and
     * the engine goes on using the data it had.
     */
    @Test
    public void Refresh_DataNotUsable_FailsAndKeepsTheData() throws Exception {
        List<Boolean> expected = isMobile();
        DeviceDetectionHashEngine engine = getWrapper().getEngine();
        String published = String.valueOf(
            engine.getDataFileMetaData().getDataPublishedDateTime());
        byte[] notData = "This is not a data file.".getBytes(
            StandardCharsets.UTF_8);

        boolean failed = false;
        try {
            engine.refreshData(
                engine.getDataFileMetaData().getIdentifier(),
                notData);
        }
        catch (RuntimeException e) {
            failed = true;
        }

        assertTrue("A refresh from data that cannot be used did not fail.",
            failed);
        assertEquals(expected, isMobile());
        assertEquals(published, String.valueOf(
            engine.getDataFileMetaData().getDataPublishedDateTime()));
    }

    /**
     * Threads process while another refreshes the engine from its data
     * file, and every process gives the values the engine gave before.
     */
    @Test
    public void Process_WhileRefreshing_GivesTheSameValues()
        throws Exception {
        List<Boolean> expected = isMobile();
        DeviceDetectionHashEngine engine = getWrapper().getEngine();
        String identifier = engine.getDataFileMetaData().getIdentifier();
        AtomicBoolean refreshing = new AtomicBoolean(true);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> processors = new ArrayList<>();
        for (int t = 0; t < THREADS - 1; t++) {
            processors.add(executor.submit(process(expected, refreshing, start)));
        }
        Future<Integer> refresher = executor.submit(() -> {
            start.await();
            try {
                for (int i = 0; i < REFRESHES; i++) {
                    engine.refreshData(identifier);
                }
            }
            finally {
                refreshing.set(false);
            }
            return REFRESHES;
        });
        start.countDown();
        assertEquals(Integer.valueOf(REFRESHES),
            refresher.get(WAIT_SECONDS, TimeUnit.SECONDS));
        for (Future<Integer> processor : processors) {
            assertTrue("A thread processed nothing during the refreshes.",
                processor.get(WAIT_SECONDS, TimeUnit.SECONDS) > 0);
        }
    }

    private Callable<Integer> process(
        List<Boolean> expected,
        AtomicBoolean refreshing,
        CountDownLatch start) {
        return () -> {
            start.await();
            int processed = 0;
            do {
                List<Boolean> values = isMobile();
                if (expected.equals(values) == false) {
                    fail("Processing during a refresh gave " + values +
                        " where " + expected + " was given before.");
                }
                processed++;
            } while (refreshing.get());
            return processed;
        };
    }

    /**
     * Whether each of the User-Agents is a mobile device.
     */
    private List<Boolean> isMobile() throws Exception {
        List<Boolean> values = new ArrayList<>();
        for (String userAgent : USER_AGENTS) {
            try (FlowData data =
                     getWrapper().getPipeline().createFlowData()) {
                data.addEvidence("header.user-agent", userAgent);
                data.process();
                values.add(
                    data.get(DeviceData.class).getIsMobile().getValue());
            }
        }
        return values;
    }
}
