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

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.data.SequenceData;
import fiftyone.pipeline.engines.fiftyone.data.SetHeadersData;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The check, for the Flow Elements in this module, that processes each one,
 * then writes to its Element Data and expects the error: the
 * {@link SequenceElement}, the {@link SetHeadersElement}, and the
 * {@link ShareUsageElement}, which adds no Element Data, so has none to
 * write to.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Specification</a>
 */
public class WritesAfterProcessingTests {

    private static final ILoggerFactory LOGGER_FACTORY =
        LoggerFactory.getILoggerFactory();

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
    }

    @Test
    public void sequenceAndSetHeaders_ElementDataRefusesWrites()
        throws Exception {
        SequenceElement sequence =
            new SequenceElementBuilder(LOGGER_FACTORY).build();
        SetHeadersElement setHeaders =
            new SetHeadersElementBuilder(LOGGER_FACTORY).build();
        try (Pipeline pipeline = new PipelineBuilder(LOGGER_FACTORY)
                .addFlowElement(sequence)
                .addFlowElement(setHeaders)
                .setAutoCloseElements(true)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.process();
            assertEquals(0, data.getErrors().size(),
                String.valueOf(data.getErrors()));

            SequenceData sequenceData =
                (SequenceData) data.getFromElement(sequence);
            assertWritesFail(sequenceData);
            assertThrows(
                IllegalStateException.class,
                () -> sequenceData.setSessionId("after"),
                "setSessionId");
            assertThrows(
                IllegalStateException.class,
                () -> sequenceData.setSequence(2),
                "setSequence");

            SetHeadersData headersData = data.getFromElement(setHeaders);
            assertWritesFail(headersData);
            assertThrows(
                IllegalStateException.class,
                () -> headersData.setResponseHeaderDictionary(
                    new HashMap<String, String>()),
                "setResponseHeaderDictionary");
        }
    }

    @Test
    public void shareUsage_AddsNoElementData() throws Exception {
        ShareUsageBuilder builder = new ShareUsageBuilder(LOGGER_FACTORY);
        builder.setSharePercentage(0);
        ShareUsageElement shareUsage = builder.build();
        try (Pipeline pipeline = new PipelineBuilder(LOGGER_FACTORY)
                .addFlowElement(shareUsage)
                .setAutoCloseElements(true)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", "test");
            data.process();

            assertFalse(data.getDataKeys().contains(
                shareUsage.getElementDataKey()));
        }
    }
}
