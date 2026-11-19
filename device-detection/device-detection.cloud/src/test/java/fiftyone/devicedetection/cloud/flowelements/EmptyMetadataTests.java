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
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Before the Cloud Request Engine's discovery has completed, the engine has
 * no property metadata. It still reads the values in the response, taking
 * the type of each from the value itself.
 */
public class EmptyMetadataTests {

    private static final String RESPONSE =
        "{ 'device': { " +
            "'hardwarevendor': 'Apple', " +
            "'ismobile': true, " +
            "'screenpixelswidth': 1170, " +
            "'pixelratio': 3.5, " +
            "'hardwarename': [ 'iPhone 13', 'iPhone 14' ], " +
            "'hardwaremodel': null, " +
            "'platformname': null, " +
            "'platformnamenullreason': 'No value on this request' }, " +
        "'javascriptProperties': [] }";

    private ILoggerFactory loggerFactory;

    @Before
    public void init() {
        Logger logger = mock(Logger.class);
        loggerFactory = mock(ILoggerFactory.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);
    }

    @Test
    public void EmptyMetadata_MetadataIsAnEmptyList() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            assertTrue(engine.getProperties().isEmpty());
            assertFalse(engine.hasLoadedProperties());
        }
    }

    @Test
    public void EmptyMetadata_StringIsRead() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            DeviceDataCloud data = process(engine);
            assertEquals("Apple", data.getHardwareVendor().getValue());
        }
    }

    @Test
    public void EmptyMetadata_BooleanIsRead() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            DeviceDataCloud data = process(engine);
            assertEquals(Boolean.TRUE, data.getIsMobile().getValue());
        }
    }

    @Test
    public void EmptyMetadata_IntegerIsRead() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            DeviceDataCloud data = process(engine);
            assertEquals(
                Integer.valueOf(1170),
                data.getScreenPixelsWidth().getValue());
        }
    }

    @Test
    public void EmptyMetadata_DoubleIsRead() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            DeviceDataCloud data = process(engine);
            assertEquals(
                Double.valueOf(3.5),
                data.getPixelRatio().getValue());
        }
    }

    /**
     * A whole number for a property whose typed accessor gives a Double is
     * read as a Double, as the cloud service can give 3 for a pixel ratio
     * of 3.0.
     */
    @Test
    public void EmptyMetadata_WholeNumberDeclaredDouble_IsDouble()
        throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            DeviceDataCloud data = process(engine,
                "{ 'device': { 'pixelratio': 3 }, " +
                    "'javascriptProperties': [] }");
            Object value = data.getPixelRatio().getValue();
            assertEquals(Double.class, value.getClass());
            assertEquals(3.0, (Double) value, 0);
        }
    }

    /**
     * A decimal with no fractional part for a property whose typed
     * accessor gives an Integer is read as an Integer, so the accessor
     * works if the cloud service gives 24.0 for a bits per pixel of 24.
     */
    @Test
    public void EmptyMetadata_WholeDecimalDeclaredInteger_IsInteger()
        throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            DeviceDataCloud data = process(engine,
                "{ 'device': { 'bitsperpixel': 24.0 }, " +
                    "'javascriptProperties': [] }");
            Object value = data.getBitsPerPixel().getValue();
            assertEquals(Integer.class, value.getClass());
            assertEquals(24, ((Integer) value).intValue());
        }
    }

    /**
     * A decimal with a fractional part for a property whose typed accessor
     * gives an Integer is not made a whole number, because that would
     * change the value the service gave.
     */
    @Test
    public void EmptyMetadata_FractionDeclaredInteger_IsNotRounded()
        throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            DeviceDataCloud data = process(engine,
                "{ 'device': { 'bitsperpixel': 24.5 }, " +
                    "'javascriptProperties': [] }");
            Object value = data.get("bitsperpixel");
            assertTrue("The value is not an AspectPropertyValue.",
                value instanceof AspectPropertyValue);
            assertEquals(24.5,
                ((Number) ((AspectPropertyValue<?>) value).getValue())
                    .doubleValue(), 0);
        }
    }

    @Test
    public void EmptyMetadata_ListIsRead() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            DeviceDataCloud data = process(engine);
            assertEquals(
                Arrays.asList("iPhone 13", "iPhone 14"),
                data.getHardwareName().getValue());
        }
    }

    @Test
    public void EmptyMetadata_NullWithReason_HasThatReason() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            DeviceDataCloud data = process(engine);
            assertFalse(data.getPlatformName().hasValue());
            assertEquals(
                "No value on this request",
                data.getPlatformName().getNoValueMessage());
        }
    }

    @Test
    public void EmptyMetadata_NullWithoutReason_IsUnknown() throws Exception {
        try (DeviceDetectionCloudEngine engine = buildEngine()) {
            DeviceDataCloud data = process(engine);
            assertFalse(data.getHardwareModel().hasValue());
            assertEquals(
                "Unknown",
                data.getHardwareModel().getNoValueMessage());
        }
    }

    /**
     * An engine whose Cloud Request Engine does not yet know the products,
     * so the engine has no property metadata.
     */
    private DeviceDetectionCloudEngine buildEngine() throws Exception {
        CloudRequestEngine requestEngine = mock(CloudRequestEngine.class);
        when(requestEngine.getPublicProperties()).thenReturn(null);
        Pipeline pipeline = mock(Pipeline.class);
        when(pipeline.getElement(CloudRequestEngine.class))
            .thenReturn(requestEngine);

        DeviceDetectionCloudEngine engine =
            new DeviceDetectionCloudEngineBuilder(loggerFactory).build();
        engine.addPipeline(pipeline);
        return engine;
    }

    private DeviceDataCloud process(DeviceDetectionCloudEngine engine) {
        return process(engine, RESPONSE);
    }

    private DeviceDataCloud process(
        DeviceDetectionCloudEngine engine,
        String response) {
        CloudRequestData cloudData = mock(CloudRequestData.class);
        when(cloudData.getJsonResponse()).thenReturn(response);

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
}
