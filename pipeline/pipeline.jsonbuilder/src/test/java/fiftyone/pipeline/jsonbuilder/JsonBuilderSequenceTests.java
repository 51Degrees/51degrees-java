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
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElement;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngine;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngineBuilder;
import fiftyone.pipeline.exceptions.AggregateException;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;

import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The JsonBuilderElement needs a sequence number, and fails without one.
 * <p>
 * The
 * <a href="https://github.com/51Degrees/specifications/blob/67ead698/pipeline-specification/pipeline-elements/json-builder.md#sequence-number">JSON
 * Builder specification</a> says the element MUST fail with an error and MUST
 * NOT set its JSON value when no Sequence Element has produced a sequence
 * number, that a query.sequence value taken from a query string is text and so
 * does not count, and that an integer a caller added to query.sequence does
 * count in Java because the element reads the integer in Evidence whoever
 * added it.
 * <p>
 * The error MUST carry the message the page gives, which is
 * {@link Constants#NO_SEQUENCE_NUMBER_MESSAGE}, and where the error is wrapped
 * in another the one the caller reads carries it.
 */
public class JsonBuilderSequenceTests {

    private final TestLoggerFactory loggerFactory;

    public JsonBuilderSequenceTests() {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
    }

    /**
     * Build a pipeline of an engine and the JSON builder, with a sequence
     * element in front when asked for.
     * @param withSequenceElement whether to place a sequence element first
     * @param suppress whether the pipeline suppresses processing exceptions
     * @return the built pipeline, which closes its elements
     * @throws Exception if the engine or the pipeline cannot be built
     */
    private Pipeline buildPipeline(
        boolean withSequenceElement,
        boolean suppress) throws Exception {
        EmptyEngine engine = new EmptyEngineBuilder(loggerFactory).build();
        JsonBuilderElement jsonBuilder =
            new JsonBuilderElementBuilder(loggerFactory).build();
        PipelineBuilder builder = new PipelineBuilder(loggerFactory);
        if (withSequenceElement) {
            SequenceElement sequenceElement =
                new SequenceElementBuilder(loggerFactory).build();
            builder = builder.addFlowElement(sequenceElement);
        }
        return builder
            .addFlowElement(engine)
            .addFlowElement(jsonBuilder)
            .setAutoCloseElements(true)
            .setSuppressProcessException(suppress)
            .build();
    }

    /**
     * Process one request with the pipeline's exceptions suppressed, and
     * assert that the element failed, said a sequence element is needed and
     * set no JSON.
     * @param sequence the query.sequence Evidence to add, or null for none
     * @throws Exception if the pipeline cannot be built
     */
    private void assertFailsWithoutASequenceNumber(Object sequence)
        throws Exception {
        try (Pipeline pipeline = buildPipeline(false, true);
             FlowData flowData = pipeline.createFlowData()) {
            if (sequence != null) {
                flowData.addEvidence("query.sequence", sequence);
            }
            flowData.process();

            Collection<FlowError> errors = flowData.getErrors();
            assertEquals(1, errors.size(),
                "Expected the JsonBuilderElement to record one error.");
            String message = errors.iterator().next().getThrowable().getMessage();
            assertEquals(Constants.NO_SEQUENCE_NUMBER_MESSAGE, message,
                "Expected the error the caller reads to carry the message " +
                    "the specification gives, word for word.");

            JsonBuilderData result = flowData.get(JsonBuilderData.class);
            assertNull(result == null ? null : result.getJson(),
                "Expected no JSON to have been set.");
        }
    }

    /**
     * The message is written out here as well as in the element, so that
     * changing the element's constant fails this test rather than moving the
     * other assertions along with it.
     */
    @Test
    public void JsonBuilder_TheMessage_IsTheOneTheSpecificationGives() {
        assertEquals(
            "The JSON Builder needs a sequence number. Place a Sequence " +
                "Element before the JSON Builder in the Pipeline.",
            Constants.NO_SEQUENCE_NUMBER_MESSAGE);
    }

    /**
     * With no sequence element and no Evidence there is no sequence number,
     * so the element fails and sets no JSON.
     */
    @Test
    public void JsonBuilder_NoSequenceElement_Fails() throws Exception {
        assertFailsWithoutASequenceNumber(null);
    }

    /**
     * A query string carries text. Text is not a sequence number, so the
     * element fails as it does with no Evidence at all.
     */
    @Test
    public void JsonBuilder_SequenceFromQueryString_Fails() throws Exception {
        assertFailsWithoutASequenceNumber("4");
    }

    /**
     * Text that is not a number at all fails the same way.
     */
    @Test
    public void JsonBuilder_SequenceThatIsNotANumber_Fails() throws Exception {
        assertFailsWithoutASequenceNumber("abc");
    }

    /**
     * The pipeline's own exception handling decides whether the error is
     * thrown or recorded. With exceptions not suppressed the failure reaches
     * the caller, and still no JSON is set.
     */
    @Test
    public void JsonBuilder_NoSequenceElement_ThrowsWhenNotSuppressed()
        throws Exception {
        try (Pipeline pipeline = buildPipeline(false, false);
             FlowData flowData = pipeline.createFlowData()) {
            AggregateException thrown = assertThrows(
                AggregateException.class,
                flowData::process);
            // The aggregate carries each element's error as a suppressed
            // exception.
            assertEquals(1, thrown.getSuppressed().length,
                "Expected one suppressed exception in the aggregate.");
            String message = thrown.getSuppressed()[0].getMessage();
            assertEquals(Constants.NO_SEQUENCE_NUMBER_MESSAGE, message,
                "Expected the error the caller reads to carry the message " +
                    "the specification gives, word for word.");

            JsonBuilderData result = flowData.get(JsonBuilderData.class);
            assertNull(result == null ? null : result.getJson(),
                "Expected no JSON to have been set.");
        }
    }

    /**
     * The error the element records carries the one getSequenceNumber threw
     * as its cause, and the cause says the same thing, because it is the line
     * a caller meets at the top of the stack trace.
     */
    @Test
    public void JsonBuilder_NoSequenceElement_TheCauseSaysWhatIsNeeded()
        throws Exception {
        try (Pipeline pipeline = buildPipeline(false, true);
             FlowData flowData = pipeline.createFlowData()) {
            flowData.process();

            Collection<FlowError> errors = flowData.getErrors();
            assertEquals(1, errors.size(),
                "Expected the JsonBuilderElement to record one error.");
            Throwable cause =
                errors.iterator().next().getThrowable().getCause();
            assertNotNull(cause, "Expected the error to carry a cause.");
            assertEquals(Constants.NO_SEQUENCE_NUMBER_MESSAGE,
                cause.getMessage(),
                "Expected the cause to carry the same message as the error " +
                    "the caller reads.");
        }
    }

    /**
     * An integer a caller added to query.sequence is a sequence number, even
     * with no sequence element, because the element reads the integer in
     * Evidence whoever added it.
     */
    @Test
    public void JsonBuilder_IntegerFromTheCaller_BuildsTheJson()
        throws Exception {
        try (Pipeline pipeline = buildPipeline(false, false);
             FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence("query.sequence", 3);
            flowData.process();

            assertEquals(0, flowData.getErrors().size(),
                "Expected no error with an integer sequence number.");
            JsonBuilderData result = flowData.get(JsonBuilderData.class);
            assertNotNull(result, "Expected the JsonBuilderData to be present.");
            assertNotNull(result.getJson(), "Expected the JSON to be set.");
        }
    }

    /**
     * With a sequence element in front the JSON is built, which is the order
     * the web integration assembles.
     */
    @Test
    public void JsonBuilder_WithSequenceElement_BuildsTheJson()
        throws Exception {
        try (Pipeline pipeline = buildPipeline(true, false);
             FlowData flowData = pipeline.createFlowData()) {
            flowData.process();

            assertEquals(0, flowData.getErrors().size(),
                "Expected no error with a sequence element in the pipeline.");
            JsonBuilderData result = flowData.get(JsonBuilderData.class);
            assertNotNull(result, "Expected the JsonBuilderData to be present.");
            assertNotNull(result.getJson(), "Expected the JSON to be set.");
        }
    }
}
