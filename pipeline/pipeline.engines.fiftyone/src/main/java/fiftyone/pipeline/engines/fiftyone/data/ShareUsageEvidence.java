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

import fiftyone.pipeline.core.data.Evidence;

import java.util.Map;
import java.util.TreeMap;

/**
 * The values usage sharing finds for a request. The usage sharing element
 * accepts no Evidence, so that cache keys, the ETag and the Vary header never
 * depend on it, and finds what to share here instead. The sharing rules of
 * {@link EvidenceKeyFilterShareUsage} then decide which of these are shared.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/usage-sharing-element.md#accepted-evidence">Specification</a>
 */
public final class ShareUsageEvidence {

    /**
     * The Evidence key under which the web integration adds the web request,
     * as a read-only map keyed the way Evidence is keyed, such as
     * header.user-agent or cookie.51D_Profile. No element accepts it.
     */
    public static final String HTTP_REQUEST_KEY = "server.http-request";

    private ShareUsageEvidence() {
    }

    /**
     * Every Evidence value, and every value of the web request when the
     * Evidence holds it, keyed without regard to case. A key in both keeps
     * the Evidence value. The web request itself is not one of the values.
     * @param evidence the Evidence of the request
     * @return the values found
     */
    public static Map<String, Object> find(Evidence evidence) {
        Map<String, Object> values = evidence.asKeyMap();
        Map<String, Object> found =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Object request = null;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (HTTP_REQUEST_KEY.equalsIgnoreCase(entry.getKey())) {
                request = entry.getValue();
            } else if (entry.getValue() != null) {
                found.put(entry.getKey(), entry.getValue());
            }
        }
        if (request instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) request).entrySet()) {
                if (entry.getKey() instanceof String &&
                    entry.getValue() != null) {
                    found.putIfAbsent((String) entry.getKey(), entry.getValue());
                }
            }
        }
        return found;
    }
}
