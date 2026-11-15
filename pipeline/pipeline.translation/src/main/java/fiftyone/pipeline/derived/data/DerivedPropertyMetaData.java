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

package fiftyone.pipeline.derived.data;

import java.util.Map;

/**
 * The full property metadata block of one script, being its name, its
 * version and its output block, so that a JSON builder or a documentation
 * generator can read every field.
 */
public class DerivedPropertyMetaData {
    private final String name;
    private final String version;
    private final Map<String, Object> output;

    /**
     * Construct a new instance.
     * @param name the script name
     * @param version the script version
     * @param output the output block, in canonical order
     */
    public DerivedPropertyMetaData(
        String name,
        String version,
        Map<String, Object> output) {
        this.name = name;
        this.version = version;
        this.output = output;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, Object> getOutput() {
        return output;
    }
}
