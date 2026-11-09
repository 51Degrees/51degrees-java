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

package fiftyone.devicedetection.cloud.flowelements;

import fiftyone.devicedetection.cloud.data.DeviceDataCloud;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.ProductMetaData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.PropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.services.MissingPropertyReason;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A resource key can be valid and include no product that holds the
 * Device Detection properties. The engine then builds with empty property
 * metadata, logs the missing product once, and logs or throws nothing on
 * each request.
 */
public class ProductMissingTests {

    private Logger logger;
    private ILoggerFactory loggerFactory;

    @Before
    public void init() {
        logger = mock(Logger.class);
        loggerFactory = mock(ILoggerFactory.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);
    }

    @Test
    public void ProductMissing_BuildsWithEmptyMetadata() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            assertNotNull(engine.getProperties());
            assertEquals(0, engine.getProperties().size());
            List<String> logged = warningsAndErrors();
            assertEquals(logged.toString(), 1, logged.size());
            assertTrue(logged.get(0), logged.get(0).contains("'device'"));
        }
    }

    @Test
    public void ProductMissing_ProcessLogsAndThrowsNothing() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            for (int i = 0; i < 3; i++) {
                process(engine);
            }
            List<String> logged = warningsAndErrors();
            assertEquals(logged.toString(), 1, logged.size());
        }
    }

    /**
     * Reading a property behaves as for any property the response does not
     * carry.
     */
    @Test
    public void ProductMissing_PropertyIsMissing() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            DeviceDataCloud device = process(engine);
            try {
                device.getHardwareVendor();
                fail("A PropertyMissingException should have been thrown");
            } catch (PropertyMissingException e) {
                assertEquals(
                    MissingPropertyReason.ProductNotAccessibleWithResourceKey,
                    e.getReason());
            }
        }
    }

    /**
     * An engine whose Cloud Request Engine reports the accessible products
     * of a resource key that includes IP Intelligence alone.
     */
    // thenReturn has a varargs overload, so a generic Map is an unchecked array.
    @SuppressWarnings("unchecked")
    private DeviceDetectionCloudEngine buildEngine() throws Exception {
        DeviceDetectionCloudEngine engine =
            new DeviceDetectionCloudEngineBuilder(loggerFactory).build();

        ProductMetaData product = new ProductMetaData();
        product.dataTier = "test";
        product.properties = Arrays.asList(
            new PropertyMetaData("registeredname", "String", "cat", null));
        Map<String, ProductMetaData> publicProperties = new HashMap<>();
        publicProperties.put("ip", product);

        CloudRequestEngine requestEngine = mock(CloudRequestEngine.class);
        when(requestEngine.getPublicProperties()).thenReturn(publicProperties);

        Pipeline pipeline = mock(Pipeline.class);
        when(pipeline.getElement(CloudRequestEngine.class))
            .thenReturn(requestEngine);

        engine.addPipeline(pipeline);
        return engine;
    }

    /**
     * Process a cloud response that holds nothing for the Device Detection
     * product.
     */
    private DeviceDataCloud process(DeviceDetectionCloudEngine engine) {
        CloudRequestData cloudData = mock(CloudRequestData.class);
        when(cloudData.getJsonResponse()).thenReturn(
            "{ 'ip': { 'registeredname': 'Example' }, " +
                "'javascriptProperties': [] }");

        FlowData flowData = mock(FlowData.class);
        when(flowData.getFromElement(any(CloudRequestEngine.class)))
            .thenReturn(cloudData);

        DeviceDataCloud device = new DeviceDataCloudInternal(
            loggerFactory.getLogger(DeviceDataCloud.class.getSimpleName()),
            flowData,
            engine,
            MissingPropertyServiceDefault.getInstance());
        engine.processEngine(flowData, device);
        return device;
    }

    /**
     * Every warning and error logged, at any overload.
     */
    private List<String> warningsAndErrors() {
        List<String> logged = new ArrayList<>();
        for (Invocation invocation :
            Mockito.mockingDetails(logger).getInvocations()) {
            String level = invocation.getMethod().getName();
            if (level.equals("warn") || level.equals("error")) {
                logged.add(level + " " +
                    Arrays.toString(invocation.getArguments()));
            }
        }
        return Collections.unmodifiableList(logged);
    }
}
