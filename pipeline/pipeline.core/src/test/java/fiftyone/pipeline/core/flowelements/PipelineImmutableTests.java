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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByFiveElement;
import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByTenElement;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Once built, a Pipeline's elements cannot be changed. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/conceptual-overview.md#L118">conceptual-overview.md line 118</a>:
 * "A Pipeline instance MUST be immutable. In other words, once created, it
 * cannot be changed by adding new elements, removing old ones, etc."
 */
public class PipelineImmutableTests {

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    /**
     * The list of elements a Pipeline gives cannot be changed.
     */
    @Test
    public void getFlowElements_CannotBeChanged() throws Exception {
        MultiplyByFiveElement five = new MultiplyByFiveElement(
            mock(Logger.class));
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(five)
                .build()) {
            try {
                pipeline.getFlowElements().add(
                    new MultiplyByTenElement(mock(Logger.class)));
                fail("An element was added to a built Pipeline.");
            } catch (UnsupportedOperationException e) {
                // Expected.
            }
            try {
                pipeline.getFlowElements().clear();
                fail("The elements of a built Pipeline were removed.");
            } catch (UnsupportedOperationException e) {
                // Expected.
            }
            assertEquals(Collections.singletonList(five),
                pipeline.getFlowElements());
        }
    }

    /**
     * An element added to the builder after it has built a Pipeline is not
     * part of that Pipeline, and does not run when it processes.
     */
    @Test
    public void builderReused_DoesNotChangeABuiltPipeline() throws Exception {
        MultiplyByFiveElement five = new MultiplyByFiveElement(
            mock(Logger.class));
        MultiplyByTenElement ten = new MultiplyByTenElement(
            mock(Logger.class));
        PipelineBuilder builder = new PipelineBuilder(loggerFactory)
            .addFlowElement(five);
        try (Pipeline pipeline = builder.build()) {
            builder.addFlowElement(ten);

            assertEquals(1, pipeline.getFlowElements().size());
            assertSame(five, pipeline.getFlowElements().get(0));
            assertEquals(null, pipeline.getElement(MultiplyByTenElement.class));
            try (FlowData flowData = pipeline.createFlowData()) {
                flowData.addEvidence(five.evidenceKeys.get(0), 2);
                flowData.process();
                assertEquals(10, flowData.getFromElement(five).getResult());
                assertFalse(
                    "An element added to the builder later ran.",
                    flowData.getDataKeys().stream().anyMatch(
                        k -> k.equalsIgnoreCase(ten.getElementDataKey())));
            }
        }
    }
}
