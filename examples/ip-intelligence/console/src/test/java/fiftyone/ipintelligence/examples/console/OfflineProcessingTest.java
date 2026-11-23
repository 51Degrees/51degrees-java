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
import fiftyone.ipintelligence.shared.testhelpers.FileUtils;
import org.junit.Test;

import java.io.FileInputStream;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assume.assumeTrue;


public class OfflineProcessingTest {
    private static final Logger logger = LoggerFactory.getLogger(OfflineProcessingTest.class);

    @Test
    public void offlineProcessingTest() throws Exception {
        // The example reads the RegisteredName property, which the free ASN
        // file does not contain, so only the enterprise and Lite files are
        // candidates here.
        String dataFile = DataFileHelper.findAvailableDataFile(
                DataFileHelper.ENTERPRISE_DATA_FILE_REL_PATH,
                DataFileHelper.LITE_DATA_FILE_REL_PATH);
        assumeTrue("Skipping test, no IP Intelligence data file with the " +
                        "RegisteredName property found",
                dataFile != null);
        try (LoggerOutputStream outStream = new LoggerOutputStream(logger)) {
            OfflineProcessing.run(dataFile,
                    new FileInputStream(Objects.requireNonNull(FileUtils.getEvidenceFile())),
                    outStream);
        }
    }

}