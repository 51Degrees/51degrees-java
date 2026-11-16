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

import fiftyone.devicedetection.hash.engine.onpremise.TestsBase;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.BoolValueSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.ResultsHashSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.StringValueSwig;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * A read of a property the engine has a position for goes by that
 * position, and asks the native results nothing by name, not even whether
 * they hold the property.
 */
public class ReadByPositionTests extends TestsBase {

    private static final Logger logger = mock(Logger.class);

    @Before
    public void init() throws Exception {
        testInitialize(Constants.PerformanceProfiles.HighPerformance);
    }

    @After
    public void cleanup() {
        testCleanup();
    }

    @Test
    public void BooleanRead_ByPosition_NoLookupByName() throws Exception {
        DeviceDetectionHashEngine engine = getWrapper().getEngine();
        Integer index = engine.getPropertyIndex("IsMobile");
        assertNotNull(index);
        try (FlowData flowData =
                 getWrapper().getPipeline().createFlowData()) {
            ResultsHashSwig results = results();
            BoolValueSwig value = mock(BoolValueSwig.class);
            when(value.hasValue()).thenReturn(true);
            when(value.getValue()).thenReturn(true);
            when(results.getValueAsBool(index.intValue())).thenReturn(value);
            DeviceDataHashDefault data = data(flowData, engine, results);

            assertEquals(Boolean.TRUE, data.getIsMobile().getValue());

            verify(results).getValueAsBool(index.intValue());
            verify(results, never()).containsProperty(any(byte[].class));
        }
    }

    @Test
    public void StringRead_ByPosition_NoLookupByName() throws Exception {
        DeviceDetectionHashEngine engine = getWrapper().getEngine();
        Integer index = engine.getPropertyIndex("BrowserName");
        assertNotNull(index);
        try (FlowData flowData =
                 getWrapper().getPipeline().createFlowData()) {
            ResultsHashSwig results = results();
            StringValueSwig value = mock(StringValueSwig.class);
            when(value.hasValue()).thenReturn(true);
            when(value.getValue()).thenReturn("Safari");
            when(results.getValueAsString(index.intValue())).thenReturn(value);
            DeviceDataHashDefault data = data(flowData, engine, results);

            assertEquals("Safari", data.getBrowserName().getValue());

            verify(results).getValueAsString(index.intValue());
            verify(results, never()).containsProperty(any(byte[].class));
        }
    }

    /**
     * Native results that say they hold every property, so that a read
     * which asks by name still reaches the value.
     */
    private static ResultsHashSwig results() {
        ResultsHashSwig results = mock(ResultsHashSwig.class);
        when(results.containsProperty(any(byte[].class))).thenReturn(true);
        return results;
    }

    private static DeviceDataHashDefault data(
        FlowData flowData,
        DeviceDetectionHashEngine engine,
        ResultsHashSwig results) {
        DeviceDataHashDefault data = new DeviceDataHashDefault(
            logger,
            flowData,
            engine,
            MissingPropertyServiceDefault.getInstance());
        data.setResults(results);
        return data;
    }
}
