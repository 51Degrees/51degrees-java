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

package fiftyone.ipintelligence.cloud.flowelements;

import fiftyone.ipintelligence.cloud.data.IPIntelligenceDataCloud;
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
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import org.json.JSONObject;
import org.slf4j.Logger;

import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.WeightedValue;
import fiftyone.pipeline.core.data.WktString;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.json.JSONArray;

/**
 * Engine that takes the JSON response from the {@link CloudRequestEngine} and
 * uses it to populate a {@link IPIntelligenceDataCloud} instance for easier
 * consumption.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/ip-intelligence-specification/pipeline-elements/ip-intelligence-cloud.md">Specification</a>
 */
public class IPIntelligenceCloudEngine
    extends CloudAspectEngineBase<IPIntelligenceDataCloud> {
    List<AspectPropertyMetaData> aspectProperties;
    private String dataSourceTier;
    // Lower-cased names of properties the cloud returns as weighted lists
    // (cloud type "Weighted..."), so processEngine can read their value/weight
    // pairs rather than treating them as a plain list.
    private final Set<String> weightedPropertyNames = new HashSet<>();
    CloudRequestEngine cloudRequestEngine;

    /**
     * Construct a new instance of the {@link IPIntelligenceCloudEngine}.
     * @param logger logger instance to use for logging
     * @param ipiDataFactory the factory to use when creating a
     *                          {@link IPIntelligenceDataCloud} instance
     */
    public IPIntelligenceCloudEngine(
        Logger logger,
        ElementDataFactory<IPIntelligenceDataCloud> ipiDataFactory) {
        super(logger, ipiDataFactory);
        this.cloudRequestEngine = null;
    }

    @Override
    public List<AspectPropertyMetaData> getProperties() {
        return aspectProperties;
    }

    @Override
    public String getDataSourceTier() {
        return dataSourceTier;
    }

    @Override
    public String getElementDataKey() {
        return "ip";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        // This engine needs no evidence.
        // It works from the cloud request data.
        return new EvidenceKeyFilterWhitelist(new ArrayList<String>());
    }

    @Override
    protected void processEngine(FlowData data, IPIntelligenceDataCloud aspectData) {
        if (cloudRequestEngine == null) {
            throw new PipelineConfigurationException(
                "The '" + getClass().getName() + "' requires a " +
                    "'CloudRequestEngine' before it in the Pipeline. This " +
                    "engine will be unable to produce results until this is " +
                    "corrected.");
        }
        else {
            CloudRequestData requestData =
                data.getFromElement(cloudRequestEngine);
            String json = "";
            json = requestData.getJsonResponse();

            // Extract data from json to the aspectData instance.
            JSONObject jsonObj = new JSONObject(json);
            JSONObject ipiObj = jsonObj.getJSONObject("ip");

            Map<String, Object> propertyMap =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for (AspectPropertyMetaData property : getProperties()) {
                if (weightedPropertyNames.contains(
                        property.getName().toLowerCase())) {
                    propertyMap.put(
                        property.getName(),
                        getWeightedListAspectPropertyValue(ipiObj, property));
                    continue;
                }
                String type = property.getType().getSimpleName();
                switch(type) {
                    case ("List"):
                        propertyMap.put(
                            property.getName(),
                            getListAspectPropertyValue(ipiObj, property));
                        break;
                    case ("JavaScript"):
                        propertyMap.put(
                            property.getName(),
                            getJavaScriptAspectPropertyValue(ipiObj, property));
                        break;
                    case ("String"):
                        propertyMap.put(
                            property.getName(),
                            getStringAspectPropertyValue(ipiObj, property));
                            break;
                    case ("InetAddress"):
                        propertyMap.put(
                            property.getName(),
                            getInetAddressAspectPropertyValue(ipiObj, property));
                        break;
                    case ("WktString"):
                        propertyMap.put(
                            property.getName(),
                            getWktStringAspectPropertyValue(ipiObj, property));
                        break;
                    case ("boolean"):
                        propertyMap.put(
                            property.getName(),
                            getBooleanAspectPropertyValue(ipiObj, property));
                        break;
                    case ("int"):
                        propertyMap.put(
                            property.getName(),
                            getIntegerAspectPropertyValue(ipiObj, property));
                        break;
                    case ("double"):
                        propertyMap.put(
                            property.getName(),
                            getDoubleAspectPropertyValue(ipiObj, property));
                        break;
                    case ("Float"):
                        propertyMap.put(
                            property.getName(),
                            getFloatAspectPropertyValue(ipiObj, property));
                        break;
                    default:
                        propertyMap.put(
                            property.getName(),
                            getStringAspectPropertyValue(ipiObj, property));
                            break;
                }
            }

            // Add no value messages for any properties which have not yet been
            // populated.
            for (String key : ipiObj.keySet()) {
                if (key.endsWith("nullreason")) {
                    String actualKey = key
                        .replace("nullreason", "");
                    if (propertyMap.containsKey(actualKey) == false) {
                        AspectPropertyValue<?> nullValue =
                            new AspectPropertyValueDefault<Object>();
                        nullValue.setNoValueMessage(
                            ipiObj.optString(key, "Unknown"));
                        propertyMap.put(
                            actualKey,
                            nullValue);
                    }
                }
            }

            aspectData.populateFromMap(propertyMap);
        }
    }

    @Override
    protected void unmanagedResourcesCleanup() {
    }

    @Override
    public void addPipeline(Pipeline pipeline) {
        if (cloudRequestEngine == null) {
            cloudRequestEngine = pipeline.getElement(CloudRequestEngine.class);
            if (cloudRequestEngine != null) {
                if (loadAspectProperties(cloudRequestEngine) == false) {
                    logger.error("Failed to load aspect properties");
                }
            }

        }
        super.addPipeline(pipeline);
    }

    /**
     * Load all aspect property meta data using the properties returned by the
     * cloud engine ({@link CloudRequestEngine#getPublicProperties()}.
     * @param engine cloud request engine
     * @return true if the properties were successfully loaded into
     * {@link #aspectProperties}
     */
    private boolean loadAspectProperties(CloudRequestEngine engine) {
        Map<String, AccessiblePropertyMetaData.ProductMetaData> map =
            engine.getPublicProperties();

        if (map != null &&
            map.size() > 0 &&
            map.containsKey(getElementDataKey())) {
            aspectProperties = new ArrayList<>();
            weightedPropertyNames.clear();
            dataSourceTier = map.get(getElementDataKey()).dataTier;

            for (AccessiblePropertyMetaData.PropertyMetaData item :
                map.get(getElementDataKey()).properties) {
                if (item.type != null && item.type.startsWith("Weighted")) {
                    weightedPropertyNames.add(item.name.toLowerCase());
                }
                AspectPropertyMetaData property = new AspectPropertyMetaDataDefault(
                    item.name,
                    this,
                    item.category,
                    resolvePropertyType(item),
                    new ArrayList<String>(),
                    true);
                aspectProperties.add(property);
            }
            return true;
        }
        else {
            logger.error("Aspect properties could not be loaded for" +
                " the IP Intelligence cloud engine", this);
            return false;
        }
    }

    /**
     * Resolve the Java type for a cloud property. The shared
     * {@link AccessiblePropertyMetaData.PropertyMetaData#getPropertyType()}
     * only knows a fixed set of scalar types, so the IP Intelligence specific
     * cloud types are mapped here (mirroring the device-detection-dotnet
     * approach of resolving weighted types in the engine):
     * <ul>
     *   <li>Weighted values (e.g. {@code WeightedString}) are returned by the
     *       cloud as a JSON array of value/weight pairs, so they are mapped to
     *       {@link List} and read by the existing list handling.</li>
     *   <li>{@code IPAddress} is parsed into an {@link InetAddress} so the
 *       typed getters (e.g. {@code getIp()}) return the expected type.</li>
     *   <li>{@code WktString} (e.g. the {@code Areas} property) is wrapped in a
     *       {@link WktString} so the typed getter ({@code getAreas()}) returns
     *       the expected type.</li>
     * </ul>
     * Any other type is delegated to the shared metadata mapping, which only
     * knows the scalar cloud types and throws on anything else - so every IP
     * Intelligence specific cloud type must be handled here (mirroring
     * {@code IpiCloudEngine.GetPropertyType} in ip-intelligence-dotnet).
     * @param item the cloud property meta data
     * @return the Java type to use for the property
     */
    private Class<?> resolvePropertyType(
        AccessiblePropertyMetaData.PropertyMetaData item) {
        if (item.type != null) {
            if (item.type.startsWith("Weighted")) {
                return List.class;
            }
            if (item.type.equals("IPAddress")) {
                return InetAddress.class;
            }
            if (item.type.equals("WktString")) {
                return WktString.class;
            }
            // The cloud is a .NET service and reports 32-bit floats (e.g.
            // Latitude/Longitude) as "Single"; the shared mapping only knows
            // "Double", so map it here to the Float the typed getters expect.
            if (item.type.equals("Single")) {
                return Float.class;
            }
        }
        return item.getPropertyType();
    }

    /**
     * Get the integer representation of a value from the cloud engine's JSON
     * response, and wrap it in an {@link AspectPropertyValue}.
     * @param ipiObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} with a parsed value, or the reason
     * for the value not being present
     */
    private AspectPropertyValue<Integer> getIntegerAspectPropertyValue(
        JSONObject ipiObj,
        AspectPropertyMetaData property) {
        String key = property.getName().toLowerCase();
        AspectPropertyValue<Integer> intValue = new AspectPropertyValueDefault<>();
        if(ipiObj.isNull(key)){
            intValue.setNoValueMessage(getNoValueReason(ipiObj, key));
        }
        else {
            intValue.setValue(ipiObj.getInt(key));
        }
        return intValue;
    }

    private AspectPropertyValue<Double> getDoubleAspectPropertyValue(JSONObject ipiObj, AspectPropertyMetaData property) {
        String key = property.getName().toLowerCase();
        AspectPropertyValue<Double> doubleValue = new AspectPropertyValueDefault<>();
        if(ipiObj.isNull(key)){
            doubleValue.setNoValueMessage(getNoValueReason(ipiObj, key));
        }
        else {
            doubleValue.setValue(ipiObj.getDouble(key));
        }
        return doubleValue;
    }

    /**
     * Get the {@link Float} representation of a value from the cloud engine's
     * JSON response, and wrap it in an {@link AspectPropertyValue}. Used for
     * cloud "Single" properties (e.g. Latitude/Longitude).
     * @param ipiObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} with a parsed value, or the reason
     * for the value not being present
     */
    private AspectPropertyValue<Float> getFloatAspectPropertyValue(JSONObject ipiObj, AspectPropertyMetaData property) {
        String key = property.getName().toLowerCase();
        AspectPropertyValue<Float> floatValue = new AspectPropertyValueDefault<>();
        if(ipiObj.isNull(key)){
            floatValue.setNoValueMessage(getNoValueReason(ipiObj, key));
        }
        else {
            floatValue.setValue(ipiObj.getFloat(key));
        }
        return floatValue;
    }

    /**
     * Get the boolean representation of a value from the cloud engine's JSON
     * response, and wrap it in an {@link AspectPropertyValue}.
     * @param ipiObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} with a parsed value, or the reason
     * for the value not being present
     */
    private AspectPropertyValue<Boolean> getBooleanAspectPropertyValue(
        JSONObject ipiObj,
        AspectPropertyMetaData property) {
        String key = property.getName().toLowerCase();
        AspectPropertyValue<Boolean> booleanValue = new AspectPropertyValueDefault<>();
        if(ipiObj.isNull(key)){
            booleanValue.setNoValueMessage(getNoValueReason(ipiObj, key));
        }
        else {
            booleanValue.setValue(ipiObj.getBoolean(key));
        }
        return booleanValue;
    }

    /**
     * Get the string list representation of a value from the cloud engine's
     * JSON response, and wrap it in an {@link AspectPropertyValue}.
     * @param ipiObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} with a parsed value, or the reason
     * for the value not being present
     */
    private AspectPropertyValue<List<String>> getListAspectPropertyValue(
        JSONObject ipiObj,
        AspectPropertyMetaData property){
        String key = property.getName().toLowerCase();
        AspectPropertyValue<List<String>> listValue = new AspectPropertyValueDefault<>();
        if(ipiObj.isNull(key))
        {
            listValue.setNoValueMessage(getNoValueReason(ipiObj, key));
        }
        else 
        {
            JSONArray jsonArray = ipiObj.getJSONArray(key);
            List<String> strings = new ArrayList<>(jsonArray.length());
            for (Object object : jsonArray.toList()) {
                strings.add(Objects.toString(object, null));
            }
            listValue.setValue(strings);
        }
        return listValue;
    }

    /**
     * Get the weighted-string-list representation of a value from the cloud
     * engine's JSON response, and wrap it in an {@link AspectPropertyValue}.
     * The cloud returns each weighted value as a JSON object with a
     * {@code rawweighting} and a {@code value}, e.g.
     * <code>[{"rawweighting":59439,"value":"GB"}, ...]</code>.
     * @param deviceObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} containing a list of
     * {@link IWeightedValue}, or the reason for the value not being present
     */
    private AspectPropertyValue<List<IWeightedValue<String>>>
        getWeightedListAspectPropertyValue(
            JSONObject deviceObj,
            AspectPropertyMetaData property) {
        String key = property.getName().toLowerCase();
        AspectPropertyValue<List<IWeightedValue<String>>> weightedValue =
            new AspectPropertyValueDefault<>();
        if (deviceObj.isNull(key)) {
            weightedValue.setNoValueMessage(getNoValueReason(deviceObj, key));
        }
        else {
            JSONArray jsonArray = deviceObj.getJSONArray(key);
            List<IWeightedValue<String>> values =
                new ArrayList<>(jsonArray.length());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject element = jsonArray.getJSONObject(i);
                values.add(new WeightedValue<String>(
                    element.getInt("rawweighting"),
                    Objects.toString(element.get("value"), null)));
            }
            weightedValue.setValue(values);
        }
        return weightedValue;
    }

    /**
     * Get the {@link InetAddress} representation of a value from the cloud
     * engine's JSON response, and wrap it in an {@link AspectPropertyValue}.
     * The cloud reports IP addresses as plain strings; they are parsed with
     * {@link InetAddress#getByName(String)} (which parses an IP literal without
     * a DNS lookup), mirroring the on-premise engine.
     * @param deviceObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} with a parsed value, or the reason
     * for the value not being present
     */
    private AspectPropertyValue<InetAddress> getInetAddressAspectPropertyValue(
        JSONObject deviceObj,
        AspectPropertyMetaData property) {
        String key = property.getName().toLowerCase();
        AspectPropertyValue<InetAddress> ipValue = new AspectPropertyValueDefault<>();
        if (deviceObj.isNull(key)) {
            ipValue.setNoValueMessage(getNoValueReason(deviceObj, key));
        }
        else {
            try {
                ipValue.setValue(InetAddress.getByName(deviceObj.getString(key)));
            } catch (UnknownHostException e) {
                ipValue.setNoValueMessage(e.getMessage());
            }
        }
        return ipValue;
    }

    /**
     * Get the JavaScript representation of a value from the cloud engine's JSON
     * response, and wrap it in an {@link AspectPropertyValue}.
     * @param ipiObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} with a parsed value, or the reason
     * for the value not being present
     */
    private AspectPropertyValue<JavaScript> getJavaScriptAspectPropertyValue(
        JSONObject ipiObj,
        AspectPropertyMetaData property){
        String key = property.getName().toLowerCase();
        AspectPropertyValue<JavaScript> jsValue = new AspectPropertyValueDefault<>();
        if(ipiObj.isNull(key)){
            jsValue.setNoValueMessage(getNoValueReason(ipiObj, key));
        } else {
            jsValue.setValue(new JavaScript(ipiObj.getString(key)));
        }
        return jsValue;
    }

    /**
     * Get the {@link WktString} representation of a value from the cloud
     * engine's JSON response, and wrap it in an {@link AspectPropertyValue}.
     * The cloud serialises WKT geometry (e.g. the {@code Areas} property) as an
     * object wrapping the text under a {@code "value"} key, so the text is
     * extracted and wrapped in a {@link WktString} to match the typed getter
     * ({@code getAreas()}).
     * @param ipiObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} with a parsed value, or the reason
     * for the value not being present
     */
    private AspectPropertyValue<WktString> getWktStringAspectPropertyValue(
        JSONObject ipiObj,
        AspectPropertyMetaData property){
        String key = property.getName().toLowerCase();
        AspectPropertyValue<WktString> wktValue = new AspectPropertyValueDefault<>();
        if(ipiObj.isNull(key)){
            wktValue.setNoValueMessage(getNoValueReason(ipiObj, key));
        } else {
            // The cloud serialises WKT geometry as an object that wraps the
            // text under a "value" key, e.g. {"value":"POLYGON((...))"};
            // fall back to a plain string for forward compatibility.
            Object raw = ipiObj.get(key);
            String wkt = (raw instanceof JSONObject)
                ? ((JSONObject) raw).getString("value")
                : ipiObj.getString(key);
            wktValue.setValue(new WktString(wkt));
        }
        return wktValue;
    }

    /**
     * Get the string representation of a value from the cloud engine's JSON
     * response, and wrap it in an {@link AspectPropertyValue}.
     * @param ipiObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} with a parsed value, or the reason
     * for the value not being present
     */
    private AspectPropertyValue<String> getStringAspectPropertyValue(
        JSONObject ipiObj,
        AspectPropertyMetaData property){
        String key = property.getName().toLowerCase();
        AspectPropertyValue<String> stringValue = new AspectPropertyValueDefault<>();
        if(ipiObj.isNull(key)){
            stringValue.setNoValueMessage(getNoValueReason(ipiObj, key));
        }
        else {
            stringValue.setValue(ipiObj.getString(key));
        }
        return stringValue;
    }

    /**
     * Get the reason for the value of a property not being present in the cloud
     * engine's JSON response.
     * @param ipiObj to get the null reason from
     * @param key to get the null reason from
     * @return the reason for the missing property
     */
    private String getNoValueReason(JSONObject ipiObj, String key){
        Object reason = tryToGet(ipiObj, key + "nullreason");
        return reason == null ? null : reason.toString();
    }

    /**
     * Try to get a value from a JSON object.
     * @param jsonObj to get the value from
     * @param key to get the value of
     * @return the value or null
     */
    private Object tryToGet(JSONObject jsonObj, String key) {
        if (jsonObj.has(key))
            return jsonObj.opt(key);
        return null;
    }
}
