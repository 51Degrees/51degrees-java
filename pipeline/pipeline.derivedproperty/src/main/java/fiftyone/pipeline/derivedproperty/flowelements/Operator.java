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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The operators of format 1, each with the types it is allowed on.
 */
enum Operator {
    EQ("Eq", ValueType.BOOL, ValueType.INT, ValueType.DOUBLE,
        ValueType.STRING),
    NE("Ne", ValueType.BOOL, ValueType.INT, ValueType.DOUBLE,
        ValueType.STRING),
    GT("Gt", ValueType.INT, ValueType.DOUBLE),
    GE("Ge", ValueType.INT, ValueType.DOUBLE),
    LT("Lt", ValueType.INT, ValueType.DOUBLE),
    LE("Le", ValueType.INT, ValueType.DOUBLE),
    IN("In", ValueType.BOOL, ValueType.INT, ValueType.DOUBLE,
        ValueType.STRING),
    NOT_IN("NotIn", ValueType.BOOL, ValueType.INT, ValueType.DOUBLE,
        ValueType.STRING),
    STARTS_WITH("StartsWith", ValueType.STRING),
    ENDS_WITH("EndsWith", ValueType.STRING),
    CONTAINS("Contains", ValueType.STRING);

    private final String text;
    private final List<ValueType> types;

    Operator(String text, ValueType... types) {
        this.text = text;
        this.types = Collections.unmodifiableList(Arrays.asList(types));
    }

    /**
     * @return the types the operator is allowed on
     */
    List<ValueType> getTypes() {
        return types;
    }

    /**
     * @return true for In and NotIn, which take a list
     */
    boolean takesList() {
        return this == IN || this == NOT_IN;
    }

    /**
     * The operator a key names, matched without regard to case.
     * @param key the key in the script
     * @return the operator, or null where format 1 has no such operator
     */
    static Operator fromText(String key) {
        for (Operator operator : values()) {
            if (operator.text.equalsIgnoreCase(key)) {
                return operator;
            }
        }
        return null;
    }

    /**
     * @return every operator's name, separated by a comma and a space
     */
    static String names() {
        StringBuilder names = new StringBuilder();
        for (Operator operator : values()) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(operator.text);
        }
        return names.toString();
    }

    /**
     * @return the name format 1 gives the operator, such as Eq
     */
    @Override
    public String toString() {
        return text;
    }
}
