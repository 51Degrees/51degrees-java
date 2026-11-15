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

import java.util.Set;

/**
 * A compiled condition. Evaluation is two-valued: every condition is true
 * or false, because a script only runs once every source property it names
 * has been read. The compiled tree is immutable.
 */
interface Condition {

    /**
     * Evaluates the condition.
     * @param values the converted value of each of the script's source
     *               properties, by index
     * @param checks the result of each of the script's checks, by index
     * @return true or false
     */
    boolean test(Object[] values, boolean[] checks);

    /**
     * The operators of a comparison.
     */
    enum Operator {
        EQ("Eq"), NE("Ne"), GT("Gt"), GE("Ge"), LT("Lt"), LE("Le"),
        IN("In"), NOTIN("NotIn"),
        STARTSWITH("StartsWith"), ENDSWITH("EndsWith"), CONTAINS("Contains");

        final String text;

        Operator(String text) {
            this.text = text;
        }

        static Operator parse(String key) {
            for (Operator operator : values()) {
                if (operator.text.equalsIgnoreCase(key)) {
                    return operator;
                }
            }
            return null;
        }

        boolean allowedOn(ValueType type) {
            switch (this) {
                case GT: case GE: case LT: case LE:
                    return type == ValueType.INT || type == ValueType.DOUBLE;
                case STARTSWITH: case ENDSWITH: case CONTAINS:
                    return type == ValueType.STRING;
                default:
                    return true;
            }
        }

        boolean isOrdering() {
            return this == EQ || this == NE || this == GT || this == GE ||
                this == LT || this == LE;
        }

        boolean compare(int order) {
            switch (this) {
                case EQ: return order == 0;
                case NE: return order != 0;
                case GT: return order > 0;
                case GE: return order >= 0;
                case LT: return order < 0;
                default: return order <= 0;
            }
        }
    }

    /**
     * A source property compared with a literal or a list of literals.
     */
    final class Comparison implements Condition {
        private final int index;
        private final Operator operator;
        private final Object literal;
        private final Set<Object> members;

        Comparison(int index, Operator operator, Object literal,
                   Set<Object> members) {
            this.index = index;
            this.operator = operator;
            this.literal = literal;
            this.members = members;
        }

        @Override
        public boolean test(Object[] values, boolean[] checks) {
            Object value = values[index];
            switch (operator) {
                case IN:
                    return members.contains(value);
                case NOTIN:
                    return members.contains(value) == false;
                case STARTSWITH:
                    return ((String) value).startsWith((String) literal);
                case ENDSWITH:
                    return ((String) value).endsWith((String) literal);
                case CONTAINS:
                    return ((String) value).contains((String) literal);
                default:
                    return operator.compare(order(value, literal));
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static int order(Object value, Object literal) {
            if (value instanceof Boolean || value instanceof String) {
                return value.equals(literal) ? 0 : 1;
            }
            return ((Comparable) value).compareTo(literal);
        }
    }

    /**
     * The result a named check already produced.
     */
    final class CheckReference implements Condition {
        private final int check;

        CheckReference(int check) {
            this.check = check;
        }

        @Override
        public boolean test(Object[] values, boolean[] checks) {
            return checks[check];
        }
    }

    /**
     * A count of the checks in a group that passed or failed, compared
     * with a whole number.
     */
    final class Aggregate implements Condition {
        private final boolean passed;
        private final int[] group;
        private final Operator operator;
        private final int operand;

        Aggregate(boolean passed, int[] group, Operator operator, int operand) {
            this.passed = passed;
            this.group = group;
            this.operator = operator;
            this.operand = operand;
        }

        @Override
        public boolean test(Object[] values, boolean[] checks) {
            int count = 0;
            for (int check : group) {
                if (checks[check] == passed) {
                    count++;
                }
            }
            return operator.compare(Integer.compare(count, operand));
        }
    }

    /**
     * True where every member is true.
     */
    final class All implements Condition {
        private final Condition[] members;

        All(Condition[] members) {
            this.members = members;
        }

        @Override
        public boolean test(Object[] values, boolean[] checks) {
            for (Condition member : members) {
                if (member.test(values, checks) == false) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * True where at least one member is true.
     */
    final class Any implements Condition {
        private final Condition[] members;

        Any(Condition[] members) {
            this.members = members;
        }

        @Override
        public boolean test(Object[] values, boolean[] checks) {
            for (Condition member : members) {
                if (member.test(values, checks)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * True becomes false and false becomes true.
     */
    final class Not implements Condition {
        private final Condition member;

        Not(Condition member) {
            this.member = member;
        }

        @Override
        public boolean test(Object[] values, boolean[] checks) {
            return member.test(values, checks) == false;
        }
    }
}
