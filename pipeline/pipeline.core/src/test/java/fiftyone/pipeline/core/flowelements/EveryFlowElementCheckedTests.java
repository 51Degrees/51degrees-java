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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Every Flow Element in the repository is processed by a check that then
 * writes to its Element Data and expects the error. Each module that holds
 * Flow Elements has a test class named {@value #CHECK}, which names each of
 * the module's Flow Elements. This test finds every class in the
 * repository's main source that is not abstract and extends
 * {@link FlowElementBase}, directly or through other classes, and fails for
 * one that its module's check does not name.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Specification</a>
 */
public class EveryFlowElementCheckedTests {

    static final String CHECK = "WritesAfterProcessingTests.java";

    private static final List<String> TOP_FOLDERS = Arrays.asList(
        "pipeline", "device-detection", "ip-intelligence", "location",
        "examples");

    private static final Set<String> SKIPPED_FOLDERS = new HashSet<>(
        Arrays.asList("target", "cxx", "node_modules", ".git"));

    private static final Pattern CLASS = Pattern.compile(
        "(?m)^[\\t ]*((?:public|protected|private|abstract|final|static)" +
        "\\s+)*class\\s+(\\w+)");

    private static final Pattern EXTENDS = Pattern.compile(
        "\\bextends\\s+([\\w.]+)");

    private static class Found {
        final String name;
        final String parent;
        final boolean isAbstract;
        final Path module;

        Found(String name, String parent, boolean isAbstract, Path module) {
            this.name = name;
            this.parent = parent;
            this.isAbstract = isAbstract;
            this.module = module;
        }
    }

    /**
     * The repository root, found from the module's folder, or null when the
     * module is built outside the repository.
     */
    private static Path root() {
        Path dir = Paths.get("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("pipeline")
                    .resolve("pipeline.core")) &&
                Files.isDirectory(dir.resolve("device-detection"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        return null;
    }

    private static List<Path> javaFiles(Path root) throws IOException {
        final List<Path> files = new ArrayList<>();
        for (String top : TOP_FOLDERS) {
            Path folder = root.resolve(top);
            if (Files.isDirectory(folder) == false) {
                continue;
            }
            Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(
                    Path dir,
                    BasicFileAttributes attributes) {
                    return SKIPPED_FOLDERS.contains(
                        String.valueOf(dir.getFileName())) ?
                        FileVisitResult.SKIP_SUBTREE :
                        FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attributes) {
                    if (file.toString().endsWith(".java")) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return files;
    }

    /**
     * The module folder a source file belongs to, being the folder that
     * holds its src folder.
     */
    private static Path module(Path file) {
        for (Path dir = file.getParent(); dir != null; dir = dir.getParent()) {
            if (String.valueOf(dir.getFileName()).equals("src")) {
                return dir.getParent();
            }
        }
        return null;
    }

    private static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    /**
     * Drop type arguments, so that an extends inside them is not read as
     * the class's own.
     */
    private static String withoutTypeArguments(String text) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        for (char c : text.toCharArray()) {
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (depth == 0) {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static boolean isFlowElement(
        String name,
        Map<String, Found> classes) {
        Set<String> seen = new HashSet<>();
        String current = name;
        while (current != null && seen.add(current)) {
            if (current.equals("FlowElementBase")) {
                return true;
            }
            Found found = classes.get(current);
            current = found == null ? null : found.parent;
        }
        return false;
    }

    @Test
    public void everyFlowElement_HasItsModulesCheck() throws IOException {
        Path root = root();
        assumeTrue("Not built inside the repository", root != null);

        Map<String, Found> classes = new HashMap<>();
        List<Found> mainClasses = new ArrayList<>();
        Map<Path, String> checks = new HashMap<>();
        for (Path file : javaFiles(root)) {
            String path = file.toString().replace('\\', '/');
            String text = read(file);
            if (path.contains("/src/test/") &&
                file.getFileName().toString().equals(CHECK)) {
                checks.put(module(file), text);
            }
            if (path.contains("/src/main/java/") == false) {
                continue;
            }
            Matcher matcher = CLASS.matcher(text);
            while (matcher.find()) {
                int end = text.indexOf('{', matcher.end());
                if (end < 0) {
                    continue;
                }
                String declaration = withoutTypeArguments(
                    text.substring(matcher.end(), end));
                Matcher parent = EXTENDS.matcher(declaration);
                String parentName = null;
                if (parent.find()) {
                    String[] parts = parent.group(1).split("\\.");
                    parentName = parts[parts.length - 1];
                }
                Found found = new Found(
                    matcher.group(2),
                    parentName,
                    matcher.group(0).contains("abstract"),
                    module(file));
                classes.put(found.name, found);
                mainClasses.add(found);
            }
        }

        List<String> missing = new ArrayList<>();
        int flowElements = 0;
        for (Found found : mainClasses) {
            if (found.isAbstract ||
                isFlowElement(found.name, classes) == false) {
                continue;
            }
            flowElements++;
            String check = checks.get(found.module);
            if (check == null ||
                Pattern.compile("\\b" + found.name + "\\b")
                    .matcher(check).find() == false) {
                missing.add(found.name + " in " +
                    root.relativize(found.module).toString()
                        .replace('\\', '/'));
            }
        }
        assertTrue("No Flow Elements were found", flowElements > 0);
        assertTrue("These Flow Elements have no check that processes them, " +
            "then writes to their Element Data and expects the error. Add " +
            "each to a " + CHECK + " in its module: " + missing,
            missing.isEmpty());
    }
}
