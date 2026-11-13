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

import fiftyone.devicedetection.hash.engine.onpremise.data.DeviceDataHash;
import fiftyone.devicedetection.hash.engine.onpremise.data.PropertyMetaDataHash;
import fiftyone.devicedetection.hash.engine.onpremise.interop.Swig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.BoolValueSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.ConfigHashSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.EngineHashSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.RequiredPropertiesConfigSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.ResultsHashSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.VectorStringSwig;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneAspectPropertyMetaData;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fiftyone.devicedetection.shared.testhelpers.FileUtils.LITE_HASH_DATA_FILE_PATH;
import static fiftyone.devicedetection.shared.testhelpers.FileUtils.TAC_HASH_DATA_FILE_NAME;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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

    private static final String MOBILE =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Version/15.0 Mobile/15E148 Safari/604.1";

    private static final int READS = 20000;

    private static final int ROUNDS = 5;

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
     * An engine built with no names has an index for every property of the
     * first data file it loads, no two properties share an index, and the
     * native results name that property at that index.
     */
    @Test
    public void PropertyIndex_NoNamesFromFirstFile() throws Exception {
        File file = dataFile(LITE_HASH_DATA_FILE_PATH);
        List<String> names = dataSetNames(file);
        try (DeviceDetectionHashEngine engine = build(file, null);
             NativeEngine nativeEngine = new NativeEngine(file, null);
             ResultsHashSwig results = nativeEngine.engine.process(MOBILE)) {
            Set<Integer> seen = new HashSet<>();
            for (String name : names) {
                Integer index = engine.getPropertyIndex(name);
                assertNotNull("Expected an index for '" + name + "'.", index);
                assertTrue(
                    "Index " + index + " given to more than one name.",
                    seen.add(index));
                assertEquals(
                    "Expected the native results to name '" + name +
                        "' at index " + index + ".",
                    name,
                    results.getPropertyName(index));
            }
        }
    }

    /**
     * An engine built with no names keeps its first data file's indexes
     * through a reload from a data file whose properties are a superset in a
     * different order. Each property both files have then reads what an
     * engine built on the new file reads, and a property only the new file
     * has gets no index.
     */
    @Test
    public void PropertyIndex_NoNamesSurvivesReload() throws Exception {
        File lite = dataFile(LITE_HASH_DATA_FILE_PATH);
        File bigger = dataFile(TAC_HASH_DATA_FILE_NAME);
        List<String> names = dataSetNames(lite);
        names.retainAll(dataSetNames(bigger));
        String added = nameOnlyIn(bigger, lite);

        DeviceDetectionHashEngine engine = build(lite, null);
        DeviceDetectionHashEngine fresh = build(bigger, null);
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                 .addFlowElement(engine)
                 .build();
             Pipeline freshPipeline = new PipelineBuilder(loggerFactory)
                 .addFlowElement(fresh)
                 .build()) {

            Map<String, Integer> before = new HashMap<>();
            for (String name : names) {
                Integer index = engine.getPropertyIndex(name);
                assertNotNull("Expected an index for '" + name + "'.", index);
                before.put(name, index);
            }

            engine.refreshData(
                engine.getDataFiles().get(0).getIdentifier(),
                Files.readAllBytes(bigger.toPath()));

            for (String name : names) {
                assertEquals(
                    "Index of '" + name + "' moved on reload.",
                    before.get(name),
                    engine.getPropertyIndex(name));
            }
            if (added != null) {
                assertNull(
                    "Expected no index for '" + added + "', which only " +
                        "the new file has.",
                    engine.getPropertyIndex(added));
            }

            try (FlowData data = pipeline.createFlowData();
                 FlowData expected = freshPipeline.createFlowData()) {
                data.addEvidence("header.user-agent", MOBILE);
                data.process();
                expected.addEvidence("header.user-agent", MOBILE);
                expected.process();
                DeviceData device = data.get(DeviceData.class);
                DeviceData want = expected.get(DeviceData.class);
                for (String name : names) {
                    AspectPropertyValue<?> got =
                        (AspectPropertyValue<?>) device.get(name);
                    AspectPropertyValue<?> wanted =
                        (AspectPropertyValue<?>) want.get(name);
                    assertEquals(
                        "Expected '" + name + "' to have a value as a " +
                            "fresh engine gives it.",
                        wanted.hasValue(),
                        got.hasValue());
                    if (wanted.hasValue()) {
                        assertEquals(
                            "Expected '" + name + "' to read as a fresh " +
                                "engine reads it.",
                            wanted.getValue(),
                            got.getValue());
                    }
                }
            }
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

    /**
     * A name the engine was built with keeps its index after a reload from a
     * data file that lacks the property. A read by index then gives no value,
     * as a read by name does, rather than an error, and the other names still
     * read their own properties.
     */
    @Test
    public void PropertyIndex_NameTheReloadedFileLacks() throws Exception {
        File lite = dataFile(LITE_HASH_DATA_FILE_PATH);
        File bigger = dataFile(TAC_HASH_DATA_FILE_NAME);
        String missing = nameOnlyIn(bigger, lite);
        assumeTrue("The TAC file has no property the Lite file lacks.",
            missing != null);
        List<String> names = Arrays.asList(missing, "IsMobile");

        DeviceDetectionHashEngine engine = build(bigger, names);
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                 .addFlowElement(engine)
                 .build()) {
            engine.refreshData(
                engine.getDataFiles().get(0).getIdentifier(),
                Files.readAllBytes(lite.toPath()));

            assertEquals(
                "Index of '" + missing + "' moved on reload.",
                Integer.valueOf(0),
                engine.getPropertyIndex(missing));
            assertEquals(
                Integer.valueOf(1),
                engine.getPropertyIndex("IsMobile"));

            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("header.user-agent", MOBILE);
                data.process();
                DeviceDataHash device =
                    (DeviceDataHash) data.get(DeviceData.class);
                AspectPropertyValue<List<String>> values =
                    device.getValues(missing);
                assertFalse(
                    "'" + missing + "' has a value from a file without it.",
                    values.hasValue());
                assertNotNull(values.getNoValueMessage());
                assertTrue("IsMobile has no value after the reload.",
                    device.getIsMobile().hasValue());
                assertEquals(Boolean.TRUE, device.getIsMobile().getValue());
            }
        }
    }

    /**
     * The first property name, in name order, that one data file has and the
     * other lacks, or null if there is none.
     */
    private static String nameOnlyIn(File has, File lacks) throws Exception {
        List<String> lacking = propertyNames(lacks);
        for (String name : propertyNames(has)) {
            if (lacking.contains(name) == false) {
                return name;
            }
        }
        return null;
    }

    /**
     * The names of the properties the data file holds, sorted, leaving out
     * the match metrics the engine adds, which are not in the data file and
     * so have no index.
     */
    private static List<String> dataSetNames(File file) throws Exception {
        List<String> names = new ArrayList<>();
        try (DeviceDetectionHashEngine engine = build(file, null)) {
            for (FiftyOneAspectPropertyMetaData property :
                engine.getProperties()) {
                if (property instanceof PropertyMetaDataHash) {
                    names.add(property.getName());
                }
            }
        }
        Collections.sort(names);
        return names;
    }

    private static List<String> propertyNames(File file) throws Exception {
        List<String> names = new ArrayList<>();
        try (DeviceDetectionHashEngine engine = build(file, null)) {
            for (FiftyOneAspectPropertyMetaData property :
                engine.getProperties()) {
                names.add(property.getName());
            }
        }
        Collections.sort(names);
        return names;
    }

    /**
     * Reads IsMobile {@link #READS} times by name and {@link #READS} times by
     * index from the same native results, and {@link #READS} times through
     * the typed accessor, which is the whole path a caller takes, and prints
     * the median nanoseconds of one read over {@link #ROUNDS} rounds after a
     * warm-up, for an engine built with {@link #NAMES}. Every read must give
     * the same value. The times are printed rather than asserted, because
     * runners vary.
     */
    @Test
    public void PropertyIndex_ReadByIndexAgainstByName() throws Exception {
        measureReads(NAMES);
    }

    /**
     * The same measurement for an engine built with no names, which reads by
     * the index of the property in its first data file.
     */
    @Test
    public void PropertyIndex_NoNamesReadByIndexAgainstByName()
        throws Exception {
        measureReads(null);
    }

    private static void measureReads(List<String> built) throws Exception {
        File file = dataFile(LITE_HASH_DATA_FILE_PATH);
        DeviceDetectionHashEngine engine = build(file, built);
        Integer found = engine.getPropertyIndex("IsMobile");
        assertNotNull("Expected an index for 'IsMobile'.", found);
        int index = found;
        ConfigHashSwig config = new ConfigHashSwig();
        config.setHighPerformance();
        RequiredPropertiesConfigSwig required;
        try (VectorStringSwig names = new VectorStringSwig()) {
            if (built != null) {
                names.addAll(built);
            }
            required = new RequiredPropertiesConfigSwig(names);
        }
        EngineHashSwig nativeEngine =
            new EngineHashSwig(file.getAbsolutePath(), config, required);
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                 .addFlowElement(engine)
                 .build();
             FlowData data = pipeline.createFlowData();
             ResultsHashSwig results = nativeEngine.process(MOBILE)) {
            data.addEvidence("header.user-agent", MOBILE);
            data.process();
            DeviceData device = data.get(DeviceData.class);

            boolean expected = readByName(results);
            assertEquals(expected, readByIndex(results, index));
            assertEquals(expected, (boolean) device.getIsMobile().getValue());

            List<Long> byName = new ArrayList<>();
            List<Long> byIndex = new ArrayList<>();
            List<Long> typed = new ArrayList<>();
            int different = 0;
            for (int round = 0; round <= ROUNDS; round++) {
                long start = System.nanoTime();
                for (int i = 0; i < READS; i++) {
                    if (readByName(results) != expected) {
                        different++;
                    }
                }
                long nameNanos = System.nanoTime() - start;
                start = System.nanoTime();
                for (int i = 0; i < READS; i++) {
                    if (readByIndex(results, index) != expected) {
                        different++;
                    }
                }
                long indexNanos = System.nanoTime() - start;
                start = System.nanoTime();
                for (int i = 0; i < READS; i++) {
                    if (device.getIsMobile().getValue() != expected) {
                        different++;
                    }
                }
                long typedNanos = System.nanoTime() - start;
                if (round > 0) {
                    byName.add(nameNanos / READS);
                    byIndex.add(indexNanos / READS);
                    typed.add(typedNanos / READS);
                }
            }
            assertEquals("Every read gives the same value", 0, different);
            System.out.println(String.format(
                "READ COST IsMobile names=%s reads=%d rounds=%d " +
                    "byNameNs=%s byIndexNs=%s typedNs=%s java=%s os=%s",
                built == null ? "none" : "given",
                READS, ROUNDS, spread(byName), spread(byIndex),
                spread(typed), System.getProperty("java.version"),
                System.getProperty("os.name")));
        }
        finally {
            nativeEngine.delete();
            required.delete();
            config.delete();
        }
    }

    /**
     * A native engine on its own, so a test can read the native results
     * directly. Built with no names when the list is null.
     */
    private static class NativeEngine implements AutoCloseable {
        private final ConfigHashSwig config = new ConfigHashSwig();
        private final RequiredPropertiesConfigSwig required;
        private final EngineHashSwig engine;

        NativeEngine(File file, List<String> names) {
            config.setHighPerformance();
            RequiredPropertiesConfigSwig made;
            try (VectorStringSwig vector = new VectorStringSwig()) {
                if (names != null) {
                    vector.addAll(names);
                }
                made = new RequiredPropertiesConfigSwig(vector);
            }
            required = made;
            engine = new EngineHashSwig(
                file.getAbsolutePath(), config, required);
        }

        @Override
        public void close() {
            engine.delete();
            required.delete();
            config.delete();
        }
    }

    /**
     * Reads IsMobile by name through the overload that takes the name's
     * bytes, which is how a read goes when the engine has no index for it.
     */
    private static boolean readByName(ResultsHashSwig results) {
        try (BoolValueSwig value =
                 results.getValueAsBool(Swig.asBytes("IsMobile"))) {
            return value.getValue();
        }
    }

    private static boolean readByIndex(ResultsHashSwig results, int index) {
        try (BoolValueSwig value = results.getValueAsBool(index)) {
            return value.getValue();
        }
    }

    /**
     * The lowest, median and highest, in that order.
     */
    private static String spread(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(0) + "/" + sorted.get(sorted.size() / 2) + "/" +
            sorted.get(sorted.size() - 1);
    }
}
