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
 * One value of a parsed script. A mapping keeps its keys in the order they
 * are written, because checks are numbered and faults reported in that
 * order. Numbers are held as doubles, because format 1 infers a number's
 * type from its value rather than from how it was written.
 */
final class ScriptNode {

    /**
     * The kinds of value a node holds.
     */
    enum Kind { NULL, BOOL, NUMBER, STRING, LIST, MAP }

    private final Kind kind;
    private final boolean bool;
    private final double number;
    private final String text;
    private final List<String> keys;
    private final List<ScriptNode> values;
    private final List<ScriptNode> items;
    private final int line;

    private ScriptNode(
        Kind kind,
        boolean bool,
        double number,
        String text,
        List<String> keys,
        List<ScriptNode> values,
        List<ScriptNode> items,
        int line) {
        this.kind = kind;
        this.bool = bool;
        this.number = number;
        this.text = text;
        this.keys = keys;
        this.values = values;
        this.items = items;
        this.line = line;
    }

    static ScriptNode ofNull(int line) {
        return new ScriptNode(Kind.NULL, false, 0, "",
            Collections.<String>emptyList(),
            Collections.<ScriptNode>emptyList(),
            Collections.<ScriptNode>emptyList(), line);
    }

    static ScriptNode ofBool(boolean value, int line) {
        return new ScriptNode(Kind.BOOL, value, 0, "",
            Collections.<String>emptyList(),
            Collections.<ScriptNode>emptyList(),
            Collections.<ScriptNode>emptyList(), line);
    }

    static ScriptNode ofNumber(double value, int line) {
        return new ScriptNode(Kind.NUMBER, false, value, "",
            Collections.<String>emptyList(),
            Collections.<ScriptNode>emptyList(),
            Collections.<ScriptNode>emptyList(), line);
    }

    static ScriptNode ofString(String value, int line) {
        return new ScriptNode(Kind.STRING, false, 0, value,
            Collections.<String>emptyList(),
            Collections.<ScriptNode>emptyList(),
            Collections.<ScriptNode>emptyList(), line);
    }

    static ScriptNode ofList(List<ScriptNode> items, int line) {
        return new ScriptNode(Kind.LIST, false, 0, "",
            Collections.<String>emptyList(),
            Collections.<ScriptNode>emptyList(),
            Collections.unmodifiableList(items), line);
    }

    static ScriptNode ofMap(
        List<String> keys,
        List<ScriptNode> values,
        int line) {
        return new ScriptNode(Kind.MAP, false, 0, "",
            Collections.unmodifiableList(keys),
            Collections.unmodifiableList(values),
            Collections.<ScriptNode>emptyList(), line);
    }

    Kind getKind() {
        return kind;
    }

    boolean getBool() {
        return bool;
    }

    double getNumber() {
        return number;
    }

    /**
     * @return the text of a string, or empty for any other kind
     */
    String getText() {
        return text;
    }

    /**
     * @return the keys of a mapping, in the order written
     */
    List<String> getKeys() {
        return keys;
    }

    /**
     * @return the values of a mapping, in the order of its keys
     */
    List<ScriptNode> getValues() {
        return values;
    }

    /**
     * @return the members of a list
     */
    List<ScriptNode> getItems() {
        return items;
    }

    /**
     * @return the one based line the value starts on, or 0 where unknown
     */
    int getLine() {
        return line;
    }

    boolean isCollection() {
        return kind == Kind.MAP || kind == Kind.LIST;
    }

    /**
     * The value of a mapping's key, matched without regard to case.
     * @param name the key
     * @return the value, or null where this is not a mapping or has no
     * such key
     */
    ScriptNode get(String name) {
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).equalsIgnoreCase(name)) {
                return values.get(i);
            }
        }
        return null;
    }

    /**
     * The value of a mapping's key, matched exactly as written.
     * @param key the key
     * @return the value, or null where there is no such key
     */
    ScriptNode valueOf(String key) {
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).equals(key)) {
                return values.get(i);
            }
        }
        return null;
    }
}
