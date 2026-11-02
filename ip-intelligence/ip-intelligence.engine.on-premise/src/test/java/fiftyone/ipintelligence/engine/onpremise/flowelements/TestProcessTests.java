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

package fiftyone.ipintelligence.engine.onpremise.flowelements;

import fiftyone.ipintelligence.engine.onpremise.TestsBase;
import fiftyone.ipintelligence.engine.onpremise.data.DataValidatorHash;
import fiftyone.ipintelligence.shared.testhelpers.flowelements.ProcessTests;
import fiftyone.pipeline.engines.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestProcessTests extends TestsBase {

    @Before
    public void init() throws Exception {
        testInitialize(Constants.PerformanceProfiles.MaxPerformance);
    }

    @After
    public void cleanup() {
        testCleanup();
    }

    @Test
    public void Process_Ipi_NoEvidence() throws Exception {
        ProcessTests.noEvidence(getWrapper(), new DataValidatorHash(getWrapper().getEngine()));
    }

    @Test
    public void Process_Ipi_EmptyIpAddress() throws Exception {
        ProcessTests.emptyIpAddress(getWrapper(), new DataValidatorHash(getWrapper().getEngine()));
    }

    @Test
    public void Process_Ipi_NoHeaders() throws Exception {
        ProcessTests.noHeaders(getWrapper(), new DataValidatorHash(getWrapper().getEngine()));
    }

    @Test
    public void Process_Ipi_NoUsefulHeaders() throws Exception {
        ProcessTests.noUsefulHeaders(getWrapper(), new DataValidatorHash(getWrapper().getEngine()));
    }

    @Test
    public void Process_Ipi_CaseInsensitiveKeys() throws Exception {
        ProcessTests.caseInsensitiveEvidenceKeys(getWrapper(), new DataValidatorHash(getWrapper().getEngine()));
    }
}
