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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/**
 * Checks that no console example adds a Usage Sharing Element to its
 * Pipeline, and that each says beside the setting that turns it off that
 * usage sharing should be on in production. The builders that add the
 * element unless told not to are the 51Degrees Pipeline builder and the
 * on-premise IP Intelligence Pipeline builder.
 */
public class UsageSharingCoverageTest {

    /**
     * Each builder that adds a Usage Sharing Element unless it is turned
     * off.
     */
    private static final Pattern SHARING_BUILDER = Pattern.compile(
        "new FiftyOnePipelineBuilder\\(|\\.useOnPremise\\(");

    private static final Pattern SHARING_OFF = Pattern.compile(
        "\\.setShareUsage\\(false\\)");

    /**
     * The words the comment before each setting that turns usage sharing
     * off must hold.
     */
    private static final String PRODUCTION_COMMENT =
        "In production, leave usage sharing on";

    /**
     * How many lines before the setting the comment may start.
     */
    private static final int COMMENT_LINES = 6;

    @Test
    public void everyExample_TurnsUsageSharingOff() throws IOException {
        List<String> examples = new ArrayList<>();
        List<String> sharing = new ArrayList<>();
        for (Path file : exampleSources()) {
            String text = code(read(file));
            examples.add(file.getFileName().toString());
            int builders = count(SHARING_BUILDER, text);
            int off = count(SHARING_OFF, text);
            if (off < builders) {
                sharing.add(file.getFileName() + " (" + builders +
                    " builders, " + off + " turned off)");
            }
        }
        assertTrue("No examples were found", examples.isEmpty() == false);
        assertTrue("These console examples share usage: " + sharing,
            sharing.isEmpty());
    }

    @Test
    public void everySettingOff_SaysToShareInProduction() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Path file : exampleSources()) {
            String[] lines = read(file).split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (SHARING_OFF.matcher(lines[i]).find()) {
                    StringBuilder before = new StringBuilder();
                    for (int j = Math.max(0, i - COMMENT_LINES); j < i; j++) {
                        before.append(lines[j]).append(' ');
                    }
                    String comment = before.toString()
                        .replaceAll("\\s*(//|\\*)\\s*", " ")
                        .replaceAll("\\s+", " ");
                    if (comment.contains(PRODUCTION_COMMENT) == false) {
                        missing.add(file.getFileName() + ":" + (i + 1));
                    }
                }
            }
        }
        assertTrue("These settings that turn usage sharing off do not say " +
            "to leave it on in production: " + missing, missing.isEmpty());
    }

    /**
     * The source of every console example, being each file with a main
     * method.
     */
    private static List<Path> exampleSources() throws IOException {
        Path sources = Paths.get(System.getProperty("basedir", "."),
            "src", "main", "java");
        assertTrue(sources + " should hold the examples",
            Files.isDirectory(sources));
        List<Path> examples = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(sources)) {
            for (Path file : walk
                .filter(f -> f.toString().endsWith(".java"))
                .collect(Collectors.toList())) {
                if (read(file).contains("static void main(")) {
                    examples.add(file);
                }
            }
        }
        return examples;
    }

    /**
     * The lines of a source that are code, leaving out the comment lines,
     * which can show a builder in an example of its use.
     */
    private static String code(String text) {
        StringBuilder code = new StringBuilder();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//") == false &&
                trimmed.startsWith("*") == false &&
                trimmed.startsWith("/*") == false) {
                code.append(line).append('\n');
            }
        }
        return code.toString();
    }

    private static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static int count(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
