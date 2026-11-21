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

import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByElement;
import fiftyone.pipeline.core.testclasses.flowelements.RunsLastElement;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A Flow Element that must run last, as the usage sharing element must, is
 * refused anywhere but last, and in a parallel group, whether the Pipeline
 * is built in code or from a configuration.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/usage-sharing-element.md#position-in-the-pipeline">Specification</a>
 */
@SuppressWarnings("rawtypes")
public class RunsLastTests {

    private static final String MUST_BE_LAST =
        "must be the last Flow Element and outside any parallel group";

    private static void assertRefused(PipelineBuilder builder) {
        try (Pipeline pipeline = builder.build()) {
            fail("A Pipeline with the element anywhere but last was built.");
        } catch (PipelineConfigurationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(MUST_BE_LAST));
            assertTrue(e.getMessage(),
                e.getMessage().startsWith("The test element"));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void RunsLast_First_Refused() {
        assertRefused(new PipelineBuilder()
            .addFlowElement(new RunsLastElement())
            .addFlowElement(new MultiplyByElement(2)));
    }

    @Test
    public void RunsLast_InAParallelGroup_Refused() {
        assertRefused(new PipelineBuilder()
            .addFlowElement(new MultiplyByElement(2))
            .addFlowElementsParallel(new FlowElement[]{
                new MultiplyByElement(3),
                new RunsLastElement()}));
    }

    @Test
    public void RunsLast_Last_Built() throws Exception {
        RunsLastElement last = new RunsLastElement();
        try (Pipeline pipeline = new PipelineBuilder()
            .addFlowElement(new MultiplyByElement(2))
            .addFlowElement(last)
            .build()) {
            List<FlowElement> elements = pipeline.getFlowElements();
            assertEquals(last, elements.get(elements.size() - 1));
        }
    }

    private static ElementOptions element(String builderName) {
        ElementOptions element = new ElementOptions();
        element.builderName = builderName;
        return element;
    }

    private static ElementOptions multiplyBy() {
        ElementOptions element = element("MultiplyByElement");
        element.buildParameters.put("Multiple", "8");
        return element;
    }

    private static void assertRefusedFrom(PipelineOptions options) {
        try (Pipeline pipeline =
                 new PipelineBuilder().buildFromConfiguration(options)) {
            fail("A Pipeline with the element anywhere but last was built " +
                "from a configuration.");
        } catch (PipelineConfigurationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(MUST_BE_LAST));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void RunsLast_FirstInAConfiguration_Refused() {
        PipelineOptions options = new PipelineOptions();
        options.elements.add(element("RunsLastElement"));
        options.elements.add(multiplyBy());
        assertRefusedFrom(options);
    }

    @Test
    public void RunsLast_InAParallelGroupInAConfiguration_Refused() {
        ElementOptions group = new ElementOptions();
        group.subElements.add(multiplyBy());
        group.subElements.add(element("RunsLastElement"));
        PipelineOptions options = new PipelineOptions();
        options.elements.add(multiplyBy());
        options.elements.add(group);
        assertRefusedFrom(options);
    }

    @Test
    public void RunsLast_LastInAConfiguration_Built() throws Exception {
        PipelineOptions options = new PipelineOptions();
        options.elements.add(multiplyBy());
        options.elements.add(element("RunsLastElement"));
        try (Pipeline pipeline =
                 new PipelineBuilder().buildFromConfiguration(options)) {
            List<FlowElement> elements = pipeline.getFlowElements();
            assertTrue(elements.get(elements.size() - 1)
                instanceof RunsLastElement);
        }
    }
}
