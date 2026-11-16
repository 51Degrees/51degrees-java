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

package fiftyone.pipeline.javascriptbuilder;

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.Evidence;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.engines.fiftyone.data.SequenceData;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElement;
import fiftyone.pipeline.javascriptbuilder.data.JavaScriptBuilderData;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static fiftyone.pipeline.core.Constants.EVIDENCE_PROTOCOL;
import static fiftyone.pipeline.javascriptbuilder.Constants.EVIDENCE_HOST_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The script takes its promise form when the Promise Property is "Full",
 * however the text "Full" was made. A value read from a data file is a
 * String made at run time, not the literal in the code that compares it.
 */
public class JavaScriptBuilderPromiseTests {

    private JavaScriptBuilderData result;

    /**
     * Renders the script for a request whose Promise Property is the value
     * given.
     */
    @SuppressWarnings("unchecked")
    private String scriptWithPromise(String promise) throws Exception {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString()))
            .thenReturn(mock(Logger.class));
        TestLoggerFactory loggerFactory = new TestLoggerFactory(internalLogger);
        FlowData flowData = mock(FlowData.class);

        JSONObject json = new JSONObject()
            .put("device", new JSONObject().put("ismobile", true));
        doAnswer((Answer<Object>) invocation -> {
            JsonBuilderData jsonData = mock(JsonBuilderData.class);
            when(jsonData.getJson()).thenReturn(json.toString(0));
            return jsonData;
        }).when(flowData).get(JsonBuilderData.class);

        Map<String, Object> evidence = new HashMap<>();
        evidence.put(EVIDENCE_HOST_KEY, "localhost");
        evidence.put(EVIDENCE_PROTOCOL, "https");
        Evidence evidenceObject = mock(Evidence.class);
        when(evidenceObject.asKeyMap()).thenReturn(evidence);
        when(flowData.getEvidence()).thenReturn(evidenceObject);
        when(flowData.tryGetEvidence(anyString(), any(Class.class)))
            .thenAnswer(invocation -> {
                TryGetResult<Object> found = new TryGetResult<>();
                if (evidence.containsKey((String) invocation.getArgument(0))) {
                    found.setValue(evidence.get(
                        (String) invocation.getArgument(0)));
                }
                return found;
            });

        ElementData sequenceData = mock(ElementData.class);
        when(sequenceData.get(SequenceData.SESSION_ID_PROPERTY_NAME))
            .thenReturn("abcdefg-hijklmn");
        when(sequenceData.get(SequenceData.SEQUENCE_PROPERTY_NAME))
            .thenReturn(1);
        when(flowData.getDataKeys()).thenReturn(Collections.singletonList(
            SequenceElement.DEFAULT_ELEMENT_DATA_KEY));
        when(flowData.get(SequenceElement.DEFAULT_ELEMENT_DATA_KEY))
            .thenReturn(sequenceData);

        when(flowData.getAs("Promise", AspectPropertyValue.class))
            .thenReturn(new AspectPropertyValueDefault<>(promise));
        doAnswer((Answer<Object>) invocation -> {
            FlowElement.DataFactory<JavaScriptBuilderData> factory =
                invocation.getArgument(1);
            result = factory.create(flowData);
            return result;
        }).when(flowData).getOrAdd(
            anyString(), any(FlowElement.DataFactory.class));

        new JavaScriptBuilderElementBuilder(loggerFactory)
            .build()
            .process(flowData);
        return result.getJavaScript();
    }

    /**
     * "Full" made at run time gives the same script as the literal "Full",
     * and both differ from the script for "None", so the comparison is of
     * the text and the test can see the promise form.
     */
    @Test
    public void PromiseFull_MadeAtRunTime_GivesThePromiseForm()
        throws Exception {
        String literal = scriptWithPromise("Full");
        String madeAtRunTime = scriptWithPromise(
            new StringBuilder("Fu").append("ll").toString());
        assertNotEquals(scriptWithPromise("None"), literal,
            "The script for Full is the same as for None.");
        assertEquals(literal, madeAtRunTime,
            "Full made at run time did not give the promise form.");
    }
}
