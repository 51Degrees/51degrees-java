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

import fiftyone.pipeline.annotations.ElementBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a {@link StubSourceElement}, so that a configuration file can put
 * a source element in front of a Derived Property Element.
 */
@ElementBuilder
public class StubSourceElementBuilder {

    private String key = "";
    private final Map<String, Object> values = new LinkedHashMap<>();

    /**
     * @param key the Element Data key the element writes under
     * @return this builder
     */
    public StubSourceElementBuilder setKey(String key) {
        this.key = key;
        return this;
    }

    /**
     * @param values the properties and their string values, written as
     *               Name=Value and separated by commas
     * @return this builder
     */
    public StubSourceElementBuilder setValues(String values) {
        for (String pair : values.split(",")) {
            int equals = pair.indexOf('=');
            this.values.put(
                pair.substring(0, equals).trim(),
                pair.substring(equals + 1).trim());
        }
        return this;
    }

    /**
     * @return a new element
     */
    public StubSourceElement build() {
        return new StubSourceElement(key, values.keySet(), values);
    }
}
