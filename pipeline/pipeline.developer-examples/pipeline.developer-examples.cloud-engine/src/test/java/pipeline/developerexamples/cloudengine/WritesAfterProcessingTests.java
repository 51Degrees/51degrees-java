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

package pipeline.developerexamples.cloudengine;

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.flowelements.FlowElement.DataFactory;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.engines.data.AspectDataBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import pipeline.developerexamples.cloudengine.flowelements.SimpleCloudEngine;
import pipeline.developerexamples.cloudengine.flowelements.SimpleCloudEngineBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The check, for the Flow Element in this module, the
 * {@link SimpleCloudEngine}, that processes it, then writes to its Element
 * Data and expects the error. It is processed through its public process
 * method with a fixed cloud response, so the check needs no resource key.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Specification</a>
 */
public class WritesAfterProcessingTests {

    private static final ILoggerFactory LOGGER_FACTORY =
        LoggerFactory.getILoggerFactory();

    /**
     * Every write method of Element Data, and of Aspect Data, fails once its
     * Flow Element has finished.
     * @param data the Element Data of a finished Flow Element
     */
    static void assertWritesFail(ElementData data) {
        assertThrows(
            IllegalStateException.class,
            () -> data.put("after", "value"),
            "put");
        assertThrows(
            IllegalStateException.class,
            () -> data.populateFromMap(
                Collections.<String, Object>singletonMap("after", "value")),
            "populateFromMap");
        if (data instanceof AspectDataBase) {
            AspectDataBase aspect = (AspectDataBase) data;
            assertThrows(
                IllegalStateException.class,
                aspect::setCloudRequestFailed,
                "setCloudRequestFailed");
            assertThrows(
                IllegalStateException.class,
                () -> aspect.addEngine(null),
                "addEngine");
            assertThrows(
                IllegalStateException.class,
                () -> aspect.addProcessCallable(null),
                "addProcessCallable");
        }
    }

    @Test
    public void simpleCloudEngine_ElementDataRefusesWrites() throws Exception {
        CloudRequestEngine requestEngine = mock(CloudRequestEngine.class);
        when(requestEngine.getPublicProperties())
            .thenReturn(Collections.emptyMap());
        Pipeline pipeline = mock(Pipeline.class);
        when(pipeline.getElement(CloudRequestEngine.class))
            .thenReturn(requestEngine);
        CloudRequestData cloudData = mock(CloudRequestData.class);
        when(cloudData.getProcessStarted()).thenReturn(true);
        when(cloudData.getJsonResponse()).thenReturn(
            "{ \"starsign\": { \"starsign\": \"Sagittarius\" } }");
        final FlowData flowData = mock(FlowData.class);
        final List<ElementData> added = new ArrayList<>();
        when(flowData.getFromElement(any(CloudRequestEngine.class)))
            .thenReturn(cloudData);
        when(flowData.getOrAdd(
            ArgumentMatchers.<TypedKey<ElementData>>any(),
            any()))
            .thenAnswer(invocation -> {
                if (added.isEmpty()) {
                    DataFactory<?> factory = invocation.getArgument(1);
                    added.add(factory.create(flowData));
                }
                return added.get(0);
            });
        when(flowData.tryGetValue(ArgumentMatchers.<TypedKey<ElementData>>any()))
            .thenAnswer(invocation -> {
                TryGetResult<ElementData> result = new TryGetResult<>();
                if (added.isEmpty() == false) {
                    result.setValue(added.get(0));
                }
                return result;
            });

        try (SimpleCloudEngine engine =
                new SimpleCloudEngineBuilder(LOGGER_FACTORY).build()) {
            engine.addPipeline(pipeline);
            engine.process(flowData);

            assertEquals(1, added.size());
            assertWritesFail(added.get(0));
        }
    }
}
