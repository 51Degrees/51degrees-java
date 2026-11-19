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

import java.util.Locale;

/**
 * The four value types format 1 has, each with the Java type a value of it
 * is held as.
 */
enum ValueType {
    STRING("string", String.class),
    BOOL("bool", Boolean.class),
    INT("int", Integer.class),
    DOUBLE("double", Double.class);

    /**
     * The smallest value of int, which format 1 fixes at a signed 32 bit
     * whole number so that one script gives one answer in every language.
     */
    static final long INT_MIN = Integer.MIN_VALUE;

    /**
     * The largest value of int.
     */
    static final long INT_MAX = Integer.MAX_VALUE;

    private final String text;
    private final Class<?> valueClass;

    ValueType(String text, Class<?> valueClass) {
        this.text = text;
        this.valueClass = valueClass;
    }

    /**
     * @return the Java type a value of this type is held as
     */
    Class<?> getValueClass() {
        return valueClass;
    }

    /**
     * The value type a script names, matched without regard to case.
     * @param text the name in the script
     * @return the type, or null where format 1 has no such type
     */
    static ValueType fromText(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (ValueType type : values()) {
            if (type.text.equals(lower)) {
                return type;
            }
        }
        return null;
    }

    /**
     * @return the name format 1 gives the type, such as int
     */
    @Override
    public String toString() {
        return text;
    }
}
