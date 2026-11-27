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

import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.VectorUInt32Swig;
import fiftyone.pipeline.engines.Constants;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static fiftyone.devicedetection.shared.testhelpers.TestFileUtils.LITE_HASH_DATA_FILE_PATH;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Engine MUST be able to give the ids of the profiles that hold a given
 * value of a given Property, such as a TAC or a native model name, through
 * the native code's EngineHash::getProfileIdsForPropertyValue()
 * (device-detection-specification/pipeline-elements/device-detection-on-premise.md
 * line 479 at 706de1f0). The bullets after it say the name and the value are
 * matched exactly, the ids are ascending and each appears once, and the
 * Indexed Properties option gives the same ids without reading every
 * profile.
 */
public class ProfilesForPropertyValueTests {

    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    /**
     * The engine gives the ids the native function gives, in its order, and
     * passes the name and the value to it as they were given.
     */
    @Test
    public void ProfilesForValue_FromTheNativeFunction() throws Exception {
        EngineOverMockNativeCode mocks = new EngineOverMockNativeCode();
        VectorUInt32Swig ids =
            new VectorUInt32Swig(new long[] {12280L, 17470L, 4294967295L});
        when(mocks.nativeEngine.getProfileIdsForPropertyValue(
            "NativeModel", "iPhone14,2")).thenReturn(ids);

        List<Long> result = mocks.engine.getProfileIdsForPropertyValue(
            "NativeModel", "iPhone14,2");

        assertEquals(Arrays.asList(12280L, 17470L, 4294967295L), result);
        verify(mocks.nativeEngine).getProfileIdsForPropertyValue(
            "NativeModel", "iPhone14,2");
        // The native vector is released once its ids are copied.
        assertEquals(0L, cPtr(ids));
    }

    /**
     * The address of the native vector a proxy holds, which is 0 once the
     * proxy is closed.
     */
    private static long cPtr(VectorUInt32Swig vector) throws Exception {
        Field field = VectorUInt32Swig.class.getDeclaredField("swigCPtr");
        field.setAccessible(true);
        return field.getLong(vector);
    }

    private static File liteFile() {
        try {
            return getFilePath(LITE_HASH_DATA_FILE_PATH);
        }
        catch (IllegalArgumentException e) {
            assumeTrue(e.getMessage(), false);
            return null;
        }
    }

    private static DeviceDetectionHashEngine build(
        File file, List<String> indexed) throws Exception {
        DeviceDetectionHashEngineBuilder builder =
            new DeviceDetectionHashEngineBuilder(loggerFactory, null)
                .setPerformanceProfile(
                    Constants.PerformanceProfiles.HighPerformance)
                .setAutoUpdate(false)
                .setDataFileSystemWatcher(false);
        if (indexed != null) {
            builder.setIndexedProperties(indexed);
        }
        return builder.build(file.toString(), false);
    }

    /**
     * Against the free data file: ids are ascending and distinct, a value in
     * another case gives none, and indexing the Property gives the same ids.
     */
    @Test
    public void ProfilesForValue_LiteDataFile() throws Exception {
        File file = liteFile();
        List<Long> unindexed;
        List<Long> otherCase;
        try (DeviceDetectionHashEngine engine = build(file, null)) {
            unindexed = engine.getProfileIdsForPropertyValue(
                "PlatformName", "iOS");
            otherCase = engine.getProfileIdsForPropertyValue(
                "PlatformName", "IOS");
        }
        assertFalse("No profiles hold PlatformName iOS", unindexed.isEmpty());
        List<Long> sorted = new ArrayList<>(unindexed);
        Collections.sort(sorted);
        assertEquals("Ids are not ascending", sorted, unindexed);
        assertEquals("An id appears more than once",
            unindexed.size(), new HashSet<>(unindexed).size());
        assertTrue("A value in another case matched", otherCase.isEmpty());

        try (DeviceDetectionHashEngine engine =
                 build(file, Collections.singletonList("PlatformName"))) {
            assertEquals(unindexed, engine.getProfileIdsForPropertyValue(
                "PlatformName", "iOS"));
        }
    }
}
