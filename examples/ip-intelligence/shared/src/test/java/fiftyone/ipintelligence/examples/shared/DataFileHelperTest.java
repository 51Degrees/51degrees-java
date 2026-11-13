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

package fiftyone.ipintelligence.examples.shared;

import fiftyone.pipeline.engines.Constants.PerformanceProfiles;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class DataFileHelperTest {

    @Test
    public void findPerformanceProfile_IgnoresCase() {
        StringWriter written = new StringWriter();
        assertEquals(PerformanceProfiles.MaxPerformance,
                DataFileHelper.findPerformanceProfile("maxperformance",
                        new PrintWriter(written)));
        assertEquals(PerformanceProfiles.LowMemory,
                DataFileHelper.findPerformanceProfile("LOWMEMORY",
                        new PrintWriter(written)));
        assertEquals("", written.toString());
    }

    @Test
    public void findPerformanceProfile_Unknown_WritesNamesAccepted() {
        StringWriter written = new StringWriter();
        assertNull(DataFileHelper.findPerformanceProfile("Fastest",
                new PrintWriter(written)));
        for (PerformanceProfiles profile : PerformanceProfiles.values()) {
            assertTrue(written.toString(),
                    written.toString().contains(profile.name()));
        }
    }

    private static final String NEW_PATH_NAME = "_51DEGREES_IPI_PATH";
    private static final String OLD_PATH_NAME = "51DEGREES_IPI_PATH";

    /**
     * System properties stand in for the environment variables, because
     * the lookup reads either and a test cannot set an environment
     * variable.
     */
    @Test
    public void getDataFileLocation_OldNameWhenNewIsNotSet()
            throws Exception {
        assumePathNamesNotInEnvironment();
        String savedNew = System.clearProperty(NEW_PATH_NAME);
        String savedOld = System.clearProperty(OLD_PATH_NAME);
        File file = File.createTempFile("old-path-name", ".ipi");
        try {
            System.setProperty(OLD_PATH_NAME, file.getPath());
            assertEquals(file.getPath(),
                    DataFileHelper.getDataFileLocation(null));
        } finally {
            restore(NEW_PATH_NAME, savedNew);
            restore(OLD_PATH_NAME, savedOld);
            file.delete();
        }
    }

    @Test
    public void getDataFileLocation_NewNameWinsOverOld() throws Exception {
        assumePathNamesNotInEnvironment();
        String savedNew = System.clearProperty(NEW_PATH_NAME);
        String savedOld = System.clearProperty(OLD_PATH_NAME);
        File newFile = File.createTempFile("new-path-name", ".ipi");
        File oldFile = File.createTempFile("old-path-name", ".ipi");
        try {
            System.setProperty(NEW_PATH_NAME, newFile.getPath());
            System.setProperty(OLD_PATH_NAME, oldFile.getPath());
            assertEquals(newFile.getPath(),
                    DataFileHelper.getDataFileLocation(null));
        } finally {
            restore(NEW_PATH_NAME, savedNew);
            restore(OLD_PATH_NAME, savedOld);
            newFile.delete();
            oldFile.delete();
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void oldPathConstantKeepsItsValue() throws Exception {
        assertEquals(OLD_PATH_NAME, DataFileHelper.IPI_PATH_ENV_VAR);
        assertTrue(DataFileHelper.class.getField("IPI_PATH_ENV_VAR")
                .isAnnotationPresent(Deprecated.class));
        assertEquals(NEW_PATH_NAME,
                DataFileHelper.class.getField("IPI_PATH_VARIABLE").get(null));
    }

    private static void assumePathNamesNotInEnvironment() {
        assumeTrue(System.getenv(NEW_PATH_NAME) == null);
        assumeTrue(System.getenv(OLD_PATH_NAME) == null);
    }

    private static void restore(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
