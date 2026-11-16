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

package fiftyone.pipeline.engines.services;

import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngine;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import static fiftyone.pipeline.engines.Constants.MissingPropertyMessages;
import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Default implementation of the {@link MissingPropertyService} interface.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/properties.md#missing-properties">Specification</a>
 */
public class MissingPropertyServiceDefault implements MissingPropertyService {
    /**
     * Singleton instance of the service.
     */
    private static MissingPropertyServiceDefault missingPropertyService = null;

    private static final Object lock = new Object();

    /**
     * Private constructor to prevent new copies
     */
    private MissingPropertyServiceDefault() {
    }

    /**
     * Get the singleton instance of the default missing property service.
     *
     * @return singleton service
     */
    public static MissingPropertyService getInstance() {
        if (missingPropertyService == null) {
            synchronized (lock) {
                if (missingPropertyService == null) {
                    missingPropertyService = new MissingPropertyServiceDefault();
                }
            }
        }
        return missingPropertyService;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public MissingPropertyResult getMissingPropertyReason(
        String propertyName,
        AspectEngine engine) {

        MissingPropertyReason reason = MissingPropertyReason.Unknown;
        boolean cloud = engine instanceof CloudAspectEngine;

        AspectPropertyMetaData property = null;

        // We know the property has extends AspectPropertyMetaData as it
        // is a constraint of the AspectEngine interface, so don't check this
        // cast. Names are compared ignoring case, as the Element Data
        // compares them when a value is read.
        List<?> properties = engine.getProperties();
        if (properties != null) {
            for (Object currentPropertyObject : properties) {
                AspectPropertyMetaData currentProperty =
                    (AspectPropertyMetaData) currentPropertyObject;
                if (currentProperty.getName().equalsIgnoreCase(propertyName)) {
                    property = currentProperty;
                    break;
                }
            }
        }

        if (property != null) {
            // Step 3.1. The metadata lists the property. Only an on-premise
            // engine has a data file whose tier can lack the property.
            if (cloud == false &&
                containsIgnoringCase(
                    property.getDataTiersWherePresent(),
                    engine.getDataSourceTier()) == false) {
                reason = MissingPropertyReason.DataFileUpgradeRequired;
            }
            // Check if the property is excluded from the results.
            else if (property.isAvailable() == false) {
                reason = MissingPropertyReason.PropertyExcludedFromEngineConfiguration;
            }
        }
        else if (cloud) {
            // Step 3.2. A cloud engine that has not loaded its metadata
            // cannot say whether the resource key includes the property.
            CloudAspectEngine cloudEngine = (CloudAspectEngine) engine;
            if (cloudEngine.hasLoadedProperties()) {
                if (properties == null || properties.isEmpty()) {
                    reason = MissingPropertyReason.ProductNotAccessibleWithResourceKey;
                }
                else {
                    reason = MissingPropertyReason.PropertyNotAccessibleWithResourceKey;
                }
            }
            else if (cloudEngine.propertiesRequestFailed()) {
                reason = MissingPropertyReason.CloudRequestFailed;
            }
        }
        else if (hasTypedAccessor(engine, propertyName)) {
            // Step 3.3. The Element Data has an accessor for the property, so
            // it is a property of the product that this data file lacks. The
            // tiers that hold it are not known.
            reason = MissingPropertyReason.DataFileUpgradeRequired;
        }

        // Build the message string to return to the caller.
        StringBuilder message = new StringBuilder();
        message.append(String.format(
            MissingPropertyMessages.PREFIX,
            propertyName,
            engine.getElementDataKey()));
        switch (reason) {
            case DataFileUpgradeRequired:
                List<String> tiers = property == null ?
                    null :
                    property.getDataTiersWherePresent();
                if (tiers == null || tiers.isEmpty()) {
                    message.append(
                        MissingPropertyMessages.DATA_UPGRADE_REQUIRED_TIERS_UNKNOWN);
                } else {
                    message.append(String.format(
                        MissingPropertyMessages.DATA_UPGRADE_REQUIRED,
                        stringJoin(tiers, ","),
                        engine.getClass().getSimpleName()));
                }
                break;
            case PropertyExcludedFromEngineConfiguration:
                message.append(MissingPropertyMessages.PROPERTY_EXCLUDED);
                break;
            case ProductNotAccessibleWithResourceKey:
                message.append(String.format(
                    MissingPropertyMessages.PRODUCT_NOT_IN_CLOUD_RESOURCE,
                    engine.getElementDataKey()));
                break;
            case PropertyNotAccessibleWithResourceKey:
                List<String> available =
                    ((List<ElementPropertyMetaData>)engine.getProperties())
                    .stream().map(ElementPropertyMetaData::getName)
                    .collect(Collectors.toList());
                message.append(String.format(
                    MissingPropertyMessages.PROPERTY_NOT_IN_CLOUD_RESOURCE,
                    engine.getElementDataKey(),
                    stringJoin(available, ",")));
                break;
            case CloudRequestFailed:
                message.append(MissingPropertyMessages.CLOUD_REQUEST_FAILED);
                break;
            case Unknown:
                message.append(MissingPropertyMessages.UNKNOWN);
                break;
            default:
                break;
        }

        return new MissingPropertyResult(reason, message.toString());
    }

    @Override
    public MissingPropertyResult getMissingPropertyReason(
        String propertyName,
        List<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>> engines) {
        // Step 2. The first engine to give a reason other than Unknown
        // decides it. Otherwise the last engine checked is named.
        MissingPropertyResult result = null;
        for (AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine : engines) {
            result = getMissingPropertyReason(propertyName, engine);
            if (result.getReason() != MissingPropertyReason.Unknown) {
                return result;
            }
        }
        return result;
    }

    @Override
    public MissingPropertyResult getMissingPropertyReason(
        String propertyName,
        List<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>> engines,
        FlowData flowData) {
        // Step 1. A cloud engine whose request for this Flow Data failed has
        // no data, whatever its metadata says.
        if (flowData != null) {
            for (AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine : engines) {
                if (engine instanceof CloudAspectEngine &&
                    ((CloudAspectEngine<?, ?>) engine).requestFailed(flowData)) {
                    String key = engines.isEmpty() ?
                        UNKNOWN_KEY :
                        engines.get(0).getElementDataKey();
                    return new MissingPropertyResult(
                        MissingPropertyReason.CloudRequestFailed,
                        String.format(
                            MissingPropertyMessages.PREFIX,
                            propertyName,
                            key) +
                            MissingPropertyMessages.CLOUD_REQUEST_FAILED);
                }
            }
        }
        return getMissingPropertyReason(propertyName, engines);
    }

    /**
     * The name given in place of a Data Key when there is no engine.
     */
    private static final String UNKNOWN_KEY = "Unknown";

    private static boolean containsIgnoringCase(
        List<String> values,
        String value) {
        if (values == null || value == null) {
            return false;
        }
        for (String candidate : values) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the Element Data type the engine creates has a typed accessor
     * for the property, counting those it inherits. A typed accessor is a
     * public method with no parameters named 'get' and the property name,
     * compared ignoring case, that returns an {@link AspectPropertyValue}.
     */
    @SuppressWarnings("rawtypes")
    private static boolean hasTypedAccessor(
        AspectEngine engine,
        String propertyName) {
        if (propertyName == null || propertyName.isEmpty()) {
            return false;
        }
        TypedKey<?> key = engine.getTypedDataKey();
        if (key == null || key.getType() == null) {
            return false;
        }
        String name = "get" + propertyName;
        for (Method method : key.getType().getMethods()) {
            if (method.getParameterTypes().length == 0 &&
                method.getName().equalsIgnoreCase(name) &&
                AspectPropertyValue.class.isAssignableFrom(
                    method.getReturnType())) {
                return true;
            }
        }
        return false;
    }
}
