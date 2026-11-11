package fiftyone.devicedetection.hash.engine.onpremise.flowelements;

import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fiftyone.devicedetection.shared.testhelpers.FileUtils.LITE_HASH_DATA_FILE_PATH;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;

/**
 * Times the typed read and the read by name through the data object, for an
 * engine built with no names and one built with names.
 */
public class TypedReadTiming {

    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    private static final List<String> NAMES = Arrays.asList(
        "ScreenPixelsHeight", "IsMobile", "PlatformName", "BrowserName");

    private static final String MOBILE =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Version/15.0 Mobile/15E148 Safari/604.1";

    private static final int READS = 50000;

    private static final int ROUNDS = 7;

    @Test
    public void typedReads() throws Exception {
        time(null);
        time(NAMES);
    }

    private static void time(List<String> names) throws Exception {
        File file = getFilePath(LITE_HASH_DATA_FILE_PATH);
        DeviceDetectionHashEngineBuilder builder =
            new DeviceDetectionHashEngineBuilder(loggerFactory, null)
                .setPerformanceProfile(
                    Constants.PerformanceProfiles.HighPerformance)
                .setAutoUpdate(false)
                .setDataFileSystemWatcher(false);
        if (names != null) {
            builder.setProperties(names);
        }
        DeviceDetectionHashEngine engine =
            builder.build(file.toString(), false);
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                 .addFlowElement(engine)
                 .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", MOBILE);
            data.process();
            DeviceData device = data.get(DeviceData.class);
            boolean expected = device.getIsMobile().getValue();
            List<Long> typed = new ArrayList<>();
            List<Long> byName = new ArrayList<>();
            int different = 0;
            for (int round = 0; round <= ROUNDS; round++) {
                long start = System.nanoTime();
                for (int i = 0; i < READS; i++) {
                    if (device.getIsMobile().getValue() != expected) {
                        different++;
                    }
                }
                long typedNanos = System.nanoTime() - start;
                start = System.nanoTime();
                for (int i = 0; i < READS; i++) {
                    AspectPropertyValue<?> value =
                        (AspectPropertyValue<?>) device.get("IsMobile");
                    if ((Boolean) value.getValue() != expected) {
                        different++;
                    }
                }
                long nameNanos = System.nanoTime() - start;
                if (round > 0) {
                    typed.add(typedNanos / READS);
                    byName.add(nameNanos / READS);
                }
            }
            assertEquals(0, different);
            System.out.println(String.format(
                "TYPED COST commit=%s names=%s typedNs=%s getNs=%s",
                System.getProperty("user.dir").contains("before") ? "before" : "after",
                names == null ? "none" : "given",
                spread(typed), spread(byName)));
        }
    }

    private static String spread(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(0) + "/" + sorted.get(sorted.size() / 2) + "/" +
            sorted.get(sorted.size() - 1);
    }
}
