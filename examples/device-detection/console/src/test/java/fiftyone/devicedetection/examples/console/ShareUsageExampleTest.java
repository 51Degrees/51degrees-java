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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

/**
 * Runs the usage sharing example, so that a change that stops it working
 * is seen. The specification asks for a unit test of every example
 * (device-detection-specification/required-examples.md, "There MUST be unit
 * tests for examples"). The example's messages go to an endpoint the test
 * starts on this machine, so the test needs no network and shares nothing
 * with 51Degrees. The evidence is held in the test, so no data file is
 * needed either.
 */
public class ShareUsageExampleTest {

    private HttpServer server;
    private final AtomicInteger messages = new AtomicInteger();

    @Before
    public void startEndpoint() throws Exception {
        server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            try (InputStream body = exchange.getRequestBody()) {
                byte[] buffer = new byte[4096];
                while (body.read(buffer) >= 0) {
                    // Read the whole message before answering.
                }
            }
            messages.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
    }

    @After
    public void stopEndpoint() {
        server.stop(0);
    }

    /**
     * YAML documents in the shape of the 20,000 evidence records file, one
     * set of evidence each, enough for more than one message at the
     * example's batch size.
     */
    private static InputStream evidence(int records) {
        StringBuilder yaml = new StringBuilder();
        for (int i = 0; i < records; i++) {
            yaml.append("---\n")
                .append("header.user-agent: 'Mozilla/5.0 (Windows NT 10.0; ")
                .append("Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) ")
                .append("Chrome/").append(100 + i).append(".0.0.0 Safari/537.36'\n")
                .append("header.sec-ch-ua-mobile: '?0'\n")
                .append("query.ignored: 'not a header, so not shared'\n");
        }
        return new ByteArrayInputStream(yaml.toString()
                .getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void runsAndShares() throws Exception {
        String url = "http://" + server.getAddress().getHostString() + ":" +
                server.getAddress().getPort() + "/new.ashx";

        ShareUsageExample.run(evidence(30), url);

        assertTrue("The example should send its evidence to the usage " +
                "sharing endpoint it is given, and it sent nothing",
                messages.get() > 0);
    }
}
