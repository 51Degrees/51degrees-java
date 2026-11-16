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

package fiftyone.ipintelligence.examples.console;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/**
 * Checks that every console example with a main method writes the "Find out
 * more" section, including those that need a key or a paid data file and so
 * do not run in ExamplesWithoutSettingsTest.
 */
public class FindOutMoreCoverageTest {

    @Test
    public void everyExample_WritesFindOutMore() throws IOException {
        Path sources = Paths.get(System.getProperty("basedir", "."),
                "src", "main", "java");
        assertTrue(sources + " should hold the examples",
                Files.isDirectory(sources));
        List<Path> files;
        try (Stream<Path> walk = Files.walk(sources)) {
            files = walk.filter(f -> f.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        }
        List<String> examples = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (Path file : files) {
            String text = new String(Files.readAllBytes(file),
                    StandardCharsets.UTF_8);
            if (text.contains("static void main(")) {
                examples.add(file.getFileName().toString());
                if (text.contains("FindOutMore.write") == false) {
                    missing.add(file.getFileName().toString());
                }
            }
        }
        assertTrue("No examples were found in " + sources,
                examples.isEmpty() == false);
        assertTrue("These examples do not write the 'Find out more' " +
                "section: " + missing, missing.isEmpty());
    }
}
