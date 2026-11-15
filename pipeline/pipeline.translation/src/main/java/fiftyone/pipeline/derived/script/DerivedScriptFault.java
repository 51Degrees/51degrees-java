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

/**
 * One fault found in a derived property script, carrying the script name,
 * where the script came from (the built-in name, the file path or the word
 * {@code code}), a path in the document such as
 * {@code Rules[3].When.All[1]}, and a plain message. The parser used does
 * not supply line numbers to the validator, so no line is given.
 */
public class DerivedScriptFault {
    private final String script;
    private final String source;
    private final String path;
    private final String message;

    /**
     * Construct a new fault.
     * @param script the script name, or the source where no name was read
     * @param source where the script came from
     * @param path the path in the document, empty for the document itself
     * @param message what is wrong
     */
    public DerivedScriptFault(
        String script,
        String source,
        String path,
        String message) {
        this.script = script;
        this.source = source;
        this.path = path;
        this.message = message;
    }

    public String getScript() {
        return script;
    }

    public String getSource() {
        return source;
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Script '" + script + "' (" + source + ")" +
            (path == null || path.isEmpty() ? "" : " at " + path) +
            ": " + message;
    }
}
