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

package fiftyone.ipintelligence.shared.testhelpers;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class FileUtilsTest {

    Logger logger = LoggerFactory.getLogger(FileUtils.class);

    /**
     * location of "." is the same as user.dir
     **/
    @Test()
    public void testUserDir() throws IOException {
        File f = new File("./");
        logger.info(". is {}", f.getCanonicalPath());
        File g = new File(System.getProperty("user.dir"));
        logger.info("user.dir is {}", g.getCanonicalPath());
        assertEquals(". and user dir should be the same", f.getCanonicalPath(), g.getCanonicalPath());
    }

    private static final String NEW_PATH_NAME = "_51DEGREES_IPI_PATH";
    private static final String OLD_PATH_NAME = "51DEGREES_IPI_PATH";

    /**
     * System properties stand in for the environment variables, because
     * the lookup reads either and a test cannot set an environment
     * variable.
     */
    @Test
    public void oldPathNameIsReadWhenNewIsNotSet() throws IOException {
        assumePathNamesNotInEnvironment();
        String savedNew = System.clearProperty(NEW_PATH_NAME);
        String savedOld = System.clearProperty(OLD_PATH_NAME);
        File file = File.createTempFile("old-path-name", ".ipi");
        try {
            System.setProperty(OLD_PATH_NAME, file.getPath());
            assertEquals(file, FileUtils.getHashFile());
        } finally {
            restore(NEW_PATH_NAME, savedNew);
            restore(OLD_PATH_NAME, savedOld);
            file.delete();
        }
    }

    @Test
    public void newPathNameWinsOverOld() throws IOException {
        assumePathNamesNotInEnvironment();
        String savedNew = System.clearProperty(NEW_PATH_NAME);
        String savedOld = System.clearProperty(OLD_PATH_NAME);
        File newFile = File.createTempFile("new-path-name", ".ipi");
        File oldFile = File.createTempFile("old-path-name", ".ipi");
        try {
            System.setProperty(NEW_PATH_NAME, newFile.getPath());
            System.setProperty(OLD_PATH_NAME, oldFile.getPath());
            assertEquals(newFile, FileUtils.getHashFile());
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
        assertEquals(OLD_PATH_NAME, FileUtils.IPI_PATH_ENV_VAR);
        assertTrue(FileUtils.class.getField("IPI_PATH_ENV_VAR")
            .isAnnotationPresent(Deprecated.class));
        assertEquals(NEW_PATH_NAME,
            FileUtils.class.getField("IPI_PATH_VARIABLE").get(null));
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
