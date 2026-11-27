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

package fiftyone.pipeline.engines.data;

import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.WeightedValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a stored {@link AspectPropertyValue} to one of a given type,
 * checking the value against the type rather than casting it unchecked.
 * Each result is a new instance, so changing it does not change the value
 * stored, and a stored value of null gives null. A value of the wrong type
 * throws a {@link ClassCastException} naming the property.
 */
public final class AspectPropertyValues {

    private AspectPropertyValues() {
    }

    /**
     * Get the stored value of a property whose values are of a single type,
     * for example {@link String} or {@link Integer}.
     * @param key name of the property
     * @param stored the stored {@link AspectPropertyValue}
     * @param type the type of the value
     * @param <V> the type of the value
     * @return the value, or no value with the reason when there is none
     * @throws ClassCastException if the stored value is not of the type
     */
    public static <V> AspectPropertyValue<V> checkedValue(
        String key,
        Object stored,
        Class<V> type) {
        AspectPropertyValue<?> value =
            cast(key, AspectPropertyValue.class, stored);
        if (value == null) {
            return null;
        }
        AspectPropertyValueDefault<V> result =
            new AspectPropertyValueDefault<>();
        if (value.hasValue()) {
            result.setValue(cast(key, type, value.getValue()));
        } else {
            result.setNoValueMessage(value.getNoValueMessage());
        }
        return result;
    }

    /**
     * Get the stored value of a property whose values are a {@link List} of
     * a single type. Each element is checked and copied into a new list.
     * @param key name of the property
     * @param stored the stored {@link AspectPropertyValue}
     * @param elementType the type of each element in the list
     * @param <V> the type of each element in the list
     * @return the value, or no value with the reason when there is none
     * @throws ClassCastException if the stored value is not a list of the
     *                            element type
     */
    public static <V> AspectPropertyValue<List<V>> checkedList(
        String key,
        Object stored,
        Class<V> elementType) {
        AspectPropertyValue<?> value =
            cast(key, AspectPropertyValue.class, stored);
        if (value == null) {
            return null;
        }
        AspectPropertyValueDefault<List<V>> result =
            new AspectPropertyValueDefault<>();
        if (value.hasValue()) {
            List<?> values = cast(key, List.class, value.getValue());
            if (values == null) {
                result.setValue(null);
            } else {
                List<V> copy = new ArrayList<>(values.size());
                for (Object item : values) {
                    copy.add(cast(key, elementType, item));
                }
                result.setValue(copy);
            }
        } else {
            result.setNoValueMessage(value.getNoValueMessage());
        }
        return result;
    }

    /**
     * Get the stored value of a property whose values are a {@link List} of
     * {@link IWeightedValue}, each holding a value of a single type. Each
     * weighted value is checked, its value checked against the value type,
     * and copied into a new list.
     * @param key name of the property
     * @param stored the stored {@link AspectPropertyValue}
     * @param valueType the type of the value in each weighted value
     * @param <V> the type of the value in each weighted value
     * @return the value, or no value with the reason when there is none
     * @throws ClassCastException if the stored value is not a list of
     *                            weighted values of the value type
     */
    public static <V> AspectPropertyValue<List<IWeightedValue<V>>>
        checkedWeightedList(
        String key,
        Object stored,
        Class<V> valueType) {
        AspectPropertyValue<?> value =
            cast(key, AspectPropertyValue.class, stored);
        if (value == null) {
            return null;
        }
        AspectPropertyValueDefault<List<IWeightedValue<V>>> result =
            new AspectPropertyValueDefault<>();
        if (value.hasValue()) {
            List<?> values = cast(key, List.class, value.getValue());
            if (values == null) {
                result.setValue(null);
            } else {
                List<IWeightedValue<V>> copy = new ArrayList<>(values.size());
                for (Object item : values) {
                    IWeightedValue<?> weighted =
                        cast(key, IWeightedValue.class, item);
                    copy.add(weighted == null ? null : new WeightedValue<>(
                        weighted.getRawWeighting(),
                        cast(key, valueType, weighted.getValue())));
                }
                result.setValue(copy);
            }
        } else {
            result.setNoValueMessage(value.getNoValueMessage());
        }
        return result;
    }

    /**
     * Check that a value is of the type, naming the property if it is not.
     * @param key name of the property the value is for
     * @param type the type expected
     * @param value the value to check, which may be null
     * @param <V> the type expected
     * @return the value as the type
     * @throws ClassCastException if the value is not of the type
     */
    private static <V> V cast(String key, Class<V> type, Object value) {
        if (value != null && type.isInstance(value) == false) {
            throw new ClassCastException("Expected property '" + key +
                "' to be of type '" + type.getSimpleName() +
                "' but it is '" + value.getClass().getSimpleName() + "'");
        }
        return type.cast(value);
    }
}
