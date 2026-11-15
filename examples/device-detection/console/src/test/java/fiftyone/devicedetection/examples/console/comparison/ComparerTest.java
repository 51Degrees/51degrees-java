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

package fiftyone.devicedetection.examples.console.comparison;

import fiftyone.common.testhelpers.LogbackHelper;
import fiftyone.devicedetection.shared.testhelpers.FileUtils;
import fiftyone.pipeline.util.FileFinder;
import org.junit.Test;

import java.io.PrintWriter;
import java.util.Arrays;

import static fiftyone.devicedetection.examples.console.comparison.Comparer.*;
import static org.junit.Assert.assertTrue;

/**
 * Runs the comparison, which uses the Enterprise data file when there is
 * one and the free Lite data file otherwise, so the test runs on a fresh
 * clone and is never skipped (device-detection-specification/required-examples.md,
 * "Examples MUST run out of the box with the free data files").
 */
public class ComparerTest {

    @Test
    public void TestCompare() throws Exception {
        LogbackHelper.configureLogback(FileFinder.getFilePath("logback.xml"));
        new Comparer().compare(Arrays.asList(DEFAULT_SOLUTIONS),
                DEFAULT_NUMBER_OF_THREADS,
                DEFAULT_NUMBER_OF_RESULTS,
                new Reporting.Minimal(),
                new PrintWriter(System.out, true));
    }

    /**
     * With no Enterprise data file, the 51Degrees solution uses the free
     * Lite data file rather than stopping.
     */
    @Test
    public void usesTheFreeDataFile() throws Exception {
        DetectionImplFiftyOneDegrees.FiftyOneConfig config =
                new DetectionImplFiftyOneDegrees.FiftyOneConfig();
        assertTrue("The comparison should use the Enterprise data file " +
                "when there is one, and otherwise the free Lite data file, " +
                "not " + config.dataFile,
                config.dataFile.endsWith(
                        DetectionImplFiftyOneDegrees.ENTERPRISE_HASH_DATA_FILE_NAME) ||
                config.dataFile.replace('\\', '/').endsWith(
                        FileUtils.LITE_HASH_DATA_FILE_PATH));
    }
}
