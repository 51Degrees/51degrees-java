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

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reads the text of a script, as YAML or as JSON, into {@link ScriptNode}s.
 * <p>
 * JSON is read as the JSON subset of YAML 1.2 by the same reader. The
 * nodes are composed rather than constructed, so every node keeps its line,
 * and a plain scalar is resolved here by the YAML 1.2 core schema, under
 * which only {@code true} and {@code false} in their three spellings are
 * booleans, so {@code yes} and {@code NO} stay strings. A quoted scalar is
 * always a string.
 * <p>
 * See
 * <a href="https://yaml.org/spec/1.2.2/#1032-tag-resolution">YAML 1.2.2,
 * 10.3.2 Tag Resolution</a> and
 * <a href="https://github.com/51Degrees/derived-properties/blob/cc30638632050795d9acc80d5118a290ffaba085/docs/format-1.md#two-places-where-the-languages-are-allowed-to-differ">format
 * 1, two places where the languages are allowed to differ</a>.
 */
final class DerivedScriptParser {

    /**
     * How deep a value may nest, which only aliases that refer to
     * themselves can reach, because the reader limits nesting written out
     * in the text to less than this.
     */
    private static final int MAX_DEPTH = 100;

    private static final Pattern DECIMAL_INT = Pattern.compile("[-+]?[0-9]+");
    private static final Pattern OCTAL_INT = Pattern.compile("0o[0-7]+");
    private static final Pattern HEX_INT = Pattern.compile("0x[0-9a-fA-F]+");
    private static final Pattern FLOAT = Pattern.compile(
        "[-+]?(\\.[0-9]+|[0-9]+(\\.[0-9]*)?)([eE][-+]?[0-9]+)?");
    private static final Pattern INFINITY =
        Pattern.compile("[-+]?\\.(inf|Inf|INF)");
    private static final Pattern NOT_A_NUMBER =
        Pattern.compile("\\.(nan|NaN|NAN)");

    private static final int OCTAL_RADIX = 8;
    private static final int HEX_RADIX = 16;

    /**
     * The length of the 0o or 0x prefix.
     */
    private static final int RADIX_PREFIX_LENGTH = 2;

    private DerivedScriptParser() {
    }

    /**
     * Parse the text of a script.
     * @param text the script, as YAML or JSON
     * @return the document, which is always a mapping
     * @throws ScriptParseException where the text is not YAML or JSON, is
     * empty, is not a mapping, or writes a key twice
     */
    static ScriptNode parse(String text) throws ScriptParseException {
        if (text == null) {
            throw new ScriptParseException("the script is empty", 0);
        }
        String what = startsLikeJson(text) ? "JSON" : "YAML";
        Node root;
        try {
            root = new Yaml(new LoaderOptions()).compose(
                new StringReader(text));
        } catch (MarkedYAMLException e) {
            Mark mark = e.getProblemMark() != null ?
                e.getProblemMark() : e.getContextMark();
            String problem = e.getProblem() != null ?
                e.getProblem() : e.getMessage();
            throw new ScriptParseException(
                "the text is not valid " + what + ": " + problem,
                mark == null ? 0 : mark.getLine() + 1);
        } catch (YAMLException e) {
            throw new ScriptParseException(
                "the text is not valid " + what + ": " + e.getMessage(), 0);
        }
        if (root == null) {
            throw new ScriptParseException("the script is empty", 0);
        }
        ScriptNode document = convert(root, 0);
        if (document.getKind() == ScriptNode.Kind.NULL) {
            throw new ScriptParseException("the script is empty", 0);
        }
        if (document.getKind() != ScriptNode.Kind.MAP) {
            throw new ScriptParseException(
                "the script must be a mapping of keys to values at the top " +
                    "level",
                0);
        }
        requireDistinctKeys(document);
        return document;
    }

    /**
     * Whether the text opens with a brace once leading white space is
     * passed over, which is how the reference tools tell JSON from YAML.
     */
    private static boolean startsLikeJson(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (JsText.isWhitespace(c) == false) {
                return c == '{';
            }
        }
        return false;
    }

    private static ScriptNode convert(Node node, int depth)
        throws ScriptParseException {
        int line = node.getStartMark() == null ?
            0 : node.getStartMark().getLine() + 1;
        if (depth > MAX_DEPTH) {
            throw new ScriptParseException(
                "the text is not valid YAML: aliases nest too deeply", line);
        }
        if (node instanceof MappingNode) {
            List<String> keys = new ArrayList<>();
            List<ScriptNode> values = new ArrayList<>();
            for (NodeTuple tuple : ((MappingNode) node).getValue()) {
                keys.add(keyText(tuple.getKeyNode()));
                values.add(convert(tuple.getValueNode(), depth + 1));
            }
            return ScriptNode.ofMap(keys, values, line);
        }
        if (node instanceof SequenceNode) {
            List<ScriptNode> items = new ArrayList<>();
            for (Node item : ((SequenceNode) node).getValue()) {
                items.add(convert(item, depth + 1));
            }
            return ScriptNode.ofList(items, line);
        }
        if (node instanceof ScalarNode) {
            return scalar((ScalarNode) node, line);
        }
        return ScriptNode.ofNull(line);
    }

    /**
     * A key is the text it is written as, as JavaScript makes every key of
     * an object a string.
     */
    private static String keyText(Node key) {
        return key instanceof ScalarNode ? ((ScalarNode) key).getValue() : "";
    }

    /**
     * Resolve a scalar. A plain scalar takes the tag the YAML 1.2 core
     * schema gives it, and any other scalar is a string.
     */
    static ScriptNode scalar(ScalarNode node, int line) {
        String value = node.getValue();
        if (node.isPlain() == false) {
            return ScriptNode.ofString(value, line);
        }
        switch (value) {
            case "":
            case "~":
            case "null":
            case "Null":
            case "NULL":
                return ScriptNode.ofNull(line);
            case "true":
            case "True":
            case "TRUE":
                return ScriptNode.ofBool(true, line);
            case "false":
            case "False":
            case "FALSE":
                return ScriptNode.ofBool(false, line);
            default:
                break;
        }
        Double number = coreNumber(value);
        return number == null ?
            ScriptNode.ofString(value, line) :
            ScriptNode.ofNumber(number, line);
    }

    /**
     * The number a plain scalar is under the core schema, or null where it
     * is not one.
     */
    private static Double coreNumber(String value) {
        if (DECIMAL_INT.matcher(value).matches() ||
            FLOAT.matcher(value).matches()) {
            return Double.parseDouble(value);
        }
        if (OCTAL_INT.matcher(value).matches()) {
            return new BigInteger(
                value.substring(RADIX_PREFIX_LENGTH), OCTAL_RADIX)
                .doubleValue();
        }
        if (HEX_INT.matcher(value).matches()) {
            return new BigInteger(
                value.substring(RADIX_PREFIX_LENGTH), HEX_RADIX)
                .doubleValue();
        }
        if (INFINITY.matcher(value).matches()) {
            return value.charAt(0) == '-' ?
                Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        }
        if (NOT_A_NUMBER.matcher(value).matches()) {
            return Double.NaN;
        }
        return null;
    }

    /**
     * Refuse two keys of one mapping that differ only in case, because keys
     * are matched without regard to case and one of the two would be
     * dropped, and refuse a key written twice for the same reason.
     */
    private static void requireDistinctKeys(ScriptNode node)
        throws ScriptParseException {
        if (node.getKind() == ScriptNode.Kind.LIST) {
            for (ScriptNode item : node.getItems()) {
                requireDistinctKeys(item);
            }
            return;
        }
        if (node.getKind() != ScriptNode.Kind.MAP) {
            return;
        }
        Map<String, String> seen = new HashMap<>();
        for (int i = 0; i < node.getKeys().size(); i++) {
            String key = node.getKeys().get(i);
            String first = seen.get(key.toLowerCase(Locale.ROOT));
            if (first != null) {
                if (first.equals(key)) {
                    throw new ScriptParseException(
                        "the key '" + key + "' is written twice in one " +
                            "mapping, so one of the two would be dropped",
                        node.getLine());
                }
                throw new ScriptParseException(
                    "the keys '" + first + "' and '" + key + "' differ only " +
                        "in case, and keys are matched without regard to " +
                        "case, so one of the two would be dropped",
                    node.getLine());
            }
            seen.put(key.toLowerCase(Locale.ROOT), key);
            requireDistinctKeys(node.getValues().get(i));
        }
    }
}
