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

package fiftyone.devicedetection.hash.engine.onpremise.interop;

import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.ConfigHashSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.EngineHashSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.EvidenceDeviceDetectionSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.RequiredPropertiesConfigSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.ResultsHashSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.StringValueSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.VectorStringSwig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static fiftyone.devicedetection.shared.testhelpers.TestFileUtils.LITE_HASH_DATA_FILE_PATH;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The byte array forms of adding evidence and reading a value by name read
 * each array up to its first zero or its end, whichever comes first. The
 * Java callers end each array with a zero, so these tests give arrays that
 * have none, that are empty, and that have a zero inside.
 */
public class ByteArrayTests {

    private static final String MOBILE =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Version/15.0 Mobile/15E148 Safari/604.1";

    private static final String USER_AGENT = "header.user-agent";

    private static final String PROPERTY = "IsMobile";

    private static EngineHashSwig engine;

    @BeforeClass
    public static void createEngine() {
        File file;
        try {
            file = getFilePath(LITE_HASH_DATA_FILE_PATH);
        } catch (IllegalArgumentException e) {
            throw new AssertionError("The test needs the free Lite data " +
                "file, which the build fetches into cxx/data.", e);
        }
        ConfigHashSwig config = new ConfigHashSwig();
        config.setHighPerformance();
        RequiredPropertiesConfigSwig required;
        try (VectorStringSwig names = new VectorStringSwig()) {
            required = new RequiredPropertiesConfigSwig(names);
        }
        try {
            engine = new EngineHashSwig(file.getPath(), config, required);
        } finally {
            required.delete();
        }
    }

    @AfterClass
    public static void deleteEngine() {
        engine.delete();
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] zeroEnded(String text) {
        byte[] value = bytes(text);
        byte[] ended = new byte[value.length + 1];
        System.arraycopy(value, 0, ended, 0, value.length);
        return ended;
    }

    @Test
    public void AddFromBytes_NoTrailingZero_ReadsWholeArray() {
        try (EvidenceDeviceDetectionSwig evidence =
                 new EvidenceDeviceDetectionSwig()) {
            evidence.addFromBytes(bytes(USER_AGENT), bytes(MOBILE));
            assertEquals(1, evidence.size());
            assertEquals(MOBILE, evidence.get(USER_AGENT));
        }
    }

    @Test
    public void AddFromBytes_TrailingZero_LeavesTheZeroOut() {
        try (EvidenceDeviceDetectionSwig evidence =
                 new EvidenceDeviceDetectionSwig()) {
            evidence.addFromBytes(zeroEnded(USER_AGENT), zeroEnded(MOBILE));
            assertEquals(MOBILE, evidence.get(USER_AGENT));
        }
    }

    @Test
    public void AddFromBytes_ZeroInside_StopsAtTheZero() {
        try (EvidenceDeviceDetectionSwig evidence =
                 new EvidenceDeviceDetectionSwig()) {
            evidence.addFromBytes(bytes(USER_AGENT + "\0rest"),
                bytes("value\0rest"));
            assertEquals("value", evidence.get(USER_AGENT));
        }
    }

    @Test
    public void AddFromBytes_EmptyArrays_AddEmptyStrings() {
        try (EvidenceDeviceDetectionSwig evidence =
                 new EvidenceDeviceDetectionSwig()) {
            evidence.addFromBytes(new byte[0], new byte[0]);
            assertEquals(1, evidence.size());
            assertTrue(evidence.containsKey(""));
            assertEquals("", evidence.get(""));
        }
    }

    @Test
    public void GetValueAsString_NoTrailingZero_ReadsTheProperty() {
        try (ResultsHashSwig results = engine.process(MOBILE);
             StringValueSwig byName = results.getValueAsString(PROPERTY);
             StringValueSwig byBytes =
                 results.getValueAsString(bytes(PROPERTY));
             StringValueSwig byEndedBytes =
                 results.getValueAsString(zeroEnded(PROPERTY))) {
            assertTrue(byName.hasValue());
            assertEquals("True", byName.getValue());
            assertTrue(byBytes.hasValue());
            assertEquals(byName.getValue(), byBytes.getValue());
            assertEquals(byName.getValue(), byEndedBytes.getValue());
        }
    }

    @Test
    public void GetValueAsString_EmptyArray_HasNoValue() {
        try (ResultsHashSwig results = engine.process(MOBILE);
             StringValueSwig value = results.getValueAsString(new byte[0])) {
            assertFalse(value.hasValue());
        }
    }
}
