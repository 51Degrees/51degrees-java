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
import fiftyone.ipintelligence.examples.shared.EvidenceHelper;
import fiftyone.ipintelligence.examples.shared.ExampleProcess;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class GettingStartedOnPremTest {

    @Test
    public void gettingStartedOnPremTest() throws Exception {
        // Use the enterprise data file when present, otherwise fall back to
        // the free Lite or ASN file so the test can run from a fresh clone.
        String dataFile = DataFileHelper.findAvailableDataFile();
        assumeTrue("Skipping test, no IP Intelligence data file found",
                dataFile != null);
        GettingStartedOnPrem.run(dataFile,
                EvidenceHelper.setUpEvidence(), System.out);
    }

    /**
     * A data file given as an argument is the one the example uses, when
     * _51DEGREES_IPI_PATH names another. The variable here names a file
     * that is not there, so the example ends well only if it left it alone.
     */
    @Test
    public void main_UsesTheDataFileGivenAsAnArgument() throws Exception {
        String dataFile = DataFileHelper.findAvailableDataFile();
        assumeTrue("Skipping test, no IP Intelligence data file found",
                dataFile != null);
        dataFile = new File(dataFile).getAbsolutePath();
        ExampleProcess.Result result = ExampleProcess.runWith(
                GettingStartedOnPrem.class,
                Collections.singletonMap(DataFileHelper.IPI_PATH_VARIABLE,
                        new File("not-there.ipi").getAbsolutePath()),
                dataFile);
        assertEquals(result.output, 0, result.exitCode);
        assertTrue(result.output, result.output.contains(
                "from location '" + dataFile + "'"));
    }

    /**
     * The example gives the data file to its options itself, and sets no
     * system property, so that a later run in the same process finds its
     * own file.
     */
    @Test
    public void run_LeavesTheDataFilePropertyAsItWas() throws Exception {
        String dataFile = DataFileHelper.findAvailableDataFile();
        assumeTrue("Skipping test, no IP Intelligence data file found",
                dataFile != null);
        String before = System.getProperty(DataFileHelper.IPI_PATH_VARIABLE);
        GettingStartedOnPrem.run(dataFile,
                EvidenceHelper.setUpEvidence(), System.out);
        assertEquals(before,
                System.getProperty(DataFileHelper.IPI_PATH_VARIABLE));
    }
}