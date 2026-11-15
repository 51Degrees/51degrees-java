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

package fiftyone.pipeline.web.services;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.fiftyone.data.ShareUsageEvidence;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A read-only view of every value the web integration offered as Evidence
 * for a request, accepted or not, keyed the way Evidence is keyed, such as
 * header.user-agent. It is added to Evidence as
 * {@link ShareUsageEvidence#HTTP_REQUEST_KEY} for the usage sharing element,
 * which accepts no Evidence and reads the request here instead. The
 * client-side endpoints make their ETag from it too, because it holds the
 * values as the request supplied them, before any Flow Element adds its own.
 * <p>
 * It is a map rather than the HttpServletRequest because the usage sharing
 * element does not depend on the servlet API, and the values are copied so
 * that they stay the same once the request is recycled.
 */
public class WebRequestEvidence extends AbstractMap<String, Object> {

    private final Map<String, Object> values =
        new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private final Set<Entry<String, Object>> entries =
        Collections.unmodifiableMap(values).entrySet();

    WebRequestEvidence() {
    }

    /**
     * The view in the Evidence of the {@link FlowData}, which is added to it
     * the first time.
     */
    static WebRequestEvidence of(FlowData flowData) {
        Object existing = flowData.getEvidence().asKeyMap()
            .get(ShareUsageEvidence.HTTP_REQUEST_KEY);
        if (existing instanceof WebRequestEvidence) {
            return (WebRequestEvidence) existing;
        }
        WebRequestEvidence created = new WebRequestEvidence();
        flowData.addEvidence(ShareUsageEvidence.HTTP_REQUEST_KEY, created);
        return created;
    }

    void record(String key, Object value) {
        if (value != null) {
            values.put(key, value);
        }
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return entries;
    }

    @Override
    public Object get(Object key) {
        return key instanceof String ? values.get(key) : null;
    }

    @Override
    public boolean containsKey(Object key) {
        return key instanceof String && values.containsKey(key);
    }

    /**
     * Says how many values the view holds rather than listing them, so that
     * code which prints every Evidence value, such as a debug log or an
     * example page, does not print every header and cookie of the request.
     */
    @Override
    public String toString() {
        return "web request (" + values.size() + " values)";
    }
}
