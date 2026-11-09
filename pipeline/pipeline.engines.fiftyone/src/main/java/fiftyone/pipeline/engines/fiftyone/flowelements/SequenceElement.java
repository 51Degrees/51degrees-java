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

import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.engines.fiftyone.data.SequenceData;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;

/**
 * Sequence element establishes session and sequence evidence in the pipeline.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/sequence-element.md">Specification</a>
 */
public class SequenceElement 
    extends FlowElementBase<ElementData, ElementPropertyMetaData> {

    private static final Pattern DIGITS = Pattern.compile("[0-9]+");

    private final List<ElementPropertyMetaData> properties;

    /**
     * The session id and sequence of a callback, which the specification
     * lists as this element's accepted Evidence. Without them the web
     * integration would not add them, and every callback would start a new
     * session.
     */
    private final EvidenceKeyFilter acceptedEvidence =
        new EvidenceKeyFilterWhitelist(Arrays.asList(
            Constants.EVIDENCE_SESSIONID,
            Constants.EVIDENCE_SEQUENCE),
            String.CASE_INSENSITIVE_ORDER);

    /**
     * Construct a new instance of the {@link FlowElement}.
     * @param logger logger instance to use for logging
     */
    @SuppressWarnings("this-escape")
    public SequenceElement(final Logger logger) {
        super(logger, new ElementDataFactory<ElementData>() {
            @Override
            public ElementData create(
                FlowData flowData,
                FlowElement<ElementData, ?> flowElement) {
                return new SequenceData(logger, flowData);
            }
        });
        properties = new ArrayList<>();
        properties.add(new ElementPropertyMetaDataDefault(
            SequenceData.SESSION_ID_PROPERTY_NAME,
            this,
            "",
            String.class,
            true));
        properties.add(new ElementPropertyMetaDataDefault(
            SequenceData.SEQUENCE_PROPERTY_NAME,
            this,
            "",
            Integer.class,
            true));
    }

    @Override
    protected void processInternal(FlowData data) throws Exception {
        Map<String, Object> evidence = data.getEvidence().asKeyMap();

        // The session id and the sequence are each used only when they are
        // valid, and otherwise count as absent.
        Object givenSession = evidence.get(Constants.EVIDENCE_SESSIONID);
        String sessionId =
            givenSession instanceof String &&
            ((String) givenSession).isEmpty() == false ?
                (String) givenSession : getNewSessionId();
        Integer previous = toSequence(evidence.get(Constants.EVIDENCE_SEQUENCE));
        int sequence = previous == null ? 1 : previous + 1;

        data.addEvidence(Constants.EVIDENCE_SESSIONID, sessionId);
        data.addEvidence(Constants.EVIDENCE_SEQUENCE, sequence);

        SequenceData sequenceData = (SequenceData) data.getOrAdd(
            getElementDataKey(),
            getDataFactory());
        sequenceData.setSessionId(sessionId);
        sequenceData.setSequence(sequence);
    }

    /**
     * The sequence in Evidence, which is a whole number or text made only of
     * decimal digits. A value that is anything else, does not fit in an int
     * or has no larger int after it counts as no sequence, so that adding
     * one never overflows.
     * @return the sequence, or null if there is none
     */
    private static Integer toSequence(Object sequence) {
        long value;
        if (sequence instanceof Integer || sequence instanceof Long ||
            sequence instanceof Short || sequence instanceof Byte) {
            value = ((Number) sequence).longValue();
        } else if (sequence instanceof BigInteger) {
            BigInteger big = (BigInteger) sequence;
            if (big.bitLength() > 31) {
                return null;
            }
            value = big.longValue();
        } else if (sequence instanceof String &&
            DIGITS.matcher((String) sequence).matches()) {
            // More digits than any int has cannot fit, however many zeros
            // come first, so only the value of a short string is read.
            String digits = ((String) sequence).replaceFirst("^0+(?=.)", "");
            if (digits.length() > 10) {
                return null;
            }
            value = Long.parseLong(digits);
        } else {
            return null;
        }
        return value < Integer.MAX_VALUE && value >= Integer.MIN_VALUE ?
            Integer.valueOf((int) value) : null;
    }

    @Override
    public String getElementDataKey() {
        return "sequence";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return acceptedEvidence;
    }

    @Override
    public List<ElementPropertyMetaData> getProperties() {
        return properties;
    }

    @Override
    protected void managedResourcesCleanup() {
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }

    /**
     * Get a unique id to use as the session id.
     * @return new session id
     */
    private String getNewSessionId() {
        UUID u = UUID.randomUUID();
        return u.toString();
    }
}
