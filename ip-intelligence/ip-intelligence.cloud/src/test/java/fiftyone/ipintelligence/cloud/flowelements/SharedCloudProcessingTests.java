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
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.ProductMetaData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.PropertyMetaData;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The cloud IP Intelligence engine leaves its processing to the shared Cloud
 * Aspect Engine logic, which finds the Cloud Request Engine's data, checks
 * the request ran, and passes on only a response that is there, so the
 * engine itself only reads its part of the response. The engine is driven
 * through its public process method with a fixed response, so the tests
 * need no resource key.
 */
public class SharedCloudProcessingTests {

    private final Logger logger = mock(Logger.class);

    private final CloudRequestData cloudData = mock(CloudRequestData.class);

    private final FlowData flowData = mock(FlowData.class);

    private IPIntelligenceCloudEngine engine;

    private IPIntelligenceDataCloud data;

    @Before
    public void init() throws Exception {
        ILoggerFactory loggerFactory = mock(ILoggerFactory.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);
        engine = new IPIntelligenceCloudEngineBuilder(loggerFactory).build();

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

        data = new IPIntelligenceDataCloudInternal(
            logger,
            flowData,
            engine,
            MissingPropertyServiceDefault.getInstance());
        when(flowData.getFromElement(any(CloudRequestEngine.class)))
            .thenReturn(cloudData);
        when(flowData.getOrAdd(ArgumentMatchers.<TypedKey<ElementData>>any(), any()))
            .thenReturn(data);
    }

    @After
    public void cleanup() throws Exception {
        engine.close();
    }

    @Test
    public void Engine_DeclaresOnlyTheResponseReading() throws Exception {
        for (Method method :
            IPIntelligenceCloudEngine.class.getDeclaredMethods()) {
            if (method.getName().equals("processEngine") &&
                method.isSynthetic() == false) {
                fail("The engine should leave processEngine to the shared " +
                    "Cloud Aspect Engine logic.");
            }
        }
        IPIntelligenceCloudEngine.class.getDeclaredMethod(
            "processCloudEngine",
            FlowData.class,
            IPIntelligenceDataCloud.class,
            String.class);
    }

    @Test
    public void Response_Read() throws Exception {
        when(cloudData.getProcessStarted()).thenReturn(true);
        when(cloudData.getJsonResponse()).thenReturn(
            "{ \"ip\": { \"registeredname\": \"Example\" } }");
        engine.process(flowData);
        assertEquals("Example", data.getRegisteredName().getValue());
    }

    @Test
    public void NoResponse_WarnsAndStops() throws Exception {
        when(cloudData.getProcessStarted()).thenReturn(true);
        when(cloudData.getJsonResponse()).thenReturn(null);
        engine.process(flowData);
        verify(logger).warn(contains("did not process"));
        assertTrue(data.asKeyMap().isEmpty());
    }

    @Test(expected = PipelineConfigurationException.class)
    public void RequestNotRun_Throws() throws Exception {
        when(cloudData.getProcessStarted()).thenReturn(false);
        when(cloudData.getJsonResponse()).thenReturn(
            "{ \"ip\": { \"registeredname\": \"Example\" } }");
        engine.process(flowData);
    }
}
