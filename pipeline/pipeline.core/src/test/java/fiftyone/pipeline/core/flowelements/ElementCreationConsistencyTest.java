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

package fiftyone.pipeline.core.flowelements;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/**
 * Every Flow Element in the repository is made the same way, by a builder
 * class (pipeline-specification/usage-examples.md, "Creating Flow Elements
 * SHOULD always be done using a consistent mechanism. In the case of C#, we
 * use a separate builder class.").
 *
 * The test reads the main source of every module below the repository
 * root. A concrete class that extends one of the Flow Element base classes
 * is a Flow Element, unless its name ends in "Base", which marks a base
 * class that released code may still make directly. Each Flow Element must
 * be made by a class whose name ends in "Builder", and no other main code
 * may make one with {@code new}.
 */
public class ElementCreationConsistencyTest {

    /**
     * The base classes a concrete Flow Element extends.
     */
    private static final Pattern ELEMENT_BASE = Pattern.compile(
        "\\bextends\\s+(FlowElementBase|AspectEngineBase|" +
        "OnPremiseAspectEngineBase|FiftyOneOnPremiseAspectEngineBase|" +
        "CloudAspectEngineBase|PropertyKeyedCloudEngineBase|" +
        "ShareUsageBase|TranslationEngineBase)\\b");

    private static final Pattern CLASS = Pattern.compile(
        "^public\\s+((?:abstract\\s+|final\\s+)*)class\\s+(\\w+)([^{]*)\\{",
        Pattern.MULTILINE);

    private static Path root() {
        String root = System.getProperty("project.root");
        return Paths.get(root == null ? "../.." : root).toAbsolutePath()
            .normalize();
    }

    /**
     * The text of each main source file, by its path.
     */
    private static Map<Path, String> mainSources() throws IOException {
        Map<Path, String> sources = new HashMap<>();
        try (Stream<Path> files = Files.walk(root())) {
            for (Path file : files
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> p.toString().replace(File.separatorChar, '/')
                    .contains("/src/main/java/"))
                .filter(p -> p.toString().replace(File.separatorChar, '/')
                    .contains("/target/") == false)
                .collect(Collectors.toList())) {
                sources.put(file, new String(Files.readAllBytes(file),
                    StandardCharsets.UTF_8));
            }
        }
        return sources;
    }

    private static boolean isBuilder(Path file) {
        String name = file.getFileName().toString();
        return name.endsWith("Builder.java") ||
            name.endsWith("BuilderBase.java");
    }

    @Test
    public void everyElementIsMadeByItsBuilderAlone() throws IOException {
        Map<Path, String> sources = mainSources();
        List<String> elements = new ArrayList<>();
        for (String source : sources.values()) {
            Matcher matcher = CLASS.matcher(source);
            if (matcher.find() && matcher.group(1).contains("abstract") == false &&
                matcher.group(2).endsWith("Base") == false &&
                ELEMENT_BASE.matcher(matcher.group(3)).find()) {
                elements.add(matcher.group(2));
            }
        }
        assertTrue("No Flow Elements found below " + root(),
            elements.size() > 10);

        List<String> problems = new ArrayList<>();
        for (String element : elements) {
            Pattern made = Pattern.compile("\\bnew\\s+" + element + "\\s*[(<]");
            boolean byBuilder = false;
            for (Map.Entry<Path, String> source : sources.entrySet()) {
                if (made.matcher(source.getValue()).find()) {
                    if (isBuilder(source.getKey())) {
                        byBuilder = true;
                    } else {
                        problems.add(element + " is made outside a builder, in " +
                            root().relativize(source.getKey()));
                    }
                }
            }
            if (byBuilder == false) {
                problems.add(element + " is made by no builder");
            }
        }
        Collections.sort(problems);
        assertTrue(String.join("\n", problems), problems.isEmpty());
    }
}
