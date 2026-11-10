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

package fiftyone.geolocation.flowelements;

import fiftyone.geolocation.core.Enums;
import fiftyone.geolocation.data.CloudGeoData;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudAspectEngineBase;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;

import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.*;
import org.json.JSONArray;

public class GeoLocationCloudEngine extends CloudAspectEngineBase<CloudGeoData> {

    private CloudRequestEngine cloudRequestEngine = null;
    private volatile List<AspectPropertyMetaData> properties;
    private final Object metadataLock = new Object();
    private String dataSourceTier;
    private String elementDataKey;
    private EvidenceKeyFilter evidenceKeyFilter = new EvidenceKeyFilterWhitelist(Collections.emptyList());
    private String dataProviderPrefix;

    /**
     * The property metadata, from the products the Cloud Request Engine
     * reports. Until they are known, after a failure at build that can
     * pass, this is empty and is read again on the next call.
     * @return the property metadata
     */
    @Override
    public List<AspectPropertyMetaData> getProperties() {
        List<AspectPropertyMetaData> loaded = properties;
        if (loaded == null && cloudRequestEngine != null) {
            synchronized (metadataLock) {
                if (properties == null) {
                    loadAspectProperties(cloudRequestEngine);
                }
                loaded = properties;
            }
        }
        return loaded == null ?
            new ArrayList<AspectPropertyMetaData>() :
            loaded;
    }

    /**
     * Whether the property metadata is loaded, which reads it if the Cloud
     * Request Engine now knows the accessible properties.
     * @return true once the property metadata is loaded
     */
    @Override
    public boolean hasLoadedProperties() {
        getProperties();
        return properties != null;
    }

    @Override
    public String getDataSourceTier() {
        return dataSourceTier;
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        // This engine needs no evidence.
        // It works from the cloud request data.
        return evidenceKeyFilter;
    }

    @Override
    public String getElementDataKey() {
        return elementDataKey;
    }

    public String getDataProviderPrefix() {
        return dataProviderPrefix;
    }

    public GeoLocationCloudEngine(
        Logger logger,
        ElementDataFactory<CloudGeoData> aspectDataFactory,
        Enums.GeoLocationProvider provider) {
        super(logger, aspectDataFactory);

        switch (provider) {
            case DigitalElement:
                elementDataKey = "location_digitalelement";
                dataProviderPrefix = "DigitalElement";
                break;
            case FiftyOneDegrees:
                elementDataKey = "location";
                dataProviderPrefix = "OSM";
                break;
            default:
                throw new PipelineConfigurationException("provider ' +" +
                    provider + "' not supported by the geolocation cloud engine");
        }
    }

    @Override
    protected void processEngine(FlowData data, CloudGeoData aspectData) {
        if (cloudRequestEngine != null) {
            CloudRequestData requestData = data.getFromElement(cloudRequestEngine);
            String json = requestData.getJsonResponse();

            JSONObject jsonObj = new JSONObject(json);
            JSONObject geoObj = getElementJson(jsonObj);

            Map<String, Object> methodData = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for (AspectPropertyMetaData property : getProperties()) {
                switch(property.getType().getSimpleName()) {
                    case ("List"):
                        JSONArray jsonArray = geoObj.optJSONArray(property.getName().toLowerCase());
                        methodData.put(
                            property.getName(),
                            jsonArray == null ? null : jsonArray.toList());
                        break;
                    case ("JavaScript"):
                        String javascript = geoObj.optString(property.getName().toLowerCase(), null);
                        methodData.put(
                            property.getName(),
                            javascript == null ? null : new JavaScript(javascript));
                        break;
                    default:
                        methodData.put(
                            property.getName(),
                            geoObj.opt(property.getName().toLowerCase()));
                            break;
                }
            }

            aspectData.populateFromMap(methodData);

            Map<String, String> noValueReasonsMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for(Map.Entry<String, Object> entry : methodData.entrySet()){
                if("null".equals(String.valueOf(entry.getValue()))){
                    String key = entry.getKey().toLowerCase();
                    Object reason = tryToGet(geoObj, key);
                    noValueReasonsMap.put(entry.getKey().toLowerCase(), (reason == null ? "" : reason.toString()));
                }
            }

            aspectData.setNoValueReasons(noValueReasonsMap);
        }
        else {
            throw new PipelineConfigurationException(
                "The '" + getClass().getName() + "' requires a 'CloudRequestEngine' " +
                "before it in the Pipeline. This engine will be unable " +
                "to produce results until this is corrected.");
        }
    }

    public static Object tryToGet(JSONObject jsonObj, String key) {
        return jsonObj.opt(key + "nullreason");
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }
    
    @Override
    public void addPipeline(Pipeline pipeline) {
        if (cloudRequestEngine == null) {
            cloudRequestEngine = pipeline.getElement(CloudRequestEngine.class);
            if (cloudRequestEngine != null) {
                // The products are not known after a failure at build that
                // can pass, which the Cloud Request Engine has logged.
                // getProperties reads them once they are.
                loadAspectProperties(cloudRequestEngine);
            }

        }
        super.addPipeline(pipeline);
    }

    private boolean loadAspectProperties(CloudRequestEngine engine) {
        AccessiblePropertyMetaData.ProductMetaData product =
            findProduct(engine.getPublicProperties());

        if (product != null) {
            List<AspectPropertyMetaData> loaded = new ArrayList<>();
            dataSourceTier = product.dataTier;

            for (AccessiblePropertyMetaData.PropertyMetaData item : product.properties)
            {
                AspectPropertyMetaData property = new AspectPropertyMetaDataDefault(
                    item.name,
                    this,
                    item.category,
                    item.getPropertyType(),
                    new ArrayList<String>(),
                    true,
                    null,
                    item.delayExecution,
                    item.evidenceProperties);
                
                loaded.add(property);
            }
            properties = loaded;
            return true;
        }
        else {
            return false;
        }
    }
}
