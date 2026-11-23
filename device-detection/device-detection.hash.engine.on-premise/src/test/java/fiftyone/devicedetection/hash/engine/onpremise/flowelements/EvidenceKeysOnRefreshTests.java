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

package fiftyone.devicedetection.hash.engine.onpremise.flowelements;

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * After loading the data source, call getKeys and store the accepted
 * evidence keys. This MUST be done at start-up and any time the data is
 * refreshed
 * (device-detection-specification/pipeline-elements/device-detection-on-premise.md
 * line 86 at 706de1f0). The native engine here is a mock whose keys change
 * between refreshes, as they do when a refresh loads a data file with other
 * keys. Start-up with a real data file is covered by EvidenceKeysTests.
 */
public class EvidenceKeysOnRefreshTests {

    @Test
    public void EvidenceKeys_ReadAgainOnRefresh() throws Exception {
        EngineOverMockNativeCode mocks = new EngineOverMockNativeCode();
        DeviceDetectionHashEngine engine = mocks.engine;

        mocks.setKeys("header.user-agent");
        engine.refreshData(null, new byte[0]);
        EvidenceKeyFilter first = engine.getEvidenceKeyFilter();
        assertTrue(first.include("header.user-agent"));
        // Not case sensitive.
        assertTrue(first.include("header.User-Agent"));
        assertFalse(first.include("header.sec-ch-ua"));

        mocks.setKeys("header.user-agent", "header.sec-ch-ua");
        engine.refreshData(null, new byte[0]);
        EvidenceKeyFilter second = engine.getEvidenceKeyFilter();
        assertTrue("A key of the new data was not accepted after refresh",
            second.include("header.sec-ch-ua"));
        verify(mocks.nativeEngine, times(2)).refreshData(new byte[0]);
    }

    /**
     * The keys are stored, so using the filter does not call the native code.
     */
    @Test
    public void EvidenceKeys_StoredNotReadForEachUse() throws Exception {
        EngineOverMockNativeCode mocks = new EngineOverMockNativeCode();
        mocks.setKeys("header.user-agent");
        mocks.engine.refreshData(null, new byte[0]);
        clearInvocations(mocks.nativeEngine);
        for (int i = 0; i < 10; i++) {
            mocks.engine.getEvidenceKeyFilter().include("header.user-agent");
        }
        verify(mocks.nativeEngine, times(0)).getKeys();
    }
}
