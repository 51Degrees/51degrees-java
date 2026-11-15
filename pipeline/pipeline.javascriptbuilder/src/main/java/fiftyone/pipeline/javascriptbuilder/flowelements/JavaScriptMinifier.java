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

package fiftyone.pipeline.javascriptbuilder.flowelements;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A cut-down minifier for the script the {@link JavaScriptBuilderElement}
 * renders, as the specification allows where a full minifier would cost too
 * much on every request. It makes one pass over the script and:
 * <ul>
 *     <li>Removes comments.</li>
 *     <li>Removes indentation, trailing white space and empty lines.</li>
 *     <li>Collapses other runs of white space to one space, and removes that
 *     space next to punctuation where nothing can join.</li>
 * </ul>
 * Line breaks between statements are kept, so automatic semicolon insertion
 * works as it did. String, template and regular expression literals are
 * copied as they are.
 */
public final class JavaScriptMinifier {

    /**
     * Punctuation that a space can be removed next to. A slash is left out,
     * as are plus and minus, so that {@code a + +b} keeps its meaning, and
     * the angle brackets, so that no HTML comment closer can be formed.
     */
    private static final String TIGHT = "{}()[];,:=?*%&|!";

    /**
     * A slash after one of these characters starts a regular expression
     * rather than a division.
     */
    private static final String BEFORE_REGEX = "(,=:[!&|?{};+-*%<>~^";

    /**
     * A slash after one of these words starts a regular expression.
     */
    private static final Set<String> KEYWORDS_BEFORE_REGEX =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "return", "typeof", "case", "do", "else", "in", "of", "new",
            "delete", "void", "throw", "instanceof", "yield", "await")));

    private JavaScriptMinifier() {
    }

    /**
     * Minify the script given.
     * @param script the JavaScript to minify
     * @return the minified JavaScript, or null if the script is null
     */
    public static String minify(String script) {
        if (script == null) {
            return null;
        }
        int length = script.length();
        StringBuilder out = new StringBuilder(length);
        boolean pendingSpace = false;
        boolean pendingNewline = false;
        int i = 0;
        while (i < length) {
            char c = script.charAt(i);
            char next = i + 1 < length ? script.charAt(i + 1) : '\0';
            if (c == '\n' || c == '\r') {
                pendingNewline = true;
                i++;
                continue;
            }
            if (c == ' ' || c == '\t' || c == '\f' || c == '\u000B') {
                pendingSpace = true;
                i++;
                continue;
            }
            if (c == '/' && next == '/') {
                // A line comment runs to the end of the line, which is kept.
                i += 2;
                while (i < length &&
                    script.charAt(i) != '\n' && script.charAt(i) != '\r') {
                    i++;
                }
                continue;
            }
            if (c == '/' && next == '*') {
                int end = script.indexOf("*/", i + 2);
                end = end < 0 ? length : end + 2;
                // A block comment with a line break in it counts as a line
                // break, and one without counts as a space.
                boolean newline = false;
                for (int j = i; j < end && newline == false; j++) {
                    char d = script.charAt(j);
                    newline = d == '\n' || d == '\r';
                }
                if (newline) {
                    pendingNewline = true;
                } else {
                    pendingSpace = true;
                }
                i = end;
                continue;
            }

            if (out.length() > 0) {
                if (pendingNewline) {
                    out.append('\n');
                } else if (pendingSpace &&
                    needsSpace(out.charAt(out.length() - 1), c)) {
                    out.append(' ');
                }
            }
            pendingSpace = false;
            pendingNewline = false;

            if (c == '\'' || c == '"' || c == '`') {
                i = copyString(script, i, out);
            } else if (c == '/' && regexAllowed(out)) {
                i = copyRegex(script, i, out);
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /**
     * Whether the space between two characters is kept. It is kept between
     * two characters that are not punctuation, and wherever removing it
     * would form a comment or an HTML comment opener.
     */
    private static boolean needsSpace(char before, char after) {
        if ((before == '/' && (after == '/' || after == '*')) ||
            (before == '*' && after == '/') ||
            (before == '<' && after == '!')) {
            return true;
        }
        return TIGHT.indexOf(before) < 0 && TIGHT.indexOf(after) < 0;
    }

    /**
     * Copy the string or template literal that starts at the index given,
     * and return the index after it. A quote string that reaches the end of
     * a line unclosed is not valid, and is copied to there.
     */
    private static int copyString(String script, int start, StringBuilder out) {
        char quote = script.charAt(start);
        int length = script.length();
        out.append(quote);
        int i = start + 1;
        while (i < length) {
            char c = script.charAt(i);
            if (c == '\\' && i + 1 < length) {
                out.append(c).append(script.charAt(i + 1));
                i += 2;
                continue;
            }
            if (quote != '`' && (c == '\n' || c == '\r')) {
                return i;
            }
            out.append(c);
            i++;
            if (c == quote) {
                return i;
            }
        }
        return i;
    }

    /**
     * Copy the regular expression literal that starts at the index given,
     * and return the index after its closing slash. Its flags follow as
     * ordinary characters.
     */
    private static int copyRegex(String script, int start, StringBuilder out) {
        int length = script.length();
        out.append('/');
        int i = start + 1;
        boolean inClass = false;
        while (i < length) {
            char c = script.charAt(i);
            if (c == '\n' || c == '\r') {
                return i;
            }
            if (c == '\\' && i + 1 < length) {
                out.append(c).append(script.charAt(i + 1));
                i += 2;
                continue;
            }
            out.append(c);
            i++;
            if (c == '[') {
                inClass = true;
            } else if (c == ']') {
                inClass = false;
            } else if (c == '/' && inClass == false) {
                return i;
            }
        }
        return i;
    }

    /**
     * Whether a slash written next is the start of a regular expression,
     * from what was written before it.
     */
    private static boolean regexAllowed(StringBuilder out) {
        int i = out.length() - 1;
        while (i >= 0 && (out.charAt(i) == ' ' || out.charAt(i) == '\n')) {
            i--;
        }
        if (i < 0) {
            return true;
        }
        char before = out.charAt(i);
        if (BEFORE_REGEX.indexOf(before) >= 0) {
            return true;
        }
        if (Character.isJavaIdentifierPart(before)) {
            int end = i + 1;
            while (i >= 0 && Character.isJavaIdentifierPart(out.charAt(i))) {
                i--;
            }
            return KEYWORDS_BEFORE_REGEX.contains(out.substring(i + 1, end));
        }
        return false;
    }
}
