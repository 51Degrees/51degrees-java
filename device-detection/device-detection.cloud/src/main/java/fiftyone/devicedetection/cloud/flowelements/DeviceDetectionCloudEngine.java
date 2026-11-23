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

package fiftyone.devicedetection.cloud.flowelements;

import fiftyone.devicedetection.cloud.data.DeviceDataCloud;
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

import static fiftyone.pipeline.cloudrequestengine.Constants.Messages.NoCloudResponse;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.json.JSONArray;

/**
 * Engine that takes the JSON response from the {@link CloudRequestEngine} and
 * uses it populate a {@link DeviceDataCloud} instance for easier consumption.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/device-detection-specification/pipeline-elements/device-detection-cloud.md">Specification</a>
 */
public class DeviceDetectionCloudEngine
    extends CloudAspectEngineBase<DeviceDataCloud> {
    /**
     * The type of value each typed accessor of the device data gives, keyed
     * by the property name in lower case, so that a value the metadata does
     * not describe can be given as the type its accessor declares.
     */
    private static final Map<String, Type> DECLARED_TYPES = declaredTypes();

    volatile List<AspectPropertyMetaData> aspectProperties;
    private String dataSourceTier;
    CloudRequestEngine cloudRequestEngine;
    private final Object metadataLock = new Object();

    /**
     * Construct a new instance of the {@link DeviceDetectionCloudEngine}.
     * @param logger logger instance to use for logging
     * @param deviceDataFactory the factory to use when creating a
     *                          {@link DeviceDataCloud} instance
     */
    public DeviceDetectionCloudEngine(
        Logger logger,
        ElementDataFactory<DeviceDataCloud> deviceDataFactory) {
        super(logger, deviceDataFactory);
        this.cloudRequestEngine = null;
    }

    /**
     * The property metadata, from the products the Cloud Request Engine
     * reports. Until they are known, after a failure at build that can
     * pass, this is empty and is read again on the next call.
     * @return the property metadata
     */
    @Override
    public List<AspectPropertyMetaData> getProperties() {
        List<AspectPropertyMetaData> loaded = aspectProperties;
        if (loaded == null && cloudRequestEngine != null) {
            synchronized (metadataLock) {
                if (aspectProperties == null) {
                    loadAspectProperties(cloudRequestEngine);
                }
                loaded = aspectProperties;
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
        return aspectProperties != null;
    }

    @Override
    public String getDataSourceTier() {
        return dataSourceTier;
    }

    @Override
    public String getElementDataKey() {
        return "device";
    }

    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        // This engine needs no evidence.
        // It works from the cloud request data.
        return new EvidenceKeyFilterWhitelist(new ArrayList<String>());
    }

    @Override
    protected void processEngine(FlowData data, DeviceDataCloud aspectData) {
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
            String json = getJsonResponse(
                data,
                aspectData,
                requestData,
                cloudRequestEngine);

            // A failed cloud request leaves no response. The Cloud Request
            // Engine reports why, so this engine adds no error of its own.
            if (json == null || json.isEmpty()) {
                logger.warn(String.format(
                    NoCloudResponse, getClass().getSimpleName()));
                return;
            }

            // Extract data from json to the aspectData instance.
            JSONObject jsonObj = new JSONObject(json);
            JSONObject deviceObj = getElementJson(jsonObj);

            Map<String, Object> deviceMap =
                new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            for (AspectPropertyMetaData property : getProperties()) {
                // A property the response leaves out has no entry, so
                // reading it throws a PropertyMissingException.
                if (deviceObj.has(
                    property.getName().toLowerCase(Locale.ROOT)) == false) {
                    continue;
                }
                String type = property.getType().getSimpleName();
                switch(type) {
                    case ("List"):
                        deviceMap.put(
                            property.getName(),
                            getListAspectPropertyValue(deviceObj, property));
                        break;
                    case ("JavaScript"):
                        deviceMap.put(
                            property.getName(),
                            getJavaScriptAspectPropertyValue(deviceObj, property));
                        break;
                    case ("String"):
                        deviceMap.put(
                            property.getName(),
                            getStringAspectPropertyValue(deviceObj, property));
                            break;
                    case ("boolean"):
                        deviceMap.put(
                            property.getName(),
                            getBooleanAspectPropertyValue(deviceObj, property));
                        break;
                    case ("int"):
                        deviceMap.put(
                            property.getName(),
                            getIntegerAspectPropertyValue(deviceObj, property));
                        break;
                    case ("double"):
                        deviceMap.put(
                            property.getName(),
                            getDoubleAspectPropertyValue(deviceObj, property));
                        break;
                    default:
                        deviceMap.put(
                            property.getName(),
                            getStringAspectPropertyValue(deviceObj, property));
                            break;
                }
            }

            // A value the metadata does not describe, as when the metadata
            // is empty before discovery has completed, takes its type from
            // the value itself.
            for (String key : deviceObj.keySet()) {
                if (key.endsWith("nullreason") == false &&
                    deviceMap.containsKey(key) == false) {
                    deviceMap.put(
                        key,
                        getInferredAspectPropertyValue(deviceObj, key));
                }
            }

            // Add no value messages for any properties which have not yet been
            // populated.
            for (String key : deviceObj.keySet()) {
                if (key.endsWith("nullreason")) {
                    String actualKey = key
                        .replace("nullreason", "");
                    if (deviceMap.containsKey(actualKey) == false) {
                        AspectPropertyValue<?> nullValue =
                            new AspectPropertyValueDefault<Object>();
                        nullValue.setNoValueMessage(
                            deviceObj.optString(key, "Unknown"));
                        deviceMap.put(
                            actualKey,
                            nullValue);
                    }
                }
            }

            aspectData.populateFromMap(deviceMap);
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
                // The products are not known after a failure at build that
                // can pass, which the Cloud Request Engine has logged.
                // getProperties reads them once they are.
                loadAspectProperties(cloudRequestEngine);
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
        AccessiblePropertyMetaData.ProductMetaData product =
            findProduct(engine.getPublicProperties());

        if (product != null) {
            List<AspectPropertyMetaData> properties = new ArrayList<>();
            dataSourceTier = product.dataTier;

            for (AccessiblePropertyMetaData.PropertyMetaData item :
                product.properties) {
                AspectPropertyMetaData property = new AspectPropertyMetaDataDefault(
                    item.name,
                    this,
                    item.category,
                    item.getPropertyType(),
                    new ArrayList<String>(),
                    true);
                properties.add(property);
            }
            aspectProperties = properties;
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Get the integer representation of a value from the cloud engine's JSON
     * response, and wrap it in an {@link AspectPropertyValue}.
     * @param deviceObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} with a parsed value, or the reason
     * for the value not being present
     */
    private AspectPropertyValue<Integer> getIntegerAspectPropertyValue(
        JSONObject deviceObj,
        AspectPropertyMetaData property) {
        String key = property.getName().toLowerCase(Locale.ROOT);
        AspectPropertyValue<Integer> intValue = new AspectPropertyValueDefault<>();
        if(deviceObj.isNull(key)){
            intValue.setNoValueMessage(getNoValueReason(deviceObj, key));
        }
        else {
            intValue.setValue(deviceObj.getInt(key));
        }
        return intValue;
    }

    private AspectPropertyValue<Double> getDoubleAspectPropertyValue(JSONObject deviceObj, AspectPropertyMetaData property) {
        String key = property.getName().toLowerCase(Locale.ROOT);
        AspectPropertyValue<Double> doubleValue = new AspectPropertyValueDefault<>();
        if(deviceObj.isNull(key)){
            doubleValue.setNoValueMessage(getNoValueReason(deviceObj, key));
        }
        else {
            doubleValue.setValue(deviceObj.getDouble(key));
        }
        return doubleValue;
    }

    /**
     * Get the boolean representation of a value from the cloud engine's JSON
     * response, and wrap it in an {@link AspectPropertyValue}.
     * @param deviceObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} with a parsed value, or the reason
     * for the value not being present
     */
    private AspectPropertyValue<Boolean> getBooleanAspectPropertyValue(
        JSONObject deviceObj,
        AspectPropertyMetaData property) {
        String key = property.getName().toLowerCase(Locale.ROOT);
        AspectPropertyValue<Boolean> booleanValue = new AspectPropertyValueDefault<>();
        if(deviceObj.isNull(key)){
            booleanValue.setNoValueMessage(getNoValueReason(deviceObj, key));
        }
        else {
            booleanValue.setValue(deviceObj.getBoolean(key));
        }
        return booleanValue;
    }

    /**
     * Get the string list representation of a value from the cloud engine's
     * JSON response, and wrap it in an {@link AspectPropertyValue}.
     * @param deviceObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} with a parsed value, or the reason
     * for the value not being present
     */
    private AspectPropertyValue<List<String>> getListAspectPropertyValue(
        JSONObject deviceObj,
        AspectPropertyMetaData property){
        String key = property.getName().toLowerCase(Locale.ROOT);
        AspectPropertyValue<List<String>> listValue = new AspectPropertyValueDefault<>();
        if(deviceObj.isNull(key))
        {
            listValue.setNoValueMessage(getNoValueReason(deviceObj, key));
        }
        else 
        {
            JSONArray jsonArray = deviceObj.getJSONArray(key);
            List<String> strings = new ArrayList<>(jsonArray.length());
            for (Object object : jsonArray.toList()) {
                strings.add(Objects.toString(object, null));
            }
            listValue.setValue(strings);
        }
        return listValue;
    }

    /**
     * Get the JavaScript representation of a value from the cloud engine's JSON
     * response, and wrap it in an {@link AspectPropertyValue}.
     * @param deviceObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} with a parsed value, or the reason
     * for the value not being present
     */
    private AspectPropertyValue<JavaScript> getJavaScriptAspectPropertyValue(
        JSONObject deviceObj,
        AspectPropertyMetaData property){
        String key = property.getName().toLowerCase(Locale.ROOT);
        AspectPropertyValue<JavaScript> jsValue = new AspectPropertyValueDefault<>();
        if(deviceObj.isNull(key)){
            jsValue.setNoValueMessage(getNoValueReason(deviceObj, key));
        } else {
            jsValue.setValue(new JavaScript(deviceObj.getString(key)));
        }
        return jsValue;
    }

    /**
     * Get the string representation of a value from the cloud engine's JSON
     * response, and wrap it in an {@link AspectPropertyValue}.
     * @param deviceObj to get the value from
     * @param property to get the value of
     * @return {@link AspectPropertyValue} with a parsed value, or the reason
     * for the value not being present
     */
    private AspectPropertyValue<String> getStringAspectPropertyValue(
        JSONObject deviceObj,
        AspectPropertyMetaData property){
        String key = property.getName().toLowerCase(Locale.ROOT);
        AspectPropertyValue<String> stringValue = new AspectPropertyValueDefault<>();
        if(deviceObj.isNull(key)){
            stringValue.setNoValueMessage(getNoValueReason(deviceObj, key));
        }
        else {
            stringValue.setValue(deviceObj.getString(key));
        }
        return stringValue;
    }

    /**
     * Get a value from the cloud engine's JSON response that the metadata
     * does not describe, with its type taken from the value: a string, a
     * boolean, a number, a list of strings, or an object as a map. A
     * number is an Integer where the typed accessor of
     * {@link DeviceDataCloud} declares one and the number is a whole
     * number an Integer can hold, and a Double where the accessor declares
     * one or the number has a decimal point. A null is no value, with the
     * reason the response gives.
     * @param deviceObj to get the value from
     * @param key to get the value of
     * @return {@link AspectPropertyValue} with the value, or the reason for
     * the value not being present
     */
    private AspectPropertyValue<?> getInferredAspectPropertyValue(
        JSONObject deviceObj,
        String key) {
        Object value = deviceObj.opt(key);
        if (value == null || value == JSONObject.NULL) {
            AspectPropertyValue<Object> noValue =
                new AspectPropertyValueDefault<>();
            noValue.setNoValueMessage(getNoValueReason(deviceObj, key));
            return noValue;
        }
        if (value instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) value;
            List<String> strings = new ArrayList<>(jsonArray.length());
            for (Object item : jsonArray.toList()) {
                strings.add(Objects.toString(item, null));
            }
            return new AspectPropertyValueDefault<>(strings);
        }
        if (value instanceof JSONObject) {
            return new AspectPropertyValueDefault<>(
                ((JSONObject) value).toMap());
        }
        if (value instanceof Number) {
            Number number = (Number) value;
            Type declared = DECLARED_TYPES.get(key.toLowerCase(Locale.ROOT));
            if (Integer.class.equals(declared) && isWholeInteger(number)) {
                return new AspectPropertyValueDefault<>(number.intValue());
            }
            if (Double.class.equals(declared) ||
                value instanceof BigDecimal ||
                value instanceof Double ||
                value instanceof Float) {
                return new AspectPropertyValueDefault<>(
                    number.doubleValue());
            }
        }
        return new AspectPropertyValueDefault<>(value);
    }

    /**
     * True when the number is a whole number an Integer can hold, such as
     * 24 or 24.0, and false for 24.5 or a number beyond an Integer's range.
     * @param number the number
     * @return true if the number can be an Integer without changing it
     */
    private static boolean isWholeInteger(Number number) {
        double value = number.doubleValue();
        return value == Math.rint(value) &&
            value >= Integer.MIN_VALUE &&
            value <= Integer.MAX_VALUE;
    }

    /**
     * The type of value each typed accessor of {@link DeviceDataCloud}
     * gives, being the type argument of the {@link AspectPropertyValue} it
     * returns, keyed by the property name in lower case.
     * @return the declared types
     */
    private static Map<String, Type> declaredTypes() {
        Map<String, Type> types = new HashMap<>();
        for (Method method : DeviceDataCloud.class.getMethods()) {
            Type returned = method.getGenericReturnType();
            if (method.getParameterCount() == 0 &&
                method.getName().startsWith("get") &&
                AspectPropertyValue.class.equals(method.getReturnType()) &&
                returned instanceof ParameterizedType) {
                types.put(
                    method.getName().substring(3).toLowerCase(Locale.ROOT),
                    ((ParameterizedType) returned).getActualTypeArguments()[0]);
            }
        }
        return Collections.unmodifiableMap(types);
    }

    /**
     * The message for a property with no value when the response gives no
     * reason.
     */
    private static final String UNKNOWN_NULL_REASON = "Unknown";

    /**
     * Get the reason for the value of a property not being present in the cloud
     * engine's JSON response.
     * @param deviceObj to get the null reason from
     * @param key to get the null reason from
     * @return the reason for the missing property
     */
    private String getNoValueReason(JSONObject deviceObj, String key){
        Object reason = tryToGet(deviceObj, key + "nullreason");
        return reason == null || reason == JSONObject.NULL ?
            UNKNOWN_NULL_REASON :
            reason.toString();
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
