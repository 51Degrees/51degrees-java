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

package fiftyone.devicedetection;

import fiftyone.devicedetection.cloud.flowelements.DeviceDetectionCloudEngine;
import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.devicedetection.shared.DeviceDataBase;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/**
 * Examples and tests SHOULD NOT be included in any published packages
 * (device-detection-specification/packaging-and-structure.md line 16 at
 * 706de1f0). Lists what each published Device Detection package's main jar
 * holds, being the classes folder in a reactor build or the jar otherwise,
 * and looks for test helpers, tests and examples.
 */
public class PublishedPackagesHoldNoTestsTests {

    /**
     * A main class of each published Device Detection package.
     */
    private static final Class<?>[] PACKAGES = {
        DeviceDataBase.class,
        DeviceDetectionHashEngine.class,
        DeviceDetectionCloudEngine.class,
        DeviceDetectionPipelineBuilder.class
    };

    private static List<String> entriesOf(Class<?> type)
        throws IOException, URISyntaxException {
        File location = new File(type.getProtectionDomain()
            .getCodeSource().getLocation().toURI());
        List<String> entries = new ArrayList<>();
        if (location.isDirectory()) {
            Path root = location.toPath();
            try (Stream<Path> paths = Files.walk(root)) {
                entries.addAll(paths
                    .map(p -> root.relativize(p).toString().replace('\\', '/'))
                    .collect(Collectors.toList()));
            }
        } else {
            try (JarFile jar = new JarFile(location)) {
                Enumeration<JarEntry> e = jar.entries();
                while (e.hasMoreElements()) {
                    entries.add(e.nextElement().getName());
                }
            }
        }
        return entries;
    }

    /**
     * Test helpers that stay in the published jar, deprecated, because a
     * patch release removes no public name, and the folder a jar lists for
     * them. A later release removes them.
     */
    private static final List<String> KEPT_DEPRECATED = Arrays.asList(
        "fiftyone/devicedetection/shared/testhelpers/",
        "fiftyone/devicedetection/shared/testhelpers/FileUtils.class",
        "fiftyone/devicedetection/shared/testhelpers/KeyUtils.class");

    private static boolean isTestOrExample(String entry) {
        if (KEPT_DEPRECATED.contains(entry)) {
            return false;
        }
        String lower = entry.toLowerCase();
        return lower.contains("testhelpers/") ||
            lower.contains("/examples/") ||
            lower.endsWith("test.class") ||
            lower.endsWith("tests.class");
    }

    @Test
    public void PublishedPackages_HoldNoTestsOrExamples() throws Exception {
        List<String> found = new ArrayList<>();
        for (Class<?> type : PACKAGES) {
            for (String entry : entriesOf(type)) {
                if (isTestOrExample(entry)) {
                    found.add(type.getSimpleName() + "'s package: " + entry);
                }
            }
        }
        assertTrue("Published packages hold tests or examples: " + found,
            found.isEmpty());
    }
}
