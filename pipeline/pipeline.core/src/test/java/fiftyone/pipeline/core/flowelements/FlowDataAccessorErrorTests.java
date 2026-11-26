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

package fiftyone.pipeline.core.flowelements;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.FlowData;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * FlowData's accessors throw only for something the caller did wrong. Data
 * that is simply not there, because an element did not run or gave no
 * value, is reported as absent rather than as an error. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/exception-handling.md#L57">exception-handling.md line 57</a>:
 * "The general guidance is that these functions and accessors SHOULD only
 * throw errors/exceptions if the failure is likely due to something that
 * the API user has done wrong."
 */
public class FlowDataAccessorErrorTests {

    private final ILoggerFactory loggerFactory = name -> mock(Logger.class);

    /**
     * An element with one available property, "value". When it runs it
     * adds Element Data holding the given value; when writes is false it
     * adds nothing, as an element that did not run.
     */
    @SuppressWarnings("rawtypes")
    private static FlowElement<?, ?> element(boolean writes, Object value)
        throws Exception {
        FlowElement element = mock(FlowElement.class);
        ElementPropertyMetaData property = new ElementPropertyMetaDataDefault(
            "value", element, "test", String.class, true);
        doReturn("props").when(element).getElementDataKey();
        doReturn(Collections.singletonList(property))
            .when(element).getProperties();
        when(element.hasLoadedProperties()).thenReturn(true);
        doAnswer(invocation -> {
            if (writes) {
                FlowData flowData = invocation.getArgument(0);
                ElementData data = mock(ElementData.class);
                when(data.get(anyString())).thenReturn(value);
                flowData.getOrAdd("props", d -> data);
            }
            return null;
        }).when(element).process(any(FlowData.class));
        return element;
    }

    private Map<String, String> where(FlowElement<?, ?> element)
        throws Exception {
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(element)
                .build();
             FlowData flowData = pipeline.createFlowData()) {
            flowData.process();
            return flowData.getWhere(p -> true);
        }
    }

    @Test
    public void getWhere_ElementThatDidNotRun_LeavesItsPropertiesOut()
        throws Exception {
        assertTrue(where(element(false, null)).isEmpty());
    }

    @Test
    public void getWhere_PropertyWithoutAValue_LeavesItOut()
        throws Exception {
        assertTrue(where(element(true, null)).isEmpty());
    }

    @Test
    public void getWhere_PropertyWithAValue_GivesIt() throws Exception {
        assertEquals(Collections.singletonMap("props.value", "x"),
            where(element(true, "x")));
    }

    @Test
    public void absentData_IsNullOrNoValue_NotAnError() throws Exception {
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(element(false, null))
                .build();
             FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence("query.number", "not a number");
            flowData.process();
            // get by key or by type throws for data that no element added,
            // which FlowDataTests and the TypedKeyMap tests expect, as
            // asking for it is taken as the caller's mistake. Evidence that
            // is missing or of another type is not.
            assertFalse(flowData.tryGetEvidence("query.number", Integer.class)
                .hasValue());
            assertFalse(flowData.tryGetEvidence("query.missing", String.class)
                .hasValue());
        }
    }

    /**
     * Keeps the List import, which the element's properties use.
     */
    private static List<?> unused;
}
