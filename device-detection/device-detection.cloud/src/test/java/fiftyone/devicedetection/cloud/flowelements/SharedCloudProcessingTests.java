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
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudAspectEngineBase;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The majority of the processing SHOULD be handled by the shared Cloud Aspect
 * Engine logic
 * (device-detection-specification/pipeline-elements/device-detection-cloud.md
 * line 41 at 706de1f0). The engine only filters and parses its part of the
 * JSON, and the shared logic does the rest: it finds the Cloud Request
 * Engine's data, refuses a Pipeline where that engine has not run, and
 * passes over an empty response, whose error the Cloud Request Engine has
 * already reported.
 */
public class SharedCloudProcessingTests {

    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    private static DeviceDetectionCloudEngine engine() throws Exception {
        DeviceDetectionCloudEngine engine =
            new DeviceDetectionCloudEngineBuilder(loggerFactory).build();
        CloudRequestEngine requestEngine = mock(CloudRequestEngine.class);
        when(requestEngine.getPublicProperties()).thenReturn(new HashMap<>());
        Pipeline pipeline = mock(Pipeline.class);
        when(pipeline.getElement(CloudRequestEngine.class))
            .thenReturn(requestEngine);
        engine.addPipeline(pipeline);
        return engine;
    }

    private static DeviceDataCloud process(
        DeviceDetectionCloudEngine engine,
        String response,
        boolean processStarted) {
        CloudRequestData cloudData = mock(CloudRequestData.class);
        when(cloudData.getJsonResponse()).thenReturn(response);
        when(cloudData.getProcessStarted()).thenReturn(processStarted);
        FlowData flowData = mock(FlowData.class);
        when(flowData.getFromElement(any(CloudRequestEngine.class)))
            .thenReturn(cloudData);
        DeviceDataCloud device = new DeviceDataCloudInternal(
            loggerFactory.getLogger(DeviceDataCloud.class.getSimpleName()),
            flowData,
            engine,
            MissingPropertyServiceDefault.getInstance());
        processThroughTheEngine(engine, flowData, device);
        return device;
    }

    /**
     * Run the engine's processing, as the Pipeline does, through the
     * processEngine method it has, whichever class declares it.
     */
    static void processThroughTheEngine(
        DeviceDetectionCloudEngine engine,
        FlowData flowData,
        DeviceDataCloud device) {
        Class<?> type = engine.getClass();
        while (type != null) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals("processEngine") &&
                    method.getParameterCount() == 2) {
                    method.setAccessible(true);
                    try {
                        method.invoke(engine, flowData, device);
                    } catch (InvocationTargetException e) {
                        if (e.getCause() instanceof RuntimeException) {
                            throw (RuntimeException) e.getCause();
                        }
                        throw new IllegalStateException(e.getCause());
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                    return;
                }
            }
            type = type.getSuperclass();
        }
        throw new IllegalStateException("No processEngine method");
    }

    /**
     * The engine leaves processing to the shared logic, and gives only the
     * parsing of its part of the response.
     */
    @Test
    public void Process_IsTheSharedLogic() throws Exception {
        try {
            DeviceDetectionCloudEngine.class.getDeclaredMethod(
                "processEngine", FlowData.class, DeviceDataCloud.class);
            fail("The engine replaces the shared processing");
        } catch (NoSuchMethodException e) {
            // The shared processing is used.
        }
        assertTrue(CloudAspectEngineBase.class.isAssignableFrom(
            DeviceDetectionCloudEngine.class));
        DeviceDetectionCloudEngine.class.getDeclaredMethod(
            "processCloudEngine",
            FlowData.class, DeviceDataCloud.class, String.class);
    }

    /**
     * An empty response, after an error the Cloud Request Engine has
     * reported, leaves the data without values and raises no error.
     */
    @Test
    public void Process_EmptyResponse_NoError() throws Exception {
        try (DeviceDetectionCloudEngine engine = engine()) {
            DeviceDataCloud device = process(engine, "", true);
            assertTrue(device.asKeyMap().isEmpty());
        }
    }

    /**
     * A Pipeline where the Cloud Request Engine has not run is a
     * configuration error.
     */
    @Test(expected = PipelineConfigurationException.class)
    public void Process_RequestEngineNotRun_ConfigurationError()
        throws Exception {
        try (DeviceDetectionCloudEngine engine = engine()) {
            process(engine, "{ 'device': {} }", false);
        }
    }
}
