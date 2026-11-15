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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The model a format 1 script becomes once it has been parsed, validated
 * and compiled. It holds no text and no reference to where the script came
 * from beyond the name of the source, and it is immutable, so concurrent
 * requests need nothing further.
 */
public final class DerivedScript {

    /**
     * The closing sentence of every no-value message, from
     * {@code USUAL_CAUSES} in the format's reference implementation.
     */
    public static final String USUAL_CAUSES =
        "Usual causes are the element that supplies the property not being " +
        "in the pipeline, or being added after this element rather than " +
        "before it, the property being excluded in the engine " +
        "configuration, the property not being included in the resource " +
        "key, or JavaScript that populates the property not having run yet.";

    /**
     * A source property a script names, as {@code elementDataKey.Property}
     * with the type inferred from the literals it is compared with.
     */
    public static final class SourceProperty {
        private final String elementDataKey;
        private final String property;
        private final ValueType type;

        SourceProperty(String elementDataKey, String property, ValueType type) {
            this.elementDataKey = elementDataKey;
            this.property = property;
            this.type = type;
        }

        public String getElementDataKey() {
            return elementDataKey;
        }

        public String getProperty() {
            return property;
        }

        public ValueType getType() {
            return type;
        }

        /**
         * The property as a script names it.
         * @return {@code elementDataKey.Property}
         */
        public String getName() {
            return elementDataKey + "." + property;
        }
    }

    /**
     * A rule: a condition, or none for the closing Else, and the value it
     * gives.
     */
    static final class Rule {
        final Condition when;
        final Object value;

        Rule(Condition when, Object value) {
            this.when = when;
            this.value = value;
        }
    }

    private final String name;
    private final String version;
    private final String source;
    private final boolean deprecated;
    private final String deprecationNote;
    private final String outputName;
    private final ValueType valueType;
    private final String category;
    private final Map<String, Object> output;
    private final List<SourceProperty> sources;
    private final Condition[] checks;
    private final int[] checkOrder;
    private final Rule[] rules;

    DerivedScript(
        String name,
        String version,
        String source,
        boolean deprecated,
        String deprecationNote,
        String outputName,
        ValueType valueType,
        String category,
        Map<String, Object> output,
        List<SourceProperty> sources,
        Condition[] checks,
        int[] checkOrder,
        Rule[] rules) {
        this.name = name;
        this.version = version;
        this.source = source;
        this.deprecated = deprecated;
        this.deprecationNote = deprecationNote;
        this.outputName = outputName;
        this.valueType = valueType;
        this.category = category;
        this.output = Collections.unmodifiableMap(new LinkedHashMap<>(output));
        this.sources = Collections.unmodifiableList(sources);
        this.checks = checks;
        this.checkOrder = checkOrder;
        this.rules = rules;
    }

    /** @return the name configuration selects the script by */
    public String getName() {
        return name;
    }

    /** @return the author's version, for the build log */
    public String getVersion() {
        return version;
    }

    /** @return the built-in name, the file path, or the word code */
    public String getSource() {
        return source;
    }

    /** @return true for a deprecated script */
    public boolean isDeprecated() {
        return deprecated;
    }

    /** @return what to use instead of a deprecated script, or null */
    public String getDeprecationNote() {
        return deprecationNote;
    }

    /** @return the name of the property the script produces */
    public String getOutputName() {
        return outputName;
    }

    /** @return the type of the property the script produces */
    public ValueType getValueType() {
        return valueType;
    }

    /** @return the category from the output block, or an empty string */
    public String getCategory() {
        return category;
    }

    /** @return the full output block, the definition of the property */
    public Map<String, Object> getOutput() {
        return output;
    }

    /**
     * @return the source properties, in the order the script first names
     * them, checks before rules
     */
    public List<SourceProperty> getSources() {
        return sources;
    }

    /**
     * Runs the checks and then the rules, once every source property has
     * been read.
     * @param values the converted value of each source property, in the
     *               order of {@link #getSources()}
     * @return the value of the first rule whose condition is true
     */
    public Object evaluate(Object[] values) {
        boolean[] results = new boolean[checks.length];
        for (int check : checkOrder) {
            results[check] = checks[check].test(values, results);
        }
        for (Rule rule : rules) {
            if (rule.when == null || rule.when.test(values, results)) {
                return rule.value;
            }
        }
        // Validation makes the last rule an Else, so this is not reached.
        throw new IllegalStateException("No rule matched in '" + name + "'");
    }

    /**
     * The message of a value with no value, naming every source property
     * that was not available and why, in the shape the format sets out.
     * @param reasons the reason for each source property, in the order of
     *                {@link #getSources()}, null where it was available
     * @return the message
     */
    public String noValueMessage(String[] reasons) {
        int absent = 0;
        StringBuilder each = new StringBuilder();
        for (int i = 0; i < reasons.length; i++) {
            if (reasons[i] != null) {
                absent++;
                each.append(" '").append(sources.get(i).getName())
                    .append("' (").append(reasons[i]).append(").");
            }
        }
        return "Derived property '" + outputName + "' has no value because " +
            (absent == 1 ? "1 source property was not available." :
                absent + " source properties were not available.") +
            each + " " + USUAL_CAUSES;
    }
}
