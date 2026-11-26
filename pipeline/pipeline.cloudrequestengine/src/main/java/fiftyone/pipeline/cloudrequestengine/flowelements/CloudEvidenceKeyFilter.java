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
import java.util.Locale;
import java.util.Map;

/**
 * The Evidence keys the cloud service accepts, from its evidencekeys
 * request. It includes nothing until the keys are known, and reads them on
 * each call, so a filter that holds this one sees the keys as soon as they
 * arrive. It derives from {@link EvidenceKeyFilterWhitelist} so that the
 * Pipeline's filter keeps it rather than copying its keys once, and so
 * that the web integration lists its headers in the Vary header.
 * <p>
 * It also includes every query and cookie key whose name starts with
 * {@link #BROWSER_ANSWER_PREFIX}, once the keys are known. The cloud
 * service names the answers of the scripts it hands to the browser, such
 * as the browser tests, after that prefix, and each script picks its own
 * name after it, so the service cannot list them. A server that passes the
 * page's script on gets the browser's answers back under those names, and
 * they have to reach the cloud service on the next iteration.
 */
class CloudEvidenceKeyFilter extends EvidenceKeyFilterWhitelist {

    /**
     * The start of the name of every answer the cloud service's scripts
     * send back, compared without regard to case.
     */
    static final String BROWSER_ANSWER_PREFIX = "51D_";

    /**
     * The prefixes the answers of the cloud service's scripts arrive
     * under.
     */
    private static final String[] BROWSER_ANSWER_SOURCES = {
        "query.", "cookie."
    };

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
        return current != null &&
            (current.include(key) || isBrowserAnswer(key));
    }

    @Override
    public Integer order(String key) {
        EvidenceKeyFilterWhitelist current = keys;
        if (current == null) {
            return null;
        }
        Integer order = current.order(key);
        return order == null && isBrowserAnswer(key) ? 0 : order;
    }

    /**
     * Whether the key is a query or cookie key whose name starts with
     * {@link #BROWSER_ANSWER_PREFIX} and has more after it, compared
     * without regard to case.
     * @param key the Evidence key
     * @return true if the key can hold the answer of a cloud script
     */
    static boolean isBrowserAnswer(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        String prefix = BROWSER_ANSWER_PREFIX.toLowerCase(Locale.ROOT);
        for (String source : BROWSER_ANSWER_SOURCES) {
            if (lower.startsWith(source) &&
                lower.length() > source.length() + prefix.length() &&
                lower.startsWith(prefix, source.length())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<String, Integer> getWhitelist() {
        EvidenceKeyFilterWhitelist current = keys;
        return current == null ?
            Collections.<String, Integer>emptyMap() :
            current.getWhitelist();
    }
}
