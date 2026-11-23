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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The Evidence keys the cloud service accepts, from its evidencekeys
 * request. It includes nothing until the keys are known, and reads them on
 * each call, so a filter that holds this one sees the keys as soon as they
 * arrive. It derives from {@link EvidenceKeyFilterWhitelist} so that the
 * Pipeline's filter keeps it rather than copying its keys once, and so
 * that the web integration lists its headers in the Vary header.
 */
class CloudEvidenceKeyFilter extends EvidenceKeyFilterWhitelist {

    private volatile EvidenceKeyFilterWhitelist keys = null;

    CloudEvidenceKeyFilter() {
        super(new ArrayList<String>(), String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * @return true once the keys are known
     */
    boolean isKnown() {
        return keys != null;
    }

    /**
     * Set the keys the cloud service accepts.
     * @param evidenceKeys the keys
     */
    void setKeys(List<String> evidenceKeys) {
        keys = new EvidenceKeyFilterWhitelist(
            evidenceKeys,
            String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * @return the keys, or null if they are not known
     */
    List<String> getKeys() {
        EvidenceKeyFilterWhitelist current = keys;
        return current == null ?
            null :
            new ArrayList<>(current.getWhitelist().keySet());
    }

    @Override
    public boolean include(String key) {
        EvidenceKeyFilterWhitelist current = keys;
        return current != null && current.include(key);
    }

    @Override
    public Integer order(String key) {
        EvidenceKeyFilterWhitelist current = keys;
        return current == null ? null : current.order(key);
    }

    @Override
    public Map<String, Integer> getWhitelist() {
        EvidenceKeyFilterWhitelist current = keys;
        return current == null ?
            Collections.<String, Integer>emptyMap() :
            current.getWhitelist();
    }
}
