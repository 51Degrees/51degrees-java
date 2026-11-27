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
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * When the Cloud Request Engine's request for the Flow Data failed, a
 * property of the IP Intelligence cloud engine is missing with the
 * CloudRequestFailed reason, rather than a reason about the resource key.
 */
public class CloudRequestFailedTests {

    private ILoggerFactory loggerFactory;

    private CloudRequestEngine requestEngine;

    @Before
    public void init() {
        Logger logger = mock(Logger.class);
        loggerFactory = mock(ILoggerFactory.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);
    }

    @Test
    public void RequestFailedWithNoResponse_CloudRequestFailed()
        throws Exception {
        assertCloudRequestFailed(null);
    }

    @Test
    public void RequestFailedWithAnErrorResponse_CloudRequestFailed()
        throws Exception {
        assertCloudRequestFailed("{ \"errors\": [ \"an error\" ] }");
    }

    private void assertCloudRequestFailed(String response) throws Exception {
        try (IPIntelligenceCloudEngine engine = buildEngine()) {
            IPIntelligenceDataCloud data = process(engine, response);
            try {
                data.getRegisteredName();
                fail("A PropertyMissingException should have been thrown");
            } catch (PropertyMissingException e) {
                assertEquals(
                    e.getMessage(),
                    "CloudRequestFailed",
                    e.getReason().name());
                assertEquals(
                    "Property 'registeredname' not found in data for " +
                        "element '" + engine.getElementDataKey() + "'. " +
                        "This is because an upstream cloud request failed, " +
                        "so no data was available to populate the property.",
                    e.getMessage());
            }
        }
    }

    /**
     * An engine whose Cloud Request Engine reports the IP Intelligence
     * product, holding a property other than the one read.
     */
    // thenReturn has a varargs overload, so a generic Map is an unchecked array.
    private IPIntelligenceCloudEngine buildEngine() throws Exception {
        IPIntelligenceCloudEngine engine =
            new IPIntelligenceCloudEngineBuilder(loggerFactory).build();

        ProductMetaData product = new ProductMetaData();
        product.dataTier = "test";
        product.properties = Arrays.asList(
            new PropertyMetaData("registeredowner", "String", "cat", null));
        Map<String, ProductMetaData> publicProperties = new HashMap<>();
        publicProperties.put(engine.getElementDataKey(), product);

        requestEngine = mock(CloudRequestEngine.class);
        when(requestEngine.getPublicProperties()).thenReturn(publicProperties);

        Pipeline pipeline = mock(Pipeline.class);
        when(pipeline.getElement(CloudRequestEngine.class))
            .thenReturn(requestEngine);

        engine.addPipeline(pipeline);
        return engine;
    }

    private IPIntelligenceDataCloud process(
        IPIntelligenceCloudEngine engine,
        String response) {
        CloudRequestData cloudData = mock(CloudRequestData.class);
        when(cloudData.getJsonResponse()).thenReturn(response);
        when(cloudData.getProcessStarted()).thenReturn(true);

        FlowData flowData = mock(FlowData.class);
        when(flowData.getFromElement(any(CloudRequestEngine.class)))
            .thenReturn(cloudData);
        when(flowData.getErrors()).thenReturn(
            Collections.<FlowError>singletonList(new FlowError.Default(
                new Exception("The cloud request failed."),
                requestEngine)));

        IPIntelligenceDataCloud data = new IPIntelligenceDataCloudInternal(
            loggerFactory.getLogger(
                IPIntelligenceDataCloud.class.getSimpleName()),
            flowData,
            engine,
            MissingPropertyServiceDefault.getInstance());
        when(flowData.getOrAdd(
            ArgumentMatchers.<TypedKey<IPIntelligenceDataCloud>>any(),
            ArgumentMatchers.<FlowElement.DataFactory<IPIntelligenceDataCloud>>any()))
            .thenReturn(data);
        try {
            engine.process(flowData);
        } catch (Exception e) {
            // A failed request's response may not be readable. The
            // property is read afterwards either way, as it is through a
            // Pipeline that suppresses process exceptions.
        }
        return data;
    }
}
