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
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * With no property metadata, as before the Cloud Request Engine's discovery
 * has completed, the engine still gives the values the response holds,
 * each with the type its JSON value has.
 */
public class EmptyMetadataTests {

    private static final String RESPONSE =
        "{ 'device': { " +
            "'hardwarevendor': 'Apple', " +
            "'ismobile': true, " +
            "'screenpixelswidth': 1080, " +
            "'screeninchesdiagonal': 6.1, " +
            "'hardwarename': ['iPhone 14', 'iPhone 14 Plus'], " +
            "'devicetype': null, " +
            "'devicetypenullreason': 'No match', " +
            "'platformname': null }, " +
            "'javascriptProperties': [] }";

    private Logger logger;
    private ILoggerFactory loggerFactory;

    @Before
    public void init() {
        logger = mock(Logger.class);
        loggerFactory = mock(ILoggerFactory.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);
    }

    @Test
    public void EmptyMetadata_ValuesKeptWithTheirJsonTypes() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildWithEmptyMetadata()) {
            assertEquals(0, engine.getProperties().size());
            DeviceDataCloud device = process(engine);

            assertEquals("Apple", device.getHardwareVendor().getValue());
            assertEquals(Boolean.TRUE, device.getIsMobile().getValue());
            assertEquals(Integer.valueOf(1080),
                device.getScreenPixelsWidth().getValue());
            assertEquals(Double.valueOf(6.1),
                device.getScreenInchesDiagonal().getValue());
            assertEquals(Arrays.asList("iPhone 14", "iPhone 14 Plus"),
                device.getHardwareName().getValue());
        }
    }

    @Test
    public void EmptyMetadata_NullsHaveTheirReason() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildWithEmptyMetadata()) {
            DeviceDataCloud device = process(engine);

            assertFalse(device.getDeviceType().hasValue());
            assertEquals("No match",
                device.getDeviceType().getNoValueMessage());
            assertFalse(device.getPlatformName().hasValue());
            assertEquals("Unknown",
                device.getPlatformName().getNoValueMessage());
        }
    }

    @Test
    public void EmptyMetadata_NothingLoggedWhenProcessing() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildWithEmptyMetadata()) {
            int before = warningsAndErrors();
            for (int i = 0; i < 3; i++) {
                process(engine);
            }
            assertEquals(before, warningsAndErrors());
        }
    }

    // thenReturn has a varargs overload, so a generic Map is an unchecked
    // array.
    @SuppressWarnings("unchecked")
    private DeviceDetectionCloudEngine buildWithEmptyMetadata()
        throws Exception {
        DeviceDetectionCloudEngine engine =
            new DeviceDetectionCloudEngineBuilder(loggerFactory).build();
        // The key gives another product, so there is no metadata for
        // this engine's properties.
        ProductMetaData product = new ProductMetaData();
        product.dataTier = "test";
        product.properties = Collections.singletonList(
            new PropertyMetaData("registeredname", "String", "cat", null));
        Map<String, ProductMetaData> publicProperties =
            Collections.singletonMap("ip", product);
        CloudRequestEngine requestEngine = mock(CloudRequestEngine.class);
        when(requestEngine.getPublicProperties()).thenReturn(publicProperties);
        Pipeline pipeline = mock(Pipeline.class);
        when(pipeline.getElement(CloudRequestEngine.class))
            .thenReturn(requestEngine);
        engine.addPipeline(pipeline);
        return engine;
    }

    private DeviceDataCloud process(DeviceDetectionCloudEngine engine) {
        CloudRequestData cloudData = mock(CloudRequestData.class);
        when(cloudData.getJsonResponse()).thenReturn(RESPONSE);
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

    private int warningsAndErrors() {
        int count = 0;
        for (Invocation invocation :
            Mockito.mockingDetails(logger).getInvocations()) {
            String level = invocation.getMethod().getName();
            if (level.equals("warn") || level.equals("error")) {
                count++;
            }
        }
        return count;
    }
}
