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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fiftyone.ipintelligence.examples.shared.DataFileHelper.ENTERPRISE_DATA_FILE_REL_PATH;
import static org.junit.Assume.assumeTrue;

public class MetricsOnPremTest {
    private static final Logger logger = LoggerFactory.getLogger(MetricsOnPremTest.class);

    @Test
    public void metricsOnPremTest() throws Exception {
        assumeTrue("Skipping test, no IP Intelligence data file found",
                SuspiciousOnPremTest.dataFileAvailable());
        try (LoggerOutputStream outStream = new LoggerOutputStream(logger)) {
            // Process a small number of IPv4 ranges with a low sample
            // percentage so that the test completes quickly.
            MetricsOnPrem.run(ENTERPRISE_DATA_FILE_REL_PATH,
                    0.001,
                    10,
                    range -> range[0].contains("."),
                    outStream);
        }
    }
}
