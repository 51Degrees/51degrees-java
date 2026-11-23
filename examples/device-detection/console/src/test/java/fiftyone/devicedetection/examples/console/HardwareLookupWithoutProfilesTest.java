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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Runs the TAC and native model examples against a stand-in for the cloud
 * service that this test starts on this machine, so no resource key or
 * network is needed. The stand-in gives the accessible properties the cloud
 * gives a resource key that does not include HardwarePlatform.Profiles, the
 * evidence keys the cloud accepts, and 404 for anything else. Each example
 * should say the key lacks the lookup, link to where a key is created and
 * to the contact page, make no lookup and end normally.
 */
public class HardwareLookupWithoutProfilesTest {

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
        "?utm_source=code&utm_medium=example&utm_campaign=51degrees-java";

    private static final String CONFIGURE =
        "https://51degrees.com/configure/QKyYH5XT" + TAGS;

    private static final String CONTACT_US =
        "https://51degrees.com/contact-us" + TAGS;

    private static final String MESSAGE =
        "The resource key does not include HardwarePlatform.Profiles";

    private final List<String> requests = new CopyOnWriteArrayList<>();

    private HttpServer cloud;

    @Before
    public void startCloud() throws Exception {
        byte[] properties = fixture("accessibleproperties-no-hardware.json");
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
            byte[] answer = path.endsWith("accessibleproperties") ?
                properties :
                path.endsWith("evidencekeys") ? evidenceKeys : null;
            if (answer == null) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            exchange.getResponseHeaders().add("Content-Type",
                "application/json");
            exchange.sendResponseHeaders(200, answer.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(answer);
            }
        });
        cloud.start();
    }

    @After
    public void stopCloud() {
        cloud.stop(0);
    }

    private static byte[] fixture(String name) throws IOException {
        try (InputStream input =
                 HardwareLookupWithoutProfilesTest.class
                     .getResourceAsStream(name)) {
            assertNotNull("The fixture " + name + " should be a resource",
                input);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                bytes.write(buffer, 0, read);
            }
            return bytes.toByteArray();
        }
    }

    private void runWithoutProfiles(Class<?> example) throws Exception {
        Map<String, String> environment = new HashMap<>();
        environment.put(KeyHelper.RESOURCE_KEY_VARIABLE, RESOURCE_KEY);
        environment.put(CLOUD_ENDPOINT, "http://" +
            cloud.getAddress().getHostString() + ":" +
            cloud.getAddress().getPort() + "/api/v4/");
        ExampleProcess.Result result =
            ExampleProcess.runWith(example, environment);
        System.out.println(result.output);
        String output = result.output;

        assertEquals(example.getSimpleName() +
                " should end with exit code 0.\n" + output,
            0, result.exitCode);
        String message = null;
        for (String line : output.split("\r?\n")) {
            assertFalse("No lookup should be listed.\n" + output,
                line.startsWith("Which devices are associated"));
            if (line.startsWith(MESSAGE)) {
                message = line;
            }
        }
        assertNotNull("The output should say the key does not include " +
            "HardwarePlatform.Profiles.\n" + output, message);
        assertTrue("The message should link to where a key is created.\n" +
            message, message.contains(CONFIGURE));
        assertTrue("The message should link to the contact page.\n" +
            message, message.contains(CONTACT_US));
        assertTrue("The output should end with the '" + FindOutMore.HEADING +
                "' section after the message.\n" + output,
            output.lastIndexOf(FindOutMore.HEADING) >
                output.indexOf(MESSAGE));
        for (String request : requests) {
            assertFalse("No lookup request should be made, and " +
                    request + " was requested.",
                request.endsWith("json"));
        }
        assertTrue("The example should have asked the stand-in for the " +
                "accessible properties, and asked for " + requests,
            requests.stream().anyMatch(
                request -> request.endsWith("accessibleproperties")));
    }

    @Test
    public void tacCloud() throws Exception {
        runWithoutProfiles(TacCloud.class);
    }

    @Test
    public void nativeModelCloud() throws Exception {
        runWithoutProfiles(NativeModelCloud.class);
    }
}
