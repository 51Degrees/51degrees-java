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

package fiftyone.ipintelligence;

import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngineBuilder;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.CollectionConfigSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.ConfigIpiSwig;
import fiftyone.ipintelligence.shared.testhelpers.FileUtils;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.Constants.PerformanceProfiles;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests that the on-premise pipeline gives its engine the Balanced
 * performance profile unless the caller names another, by reading the
 * native configuration the engine was built with.
 */
public class OnPremisePerformanceProfileTests {

    private static File dataFile;

    @BeforeClass
    public static void findDataFile() {
        dataFile = FileUtils.getHashFile();
    }

    @Before
    public void requireDataFile() {
        Assume.assumeTrue(
            "No IP Intelligence data file was found, so this test is "
            + "skipped. Set " + FileUtils.IPI_PATH_ENV_VAR + " to the path "
            + "of an .ipi file, or download the Lite file with "
            + "cxx/data/get-data-files.",
            dataFile != null);
    }

    @Test
    public void build_NoProfile_EngineIsGivenBalanced() throws Exception {
        try (Pipeline pipeline = builder().build()) {
            ConfigIpiSwig given = configOf(pipeline);

            assertArrayEquals(
                collections(PerformanceProfiles.Balanced),
                collections(given));
            assertFalse(Arrays.equals(
                collections(PerformanceProfiles.MaxPerformance),
                collections(given)));
            // Applying a profile replaces the concurrency, so this checks
            // the pipeline did not apply one over the engine builder's.
            assertEquals(
                configOf(new IPIntelligenceOnPremiseEngineBuilder())
                    .getConcurrency(),
                given.getConcurrency());
        }
    }

    /**
     * MaxPerformance and LowMemory give every collection the same settings,
     * so this checks only that the profile named replaced Balanced.
     */
    @Test
    public void build_LowMemory_ReplacesBalanced() throws Exception {
        try (Pipeline pipeline = builder()
                .setPerformanceProfile(PerformanceProfiles.LowMemory)
                .build()) {
            assertArrayEquals(
                collections(PerformanceProfiles.LowMemory),
                collections(configOf(pipeline)));
        }
    }

    /**
     * A pipeline builder with the profile and the concurrency left at their
     * defaults. Data updates and usage sharing are switched off, so that
     * the test needs no licence key and no network, and neither touches
     * the profile.
     */
    private static IPIntelligenceOnPremisePipelineBuilder builder()
            throws Exception {
        return new IPIntelligencePipelineBuilder(
                LoggerFactory.getILoggerFactory())
            .useOnPremise(dataFile.getAbsolutePath(), false)
            .setAutoUpdate(false)
            .setDataUpdateOnStartup(false)
            .setShareUsage(false);
    }

    /** The native configuration the pipeline's engine was built with. */
    private static ConfigIpiSwig configOf(Pipeline pipeline)
            throws Exception {
        return (ConfigIpiSwig) field(
            IPIntelligenceOnPremiseEngine.class, "config")
            .get(pipeline.getElement(IPIntelligenceOnPremiseEngine.class));
    }

    /** The native configuration an engine builder would hand on. */
    private static ConfigIpiSwig configOf(
            IPIntelligenceOnPremiseEngineBuilder builder) throws Exception {
        return (ConfigIpiSwig) field(
            IPIntelligenceOnPremiseEngineBuilder.class, "config")
            .get(builder);
    }

    private static Field field(Class<?> type, String name)
            throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    /** The collection settings the native library gives the profile. */
    private static long[] collections(PerformanceProfiles profile) {
        ConfigIpiSwig config = new ConfigIpiSwig();
        try {
            switch (profile) {
                case Balanced:
                    config.setBalanced();
                    break;
                case MaxPerformance:
                    config.setMaxPerformance();
                    break;
                case LowMemory:
                    config.setLowMemory();
                    break;
                default:
                    throw new IllegalArgumentException(profile.name());
            }
            return collections(config);
        } finally {
            config.delete();
        }
    }

    /**
     * The number of entries loaded at the start and the cache capacity of
     * every collection in the configuration, in a fixed order. Concurrency
     * is left out, because the engine builder sets its own.
     */
    private static long[] collections(ConfigIpiSwig config) {
        CollectionConfigSwig[] all = {
            config.getStrings(),
            config.getComponents(),
            config.getMaps(),
            config.getProperties(),
            config.getValues(),
            config.getProfiles(),
            config.getGraphs(),
            config.getProfileGroups(),
            config.getProfileOffsets(),
            config.getPropertyTypes(),
            config.getGraph()
        };
        long[] settings = new long[all.length * 2];
        for (int i = 0; i < all.length; i++) {
            settings[i * 2] = all[i].getLoaded();
            settings[i * 2 + 1] = all[i].getCapacity();
        }
        return settings;
    }
}
