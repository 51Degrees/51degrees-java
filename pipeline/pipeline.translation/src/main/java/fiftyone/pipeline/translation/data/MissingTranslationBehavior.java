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

package fiftyone.pipeline.translation.data;

/**
 * Defines the behaviour of the translation engine when a translation is
 * missing for a value.
 */
public enum MissingTranslationBehavior {

    /**
     * Use the original value if there is no translation for it. This is the
     * default behaviour.
     */
    ORIGINAL("Original"),

    /**
     * Use an empty string if there is no translation for the value.
     */
    EMPTY_STRING("EmptyString"),

    /**
     * Add a flow error if there is no translation for the value. The error
     * contains the reason there was no translation.
     */
    FLOW_ERROR("FlowError");

    /**
     * The name the specification gives the behaviour, which a configuration
     * file uses.
     */
    private final String specificationName;

    MissingTranslationBehavior(String specificationName) {
        this.specificationName = specificationName;
    }

    /**
     * Reads a behaviour from text, as a configuration file gives it. The
     * names the specification uses, Original, EmptyString and FlowError, and
     * the names of these constants are accepted, ignoring case and any white
     * space around the text.
     * @param value the text to read
     * @return the behaviour the text names
     * @throws IllegalArgumentException if the text names no behaviour
     */
    public static MissingTranslationBehavior parse(String value) {
        if (value != null) {
            String text = value.trim();
            for (MissingTranslationBehavior behavior : values()) {
                if (behavior.specificationName.equalsIgnoreCase(text) ||
                    behavior.name().equalsIgnoreCase(text)) {
                    return behavior;
                }
            }
        }
        StringBuilder accepted = new StringBuilder();
        MissingTranslationBehavior[] behaviors = values();
        for (int i = 0; i < behaviors.length; i++) {
            if (i > 0) {
                accepted.append(i == behaviors.length - 1 ? " or " : ", ");
            }
            accepted.append(behaviors[i].specificationName);
        }
        throw new IllegalArgumentException("'" + value + "' is not a " +
            "missing translation behavior. Use " + accepted + ".");
    }
}
