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

package fiftyone.pipeline.cloudrequestengine;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A copy of what the Cloud Request Engine discovered for a resource key,
 * being the accepted Evidence keys and the accessible properties, which
 * depend only on the resource key. Read it from the builder after a
 * successful build, keep it, for example as {@link #toJson()}, and give it
 * to a later builder, which then makes no discovery request. A Pipeline can
 * so be built offline, and a host that starts often need not call the
 * cloud service each time.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/cloud-request-engine.md#resilience">Specification</a>
 */
public final class CloudDiscoveryState {

    private static final String EVIDENCE_KEYS = "evidenceKeys";

    private static final String ACCESSIBLE_PROPERTIES = "accessibleProperties";

    private final List<String> evidenceKeys;

    private final String accessibleProperties;

    /**
     * Construct a new instance.
     * @param evidenceKeys the evidence keys the cloud service accepts
     * @param accessibleProperties the answer to the accessibleproperties
     *                             request, as JSON
     * @throws org.json.JSONException if the accessible properties are not
     * a JSON object with a Products member
     */
    public CloudDiscoveryState(
        List<String> evidenceKeys,
        String accessibleProperties) {
        Objects.requireNonNull(evidenceKeys, "evidenceKeys");
        Objects.requireNonNull(accessibleProperties, "accessibleProperties");
        new JSONObject(accessibleProperties).getJSONObject("Products");
        this.evidenceKeys = Collections.unmodifiableList(
            new ArrayList<>(evidenceKeys));
        this.accessibleProperties = accessibleProperties;
    }

    /**
     * @return the evidence keys the cloud service accepts
     */
    public List<String> getEvidenceKeys() {
        return evidenceKeys;
    }

    /**
     * @return the answer to the accessibleproperties request, as JSON
     */
    public String getAccessibleProperties() {
        return accessibleProperties;
    }

    /**
     * @return this copy as one JSON document, which {@link #fromJson}
     * reads back
     */
    public String toJson() {
        JSONObject json = new JSONObject();
        json.put(EVIDENCE_KEYS, new JSONArray(evidenceKeys));
        json.put(ACCESSIBLE_PROPERTIES, new JSONObject(accessibleProperties));
        return json.toString();
    }

    /**
     * Read a copy written by {@link #toJson()}.
     * @param json the JSON document
     * @return the copy
     * @throws org.json.JSONException if the document is not one
     * {@link #toJson()} writes
     */
    public static CloudDiscoveryState fromJson(String json) {
        JSONObject document = new JSONObject(json);
        JSONArray keys = document.getJSONArray(EVIDENCE_KEYS);
        List<String> evidenceKeys = new ArrayList<>();
        for (int i = 0; i < keys.length(); i++) {
            evidenceKeys.add(keys.getString(i));
        }
        return new CloudDiscoveryState(
            evidenceKeys,
            document.getJSONObject(ACCESSIBLE_PROPERTIES).toString());
    }
}
