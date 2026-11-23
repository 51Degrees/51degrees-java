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

import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.BoolValueSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.ResultsHashSwig;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.junit.Test;
import org.slf4j.Logger;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Every read SHOULD use the position
 * (device-detection-specification/pipeline-elements/device-detection-on-premise.md
 * line 205 at 706de1f0). A read by name looks the name up in the table from
 * the names the engine was created with to their positions, and then reads
 * by position. The native results here are mocks that record every call.
 */
public class ReadsByPositionTests {

    @Test
    public void Read_OfANamedProperty_IsByPositionOnly() throws Exception {
        EngineOverMockNativeCode mocks = new EngineOverMockNativeCode();
        mocks.setPropertyNames("ScreenPixelsHeight", "IsMobile");

        ResultsHashSwig results = mock(ResultsHashSwig.class);
        BoolValueSwig value = mock(BoolValueSwig.class);
        when(value.hasValue()).thenReturn(true);
        when(value.getValue()).thenReturn(true);
        when(results.containsProperty(any(byte[].class))).thenReturn(true);
        when(results.getValueAsBool(any(byte[].class))).thenReturn(value);
        when(results.getValueAsBool(1)).thenReturn(value);

        DeviceDataHashDefault data = new DeviceDataHashDefault(
            mock(Logger.class),
            mock(FlowData.class),
            mocks.engine,
            mock(MissingPropertyService.class));
        data.setResults(results);

        assertTrue(data.getIsMobile().getValue());
        // IsMobile is at position 1, and nothing is read by its name.
        verify(results).getValueAsBool(1);
        verifyNoMoreInteractions(results);
    }
}
