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

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.exceptions.AggregateException;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * With no Sequence Element before it, the JSON Builder fails with the message
 * the specification gives, which is the same in every language.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/json-builder.md#sequence-number">Specification</a>
 */
public class JsonBuilderSequenceMessageTests {
    private static final String MESSAGE =
        "The JSON Builder needs a sequence number. Place a Sequence Element " +
        "before the JSON Builder in the Pipeline.";

    private final TestLoggerFactory loggerFactory;

    public JsonBuilderSequenceMessageTests() {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString()))
            .thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
    }

    private Pipeline buildPipeline(boolean suppress) throws Exception {
        return new PipelineBuilder(loggerFactory)
            .addFlowElement(new JsonBuilderElementBuilder(loggerFactory).build())
            .setAutoCloseElements(true)
            .setSuppressProcessException(suppress)
            .build();
    }

    /**
     * The recorded error and its cause both carry the message.
     */
    @Test
    public void JsonBuilder_NoSequenceElement_RecordedMessage()
        throws Exception {
        try (Pipeline pipeline = buildPipeline(true);
             FlowData flowData = pipeline.createFlowData()) {
            flowData.process();

            Collection<FlowError> errors = flowData.getErrors();
            assertEquals(1, errors.size());
            Throwable error = errors.iterator().next().getThrowable();
            assertInstanceOf(PipelineConfigurationException.class, error);
            assertEquals(MESSAGE, error.getMessage());
            assertEquals(MESSAGE, error.getCause().getMessage());
        }
    }

    /**
     * With exceptions not suppressed, the error the caller catches carries
     * the message.
     */
    @Test
    public void JsonBuilder_NoSequenceElement_ThrownMessage()
        throws Exception {
        try (Pipeline pipeline = buildPipeline(false);
             FlowData flowData = pipeline.createFlowData()) {
            AggregateException thrown = assertThrows(
                AggregateException.class,
                flowData::process);
            assertEquals(1, thrown.getSuppressed().length);
            assertEquals(MESSAGE, thrown.getSuppressed()[0].getMessage());
        }
    }
}