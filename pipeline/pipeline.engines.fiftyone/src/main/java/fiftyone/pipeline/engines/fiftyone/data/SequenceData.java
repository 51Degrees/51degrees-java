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

package fiftyone.pipeline.engines.fiftyone.data;

import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElement;
import org.slf4j.Logger;

/**
 * Data containing the result of {@link SequenceElement}.
 * @see <a href="https://github.com/51Degrees/specifications/blob/rc/pipeline-specification/pipeline-elements/sequence-element.md#element-data">Specification</a>
 */
public class SequenceData extends ElementDataBase {

    /**
     * Name of the property holding the session id.
     */
    public static final String SESSION_ID_PROPERTY_NAME = "session-id";

    /**
     * Name of the property holding the sequence.
     */
    public static final String SEQUENCE_PROPERTY_NAME = "sequence";

    /**
     * Construct a new instance.
     * @param logger the logger to use
     * @param flowData the flow data the data belongs to
     */
    public SequenceData(Logger logger, FlowData flowData) {
        super(logger, flowData);
    }

    /**
     * Get the identifier of the session.
     * @return the session id
     */
    public String getSessionId() {
        return getAs(SESSION_ID_PROPERTY_NAME, String.class);
    }

    /**
     * Set the identifier of the session.
     * @param sessionId the session id
     */
    public void setSessionId(String sessionId) {
        put(SESSION_ID_PROPERTY_NAME, sessionId);
    }

    /**
     * Get the number of consecutive callbacks made in the session.
     * @return the sequence
     */
    public Integer getSequence() {
        return getAs(SEQUENCE_PROPERTY_NAME, Integer.class);
    }

    /**
     * Set the number of consecutive callbacks made in the session.
     * @param sequence the sequence
     */
    public void setSequence(int sequence) {
        put(SEQUENCE_PROPERTY_NAME, sequence);
    }
}
