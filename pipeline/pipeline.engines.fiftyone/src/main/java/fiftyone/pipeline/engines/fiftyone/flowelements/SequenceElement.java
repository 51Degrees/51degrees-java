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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;

/**
 * Sequence element establishes session and sequence evidence in the pipeline.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/sequence-element.md">Specification</a>
 */
public class SequenceElement 
    extends FlowElementBase<ElementData, ElementPropertyMetaData> {

    private final List<ElementPropertyMetaData> properties;

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
        
        // If the evidence does not contain a session id then create a new one.
        if (evidence.containsKey(Constants.EVIDENCE_SESSIONID) == false) {
            data.addEvidence(Constants.EVIDENCE_SESSIONID, getNewSessionId());
        }

        // If the evidence does not have a sequence then add one.
        if (evidence.containsKey(Constants.EVIDENCE_SEQUENCE) == false) {
            data.addEvidence(Constants.EVIDENCE_SEQUENCE, 1);
        } else {
            Object sequence = evidence.get(Constants.EVIDENCE_SEQUENCE);
            int seq = (int) sequence;
            data.addEvidence(Constants.EVIDENCE_SEQUENCE, seq + 1);
        }
        addElementData(data);
    }

    /**
     * Write the session id and sequence now in Evidence to the element data.
     */
    private void addElementData(FlowData data) {
        Map<String, Object> evidence = data.getEvidence().asKeyMap();
        SequenceData sequenceData = (SequenceData) data.getOrAdd(
            getElementDataKey(),
            getDataFactory());
        sequenceData.setSessionId(
            String.valueOf(evidence.get(Constants.EVIDENCE_SESSIONID)));
        sequenceData.setSequence(
            ((Number) evidence.get(Constants.EVIDENCE_SEQUENCE)).intValue());
    }

    @Override
    public String getElementDataKey() {
        return "sequence-element";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return new EvidenceKeyFilterWhitelist(new ArrayList<String>());
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
