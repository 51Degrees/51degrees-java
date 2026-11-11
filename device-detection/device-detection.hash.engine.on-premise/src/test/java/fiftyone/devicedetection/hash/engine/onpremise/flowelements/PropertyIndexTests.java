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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static fiftyone.devicedetection.shared.testhelpers.FileUtils.LITE_HASH_DATA_FILE_PATH;
import static fiftyone.devicedetection.shared.testhelpers.FileUtils.TAC_HASH_DATA_FILE_NAME;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

/**
 * Tests for the property index rule, which says that a property's index is
 * its place in the list of names the engine was built with, that the index is
 * fixed at build, and that every read by name goes through that index.
 */
public class PropertyIndexTests {

    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    /**
     * Names in an order that is deliberately not the order the data file
     * holds them in, so an index taken from the data file would be wrong.
     */
    private static final List<String> NAMES = Arrays.asList(
        "ScreenPixelsHeight",
        "IsMobile",
        "PlatformName",
        "BrowserName");

    private static File dataFile(String name) {
        try {
            return getFilePath(name);
        }
        catch (IllegalArgumentException e) {
            assumeTrue(e.getMessage(), false);
            return null;
        }
    }

    private static DeviceDetectionHashEngine build(
        File file, List<String> names) throws Exception {
        DeviceDetectionHashEngineBuilder builder =
            new DeviceDetectionHashEngineBuilder(loggerFactory, null)
                .setPerformanceProfile(
                    Constants.PerformanceProfiles.HighPerformance)
                .setAutoUpdate(false)
                .setDataFileSystemWatcher(false);
        if (names != null) {
            builder.setProperties(names);
        }
        return builder.build(file.toString(), false);
    }

    /**
     * Every name the engine was built with has an index, and the index is the
     * place of the name in the build time list, not its place in the data
     * file.
     */
    @Test
    public void PropertyIndex_FromBuildTimeNames() throws Exception {
        File file = dataFile(LITE_HASH_DATA_FILE_PATH);
        try (DeviceDetectionHashEngine engine = build(file, NAMES)) {
            for (int i = 0; i < NAMES.size(); i++) {
                Integer index = engine.getPropertyIndex(NAMES.get(i));
                assertNotNull(
                    "Expected an index for '" + NAMES.get(i) + "'.",
                    index);
                assertEquals(
                    "Expected '" + NAMES.get(i) + "' at index " + i + ".",
                    Integer.valueOf(i),
                    index);
            }
        }
    }

    /**
     * The lookup is case insensitive, because a caller may ask for a property
     * in any casing.
     */
    @Test
    public void PropertyIndex_CaseInsensitive() throws Exception {
        File file = dataFile(LITE_HASH_DATA_FILE_PATH);
        try (DeviceDetectionHashEngine engine = build(file, NAMES)) {
            assertEquals(
                engine.getPropertyIndex("IsMobile"),
                engine.getPropertyIndex("ismobile"));
        }
    }

    /**
     * A name given twice, in any letter case, has the index of its first
     * place, as the native engine gives it.
     */
    @Test
    public void PropertyIndex_NameGivenTwiceHasFirstIndex() throws Exception {
        File file = dataFile(LITE_HASH_DATA_FILE_PATH);
        try (DeviceDetectionHashEngine engine = build(file,
            Arrays.asList("IsMobile", "BrowserName", "ismobile"))) {
            assertEquals(
                Integer.valueOf(0),
                engine.getPropertyIndex("IsMobile"));
            assertEquals(
                Integer.valueOf(1),
                engine.getPropertyIndex("BrowserName"));
        }
    }

    /**
     * A name the engine was not built with has no index, so the read goes by
     * name.
     */
    @Test
    public void PropertyIndex_UnknownNameHasNoIndex() throws Exception {
        File file = dataFile(LITE_HASH_DATA_FILE_PATH);
        try (DeviceDetectionHashEngine engine = build(file, NAMES)) {
            assertNull(engine.getPropertyIndex("HardwareVendor"));
            assertNull(engine.getPropertyIndex("NotAPropertyAtAll"));
        }
    }

    /**
     * An engine built with no names has no indexes, so every read goes by
     * name.
     */
    @Test
    public void PropertyIndex_NoNamesReadsByName() throws Exception {
        File file = dataFile(LITE_HASH_DATA_FILE_PATH);
        try (DeviceDetectionHashEngine engine = build(file, null)) {
            assertNull(engine.getPropertyIndex("IsMobile"));
        }
    }

    /**
     * A read by name and the typed read give the same value, because the
     * typed accessor goes through the same path.
     */
    @Test
    public void PropertyIndex_ByNameMatchesTyped() throws Exception {
        File file = dataFile(LITE_HASH_DATA_FILE_PATH);
        DeviceDetectionHashEngine engine = build(file, NAMES);
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                 .addFlowElement(engine)
                 .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence(
                "header.user-agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) " +
                    "AppleWebKit/605.1.15 (KHTML, like Gecko) " +
                    "Version/15.0 Mobile/15E148 Safari/604.1");
            data.process();
            DeviceData device = data.get(DeviceData.class);
            AspectPropertyValue<?> typed = device.getIsMobile();
            AspectPropertyValue<?> byName =
                (AspectPropertyValue<?>) device.get("IsMobile");
            assertEquals(typed.hasValue(), byName.hasValue());
            if (typed.hasValue()) {
                assertEquals(typed.getValue(), byName.getValue());
            }
        }
    }

    /**
     * After a reload from a data file whose properties are a superset and in
     * a different order, each index still names the property it named at
     * build, so every value read by name is still the right one.
     */
    @Test
    public void PropertyIndex_SurvivesReload() throws Exception {
        File lite = dataFile(LITE_HASH_DATA_FILE_PATH);
        File bigger = dataFile(TAC_HASH_DATA_FILE_NAME);

        String userAgent =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) " +
                "AppleWebKit/605.1.15 (KHTML, like Gecko) " +
                "Version/15.0 Mobile/15E148 Safari/604.1";

        DeviceDetectionHashEngine engine = build(lite, NAMES);
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                 .addFlowElement(engine)
                 .build()) {

            // The indexes the engine was built with.
            for (int i = 0; i < NAMES.size(); i++) {
                assertEquals(
                    Integer.valueOf(i),
                    engine.getPropertyIndex(NAMES.get(i)));
            }

            // Reload from the larger data file, whose property order differs.
            engine.refreshData(
                engine.getDataFiles().get(0).getIdentifier(),
                Files.readAllBytes(bigger.toPath()));

            // The indexes have not moved.
            for (int i = 0; i < NAMES.size(); i++) {
                assertEquals(
                    "Index of '" + NAMES.get(i) + "' moved on reload.",
                    Integer.valueOf(i),
                    engine.getPropertyIndex(NAMES.get(i)));
            }

            // Every name still reads its own property, and the typed read
            // agrees with the read by name.
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("header.user-agent", userAgent);
                data.process();
                DeviceData device = data.get(DeviceData.class);
                AspectPropertyValue<?> typed = device.getIsMobile();
                AspectPropertyValue<?> byName =
                    (AspectPropertyValue<?>) device.get("IsMobile");
                assertEquals(typed.hasValue(), byName.hasValue());
                if (typed.hasValue()) {
                    assertEquals(typed.getValue(), byName.getValue());
                }
                for (String name : NAMES) {
                    assertNotNull(
                        "Expected a result for '" + name + "'.",
                        device.get(name));
                }
            }
        }
    }
}
