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

package fiftyone.pipeline.jsonbuilder;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.data.PropertyMatcher;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * One JSON Builder in two Pipelines keeps the configuration it reads from
 * each Pipeline apart, and uses the right one for each. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/advanced-features/multi-pipeline-elements.md#L17">multi-pipeline-elements.md line 17</a>:
 * "If this feature is enabled, then all Flow Elements that maintain any
 * such state information MUST ensure that they have the means to keep
 * track of isolated instances of this state for each Pipeline to which
 * they are added and use the correct state information when processing."
 */
public class JsonBuilderMultiPipelineTests {

    /**
     * A Pipeline whose "test" element has a JavaScript property, delayed or
     * not.
     */
    private static Pipeline pipeline(boolean delayExecution) {
        FlowElement<?, ?> element = mock(FlowElement.class);
        doReturn("test").when(element).getElementDataKey();
        ElementPropertyMetaData property = new ElementPropertyMetaDataDefault(
            "jsproperty",
            element,
            "jsproperty",
            JavaScript.class,
            true,
            null,
            delayExecution,
            null);
        Map<String, Map<String, ElementPropertyMetaData>> metaData =
            new HashMap<>();
        metaData.put("test",
            Collections.singletonMap("jsproperty", property));
        Pipeline pipeline = mock(Pipeline.class);
        when(pipeline.getElementAvailableProperties()).thenReturn(metaData);
        return pipeline;
    }

    /**
     * Process Flow Data of the Pipeline with the element and return the
     * JSON of the "test" element.
     */
    private static JSONObject process(
        JsonBuilderElement element,
        Pipeline pipeline) throws Exception {
        ElementData data = mock(ElementData.class);
        Map<String, Object> values = new HashMap<>();
        values.put("jsproperty", "var x = 1;");
        doReturn(values).when(data).asKeyMap();
        Map<String, Object> elementData = new HashMap<>();
        elementData.put("test", data);

        FlowData flowData = mock(FlowData.class);
        when(flowData.elementDataAsMap()).thenReturn(elementData);
        TryGetResult<Object> session = new TryGetResult<>();
        session.setValue("session");
        TryGetResult<Integer> sequence = new TryGetResult<>();
        sequence.setValue(1);
        doReturn(session)
            .when(flowData).tryGetEvidence(eq("query.session-id"), any());
        doReturn(sequence)
            .when(flowData).tryGetEvidence(eq("query.sequence"), any());
        when(flowData.getWhere(any(PropertyMatcher.class)))
            .thenReturn(new HashMap<>());
        when(flowData.getPipeline()).thenReturn(pipeline);
        when(flowData.getErrors()).thenReturn(new ArrayList<FlowError>());
        final JsonBuilderData[] result = { null };
        when(flowData.getOrAdd(anyString(), any()))
            .thenAnswer((Answer<ElementData>) invocation -> {
                FlowElement.DataFactory<?> factory = invocation.getArgument(1);
                result[0] = (JsonBuilderData) factory.create(flowData);
                return result[0];
            });

        element.process(flowData);
        return new JSONObject(result[0].getJson()).getJSONObject("test");
    }

    @Test
    public void onePipelinesConfiguration_IsNotUsedForAnother()
        throws Exception {
        JsonBuilderElement element = new JsonBuilderElementBuilder(
            LoggerFactory.getILoggerFactory()).build();
        Pipeline delayed = pipeline(true);
        Pipeline notDelayed = pipeline(false);

        assertTrue(process(element, delayed).has("jspropertydelayexecution"),
            "The first Pipeline's property is delayed.");
        assertFalse(
            process(element, notDelayed).has("jspropertydelayexecution"),
            "The second Pipeline's configuration came from the first.");
        assertTrue(process(element, delayed).has("jspropertydelayexecution"),
            "The first Pipeline's configuration came from the second.");
    }
}
