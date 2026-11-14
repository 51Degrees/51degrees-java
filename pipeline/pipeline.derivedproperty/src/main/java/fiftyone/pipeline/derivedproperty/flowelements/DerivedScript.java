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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A validated script, the model every language builds from the same text.
 * Nothing in it changes once it is built.
 */
final class DerivedScript {

    /**
     * A check, which is a named condition.
     */
    static final class NamedCheck {
        private final String name;
        private final Condition condition;

        NamedCheck(String name, Condition condition) {
            this.name = name;
            this.condition = condition;
        }

        String getName() {
            return name;
        }

        Condition getCondition() {
            return condition;
        }
    }

    /**
     * A When with a Then, or, where the condition is null, an Else.
     */
    static final class Rule {
        private final Condition when;
        private final Literal value;

        Rule(Condition when, Literal value) {
            this.when = when;
            this.value = value;
        }

        /**
         * @return the condition, or null for an Else
         */
        Condition getWhen() {
            return when;
        }

        Literal getValue() {
            return value;
        }
    }

    private final int format;
    private final String name;
    private final String version;
    private final boolean deprecated;
    private final String deprecationNote;
    private final String source;
    private final OutputModel output;
    private final List<SourceProperty> properties;
    private final List<NamedCheck> checks;
    private final List<Rule> rules;

    /**
     * @param properties the source properties in the order the script first
     *                   names them, checks before rules. A comparison reads
     *                   its property from the slot with the same index.
     */
    DerivedScript(
        int format,
        String name,
        String version,
        boolean deprecated,
        String deprecationNote,
        String source,
        OutputModel output,
        List<SourceProperty> properties,
        List<NamedCheck> checks,
        List<Rule> rules) {
        this.format = format;
        this.name = name;
        this.version = version;
        this.deprecated = deprecated;
        this.deprecationNote = deprecationNote;
        this.source = source;
        this.output = output;
        this.properties = Collections.unmodifiableList(
            new ArrayList<>(properties));
        this.checks = Collections.unmodifiableList(new ArrayList<>(checks));
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
    }

    int getFormat() {
        return format;
    }

    String getName() {
        return name;
    }

    String getVersion() {
        return version;
    }

    boolean isDeprecated() {
        return deprecated;
    }

    String getDeprecationNote() {
        return deprecationNote;
    }

    /**
     * @return the built-in script's name, the file path, or "code"
     */
    String getSource() {
        return source;
    }

    OutputModel getOutput() {
        return output;
    }

    List<SourceProperty> getProperties() {
        return properties;
    }

    List<NamedCheck> getChecks() {
        return checks;
    }

    List<Rule> getRules() {
        return rules;
    }

    /**
     * Run the script on the source properties read for it. Every slot is
     * available, so every condition is true or false, and the Else every
     * script ends in always matches.
     * @param slots the source properties, in the order of
     *              {@link #getProperties()}
     * @return the value of the first rule whose condition is true
     */
    Literal evaluate(Slot[] slots) {
        boolean[] results = new boolean[checks.size()];
        for (int i = 0; i < results.length; i++) {
            results[i] = checks.get(i).getCondition().test(slots, results);
        }
        for (Rule rule : rules) {
            if (rule.getWhen() == null ||
                rule.getWhen().test(slots, results)) {
                return rule.getValue();
            }
        }
        throw new IllegalStateException(
            "The rules of '" + output.getName() + "' do not end in an Else.");
    }
}
