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

package fiftyone.devicedetection.shared;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/**
 * The published Device Detection packages hold no tests or test helpers,
 * other than deprecated test helpers that earlier releases published, which
 * stay because a patch release removes no public name. Each module's main
 * sources are what its published jar holds, so they are read for a test
 * helper package or a test class.
 */
public class PublishedPackageTests {

    /**
     * The published Device Detection modules, which sit beside this one.
     */
    private static final String[] MODULES = {
        "device-detection.shared",
        "device-detection.cloud",
        "device-detection.hash.engine.on-premise",
        "device-detection"
    };

    @Test
    public void publishedModules_HoldNoTestCode() throws IOException {
        Path root = Paths.get(System.getProperty("basedir", "."))
            .toAbsolutePath()
            .getParent();
        List<String> found = new ArrayList<>();
        for (String module : MODULES) {
            Path sources = root.resolve(module)
                .resolve("src").resolve("main").resolve("java");
            assertTrue(sources + " should hold the module's sources",
                Files.isDirectory(sources));
            List<String> files;
            try (Stream<Path> walk = Files.walk(sources)) {
                files = walk
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(p -> sources.relativize(p).toString()
                        .replace('\\', '/'))
                    .collect(Collectors.toList());
            }
            for (String file : files) {
                if ((file.contains("/testhelpers/") &&
                    isDeprecated(file) == false) ||
                    file.matches(".*Tests?\\.java")) {
                    found.add(module + ": " + file);
                }
            }
        }
        assertTrue("These published sources are tests or test helpers: " +
            found, found.isEmpty());
    }

    /**
     * Whether the class a source file holds is deprecated, which is false
     * for a class this module cannot load.
     */
    private static boolean isDeprecated(String file) {
        String name = file.substring(0, file.length() - ".java".length())
            .replace('/', '.');
        try {
            return Class.forName(name).isAnnotationPresent(Deprecated.class);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
