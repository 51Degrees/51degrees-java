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
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static fiftyone.pipeline.util.FileFinder.getFilePath;
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
}
