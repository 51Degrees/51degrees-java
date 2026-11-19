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

import java.util.Locale;

/**
 * A property another element produces, which a script reads as the type it
 * infers from the literals compared with it.
 */
final class SourceProperty {

    private final String key;
    private final String name;
    private final ValueType type;
    private final String elementKey;
    private final String writtenKey;
    private final String propertyName;

    /**
     * @param name the name as the script first writes it, being
     *             elementKey.PropertyName
     * @param type the type inferred for it
     */
    SourceProperty(String name, ValueType type) {
        int dot = name.indexOf('.');
        this.key = name.toLowerCase(Locale.ROOT);
        this.name = name;
        this.type = type;
        this.writtenKey = name.substring(0, dot);
        this.elementKey = writtenKey.toLowerCase(Locale.ROOT);
        this.propertyName = name.substring(dot + 1);
    }

    /**
     * @return the name in lower case, which identifies the property
     */
    String getKey() {
        return key;
    }

    /**
     * @return the name as the script first writes it
     */
    String getName() {
        return name;
    }

    ValueType getType() {
        return type;
    }

    /**
     * @return the Element Data key, in lower case
     */
    String getElementKey() {
        return elementKey;
    }

    /**
     * @return the Element Data key as the script writes it
     */
    String getWrittenKey() {
        return writtenKey;
    }

    /**
     * @return the property half of the name, as written
     */
    String getPropertyName() {
        return propertyName;
    }
}
