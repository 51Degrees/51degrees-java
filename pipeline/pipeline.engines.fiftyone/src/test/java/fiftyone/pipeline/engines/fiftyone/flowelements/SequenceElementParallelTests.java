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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A Flow Element in a parallel group must not add Evidence, and that is the
 * Flow Element's responsibility, so implementations do not check for it. A
 * parallel group that holds the Sequence Element, which adds Evidence, is
 * built by every public way of making one, as it always has been.
 * @see <a href="https://github.com/51Degrees/specifications/blob/d79bbe3b2aef86273570742f6b660f29bfe72816/pipeline-specification/features/thread-safety.md#evidence">Specification</a>
 */
public class SequenceElementParallelTests {

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    private FlowElement<?, ?> sequence() {
        return new SequenceElementBuilder(loggerFactory).build();
    }

    @Test
    public void AddFlowElementsParallel_SequenceElement_IsBuilt()
        throws Exception {
        FlowElement<?, ?>[] group =
            new FlowElement<?, ?>[] {sequence(), sequence()};
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElementsParallel(group)
            .setAutoCloseElements(true)
            .build()) {
            assertNotNull(pipeline);
        }
    }

    @Test
    public void BuildFromConfiguration_SequenceElementInSubElements_IsBuilt()
        throws Exception {
        ElementOptions first = new ElementOptions();
        first.builderName = "SequenceElementBuilder";
        ElementOptions second = new ElementOptions();
        second.builderName = "SequenceElementBuilder";
        ElementOptions group = new ElementOptions();
        group.subElements = Arrays.asList(first, second);
        PipelineOptions options = new PipelineOptions();
        options.elements.add(group);
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .buildFromConfiguration(options)) {
            assertNotNull(pipeline);
        }
    }

    @Test
    public void AddFlowElement_SequenceElementOnItsOwn_IsBuilt()
        throws Exception {
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(sequence())
            .setAutoCloseElements(true)
            .build()) {
            assertNotNull(pipeline);
        }
    }
}