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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Output block of a script, with the field names 51Degrees uses for the
 * metadata of every property. It is the whole definition of the derived
 * property, so a JSON builder, the cloud or a documentation generator can
 * read every field. A field the script does not give is null.
 */
public final class DerivedOutputMetaData {

    private final OutputModel output;
    private final List<Object> vendorIds;
    private final List<Object> dependencies;
    private final List<DerivedValueMetaData> values;

    DerivedOutputMetaData(OutputModel output) {
        this.output = output;
        this.vendorIds = output.getVendorIds() == null ?
            null : plainList(output.getVendorIds());
        this.dependencies = output.getDependencies() == null ?
            Collections.<Object>unmodifiableList(
                new ArrayList<Object>(output.getComputed())) :
            plainList(output.getDependencies());
        if (output.getValues() == null) {
            this.values = null;
        } else {
            List<DerivedValueMetaData> entries = new ArrayList<>();
            for (OutputModel.ValueEntry entry : output.getValues()) {
                entries.add(new DerivedValueMetaData(
                    entry.getName().toString(), entry.getDescription()));
            }
            this.values = Collections.unmodifiableList(entries);
        }
    }

    /**
     * @return the property name published under the derived Element Data
     * key
     */
    public String getName() {
        return output.getName();
    }

    /**
     * @return what the property asserts
     */
    public String getDescription() {
        return output.getDescription();
    }

    /**
     * @return string, bool, int or double
     */
    public String getValueType() {
        return output.getValueType().toString();
    }

    /**
     * @return the Java type of the value, being {@link String},
     * {@link Boolean}, {@link Integer} or {@link Double}
     */
    public Class<?> getValueClass() {
        return output.getValueType().getValueClass();
    }

    public String getStoredValueType() {
        return output.getStoredValueType();
    }

    /**
     * @return the string form of the default recorded for the property.
     * This is metadata only, and nothing reads it while a request is
     * processed.
     */
    public String getDefaultValue() {
        return output.getDefaultValue();
    }

    /**
     * @return false, as format 1 has no list outputs
     */
    public boolean isList() {
        return output.isList();
    }

    public Boolean getIsMandatory() {
        return output.getIsMandatory();
    }

    public Boolean getIsObsolete() {
        return output.getIsObsolete();
    }

    public String getCategory() {
        return output.getCategory();
    }

    public Boolean getIsPopular() {
        return output.getIsPopular();
    }

    public Boolean getExportValues() {
        return output.getExportValues();
    }

    public String getUrl() {
        return output.getUrl();
    }

    public Long getDisplayOrder() {
        return output.getDisplayOrder();
    }

    public Long getPropertyId() {
        return output.getPropertyId();
    }

    /**
     * @return the list as written, with numbers as {@link Double}
     */
    public List<Object> getVendorIds() {
        return vendorIds;
    }

    /**
     * @return the list the script gives, or every source property its
     * checks and rules name, in elementKey.PropertyName form, where it gives
     * none
     */
    public List<Object> getDependencies() {
        return dependencies;
    }

    /**
     * @return the values the property can have, or null where the script
     * gives no list
     */
    public List<DerivedValueMetaData> getValues() {
        return values;
    }

    /**
     * A list from the script as plain Java values, being null, a
     * {@link Boolean}, a {@link Double}, a {@link String}, a list or a map.
     */
    private static List<Object> plainList(ScriptNode node) {
        List<Object> items = new ArrayList<>();
        for (ScriptNode item : node.getItems()) {
            items.add(plain(item));
        }
        return Collections.unmodifiableList(items);
    }

    private static Object plain(ScriptNode node) {
        switch (node.getKind()) {
            case BOOL:
                return node.getBool();
            case NUMBER:
                return node.getNumber();
            case STRING:
                return node.getText();
            case LIST:
                return plainList(node);
            case MAP:
                Map<String, Object> values = new LinkedHashMap<>();
                for (int i = 0; i < node.getKeys().size(); i++) {
                    values.put(
                        node.getKeys().get(i),
                        plain(node.getValues().get(i)));
                }
                return Collections.unmodifiableMap(values);
            default:
                return null;
        }
    }
}
