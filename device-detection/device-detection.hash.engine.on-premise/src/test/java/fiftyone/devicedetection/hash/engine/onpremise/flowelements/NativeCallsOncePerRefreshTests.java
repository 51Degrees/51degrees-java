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

import org.junit.Test;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The main performance bottleneck is usually marshalling data to and from
 * native code. As such, steps SHOULD be taken to minimize this as much as
 * possible
 * (device-detection-specification/pipeline-elements/device-detection-on-premise.md
 * line 339 at 706de1f0), the last being that values which change only on a
 * data refresh are stored in the engine until a refresh occurs. Counts the
 * calls to a mock native engine.
 */
public class NativeCallsOncePerRefreshTests {

    /**
     * Property metadata is read from the native code once, and again only
     * after a refresh.
     */
    @Test
    public void PropertyMetaData_ReadOncePerRefresh() throws Exception {
        EngineOverMockNativeCode mocks = new EngineOverMockNativeCode();
        mocks.engine.refreshData(null, new byte[0]);
        clearInvocations(mocks.nativeEngine);

        for (int i = 0; i < 5; i++) {
            mocks.engine.getProperties();
        }
        verify(mocks.nativeEngine, times(1)).getMetaData();

        mocks.engine.refreshData(null, new byte[0]);
        for (int i = 0; i < 5; i++) {
            mocks.engine.getProperties();
        }
        verify(mocks.nativeEngine, times(2)).getMetaData();
    }

    /**
     * The evidence keys and their filter are read once for each refresh.
     */
    @Test
    public void EvidenceKeys_ReadOncePerRefresh() throws Exception {
        EngineOverMockNativeCode mocks = new EngineOverMockNativeCode();
        mocks.setKeys("header.user-agent");
        for (int refresh = 1; refresh <= 3; refresh++) {
            mocks.engine.refreshData(null, new byte[0]);
            for (int i = 0; i < 5; i++) {
                mocks.engine.getEvidenceKeyFilter()
                    .include("header.user-agent");
            }
            verify(mocks.nativeEngine, times(refresh)).getKeys();
        }
    }
}
