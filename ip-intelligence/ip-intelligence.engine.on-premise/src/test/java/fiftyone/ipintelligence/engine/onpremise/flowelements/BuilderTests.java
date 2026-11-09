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

import fiftyone.ipintelligence.engine.onpremise.interop.swig.CollectionConfigSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.ConfigIpiSwig;
import fiftyone.pipeline.engines.Constants.PerformanceProfiles;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

public class BuilderTests {
    protected static final ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

    private IPIntelligenceOnPremiseEngineBuilder builder;

    @Before
    public void init() {
        builder = new IPIntelligenceOnPremiseEngineBuilder(loggerFactory);
    }

    /**
     * Check that an exception is thrown when attempting to add a cache to the
     * on-premise engine.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void Builder_CacheDisabled() {
        builder.setCache(new CacheConfiguration(1000));
    }

    /**
     * Check that a builder whose caller sets no performance profile gives
     * the engine the collection settings of the Balanced profile, and not
     * the in-memory settings of MaxPerformance.
     */
    @Test
    public void Builder_NoProfile_CollectionsAreBalanced() throws Exception {
        long[] given = collections(configOf(builder));

        assertArrayEquals(
            collections(PerformanceProfiles.Balanced), given);
        assertFalse(Arrays.equals(
            collections(PerformanceProfiles.MaxPerformance), given));
    }

    /**
     * Check that a profile the caller names, in code or by name as a
     * configuration file gives it, replaces the Balanced default.
     * MaxPerformance and LowMemory give every collection the same settings,
     * so this cannot tell those two apart.
     */
    @Test
    public void Builder_MaxPerformance_ReplacesBalanced() throws Exception {
        long[] inMemory = collections(PerformanceProfiles.MaxPerformance);

        builder.setPerformanceProfile(PerformanceProfiles.MaxPerformance);
        assertArrayEquals(inMemory, collections(configOf(builder)));

        IPIntelligenceOnPremiseEngineBuilder named =
            new IPIntelligenceOnPremiseEngineBuilder(loggerFactory)
                .setPerformanceProfile("MaxPerformance");
        assertArrayEquals(inMemory, collections(configOf(named)));
        assertFalse(Arrays.equals(
            collections(PerformanceProfiles.Balanced), inMemory));
    }

    /**
     * The native configuration the builder hands to the engine it builds.
     */
    private static ConfigIpiSwig configOf(
            IPIntelligenceOnPremiseEngineBuilder builder) throws Exception {
        Field field = IPIntelligenceOnPremiseEngineBuilder.class
            .getDeclaredField("config");
        field.setAccessible(true);
        return (ConfigIpiSwig) field.get(builder);
    }

    /**
     * The collection settings the native library gives the profile.
     */
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
     * is left out, because the builder sets its own.
     */
    private static long[] collections(ConfigIpiSwig config) {
        CollectionConfigSwig[] all = {
            borrowed(config.getStrings()),
            borrowed(config.getComponents()),
            borrowed(config.getMaps()),
            borrowed(config.getProperties()),
            borrowed(config.getValues()),
            borrowed(config.getProfiles()),
            borrowed(config.getGraphs()),
            borrowed(config.getProfileGroups()),
            borrowed(config.getProfileOffsets()),
            borrowed(config.getPropertyTypes()),
            borrowed(config.getGraph())
        };
        long[] settings = new long[all.length * 2];
        for (int i = 0; i < all.length; i++) {
            settings[i * 2] = all[i].getLoaded();
            settings[i * 2 + 1] = all[i].getCapacity();
        }
        return settings;
    }

    /**
     * The generated wrappers mark four ConfigIpiSwig getters as owned until
     * they are regenerated with the C library's corrected MetaData.i.
     */
    private static CollectionConfigSwig borrowed(CollectionConfigSwig proxy) {
        try {
            Field own = CollectionConfigSwig.class
                .getDeclaredField("swigCMemOwn");
            own.setAccessible(true);
            own.setBoolean(proxy, false);
            return proxy;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
