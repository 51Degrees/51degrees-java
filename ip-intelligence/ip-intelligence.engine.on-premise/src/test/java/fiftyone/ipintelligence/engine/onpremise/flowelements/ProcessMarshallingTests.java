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
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Processing keeps what crosses into native code small. It passes the native
 * engine only the Evidence the engine accepts, and takes the accepted keys
 * from the native engine once for each data set, never on each process. A
 * stand-in for the native engine records what it is given and asked.
 */
public class ProcessMarshallingTests extends TestsBase {

    private static final String CLIENT_IP_KEY = "query.client-ip";

    private static final String ADDRESS = "116.154.188.222";

    private static final int OTHER_EVIDENCE = 20;

    private static final int PROCESSES = 5;

    private final List<Map<String, String>> passed = new ArrayList<>();

    private EngineIpiSwig nativeEngine;

    private Field engineField;

    private Object realEngine;

    @Before
    public void init() throws Exception {
        testInitialize(Constants.PerformanceProfiles.LowMemory);
        nativeEngine = mock(EngineIpiSwig.class);
        when(nativeEngine.process(any(EvidenceIpiSwig.class)))
            .thenAnswer(invocation -> {
                passed.add(new TreeMap<>(
                    (EvidenceIpiSwig) invocation.getArgument(0)));
                return mock(ResultsIpiSwig.class);
            });
        engineField = IPIntelligenceOnPremiseEngine.class
            .getDeclaredField("engine");
        engineField.setAccessible(true);
        realEngine = engineField.get(getWrapper().getEngine());
        when(nativeEngine.getKeys())
            .thenReturn(((EngineIpiSwig) realEngine).getKeys());
        engineField.set(getWrapper().getEngine(), nativeEngine);
    }

    @After
    public void cleanup() throws Exception {
        engineField.set(getWrapper().getEngine(), realEngine);
        testCleanup();
    }

    @Test
    public void Process_PassesOnlyAcceptedEvidence() throws Exception {
        try (FlowData data = getWrapper().getPipeline().createFlowData()) {
            data.addEvidence(CLIENT_IP_KEY, ADDRESS);
            for (int i = 0; i < OTHER_EVIDENCE; i++) {
                data.addEvidence("query.other-" + i, "value " + i);
                data.addEvidence("cookie.other-" + i, "value " + i);
            }
            data.process();
        }
        assertEquals(1, passed.size());
        assertEquals(
            Collections.singletonMap(CLIENT_IP_KEY, ADDRESS),
            passed.get(0));
    }

    @Test
    public void Process_TakesNoKeysFromTheNativeEngine() throws Exception {
        IPIntelligenceOnPremiseEngine engine = getWrapper().getEngine();
        EvidenceKeyFilter filter = engine.getEvidenceKeyFilter();
        for (int i = 0; i < PROCESSES; i++) {
            try (FlowData data =
                     getWrapper().getPipeline().createFlowData()) {
                data.addEvidence(CLIENT_IP_KEY, ADDRESS);
                data.process();
            }
        }
        verify(nativeEngine, times(PROCESSES))
            .process(any(EvidenceIpiSwig.class));
        verify(nativeEngine, never()).getKeys();
        assertSame(filter, engine.getEvidenceKeyFilter());
    }
}
