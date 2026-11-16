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

package fiftyone.pipeline.web;

import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElement;
import fiftyone.pipeline.engines.fiftyone.flowelements.SetHeadersElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The positions the web integration gives the elements it adds, as the web
 * integration specification's Pipeline construction section sets them out.
 * The Sequence Element is added first, the JavaScript Builder Element after
 * every other element, the JSON Builder Element immediately before the
 * JavaScript Builder Element, and the Set Headers Element after every other
 * element, so the last two are the JavaScript Builder and the Set Headers
 * Element in either order.
 */
public class AddedElementPositionTests {

    private static final String MULTIPLY_BY = "MultiplyByElement";

    private static final String JAVASCRIPT_BUILDER =
        JavaScriptBuilderElement.class.getSimpleName();

    private static ElementOptions element(String builderName) {
        ElementOptions element = new ElementOptions();
        element.builderName = builderName;
        return element;
    }

    private static ElementOptions multiplyBy() {
        ElementOptions element = element(MULTIPLY_BY);
        element.buildParameters.put("Multiple", "8");
        return element;
    }

    /**
     * Builds the Pipeline as the servlet filter does, with client-side
     * properties, the Sequence Element and the Set Headers Element turned on,
     * and returns the simple class names of its elements in order.
     */
    private static List<String> namesBuiltFrom(ElementOptions... configured)
        throws Exception {
        PipelineOptions options = new PipelineOptions();
        for (ElementOptions element : configured) {
            options.elements.add(element);
        }
        List<String> names = new ArrayList<>();
        try (Pipeline pipeline = StartupHelpers.buildFromConfiguration(
            new PipelineBuilder(), options, true, null,
            Constants.DEFAULT_JSON_PATH, true, true)) {
            for (FlowElement<?, ?> element : pipeline.getFlowElements()) {
                names.add(element.getClass().getSimpleName());
            }
        }
        return names;
    }

    private static void assertPositions(List<String> names) {
        int last = names.size() - 1;
        int sequence = names.indexOf(SequenceElement.class.getSimpleName());
        int json = names.indexOf(JsonBuilderElement.class.getSimpleName());
        int javaScript = names.indexOf(JAVASCRIPT_BUILDER);
        int setHeaders = names.indexOf(SetHeadersElement.class.getSimpleName());
        assertEquals("The Sequence Element should be first. " + names,
            0, sequence);
        assertEquals("The JSON Builder Element should be immediately before " +
            "the JavaScript Builder Element. " + names,
            javaScript - 1, json);
        assertTrue("The JavaScript Builder and Set Headers Elements should " +
            "be the last two. " + names,
            Math.min(javaScript, setHeaders) == last - 1 &&
            Math.max(javaScript, setHeaders) == last);
    }

    /**
     * With only an element of the application's configured, each added
     * element takes its position around it.
     */
    @Test
    public void AddedElements_TakeTheirPositions() throws Exception {
        List<String> names = namesBuiltFrom(multiplyBy());

        assertEquals(names.toString(), 5, names.size());
        assertEquals(names.toString(), 1, names.indexOf(MULTIPLY_BY));
        assertPositions(names);
    }

    /**
     * With a JavaScript Builder Element configured, the JSON Builder Element
     * is added immediately before it.
     */
    @Test
    public void JsonBuilder_AddedImmediatelyBeforeAConfiguredJavaScriptBuilder()
        throws Exception {
        List<String> names = namesBuiltFrom(
            multiplyBy(), element(JAVASCRIPT_BUILDER));

        assertEquals(names.toString(), 5, names.size());
        assertPositions(names);
    }
}
