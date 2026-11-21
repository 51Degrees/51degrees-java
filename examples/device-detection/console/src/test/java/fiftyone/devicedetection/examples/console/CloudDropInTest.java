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
import fiftyone.devicedetection.DeviceDetectionPipelineBuilder;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * A cloud engine is a drop-in replacement for an on-premise engine: the
 * same code reads the results of either
 * (device-detection-specification/usage-examples.md, "In particular, a
 * cloud Engine MUST be a drop-in replacement for an on-premise Engine for
 * performing Device Detection and querying the results", and "However,
 * once created, usage SHOULD be as similar as possible").
 *
 * {@link #isMobile(Pipeline)} is that code. It reads the result by its type,
 * by the element data key "device", and a property by name, and it is given
 * a cloud pipeline and an on-premise one. The cloud pipeline talks to a
 * stand-in for the cloud service that this test starts on this machine, so
 * no resource key or network is needed. The on-premise pipeline uses the
 * free Lite data file, which a fresh clone fetches from the data
 * repository; the test does not download it.
 */
public class CloudDropInTest {

    private static final String IPHONE =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) " +
        "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 " +
        "Mobile/15E148 Safari/604.1";

    private HttpServer cloud;

    @Before
    public void startCloud() throws Exception {
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
            String answer;
            if (path.endsWith("accessibleproperties")) {
                answer = "{\"Products\":{\"device\":{\"DataTier\":\"Lite\"," +
                    "\"Properties\":[{\"Name\":\"IsMobile\"," +
                    "\"Type\":\"Boolean\",\"Category\":\"Device\"}]}}}";
            } else if (path.endsWith("evidencekeys")) {
                answer = "[\"header.user-agent\"]";
            } else {
                answer = "{\"device\":{\"ismobile\":true}," +
                    "\"javascriptProperties\":[]}";
            }
            byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type",
                "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        cloud.start();
    }

    @After
    public void stopCloud() {
        cloud.stop(0);
    }

    /**
     * The code a developer writes once, for either engine.
     */
    private static boolean isMobile(Pipeline pipeline) throws Exception {
        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", IPHONE).process();
            DeviceData device = data.get(DeviceData.class);
            ElementData byKey = data.get("device");
            assertSame("The result read by type and by the key 'device' " +
                "should be the same", device, byKey);
            Object byName = byKey.get("ismobile");
            assertTrue("The property read by name should be a value " +
                    "with a reason when it has none, and was " + byName,
                byName instanceof AspectPropertyValue);
            return device.getIsMobile().getValue();
        }
    }

    @Test
    public void cloudResultsReadAsOnPremise() throws Exception {
        String endPoint = "http://" + cloud.getAddress().getHostString() +
            ":" + cloud.getAddress().getPort() + "/api/v4/";
        try (Pipeline pipeline = new DeviceDetectionPipelineBuilder()
            .useCloud("dropinresourcekey")
            .setEndPoint(endPoint)
            .build()) {
            assertTrue(isMobile(pipeline));
        }
    }

    @Test
    public void onPremiseResultsReadAsCloud() throws Exception {
        String file = getFilePath("device-detection/51Degrees-LiteV4.1.hash")
            .getAbsolutePath();
        try (Pipeline pipeline = new DeviceDetectionPipelineBuilder()
            .useOnPremise(file, false)
            .setAutoUpdate(false)
            .setShareUsage(false)
            .build()) {
            assertTrue(isMobile(pipeline));
        }
    }
}
