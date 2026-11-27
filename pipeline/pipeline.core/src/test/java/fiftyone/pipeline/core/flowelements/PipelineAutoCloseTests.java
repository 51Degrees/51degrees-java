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
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Closing a Pipeline closes its elements only when it is set to, so that
 * an element shared with another Pipeline can be kept open. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/resource-cleanup.md#L28">resource-cleanup.md line 28</a>:
 * "However, if the multi Pipeline Elements feature is implemented then
 * there MUST be an option to disable this behavior."
 */
public class PipelineAutoCloseTests {

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    @Test
    public void autoCloseOff_SharedElementStaysOpen() throws Exception {
        MultiplyByFiveElement shared =
            new MultiplyByFiveElement(mock(Logger.class));
        Pipeline first = new PipelineBuilder(loggerFactory)
            .setAutoCloseElements(false)
            .addFlowElement(shared)
            .build();
        try (Pipeline second = new PipelineBuilder(loggerFactory)
                .setAutoCloseElements(false)
                .addFlowElement(shared)
                .build()) {
            first.close();

            assertFalse("Closing one Pipeline closed the shared element.",
                shared.isClosed());
            try (FlowData flowData = second.createFlowData()) {
                flowData.addEvidence(shared.evidenceKeys.get(0), 2);
                flowData.process();
                assertEquals(10, flowData.getFromElement(shared).getResult());
            }
        } finally {
            // Closing again does nothing.
            first.close();
            shared.close();
        }
    }

    @Test
    public void autoCloseOn_ClosesTheElement() throws Exception {
        MultiplyByFiveElement element =
            new MultiplyByFiveElement(mock(Logger.class));
        Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .setAutoCloseElements(true)
            .addFlowElement(element)
            .build();
        try {
            assertFalse(element.isClosed());
        } finally {
            pipeline.close();
        }
        assertTrue(element.isClosed());
    }
}
