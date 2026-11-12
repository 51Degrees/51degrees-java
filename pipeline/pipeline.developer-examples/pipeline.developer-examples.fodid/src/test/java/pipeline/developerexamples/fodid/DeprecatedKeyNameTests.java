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

package pipeline.developerexamples.fodid;

import fiftyone.pipeline.did.DidClient;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests which environment variables the example takes its resource key and
 * its licence key from. Each test starts {@link KeyProbe} in a Java process
 * of its own with the variables it needs, so the example reads a real
 * environment.
 */
public class DeprecatedKeyNameTests {

    private static final String RESOURCE_KEY_NAME = "_51DEGREES_RESOURCE_KEY";
    private static final String OLD_RESOURCE_KEY_NAME = "RESOURCE_KEY";
    private static final String LICENCE_KEY_NAME = "_51DEGREES_LICENSE_KEY";
    private static final String OLD_LICENCE_KEY_NAME = "LICENSE_KEY";

    private static final int PROBE_TIMEOUT_SECONDS = 120;

    @Test
    public void oldNamesAlone_GiveTheirKeys() throws Exception {
        List<String> output =
            probe(null, "old-name-key", null, "old-name-licence");

        assertEquals("old-name-key",
            value(output, KeyProbe.RESOURCE_PREFIX), output.toString());
        assertEquals("old-name-licence",
            value(output, KeyProbe.LICENCE_PREFIX), output.toString());
    }

    @Test
    public void oldNamesAlone_AreEachReportedAsDeprecatedOnce()
        throws Exception {
        List<String> output =
            probe(null, "old-name-key", null, "old-name-licence");

        assertEquals(1, deprecations(output, OLD_RESOURCE_KEY_NAME,
            RESOURCE_KEY_NAME), output.toString());
        assertEquals(1, deprecations(output, OLD_LICENCE_KEY_NAME,
            LICENCE_KEY_NAME), output.toString());
    }

    @Test
    public void bothNames_NewNamesWin() throws Exception {
        List<String> output = probe(
            "new-name-key", "old-name-key",
            "new-name-licence", "old-name-licence");

        assertEquals("new-name-key",
            value(output, KeyProbe.RESOURCE_PREFIX), output.toString());
        assertEquals("new-name-licence",
            value(output, KeyProbe.LICENCE_PREFIX), output.toString());
        assertEquals(0, deprecations(output, OLD_RESOURCE_KEY_NAME,
            RESOURCE_KEY_NAME), output.toString());
        assertEquals(0, deprecations(output, OLD_LICENCE_KEY_NAME,
            LICENCE_KEY_NAME), output.toString());
    }

    @Test
    public void noNames_GiveNoKeys() throws Exception {
        List<String> output = probe(null, null, null, null);

        assertEquals("null",
            value(output, KeyProbe.RESOURCE_PREFIX), output.toString());
        assertEquals("null",
            value(output, KeyProbe.LICENCE_PREFIX), output.toString());
    }

    /**
     * Runs the probe with each variable set to the value given, or absent
     * where the value is null, and returns the lines it printed.
     */
    private static List<String> probe(
        String resourceKey,
        String oldResourceKey,
        String licenceKey,
        String oldLicenceKey) throws Exception {
        String java = Paths.get(
            System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder builder =
            new ProcessBuilder(java, KeyProbe.class.getName());
        Map<String, String> environment = builder.environment();
        environment.put("CLASSPATH", classPath());
        set(environment, RESOURCE_KEY_NAME, resourceKey);
        set(environment, OLD_RESOURCE_KEY_NAME, oldResourceKey);
        set(environment, LICENCE_KEY_NAME, licenceKey);
        set(environment, OLD_LICENCE_KEY_NAME, oldLicenceKey);
        builder.redirectErrorStream(true);

        Process process = builder.start();
        List<String> output = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
        }
        assertTrue(
            process.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS),
            "The probe did not finish.");
        assertEquals(0, process.exitValue(), output.toString());
        return output;
    }

    private static void set(
        Map<String, String> environment, String name, String value) {
        if (value == null) {
            environment.remove(name);
        } else {
            environment.put(name, value);
        }
    }

    /**
     * The places the probe loads its classes from, being the classes of
     * the example, its test classes, the 51Did package and the JSON
     * library.
     */
    private static String classPath() throws Exception {
        return location(CreatorContextDemoServer.class) + File.pathSeparator
            + location(KeyProbe.class) + File.pathSeparator
            + location(DidClient.class) + File.pathSeparator
            + location(JSONObject.class);
    }

    private static String location(Class<?> type) throws Exception {
        return Paths.get(type.getProtectionDomain().getCodeSource()
            .getLocation().toURI()).toString();
    }

    /** The text after the prefix on the one line that starts with it. */
    private static String value(List<String> output, String prefix) {
        String found = null;
        for (String line : output) {
            if (line.startsWith(prefix)) {
                assertNull(found, output.toString());
                found = line.substring(prefix.length());
            }
        }
        return found;
    }

    /**
     * How many lines say the old name is deprecated and name the new one.
     */
    private static int deprecations(
        List<String> output, String oldName, String newName) {
        int count = 0;
        for (String line : output) {
            if (line.contains(" " + oldName + " is deprecated")
                && line.contains(newName)) {
                count++;
            }
        }
        return count;
    }
}
