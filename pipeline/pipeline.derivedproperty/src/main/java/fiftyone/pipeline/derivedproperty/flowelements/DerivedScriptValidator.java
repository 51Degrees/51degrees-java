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

import fiftyone.pipeline.derivedproperty.DerivedScriptFault;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Checks a parsed script and builds its model, collecting every fault
 * rather than stopping at the first. The paths and the messages are those
 * of the reference implementation, tools/validate.mjs in the
 * derived-properties repository, so a fault reads the same in every
 * language.
 * <p>
 * See <a href="https://github.com/51Degrees/derived-properties/blob/cc30638632050795d9acc80d5118a290ffaba085/docs/format-1.md">format
 * 1</a>.
 */
final class DerivedScriptValidator {

    /**
     * The result of validating one script.
     */
    static final class Result {
        private final DerivedScript script;
        private final List<DerivedScriptFault> faults;

        Result(DerivedScript script, List<DerivedScriptFault> faults) {
            this.script = script;
            this.faults = Collections.unmodifiableList(faults);
        }

        /**
         * @return the model, or null where the script has a fault
         */
        DerivedScript getScript() {
            return script;
        }

        List<DerivedScriptFault> getFaults() {
            return faults;
        }
    }

    static final List<String> VALUE_TYPES =
        Arrays.asList("string", "bool", "int", "double");
    static final List<String> AGGREGATE_NAMES =
        Arrays.asList("Passed", "Failed");
    static final List<String> TOP_LEVEL_KEYS = Arrays.asList(
        "Format", "Name", "Version", "Deprecated", "DeprecationNote",
        "Output", "Checks", "Rules");
    /**
     * The keys of Output, in the order the canonical form prints them.
     */
    static final List<String> OUTPUT_KEYS = Arrays.asList(
        "Name", "Description", "ValueType", "StoredValueType",
        "DefaultValue", "IsList", "IsMandatory", "IsObsolete", "Category",
        "IsPopular", "ExportValues", "Url", "DisplayOrder", "PropertyId",
        "VendorIds", "Dependencies", "Values");
    private static final List<String> COUNT_OPERATORS =
        Arrays.asList("Eq", "Ne", "Gt", "Ge", "Lt", "Le");

    private static final Pattern IDENTIFIER =
        Pattern.compile("^[A-Za-z][A-Za-z0-9]*$");
    private static final Pattern SOURCE_PATTERN =
        Pattern.compile("^[A-Za-z][A-Za-z0-9]*\\.[A-Za-z][A-Za-z0-9]*$");
    private static final Pattern SEMANTIC_VERSION = Pattern.compile(
        "^\\d+\\.\\d+\\.\\d+(?:-[0-9A-Za-z.-]+(?:\\.[0-9A-Za-z.-]+)*)?" +
            "(?:\\+[0-9A-Za-z.-]+)?$");

    /**
     * Where a source property was first seen and the type inferred there,
     * so a conflict can name both places.
     */
    private static final class PropertyUse {
        private final int slot;
        private final ValueType type;
        private final String typePath;

        PropertyUse(int slot, ValueType type, String typePath) {
            this.slot = slot;
            this.type = type;
            this.typePath = typePath;
        }
    }

    private final ScriptNode document;
    private final String script;
    private final String source;
    private final List<DerivedScriptFault> faults = new ArrayList<>();
    private final Map<String, PropertyUse> uses = new HashMap<>();
    private final List<SourceProperty> properties = new ArrayList<>();

    private DerivedScriptValidator(
        ScriptNode document,
        String script,
        String source) {
        this.document = document;
        this.script = script;
        this.source = source;
    }

    /**
     * Parse and validate the text of a script.
     * @param text the script, as YAML or JSON
     * @param name the file name without its extension, which the script's
     *             Name must equal, or empty where there is none
     * @param source the built-in script's name, the file path, or "code"
     * @return the model, or every fault found
     */
    static Result validate(String text, String name, String source) {
        String expected = name == null ? "" : name;
        ScriptNode document;
        try {
            document = DerivedScriptParser.parse(text);
        } catch (ScriptParseException e) {
            List<DerivedScriptFault> faults = new ArrayList<>();
            faults.add(new DerivedScriptFault(
                expected, source, "", e.getLine(), e.getMessage()));
            return new Result(null, faults);
        }
        DerivedScriptValidator validator =
            new DerivedScriptValidator(document, expected, source);
        DerivedScript model = validator.buildModel();
        if (validator.faults.isEmpty() == false) {
            return new Result(null, validator.faults);
        }
        return new Result(model, validator.faults);
    }

    private void fault(String path, ScriptNode at, String message) {
        int line = at != null && at.isCollection() ? at.getLine() : 0;
        faults.add(new DerivedScriptFault(
            script, source, path, line, message));
    }

    private DerivedScript buildModel() {
        for (String key : unknownKeys(document, TOP_LEVEL_KEYS)) {
            fault(key, document, "unknown key '" + key + "' at the top " +
                "level. Expected one of " + String.join(", ", TOP_LEVEL_KEYS));
        }
        int format = readFormat();
        String name = readName();
        String version = readVersion();
        boolean deprecated = readDeprecated();
        String deprecationNote = readDeprecationNote(deprecated);
        OutputModel.Draft output = readOutput();
        // Checks are read before rules, so that a rule can refer to one.
        List<String> checkNames = readCheckNames();
        List<DerivedScript.NamedCheck> checks = readChecks(checkNames);
        List<DerivedScript.Rule> rules = readRules(output, checkNames);
        List<String> computed = new ArrayList<>();
        if (output.dependencies == null) {
            for (SourceProperty property : properties) {
                computed.add(property.getName());
            }
        }
        return new DerivedScript(format, name, version, deprecated,
            deprecationNote, source, new OutputModel(output, computed),
            properties, checks, rules);
    }

    private int readFormat() {
        ScriptNode value = document.get("Format");
        if (value == null) {
            fault("Format", document, "required key 'Format' is missing");
            return 0;
        }
        if (value.getKind() != ScriptNode.Kind.NUMBER ||
            value.getNumber() != 1) {
            fault("Format", document,
                "Format must be 1, found " + jsonOf(value));
            return 0;
        }
        return 1;
    }

    private String readName() {
        ScriptNode value = document.get("Name");
        if (value == null) {
            fault("Name", document, "required key 'Name' is missing");
            return "";
        }
        if (value.getKind() != ScriptNode.Kind.STRING) {
            fault("Name", document,
                "Name expected a string, found " + typeOf(value));
            return "";
        }
        if (IDENTIFIER.matcher(value.getText()).matches() == false) {
            fault("Name", document, "script name '" + value.getText() +
                "' does not match the pattern " + IDENTIFIER.pattern());
            return value.getText();
        }
        if (script.isEmpty() == false &&
            script.equals(value.getText()) == false) {
            fault("Name", document, "script name '" + value.getText() +
                "' must equal the file name '" + script + "'");
        }
        return value.getText();
    }

    private String readVersion() {
        ScriptNode value = document.get("Version");
        if (value == null) {
            fault("Version", document, "required key 'Version' is missing");
            return "";
        }
        if (value.getKind() != ScriptNode.Kind.STRING ||
            SEMANTIC_VERSION.matcher(value.getText()).matches() == false) {
            fault("Version", document, "Version expected a semantic " +
                "version such as 1.0.0, found " + jsonOf(value));
            return "";
        }
        return value.getText();
    }

    private boolean readDeprecated() {
        ScriptNode deprecated = document.get("Deprecated");
        if (deprecated == null) {
            return false;
        }
        if (deprecated.getKind() != ScriptNode.Kind.BOOL) {
            fault("Deprecated", document,
                "Deprecated expected a boolean, found " + typeOf(deprecated));
            return false;
        }
        return deprecated.getBool();
    }

    private String readDeprecationNote(boolean deprecated) {
        ScriptNode note = document.get("DeprecationNote");
        String text = "";
        if (note != null && note.getKind() != ScriptNode.Kind.STRING) {
            fault("DeprecationNote", document,
                "DeprecationNote expected a string, found " + typeOf(note));
        } else if (note != null) {
            text = note.getText();
        }
        if (deprecated && text.isEmpty()) {
            fault("DeprecationNote", document, "a deprecated script must " +
                "say what to use instead in DeprecationNote");
        }
        if (deprecated == false && text.isEmpty() == false) {
            fault("DeprecationNote", document,
                "DeprecationNote is only allowed when Deprecated is true");
        }
        return text;
    }

    private OutputModel.Draft readOutput() {
        OutputModel.Draft output = new OutputModel.Draft();
        String fallbackName = script.isEmpty() ? "unknown" : script;
        ScriptNode raw = document.get("Output");
        if (raw == null) {
            fault("Output", document, "required key 'Output' is missing");
            output.name = fallbackName;
            return output;
        }
        if (raw.getKind() != ScriptNode.Kind.MAP) {
            fault("Output", document,
                "Output expected a mapping, found " + typeOf(raw));
            output.name = fallbackName;
            return output;
        }
        for (String key : unknownKeys(raw, OUTPUT_KEYS)) {
            fault("Output." + key, raw, "unknown key '" + key + "' under " +
                "Output. A typo in a metadata field is a fault rather than " +
                "a value that is quietly dropped");
        }

        ScriptNode name = raw.get("Name");
        if (name == null) {
            fault("Output.Name", raw, "required key 'Name' is missing");
        } else if (name.getKind() != ScriptNode.Kind.STRING ||
            IDENTIFIER.matcher(name.getText()).matches() == false) {
            fault("Output.Name", raw, "Output.Name " + jsonOf(name) +
                " does not match the pattern " + IDENTIFIER.pattern());
        } else {
            output.name = name.getText();
        }
        if (output.name.isEmpty()) {
            output.name = fallbackName;
        }

        ScriptNode description = raw.get("Description");
        if (description == null) {
            fault("Output.Description", raw, "required key 'Description' " +
                "is missing. Say what the property asserts, not how far to " +
                "trust it");
        } else if (description.getKind() != ScriptNode.Kind.STRING ||
            JsText.trim(description.getText()).isEmpty()) {
            fault("Output.Description", raw, "Output.Description expected " +
                "a non empty string, found " + typeOf(description));
        } else {
            output.description = description.getText();
        }

        ScriptNode valueType = raw.get("ValueType");
        if (valueType == null) {
            fault("Output.ValueType", raw,
                "required key 'ValueType' is missing");
        } else if (valueType.getKind() != ScriptNode.Kind.STRING ||
            ValueType.fromText(valueType.getText()) == null) {
            fault("Output.ValueType", raw, "Output.ValueType '" +
                displayOf(valueType) + "' is not allowed in format 1. " +
                "Expected one of " + String.join(", ", VALUE_TYPES));
        } else {
            output.valueType = ValueType.fromText(valueType.getText());
        }

        ScriptNode isList = raw.get("IsList");
        if (isList == null) {
            fault("Output.IsList", raw, "required key 'IsList' is missing");
        } else if (isList.getKind() != ScriptNode.Kind.BOOL) {
            fault("Output.IsList", raw,
                "Output.IsList expected a boolean, found " + typeOf(isList));
        } else if (isList.getBool()) {
            fault("Output.IsList", raw, "Output.IsList must be false in " +
                "format 1. List outputs are deferred");
        }

        readValues(raw, output);

        output.isMandatory = readBoolean(raw, "IsMandatory");
        output.isObsolete = readBoolean(raw, "IsObsolete");
        output.isPopular = readBoolean(raw, "IsPopular");
        output.exportValues = readBoolean(raw, "ExportValues");
        output.category = readString(raw, "Category");
        output.url = readString(raw, "Url");
        output.storedValueType = readString(raw, "StoredValueType");
        output.displayOrder = readInteger(raw, "DisplayOrder");
        output.propertyId = readInteger(raw, "PropertyId");
        output.vendorIds = readList(raw, "VendorIds");
        output.dependencies = readList(raw, "Dependencies");

        // DefaultValue is the string form of the value, so it is checked
        // against the value type and the value list.
        ScriptNode defaultValue = raw.get("DefaultValue");
        if (defaultValue != null) {
            if (defaultValue.getKind() != ScriptNode.Kind.STRING) {
                fault("Output.DefaultValue", raw, "Output.DefaultValue " +
                    "expected a string holding the string form of the " +
                    "value, found " + typeOf(defaultValue));
            } else {
                String text = defaultValue.getText();
                output.defaultValue = text;
                if (output.valueType != null &&
                    SourceValues.convertString(text, output.valueType) ==
                        null) {
                    fault("Output.DefaultValue", raw, "Output.DefaultValue '" +
                        text + "' cannot be read as " + output.valueType);
                }
                if (output.hasValues && output.listsValue(text) == false) {
                    fault("Output.DefaultValue", raw, "Output.DefaultValue '" +
                        text + "' is not one of the values listed under " +
                        "Output.Values");
                }
            }
        }
        return output;
    }

    private Boolean readBoolean(ScriptNode raw, String key) {
        ScriptNode value = raw.get(key);
        if (value == null) {
            return null;
        }
        if (value.getKind() != ScriptNode.Kind.BOOL) {
            fault("Output." + key, raw, "Output." + key +
                " expected a boolean, found " + typeOf(value));
            return null;
        }
        return value.getBool();
    }

    private String readString(ScriptNode raw, String key) {
        ScriptNode value = raw.get(key);
        if (value == null) {
            return null;
        }
        if (value.getKind() != ScriptNode.Kind.STRING) {
            fault("Output." + key, raw, "Output." + key +
                " expected a string, found " + typeOf(value));
            return null;
        }
        return value.getText();
    }

    private Long readInteger(ScriptNode raw, String key) {
        ScriptNode value = raw.get(key);
        if (value == null) {
            return null;
        }
        if (value.getKind() != ScriptNode.Kind.NUMBER ||
            isWhole(value.getNumber()) == false) {
            fault("Output." + key, raw, "Output." + key +
                " expected an integer, found " + typeOf(value));
            return null;
        }
        return (long) value.getNumber();
    }

    private ScriptNode readList(ScriptNode raw, String key) {
        ScriptNode value = raw.get(key);
        if (value == null) {
            return null;
        }
        if (value.getKind() != ScriptNode.Kind.LIST) {
            fault("Output." + key, raw, "Output." + key +
                " expected a list, found " + typeOf(value));
            return null;
        }
        return value;
    }

    private void readValues(ScriptNode raw, OutputModel.Draft output) {
        ScriptNode values = raw.get("Values");
        if (values == null) {
            return;
        }
        if (values.getKind() != ScriptNode.Kind.LIST) {
            fault("Output.Values", raw,
                "Output.Values expected a list, found " + typeOf(values));
            return;
        }
        if (output.valueType != null &&
            output.valueType != ValueType.STRING &&
            output.valueType != ValueType.INT) {
            fault("Output.Values", raw, "Output.Values is only allowed " +
                "where ValueType is string or int, not " + output.valueType);
            return;
        }
        for (int index = 0; index < values.getItems().size(); index++) {
            ScriptNode entry = values.getItems().get(index);
            String path = "Output.Values[" + index + "]";
            if (entry.getKind() != ScriptNode.Kind.MAP) {
                fault(path, values, "a value expected a mapping of Name " +
                    "and Description, found " + typeOf(entry));
                continue;
            }
            for (String key : unknownKeys(
                entry, Arrays.asList("Name", "Description"))) {
                fault(path + "." + key, entry, "unknown key '" + key +
                    "' in a value. Expected Name and Description");
            }
            ScriptNode name = entry.get("Name");
            if (name == null || (name.getKind() != ScriptNode.Kind.STRING &&
                name.getKind() != ScriptNode.Kind.NUMBER)) {
                fault(path + ".Name", entry, "a value must have a Name");
                continue;
            }
            String text = null;
            ScriptNode description = entry.get("Description");
            if (description != null) {
                if (description.getKind() != ScriptNode.Kind.STRING) {
                    fault(path + ".Description", entry, "a value " +
                        "Description expected a string, found " +
                        typeOf(description));
                }
                text = description.getText();
            }
            output.values.add(
                new OutputModel.ValueEntry(Literal.of(name), text));
        }
        List<String> seen = new ArrayList<>();
        for (OutputModel.ValueEntry entry : output.values) {
            String key = entry.getName().toString();
            if (seen.contains(key)) {
                fault("Output.Values", values,
                    "the value '" + key + "' is listed more than once");
            }
            seen.add(key);
        }
        output.hasValues = true;
    }

    private List<String> readCheckNames() {
        ScriptNode raw = document.get("Checks");
        if (raw == null) {
            return Collections.emptyList();
        }
        if (raw.getKind() != ScriptNode.Kind.MAP) {
            fault("Checks", document, "Checks expected a mapping of names " +
                "to conditions, found " + typeOf(raw));
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (String name : raw.getKeys()) {
            if (IDENTIFIER.matcher(name).matches() == false) {
                fault("Checks." + name, raw, "check name '" + name +
                    "' does not match the pattern " + IDENTIFIER.pattern());
                continue;
            }
            names.add(name);
        }
        return names;
    }

    private List<DerivedScript.NamedCheck> readChecks(
        List<String> checkNames) {
        List<DerivedScript.NamedCheck> checks = new ArrayList<>();
        ScriptNode raw = document.get("Checks");
        if (raw == null || raw.getKind() != ScriptNode.Kind.MAP) {
            return checks;
        }
        for (String name : checkNames) {
            Condition condition = readCondition(
                raw.valueOf(name), "Checks." + name, raw, checkNames);
            checks.add(new DerivedScript.NamedCheck(name, condition));
        }
        return checks;
    }

    private List<DerivedScript.Rule> readRules(
        OutputModel.Draft output,
        List<String> checkNames) {
        List<DerivedScript.Rule> rules = new ArrayList<>();
        ScriptNode raw = document.get("Rules");
        if (raw == null) {
            fault("Rules", document, "required key 'Rules' is missing");
            return rules;
        }
        if (raw.getKind() != ScriptNode.Kind.LIST) {
            fault("Rules", document,
                "Rules expected a list, found " + typeOf(raw));
            return rules;
        }
        List<ScriptNode> items = raw.getItems();
        if (items.isEmpty()) {
            fault("Rules", raw, "Rules must hold at least one rule");
            return rules;
        }
        // Every script ends in an Else, so a script always chooses a value
        // once its source properties have been read.
        if (items.get(items.size() - 1).get("Else") == null) {
            fault("Rules", raw, "the last rule must be an Else, which is " +
                "what a script falls back to when no earlier rule matched");
        }
        for (int index = 0; index < items.size(); index++) {
            ScriptNode entry = items.get(index);
            String path = "Rules[" + index + "]";
            if (entry.getKind() != ScriptNode.Kind.MAP) {
                fault(path, raw, "a rule expected a mapping, found " +
                    typeOf(entry));
                continue;
            }
            for (String key : unknownKeys(
                entry, Arrays.asList("When", "Then", "Else"))) {
                fault(path + "." + key, entry, "unknown key '" + key +
                    "' in a rule. Expected When and Then, or Else");
            }
            ScriptNode when = entry.get("When");
            ScriptNode then = entry.get("Then");
            ScriptNode otherwise = entry.get("Else");
            boolean isLast = index == items.size() - 1;
            if (otherwise != null && when != null) {
                fault(path, entry, "a rule has both When and Else. A rule " +
                    "is either When with Then, or Else on its own");
                continue;
            }
            if (otherwise != null && isLast == false) {
                fault(path, entry, "Else is only allowed on the last rule");
                continue;
            }
            if (otherwise == null && when == null) {
                fault(path, entry,
                    "a rule needs a When, or an Else on the last rule");
                continue;
            }
            if (when != null && then == null) {
                fault(path, entry, "a rule with When needs a Then");
                continue;
            }
            Literal value = otherwise != null ?
                readRuleValue(output, otherwise, path + ".Else", entry) :
                readRuleValue(output, then, path + ".Then", entry);
            Condition condition = when == null ? null :
                readCondition(when, path + ".When", entry, checkNames);
            rules.add(new DerivedScript.Rule(condition, value));
        }
        return rules;
    }

    /**
     * Read a Then or an Else, which is a literal of Output.ValueType and
     * one of Output.Values where that list is given.
     */
    private Literal readRuleValue(
        OutputModel.Draft output,
        ScriptNode raw,
        String path,
        ScriptNode at) {
        if (raw.getKind() == ScriptNode.Kind.NULL) {
            fault(path, at, "a rule value is a null literal, which format 1 " +
                "does not allow");
            return Literal.NONE;
        }
        if (raw.isCollection()) {
            fault(path, at, "a rule value is a literal of the output value " +
                "type, found " + typeOf(raw));
            return Literal.NONE;
        }
        if (output.valueType != null &&
            literalMatches(inferType(raw), output.valueType) == false) {
            fault(path, at, "expected a " + output.valueType +
                " to match Output.ValueType, found " + typeOf(raw));
            return Literal.NONE;
        }
        Literal value = Literal.of(raw);
        if (output.hasValues && output.listsValue(value.toString()) == false) {
            List<String> names = new ArrayList<>();
            for (OutputModel.ValueEntry entry : output.values) {
                names.add(entry.getName().toString());
            }
            fault(path, at, "'" + value + "' is not one of the values " +
                "listed under Output.Values (" + String.join(", ", names) +
                ")");
            return Literal.NONE;
        }
        return value;
    }

    private Condition readCondition(
        ScriptNode raw,
        String path,
        ScriptNode parent,
        List<String> checkNames) {
        if (raw == null || raw.getKind() != ScriptNode.Kind.MAP) {
            fault(path, parent,
                "a condition expected a mapping, found " + typeOf(raw));
            return Condition.faulted();
        }
        if (raw.getKeys().isEmpty()) {
            fault(path, raw, "a condition is empty");
            return Condition.faulted();
        }
        if (raw.get("Property") != null) {
            return readComparison(raw, path);
        }
        if (raw.get("Check") != null) {
            return readCheckReference(raw, path, checkNames);
        }
        if (raw.get("Passed") != null || raw.get("Failed") != null) {
            return readAggregate(raw, path, checkNames);
        }
        if (raw.get("All") != null || raw.get("Any") != null) {
            String which = raw.get("All") != null ? "All" : "Any";
            if (raw.getKeys().size() != 1) {
                fault(path, raw, which + " must be the only key of its " +
                    "condition, found " + String.join(", ", raw.getKeys()));
            }
            ScriptNode items = raw.get(which);
            if (items.getKind() != ScriptNode.Kind.LIST) {
                fault(path + "." + which, raw, which + " expected a list of " +
                    "conditions, found " + typeOf(items));
                return Condition.faulted();
            }
            if (items.getItems().isEmpty()) {
                fault(path + "." + which, raw,
                    which + " must list at least one condition");
                return Condition.faulted();
            }
            Condition[] members = new Condition[items.getItems().size()];
            for (int index = 0; index < members.length; index++) {
                members[index] = readCondition(
                    items.getItems().get(index),
                    path + "." + which + "[" + index + "]",
                    items,
                    checkNames);
            }
            return Condition.compound(
                which.equals("All") ?
                    Condition.Kind.ALL : Condition.Kind.ANY,
                members);
        }
        if (raw.get("Not") != null) {
            if (raw.getKeys().size() != 1) {
                fault(path, raw, "Not must be the only key of its " +
                    "condition, found " + String.join(", ", raw.getKeys()));
            }
            Condition item = readCondition(
                raw.get("Not"), path + ".Not", raw, checkNames);
            return Condition.compound(
                Condition.Kind.NOT, new Condition[]{item});
        }
        fault(path, raw, "a condition must be a comparison, a Check " +
            "reference, an aggregate, All, Any or Not. Found the keys " +
            String.join(", ", raw.getKeys()));
        return Condition.faulted();
    }

    private Condition readComparison(ScriptNode raw, String path) {
        String propertyKey = null;
        List<String> operatorKeys = new ArrayList<>();
        for (String key : raw.getKeys()) {
            if (propertyKey == null && key.equalsIgnoreCase("Property")) {
                propertyKey = key;
                continue;
            }
            operatorKeys.add(key);
        }
        ScriptNode property = raw.valueOf(propertyKey);
        if (property.getKind() != ScriptNode.Kind.STRING ||
            SOURCE_PATTERN.matcher(property.getText()).matches() == false) {
            fault(path + ".Property", raw, "'" + displayOf(property) +
                "' is not a source property. Write it as " +
                "elementKey.PropertyName, for example device.IsCrawler");
            return Condition.faulted();
        }
        if (operatorKeys.isEmpty()) {
            fault(path, raw, "a comparison on '" + property.getText() +
                "' has no operator. Expected exactly one of " +
                Operator.names());
            return Condition.faulted();
        }
        List<String> known = new ArrayList<>();
        for (String key : operatorKeys) {
            if (Operator.fromText(key) != null) {
                known.add(key);
            } else {
                fault(path, raw, "unknown operator '" + key +
                    "', expected one of " + Operator.names());
            }
        }
        if (known.isEmpty()) {
            return Condition.faulted();
        }
        if (operatorKeys.size() > 1) {
            fault(path, raw, "a condition takes exactly one operator, found " +
                String.join(", ", operatorKeys));
            return Condition.faulted();
        }
        Operator operator = Operator.fromText(known.get(0));
        ScriptNode operand = raw.valueOf(known.get(0));
        if (operand.getKind() == ScriptNode.Kind.NULL) {
            fault(path + "." + operator, raw, "a null literal is not " +
                "allowed. Give the value to compare against");
            return Condition.faulted();
        }

        ValueType type;
        if (operator.takesList()) {
            if (operand.getKind() != ScriptNode.Kind.LIST) {
                fault(path + "." + operator, raw, operator + " expects a " +
                    "list of values, found " + typeOf(operand));
                return Condition.faulted();
            }
            if (operand.getItems().isEmpty()) {
                fault(path + "." + operator, raw,
                    operator + " expects a non empty list");
                return Condition.faulted();
            }
            // A member with no type, such as a mapping, joins as an empty
            // string, as JavaScript joins a null.
            List<String> memberTypes = new ArrayList<>();
            boolean numeric = true;
            boolean allSame = true;
            for (ScriptNode member : operand.getItems()) {
                if (member.getKind() == ScriptNode.Kind.NULL) {
                    fault(path + "." + operator, raw,
                        "a null literal is not allowed in a list");
                    return Condition.faulted();
                }
                ValueType memberType = inferType(member);
                String text = memberType == null ? "" : memberType.toString();
                memberTypes.add(text);
                if (memberType != ValueType.INT &&
                    memberType != ValueType.DOUBLE) {
                    numeric = false;
                }
                if (text.equals(memberTypes.get(0)) == false) {
                    allSame = false;
                }
            }
            if (numeric == false && allSame == false) {
                fault(path + "." + operator, raw, "every member of a list " +
                    "must be of the same type, found " +
                    String.join(", ", memberTypes));
                return Condition.faulted();
            }
            if (numeric) {
                type = memberTypes.contains(ValueType.DOUBLE.toString()) ?
                    ValueType.DOUBLE : ValueType.INT;
            } else {
                type = inferType(operand.getItems().get(0));
            }
        } else {
            type = inferType(operand);
            if (type == null) {
                fault(path + "." + operator, raw, "the literal " +
                    jsonOf(operand) + " has no type format 1 knows");
                return Condition.faulted();
            }
        }

        if (type == null || operator.getTypes().contains(type) == false) {
            List<String> allowed = new ArrayList<>();
            for (ValueType t : operator.getTypes()) {
                allowed.add(t.toString());
            }
            fault(path, raw, "operator '" + operator + "' is not allowed on " +
                "type " + (type == null ? "null" : type.toString()) +
                ". It is allowed on " + String.join(", ", allowed));
            return Condition.faulted();
        }

        int slot = useProperty(property.getText(), type, path, raw);
        return Condition.comparison(
            slot, property.getText(), operator, operand, type);
    }

    private Condition readCheckReference(
        ScriptNode raw,
        String path,
        List<String> checkNames) {
        if (raw.getKeys().size() != 1) {
            fault(path, raw, "Check must be the only key of its condition, " +
                "found " + String.join(", ", raw.getKeys()));
        }
        ScriptNode name = raw.get("Check");
        if (name.getKind() != ScriptNode.Kind.STRING) {
            fault(path + ".Check", raw,
                "Check expected the name of a check, found " + typeOf(name));
            return Condition.faulted();
        }
        int index = checkNames.indexOf(name.getText());
        if (index < 0) {
            fault(path + ".Check", raw, "check '" + name.getText() +
                "' is not defined. The checks are " + listOrNone(checkNames));
            return Condition.faulted();
        }
        return Condition.checkReference(name.getText(), index);
    }

    private Condition readAggregate(
        ScriptNode raw,
        String path,
        List<String> checkNames) {
        List<String> aggregateKeys = new ArrayList<>();
        for (String key : raw.getKeys()) {
            if (canonicalName(AGGREGATE_NAMES, key) != null) {
                aggregateKeys.add(key);
            }
        }
        if (aggregateKeys.size() > 1) {
            fault(path, raw, "an aggregate condition takes one of " +
                String.join(", ", AGGREGATE_NAMES) + ", found " +
                String.join(", ", aggregateKeys));
            return Condition.faulted();
        }
        String aggregateKey = aggregateKeys.get(0);
        String aggregate = canonicalName(AGGREGATE_NAMES, aggregateKey);
        List<String> groupNames = new ArrayList<>();
        int[] group = readGroup(raw.valueOf(aggregateKey),
            path + "." + aggregate, raw, checkNames, groupNames);

        List<String> operatorKeys = new ArrayList<>();
        for (String key : raw.getKeys()) {
            if (key.equals(aggregateKey) == false) {
                operatorKeys.add(key);
            }
        }
        if (operatorKeys.isEmpty()) {
            fault(path, raw, "an aggregate condition has no operator. " +
                "Expected exactly one of " +
                String.join(", ", COUNT_OPERATORS));
            return Condition.faulted();
        }
        if (operatorKeys.size() > 1) {
            fault(path, raw, "a condition takes exactly one operator, found " +
                String.join(", ", operatorKeys));
            return Condition.faulted();
        }
        Operator operator = Operator.fromText(operatorKeys.get(0));
        if (operator == null) {
            fault(path, raw, "unknown operator '" + operatorKeys.get(0) +
                "', expected one of " + String.join(", ", COUNT_OPERATORS));
            return Condition.faulted();
        }
        // In and NotIn are refused as well, because a count is compared
        // with one whole number, never with a list.
        if (COUNT_OPERATORS.contains(operator.toString()) == false) {
            fault(path, raw, "operator '" + operator + "' is not allowed " +
                "on a count, which is an int");
            return Condition.faulted();
        }
        // A count is compared with a whole number and with nothing else.
        ScriptNode operand = raw.valueOf(operatorKeys.get(0));
        if (operand.getKind() != ScriptNode.Kind.NUMBER ||
            isWhole(operand.getNumber()) == false) {
            fault(path + "." + operator, raw, "an aggregate is compared " +
                "with a whole number, found " + typeOf(operand));
            return Condition.faulted();
        }
        return Condition.aggregate(
            aggregate.equals("Passed"),
            group,
            group == null ? null : groupNames,
            operator,
            operand);
    }

    /**
     * Read the word Checks, meaning every check, which gives a null group,
     * or a list of check names, which gives their indexes.
     */
    private int[] readGroup(
        ScriptNode raw,
        String path,
        ScriptNode at,
        List<String> checkNames,
        List<String> groupNames) {
        if (raw.getKind() == ScriptNode.Kind.STRING) {
            if (raw.getText().equalsIgnoreCase("Checks")) {
                return null;
            }
            fault(path, at, "a group is the word Checks, meaning every " +
                "check, or a list of check names. Found '" + raw.getText() +
                "'");
            return new int[0];
        }
        if (raw.getKind() == ScriptNode.Kind.LIST) {
            List<Integer> indexes = new ArrayList<>();
            for (ScriptNode name : raw.getItems()) {
                if (name.getKind() != ScriptNode.Kind.STRING) {
                    fault(path, at, "a group lists check names, found " +
                        typeOf(name));
                    continue;
                }
                int index = checkNames.indexOf(name.getText());
                if (index < 0) {
                    fault(path, at, "check '" + name.getText() + "' is not " +
                        "defined. The checks are " + listOrNone(checkNames));
                    continue;
                }
                indexes.add(index);
                groupNames.add(name.getText());
            }
            int[] group = new int[indexes.size()];
            for (int i = 0; i < group.length; i++) {
                group[i] = indexes.get(i);
            }
            return group;
        }
        fault(path, at, "a group is the word Checks or a list of check " +
            "names, found " + typeOf(raw));
        return new int[0];
    }

    /**
     * Record that a condition names a source property with the type its
     * literal infers, and give the slot the property is read from.
     */
    private int useProperty(
        String name,
        ValueType type,
        String path,
        ScriptNode at) {
        String key = name.toLowerCase(Locale.ROOT);
        PropertyUse use = uses.get(key);
        if (use == null) {
            use = new PropertyUse(properties.size(), type, path);
            uses.put(key, use);
            properties.add(new SourceProperty(name, type));
            return use.slot;
        }
        if (use.type != type) {
            fault(path, at, "'" + name + "' is inferred as " + type +
                " here but was already inferred as " + use.type + " at " +
                use.typePath + ". Every use of a property must infer the " +
                "same type");
        }
        return use.slot;
    }

    /**
     * The type a literal infers. A number infers its type from its value
     * rather than the way it is written, so 8, 8.0 and 8e0 all infer int,
     * and a whole number outside the range of int infers double.
     * @return the type, or null for a literal with no type
     */
    static ValueType inferType(ScriptNode node) {
        switch (node.getKind()) {
            case BOOL:
                return ValueType.BOOL;
            case NUMBER:
                if (isWhole(node.getNumber()) == false) {
                    return ValueType.DOUBLE;
                }
                return node.getNumber() >= ValueType.INT_MIN &&
                    node.getNumber() <= ValueType.INT_MAX ?
                    ValueType.INT : ValueType.DOUBLE;
            case STRING:
                return ValueType.STRING;
            default:
                return null;
        }
    }

    /**
     * Whether a literal of one type may stand for a value of another. A
     * whole number reads as a double.
     */
    private static boolean literalMatches(
        ValueType literalType,
        ValueType outputType) {
        return literalType == outputType ||
            (literalType == ValueType.INT && outputType == ValueType.DOUBLE);
    }

    static boolean isWhole(double value) {
        return Double.isInfinite(value) == false &&
            Double.isNaN(value) == false &&
            value == Math.floor(value);
    }

    /**
     * Describe a value in a message.
     */
    static String typeOf(ScriptNode node) {
        if (node == null) {
            return "nothing";
        }
        switch (node.getKind()) {
            case NULL:
                return "a null literal";
            case LIST:
                return "a list";
            case BOOL:
                return "a boolean";
            case NUMBER:
                return isWhole(node.getNumber()) ? "an integer" : "a number";
            case STRING:
                return "a string (" + jsonOf(node) + ")";
            default:
                return "a mapping";
        }
    }

    /**
     * A value as JavaScript's template strings print it.
     */
    static String displayOf(ScriptNode node) {
        switch (node.getKind()) {
            case NULL:
                return "null";
            case BOOL:
            case NUMBER:
                return Literal.of(node).toString();
            case STRING:
                return node.getText();
            case LIST:
                List<String> parts = new ArrayList<>();
                for (ScriptNode item : node.getItems()) {
                    parts.add(displayOf(item));
                }
                return String.join(",", parts);
            default:
                return "[object Object]";
        }
    }

    /**
     * A value as compact JSON, as JSON.stringify gives it.
     */
    static String jsonOf(ScriptNode node) {
        StringBuilder text = new StringBuilder();
        appendJson(text, node);
        return text.toString();
    }

    private static void appendJson(StringBuilder text, ScriptNode node) {
        switch (node.getKind()) {
            case NULL:
                text.append("null");
                break;
            case BOOL:
                text.append(node.getBool() ? "true" : "false");
                break;
            case NUMBER:
                text.append(Double.isInfinite(node.getNumber()) ||
                    Double.isNaN(node.getNumber()) ?
                    "null" : JsText.number(node.getNumber()));
                break;
            case STRING:
                text.append(JsText.quote(node.getText()));
                break;
            case LIST:
                text.append('[');
                for (int i = 0; i < node.getItems().size(); i++) {
                    if (i > 0) {
                        text.append(',');
                    }
                    appendJson(text, node.getItems().get(i));
                }
                text.append(']');
                break;
            default:
                text.append('{');
                for (int i = 0; i < node.getKeys().size(); i++) {
                    if (i > 0) {
                        text.append(',');
                    }
                    text.append(JsText.quote(node.getKeys().get(i)))
                        .append(':');
                    appendJson(text, node.getValues().get(i));
                }
                text.append('}');
                break;
        }
    }

    private static List<String> unknownKeys(
        ScriptNode node,
        List<String> allowed) {
        List<String> unknown = new ArrayList<>();
        for (String key : node.getKeys()) {
            if (canonicalName(allowed, key) == null) {
                unknown.add(key);
            }
        }
        return unknown;
    }

    /**
     * The name in names that matches the key without regard to case.
     * @return the name, or null where none matches
     */
    private static String canonicalName(List<String> names, String key) {
        for (String name : names) {
            if (name.equalsIgnoreCase(key)) {
                return name;
            }
        }
        return null;
    }

    private static String listOrNone(List<String> names) {
        return names.isEmpty() ? "(none)" : String.join(", ", names);
    }
}
