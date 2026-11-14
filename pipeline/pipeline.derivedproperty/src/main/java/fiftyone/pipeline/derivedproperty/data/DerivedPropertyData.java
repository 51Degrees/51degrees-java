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

package fiftyone.pipeline.derivedproperty.data;

import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.services.MissingPropertyReason;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentSkipListMap;

/**
 * The Element Data of the Derived Property Elements. Every Derived Property
 * Element in a Pipeline writes into the one instance on a request, and
 * elements in a parallel group write at the same time, so the values are
 * held in a concurrent map whose keys are matched without regard to case.
 */
public class DerivedPropertyData
    extends ElementDataBase
    implements IDerivedPropertyData {

    /**
     * Construct the Element Data for one request.
     * @param logger used for logging
     * @param flowData the request the data belongs to
     */
    public DerivedPropertyData(Logger logger, FlowData flowData) {
        super(
            logger,
            flowData,
            new ConcurrentSkipListMap<String, Object>(
                String.CASE_INSENSITIVE_ORDER));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> AspectPropertyValue<T> getAs(
        String propertyName,
        Class<T> valueType) {
        Object value = get(propertyName);
        if (value instanceof AspectPropertyValue<?> == false) {
            throw new PropertyMissingException(
                MissingPropertyReason.Unknown,
                propertyName,
                "There is no derived property '" + propertyName + "'. A " +
                    "derived property is named by the Output.Name of a " +
                    "script the Derived Property Element holds.");
        }
        AspectPropertyValue<?> result = (AspectPropertyValue<?>) value;
        if (result.hasValue() &&
            valueType.isInstance(result.getValue()) == false) {
            throw new ClassCastException(
                "The derived property '" + propertyName + "' holds a " +
                    result.getValue().getClass().getSimpleName() +
                    ", not a " + valueType.getSimpleName() + ".");
        }
        return (AspectPropertyValue<T>) result;
    }
}
