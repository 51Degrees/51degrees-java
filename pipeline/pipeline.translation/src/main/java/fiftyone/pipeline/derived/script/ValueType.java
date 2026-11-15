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

package fiftyone.pipeline.derived.script;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The value types format 1 knows, being the types an output can have and
 * the types a source property is read as. Conversion follows the table in
 * the format 1 reference: values are never coerced loosely.
 */
public enum ValueType {
    STRING("string", String.class),
    BOOL("bool", Boolean.class),
    INT("int", Integer.class),
    DOUBLE("double", Double.class);

    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL = Pattern.compile(
        "[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

    private final String text;
    private final Class<?> javaType;

    ValueType(String text, Class<?> javaType) {
        this.text = text;
        this.javaType = javaType;
    }

    /**
     * The name of the type as a script writes it.
     * @return the name
     */
    public String getText() {
        return text;
    }

    /**
     * The Java type a value of this type has.
     * @return the type
     */
    public Class<?> getJavaType() {
        return javaType;
    }

    /**
     * The type a script names, matched without regard to letter case.
     * @param text the name
     * @return the type, or null if format 1 does not have it
     */
    public static ValueType parse(String text) {
        if (text == null) {
            return null;
        }
        for (ValueType type : values()) {
            if (type.text.equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        return null;
    }

    /**
     * The type inferred from a literal in a script, or null for a value
     * that is not a literal (null, a mapping or a list).
     * @param literal the literal
     * @return the inferred type
     */
    public static ValueType infer(Object literal) {
        if (literal instanceof Boolean) {
            return BOOL;
        }
        if (literal instanceof Number) {
            return isWholeInt((Number) literal) ? INT : DOUBLE;
        }
        if (literal instanceof String) {
            return STRING;
        }
        return null;
    }

    /**
     * True for a number whose value is whole and fits a signed 32 bit
     * integer, whichever way it was written.
     */
    static boolean isWholeInt(Number number) {
        double value = number.doubleValue();
        if (number instanceof Long || number instanceof Integer ||
            number instanceof Short || number instanceof Byte) {
            long whole = number.longValue();
            return whole >= Integer.MIN_VALUE && whole <= Integer.MAX_VALUE;
        }
        if (number instanceof java.math.BigInteger) {
            return ((java.math.BigInteger) number).bitLength() < 32;
        }
        return Double.isFinite(value) && value == Math.rint(value) &&
            value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }

    /**
     * Converts a literal from a script to this type. The literal has
     * already been checked to infer this type, or INT for a DOUBLE.
     * @param literal the literal
     * @return the converted value
     */
    Object fromLiteral(Object literal) {
        switch (this) {
            case INT:
                return ((Number) literal).intValue();
            case DOUBLE:
                return ((Number) literal).doubleValue();
            case BOOL:
                return literal;
            default:
                return literal.toString();
        }
    }

    /**
     * The result of reading a source value as this type.
     */
    public static final class Read {
        final Object value;
        final String failure;

        private Read(Object value, String failure) {
            this.value = value;
            this.failure = failure;
        }

        static Read of(Object value) {
            return new Read(value, null);
        }

        static Read failed(String failure) {
            return new Read(null, failure);
        }

        public boolean isAvailable() {
            return failure == null;
        }

        /** @return the value read, where it is available */
        public Object value() {
            return value;
        }

        /** @return why the value cannot be read, or null */
        public String failure() {
            return failure;
        }
    }

    /**
     * Reads a source value, which is not null and not a no-value, as this
     * type, following the conversion table of format 1.
     * @param raw the source value
     * @return the value, or the reason it cannot be read
     */
    public Read read(Object raw) {
        if (raw instanceof List) {
            return Read.failed("held a list where a single value is needed");
        }
        switch (this) {
            case BOOL:
                if (raw instanceof Boolean) {
                    return Read.of(raw);
                }
                if (raw instanceof String) {
                    String text = ((String) raw).trim();
                    if (text.equalsIgnoreCase("true")) {
                        return Read.of(Boolean.TRUE);
                    }
                    if (text.equalsIgnoreCase("false")) {
                        return Read.of(Boolean.FALSE);
                    }
                }
                break;
            case INT:
                if (raw instanceof Number && raw instanceof Double == false &&
                    raw instanceof Float == false) {
                    if (isWholeInt((Number) raw)) {
                        return Read.of(((Number) raw).intValue());
                    }
                } else if (raw instanceof Number) {
                    double value = ((Number) raw).doubleValue();
                    if (isWholeInt(value)) {
                        return Read.of((int) value);
                    }
                } else if (raw instanceof String) {
                    String text = ((String) raw).trim();
                    if (INTEGER.matcher(text).matches()) {
                        try {
                            long value = Long.parseLong(text);
                            if (value >= Integer.MIN_VALUE &&
                                value <= Integer.MAX_VALUE) {
                                return Read.of((int) value);
                            }
                        } catch (NumberFormatException tooLong) {
                            // Out of range, so not readable.
                        }
                    }
                }
                break;
            case DOUBLE:
                if (raw instanceof Number) {
                    double value = ((Number) raw).doubleValue();
                    if (Double.isFinite(value)) {
                        return Read.of(value);
                    }
                } else if (raw instanceof String) {
                    String text = ((String) raw).trim();
                    if (DECIMAL.matcher(text).matches()) {
                        double value = Double.parseDouble(text);
                        if (Double.isFinite(value)) {
                            return Read.of(value);
                        }
                    }
                }
                break;
            default:
                if (raw instanceof String) {
                    return Read.of(raw);
                }
                if (raw instanceof Boolean) {
                    return Read.of((Boolean) raw ? "True" : "False");
                }
                if (raw instanceof Number) {
                    return Read.of(plain((Number) raw));
                }
                break;
        }
        return Read.failed("held '" + raw + "' which cannot be read as " + text);
    }

    private static String plain(Number number) {
        if (number instanceof Double || number instanceof Float) {
            double value = number.doubleValue();
            if (value == Math.rint(value) && Math.abs(value) < 1e15) {
                return Long.toString((long) value);
            }
            return Double.toString(value).toLowerCase(Locale.ROOT);
        }
        return number.toString();
    }
}
