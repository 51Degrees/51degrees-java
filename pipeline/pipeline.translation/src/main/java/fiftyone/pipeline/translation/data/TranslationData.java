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

package fiftyone.pipeline.translation.data;

import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValues;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Data object populated by
 * {@link fiftyone.pipeline.translation.flowelements.TranslationEngine}.
 */
public class TranslationData extends ElementDataBase implements ITranslationData {

    /**
     * Construct a new instance.
     * @param logger used for logging
     * @param flowData the {@link FlowData} the element data will be added to
     */
    public TranslationData(Logger logger, FlowData flowData) {
        super(logger, flowData);
    }

    /**
     * Construct a new instance using an existing map of values.
     * @param logger used for logging
     * @param flowData the {@link FlowData} the element data will be added to
     * @param data the values to populate the element data with
     */
    public TranslationData(
        Logger logger,
        FlowData flowData,
        Map<String, Object> data) {
        super(logger, flowData, data);
    }

    /**
     * Get the translated value of a property whose values are of a single
     * type, checked against the type. The result is a copy of the value
     * stored.
     * @param key name of the property
     * @param type the type of the value
     * @param <V> the type of the value
     * @return the value, or no value with the reason when there is none
     * @throws ClassCastException if the stored value is not of the type
     */
    protected <V> AspectPropertyValue<V> getValue(String key, Class<V> type) {
        return AspectPropertyValues.checkedValue(
            key,
            getAs(key, AspectPropertyValue.class, type),
            type);
    }

    /**
     * Get the translated value of a property whose values are a
     * {@link List} of a single type, with each element checked. The result
     * is a copy of the value stored.
     * @param key name of the property
     * @param elementType the type of each element in the list
     * @param <V> the type of each element in the list
     * @return the value, or no value with the reason when there is none
     * @throws ClassCastException if the stored value is not a list of the
     *                            element type
     */
    protected <V> AspectPropertyValue<List<V>> getList(
        String key,
        Class<V> elementType) {
        return AspectPropertyValues.checkedList(
            key,
            getAs(key, AspectPropertyValue.class, List.class, elementType),
            elementType);
    }

    /**
     * Get the translated value of a property whose values are a
     * {@link List} of {@link IWeightedValue}, with each value checked. The
     * result is a copy of the value stored.
     * @param key name of the property
     * @param valueType the type of the value in each weighted value
     * @param <V> the type of the value in each weighted value
     * @return the value, or no value with the reason when there is none
     * @throws ClassCastException if the stored value is not a list of
     *                            weighted values of the value type
     */
    protected <V> AspectPropertyValue<List<IWeightedValue<V>>> getWeightedList(
        String key,
        Class<V> valueType) {
        return AspectPropertyValues.checkedWeightedList(
            key,
            getAs(
                key,
                AspectPropertyValue.class,
                List.class,
                IWeightedValue.class,
                valueType),
            valueType);
    }
}
