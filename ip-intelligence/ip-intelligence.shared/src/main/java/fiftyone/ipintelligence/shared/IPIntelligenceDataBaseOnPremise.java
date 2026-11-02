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

package fiftyone.ipintelligence.shared;

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.core.data.WktString;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static fiftyone.pipeline.util.Types.getPrimitiveTypeMap;

/**
 * Base class used for all 51Degrees on-premise IP Intelligence results classes.
 */
public abstract class IPIntelligenceDataBaseOnPremise extends IPIntelligenceDataBase {

    protected final Map<Class<?>, Class<?>> primitiveTypes;
    private final Object dataLock = new Object();
    private final Object getLock = new Object();
    private boolean mapPopulated = false;

    /**
     * Constructs a new instance.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     * @param engine the engine which created the instance
     * @param missingPropertyService service used to determine the reason for
     *                               a property value being missing
     */
    protected IPIntelligenceDataBaseOnPremise(
        Logger logger,
        FlowData flowData,
        AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine,
        MissingPropertyService missingPropertyService) {
        super(logger, flowData, engine, missingPropertyService);
        primitiveTypes = getPrimitiveTypeMap();
    }

    /**
     * Determine whether a property is available to return values from the
     * underlying results.
     * @param propertyName name of the property to check
     * @return true if the property is available
     */
    protected abstract boolean propertyIsAvailable(String propertyName);

    /**
     * Get the values for the specified property as a {@link List}.
     * @param propertyName name of the property to get values for
     * @param parametrizedTypes types parametrizing {@link AspectPropertyValue} starting with {@link List}
     * @return values as a list
     */
    public abstract AspectPropertyValue<List<?>> getValues(
        String propertyName,
        Class<?>[] parametrizedTypes);

    /**
     * Get the value for the specified property as a {@link String}.
     * @param propertyName name of the property to the the value for
     * @return value as a string
     */
    protected abstract AspectPropertyValue<String> getValueAsString(
        String propertyName);

    /**
     * Get the value for the specified property as an {@link Integer}. If the
     * property cannot be represented as an {@link Integer}, then the result
     * will have no value i.e. {@link AspectPropertyValue#hasValue()} == false.
     * @param propertyName name of the property to get the value for
     * @return value a an integer
     */
    protected abstract AspectPropertyValue<Integer> getValueAsInteger(
        String propertyName);

    /**
     * Get the value for the specified property as a {@link Boolean}. If the
     * property cannot be represented as a {@link Boolean}, then the result
     * will have no value i.e. {@link AspectPropertyValue#hasValue()} == false.
     * @param propertyName name of the property to get the value for
     * @return value a an integer
     */
    protected abstract AspectPropertyValue<Boolean> getValueAsBool(
        String propertyName);

    /**
     * Get the value for the specified property as a {@link Double}. If the
     * property cannot be represented as a {@link Double}, then the result
     * will have no value i.e. {@link AspectPropertyValue#hasValue()} == false.
     * @param propertyName name of the property to get the value for
     * @return value a an integer
     */
    protected abstract AspectPropertyValue<Double> getValueAsDouble(
        String propertyName);

    /**
     * Get the value for the specified property as a {@link JavaScript}.
     * @param propertyName name of the property to the the value for
     * @return value as a JavaScript
     */
    protected abstract AspectPropertyValue<JavaScript> getValueAsJavaScript(
        String propertyName);

    /**
     * Get the value for the specified property as a {@link Float}. Used for
     * {@code single} (32-bit float) properties such as Latitude/Longitude. The
     * native engine exposes numeric values as doubles, so this is derived from
     * {@link #getValueAsDouble(String)} rather than adding a new native binding
     * or abstract method.
     * @param propertyName name of the property to get the value for
     * @return value as a float
     */
    protected AspectPropertyValue<Float> getValueAsFloat(String propertyName) {
        AspectPropertyValue<Double> dbl = getValueAsDouble(propertyName);
        AspectPropertyValue<Float> result = new AspectPropertyValueDefault<>();
        if (dbl.hasValue()) {
            result.setValue((float) (double) dbl.getValue());
        } else {
            result.setNoValueMessage(dbl.getNoValueMessage());
        }
        return result;
    }

    /**
     * Get the value for the specified property as an {@link InetAddress}. Used
     * for {@code ipaddress} properties (e.g. Ip, IpRangeStart/End). The native
     * engine returns the address as its string form, so this parses that string
     * with {@link InetAddress#getByName(String)} (which resolves an IP literal
     * without performing a DNS lookup).
     * @param propertyName name of the property to get the value for
     * @return value as an InetAddress
     */
    protected AspectPropertyValue<InetAddress> getValueAsIpAddress(String propertyName) {
        AspectPropertyValue<String> str = getValueAsString(propertyName);
        AspectPropertyValue<InetAddress> result = new AspectPropertyValueDefault<>();
        if (str.hasValue()) {
            try {
                result.setValue(InetAddress.getByName(str.getValue()));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        } else {
            result.setNoValueMessage(str.getNoValueMessage());
        }
        return result;
    }

    /**
     * Get the value for the specified property as a {@link WktString}. Used for
     * {@code wkb}/{@code wkbr} geometry properties (e.g. Areas). The native
     * engine returns the geometry as its WKT string form, which is wrapped in a
     * {@link WktString} so the typed getter returns the declared type.
     * @param propertyName name of the property to get the value for
     * @return value as a WktString
     */
    protected AspectPropertyValue<WktString> getValueAsWktString(String propertyName) {
        AspectPropertyValue<String> str = getValueAsString(propertyName);
        AspectPropertyValue<WktString> result = new AspectPropertyValueDefault<>();
        if (str.hasValue()) {
            result.setValue(new WktString(str.getValue()));
        } else {
            result.setNoValueMessage(str.getNoValueMessage());
        }
        return result;
    }

    private static Class<?>[] toWeightedListType(Class<?> deepType, boolean isList) {
        final List<Class<?>> list = new ArrayList<>();
        list.add(List.class);
        list.add(IWeightedValue.class);
        if  (isList) {
            list.add(List.class);
        }
        list.add(deepType);
        return list.toArray(new Class<?>[0]);
    }

    /**
     * By default, the base map will not be populated as doing so is a fairly
     * expensive operation. Instead, we override the AsDictionary method to
     * populate the dictionary on-demand.
     * @return the data
     */
    @Override
    public Map<String, Object> asKeyMap() {
        if (mapPopulated == false) {
            synchronized (dataLock) {
                if (mapPopulated == false) {
                    Map<String, Object> map = new TreeMap<>(
                        String.CASE_INSENSITIVE_ORDER);
                    final Map<String, Map<String, ElementPropertyMetaData>> allProps = getPipeline().getElementAvailableProperties();
                    final String key = getEngines().get(0).getElementDataKey();
                    final Map<String, ElementPropertyMetaData> keyedProps = allProps.get(key);
                    final java.util.Collection<ElementPropertyMetaData> relevantProps = keyedProps.values();
                    for (ElementPropertyMetaData property : relevantProps) {
                        map.put(property.getName().toLowerCase(),
                            getAs(property.getName(),
                                AspectPropertyValue.class,
                                toWeightedListType(property.getType(), false)));
                    }
                    populateFromMap(map);
                    mapPopulated = true;
                }
            }
        }
        // Now that the base map has been populated,
        // we can return it.
        return super.asKeyMap();
    }

    /**
     * Get the {@link Class} representing the type values a property contains
     * from its name.
     * @param propertyName name of the property
     * @return value type, or {@link Object} if unknown
     */
    @SuppressWarnings("unchecked")
    protected Class<?> getPropertyType(String propertyName) {
        Class<?> type = Object.class;
        Map<String, ElementPropertyMetaData> properties =
            getPipeline().getElementAvailableProperties()
                .get(getEngines().get(0).getElementDataKey());
        if (properties != null) {
            ElementPropertyMetaData property = properties.get(propertyName);
            if (property != null) {
                type = (Class<Object>)property.getType();
            }
        }
        return type;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> TryGetResult<T> tryGetValue(
        String key,
        Class<T> type,
        Class<?>... parameterisedTypes) {
        TryGetResult<T> result = new TryGetResult<>();
        if (mapPopulated == true) {
            // If the complete set of values has been populated
            // then we can use the base implementation to get
            // the value from the dictionary.
            return super.tryGetValue(key, type);
        } else {
            // If the complete set of values has not been populated
            // then we don't want to retrieve values for all
            // properties so just get the one we want.
            if (propertyIsAvailable(key)) {
                if (type.equals(Object.class)) {
                    type = (Class<T>)AspectPropertyValue.class;
                    parameterisedTypes = toWeightedListType(getPropertyType(key), false);
                }
                synchronized (getLock) {
                    Object obj = null;
                    if (type.equals(AspectPropertyValue.class)){
                        if (parameterisedTypes.length > 0) {
                            Class<?> innerType = parameterisedTypes[0];
                            if (innerType.equals(String.class)) {
                                obj = getValueAsString(key);
                            } else if (innerType.equals(Boolean.class)) {
                                obj = getValueAsBool(key);
                            } else if (innerType.equals(Integer.class)) {
                                obj = getValueAsInteger(key);
                            } else if (innerType.equals(Double.class)) {
                                obj = getValueAsDouble(key);
                            } else if (innerType.equals(Float.class)) {
                                obj = getValueAsFloat(key);
                            } else if (InetAddress.class.isAssignableFrom(innerType)) {
                                obj = getValueAsIpAddress(key);
                            } else if (innerType.equals(WktString.class)) {
                                obj = getValueAsWktString(key);
                            } else if (innerType.equals(List.class)) {
                                obj = getValues(key, parameterisedTypes);
                            } else if (innerType.equals(JavaScript.class)) {
                                obj = getValueAsJavaScript(key);
                            } else {
                                obj = getValueAsString(key);
                            }
                        }
                        else {
                            obj = getValueAsString(key);
                        }
                    }
                    try {
                        T value;
                        if (type.isPrimitive()) {
                            value = (T) primitiveTypes.get(type).cast(obj);
                        } else {
                            value = type.cast(obj);
                        }
                        result.setValue(value);
                    } catch (ClassCastException e) {
                        throw new ClassCastException(
                            "Expected property '" + key + "' to be of " +
                                "type '" + type.getSimpleName() + "' but it is " +
                                "'" +
                                (obj == null ? "null" : obj.getClass().getSimpleName()) +
                                "'");
                    }
                }
            }
        }
        return result;
    }
}