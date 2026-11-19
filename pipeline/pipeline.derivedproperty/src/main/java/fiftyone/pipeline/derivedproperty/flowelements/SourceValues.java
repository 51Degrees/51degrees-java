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

package fiftyone.pipeline.derivedproperty.flowelements;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.WktString;
import fiftyone.pipeline.engines.data.AspectPropertyValue;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;

/**
 * Reads a source property from the Element Data of the element that
 * supplies it, and converts the value to the type the script infers. A
 * value arrives as its native type or as a string, and both are accepted,
 * but values are never coerced loosely, so N/A, Unknown and the empty
 * string never become false or zero.
 * <p>
 * The wording of every reason is that of readSlot in tools/evaluate.mjs in
 * the derived-properties repository. See
 * <a href="https://github.com/51Degrees/derived-properties/blob/cc30638632050795d9acc80d5118a290ffaba085/docs/format-1.md#absent-and-invalid-source-properties">format
 * 1, absent and invalid source properties</a>.
 */
final class SourceValues {

    /**
     * The reason given where the property is not in the Element Data, or
     * its value is null.
     */
    static final String NOT_PRESENT = "property not present on this request";

    /**
     * Stands for a list whose members are not all weighted values.
     */
    private static final Object NOT_WEIGHTED = new Object();

    /**
     * The bits in a long, which a BigInteger must fit inside to be read as
     * a whole number.
     */
    private static final int LONG_BITS = 63;

    private SourceValues() {
    }

    /**
     * Read a property from the Element Data of its source element.
     * @param data the Element Data, or null where the request carries none
     *             under the key
     * @param property the property
     * @return the value, converted, or the reason it could not be read
     */
    static Slot read(ElementData data, SourceProperty property) {
        if (data == null) {
            return notAvailable(property, NOT_PRESENT);
        }
        Object raw;
        try {
            raw = data.get(property.getPropertyName());
        } catch (RuntimeException e) {
            return notAvailable(property, NOT_PRESENT);
        }
        if (raw == null) {
            return notAvailable(property, NOT_PRESENT);
        }
        return readValue(raw, property);
    }

    /**
     * Convert a value an element holds. A value with no value carries its
     * own message, a list of weighted values gives the value with the
     * highest weight, and any other list cannot be read where one value is
     * needed.
     */
    static Slot readValue(Object value, SourceProperty property) {
        Object raw = value;
        if (raw instanceof AspectPropertyValue<?>) {
            AspectPropertyValue<?> wrapped = (AspectPropertyValue<?>) raw;
            if (wrapped.hasValue() == false) {
                String message = wrapped.getNoValueMessage();
                return notAvailable(property,
                    message == null || message.isEmpty() ?
                        NOT_PRESENT : message);
            }
            raw = wrapped.getValue();
            if (raw == null) {
                return notAvailable(property, NOT_PRESENT);
            }
        }
        if (raw instanceof List<?>) {
            raw = heaviest((List<?>) raw);
            if (raw == NOT_WEIGHTED) {
                return Slot.absent(
                    "held a list where a single value is needed");
            }
            if (raw == null) {
                return notAvailable(property, NOT_PRESENT);
            }
        } else if (raw instanceof IWeightedValue<?>) {
            raw = ((IWeightedValue<?>) raw).getValue();
            if (raw == null) {
                return notAvailable(property, NOT_PRESENT);
            }
        }
        Slot slot = convert(raw, property.getType());
        if (slot != null) {
            return slot;
        }
        return Slot.absent("held '" + display(raw) +
            "' which cannot be read as " + property.getType());
    }

    private static Slot notAvailable(SourceProperty property, String detail) {
        return Slot.absent("element '" + property.getWrittenKey() +
            "' has no value for '" + property.getPropertyName() + "': " +
            detail);
    }

    /**
     * The value with the highest weight from a list of weighted values, the
     * first where two weigh the same.
     * @return the value, or {@link #NOT_WEIGHTED} where the list is empty or
     * any member is not a weighted value
     */
    private static Object heaviest(List<?> list) {
        if (list.isEmpty()) {
            return NOT_WEIGHTED;
        }
        IWeightedValue<?> best = null;
        for (Object member : list) {
            if (member instanceof IWeightedValue<?> == false) {
                return NOT_WEIGHTED;
            }
            IWeightedValue<?> weighted = (IWeightedValue<?>) member;
            if (best == null ||
                weighted.getRawWeighting() > best.getRawWeighting()) {
                best = weighted;
            }
        }
        return best.getValue();
    }

    /**
     * Convert a source value, which arrives as its native type or as a
     * string.
     * @return the value, or null where it cannot be read as the type
     */
    static Slot convert(Object raw, ValueType type) {
        if (raw instanceof String) {
            return convertString((String) raw, type);
        }
        if (raw instanceof WktString) {
            String text = raw.toString();
            return text == null ? null : convertString(text, type);
        }
        if (raw instanceof Boolean) {
            boolean value = (Boolean) raw;
            switch (type) {
                case BOOL:
                    return Slot.ofBool(value);
                case STRING:
                    return Slot.ofString(value ? "True" : "False");
                default:
                    return null;
            }
        }
        Long whole = wholeNumber(raw);
        if (whole != null) {
            long value = whole;
            switch (type) {
                case INT:
                    return value < ValueType.INT_MIN ||
                        value > ValueType.INT_MAX ?
                        null : Slot.ofInt(value);
                case DOUBLE:
                    return Slot.ofDouble(value);
                case STRING:
                    return Slot.ofString(Long.toString(value));
                default:
                    return null;
            }
        }
        if (raw instanceof Double || raw instanceof Float) {
            double value = ((Number) raw).doubleValue();
            switch (type) {
                case INT:
                    return DerivedScriptValidator.isWhole(value) == false ||
                        value < ValueType.INT_MIN ||
                        value > ValueType.INT_MAX ?
                        null : Slot.ofInt((long) value);
                case DOUBLE:
                    return Double.isInfinite(value) || Double.isNaN(value) ?
                        null : Slot.ofDouble(value);
                case STRING:
                    return Slot.ofString(JsText.number(value));
                default:
                    return null;
            }
        }
        return null;
    }

    /**
     * A value of one of Java's whole number types, or null for any other.
     */
    private static Long wholeNumber(Object raw) {
        if (raw instanceof Integer || raw instanceof Long ||
            raw instanceof Short || raw instanceof Byte) {
            return ((Number) raw).longValue();
        }
        if (raw instanceof BigInteger &&
            ((BigInteger) raw).bitLength() <= LONG_BITS) {
            return ((BigInteger) raw).longValue();
        }
        return null;
    }

    /**
     * Convert the string form of a value.
     * @return the value, or null where it cannot be read as the type
     */
    static Slot convertString(String raw, ValueType type) {
        switch (type) {
            case BOOL: {
                String trimmed = JsText.trim(raw).toLowerCase(Locale.ROOT);
                if (trimmed.equals("true")) {
                    return Slot.ofBool(true);
                }
                if (trimmed.equals("false")) {
                    return Slot.ofBool(false);
                }
                return null;
            }
            case INT: {
                String trimmed = JsText.trim(raw);
                if (isIntegerText(trimmed) == false) {
                    return null;
                }
                try {
                    long value = Long.parseLong(trimmed);
                    return value < ValueType.INT_MIN ||
                        value > ValueType.INT_MAX ?
                        null : Slot.ofInt(value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            case DOUBLE: {
                String trimmed = JsText.trim(raw);
                if (isDecimalText(trimmed) == false) {
                    return null;
                }
                double value = Double.parseDouble(trimmed);
                return Double.isInfinite(value) || Double.isNaN(value) ?
                    null : Slot.ofDouble(value);
            }
            case STRING:
                return Slot.ofString(raw);
            default:
                return null;
        }
    }

    /**
     * Whether text is an optional sign and then ASCII digits.
     */
    private static boolean isIntegerText(String text) {
        int start = startAfterSign(text);
        return start < text.length() &&
            digits(text, start) == text.length() - start;
    }

    /**
     * Whether text is an optional sign, digits with a full stop as the
     * decimal separator, and an optional exponent.
     */
    private static boolean isDecimalText(String text) {
        int position = startAfterSign(text);
        int whole = digits(text, position);
        position += whole;
        int fraction = 0;
        if (position < text.length() && text.charAt(position) == '.') {
            position++;
            fraction = digits(text, position);
            position += fraction;
        }
        if (whole == 0 && fraction == 0) {
            return false;
        }
        if (position < text.length() &&
            (text.charAt(position) == 'e' || text.charAt(position) == 'E')) {
            position++;
            if (position < text.length() &&
                (text.charAt(position) == '+' ||
                    text.charAt(position) == '-')) {
                position++;
            }
            int exponent = digits(text, position);
            if (exponent == 0) {
                return false;
            }
            position += exponent;
        }
        return position == text.length();
    }

    private static int startAfterSign(String text) {
        return text.length() > 0 &&
            (text.charAt(0) == '+' || text.charAt(0) == '-') ? 1 : 0;
    }

    /**
     * The number of ASCII digits in text from start.
     */
    private static int digits(String text, int start) {
        int position = start;
        while (position < text.length() &&
            text.charAt(position) >= '0' && text.charAt(position) <= '9') {
            position++;
        }
        return position - start;
    }

    /**
     * A source value as a message prints it.
     */
    private static String display(Object raw) {
        if (raw instanceof String) {
            return (String) raw;
        }
        if (raw instanceof Boolean) {
            return (Boolean) raw ? "True" : "False";
        }
        Long whole = wholeNumber(raw);
        if (whole != null) {
            return whole.toString();
        }
        if (raw instanceof Double || raw instanceof Float) {
            return JsText.number(((Number) raw).doubleValue());
        }
        return String.valueOf(raw);
    }
}
