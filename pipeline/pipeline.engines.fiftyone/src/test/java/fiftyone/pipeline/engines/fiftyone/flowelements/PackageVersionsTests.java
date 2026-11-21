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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The packages in use are found from the Maven descriptor in each jar, as
 * the usage sharing record's Package elements need.
 */
public class PackageVersionsTests {

    /**
     * A class to put in a jar, so that a test can load it from there.
     */
    public static class InJar {
    }

    @TempDir
    Path folder;

    @Test
    public void jar_FollowsTheGroupsDependencies() throws Exception {
        Map<String, byte[]> a = withClass();
        descriptor(a, "a", "1.2.3",
            "<dependencyManagement><dependencies>" +
            dependency("com.51degrees", "e", null) +
            "</dependencies></dependencyManagement><dependencies>" +
            dependency("${project.groupId}", "b", null) +
            dependency("com.51degrees", "c", "test") +
            dependency("org.example", "d", null) +
            "</dependencies>");
        Map<String, byte[]> b = new LinkedHashMap<>();
        descriptor(b, "b", "4.5.6", "");
        Map<String, byte[]> c = new LinkedHashMap<>();
        descriptor(c, "c", "7.8.9", "");
        Map<String, byte[]> e = new LinkedHashMap<>();
        descriptor(e, "e", "1.0.0", "");

        SortedMap<String, String> expected = new TreeMap<>();
        expected.put("com.51degrees:a", "1.2.3");
        expected.put("com.51degrees:b", "4.5.6");
        assertEquals(expected, packagesOfClassIn(
            write("a.jar", a), write("b.jar", b), write("c.jar", c),
            write("e.jar", e)));
    }

    @Test
    public void jarHoldingSeveralPackages_GivesEachOfThem() throws Exception {
        Map<String, byte[]> fat = withClass();
        descriptor(fat, "a", "1.0.0", "");
        descriptor(fat, "b", "2.0.0", "");

        SortedMap<String, String> expected = new TreeMap<>();
        expected.put("com.51degrees:a", "1.0.0");
        expected.put("com.51degrees:b", "2.0.0");
        assertEquals(expected, packagesOfClassIn(write("fat.jar", fat)));
    }

    @Test
    public void classFromAFolder_GivesNoPackage() {
        assertEquals(Collections.emptyMap(), PackageVersions.of(
            Collections.<Class<?>>singletonList(PackageVersionsTests.class),
            PackageVersions.GROUP_ID));
    }

    /**
     * A jar from Maven Central on the test's class path, so the reading is
     * checked against a descriptor Maven wrote.
     */
    @Test
    public void publishedJar_GivesItsVersion() {
        SortedMap<String, String> packages = PackageVersions.of(
            Collections.<Class<?>>singletonList(org.slf4j.Logger.class),
            "org.slf4j");
        String version = packages.get("org.slf4j:slf4j-api");
        assertTrue(version != null && version.matches("\\d+\\.\\d+\\.\\d+.*"),
            "slf4j-api should have a version, found " + packages);
    }

    private SortedMap<String, String> packagesOfClassIn(Path... jars)
        throws Exception {
        URL[] urls = new URL[jars.length];
        for (int i = 0; i < jars.length; i++) {
            urls[i] = jars[i].toUri().toURL();
        }
        try (URLClassLoader loader = new URLClassLoader(urls, null)) {
            Class<?> type = loader.loadClass(InJar.class.getName());
            assertEquals(loader, type.getClassLoader());
            return PackageVersions.of(
                Collections.<Class<?>>singletonList(type),
                PackageVersions.GROUP_ID);
        }
    }

    private static String dependency(String group, String artifact,
                                     String scope) {
        return "<dependency><groupId>" + group +
            "</groupId><artifactId>" + artifact + "</artifactId>" +
            (scope == null ? "" : "<scope>" + scope + "</scope>") +
            "</dependency>";
    }

    private static void descriptor(Map<String, byte[]> entries,
                                   String artifact,
                                   String version,
                                   String body) {
        String path = "META-INF/maven/com.51degrees/" + artifact + "/";
        entries.put(path + "pom.properties", ("groupId=com.51degrees\n" +
            "artifactId=" + artifact + "\nversion=" + version + "\n")
            .getBytes(StandardCharsets.UTF_8));
        entries.put(path + "pom.xml", ("<?xml version=\"1.0\"?><project>" +
            "<groupId>com.51degrees</groupId><artifactId>" + artifact +
            "</artifactId><version>" + version + "</version>" + body +
            "</project>").getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, byte[]> withClass() throws IOException {
        String path = InJar.class.getName().replace('.', '/') + ".class";
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (InputStream input = PackageVersionsTests.class.getClassLoader()
            .getResourceAsStream(path)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                bytes.write(buffer, 0, read);
            }
            entries.put(path, bytes.toByteArray());
        }
        return entries;
    }

    private Path write(String name, Map<String, byte[]> entries)
        throws IOException {
        Path file = folder.resolve(name);
        try (OutputStream output = Files.newOutputStream(file);
             JarOutputStream jar = new JarOutputStream(output)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                jar.putNextEntry(new JarEntry(entry.getKey()));
                jar.write(entry.getValue());
                jar.closeEntry();
            }
        }
        return file;
    }
}
