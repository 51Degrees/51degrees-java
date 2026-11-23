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

package fiftyone.geolocation.examples;

import com.sun.net.httpserver.HttpServer;
import fiftyone.geolocation.examples.cloud.GettingStarted;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The Location cloud examples call the cloud service that
 * _51DEGREES_CLOUD_ENDPOINT names, and the public service when it is not
 * set. These tests need no key and no network, because the service they
 * name is a server the test starts on this machine.
 */
public class CloudEndpointTests {

    private static final String ENDPOINT_NAME = "_51DEGREES_CLOUD_ENDPOINT";

    private static final String PUBLIC_SERVICE =
        "https://cloud.51degrees.com/api/v4";

    /**
     * The two web examples, each with an options file of its own.
     */
    private static final String[] WEB_EXAMPLES = {
        "geo-location.web.examples.mvc",
        "geo-location.web.examples.servlet"
    };

    /**
     * The shell example is run in a process of its own with the variable
     * naming a local server, and every call it makes has to arrive there.
     */
    @Test
    public void gettingStarted_CallsTheServiceTheVariableNames()
        throws Exception {
        List<String> paths = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(
                "Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            Map<String, String> environment = new HashMap<>();
            environment.put(ENDPOINT_NAME, "http://127.0.0.1:"
                + server.getAddress().getPort() + "/api/v4/");
            environment.put(ResourceKey.VARIABLE, "not-a-real-key");
            ExampleProcess.Result result =
                ExampleProcess.runWith(GettingStarted.class, environment);
            assertFalse("The example made no call to the service "
                + ENDPOINT_NAME + " names. It wrote:\n" + result.output,
                paths.isEmpty());
            for (String path : paths) {
                assertTrue("A call outside the address the variable "
                    + "names: " + path, path.startsWith("/api/v4/"));
            }
        } finally {
            server.stop(0);
        }
    }

    /**
     * Each web example's options file reads the variable, and gives the
     * public service for when it is not set.
     */
    @Test
    public void webExamples_OptionsReadTheVariable() throws IOException {
        Path location = Paths.get(System.getProperty("basedir", "."))
            .toAbsolutePath().normalize().getParent();
        for (String example : WEB_EXAMPLES) {
            Path file = location.resolve(Paths.get(
                "geo-location.web.examples", example, "src", "main",
                "webapp", "WEB-INF", "51Degrees-Cloud.xml"));
            String xml = new String(
                Files.readAllBytes(file), StandardCharsets.UTF_8);
            Matcher endPoint = Pattern.compile(
                "<EndPoint>\\$\\{([^:}]*):-([^}]*)\\}</EndPoint>")
                .matcher(xml);
            assertTrue(file + " should read its endpoint from a variable, "
                + "with a default", endPoint.find());
            assertEquals(file.toString(), ENDPOINT_NAME, endPoint.group(1));
            assertEquals(file.toString(), PUBLIC_SERVICE, endPoint.group(2));
        }
    }
}
