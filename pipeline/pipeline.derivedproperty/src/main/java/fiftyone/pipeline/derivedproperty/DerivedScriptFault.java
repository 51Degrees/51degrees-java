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

package fiftyone.pipeline.derivedproperty;

import java.io.Serializable;

/**
 * One thing wrong with a derived property script.
 */
public final class DerivedScriptFault implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String script;
    private final String source;
    private final String path;
    private final int line;
    private final String message;

    /**
     * Construct a fault.
     * @param script the name the script was given, or empty where it had
     *               none
     * @param source the built-in script's name, the file path, or "code"
     * @param path the place in the document, such as Rules[3].When.All[1],
     *             or empty for the document as a whole
     * @param line the one based line, or 0 where the parser gave none
     * @param message what is wrong
     */
    public DerivedScriptFault(
        String script,
        String source,
        String path,
        int line,
        String message) {
        this.script = script == null ? "" : script;
        this.source = source == null ? "" : source;
        this.path = path == null ? "" : path;
        this.line = line;
        this.message = message;
    }

    /**
     * @return the name the script was given, or empty where it had none
     */
    public String getScript() {
        return script;
    }

    /**
     * @return the built-in script's name, the file path, or "code"
     */
    public String getSource() {
        return source;
    }

    /**
     * @return the place in the document, such as Rules[3].When.All[1], or
     * empty for the document as a whole
     */
    public String getPath() {
        return path;
    }

    /**
     * @return the one based line, or 0 where the parser gave none
     */
    public int getLine() {
        return line;
    }

    /**
     * @return what is wrong
     */
    public String getMessage() {
        return message;
    }

    /**
     * The fault as one line, giving the script, the source, the line where
     * known, the path and the message.
     * @return the fault as one line
     */
    @Override
    public String toString() {
        return (script.isEmpty() ? "script" : script) +
            " (" + source + ")" +
            (line > 0 ? " line " + line : "") +
            " at " + (path.isEmpty() ? "(document)" : path) +
            ": " + message;
    }
}
