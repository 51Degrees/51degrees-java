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
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.DataUploader;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngine;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngineBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * The usage sharing element is the last Flow Element of every Pipeline a
 * builder makes, and a Pipeline that puts it anywhere else, or in a parallel
 * group, is refused when it is built, in code or from a configuration.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/usage-sharing-element.md#position-in-the-pipeline">Specification</a>
 */
public class ShareUsagePositionTests {

    private static final String MUST_BE_LAST =
        "must be the last Flow Element and outside any parallel group";

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    // Sends nothing anywhere when the element is closed.
    private ShareUsageElement shareUsage() throws Exception {
        return new ShareUsageBuilder(loggerFactory, mock(DataUploader.class))
            .build();
    }

    private EmptyEngine engine() throws Exception {
        return new EmptyEngineBuilder(loggerFactory).build();
    }

    private static void assertRefused(Executable build) {
        PipelineConfigurationException e = assertThrows(
            PipelineConfigurationException.class,
            build::run,
            "A Pipeline with usage sharing anywhere but last was built.");
        assertTrue(e.getMessage().startsWith("The usage sharing element " +
            MUST_BE_LAST), e.getMessage());
    }

    private interface Executable {
        void run() throws Exception;
    }

    private static void assertLast(Pipeline pipeline) {
        List<FlowElement<?, ?>> elements = pipeline.getFlowElements();
        assertTrue(
            elements.get(elements.size() - 1) instanceof ShareUsageElement,
            "Usage sharing is not the last Flow Element.");
    }

    @Test
    public void ShareUsage_BeforeAnotherElement_Refused() throws Exception {
        ShareUsageElement shareUsage = shareUsage();
        EmptyEngine engine = engine();
        try {
            assertRefused(() -> new PipelineBuilder(loggerFactory)
                .addFlowElement(shareUsage)
                .addFlowElement(engine)
                .build()
                .close());
        } finally {
            shareUsage.close();
            engine.close();
        }
    }

    @Test
    public void ShareUsage_InAParallelGroup_Refused() throws Exception {
        ShareUsageElement shareUsage = shareUsage();
        EmptyEngine engine = engine();
        try {
            assertRefused(() -> new PipelineBuilder(loggerFactory)
                .addFlowElementsParallel(
                    new FlowElement<?, ?>[]{engine, shareUsage})
                .build()
                .close());
        } finally {
            shareUsage.close();
            engine.close();
        }
    }

    @Test
    public void ShareUsage_Last_Built() throws Exception {
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(engine())
            .addFlowElement(shareUsage())
            .setAutoCloseElements(true)
            .build()) {
            assertLast(pipeline);
        }
    }

    private static ElementOptions element(String builderName) {
        ElementOptions element = new ElementOptions();
        element.builderName = builderName;
        return element;
    }

    @Test
    public void ShareUsage_FirstInAConfiguration_Refused() {
        PipelineOptions options = new PipelineOptions();
        options.elements.add(element("ShareUsage"));
        options.elements.add(element("SetHeadersElement"));
        assertRefused(() -> new PipelineBuilder(loggerFactory)
            .buildFromConfiguration(options)
            .close());
    }

    @Test
    public void ShareUsage_InAParallelGroupInAConfiguration_Refused() {
        ElementOptions group = new ElementOptions();
        group.subElements.add(element("SetHeadersElement"));
        group.subElements.add(element("ShareUsage"));
        PipelineOptions options = new PipelineOptions();
        options.elements.add(group);
        assertRefused(() -> new PipelineBuilder(loggerFactory)
            .buildFromConfiguration(options)
            .close());
    }

    @Test
    public void FiftyOnePipelineBuilder_PutsShareUsageLast() throws Exception {
        try (Pipeline pipeline = new FiftyOnePipelineBuilder(loggerFactory)
            .addFlowElement(engine())
            .setAutoCloseElements(true)
            .build()) {
            assertLast(pipeline);
        }
    }

    @Test
    public void FiftyOnePipelineBuilder_FromAConfiguration_PutsShareUsageLast()
        throws Exception {
        PipelineOptions options = new PipelineOptions();
        options.elements.add(element("SetHeadersElement"));
        try (Pipeline pipeline = new FiftyOnePipelineBuilder(loggerFactory)
            .buildFromConfiguration(options)) {
            assertLast(pipeline);
        }
    }
}
