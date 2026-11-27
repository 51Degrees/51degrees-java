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

package fiftyone.devicedetection.examples.console;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fiftyone.devicedetection.examples.shared.ExampleProcess;
import fiftyone.devicedetection.examples.shared.FindOutMore;
import fiftyone.devicedetection.examples.shared.KeyHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Runs the TAC and native model examples against a stand-in for the cloud
 * service that this test starts on this machine, so no resource key or
 * network is needed. The stand-in answers the accessible properties, the
 * evidence keys and the lookup with what each test sets, and anything else
 * with 404, and records the path of every request.
 */
public class HardwareLookupStandInTest {

    /**
     * The variable the cloud request engine reads its endpoint from.
     */
    private static final String CLOUD_ENDPOINT = "_51DEGREES_CLOUD_ENDPOINT";

    /**
     * A value the examples accept as a resource key, which the stand-in
     * does not check.
     */
    private static final String RESOURCE_KEY = "standInResourceKey000000";

    private static final String TAGS =
        "?utm_source=code&utm_medium=example&utm_campaign=51degrees-java" +
        "&utm_content=";

    private static final String MESSAGE =
        "The resource key does not include HardwarePlatform.Profiles";

    private static final String HEADING = "Which devices are associated";

    /**
     * The error the stand-in gives when it fails a request.
     */
    private static final String CLOUD_ERROR =
        "The stand-in cloud service failed this request.";

    /**
     * A lookup answer for a resource key without the hardware element.
     */
    private static final String NO_HARDWARE_ANSWER =
        "{\"device\":{\"ismobile\":true},\"javascriptProperties\":[]}";

    private final List<String> requests = new CopyOnWriteArrayList<>();

    /**
     * How many more accessible properties requests to answer with 500.
     */
    private final AtomicInteger propertiesFailures = new AtomicInteger();

    private volatile byte[] properties;

    private volatile byte[] lookup;

    private HttpServer cloud;

    @Before
    public void startCloud() throws Exception {
        byte[] evidenceKeys = fixture("evidencekeys.json");
        cloud = HttpServer.create(
            new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        cloud.createContext("/", exchange -> {
            try (InputStream body = exchange.getRequestBody()) {
                byte[] buffer = new byte[4096];
                while (body.read(buffer) >= 0) {
                    // Read the whole request before answering.
                }
            }
            String path = exchange.getRequestURI().getPath();
            requests.add(path);
            if (path.endsWith("accessibleproperties")) {
                if (propertiesFailures.getAndUpdate(
                        failures -> Math.max(0, failures - 1)) > 0) {
                    answer(exchange, 500, ("{\"errors\":[\"" + CLOUD_ERROR +
                        "\"]}").getBytes(StandardCharsets.UTF_8));
                } else {
                    answer(exchange, 200, properties);
                }
            } else if (path.endsWith("evidencekeys")) {
                answer(exchange, 200, evidenceKeys);
            } else if (path.endsWith("json") && lookup != null) {
                answer(exchange, 200, lookup);
            } else {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        });
        cloud.start();
    }

    @After
    public void stopCloud() {
        cloud.stop(0);
    }

    private static void answer(HttpExchange exchange, int status,
                               byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private static byte[] fixture(String name) throws IOException {
        try (InputStream input =
                 HardwareLookupStandInTest.class.getResourceAsStream(name)) {
            if (input == null) {
                throw new IOException(
                    "The fixture " + name + " should be a resource.");
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                bytes.write(buffer, 0, read);
            }
            return bytes.toByteArray();
        }
    }

    /**
     * Runs the example's main method in its own process, with a resource
     * key and the stand-in as its cloud service.
     */
    private ExampleProcess.Result run(Class<?> example) throws Exception {
        Map<String, String> environment = new HashMap<>();
        environment.put(KeyHelper.RESOURCE_KEY_VARIABLE, RESOURCE_KEY);
        environment.put(CLOUD_ENDPOINT, "http://" +
            cloud.getAddress().getHostString() + ":" +
            cloud.getAddress().getPort() + "/api/v4/");
        ExampleProcess.Result result =
            ExampleProcess.runWith(example, environment);
        System.out.println(result.output);
        return result;
    }

    /**
     * Fails unless the example ended with exit code 0 and logged no error.
     * Unless the stand-in failed a request, which the library logs as a
     * warning with its stack trace, no stack trace may be printed either.
     */
    private static String requireEndedNormally(Class<?> example,
                                               ExampleProcess.Result result,
                                               boolean failedRequest) {
        String output = plain(result.output);
        assertEquals(example.getSimpleName() +
                " should end with exit code 0.\n" + output,
            0, result.exitCode);
        for (String line : lines(output)) {
            assertFalse("Nothing should be logged as an error.\n" + output,
                line.contains(" ERROR "));
            assertFalse("No stack trace should be printed.\n" + output,
                failedRequest == false && (
                    line.startsWith("Exception") ||
                    line.startsWith("Caused by") ||
                    line.startsWith("\tat ")));
        }
        assertTrue("The output should hold the '" + FindOutMore.HEADING +
                "' section.\n" + output,
            output.contains(FindOutMore.HEADING));
        return output;
    }

    /**
     * The output without the color codes around the level of each log
     * line.
     */
    private static String plain(String output) {
        return output.replaceAll("\u001B\\[[0-9;]*m", "");
    }

    private static List<String> lines(String output) {
        return Arrays.asList(output.split("\r?\n"));
    }

    /**
     * The line the example prints when the key lacks the lookup.
     */
    private static String noHardwareLine(Class<?> example) {
        String content = "console-src-main-java-fiftyone-devicedetection-" +
            "examples-console-" +
            example.getSimpleName().toLowerCase(Locale.ROOT) + ".java";
        String tags = TAGS + content + "&utm_term=no-hardware-lookup";
        return MESSAGE + ", the hardware lookup this example needs, so no " +
            "devices are listed. Create a resource key that includes it at " +
            "https://51degrees.com/configure/QKyYH5XT" + tags +
            ", or contact us about it at https://51degrees.com/contact-us" +
            tags + ".";
    }

    private int lookupRequests() {
        int count = 0;
        for (String request : requests) {
            if (request.endsWith("json")) {
                count++;
            }
        }
        return count;
    }

    private void requireNoHardwareLine(Class<?> example, String output) {
        assertTrue("The output should hold the line that says the key " +
                "does not include HardwarePlatform.Profiles, being\n" +
                noHardwareLine(example) + "\n" + output,
            lines(output).contains(noHardwareLine(example)));
        assertTrue("The '" + FindOutMore.HEADING + "' section should " +
                "follow the line.\n" + output,
            output.lastIndexOf(FindOutMore.HEADING) >
                output.indexOf(MESSAGE));
        requireNoHeading(output);
    }

    private static void requireNoHeading(String output) {
        for (String line : lines(output)) {
            assertFalse("No lookup should be listed.\n" + output,
                line.startsWith(HEADING));
        }
    }

    /**
     * The accessible properties have no hardware element, as for a key
     * without HardwarePlatform.Profiles. The example should say so in one
     * line, make no lookup and end normally.
     */
    private void withoutProfiles(Class<?> example) throws Exception {
        properties = fixture("accessibleproperties-no-hardware.json");
        String output = requireEndedNormally(example, run(example), false);
        requireNoHardwareLine(example, output);
        assertEquals("No lookup request should be made, and " + requests +
            " were made.", 0, lookupRequests());
        assertTrue("The example should have asked the stand-in for the " +
                "accessible properties, and asked for " + requests,
            requests.stream().anyMatch(
                request -> request.endsWith("accessibleproperties")));
    }

    /**
     * The key includes the lookup and each lookup matches no device. The
     * example should print each heading with the line that says so under
     * it, and end normally.
     */
    private void noDevices(Class<?> example, String subject, String none,
                           String... values) throws Exception {
        properties = fixture("accessibleproperties-with-hardware.json");
        lookup = fixture("json-no-devices.json");
        String output = requireEndedNormally(example, run(example), false);
        List<String> lines = lines(output);
        for (String value : values) {
            String heading = HEADING + " with " + subject + " '" + value +
                "'?";
            int at = lines.indexOf(heading);
            assertTrue("The output should hold the heading " + heading +
                "\n" + output, at >= 0);
            assertTrue("'" + none + "' should follow the heading " +
                    heading + "\n" + output,
                at + 1 < lines.size() && lines.get(at + 1).equals(none));
        }
        assertFalse("The output should not say the key lacks the " +
            "lookup.\n" + output, output.contains(MESSAGE));
        assertEquals("A lookup request should be made for each value, and " +
            requests + " were made.", values.length, lookupRequests());
    }

    /**
     * The accessible properties request fails every time. The example
     * should end with the cloud service's error, and not say the key lacks
     * the lookup.
     */
    private void propertiesFailEveryTime(Class<?> example) throws Exception {
        properties = fixture("accessibleproperties-no-hardware.json");
        propertiesFailures.set(Integer.MAX_VALUE);
        ExampleProcess.Result result = run(example);
        String output = plain(result.output);
        assertNotEquals(example.getSimpleName() +
                " should end with an error.\n" + output,
            0, result.exitCode);
        // The failure as the pipeline is built is a warning, so the error
        // has to follow a line logged as an error.
        int error = output.indexOf(" ERROR ");
        assertTrue("The cloud service's error should be logged as an " +
                "error.\n" + output,
            error >= 0 && output.indexOf(CLOUD_ERROR, error) >= 0);
        assertFalse("The output should not say the key lacks the " +
            "lookup.\n" + output, output.contains(MESSAGE));
        requireNoHeading(output);
        assertEquals("No lookup request should be made, and " + requests +
            " were made.", 0, lookupRequests());
    }

    /**
     * The accessible properties request fails as the pipeline is built and
     * then gives no hardware element. The example should fetch them again
     * and decide from them, saying the key lacks the lookup and ending
     * normally, rather than listing no devices.
     */
    private void propertiesFailOnce(Class<?> example) throws Exception {
        properties = fixture("accessibleproperties-no-hardware.json");
        lookup = NO_HARDWARE_ANSWER.getBytes(StandardCharsets.UTF_8);
        propertiesFailures.set(1);
        String output = requireEndedNormally(example, run(example), true);
        requireNoHardwareLine(example, output);
        assertEquals("The accessible properties should be asked for " +
                "twice, and " + requests + " were requested.",
            2, requests.stream().filter(
                request -> request.endsWith("accessibleproperties")).count());
        // Processing the first lookup is what fetches them again, so that
        // lookup is made, and no other.
        assertTrue("At most one lookup request should be made, and " +
            requests + " were made.", lookupRequests() <= 1);
    }

    @Test
    public void tacCloudWithoutProfiles() throws Exception {
        withoutProfiles(TacCloud.class);
    }

    @Test
    public void nativeModelCloudWithoutProfiles() throws Exception {
        withoutProfiles(NativeModelCloud.class);
    }

    @Test
    public void tacCloudNoDevices() throws Exception {
        noDevices(TacCloud.class, "the TAC",
            "\tNo devices are associated with this TAC.",
            TacCloud.TAC_1, TacCloud.TAC_2);
    }

    @Test
    public void nativeModelCloudNoDevices() throws Exception {
        noDevices(NativeModelCloud.class, "the native model name",
            "\tNo devices are associated with this native model name.",
            NativeModelCloud.NATIVE_MODEL_1, NativeModelCloud.NATIVE_MODEL_2);
    }

    @Test
    public void tacCloudPropertiesFailEveryTime() throws Exception {
        propertiesFailEveryTime(TacCloud.class);
    }

    @Test
    public void nativeModelCloudPropertiesFailEveryTime() throws Exception {
        propertiesFailEveryTime(NativeModelCloud.class);
    }

    @Test
    public void tacCloudPropertiesFailOnce() throws Exception {
        propertiesFailOnce(TacCloud.class);
    }

    @Test
    public void nativeModelCloudPropertiesFailOnce() throws Exception {
        propertiesFailOnce(NativeModelCloud.class);
    }
}
