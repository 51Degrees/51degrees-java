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

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Gives the HumanConfidence and HumanConfidenceSource Properties of the
 * {@code derived} Element Data from the Cloud Request Engine's JSON call,
 * and redeems a sealed Human Confidence result through that call. Add it to
 * the Pipeline after the Cloud Request Engine it is given.
 * <p>
 * A request carries a sealed result where its Evidence holds
 * {@code query.humanconfidence} with a value that is not empty. The Cloud
 * Request Engine sends it in the JSON call, and the answer's {@code derived}
 * member holds the redeemed Value. On such a request HumanConfidenceSource
 * is always {@code Active}, and HumanConfidence is the redeemed Value, or
 * {@code Medium} wherever the sealed result was not redeemed. Reading either
 * Property never throws. On any other request the two Properties are the
 * ones in the answer's {@code derived} member.
 * <p>
 * A consumer reads only the two Properties. It never reads a Value from a
 * sealed result and never redeems one itself. See the Human Confidence
 * specification, "What a consumer reads" and "When HumanConfidence arrives
 * sealed".
 */
public class HumanConfidenceElement
    extends FlowElementBase<ElementData, ElementPropertyMetaData> {

    /** The key of the Element Data this element adds. */
    public static final String ELEMENT_DATA_KEY = "derived";

    /** The Property that holds High, Medium or Low. */
    public static final String HUMAN_CONFIDENCE = "HumanConfidence";

    /** The Property that holds Active or Passive. */
    public static final String HUMAN_CONFIDENCE_SOURCE =
        "HumanConfidenceSource";

    /** The Value on a request whose sealed result was not redeemed. */
    public static final String NOT_REDEEMED_VALUE = "Medium";

    /** The source on every request that carries a sealed result. */
    public static final String ACTIVE = "Active";

    /** The reason given where the cloud response leaves a Property out. */
    static final String MISSING =
        "The cloud response did not include the property '%s'.";

    /** The reason given where the cloud request gave no response. */
    static final String NO_RESPONSE =
        "The cloud request gave no response, so '%s' has no value.";

    private static final List<String> REDEEMED_VALUES =
        Arrays.asList("High", "Medium", "Low");

    private final CloudRequestEngine requestEngine;

    private final List<ElementPropertyMetaData> properties;

    private final EvidenceKeyFilter evidenceKeyFilter =
        new EvidenceKeyFilterWhitelist(
            Collections.singletonList(
                CloudRequestEngineDefault.SEALED_HUMAN_CONFIDENCE_EVIDENCE),
            String.CASE_INSENSITIVE_ORDER);

    /**
     * @param logger        the logger
     * @param requestEngine the Cloud Request Engine whose JSON call this
     *                      element reads, which comes before this element
     *                      in the Pipeline, or null where there is none
     */
    @SuppressWarnings("this-escape")
    public HumanConfidenceElement(
        final Logger logger,
        CloudRequestEngine requestEngine) {
        super(logger, new ElementDataFactory<ElementData>() {
            @Override
            public ElementData create(
                FlowData flowData,
                FlowElement<ElementData, ?> flowElement) {
                return new Data(logger, flowData);
            }
        });
        this.requestEngine = requestEngine;
        this.properties = Collections.unmodifiableList(Arrays.asList(
            (ElementPropertyMetaData) new ElementPropertyMetaDataDefault(
                HUMAN_CONFIDENCE, this, "", String.class, true),
            new ElementPropertyMetaDataDefault(
                HUMAN_CONFIDENCE_SOURCE, this, "", String.class, true)));
    }

    @Override
    protected void processInternal(FlowData data) {
        ElementData result = data.getOrAdd(
            getElementDataKey(), getDataFactory());
        boolean sealed = CloudRequestEngineDefault.carriesSealedResult(data);
        JSONObject derived = derived(data, sealed);
        if (sealed) {
            String value = redeemedValue(derived);
            result.put(HUMAN_CONFIDENCE, new AspectPropertyValueDefault<>(
                value == null ? NOT_REDEEMED_VALUE : value));
            result.put(HUMAN_CONFIDENCE_SOURCE,
                new AspectPropertyValueDefault<>(ACTIVE));
        } else {
            result.put(HUMAN_CONFIDENCE,
                fromResponse(derived, HUMAN_CONFIDENCE));
            result.put(HUMAN_CONFIDENCE_SOURCE,
                fromResponse(derived, HUMAN_CONFIDENCE_SOURCE));
        }
    }

    /**
     * The {@code derived} member of the JSON call's answer, or null where
     * there is none. It is null where the Pipeline has no Cloud Request
     * Engine, where the engine made no request, where the request failed,
     * and, on a request that carries a sealed result, where the engine has
     * no License Key, because the sealed result is then not redeemed. A
     * failed request is recorded once as the Cloud Request Engine's error,
     * and this element adds no error of its own.
     */
    private JSONObject derived(FlowData data, boolean sealed) {
        if (requestEngine == null) {
            return null;
        }
        if (sealed && requestEngine instanceof CloudRequestEngineDefault &&
            ((CloudRequestEngineDefault) requestEngine).hasLicenseKey()
                == false) {
            return null;
        }
        CloudRequestData request;
        try {
            request = data.getFromElement(requestEngine);
        } catch (RuntimeException notInPipeline) {
            return null;
        }
        if (request == null || request.getProcessStarted() == false) {
            return null;
        }
        String json;
        try {
            json = request.getJsonResponse();
        } catch (RuntimeException failed) {
            CloudRequestDataInternal internal = request;
            Throwable failure = internal.getRequestFailure();
            if (failure != null && internal.firstToMeetFailure()) {
                data.addError(failure, requestEngine);
            }
            return null;
        }
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JSONObject answer = new JSONObject(json);
            for (String key : answer.keySet()) {
                if (ELEMENT_DATA_KEY.equalsIgnoreCase(key)) {
                    return answer.optJSONObject(key);
                }
            }
            return null;
        } catch (JSONException unreadable) {
            return null;
        }
    }

    /**
     * The redeemed Value, being {@code High}, {@code Medium} or {@code Low}
     * exactly, or null for anything else, a missing or null value and
     * {@code Unknown} included.
     */
    private static String redeemedValue(JSONObject derived) {
        String value = member(derived, HUMAN_CONFIDENCE);
        return value != null && REDEEMED_VALUES.contains(value) ? value : null;
    }

    /**
     * The Property as the answer's {@code derived} member gives it, with
     * the cloud's null reason where the value is null, and a reason of
     * this element's where the member or the response is missing.
     */
    private static AspectPropertyValue<String> fromResponse(
        JSONObject derived,
        String name) {
        AspectPropertyValue<String> value = new AspectPropertyValueDefault<>();
        if (derived == null) {
            value.setNoValueMessage(String.format(NO_RESPONSE, name));
            return value;
        }
        String key = keyIgnoringCase(derived, name);
        if (key == null) {
            value.setNoValueMessage(String.format(MISSING, name));
        } else if (derived.isNull(key)) {
            String reason = member(derived, name + "nullreason");
            value.setNoValueMessage(reason == null ? "Unknown" : reason);
        } else {
            value.setValue(derived.get(key).toString());
        }
        return value;
    }

    /** A string member of the object, ignoring case, or null. */
    private static String member(JSONObject object, String name) {
        if (object == null) {
            return null;
        }
        String key = keyIgnoringCase(object, name);
        if (key == null || object.isNull(key)) {
            return null;
        }
        return object.get(key).toString();
    }

    private static String keyIgnoringCase(JSONObject object, String name) {
        for (String key : object.keySet()) {
            if (key.equalsIgnoreCase(name)) {
                return key;
            }
        }
        return null;
    }

    @Override
    public String getElementDataKey() {
        return ELEMENT_DATA_KEY;
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return evidenceKeyFilter;
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

    /** The {@code derived} Element Data this element adds. */
    static final class Data extends ElementDataBase {
        Data(Logger logger, FlowData flowData) {
            super(logger, flowData);
        }
    }
}
