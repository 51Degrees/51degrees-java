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

import java.util.Collections;
import java.util.List;

/**
 * One condition of a script, which is always true or false, because a
 * script only runs once every source property it names has been read.
 */
final class Condition {

    /**
     * The forms a condition takes.
     */
    enum Kind { COMPARE, CHECK, AGGREGATE, ALL, ANY, NOT }

    private final Kind kind;

    // A comparison of a source property with a literal.
    private final int slot;
    private final String property;
    private final Operator operator;
    private final ScriptNode operand;
    private final ValueType type;
    private final Literal literal;
    private final Literal[] literals;

    // A reference to a check by name.
    private final String check;
    private final int index;

    // A count of the checks in a group that passed or failed. A null group
    // is every check.
    private final boolean countPassed;
    private final int[] group;
    private final List<String> groupNames;
    private final long count;

    // The members of All and Any, or the one condition of Not.
    private final Condition[] items;

    private Condition(
        Kind kind,
        int slot,
        String property,
        Operator operator,
        ScriptNode operand,
        ValueType type,
        Literal literal,
        Literal[] literals,
        String check,
        int index,
        boolean countPassed,
        int[] group,
        List<String> groupNames,
        long count,
        Condition[] items) {
        this.kind = kind;
        this.slot = slot;
        this.property = property;
        this.operator = operator;
        this.operand = operand;
        this.type = type;
        this.literal = literal;
        this.literals = literals;
        this.check = check;
        this.index = index;
        this.countPassed = countPassed;
        this.group = group;
        this.groupNames = groupNames;
        this.count = count;
        this.items = items;
    }

    /**
     * What a condition that could not be read becomes. A fault has been
     * recorded by then, so the model is discarded unevaluated.
     */
    static Condition faulted() {
        return compound(Kind.ALL, new Condition[0]);
    }

    static Condition comparison(
        int slot,
        String property,
        Operator operator,
        ScriptNode operand,
        ValueType type) {
        Literal literal = Literal.NONE;
        Literal[] literals = new Literal[0];
        if (operand.getKind() == ScriptNode.Kind.LIST) {
            literals = new Literal[operand.getItems().size()];
            for (int i = 0; i < literals.length; i++) {
                literals[i] = Literal.of(operand.getItems().get(i));
            }
        } else {
            literal = Literal.of(operand);
        }
        return new Condition(Kind.COMPARE, slot, property, operator, operand,
            type, literal, literals, null, -1, false, null, null, 0, null);
    }

    static Condition checkReference(String check, int index) {
        return new Condition(Kind.CHECK, -1, null, null, null, null,
            Literal.NONE, null, check, index, false, null, null, 0, null);
    }

    /**
     * @param countPassed true for Passed, false for Failed
     * @param group the indexes of the checks counted, or null for every
     *              check
     * @param groupNames the names of the checks counted, or null for every
     *                   check
     * @param operator the comparison
     * @param operand the whole number compared with, as written
     */
    static Condition aggregate(
        boolean countPassed,
        int[] group,
        List<String> groupNames,
        Operator operator,
        ScriptNode operand) {
        return new Condition(Kind.AGGREGATE, -1, null, operator, operand,
            ValueType.INT, Literal.NONE, null, null, -1, countPassed, group,
            groupNames == null ?
                null : Collections.unmodifiableList(groupNames),
            (long) operand.getNumber(), null);
    }

    /**
     * @param kind ALL, ANY or NOT
     * @param items the members, or the one condition of Not
     */
    static Condition compound(Kind kind, Condition[] items) {
        return new Condition(kind, -1, null, null, null, null, Literal.NONE,
            null, null, -1, false, null, null, 0, items);
    }

    Kind getKind() {
        return kind;
    }

    String getProperty() {
        return property;
    }

    Operator getOperator() {
        return operator;
    }

    /**
     * @return the literal compared with, as written in the script
     */
    ScriptNode getOperand() {
        return operand;
    }

    String getCheck() {
        return check;
    }

    boolean isCountPassed() {
        return countPassed;
    }

    /**
     * @return the names of the checks counted, or null for every check
     */
    List<String> getGroupNames() {
        return groupNames;
    }

    long getCount() {
        return count;
    }

    /**
     * @return the number of members of All or Any, or 1 for Not
     */
    int itemCount() {
        return items.length;
    }

    Condition item(int i) {
        return items[i];
    }

    /**
     * Evaluate the condition. Every slot is available.
     * @param slots the source properties, read for this script
     * @param checks the result of every check, in the order defined
     * @return true or false
     */
    boolean test(Slot[] slots, boolean[] checks) {
        switch (kind) {
            case COMPARE:
                return compare(slots[slot]);
            case CHECK:
                return checks[index];
            case AGGREGATE:
                return compareCount(countOf(checks));
            case ALL:
                for (Condition item : items) {
                    if (item.test(slots, checks) == false) {
                        return false;
                    }
                }
                return true;
            case ANY:
                for (Condition item : items) {
                    if (item.test(slots, checks)) {
                        return true;
                    }
                }
                return false;
            case NOT:
                return items[0].test(slots, checks) == false;
            default:
                return false;
        }
    }

    private long countOf(boolean[] checks) {
        long counted = 0;
        if (group == null) {
            for (boolean passed : checks) {
                if (passed == countPassed) {
                    counted++;
                }
            }
        } else {
            for (int member : group) {
                if (checks[member] == countPassed) {
                    counted++;
                }
            }
        }
        return counted;
    }

    private boolean compareCount(long counted) {
        switch (operator) {
            case EQ:
                return counted == count;
            case NE:
                return counted != count;
            case GT:
                return counted > count;
            case GE:
                return counted >= count;
            case LT:
                return counted < count;
            case LE:
                return counted <= count;
            default:
                return false;
        }
    }

    /**
     * Apply the operator to the value read for the property.
     */
    private boolean compare(Slot value) {
        switch (operator) {
            case IN:
                for (Literal member : literals) {
                    if (equal(value, member)) {
                        return true;
                    }
                }
                return false;
            case NOT_IN:
                for (Literal member : literals) {
                    if (equal(value, member)) {
                        return false;
                    }
                }
                return true;
            case EQ:
                return equal(value, literal);
            case NE:
                return equal(value, literal) == false;
            case STARTS_WITH:
                return value.getString().startsWith(literal.getText());
            case ENDS_WITH:
                return value.getString().endsWith(literal.getText());
            case CONTAINS:
                return value.getString().contains(literal.getText());
            case GT:
                return value.number(type) > literal.getNumber();
            case GE:
                return value.number(type) >= literal.getNumber();
            case LT:
                return value.number(type) < literal.getNumber();
            case LE:
                return value.number(type) <= literal.getNumber();
            default:
                return false;
        }
    }

    /**
     * Whether the value read equals a literal, ordinally for strings.
     */
    private boolean equal(Slot value, Literal other) {
        switch (type) {
            case BOOL:
                return other.getKind() == ScriptNode.Kind.BOOL &&
                    value.getBool() == other.getBool();
            case STRING:
                return other.getKind() == ScriptNode.Kind.STRING &&
                    value.getString().equals(other.getText());
            default:
                return other.getKind() == ScriptNode.Kind.NUMBER &&
                    value.number(type) == other.getNumber();
        }
    }
}
