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

import fiftyone.pipeline.engines.Constants;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static fiftyone.ipintelligence.shared.testhelpers.FileUtils.LITE_IPI_DATA_FILE_NAME;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

/**
 * Tests for the property index rule on the IP Intelligence on-premise
 * engine, which says that a property's index is its place in the list of
 * names the engine was built with, that the index is fixed at build, and that
 * every read by name goes through that index.
 */
public class PropertyIndexTests {

    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    /**
     * Names in an order that is deliberately not the order the data file
     * holds them in, so an index taken from the data file would be wrong.
     */
    private static final List<String> NAMES = Arrays.asList(
        "RegisteredCountry",
        "AccuracyRadiusMin",
        "RegisteredName",
        "Latitude");

    private static File dataFile(String name) {
        try {
            return getFilePath(name);
        }
        catch (IllegalArgumentException e) {
            assumeTrue(e.getMessage(), false);
            return null;
        }
    }

    private static IPIntelligenceOnPremiseEngine build(
        File file, List<String> names) throws Exception {
        IPIntelligenceOnPremiseEngineBuilder builder =
            new IPIntelligenceOnPremiseEngineBuilder(loggerFactory, null)
                .setPerformanceProfile(
                    Constants.PerformanceProfiles.MaxPerformance)
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
     * file. This is the test that goes red without the change, because
     * reading by name alone gives the right answer today.
     */
    @Test
    public void PropertyIndex_FromBuildTimeNames() throws Exception {
        File file = dataFile(LITE_IPI_DATA_FILE_NAME);
        try (IPIntelligenceOnPremiseEngine engine = build(file, NAMES)) {
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
        File file = dataFile(LITE_IPI_DATA_FILE_NAME);
        try (IPIntelligenceOnPremiseEngine engine = build(file, NAMES)) {
            assertEquals(
                engine.getPropertyIndex("RegisteredName"),
                engine.getPropertyIndex("registeredname"));
        }
    }

    /**
     * A name the engine was not built with has no index, so the read falls
     * back to the name and gives the answer it gives today.
     */
    @Test
    public void PropertyIndex_UnknownNameHasNoIndex() throws Exception {
        File file = dataFile(LITE_IPI_DATA_FILE_NAME);
        try (IPIntelligenceOnPremiseEngine engine = build(file, NAMES)) {
            assertNull(engine.getPropertyIndex("NotAPropertyAtAll"));
        }
    }

    /**
     * An engine built with no names reads by name, which is the path left
     * unchanged until the native layer keeps the first load's names across a
     * reload.
     */
    @Test
    public void PropertyIndex_NoNamesReadsByName() throws Exception {
        File file = dataFile(LITE_IPI_DATA_FILE_NAME);
        try (IPIntelligenceOnPremiseEngine engine = build(file, null)) {
            assertNull(engine.getPropertyIndex("RegisteredName"));
        }
    }
}
