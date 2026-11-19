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
import fiftyone.ipintelligence.engine.onpremise.interop.swig.EngineIpiSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.EvidenceIpiSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.ResultsIpiSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.StringValueSwig;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Processing keeps the native results and reads no value from them, and a
 * value is read from them only when it is asked for. A stand-in for the
 * native engine gives results that record what is asked of them.
 */
public class ProcessReadsNoValueTests extends TestsBase {

    @Before
    public void init() throws Exception {
        testInitialize(Constants.PerformanceProfiles.LowMemory);
    }

    @After
    public void cleanup() {
        testCleanup();
    }

    @Test
    public void Process_ReadsNoValue() throws Exception {
        IPIntelligenceOnPremiseEngine engine = getWrapper().getEngine();
        Integer index = engine.getPropertyIndex("RegisteredName");
        assertNotNull(index);
        EngineIpiSwig nativeEngine = mock(EngineIpiSwig.class);
        ResultsIpiSwig results = mock(ResultsIpiSwig.class);
        when(nativeEngine.process(any(EvidenceIpiSwig.class)))
            .thenReturn(results);
        when(results.containsProperty(anyString())).thenReturn(true);
        StringValueSwig value = mock(StringValueSwig.class);
        when(value.hasValue()).thenReturn(true);
        when(value.getValue()).thenReturn("Example");
        when(results.getValueAsString(index.intValue())).thenReturn(value);

        Field field = IPIntelligenceOnPremiseEngine.class
            .getDeclaredField("engine");
        field.setAccessible(true);
        Object real = field.get(engine);
        field.set(engine, nativeEngine);
        try (FlowData data = getWrapper().getPipeline().createFlowData()) {
            data.addEvidence("query.client-ip", "116.154.188.222");
            data.process();

            verify(nativeEngine).process(any(EvidenceIpiSwig.class));
            verifyNoInteractions(results);

            IPIntelligenceData ipi = data.get(IPIntelligenceData.class);
            assertEquals("Example", ipi.getRegisteredName().getValue());
            verify(results).getValueAsString(index.intValue());
            verify(results, never()).getValueAsString(anyString());
        } finally {
            field.set(engine, real);
        }
    }
}
