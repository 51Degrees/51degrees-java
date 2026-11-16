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

import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneAspectPropertyMetaData;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;

import static fiftyone.devicedetection.shared.testhelpers.TestFileUtils.LITE_HASH_DATA_FILE_PATH;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Every Property of the Hash engine has the metadata an Aspect Engine must
 * add to that of a Flow Element: a description, and the data tiers where the
 * Property is present.
 * <p>
 * Specification: pipeline-specification/features/properties.md line 101 at
 * 51Degrees/specifications 706de1f0, "Aspect Engines are REQUIRED to
 * populate some additional metadata values beyond those described for Flow
 * Elements.", with lines 108 and 109 naming Description and Data tiers where
 * present.
 */
public class PropertyMetaDataTests {

    @Test
    public void everyProperty_HasDescriptionAndDataTiers() throws Exception {
        File dataFile = null;
        try {
            dataFile = getFilePath(LITE_HASH_DATA_FILE_PATH);
        } catch (IllegalArgumentException e) {
            assumeTrue(e.getMessage(), false);
        }

        try (DeviceDetectionHashEngine engine =
                 new DeviceDetectionHashEngineBuilder(
                     LoggerFactory.getILoggerFactory(), null)
                     .setPerformanceProfile(
                         Constants.PerformanceProfiles.LowMemory)
                     .setAutoUpdate(false)
                     .build(dataFile.toString(), false)) {
            String tier = engine.getDataSourceTier();
            assertFalse(engine.getProperties().isEmpty());
            for (FiftyOneAspectPropertyMetaData property :
                engine.getProperties()) {
                String name = property.getName();
                assertNotNull(name + " has a description",
                    property.getDescription());
                assertFalse(name + " has a description",
                    property.getDescription().trim().isEmpty());
                assertNotNull(name + " has data tiers",
                    property.getDataTiersWherePresent());
                assertFalse(name + " has data tiers",
                    property.getDataTiersWherePresent().isEmpty());
                if (property.isAvailable()) {
                    assertTrue(
                        name + " is in the data file, so its tiers name " +
                            "the data file's tier '" + tier + "'",
                        containsIgnoringCase(
                            property.getDataTiersWherePresent(), tier));
                }
            }
        }
    }

    private static boolean containsIgnoringCase(
        Iterable<String> values,
        String value) {
        for (String candidate : values) {
            if (candidate.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
