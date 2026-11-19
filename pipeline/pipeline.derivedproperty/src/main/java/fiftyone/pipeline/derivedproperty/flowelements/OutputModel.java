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
import java.util.List;

/**
 * The Output block of a script, which is the whole definition of the
 * derived property. A field the script does not give is null.
 */
final class OutputModel {

    /**
     * One entry of Output.Values.
     */
    static final class ValueEntry {
        private final Literal name;
        private final String description;

        ValueEntry(Literal name, String description) {
            this.name = name;
            this.description = description;
        }

        Literal getName() {
            return name;
        }

        /**
         * @return the description, or null where the script gives none
         */
        String getDescription() {
            return description;
        }
    }

    /**
     * The Output block while the validator reads it.
     */
    static final class Draft {
        String name = "";
        String description = "";
        ValueType valueType;
        String storedValueType;
        String defaultValue;
        boolean isList;
        Boolean isMandatory;
        Boolean isObsolete;
        String category;
        Boolean isPopular;
        Boolean exportValues;
        String url;
        Long displayOrder;
        Long propertyId;
        ScriptNode vendorIds;
        ScriptNode dependencies;
        final List<ValueEntry> values = new ArrayList<>();
        boolean hasValues;

        /**
         * Whether the string form of a value is one of the names under
         * Output.Values.
         */
        boolean listsValue(String value) {
            for (ValueEntry entry : values) {
                if (entry.getName().toString().equals(value)) {
                    return true;
                }
            }
            return false;
        }
    }

    private final String name;
    private final String description;
    private final ValueType valueType;
    private final String storedValueType;
    private final String defaultValue;
    private final boolean isList;
    private final Boolean isMandatory;
    private final Boolean isObsolete;
    private final String category;
    private final Boolean isPopular;
    private final Boolean exportValues;
    private final String url;
    private final Long displayOrder;
    private final Long propertyId;
    private final ScriptNode vendorIds;
    private final ScriptNode dependencies;
    private final List<String> computed;
    private final List<ValueEntry> values;

    /**
     * @param draft the block as read
     * @param computed every source property the script names, used where
     *                 the script gives no Dependencies
     */
    OutputModel(Draft draft, List<String> computed) {
        this.name = draft.name;
        this.description = draft.description;
        this.valueType = draft.valueType;
        this.storedValueType = draft.storedValueType;
        this.defaultValue = draft.defaultValue;
        this.isList = draft.isList;
        this.isMandatory = draft.isMandatory;
        this.isObsolete = draft.isObsolete;
        this.category = draft.category;
        this.isPopular = draft.isPopular;
        this.exportValues = draft.exportValues;
        this.url = draft.url;
        this.displayOrder = draft.displayOrder;
        this.propertyId = draft.propertyId;
        this.vendorIds = draft.vendorIds;
        this.dependencies = draft.dependencies;
        this.computed = Collections.unmodifiableList(
            new ArrayList<>(computed));
        this.values = draft.hasValues ?
            Collections.unmodifiableList(new ArrayList<>(draft.values)) :
            null;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    ValueType getValueType() {
        return valueType;
    }

    String getStoredValueType() {
        return storedValueType;
    }

    String getDefaultValue() {
        return defaultValue;
    }

    boolean isList() {
        return isList;
    }

    Boolean getIsMandatory() {
        return isMandatory;
    }

    Boolean getIsObsolete() {
        return isObsolete;
    }

    String getCategory() {
        return category;
    }

    Boolean getIsPopular() {
        return isPopular;
    }

    Boolean getExportValues() {
        return exportValues;
    }

    String getUrl() {
        return url;
    }

    Long getDisplayOrder() {
        return displayOrder;
    }

    Long getPropertyId() {
        return propertyId;
    }

    /**
     * @return the VendorIds list as written, or null
     */
    ScriptNode getVendorIds() {
        return vendorIds;
    }

    /**
     * @return the Dependencies list as written, or null where the script
     * gives none and {@link #getComputed()} holds them
     */
    ScriptNode getDependencies() {
        return dependencies;
    }

    /**
     * @return every source property the checks and the rules name, in
     * elementKey.PropertyName form
     */
    List<String> getComputed() {
        return computed;
    }

    /**
     * @return the entries of Output.Values, or null where the script gives
     * no list
     */
    List<ValueEntry> getValues() {
        return values;
    }
}
