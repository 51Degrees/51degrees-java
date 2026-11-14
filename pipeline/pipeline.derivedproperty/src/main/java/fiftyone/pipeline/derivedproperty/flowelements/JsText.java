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

import java.math.BigDecimal;

/**
 * Text written the way the JavaScript reference tools write it, so that a
 * message, a number read as a string and the canonical form read the same
 * in every language.
 */
final class JsText {

    /**
     * From this size up, and below {@link #SMALLEST_PLAIN}, JavaScript
     * writes a number with an exponent.
     */
    private static final double LARGEST_PLAIN = 1e21;

    private static final double SMALLEST_PLAIN = 1e-6;

    private static final char LAST_CONTROL = 0x1f;
    private static final char VERTICAL_TAB = 0x0b;
    private static final String HEX_DIGITS = "0123456789abcdef";
    private static final int NIBBLE_BITS = 4;
    private static final int NIBBLE_MASK = 0xf;
    private static final int UNICODE_ESCAPE_DIGITS = 4;

    private JsText() {
    }

    /**
     * Write a number as JavaScript's String does.
     * @param value the number
     * @return the text
     */
    static String number(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "Infinity" : "-Infinity";
        }
        if (value == 0) {
            return "0";
        }
        BigDecimal decimal =
            new BigDecimal(Double.toString(value)).stripTrailingZeros();
        double size = Math.abs(value);
        if (size >= SMALLEST_PLAIN && size < LARGEST_PLAIN) {
            return decimal.toPlainString();
        }
        String digits = decimal.unscaledValue().abs().toString();
        int exponent = digits.length() - 1 - decimal.scale();
        StringBuilder text = new StringBuilder();
        if (value < 0) {
            text.append('-');
        }
        text.append(digits.charAt(0));
        if (digits.length() > 1) {
            text.append('.').append(digits, 1, digits.length());
        }
        text.append('e').append(exponent < 0 ? '-' : '+')
            .append(Math.abs(exponent));
        return text.toString();
    }

    /**
     * Quote a string as JSON.stringify does.
     * @param value the string
     * @return the quoted string
     */
    static String quote(String value) {
        StringBuilder text = new StringBuilder(value.length() + 2);
        text.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    text.append("\\\"");
                    break;
                case '\\':
                    text.append("\\\\");
                    break;
                case '\b':
                    text.append("\\b");
                    break;
                case '\f':
                    text.append("\\f");
                    break;
                case '\n':
                    text.append("\\n");
                    break;
                case '\r':
                    text.append("\\r");
                    break;
                case '\t':
                    text.append("\\t");
                    break;
                default:
                    if (c <= LAST_CONTROL || isLoneSurrogate(value, i)) {
                        unicodeEscape(text, c);
                    } else {
                        text.append(c);
                    }
                    break;
            }
        }
        return text.append('"').toString();
    }

    /**
     * Whether a character is white space as JavaScript's trim reads it.
     * @param c the character
     * @return true for white space or a line terminator
     */
    static boolean isWhitespace(char c) {
        return c == '\t' || c == '\n' || c == VERTICAL_TAB || c == '\f' ||
            c == '\r' || c == ' ' || c == ' ' || c == '﻿' ||
            Character.getType(c) == Character.SPACE_SEPARATOR;
    }

    /**
     * Remove leading and trailing white space as JavaScript's trim does.
     * @param value the text
     * @return the text without the white space around it
     */
    static String trim(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isWhitespace(value.charAt(start))) {
            start++;
        }
        while (end > start && isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    private static boolean isLoneSurrogate(String value, int i) {
        char c = value.charAt(i);
        if (Character.isHighSurrogate(c)) {
            return i + 1 >= value.length() ||
                Character.isLowSurrogate(value.charAt(i + 1)) == false;
        }
        if (Character.isLowSurrogate(c)) {
            return i == 0 ||
                Character.isHighSurrogate(value.charAt(i - 1)) == false;
        }
        return false;
    }

    private static void unicodeEscape(StringBuilder text, char c) {
        text.append("\\u");
        for (int shift = (UNICODE_ESCAPE_DIGITS - 1) * NIBBLE_BITS;
             shift >= 0;
             shift -= NIBBLE_BITS) {
            text.append(HEX_DIGITS.charAt((c >> shift) & NIBBLE_MASK));
        }
    }
}
