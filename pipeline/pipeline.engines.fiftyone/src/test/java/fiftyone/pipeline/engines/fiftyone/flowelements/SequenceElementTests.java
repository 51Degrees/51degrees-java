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
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The session id and sequence are written to Element Data as well as
 * Evidence.
 * @see <a href="https://github.com/51Degrees/specifications/blob/rc/pipeline-specification/pipeline-elements/sequence-element.md">Specification</a>
 */
public class SequenceElementTests {

    private static final String SESSION_ID_EVIDENCE = "query.session-id";
    private static final String SEQUENCE_EVIDENCE = "query.sequence";

    private Pipeline newPipeline() throws Exception {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        return new PipelineBuilder(loggerFactory)
            .addFlowElement(new SequenceElementBuilder(loggerFactory).build())
            .build();
    }

    private ElementData sequenceData(FlowData flowData) throws Exception {
        ElementData data = flowData.get("sequence");
        assertNotNull(data, "Expected Element Data under 'sequence'");
        return data;
    }

    @Test
    public void NoEvidence_NewSessionAndSequenceOne() throws Exception {
        try (Pipeline pipeline = newPipeline();
             FlowData flowData = pipeline.createFlowData()) {
            flowData.process();

            ElementData data = sequenceData(flowData);
            assertNotNull(data.get("session-id"));
            assertEquals(1, data.get("sequence"));
            assertEquals(
                data.get("session-id"),
                flowData.getEvidence().asKeyMap().get(SESSION_ID_EVIDENCE));
            assertEquals(
                1,
                flowData.getEvidence().asKeyMap().get(SEQUENCE_EVIDENCE));
        }
    }

    @Test
    public void EvidenceGiven_SessionKeptAndSequenceIncreased()
        throws Exception {
        try (Pipeline pipeline = newPipeline();
             FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence(SESSION_ID_EVIDENCE, "abc");
            flowData.addEvidence(SEQUENCE_EVIDENCE, 2);
            flowData.process();

            ElementData data = sequenceData(flowData);
            assertEquals("abc", data.get("session-id"));
            assertEquals(3, data.get("sequence"));
            assertEquals(
                3,
                flowData.getEvidence().asKeyMap().get(SEQUENCE_EVIDENCE));
        }
    }

    @Test
    public void Properties_AreSessionIdAndSequence() throws Exception {
        try (Pipeline pipeline = newPipeline()) {
            SequenceElement element =
                (SequenceElement) pipeline.getFlowElements().get(0);
            List<String> names = new ArrayList<>();
            for (ElementPropertyMetaData property : element.getProperties()) {
                names.add(property.getName());
            }
            assertTrue(names.contains("session-id"));
            assertTrue(names.contains("sequence"));
            assertEquals(2, names.size());
        }
    }

    /**
     * Process with the given Evidence values, where null means none, and
     * check that the Evidence and the Element Data hold the same values.
     * @return the session id and the sequence
     */
    private Object[] run(Object sessionId, Object sequence) throws Exception {
        try (Pipeline pipeline = newPipeline();
             FlowData flowData = pipeline.createFlowData()) {
            if (sessionId != null) {
                flowData.addEvidence(SESSION_ID_EVIDENCE, sessionId);
            }
            if (sequence != null) {
                flowData.addEvidence(SEQUENCE_EVIDENCE, sequence);
            }
            flowData.process();

            ElementData data = sequenceData(flowData);
            Object evidenceSession =
                flowData.getEvidence().asKeyMap().get(SESSION_ID_EVIDENCE);
            Object evidenceSequence =
                flowData.getEvidence().asKeyMap().get(SEQUENCE_EVIDENCE);
            assertEquals(evidenceSession, data.get("session-id"));
            assertEquals(evidenceSequence, data.get("sequence"));
            return new Object[] {evidenceSession, evidenceSequence};
        }
    }

    private void assertNewSession(Object sessionId, Object sequence,
        int expectedSequence) throws Exception {
        Object[] result = run(sessionId, sequence);
        assertTrue(result[0] instanceof String);
        assertFalse(((String) result[0]).isEmpty());
        if (sessionId != null) {
            assertNotEquals(sessionId, result[0]);
        }
        assertEquals(expectedSequence, result[1]);
    }

    private void assertSequence(Object sequence, int expected)
        throws Exception {
        Object[] result = run("abc", sequence);
        assertEquals("abc", result[0]);
        assertEquals(expected, result[1]);
    }

    @Test
    public void SequenceAsText_Digits_CountsUp() throws Exception {
        assertSequence("2", 3);
        assertSequence("0", 1);
        assertSequence("00012", 13);
    }

    @Test
    public void SequenceAsOtherWholeNumbers_CountsUp() throws Exception {
        assertSequence(2L, 3);
        assertSequence((short) 2, 3);
        assertSequence(new java.math.BigInteger("2"), 3);
    }

    @Test
    public void SequenceNotAWholeNumber_CountsAsAbsent() throws Exception {
        assertSequence("abc", 1);
        assertSequence("", 1);
        assertSequence(" 2", 1);
        assertSequence("2 ", 1);
        assertSequence("-2", 1);
        assertSequence("+2", 1);
        assertSequence("2.5", 1);
        assertSequence(2.5, 1);
        assertSequence(-1, 1);
        assertSequence(-1L, 1);
        assertSequence((short) -1, 1);
        assertSequence(Integer.MIN_VALUE, 1);
        assertSequence("-1", 1);
        assertSequence("+5", 1);
        assertSequence("5 ", 1);
        assertSequence("	5", 1);
        assertSequence(new Object(), 1);
    }

    @Test
    public void SequenceOutOfRange_CountsAsAbsent() throws Exception {
        assertSequence("99999999999999999999", 1);
        assertSequence("2147483648", 1);
        assertSequence(2147483648L, 1);
        // One more would not fit in an int either.
        assertSequence("2147483647", 1);
        assertSequence(2147483647, 1);
        assertSequence("2147483646", 2147483647);
    }

    @Test
    public void SessionIdNotANonEmptyString_CountsAsAbsent() throws Exception {
        assertNewSession("", 2, 3);
        assertNewSession(5, 2, 3);
        assertNewSession(new Object(), null, 1);
    }

    @Test
    public void SessionIdZero_Kept() throws Exception {
        Object[] result = run("0", 4);
        assertEquals("0", result[0]);
        assertEquals(5, result[1]);
    }

    @Test
    public void SequenceWithoutSessionId_CountsUpWithNewSession()
        throws Exception {
        assertNewSession(null, 4, 5);
    }

    @Test
    public void SessionIdWithoutSequence_GivesOne() throws Exception {
        Object[] result = run("abc", null);
        assertEquals("abc", result[0]);
        assertEquals(1, result[1]);
    }
}
