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
import fiftyone.ipintelligence.engine.onpremise.interop.swig.ResultsIpiSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.StringValueSwig;
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
        testInitialize(Constants.PerformanceProfiles.MaxPerformance);
    }

    @After
    public void cleanup() {
        testCleanup();
    }

    @Test
    public void StringRead_ByPosition_NoLookupByName() throws Exception {
        IPIntelligenceOnPremiseEngine engine = getWrapper().getEngine();
        Integer index = engine.getPropertyIndex("RegisteredName");
        assertNotNull(index);
        try (FlowData flowData =
                 getWrapper().getPipeline().createFlowData()) {
            ResultsIpiSwig results = mock(ResultsIpiSwig.class);
            when(results.containsProperty(any(String.class))).thenReturn(true);
            StringValueSwig value = mock(StringValueSwig.class);
            when(value.hasValue()).thenReturn(true);
            when(value.getValue()).thenReturn("Example");
            when(results.getValueAsString(index.intValue())).thenReturn(value);
            IPIntelligenceDataHashDefault data =
                new IPIntelligenceDataHashDefault(
                    logger,
                    flowData,
                    engine,
                    MissingPropertyServiceDefault.getInstance());
            data.setResults(results);

            assertEquals("Example", data.getRegisteredName().getValue());

            verify(results).getValueAsString(index.intValue());
            verify(results, never()).containsProperty(any(String.class));
        }
    }
}
