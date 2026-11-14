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

/**
 * A scalar from a script, being a boolean, a number or a string.
 */
final class Literal {

    /**
     * What a value the validator refused becomes. The model holding it is
     * discarded before anything reads it.
     */
    static final Literal NONE =
        new Literal(ScriptNode.Kind.NULL, false, 0, "");

    private final ScriptNode.Kind kind;
    private final boolean bool;
    private final double number;
    private final String text;

    private Literal(
        ScriptNode.Kind kind,
        boolean bool,
        double number,
        String text) {
        this.kind = kind;
        this.bool = bool;
        this.number = number;
        this.text = text;
    }

    static Literal of(ScriptNode node) {
        return new Literal(
            node.getKind(),
            node.getBool(),
            node.getNumber(),
            node.getText());
    }

    ScriptNode.Kind getKind() {
        return kind;
    }

    boolean getBool() {
        return bool;
    }

    double getNumber() {
        return number;
    }

    String getText() {
        return text;
    }

    /**
     * The string form of the literal, as JavaScript's String gives it,
     * which is how a rule's value is compared with Output.Values.
     * @return the string form
     */
    @Override
    public String toString() {
        switch (kind) {
            case BOOL:
                return bool ? "true" : "false";
            case NUMBER:
                return JsText.number(number);
            default:
                return text;
        }
    }
}
