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
import fiftyone.devicedetection.cloud.data.MultiDeviceDataCloud;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudAspectEngineBase;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.ProductMetaData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.PropertyMetaData;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.FlowElement.DataFactory;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.engines.data.AspectDataBase;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The check, for the Flow Elements in this module, that processes each one,
 * then writes to its Element Data and expects the error: the
 * {@link DeviceDetectionCloudEngine} and the
 * {@link HardwareProfileCloudEngine}, whose profiles are written only while
 * it processes as well. Each is processed through its public process method
 * with a fixed cloud response, so the check needs no resource key.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Specification</a>
 */
public class WritesAfterProcessingTests {

    private static final ILoggerFactory LOGGER_FACTORY =
        LoggerFactory.getILoggerFactory();

    private static void assertFails(String what, ThrowingRunnable write) {
        assertThrows(what, IllegalStateException.class, write);
    }

    /**
     * Every write method of Element Data, and of Aspect Data, fails once its
     * Flow Element has finished.
     * @param data the Element Data of a finished Flow Element
     */
    static void assertWritesFail(ElementData data) {
        assertFails("put", () -> data.put("after", "value"));
        assertFails("populateFromMap", () -> data.populateFromMap(
            Collections.<String, Object>singletonMap("after", "value")));
        if (data instanceof AspectDataBase) {
            AspectDataBase aspect = (AspectDataBase) data;
            assertFails("setCloudRequestFailed", aspect::setCloudRequestFailed);
            assertFails("addEngine", () -> aspect.addEngine(null));
            assertFails("addProcessCallable",
                () -> aspect.addProcessCallable(null));
        }
    }

    /**
     * Attach the engine to a Pipeline whose Cloud Request Engine reports the
     * product given.
     */
    private static <T extends CloudAspectEngineBase<?>> T attach(
        T engine,
        String productKey,
        PropertyMetaData property) {
        ProductMetaData product = new ProductMetaData();
        product.dataTier = "test";
        product.properties = Collections.singletonList(property);
        Map<String, ProductMetaData> products = new HashMap<>();
        products.put(productKey, product);
        CloudRequestEngine requestEngine = mock(CloudRequestEngine.class);
        when(requestEngine.getPublicProperties()).thenReturn(products);
        Pipeline pipeline = mock(Pipeline.class);
        when(pipeline.getElement(CloudRequestEngine.class))
            .thenReturn(requestEngine);
        engine.addPipeline(pipeline);
        return engine;
    }

    /**
     * Process the engine through its public process method, with a Flow
     * Data stand-in that holds the Element Data the engine adds and gives
     * the Cloud Request Engine's data with the response given.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends ElementData> T process(
        FlowElement<T, ?> engine,
        String response) throws Exception {
        CloudRequestData cloudData = mock(CloudRequestData.class);
        when(cloudData.getProcessStarted()).thenReturn(true);
        when(cloudData.getJsonResponse()).thenReturn(response);
        final FlowData flowData = mock(FlowData.class);
        final List<ElementData> added = new ArrayList<>();
        when(flowData.getFromElement(any(CloudRequestEngine.class)))
            .thenReturn(cloudData);
        when(flowData.getOrAdd(any(TypedKey.class), any(DataFactory.class)))
            .thenAnswer(invocation -> {
                if (added.isEmpty()) {
                    DataFactory factory = invocation.getArgument(1);
                    added.add(factory.create(flowData));
                }
                return added.get(0);
            });
        when(flowData.tryGetValue(any(TypedKey.class)))
            .thenAnswer(invocation -> {
                TryGetResult<ElementData> result = new TryGetResult<>();
                if (added.isEmpty() == false) {
                    result.setValue(added.get(0));
                }
                return result;
            });
        engine.process(flowData);
        assertEquals(1, added.size());
        return (T) added.get(0);
    }

    @Test
    public void deviceDetectionCloudEngine_ElementDataRefusesWrites()
        throws Exception {
        try (DeviceDetectionCloudEngine engine = attach(
            new DeviceDetectionCloudEngineBuilder(LOGGER_FACTORY).build(),
            "device",
            new PropertyMetaData("hardwarevendor", "String", "cat", null))) {
            DeviceDataCloud own = process(
                engine,
                "{ \"device\": { \"hardwarevendor\": \"Example\" } }");
            assertEquals("Example", own.getHardwareVendor().getValue());
            assertWritesFail(own);
        }
    }

    @Test
    public void hardwareProfileCloudEngine_ElementDataRefusesWrites()
        throws Exception {
        try (HardwareProfileCloudEngine engine = attach(
            new HardwareProfileCloudEngineBuilder(LOGGER_FACTORY).build(),
            "hardware",
            new PropertyMetaData("profiles", "Array", "cat",
                Collections.singletonList(new PropertyMetaData(
                    "hardwarevendor", "String", "cat", null))))) {
            MultiDeviceDataCloud own = process(
                engine,
                "{ \"hardware\": { \"profiles\": [ " +
                    "{ \"hardwarevendor\": \"Example\" } ] } }");
            assertEquals(1, own.getProfiles().size());
            assertWritesFail(own);
            assertFails("addProfile", () -> own.addProfile(null));
            assertWritesFail(own.getProfiles().get(0));
        }
    }
}
