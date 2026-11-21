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

import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.RequiredPropertiesConfigSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.VectorStringSwig;
import fiftyone.pipeline.engines.Constants;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fiftyone.devicedetection.shared.testhelpers.TestFileUtils.LITE_HASH_DATA_FILE_PATH;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * An implementation configured with no names MUST create the Engine with no
 * names, and MUST NOT pass the data set's names itself
 * (device-detection-specification/pipeline-elements/device-detection-on-premise.md
 * line 189 at 706de1f0). Reads the names the native configuration the
 * engine was created with holds, which is what decides whether the engine
 * walks graphs during process, rather than the indexes the engine has
 * afterwards, which are the same either way.
 */
public class NoNamesTests {

    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    private static File liteFile() {
        try {
            return getFilePath(LITE_HASH_DATA_FILE_PATH);
        }
        catch (IllegalArgumentException e) {
            assumeTrue(e.getMessage(), false);
            return null;
        }
    }

    private static List<String> namesCreatedWith(
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
        try (DeviceDetectionHashEngine engine =
                 builder.build(file.toString(), false)) {
            Field field = DeviceDetectionHashEngine.class
                .getDeclaredField("propertiesConfigSwig");
            field.setAccessible(true);
            RequiredPropertiesConfigSwig config =
                (RequiredPropertiesConfigSwig) field.get(engine);
            try (VectorStringSwig given = config.getProperties()) {
                return new ArrayList<>(given);
            }
        }
    }

    @Test
    public void NoNames_EngineCreatedWithNoNames() throws Exception {
        File file = liteFile();
        List<String> names = namesCreatedWith(file, null);
        assertTrue("The engine was created with names " + names +
            " when none were configured", names.isEmpty());
    }

    @Test
    public void Names_EngineCreatedWithThoseNames() throws Exception {
        File file = liteFile();
        List<String> configured = Arrays.asList("IsMobile", "BrowserName");
        assertEquals(configured, namesCreatedWith(file, configured));
    }
}
