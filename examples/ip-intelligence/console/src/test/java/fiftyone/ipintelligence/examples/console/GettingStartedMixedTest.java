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

package fiftyone.ipintelligence.examples.console;

import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import fiftyone.ipintelligence.examples.shared.ExampleProcess;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

public class GettingStartedMixedTest {

    /**
     * The device detection data file is not part of this repository and is
     * not fetched by CI, so the test is skipped when it is absent.
     */
    static boolean ddDataFileExists() {
        if (Files.exists(Paths.get(GettingStartedMixed.DD_DATA_FILE_DEFAULT))) {
            return true;
        }
        try {
            getFilePath(GettingStartedMixed.DD_DATA_FILE_DEFAULT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    public void gettingStartedMixedTest() throws Exception {
        assumeTrue("Skipping test, no device detection data file found at " +
                        GettingStartedMixed.DD_DATA_FILE_DEFAULT,
                ddDataFileExists());
        // Use the enterprise data file when present, otherwise fall back to
        // the free Lite or ASN file so the test can run from a fresh clone.
        String ipiDataFile = DataFileHelper.findAvailableDataFile();
        assumeTrue("Skipping test, no IP Intelligence data file found",
                ipiDataFile != null);
        GettingStartedMixed.run(GettingStartedMixed.DD_DATA_FILE_DEFAULT,
                ipiDataFile,
                GettingStartedMixed.setUpCombinedEvidence(),
                System.out);
    }

    /**
     * The data files given as arguments are the ones the example uses,
     * when _51DEGREES_DD_PATH and _51DEGREES_IPI_PATH name others. The
     * variables here name files that are not there, so the example ends
     * well only if it left them alone.
     */
    @Test
    public void main_UsesTheDataFilesGivenAsArguments() throws Exception {
        assumeTrue("Skipping test, no device detection data file found at " +
                        GettingStartedMixed.DD_DATA_FILE_DEFAULT,
                ddDataFileExists());
        String ipiDataFile = DataFileHelper.findAvailableDataFile();
        assumeTrue("Skipping test, no IP Intelligence data file found",
                ipiDataFile != null);
        Map<String, String> environment = new HashMap<>();
        environment.put("_51DEGREES_DD_PATH",
                new File("not-there.hash").getAbsolutePath());
        environment.put(DataFileHelper.IPI_PATH_VARIABLE,
                new File("not-there.ipi").getAbsolutePath());
        ExampleProcess.Result result = ExampleProcess.runWith(
                GettingStartedMixed.class, environment,
                getFilePath(GettingStartedMixed.DD_DATA_FILE_DEFAULT)
                        .getAbsolutePath(),
                new File(ipiDataFile).getAbsolutePath());
        assertEquals(result.output, 0, result.exitCode);
    }

    /**
     * The example gives each data file to its options itself, and sets no
     * system property, so that a later run in the same process finds its
     * own files.
     */
    @Test
    public void run_LeavesTheDataFilePropertiesAsTheyWere() throws Exception {
        assumeTrue("Skipping test, no device detection data file found at " +
                        GettingStartedMixed.DD_DATA_FILE_DEFAULT,
                ddDataFileExists());
        String ipiDataFile = DataFileHelper.findAvailableDataFile();
        assumeTrue("Skipping test, no IP Intelligence data file found",
                ipiDataFile != null);
        String ddBefore = System.getProperty("_51DEGREES_DD_PATH");
        String ipiBefore =
                System.getProperty(DataFileHelper.IPI_PATH_VARIABLE);
        GettingStartedMixed.run(GettingStartedMixed.DD_DATA_FILE_DEFAULT,
                ipiDataFile,
                GettingStartedMixed.setUpCombinedEvidence(),
                System.out);
        assertEquals(ddBefore, System.getProperty("_51DEGREES_DD_PATH"));
        assertEquals(ipiBefore,
                System.getProperty(DataFileHelper.IPI_PATH_VARIABLE));
    }
}
