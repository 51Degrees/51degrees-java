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

package fiftyone.pipeline.derivedproperty;

import fiftyone.pipeline.derivedproperty.flowelements.DerivedPropertyElement;
import fiftyone.pipeline.derivedproperty.flowelements.DerivedPropertyElementBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The scripts this package ships are the scripts folder of the
 * derived-properties submodule, and {@link BuiltInScript} names every one
 * of them, so a script added to the submodule fails this test until it has
 * a constant.
 */
public class BuiltInScriptTests {

    /**
     * The scripts in the submodule, by name, with their text.
     */
    private static Map<String, String> submoduleScripts() throws IOException {
        Path folder = Paths.get("..", "derived-properties", "scripts");
        assertTrue(Files.isDirectory(folder), "The derived-properties " +
            "submodule is missing from " + folder.toAbsolutePath() +
            ". Run 'git submodule update --init pipeline/derived-properties'.");
        Map<String, String> scripts = new TreeMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
            folder, "*.{yaml,yml,json}")) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                scripts.put(
                    name.substring(0, name.lastIndexOf('.')),
                    new String(Files.readAllBytes(file),
                        StandardCharsets.UTF_8));
            }
        }
        return scripts;
    }

    @Test
    public void enumNamesEveryScriptInTheSubmodule() throws IOException {
        TreeSet<String> names = new TreeSet<>();
        for (BuiltInScript script : BuiltInScript.values()) {
            names.add(script.name());
        }
        assertEquals(submoduleScripts().keySet(), names,
            "BuiltInScript and the scripts folder of the derived-properties " +
                "submodule differ. Add a constant for each script, named " +
                "exactly as the script is.");
    }

    @Test
    public void packagedScriptsAreTheSubmodulesScripts() throws IOException {
        Map<String, String> scripts = submoduleScripts();
        for (BuiltInScript script : BuiltInScript.values()) {
            String text = script.readText();
            assertNotNull(text, script + " is not in the package.");
            assertEquals(scripts.get(script.name()), text,
                "The packaged " + script + " differs from the submodule's.");
            assertSame(script, BuiltInScript.fromName(script.name()));
        }
    }

    @Test
    public void everyScriptBuilds() {
        for (BuiltInScript script : BuiltInScript.values()) {
            DerivedPropertyElement element = new DerivedPropertyElementBuilder(
                LoggerFactory.getILoggerFactory())
                .addScript(script)
                .build();
            assertEquals(script.name(), element.getScripts().get(0).getName());
        }
    }
}
