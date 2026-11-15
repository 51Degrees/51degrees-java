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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Checks a parsed script against format 1 and compiles it. Every fault is
 * collected rather than stopping at the first, each with the script name,
 * the source and a path in the document such as
 * {@code Rules[3].When.All[1]}.
 */
final class DerivedScriptValidator {

    private static final Pattern IDENTIFIER =
        Pattern.compile("^[A-Za-z][A-Za-z0-9]*$");

    private static final Pattern VERSION = Pattern.compile(
        "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
        "(-[0-9A-Za-z-]+(\\.[0-9A-Za-z-]+)*)?(\\+[0-9A-Za-z-]+(\\.[0-9A-Za-z-]+)*)?$");

    private static final List<String> TOP_KEYS = Arrays.asList(
        "Format", "Name", "Version", "Deprecated", "DeprecationNote",
        "Output", "Checks", "Rules");

    private static final List<String> OUTPUT_KEYS = Arrays.asList(
        "Name", "Description", "ValueType", "StoredValueType", "DefaultValue",
        "IsList", "IsMandatory", "IsObsolete", "Category", "IsPopular",
        "ExportValues", "Url", "DisplayOrder", "PropertyId", "VendorIds",
        "Dependencies", "Values");

    private static final List<String> RULE_KEYS =
        Arrays.asList("When", "Then", "Else");

    private static final List<String> AGGREGATE_OPERATORS =
        Arrays.asList("Eq", "Ne", "Gt", "Ge", "Lt", "Le");

    private final String fileName;
    private final String source;
    private final List<DerivedScriptFault> faults;
    private String name;

    /** The source properties, keyed by their qualified name in lower case. */
    private final Map<String, Integer> sourceIndex = new LinkedHashMap<>();
    private final List<String[]> sourceNames = new ArrayList<>();
    private final List<ValueType> sourceTypes = new ArrayList<>();
    private final List<String> sourcePaths = new ArrayList<>();

    /** The comparisons, compiled once every type is known. */
    private final List<Runnable> pending = new ArrayList<>();

    private Map<String, Integer> checkIndex = new LinkedHashMap<>();

    DerivedScriptValidator(
        String fileName,
        String source,
        List<DerivedScriptFault> faults) {
        this.fileName = fileName;
        this.source = source;
        this.faults = faults;
        this.name = fileName == null ? source : fileName;
    }

    private void fault(String path, String message) {
        faults.add(new DerivedScriptFault(name, source, path, message));
    }

    /**
     * The mapping with its keys matched to the names format 1 knows,
     * without regard to letter case, or null with a fault for a key it
     * does not know or a key written twice.
     */
    private Map<String, Object> keys(Object value, String path,
                                     List<String> known, String what) {
        if (value instanceof Map == false) {
            fault(path, what + " must be a mapping.");
            return null;
        }
        Map<String, Object> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        boolean good = true;
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            String key = String.valueOf(entry.getKey());
            String canonical = null;
            for (String name : known) {
                if (name.equalsIgnoreCase(key)) {
                    canonical = name;
                }
            }
            if (canonical == null) {
                fault(join(path, key), "'" + key + "' is not a key format 1 " +
                    "knows in " + what + ". The keys are " +
                    String.join(", ", known) + ".");
                good = false;
            } else if (result.containsKey(canonical)) {
                fault(join(path, key), "'" + canonical + "' is written " +
                    "more than once, as keys are matched without regard to " +
                    "letter case.");
                good = false;
            } else {
                result.put(canonical, entry.getValue());
            }
        }
        // The keys format 1 knows are still checked, so that every fault is
        // reported at once.
        return result;
    }

    private static String join(String path, String key) {
        return path == null || path.isEmpty() ? key : path + "." + key;
    }

    private String string(Map<String, Object> map, String key, String path,
                          boolean required) {
        Object value = map.get(key);
        if (value == null) {
            if (required) {
                fault(join(path, key), "'" + key + "' is required.");
            }
            return null;
        }
        if (value instanceof String == false) {
            fault(join(path, key), "'" + key + "' must be a string.");
            return null;
        }
        return (String) value;
    }

    private Boolean bool(Map<String, Object> map, String key, String path) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean == false) {
            fault(join(path, key), "'" + key + "' must be true or false.");
            return null;
        }
        return (Boolean) value;
    }

    DerivedScript validate(Object document) {
        int faultsBefore = faults.size();
        Map<String, Object> top = keys(document, "", TOP_KEYS, "the script");
        if (top == null) {
            return null;
        }
        // Name first, so that every later fault names the script.
        String scriptName = string(top, "Name", "", true);
        if (scriptName != null) {
            if (IDENTIFIER.matcher(scriptName).matches() == false) {
                fault("Name", "'" + scriptName + "' does not match " +
                    IDENTIFIER.pattern() + ".");
            } else {
                name = scriptName;
                if (fileName != null && fileName.equals(scriptName) == false) {
                    fault("Name", "The name '" + scriptName + "' must equal " +
                        "the name the script is given, which for a file is " +
                        "the file name without its extension, '" + fileName +
                        "'.");
                }
            }
        }
        Object format = top.get("Format");
        if (format == null) {
            fault("Format", "'Format' is required.");
        } else if (format instanceof Number == false ||
            ((Number) format).doubleValue() != 1) {
            fault("Format", "'Format' must be 1, the only format this " +
                "implementation reads.");
        }
        String version = string(top, "Version", "", true);
        if (version != null && VERSION.matcher(version).matches() == false) {
            fault("Version", "'" + version + "' is not a semantic version.");
        }
        Boolean deprecated = bool(top, "Deprecated", "");
        String note = string(top, "DeprecationNote", "", false);
        if (Boolean.TRUE.equals(deprecated) && note == null) {
            fault("DeprecationNote", "A deprecated script needs a " +
                "'DeprecationNote' saying what to use instead.");
        }
        if (Boolean.TRUE.equals(deprecated) == false && note != null) {
            fault("DeprecationNote", "'DeprecationNote' is only allowed " +
                "where 'Deprecated' is true.");
        }

        // Output
        Map<String, Object> output = null;
        String outputName = null;
        ValueType valueType = null;
        Set<String> values = null;
        String category = "";
        if (top.get("Output") == null) {
            fault("Output", "'Output' is required.");
        } else {
            output = keys(top.get("Output"), "Output", OUTPUT_KEYS,
                "'Output'");
        }
        if (output != null) {
            outputName = string(output, "Name", "Output", true);
            if (outputName != null &&
                IDENTIFIER.matcher(outputName).matches() == false) {
                fault("Output.Name", "'" + outputName + "' does not match " +
                    IDENTIFIER.pattern() + ".");
            }
            String description = string(output, "Description", "Output", true);
            if (description != null && description.trim().isEmpty()) {
                fault("Output.Description", "'Description' must not be empty.");
            }
            String typeText = string(output, "ValueType", "Output", true);
            if (typeText != null) {
                valueType = ValueType.parse(typeText);
                if (valueType == null) {
                    fault("Output.ValueType", "'" + typeText + "' is not a " +
                        "value type in format 1, which has string, bool, int " +
                        "and double.");
                }
            }
            Object isList = output.get("IsList");
            if (isList == null) {
                fault("Output.IsList", "'IsList' is required.");
            } else if (Boolean.FALSE.equals(isList) == false) {
                fault("Output.IsList", "'IsList' must be false in format 1.");
            }
            for (String key : Arrays.asList("IsMandatory", "IsObsolete",
                "IsPopular", "ExportValues")) {
                bool(output, key, "Output");
            }
            String categoryText = string(output, "Category", "Output", false);
            if (categoryText != null) {
                category = categoryText;
            }
            string(output, "Url", "Output", false);
            string(output, "StoredValueType", "Output", false);
            values = values(output.get("Values"), valueType);
            Object defaultValue = output.get("DefaultValue");
            if (defaultValue != null && valueType != null) {
                if (literal(defaultValue, valueType, "Output.DefaultValue") &&
                    values != null &&
                    values.contains(String.valueOf(defaultValue)) == false) {
                    fault("Output.DefaultValue", "'" + defaultValue + "' is " +
                        "not one of 'Values'.");
                }
            }
        }

        // Checks, first by name so that a reference can be resolved in any
        // order, then each condition.
        List<String> checkNames = new ArrayList<>();
        List<Object> checkBodies = new ArrayList<>();
        Object checksValue = top.get("Checks");
        if (checksValue != null) {
            if (checksValue instanceof Map == false) {
                fault("Checks", "'Checks' must be a mapping of check names " +
                    "to conditions.");
            } else {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) checksValue).entrySet()) {
                    String checkName = String.valueOf(entry.getKey());
                    checkIndex.put(checkName, checkNames.size());
                    checkNames.add(checkName);
                    checkBodies.add(entry.getValue());
                }
            }
        }
        Condition[] checks = new Condition[checkNames.size()];
        List<Set<Integer>> checkUses = new ArrayList<>();
        for (int i = 0; i < checkNames.size(); i++) {
            Set<Integer> uses = new HashSet<>();
            checks[i] = condition(checkBodies.get(i),
                "Checks." + checkNames.get(i), uses);
            checkUses.add(uses);
        }
        int[] checkOrder = order(checkNames, checkUses);

        // Rules
        List<DerivedScript.Rule> rules = new ArrayList<>();
        Object rulesValue = top.get("Rules");
        if (rulesValue == null) {
            fault("Rules", "'Rules' is required.");
        } else if (rulesValue instanceof List == false ||
            ((List<?>) rulesValue).isEmpty()) {
            fault("Rules", "'Rules' must be a list of at least one rule.");
        } else {
            List<?> list = (List<?>) rulesValue;
            for (int i = 0; i < list.size(); i++) {
                String path = "Rules[" + i + "]";
                Map<String, Object> rule = keys(list.get(i), path, RULE_KEYS,
                    "a rule");
                if (rule == null) {
                    continue;
                }
                boolean last = i == list.size() - 1;
                if (rule.containsKey("Else")) {
                    if (rule.containsKey("When") || rule.containsKey("Then")) {
                        fault(path, "A rule cannot carry 'Else' with 'When' " +
                            "or 'Then'.");
                    }
                    if (last == false) {
                        fault(path, "'Else' must be the last rule.");
                    }
                    Object value = ruleValue(rule.get("Else"), valueType,
                        values, join(path, "Else"));
                    rules.add(new DerivedScript.Rule(null, value));
                } else {
                    if (last) {
                        fault(path, "The last rule must be an 'Else'.");
                    }
                    if (rule.containsKey("When") == false) {
                        fault(path, "A rule needs 'When' and 'Then', or 'Else'.");
                    }
                    if (rule.containsKey("Then") == false) {
                        fault(path, "A 'When' needs a 'Then'.");
                    }
                    Condition when = condition(rule.get("When"),
                        join(path, "When"), new HashSet<>());
                    Object value = ruleValue(rule.get("Then"), valueType,
                        values, join(path, "Then"));
                    rules.add(new DerivedScript.Rule(when, value));
                }
            }
        }

        if (faults.size() > faultsBefore) {
            return null;
        }
        for (Runnable compile : pending) {
            compile.run();
        }
        List<DerivedScript.SourceProperty> sources = new ArrayList<>();
        for (int i = 0; i < sourceNames.size(); i++) {
            sources.add(new DerivedScript.SourceProperty(
                sourceNames.get(i)[0], sourceNames.get(i)[1],
                sourceTypes.get(i)));
        }
        Map<String, Object> canonical = canonicalOutput(output, sources);
        return new DerivedScript(name, version, source,
            Boolean.TRUE.equals(deprecated), note, outputName, valueType,
            category, canonical, sources, checks, checkOrder,
            rules.toArray(new DerivedScript.Rule[0]));
    }

    /**
     * The output block in the canonical order, with the dependencies
     * computed where the script does not give them.
     */
    private static Map<String, Object> canonicalOutput(
        Map<String, Object> output,
        List<DerivedScript.SourceProperty> sources) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : OUTPUT_KEYS) {
            if (output.containsKey(key)) {
                result.put(key, output.get(key));
            } else if (key.equals("Dependencies")) {
                List<String> dependencies = new ArrayList<>();
                for (DerivedScript.SourceProperty source : sources) {
                    dependencies.add(source.getName());
                }
                result.put(key, dependencies);
            }
        }
        return result;
    }

    /**
     * The names of the output's values, or null where there is no list.
     */
    private Set<String> values(Object value, ValueType valueType) {
        if (value == null) {
            return null;
        }
        if (valueType != null && valueType != ValueType.STRING &&
            valueType != ValueType.INT) {
            fault("Output.Values", "'Values' is only allowed where " +
                "'ValueType' is string or int.");
            return null;
        }
        if (value instanceof List == false) {
            fault("Output.Values", "'Values' must be a list.");
            return null;
        }
        Set<String> names = new HashSet<>();
        List<?> list = (List<?>) value;
        for (int i = 0; i < list.size(); i++) {
            String path = "Output.Values[" + i + "]";
            Map<String, Object> entry = keys(list.get(i), path,
                Arrays.asList("Name", "Description"), "a value");
            if (entry == null) {
                continue;
            }
            Object entryName = entry.get("Name");
            if (entryName instanceof String == false &&
                (entryName instanceof Number == false ||
                    ValueType.isWholeInt((Number) entryName) == false)) {
                fault(join(path, "Name"), "'Name' is required and must be " +
                    "a string or a whole number.");
            } else if (names.add(String.valueOf(entryName)) == false) {
                fault(join(path, "Name"), "'" + entryName + "' is in " +
                    "'Values' more than once.");
            }
            string(entry, "Description", path, false);
        }
        return names;
    }

    /**
     * True if the literal is of the output's type. A whole number is
     * accepted for a double.
     */
    private boolean literal(Object value, ValueType valueType, String path) {
        if (value == null) {
            fault(path, "A null value is not allowed.");
            return false;
        }
        boolean good;
        switch (valueType) {
            case BOOL:
                good = value instanceof Boolean;
                break;
            case INT:
                good = value instanceof Number &&
                    ValueType.isWholeInt((Number) value);
                break;
            case DOUBLE:
                good = value instanceof Number;
                break;
            default:
                good = value instanceof String;
                break;
        }
        if (good == false) {
            fault(path, "'" + value + "' is not a " + valueType.getText() +
                ", the output's value type.");
        }
        return good;
    }

    private Object ruleValue(Object value, ValueType valueType,
                             Set<String> values, String path) {
        if (valueType == null) {
            return null;
        }
        if (literal(value, valueType, path) == false) {
            return null;
        }
        if (values != null && values.contains(String.valueOf(value)) == false) {
            fault(path, "'" + value + "' is not one of 'Values'.");
        }
        return valueType.fromLiteral(value);
    }

    /**
     * Parses a condition, adding to the set the checks it refers to.
     */
    private Condition condition(Object value, String path, Set<Integer> uses) {
        if (value instanceof Map == false || ((Map<?, ?>) value).isEmpty()) {
            fault(path, "A condition must be a mapping in one of the forms " +
                "of format 1.");
            return null;
        }
        Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (map.containsKey(key)) {
                fault(join(path, key), "'" + key + "' is written more than " +
                    "once, as keys are matched without regard to letter case.");
                return null;
            }
            map.put(key, entry.getValue());
        }
        if (map.containsKey("Property")) {
            return comparison(map, path);
        }
        if (map.containsKey("Check")) {
            if (map.size() != 1) {
                fault(path, "'Check' must be the only key of its mapping.");
                return null;
            }
            Object checkName = map.get("Check");
            Integer check = checkIndex.get(String.valueOf(checkName));
            if (check == null) {
                fault(join(path, "Check"), "'" + checkName + "' is not a " +
                    "check defined in this script. Check names are matched " +
                    "exactly as written.");
                return null;
            }
            uses.add(check);
            return new Condition.CheckReference(check);
        }
        if (map.containsKey("Passed") || map.containsKey("Failed")) {
            return aggregate(map, path, uses);
        }
        for (String group : Arrays.asList("All", "Any")) {
            if (map.containsKey(group)) {
                if (map.size() != 1) {
                    fault(path, "'" + group + "' must be the only key of " +
                        "its mapping.");
                    return null;
                }
                Object members = map.get(group);
                if (members instanceof List == false ||
                    ((List<?>) members).isEmpty()) {
                    fault(join(path, group), "'" + group + "' takes a list " +
                        "of at least one condition.");
                    return null;
                }
                List<?> list = (List<?>) members;
                Condition[] compiled = new Condition[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    compiled[i] = condition(list.get(i),
                        join(path, group) + "[" + i + "]", uses);
                }
                return group.equals("All") ?
                    new Condition.All(compiled) : new Condition.Any(compiled);
            }
        }
        if (map.containsKey("Not")) {
            if (map.size() != 1) {
                fault(path, "'Not' must be the only key of its mapping.");
                return null;
            }
            Condition member = condition(map.get("Not"), join(path, "Not"), uses);
            return new Condition.Not(member);
        }
        fault(path, "A condition must be a comparison, a 'Check', an " +
            "aggregate ('Passed' or 'Failed'), 'All', 'Any' or 'Not'.");
        return null;
    }

    private Condition comparison(Map<String, Object> map, String path) {
        Object property = map.get("Property");
        String operatorKey = null;
        for (String key : map.keySet()) {
            if (key.equalsIgnoreCase("Property") == false) {
                if (Condition.Operator.parse(key) == null) {
                    fault(join(path, key), "'" + key + "' is not an operator " +
                        "format 1 knows.");
                    return null;
                }
                if (operatorKey != null) {
                    fault(path, "A comparison takes exactly one operator, " +
                        "and this one has '" + operatorKey + "' and '" + key +
                        "'.");
                    return null;
                }
                operatorKey = key;
            }
        }
        if (operatorKey == null) {
            fault(path, "A comparison needs an operator.");
            return null;
        }
        String[] parts = property instanceof String ?
            ((String) property).split("\\.", -1) : new String[0];
        if (parts.length != 2 || IDENTIFIER.matcher(parts[0]).matches() == false ||
            IDENTIFIER.matcher(parts[1]).matches() == false) {
            fault(join(path, "Property"), "'" + property + "' must name a " +
                "source property as elementDataKey.PropertyName.");
            return null;
        }
        Condition.Operator operator = Condition.Operator.parse(operatorKey);
        Object operand = map.get(operatorKey);
        String operandPath = join(path, operatorKey);
        ValueType type;
        List<Object> members = null;
        if (operator == Condition.Operator.IN ||
            operator == Condition.Operator.NOTIN) {
            if (operand instanceof List == false || ((List<?>) operand).isEmpty()) {
                fault(operandPath, "'" + operatorKey + "' takes a non empty " +
                    "list of literals.");
                return null;
            }
            members = new ArrayList<>((List<?>) operand);
            type = null;
            for (int i = 0; i < members.size(); i++) {
                ValueType member = ValueType.infer(members.get(i));
                if (member == null) {
                    fault(operandPath + "[" + i + "]", members.get(i) == null ?
                        "A null literal is not allowed." :
                        "A member must be a literal.");
                    return null;
                }
                type = type == null ? member : merge(type, member);
                if (type == null) {
                    fault(operandPath, "The members of the list do not " +
                        "share one type.");
                    return null;
                }
            }
        } else {
            type = ValueType.infer(operand);
            if (type == null) {
                fault(operandPath, operand == null ?
                    "A null literal is not allowed." :
                    "The operand must be a literal.");
                return null;
            }
        }
        if (operator.allowedOn(type) == false) {
            fault(operandPath, "'" + operator.text + "' is not allowed on " +
                type.getText() + ".");
            return null;
        }
        String qualified = parts[0] + "." + parts[1];
        String lower = qualified.toLowerCase();
        Integer index = sourceIndex.get(lower);
        if (index == null) {
            index = sourceNames.size();
            sourceIndex.put(lower, index);
            sourceNames.add(parts);
            sourceTypes.add(type);
            sourcePaths.add(join(path, "Property"));
        } else if (sourceTypes.get(index) != type) {
            fault(join(path, "Property"), "'" + qualified + "' is compared " +
                "with a " + type.getText() + " here and with a " +
                sourceTypes.get(index).getText() + " at " +
                sourcePaths.get(index) + ". Every use of a property must " +
                "infer the same type.");
            return null;
        }
        // Compiled once every use has been checked, with the literal read as
        // the property's type.
        final int slot = index;
        final List<Object> finalMembers = members;
        final Object literal = operand;
        final Condition[] holder = new Condition[1];
        Condition deferred = (values, checks) -> holder[0].test(values, checks);
        pending.add(() -> {
            ValueType propertyType = sourceTypes.get(slot);
            Set<Object> set = null;
            if (finalMembers != null) {
                set = new HashSet<>();
                for (Object member : finalMembers) {
                    set.add(propertyType.fromLiteral(member));
                }
            }
            holder[0] = new Condition.Comparison(slot, operator,
                finalMembers == null ? propertyType.fromLiteral(literal) : null,
                set);
        });
        return deferred;
    }

    private static ValueType merge(ValueType a, ValueType b) {
        if (a == b) {
            return a;
        }
        if ((a == ValueType.INT || a == ValueType.DOUBLE) &&
            (b == ValueType.INT || b == ValueType.DOUBLE)) {
            return ValueType.DOUBLE;
        }
        return null;
    }

    private Condition aggregate(Map<String, Object> map, String path,
                                Set<Integer> uses) {
        boolean passed = map.containsKey("Passed");
        String countKey = passed ? "Passed" : "Failed";
        if (map.containsKey("Passed") && map.containsKey("Failed")) {
            fault(path, "An aggregate counts 'Passed' or 'Failed', not both.");
            return null;
        }
        Object group = map.get(countKey);
        int[] members;
        if (group instanceof String && ((String) group).equalsIgnoreCase("Checks")) {
            members = new int[checkIndex.size()];
            for (int i = 0; i < members.length; i++) {
                members[i] = i;
            }
        } else if (group instanceof List && ((List<?>) group).isEmpty() == false) {
            List<?> list = (List<?>) group;
            members = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Integer check = checkIndex.get(String.valueOf(list.get(i)));
                if (check == null) {
                    fault(join(path, countKey) + "[" + i + "]", "'" +
                        list.get(i) + "' is not a check defined in this script.");
                    return null;
                }
                members[i] = check;
            }
        } else {
            fault(join(path, countKey), "The group must be the word 'Checks' " +
                "or a list of check names.");
            return null;
        }
        for (int member : members) {
            uses.add(member);
        }
        String operatorKey = null;
        for (String key : map.keySet()) {
            if (key.equalsIgnoreCase(countKey) == false) {
                if (operatorKey != null) {
                    fault(path, "An aggregate takes exactly one operator.");
                    return null;
                }
                operatorKey = key;
            }
        }
        Condition.Operator operator = operatorKey == null ? null :
            Condition.Operator.parse(operatorKey);
        if (operator == null || operator.isOrdering() == false) {
            fault(path, "An aggregate takes one of " +
                String.join(", ", AGGREGATE_OPERATORS) + ".");
            return null;
        }
        Object operand = map.get(operatorKey);
        if (operand instanceof Number == false ||
            ValueType.isWholeInt((Number) operand) == false) {
            fault(join(path, operatorKey), "An aggregate is compared with a " +
                "whole number.");
            return null;
        }
        return new Condition.Aggregate(passed, members, operator,
            ((Number) operand).intValue());
    }

    /**
     * The order to evaluate the checks in, so that a check another check
     * refers to is evaluated first. A check that refers to itself, directly
     * or through others, is a fault.
     */
    private int[] order(List<String> names, List<Set<Integer>> uses) {
        int[] state = new int[names.size()];
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            visit(i, names, uses, state, order);
        }
        int[] result = new int[order.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = order.get(i);
        }
        return result;
    }

    private void visit(int check, List<String> names, List<Set<Integer>> uses,
                       int[] state, List<Integer> order) {
        if (state[check] == 2) {
            return;
        }
        if (state[check] == 1) {
            fault("Checks." + names.get(check), "The check '" +
                names.get(check) + "' refers to itself.");
            state[check] = 2;
            return;
        }
        state[check] = 1;
        for (int used : uses.get(check)) {
            visit(used, names, uses, state, order);
        }
        state[check] = 2;
        order.add(check);
    }
}
