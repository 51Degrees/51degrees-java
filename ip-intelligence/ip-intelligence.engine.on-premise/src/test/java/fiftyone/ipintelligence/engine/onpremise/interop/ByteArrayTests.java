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

package fiftyone.ipintelligence.engine.onpremise.interop;

import fiftyone.ipintelligence.engine.onpremise.interop.swig.ConfigIpiSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.EngineIpiSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.RequiredPropertiesConfigSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.ResultsIpiSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.UTF8StringSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.UTF8StringValueSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.VectorStringSwig;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static fiftyone.ipintelligence.shared.testhelpers.FileUtils.LITE_IPI_DATA_FILE_NAME;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The methods that copy a UTF-8 value into a Java byte array take the array
 * and use its length, so they copy no more than it holds, and an empty array
 * gets nothing.
 */
public class ByteArrayTests {

    private static final String ADDRESS = "116.154.188.222";

    private static final String PROPERTY = "RegisteredName";

    private static final byte[] ABC = {'a', 'b', 'c'};

    private static UTF8StringSwig abc() {
        return new UTF8StringSwig(new short[]{'a', 'b', 'c'});
    }

    @Test
    public void CopyToArray_ArrayLongerThanValue_CopiesTheValue() {
        try (UTF8StringSwig value = abc()) {
            byte[] copy = {9, 9, 9, 9, 9};
            assertEquals(3, value.copyToArray(copy));
            assertArrayEquals(new byte[]{'a', 'b', 'c', 9, 9}, copy);
        }
    }

    @Test
    public void CopyToArray_ArrayShorterThanValue_CopiesWhatFits() {
        try (UTF8StringSwig value = abc()) {
            byte[] copy = new byte[2];
            assertEquals(2, value.copyToArray(copy));
            assertArrayEquals(Arrays.copyOf(ABC, 2), copy);
        }
    }

    @Test
    public void CopyToArray_EmptyArray_CopiesNothing() {
        try (UTF8StringSwig value = abc()) {
            assertEquals(0, value.copyToArray(new byte[0]));
        }
    }

    @Test
    public void CopyToArray_NullArray_Throws() {
        try (UTF8StringSwig value = abc()) {
            value.copyToArray(null);
            fail("Copying into a null array should throw");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("null"));
        }
    }

    @Test
    public void CopyValueAsUTF8String_CopiesOnlyWhenTheArrayIsLongEnough() {
        File file;
        try {
            file = getFilePath(LITE_IPI_DATA_FILE_NAME);
        } catch (IllegalArgumentException e) {
            throw new AssertionError("The test needs the free Lite data " +
                "file, which the build fetches into cxx/data.", e);
        }
        ConfigIpiSwig config = new ConfigIpiSwig();
        config.setLowMemory();
        RequiredPropertiesConfigSwig required;
        try (VectorStringSwig names = new VectorStringSwig()) {
            names.add(PROPERTY);
            required = new RequiredPropertiesConfigSwig(names);
        }
        EngineIpiSwig engine =
            new EngineIpiSwig(file.getAbsolutePath(), config, required);
        try (ResultsIpiSwig results = engine.process(ADDRESS);
             UTF8StringValueSwig value = results.getValueAsUTF8String(0)) {
            assertTrue(value.hasValue());
            byte[] expected;
            try (UTF8StringSwig bytes = value.getValue()) {
                expected = new byte[bytes.size()];
                for (int i = 0; i < expected.length; i++) {
                    expected[i] = (byte) (short) bytes.get(i);
                }
            }
            assertTrue(expected.length > 1);

            byte[] tooShort = new byte[expected.length - 1];
            assertEquals(expected.length,
                results.copyValueAsUTF8String(0, tooShort));
            assertArrayEquals(new byte[expected.length - 1], tooShort);

            assertEquals(expected.length,
                results.copyValueAsUTF8String(0, new byte[0]));

            byte[] copy = new byte[expected.length + 1];
            assertEquals(expected.length,
                results.copyValueAsUTF8String(0, copy));
            assertArrayEquals(expected,
                Arrays.copyOf(copy, expected.length));
        } finally {
            engine.delete();
            required.delete();
            config.delete();
        }
    }
}
