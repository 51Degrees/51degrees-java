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

import fiftyone.devicedetection.hash.engine.onpremise.interop.Swig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.BoolValueSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.EvidenceDeviceDetectionSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.ResultsHashSwig;
import fiftyone.pipeline.core.data.Evidence;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.junit.Test;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * The Element Data MUST read a Property value from ResultsHash only when
 * that Property is requested, and MUST NOT copy Property values out of
 * ResultsHash while the Engine processes the Flow Data
 * (device-detection-specification/pipeline-elements/device-detection-on-premise.md
 * line 152 at 706de1f0). The native results here are mocks that record
 * every call made to them.
 */
public class ReadOnlyWhenRequestedTests {

    private static DeviceDataHashDefault newData(
        DeviceDetectionHashEngine engine) {
        return new DeviceDataHashDefault(
            mock(Logger.class),
            mock(FlowData.class),
            engine,
            mock(MissingPropertyService.class));
    }

    /**
     * Processing reads nothing from the results it stores. This creates
     * native evidence, so it needs the native library, though no data file.
     */
    @Test
    public void Process_ReadsNoValue() throws Exception {
        EngineOverMockNativeCode mocks = new EngineOverMockNativeCode();
        Field keys = DeviceDetectionHashEngine.class
            .getDeclaredField("evidenceKeys");
        keys.setAccessible(true);
        keys.set(mocks.engine,
            Collections.singletonList("header.user-agent"));
        ResultsHashSwig results = mock(ResultsHashSwig.class);
        when(mocks.nativeEngine.process(any(EvidenceDeviceDetectionSwig.class)))
            .thenReturn(results);

        Map<String, Object> evidence = new HashMap<>();
        evidence.put("header.user-agent", "Mozilla/5.0");
        Evidence flowEvidence = mock(Evidence.class);
        when(flowEvidence.asKeyMap()).thenReturn(evidence);
        FlowData flowData = mock(FlowData.class);
        when(flowData.getEvidence()).thenReturn(flowEvidence);

        DeviceDataHashDefault data = newData(mocks.engine);
        mocks.engine.processEngine(flowData, data);

        verify(mocks.nativeEngine).process(any(EvidenceDeviceDetectionSwig.class));
        verifyNoInteractions(results);
    }

    /**
     * Storing the results reads nothing, and a request for one Property reads
     * that Property and no other.
     */
    @Test
    public void Request_ReadsOnlyThatProperty() throws Exception {
        EngineOverMockNativeCode mocks = new EngineOverMockNativeCode();
        ResultsHashSwig results = mock(ResultsHashSwig.class);
        BoolValueSwig value = mock(BoolValueSwig.class);
        when(value.hasValue()).thenReturn(true);
        when(value.getValue()).thenReturn(true);
        when(results.containsProperty(any(byte[].class))).thenReturn(true);
        when(results.getValueAsBool(any(byte[].class))).thenReturn(value);
        when(results.getValueAsBool(any(int.class))).thenReturn(value);

        DeviceDataHashDefault data = newData(mocks.engine);
        data.setResults(results);
        verifyNoInteractions(results);

        assertTrue(data.getIsMobile().getValue());
        byte[] name = Swig.asBytes("ismobile");
        verify(results).containsProperty(aryEq(name));
        verify(results).getValueAsBool(aryEq(name));
        verifyNoMoreInteractions(results);
    }
}
