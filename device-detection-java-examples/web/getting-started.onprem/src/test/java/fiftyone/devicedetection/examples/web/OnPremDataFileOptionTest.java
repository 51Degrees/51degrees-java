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

package fiftyone.devicedetection.examples.web;

import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.configuration.PipelineOptionsFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static fiftyone.devicedetection.examples.web.GettingStartedWebOnPrem.resourceBase;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * Checks the order in which the on-premise web example picks its data file,
 * being 51DEGREES_DD_PATH first, then TestDataFile, then the Lite file.
 * System properties stand in for the environment variables, because the
 * lookup reads either and a test cannot set an environment variable.
 */
public class OnPremDataFileOptionTest {
    private static final String DD_PATH = "51DEGREES_DD_PATH";
    private static final String TEST_DATA_FILE = "TestDataFile";
    private static final String LITE =
            "device-detection-data/51Degrees-LiteV4.1.hash";

    private String savedDdPath;
    private String savedTestDataFile;

    @Before
    public void saveProperties() {
        // An environment variable of either name would take precedence over
        // the system properties set here, so the test cannot run.
        assumeTrue(System.getenv(DD_PATH) == null);
        assumeTrue(System.getenv(TEST_DATA_FILE) == null);
        assumeTrue(System.getenv(TEST_DATA_FILE.toUpperCase()) == null);
        savedDdPath = System.getProperty(DD_PATH);
        savedTestDataFile = System.getProperty(TEST_DATA_FILE);
        System.clearProperty(DD_PATH);
        System.clearProperty(TEST_DATA_FILE);
    }

    @After
    public void restoreProperties() {
        restore(DD_PATH, savedDdPath);
        restore(TEST_DATA_FILE, savedTestDataFile);
    }

    @Test
    public void usesLiteWhenNothingIsSet() throws Exception {
        assertEquals(LITE, dataFile());
    }

    @Test
    public void usesTestDataFileWhenOnlyThatIsSet() throws Exception {
        System.setProperty(TEST_DATA_FILE, "from-test-data-file.hash");
        assertEquals("from-test-data-file.hash", dataFile());
    }

    @Test
    public void prefersDdPathOverTestDataFile() throws Exception {
        System.setProperty(TEST_DATA_FILE, "from-test-data-file.hash");
        System.setProperty(DD_PATH, "from-dd-path.hash");
        assertEquals("from-dd-path.hash", dataFile());
    }

    private static String dataFile() throws Exception {
        PipelineOptions options = PipelineOptionsFactory.getOptionsFromFile(
                getFilePath(resourceBase) + "/WEB-INF/51Degrees-OnPrem.xml");
        return options.findAndSubstitute("DeviceDetectionHashEngine", "DataFile");
    }

    private static void restore(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
