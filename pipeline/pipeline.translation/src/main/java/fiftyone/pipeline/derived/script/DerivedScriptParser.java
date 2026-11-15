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

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.schema.CoreSchema;

import java.util.List;

/**
 * Parses the text of a script, YAML or JSON, into the model. JSON is read
 * as the JSON subset of YAML 1.2, and YAML is read with the YAML 1.2 core
 * schema, so {@code yes} and {@code no} stay strings, as the format's
 * reference tools read them.
 */
public final class DerivedScriptParser {

    private DerivedScriptParser() {
    }

    /**
     * Parses and validates one script, adding every fault found to the
     * list.
     * @param text the text of the script
     * @param fileName the file name without its extension, which the
     *                 script's name must equal, or null for a script given
     *                 as text in code or built in
     * @param source the built-in name, the file path, or the word code
     * @param faults where to add the faults found
     * @return the compiled script, or null if there were faults
     */
    public static DerivedScript parse(
        String text,
        String fileName,
        String source,
        List<DerivedScriptFault> faults) {
        Object document;
        try {
            LoadSettings settings = LoadSettings.builder()
                .setSchema(new CoreSchema())
                .setAllowDuplicateKeys(false)
                .build();
            document = new Load(settings).loadFromString(text);
        } catch (RuntimeException e) {
            faults.add(new DerivedScriptFault(
                fileName == null ? source : fileName, source, "",
                "The script cannot be parsed as YAML or JSON. " +
                e.getMessage()));
            return null;
        }
        return new DerivedScriptValidator(fileName, source, faults)
            .validate(document);
    }
}
