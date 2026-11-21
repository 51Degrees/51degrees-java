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

import fiftyone.devicedetection.cloud.data.MultiDeviceDataCloud;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.cloudrequestengine.flowelements.PropertyKeyedCloudEngineBase;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.ProductMetaData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.PropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The majority of the processing SHOULD be handled by the shared Cloud Aspect
 * Engine logic
 * (device-detection-specification/pipeline-elements/hardware-profile-lookup-cloud.md
 * line 56 at 706de1f0). The hardware profile engine gives no processing of
 * its own, and the shared logic for property keyed engines turns each
 * profile in its part of the response into DeviceData.
 */
public class HardwareProfileCloudEngineTests {

    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    private static HardwareProfileCloudEngine engine() throws Exception {
        HardwareProfileCloudEngine engine =
            new HardwareProfileCloudEngineBuilder(loggerFactory).build();
        ProductMetaData product = new ProductMetaData();
        product.dataTier = "test";
        product.properties = Arrays.asList(
            new PropertyMetaData("profiles", "Array", "Device", Arrays.asList(
                new PropertyMetaData("hardwarevendor", "String", "Device", null),
                new PropertyMetaData("hardwarename", "Array", "Device", null))));
        Map<String, ProductMetaData> products = new HashMap<>();
        products.put("hardware", product);
        CloudRequestEngine requestEngine = mock(CloudRequestEngine.class);
        when(requestEngine.getPublicProperties()).thenReturn(products);
        Pipeline pipeline = mock(Pipeline.class);
        when(pipeline.getElement(CloudRequestEngine.class))
            .thenReturn(requestEngine);
        engine.addPipeline(pipeline);
        return engine;
    }

    private static MultiDeviceDataCloud process(
        HardwareProfileCloudEngine engine,
        String response) throws Exception {
        CloudRequestData cloudData = mock(CloudRequestData.class);
        when(cloudData.getJsonResponse()).thenReturn(response);
        when(cloudData.getProcessStarted()).thenReturn(true);
        FlowData flowData = mock(FlowData.class);
        when(flowData.getFromElement(any(CloudRequestEngine.class)))
            .thenReturn(cloudData);
        MultiDeviceDataCloud data = new MultiDeviceDataCloud(
            loggerFactory.getLogger(MultiDeviceDataCloud.class.getSimpleName()),
            flowData,
            engine,
            MissingPropertyServiceDefault.getInstance());
        // The Pipeline calls the protected processEngine of the shared base.
        Method processEngine = null;
        for (Class<?> type = engine.getClass(); processEngine == null &&
            type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals("processEngine") &&
                    method.getParameterCount() == 2) {
                    processEngine = method;
                }
            }
        }
        processEngine.setAccessible(true);
        try {
            processEngine.invoke(engine, flowData, data);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
        return data;
    }

    /**
     * The engine gives no processing of its own.
     */
    @Test
    public void HardwareProfile_ProcessingIsShared() {
        assertTrue(PropertyKeyedCloudEngineBase.class.isAssignableFrom(
            HardwareProfileCloudEngine.class));
        for (Method method :
            HardwareProfileCloudEngine.class.getDeclaredMethods()) {
            assertTrue("The engine processes with " + method.getName(),
                method.getName().equals("processEngine") == false &&
                method.getName().equals("processCloudEngine") == false);
        }
    }

    /**
     * Each profile in the engine's part of the response becomes DeviceData
     * with the values the response gives.
     */
    @Test
    public void HardwareProfile_ProfilesAreDeviceData() throws Exception {
        try (HardwareProfileCloudEngine engine = engine()) {
            MultiDeviceDataCloud data = process(engine,
                "{ 'hardware': { 'profiles': [" +
                    "{ 'hardwarevendor': 'Apple', " +
                    "'hardwarename': ['iPhone 12', 'iPhone 12 Pro'] }, " +
                    "{ 'hardwarevendor': null, " +
                    "'hardwarevendornullreason': 'No match', " +
                    "'hardwarename': [] } ] } }");
            List<DeviceData> profiles = data.getProfiles();
            assertEquals(2, profiles.size());
            assertEquals("Apple",
                profiles.get(0).getHardwareVendor().getValue());
            assertEquals(Arrays.asList("iPhone 12", "iPhone 12 Pro"),
                profiles.get(0).getHardwareName().getValue());
            assertEquals(false,
                profiles.get(1).getHardwareVendor().hasValue());
            assertEquals("No match",
                profiles.get(1).getHardwareVendor().getNoValueMessage());
        }
    }

    /**
     * An empty response, after an error the Cloud Request Engine has
     * reported, gives no profiles and raises no error.
     */
    @Test
    public void HardwareProfile_EmptyResponse_NoProfiles() throws Exception {
        try (HardwareProfileCloudEngine engine = engine()) {
            assertTrue(process(engine, "").getProfiles().isEmpty());
        }
    }
}
