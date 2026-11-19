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

package fiftyone.pipeline.did;

import org.json.JSONObject;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests which environment variable {@link DidClient} takes its endpoint
 * from. Each test starts {@link EndpointProbe} in a Java process of its own
 * with the variables it needs, so the client reads a real environment.
 */
public class DidClientEnvironmentTests {

    private static final String NEW_NAME = "_51DEGREES_CLOUD_ENDPOINT";
    private static final String OLD_NAME = "FOD_CLOUD_API_URL";

    private static final String NEW_ENDPOINT = "https://new.example/api/v4/";
    private static final String OLD_ENDPOINT = "https://old.example/api/v4/";

    private static final int PROBE_TIMEOUT_SECONDS = 120;

    @Test
    public void oldNameAlone_GivesItsEndpoint() throws Exception {
        List<String> output = probe(null, OLD_ENDPOINT);

        assertEquals(
            Arrays.asList(OLD_ENDPOINT, OLD_ENDPOINT), endpoints(output));
    }

    @Test
    public void oldNameAlone_IsLoggedAsDeprecatedOnce() throws Exception {
        List<String> logged = logged(probe(null, OLD_ENDPOINT));

        assertEquals(logged.toString(), 1, logged.size());
        String record = logged.get(0);
        assertTrue(record, record.startsWith("WARNING "));
        assertTrue(record, record.contains(OLD_NAME));
        assertTrue(record, record.contains(NEW_NAME));
        assertTrue(record, record.contains("deprecated"));
    }

    @Test
    public void newNameBlank_OldNameIsRead() throws Exception {
        List<String> output = probe(" ", OLD_ENDPOINT);

        assertEquals(
            Arrays.asList(OLD_ENDPOINT, OLD_ENDPOINT), endpoints(output));
    }

    @Test
    public void bothNames_NewNameWins() throws Exception {
        List<String> output = probe(NEW_ENDPOINT, OLD_ENDPOINT);

        assertEquals(
            Arrays.asList(NEW_ENDPOINT, NEW_ENDPOINT), endpoints(output));
        assertEquals(output.toString(), 0, logged(output).size());
    }

    @Test
    public void neitherName_GivesTheDefault() throws Exception {
        List<String> output = probe(null, null);

        assertEquals(
            Arrays.asList(
                DidClient.DEFAULT_ENDPOINT, DidClient.DEFAULT_ENDPOINT),
            endpoints(output));
        assertEquals(output.toString(), 0, logged(output).size());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void oldConstant_KeepsItsNameAndValue() throws Exception {
        assertEquals(OLD_NAME, DidClient.ENDPOINT_VARIABLE);
        assertTrue(DidClient.class.getField("ENDPOINT_VARIABLE")
            .isAnnotationPresent(Deprecated.class));
        assertEquals(
            NEW_NAME,
            DidClient.class.getField("CLOUD_ENDPOINT_VARIABLE").get(null));
    }

    /**
     * Runs the probe with each variable set to the value given, or absent
     * where the value is null, and returns the lines it printed.
     */
    private static List<String> probe(String newValue, String oldValue)
        throws Exception {
        String java = Paths.get(
            System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder builder =
            new ProcessBuilder(java, EndpointProbe.class.getName());
        Map<String, String> environment = builder.environment();
        environment.put("CLASSPATH", classPath());
        set(environment, NEW_NAME, newValue);
        set(environment, OLD_NAME, oldValue);
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
            "The probe did not finish.",
            process.waitFor(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(output.toString(), 0, process.exitValue());
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
     * this module, its test classes and the JSON library the client uses.
     */
    private static String classPath() throws Exception {
        return location(DidClient.class) + File.pathSeparator
            + location(EndpointProbe.class) + File.pathSeparator
            + location(JSONObject.class);
    }

    private static String location(Class<?> type) throws Exception {
        return Paths.get(type.getProtectionDomain().getCodeSource()
            .getLocation().toURI()).toString();
    }

    private static List<String> endpoints(List<String> output) {
        return withPrefix(output, EndpointProbe.ENDPOINT_PREFIX);
    }

    private static List<String> logged(List<String> output) {
        return withPrefix(output, EndpointProbe.LOG_PREFIX);
    }

    private static List<String> withPrefix(
        List<String> output, String prefix) {
        List<String> result = new ArrayList<>();
        for (String line : output) {
            if (line.startsWith(prefix)) {
                result.add(line.substring(prefix.length()));
            }
        }
        return result;
    }
}
