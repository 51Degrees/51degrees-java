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
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.CloudAspectEngine;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;

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
     * The start of the name of a typed accessor for a property.
     */
    private static final String ACCESSOR_PREFIX = "get";

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

    @Override
    public MissingPropertyResult getMissingPropertyReason(
        String propertyName,
        AspectEngine<?, ?> engine) {

        MissingPropertyReason reason = MissingPropertyReason.Unknown;

        boolean cloud = engine instanceof CloudAspectEngine<?, ?>;
        boolean onPremise = engine instanceof OnPremiseAspectEngine<?, ?>;
        // An On-premise Engine loads its metadata when it is built.
        boolean loaded = cloud == false || engine.hasLoadedProperties();

        AspectPropertyMetaData property = null;
        if (loaded) {
            // We know the property has extends AspectPropertyMetaData as it
            // is a constraint of the AspectEngine interface, so don't check
            // this cast
            for (Object currentPropertyObject : engine.getProperties()) {
                AspectPropertyMetaData currentProperty =
                    (AspectPropertyMetaData)currentPropertyObject;
                if (currentProperty.getName().equalsIgnoreCase(propertyName)) {
                    property = currentProperty;
                    break;
                }
            }
        }

        // The metadata lists the property. Only an On-premise Engine has a
        // data file tier to compare.
        if (property != null) {
            if (onPremise && hasDataTier(property, engine) == false) {
                reason = MissingPropertyReason.DataFileUpgradeRequired;
            }
            else if (property.isAvailable() == false) {
                reason = MissingPropertyReason.PropertyExcludedFromEngineConfiguration;
            }
        }

        if (reason == MissingPropertyReason.Unknown && cloud) {
            if (loaded) {
                reason = engine.getProperties().isEmpty() ?
                    MissingPropertyReason.ProductNotAccessibleWithResourceKey :
                    MissingPropertyReason.PropertyNotAccessibleWithResourceKey;
            }
            else if (((CloudAspectEngine<?, ?>) engine)
                .propertyListRequestFailed()) {
                reason = MissingPropertyReason.CloudRequestFailed;
            }
        }

        // The metadata does not list the property, but the Element Data has
        // a typed accessor for it, so another data file has it.
        if (reason == MissingPropertyReason.Unknown &&
            onPremise &&
            property == null &&
            hasTypedAccessor(engine, propertyName)) {
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
                        MissingPropertyMessages.DATA_UPGRADE_REQUIRED_TIERS_NOT_KNOWN);
                } else {
                    message.append(String.format(
                        MissingPropertyMessages.DATA_UPGRADE_REQUIRED,
                        stringJoin(
                            tiers,
                            MissingPropertyMessages.LIST_SEPARATOR),
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
                    engine.getProperties()
                    .stream().map(ElementPropertyMetaData::getName)
                    .collect(Collectors.toList());
                message.append(String.format(
                    MissingPropertyMessages.PROPERTY_NOT_IN_CLOUD_RESOURCE,
                    engine.getElementDataKey(),
                    stringJoin(
                        available,
                        MissingPropertyMessages.LIST_SEPARATOR)));
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

    /**
     * Whether the engine's data file tier is among the tiers the property
     * is present in.
     */
    private static boolean hasDataTier(
        AspectPropertyMetaData property,
        AspectEngine<?, ?> engine) {
        List<String> tiers = property.getDataTiersWherePresent();
        return tiers != null && tiers.contains(engine.getDataSourceTier());
    }

    /**
     * Whether the type of the engine's Element Data, counting the types it
     * inherits, has a typed accessor for the property, being a public
     * method with no parameters named 'get' and the property name, compared
     * ignoring case, that returns an {@link AspectPropertyValue}.
     */
    private static boolean hasTypedAccessor(
        AspectEngine<?, ?> engine,
        String propertyName) {
        TypedKey<?> key = engine.getTypedDataKey();
        Class<?> type = key == null ? null : key.getType();
        if (type == null) {
            return false;
        }
        String accessor = ACCESSOR_PREFIX + propertyName;
        for (Method method : type.getMethods()) {
            if (method.getParameterCount() == 0 &&
                method.getName().equalsIgnoreCase(accessor) &&
                AspectPropertyValue.class.isAssignableFrom(
                    method.getReturnType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MissingPropertyResult getMissingPropertyReason(
        String propertyName,
        List<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>> engines) {
        MissingPropertyResult result = null;
        for (AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine : engines) {
            result = getMissingPropertyReason(propertyName, engine);
            if (result.getReason() != MissingPropertyReason.Unknown) {
                return result;
            }
        }
        return result;
    }
}
