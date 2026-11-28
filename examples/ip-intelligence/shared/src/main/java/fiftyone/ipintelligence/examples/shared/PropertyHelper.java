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

package fiftyone.ipintelligence.examples.shared;

import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.WeightedValue;
import fiftyone.pipeline.core.data.WktString;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class PropertyHelper {

    /**
     * Advisory logged once when a property has no value, as this is
     * commonly because the resource key in use does not include the
     * property.
     */
    public static final String PRICING_MESSAGE = "Some properties used " +
            "by this example are not available with a free resource key or " +
            "the free data file. Contact us about a key or a data file " +
            "that has them at " +
            "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-ipintelligence-examples-shared-propertyhelper.java&utm_term=paid-properties";

    static Logger logger = LoggerFactory.getLogger(PropertyHelper.class);

    private static final AtomicBoolean pricingMessageLogged =
            new AtomicBoolean(false);

    /**
     * Unwrap a value that may be a List of IWeightedValue at runtime
     * (due to Java generics type erasure, the on-premise engine may return
     * List&lt;IWeightedValue&lt;T&gt;&gt; even when the declared type is T).
     * A list's values are joined with ", ", and a weighted value is
     * followed by its weighting, as in "Japan (weighting 1.00)".
     * @param value the raw value from AspectPropertyValue.getValue()
     * @return the unwrapped string representation
     */
    private static String unwrapValue(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "Unknown";
            }
            return list.stream()
                    .map(item -> {
                        if (item instanceof IWeightedValue) {
                            // An IP Intelligence value comes with the
                            // weighting the data gives it, which is 1 for
                            // a property with one answer.
                            IWeightedValue<?> weighted = (IWeightedValue<?>) item;
                            return asWeightedString(
                                    weighted.getValue(),
                                    weighted.getWeighting());
                        }
                        return toDisplayString(item);
                    })
                    .collect(Collectors.joining(", "));
        }
        return toDisplayString(value);
    }

    /**
     * Text for a single value. An address is shown in its literal form, for
     * example "8.8.8.0", because InetAddress.toString() puts the host name
     * and a slash in front of it.
     * @param value a single unwrapped value, may be null
     * @return the text to display
     */
    private static String toDisplayString(Object value) {
        if (value == null) {
            return "Unknown";
        }
        if (value instanceof InetAddress) {
            return ((InetAddress) value).getHostAddress();
        }
        return value.toString();
    }
    /**
     * Build the "no value" output for a property and log the
     * {@link #PRICING_MESSAGE} advisory the first time any property
     * has no value.
     * @param property the property value, may be null
     * @return a string describing why no value is available
     */
    private static String reportNoValue(AspectPropertyValue<?> property) {
        if (pricingMessageLogged.compareAndSet(false, true)) {
            logger.warn(PRICING_MESSAGE);
        }
        String message = property != null ? property.getNoValueMessage() : "No data available";
        return "Unknown. " + message;
    }

    /**
     * Try to carry out a 'get' on a property getter, and catch a
     * {@link PropertyMissingException} to avoid the example breaking if the
     * resource key, or data file are not configured correctly by the user.
     *
     * @param supplier to use e.g. IPIntelligenceData::getIsMobile()
     * @return value
     */
    public static <T> AspectPropertyValue<T> tryGet(Supplier<AspectPropertyValue<T>> supplier) {
        try {
            return supplier.get();
        } catch (PropertyMissingException e) {
            String message =
                    "The property '" + e.getPropertyName() + "' is not " +
                    "available with the data file or resource key in use. " +
                    "Contact us about one that has it at " +
                    "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-ipintelligence-examples-shared-propertyhelper.java&utm_term=property-missing";
            AspectPropertyValue<T> result = new AspectPropertyValueDefault<>();
            result.setNoValueMessage(message);
            return result;
        }
    }

    /**
     * Helper to get the value of an IP Intelligence string property.
     * Handles runtime List&lt;IWeightedValue&gt; values due to type erasure.
     */
    public static String asStringProperty(AspectPropertyValue<String> property) {
        if (property == null || !property.hasValue()) {
            return reportNoValue(property);
        } else {
            return unwrapValue(property.getValue());
        }
    }

    /**
     * Helper to get the value of an IP Intelligence integer property.
     * Handles runtime List&lt;IWeightedValue&gt; values due to type erasure.
     */
    public static String asIntegerProperty(AspectPropertyValue<Integer> property) {
        if (property == null || !property.hasValue()) {
            return reportNoValue(property);
        } else {
            return unwrapValue(property.getValue());
        }
    }

    /**
     * Helper to get the value of an IP Intelligence float property.
     * Handles runtime List&lt;IWeightedValue&gt; values due to type erasure.
     */
    public static String asFloatProperty(AspectPropertyValue<Float> property) {
        if (property == null || !property.hasValue()) {
            return reportNoValue(property);
        } else {
            return unwrapValue(property.getValue());
        }
    }

    /**
     * Helper to get the value of an IP Intelligence InetAddress property.
     * Handles runtime List&lt;IWeightedValue&gt; values due to type erasure.
     * The address is shown in its literal form, for example "8.8.8.0".
     */
    public static String asIPAddressProperty(AspectPropertyValue<java.net.InetAddress> property) {
        if (property == null || !property.hasValue()) {
            return reportNoValue(property);
        } else {
            return unwrapValue(property.getValue());
        }
    }

    /**
     * Helper to get the value of an IP Intelligence WktString property.
     * Handles runtime List&lt;IWeightedValue&gt; values due to type erasure.
     */
    public static String asWktStringProperty(AspectPropertyValue<WktString> property) {
        if (property == null || !property.hasValue()) {
            return reportNoValue(property);
        } else {
            return unwrapValue(property.getValue());
        }
    }

    /**
     * Generic helper to get the value of a property as a string.
     * Used primarily for testing and simple property display.
     * @param value the property value
     * @param <T> the type
     * @return a string representation of the value or a "no value" message
     */
    public static <T> String asString(AspectPropertyValue<T> value) {
        if (value.hasValue()) {
            return unwrapValue(value.getValue());
        }
        return "Unknown. " + value.getNoValueMessage();
    }

    /**
     * The raw weighting of a value that is the only answer, which is a
     * weighting of 1.
     */
    private static final int FULL_WEIGHTING = 65535;

    /**
     * Get each value of an IP Intelligence property with its weighting. IP
     * Intelligence properties return weighted values, and a property with a
     * single answer returns it with a weighting of 1. Due to Java generics
     * type erasure, the on-premise engine returns
     * List&lt;IWeightedValue&lt;T&gt;&gt; even when the declared type is T, so
     * a value that is not such a list is given here with a weighting of 1.
     * @param property the property value, may be null
     * @return the weighted values, or an empty list when there is no value
     */
    public static List<IWeightedValue<?>> weightedValues(
            AspectPropertyValue<?> property) {
        List<IWeightedValue<?>> result = new ArrayList<>();
        if (property == null || property.hasValue() == false) {
            return result;
        }
        Object value = property.getValue();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item instanceof IWeightedValue) {
                    result.add((IWeightedValue<?>) item);
                } else {
                    result.add(new WeightedValue<>(FULL_WEIGHTING, item));
                }
            }
        } else {
            result.add(new WeightedValue<>(FULL_WEIGHTING, value));
        }
        return result;
    }

    /**
     * Write a value with its weighting, for example "gb (weighting 1.00)".
     * Every weighting the examples print is written this way.
     * @param value the value, may be null
     * @param weighting the weighting, between 0 and 1
     * @return the value followed by its weighting
     */
    public static String asWeightedString(Object value, float weighting) {
        return String.format(
                Locale.ROOT,
                "%s (weighting %.2f)",
                toDisplayString(value),
                weighting);
    }

    /**
     * Write each value of an IP Intelligence property with its weighting,
     * for example "gb (weighting 1.00)", or why there is no value.
     * @param property the property value, may be null
     * @return the values with their weightings, joined with ", "
     */
    public static String asWeightedString(AspectPropertyValue<?> property) {
        if (property == null || property.hasValue() == false) {
            return reportNoValue(property);
        }
        return weightedValues(property).stream()
                .map(weighted -> asWeightedString(
                        weighted.getValue(),
                        weighted.getWeighting()))
                .collect(Collectors.joining(", "));
    }

    /**
     * Helper to get the first raw value of an IP Intelligence property.
     * Due to Java generics type erasure, the on-premise engine may return
     * List&lt;IWeightedValue&lt;T&gt;&gt; even when the declared type is T.
     * This method unwraps such values and returns the first one, which is
     * useful when the typed value is needed for further calculation rather
     * than for display.
     * @param property the property value
     * @param type the type of the value, which the value is checked against
     * @param <T> the type of the value
     * @return the first unwrapped value, or null when there is no value
     * @throws ClassCastException if the value is not of the type
     */
    public static <T> T firstValue(
            AspectPropertyValue<?> property,
            Class<T> type) {
        if (property == null || property.hasValue() == false) {
            return null;
        }
        Object value = property.getValue();
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return null;
            }
            Object item = list.get(0);
            if (item instanceof IWeightedValue) {
                item = ((IWeightedValue<?>) item).getValue();
            }
            return type.cast(item);
        }
        return type.cast(value);
    }

}
