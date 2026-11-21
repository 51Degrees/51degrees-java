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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prints a model as canonical JSON, with PascalCase keys, two space indent,
 * literal types preserved, and the inferred types and computed dependencies
 * included, as tools/canonical.mjs in the derived-properties repository
 * does. The debug log shows this text, so anyone holding the log can
 * reconstruct what was evaluated, and a YAML script and the JSON that
 * mirrors it print the same.
 */
final class CanonicalJson {

    private static final String INDENT = "  ";

    /**
     * JSON text written as it is.
     */
    private static final class Raw {
        private final String text;

        Raw(String text) {
            this.text = text;
        }
    }

    private CanonicalJson() {
    }

    /**
     * Print a model.
     * @param script the model
     * @return the canonical JSON, without a trailing new line
     */
    static String print(DerivedScript script) {
        StringBuilder text = new StringBuilder();
        write(text, object(script), 0);
        return text.toString();
    }

    private static Map<String, Object> object(DerivedScript script) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("Format", new Raw("1"));
        result.put("Name", script.getName());
        result.put("Version", script.getVersion());
        if (script.isDeprecated()) {
            result.put("Deprecated", Boolean.TRUE);
            result.put("DeprecationNote", script.getDeprecationNote());
        }
        result.put("Output", output(script.getOutput()));

        // The inferred type of every source property, which the file does
        // not carry because the literals compared with it give it.
        Map<String, Object> properties = new LinkedHashMap<>();
        for (SourceProperty property : script.getProperties()) {
            Map<String, Object> type = new LinkedHashMap<>();
            type.put("Type", property.getType().toString());
            properties.put(property.getName(), type);
        }
        result.put("Properties", properties);

        if (script.getChecks().isEmpty() == false) {
            Map<String, Object> checks = new LinkedHashMap<>();
            for (DerivedScript.NamedCheck check : script.getChecks()) {
                checks.put(check.getName(), condition(check.getCondition()));
            }
            result.put("Checks", checks);
        }
        List<Object> rules = new ArrayList<>();
        for (DerivedScript.Rule rule : script.getRules()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            if (rule.getWhen() == null) {
                entry.put("Else", literal(rule.getValue()));
            } else {
                entry.put("When", condition(rule.getWhen()));
                entry.put("Then", literal(rule.getValue()));
            }
            rules.add(entry);
        }
        result.put("Rules", rules);
        return result;
    }

    private static Map<String, Object> output(OutputModel output) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("Name", output.getName());
        if (output.getDescription().isEmpty() == false) {
            result.put("Description", output.getDescription());
        }
        if (output.getValueType() != null) {
            result.put("ValueType", output.getValueType().toString());
        }
        putIfGiven(result, "StoredValueType", output.getStoredValueType());
        putIfGiven(result, "DefaultValue", output.getDefaultValue());
        result.put("IsList", output.isList());
        putIfGiven(result, "IsMandatory", output.getIsMandatory());
        putIfGiven(result, "IsObsolete", output.getIsObsolete());
        putIfGiven(result, "Category", output.getCategory());
        putIfGiven(result, "IsPopular", output.getIsPopular());
        putIfGiven(result, "ExportValues", output.getExportValues());
        putIfGiven(result, "Url", output.getUrl());
        if (output.getDisplayOrder() != null) {
            result.put("DisplayOrder",
                new Raw(JsText.number(output.getDisplayOrder())));
        }
        if (output.getPropertyId() != null) {
            result.put("PropertyId",
                new Raw(JsText.number(output.getPropertyId())));
        }
        if (output.getVendorIds() != null) {
            result.put("VendorIds", node(output.getVendorIds()));
        }
        if (output.getDependencies() != null) {
            result.put("Dependencies", node(output.getDependencies()));
        } else {
            result.put("Dependencies",
                new ArrayList<Object>(output.getComputed()));
        }
        if (output.getValues() != null) {
            List<Object> values = new ArrayList<>();
            for (OutputModel.ValueEntry value : output.getValues()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("Name", literal(value.getName()));
                if (value.getDescription() != null) {
                    entry.put("Description", value.getDescription());
                }
                values.add(entry);
            }
            result.put("Values", values);
        }
        return result;
    }

    private static void putIfGiven(
        Map<String, Object> result,
        String key,
        Object value) {
        if (value != null) {
            result.put(key, value);
        }
    }

    private static Map<String, Object> condition(Condition condition) {
        Map<String, Object> result = new LinkedHashMap<>();
        switch (condition.getKind()) {
            case COMPARE:
                result.put("Property", condition.getProperty());
                result.put(condition.getOperator().toString(),
                    node(condition.getOperand()));
                break;
            case CHECK:
                result.put("Check", condition.getCheck());
                break;
            case AGGREGATE:
                result.put(
                    condition.isCountPassed() ? "Passed" : "Failed",
                    condition.getGroupNames() == null ?
                        "Checks" :
                        new ArrayList<Object>(condition.getGroupNames()));
                result.put(condition.getOperator().toString(),
                    new Raw(JsText.number(condition.getCount())));
                break;
            case ALL:
            case ANY:
                List<Object> items = new ArrayList<>();
                for (int i = 0; i < condition.itemCount(); i++) {
                    items.add(condition(condition.item(i)));
                }
                result.put(
                    condition.getKind() == Condition.Kind.ALL ? "All" : "Any",
                    items);
                break;
            case NOT:
                result.put("Not", condition(condition.item(0)));
                break;
            default:
                break;
        }
        return result;
    }

    private static Object literal(Literal literal) {
        switch (literal.getKind()) {
            case BOOL:
                return literal.getBool();
            case NUMBER:
                return new Raw(JsText.number(literal.getNumber()));
            default:
                return literal.getText();
        }
    }

    /**
     * A value from the script with its types as written.
     */
    private static Object node(ScriptNode node) {
        switch (node.getKind()) {
            case NULL:
            case BOOL:
            case NUMBER:
                return new Raw(DerivedScriptValidator.jsonOf(node));
            case STRING:
                return node.getText();
            case LIST:
                List<Object> items = new ArrayList<>();
                for (ScriptNode item : node.getItems()) {
                    items.add(node(item));
                }
                return items;
            default:
                Map<String, Object> result = new LinkedHashMap<>();
                for (int i = 0; i < node.getKeys().size(); i++) {
                    result.put(
                        node.getKeys().get(i),
                        node(node.getValues().get(i)));
                }
                return result;
        }
    }

    /**
     * Write a value as JSON.stringify does with an indent of two.
     */
    private static void write(StringBuilder text, Object value, int depth) {
        if (value instanceof Raw) {
            text.append(((Raw) value).text);
        } else if (value instanceof String) {
            text.append(JsText.quote((String) value));
        } else if (value instanceof Boolean) {
            text.append(((Boolean) value) ? "true" : "false");
        } else if (value instanceof List<?>) {
            List<?> items = (List<?>) value;
            if (items.isEmpty()) {
                text.append("[]");
                return;
            }
            text.append('[');
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    text.append(',');
                }
                newLine(text, depth + 1);
                write(text, items.get(i), depth + 1);
            }
            newLine(text, depth);
            text.append(']');
        } else if (value instanceof Map<?, ?>) {
            Map<?, ?> entries = (Map<?, ?>) value;
            if (entries.isEmpty()) {
                text.append("{}");
                return;
            }
            text.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : entries.entrySet()) {
                if (first == false) {
                    text.append(',');
                }
                first = false;
                newLine(text, depth + 1);
                text.append(JsText.quote((String) entry.getKey()))
                    .append(": ");
                write(text, entry.getValue(), depth + 1);
            }
            newLine(text, depth);
            text.append('}');
        }
    }

    private static void newLine(StringBuilder text, int depth) {
        text.append('\n');
        for (int i = 0; i < depth; i++) {
            text.append(INDENT);
        }
    }
}
