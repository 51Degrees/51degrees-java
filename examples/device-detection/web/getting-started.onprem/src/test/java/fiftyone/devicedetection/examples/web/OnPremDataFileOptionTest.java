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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static fiftyone.devicedetection.examples.web.GettingStartedWebOnPrem.resourceBase;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Checks where the on-premise web example takes its data file from, being
 * _51DEGREES_DD_PATH when it is set and the Lite file otherwise. A system
 * property stands in for the environment variable, because the lookup reads
 * either and a test cannot set an environment variable.
 */
public class OnPremDataFileOptionTest {
    private static final String DD_PATH = "_51DEGREES_DD_PATH";
    private static final String LITE =
            "cxx/data/device-detection/51Degrees-LiteV4.1.hash";

    private String savedDdPath;

    @Before
    public void saveProperties() {
        // An environment variable of this name would take precedence over
        // the system property set here, so the test cannot run.
        assumeTrue(System.getenv(DD_PATH) == null);
        savedDdPath = System.getProperty(DD_PATH);
        System.clearProperty(DD_PATH);
    }

    @After
    public void restoreProperties() {
        restore(DD_PATH, savedDdPath);
    }

    @Test
    public void usesLiteWhenNothingIsSet() throws Exception {
        assertEquals(LITE, dataFile());
    }

    @Test
    public void usesDdPathWhenItIsSet() throws Exception {
        System.setProperty(DD_PATH, "from-dd-path.hash");
        assertEquals("from-dd-path.hash", dataFile());
    }

    @Test
    public void namesOnlyDdPath() throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get(
                getFilePath(resourceBase) + "/WEB-INF/51Degrees-OnPrem.xml")),
                StandardCharsets.UTF_8);
        assertTrue("51Degrees-OnPrem.xml should take its data file from " +
                DD_PATH + " and otherwise use the Lite file",
                xml.contains("<DataFile>${" + DD_PATH + ":-" + LITE +
                        "}</DataFile>"));
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
