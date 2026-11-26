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

import fiftyone.pipeline.cloudrequestengine.data.CloudPassThroughData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads one part of the cloud service's response that no other engine in
 * this package reads, such as {@code browserderived}, {@code derived} or
 * {@code fodid}, and makes its values part of the Flow Data under the same
 * name. The JSON and JavaScript builders then pass them on to the page.
 * <p>
 * This is how a server passes the cloud service's page scripts on to the
 * browser. A script the cloud service hands out, such as the browser tests
 * in {@code browserderived.BrowserTestsJavaScript}, is a JavaScript
 * property, so the JavaScript builder runs it in the page, and the page
 * sends its answers back to the server with the session id and the
 * sequence. The Cloud Request Engine sends them on to the cloud service on
 * the next iteration.
 * <p>
 * The engine takes its property metadata from the products the Cloud
 * Request Engine reports for the resource key, matching its Data Key to a
 * product without regard to case, so {@code fodid} reads the
 * {@code FODid} product.
 */
public class CloudPassThroughEngine
    extends CloudAspectEngineBase<CloudPassThroughData> {

    private final String elementDataKey;

    /**
     * Construct a new instance.
     * @param logger used for logging
     * @param dataFactory creates the Element Data
     * @param elementDataKey the name of the part of the cloud response to
     *                       read, which is also this engine's Data Key
     */
    public CloudPassThroughEngine(
        Logger logger,
        ElementDataFactory<CloudPassThroughData> dataFactory,
        String elementDataKey) {
        super(logger, dataFactory);
        if (elementDataKey == null || elementDataKey.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "The Data Key of a cloud pass through engine must name the "
                + "part of the cloud response it reads.");
        }
        this.elementDataKey = elementDataKey.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getElementDataKey() {
        return elementDataKey;
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        // This engine needs no evidence. It works from the cloud response.
        return new EvidenceKeyFilterWhitelist(new ArrayList<String>());
    }

    /**
     * The product whose name matches this engine's Data Key without regard
     * to case, or the empty product the base gives where there is none.
     * @param products the products the Cloud Request Engine reports, or
     *                 null where they are not known yet
     * @return the product
     */
    @Override
    protected AccessiblePropertyMetaData.ProductMetaData findProduct(
        Map<String, AccessiblePropertyMetaData.ProductMetaData> products) {
        if (products != null) {
            for (Map.Entry<String, AccessiblePropertyMetaData.ProductMetaData>
                    product : products.entrySet()) {
                if (product.getKey().equalsIgnoreCase(elementDataKey)) {
                    return product.getValue();
                }
            }
        }
        return super.findProduct(products);
    }

    @Override
    protected void processCloudEngine(
        FlowData data,
        CloudPassThroughData aspectData,
        String json) {
        JSONObject element = getElementJson(new JSONObject(json));
        Map<String, Object> values = element.toMap();
        List<ElementPropertyMetaData> metaData =
            new ArrayList<ElementPropertyMetaData>(getProperties());
        Map<String, Object> read = createAPVMap(values, metaData);
        // The cloud service leaves a part out of a response where it has
        // nothing for it, as it leaves out the browser tests' script once
        // the answers have come back. Each property of a part left out
        // has no value, with the reason, rather than being missing.
        Map<String, Object> lowerNames = new TreeMap<String, Object>(
            String.CASE_INSENSITIVE_ORDER);
        lowerNames.putAll(read);
        for (ElementPropertyMetaData property : metaData) {
            if (lowerNames.containsKey(property.getName()) == false) {
                AspectPropertyValue<Object> none =
                    new AspectPropertyValueDefault<>();
                none.setNoValueMessage(String.format(NOT_RETURNED,
                    property.getName(), elementDataKey));
                read.put(property.getName().toLowerCase(Locale.ROOT), none);
            }
        }
        aspectData.populateFromMap(read);
    }

    /**
     * The reason a property has no value where the cloud response left it
     * out.
     */
    static final String NOT_RETURNED = "The cloud service did not return "
        + "'%s' in '%s' for this request.";

    @Override
    protected void unmanagedResourcesCleanup() {
        // Nothing to clean up.
    }
}
