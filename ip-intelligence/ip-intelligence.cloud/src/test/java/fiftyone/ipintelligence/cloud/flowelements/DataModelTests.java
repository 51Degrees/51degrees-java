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

package fiftyone.ipintelligence.cloud.flowelements;

import fiftyone.ipintelligence.cloud.data.IPIntelligenceDataCloud;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.ProductMetaData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.PropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The cloud IP Intelligence engine populates Aspect Data, which is the IP
 * Intelligence Element Data type. The engine processes a fixed response,
 * so the test needs no resource key.
 */
public class DataModelTests {

    @Test
    @SuppressWarnings("unchecked")
    public void Engine_PopulatesAspectData() throws Exception {
        Logger logger = mock(Logger.class);
        ILoggerFactory loggerFactory = mock(ILoggerFactory.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);

        try (IPIntelligenceCloudEngine engine =
                 new IPIntelligenceCloudEngineBuilder(loggerFactory).build()) {
            ProductMetaData product = new ProductMetaData();
            product.dataTier = "test";
            product.properties = Arrays.asList(
                new PropertyMetaData("registeredname", "String", "cat", null));
            Map<String, ProductMetaData> products = new HashMap<>();
            products.put(engine.getElementDataKey(), product);
            CloudRequestEngine requestEngine = mock(CloudRequestEngine.class);
            when(requestEngine.getPublicProperties()).thenReturn(products);
            Pipeline pipeline = mock(Pipeline.class);
            when(pipeline.getElement(CloudRequestEngine.class))
                .thenReturn(requestEngine);
            engine.addPipeline(pipeline);

            CloudRequestData cloudData = mock(CloudRequestData.class);
            when(cloudData.getJsonResponse()).thenReturn(
                "{ \"ip\": { \"registeredname\": \"Example\" } }");
            when(cloudData.getProcessStarted()).thenReturn(true);
            FlowData flowData = mock(FlowData.class);
            when(flowData.getFromElement(any(CloudRequestEngine.class)))
                .thenReturn(cloudData);

            IPIntelligenceDataCloud data = new IPIntelligenceDataCloudInternal(
                logger,
                flowData,
                engine,
                MissingPropertyServiceDefault.getInstance());
            when(flowData.getOrAdd(any(TypedKey.class), any()))
                .thenReturn(data);
            engine.process(flowData);

            assertTrue(data instanceof AspectData);
            assertTrue(data instanceof IPIntelligenceData);
        }
    }
}
