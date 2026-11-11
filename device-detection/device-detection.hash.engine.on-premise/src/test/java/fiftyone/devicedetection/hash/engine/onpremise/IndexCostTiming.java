package fiftyone.devicedetection.hash.engine.onpremise;

import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngineBuilder;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Temporary timing harness, never committed. Prints COST lines giving the
 * lowest, median and highest of five rounds after one warm-up round.
 */
public class IndexCostTiming {

    private static final int ROUNDS = 5;
    private static final int BUILDS = 5;
    private static final int USER_AGENTS = 5000;

    /**
     * The names the engine is built with, in an order that is not the data
     * file's order.
     */
    private static final List<String> NAMES = Arrays.asList(
        "ScreenPixelsHeight",
        "IsMobile",
        "PlatformName",
        "BrowserName");

    private static final String[] MODES = {
        "process", "typedIsMobile", "byNameIsMobile", "everyName"};

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    private long checksum = 0;

    private DeviceDetectionHashEngine build(
        String file, String profile, List<String> names) throws Exception {
        DeviceDetectionHashEngineBuilder builder =
            new DeviceDetectionHashEngineBuilder(loggerFactory)
                .setPerformanceProfile(profile)
                .setAutoUpdate(false)
                .setDataFileSystemWatcher(false);
        if (names != null) {
            builder.setProperties(names);
        }
        return builder.build(file, false);
    }

    /** Lowest, median and highest. */
    private static String spread(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(0) + "/" + sorted.get(sorted.size() / 2) + "/" +
            sorted.get(sorted.size() - 1);
    }

    private String buildMillis(String file, String profile, List<String> names)
        throws Exception {
        List<Long> times = new ArrayList<>();
        for (int i = 0; i <= BUILDS; i++) {
            long start = System.nanoTime();
            DeviceDetectionHashEngine engine = build(file, profile, names);
            long elapsed = (System.nanoTime() - start) / 100000;
            engine.close();
            if (i > 0) {
                times.add(elapsed);
            }
        }
        // Tenths of a millisecond.
        return spread(times);
    }

    private void touch(AspectPropertyValue<?> value) {
        if (value.hasValue()) {
            Object inner = value.getValue();
            checksum += inner == null ? 1 : inner.hashCode();
        }
    }

    private String processNanos(
        Pipeline pipeline,
        List<String> userAgents,
        List<String> names,
        int[] modes) {
        StringBuilder result = new StringBuilder();
        for (int mode : modes) {
            List<Long> rounds = new ArrayList<>();
            for (int round = 0; round <= ROUNDS; round++) {
                long start = System.nanoTime();
                for (String userAgent : userAgents) {
                    try (FlowData data = pipeline.createFlowData()) {
                        data.addEvidence("header.user-agent", userAgent);
                        data.process();
                        DeviceData device = data.get(DeviceData.class);
                        if (mode == 1) {
                            touch(device.getIsMobile());
                        }
                        else if (mode == 2) {
                            touch((AspectPropertyValue<?>) device.get(
                                "IsMobile"));
                        }
                        else if (mode == 3) {
                            for (String name : names) {
                                touch((AspectPropertyValue<?>) device.get(
                                    name));
                            }
                        }
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                long elapsed = System.nanoTime() - start;
                if (round > 0) {
                    rounds.add(elapsed / userAgents.size());
                }
            }
            result.append(' ').append(MODES[mode]).append("Ns=")
                .append(spread(rounds));
        }
        return result.toString();
    }

    private void measure(
        String file,
        String profile,
        String label,
        List<String> userAgents,
        List<String> names,
        int[] modes) throws Exception {
        String build = buildMillis(file, profile, names);
        DeviceDetectionHashEngine engine = build(file, profile, names);
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                 .addFlowElement(engine)
                 .build()) {
            String nanos = processNanos(pipeline, userAgents, NAMES, modes);
            System.out.println(String.format(
                "COST %s %s profile=%s names=%s buildTenthsMs=%s%s",
                System.getProperty("costTree", ""), label, profile,
                names == null ? "none" : String.valueOf(names.size()),
                build, nanos));
        }
        engine.close();
    }

    @Test
    public void measure() throws Exception {
        String file = System.getProperty("costDataFile");
        String profile = System.getProperty("costProfile", "MaxPerformance");
        String label = System.getProperty("costLabel", "");
        List<String> userAgents = new ArrayList<>();
        for (String line : Files.readAllLines(
            Paths.get(System.getProperty("costUaFile")))) {
            if (line.isEmpty() == false && userAgents.size() < USER_AGENTS) {
                userAgents.add(line);
            }
        }
        measure(file, profile, label, userAgents, NAMES,
            new int[] {0, 1, 2, 3});
        measure(file, profile, label, userAgents, null,
            new int[] {0, 2});
        System.out.println("COST " + System.getProperty("costTree", "") +
            " checksum " + checksum);
    }
}
