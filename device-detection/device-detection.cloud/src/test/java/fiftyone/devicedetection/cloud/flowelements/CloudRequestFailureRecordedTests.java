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

package fiftyone.devicedetection.cloud.flowelements;

import fiftyone.devicedetection.cloud.data.DeviceDataCloud;
import fiftyone.pipeline.cloudrequestengine.CloudRequestException;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngineBuilder;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.services.HttpClient;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A failed Cloud Request Engine request that the Device Detection cloud
 * engine meets while the Flow Data is processed is recorded once, as the
 * Cloud Request Engine's error and not the Device Detection engine's, and a
 * property is then missing with the CloudRequestFailed reason.
 * @see <a href="https://github.com/51Degrees/specifications/blob/52e9b047/pipeline-specification/pipeline-elements/cloud-request-engine.md#processing">Specification</a>
 */
public class CloudRequestFailureRecordedTests {

    private static final String PROPERTIES =
        "{'Products': {'device': {'DataTier': 'tier', 'Properties': " +
        "[{'Name': 'HardwareVendor', 'Type': 'String', " +
        "'Category': 'Device'}]}}}";

    private static final String EVIDENCE_KEYS = "['query.User-Agent']";

    private static final String FAILED =
        "{\"errors\":[\"The request failed.\"]}";

    /**
     * A client that answers the discovery requests and fails the request
     * for the Flow Data with an error response.
     */
    private static HttpClient failingClient() throws Exception {
        HttpClient client = mock(HttpClient.class);
        when(client.connect(any(URL.class))).thenAnswer(invocation -> {
            URL url = invocation.getArgument(0);
            HttpURLConnection connection = mock(HttpURLConnection.class);
            when(connection.getURL()).thenReturn(url);
            when(connection.getResponseCode()).thenReturn(200);
            when(connection.getHeaderFields()).thenReturn(new HashMap<>());
            return connection;
        });
        Answer<String> byUrl = invocation -> {
            String path = ((HttpURLConnection) invocation.getArgument(0))
                .getURL().getPath();
            if (path.endsWith("properties")) {
                return PROPERTIES;
            }
            if (path.endsWith("evidencekeys")) {
                return EVIDENCE_KEYS;
            }
            return FAILED;
        };
        when(client.getResponseString(any(HttpURLConnection.class)))
            .thenAnswer(byUrl);
        when(client.getResponseString(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap())).thenAnswer(byUrl);
        when(client.postData(
            any(HttpURLConnection.class),
            ArgumentMatchers.<String, String>anyMap(),
            any(byte[].class))).thenAnswer(byUrl);
        return client;
    }

    @Test
    public void FailedRequest_RecordedOnceAsTheRequestEnginesError()
        throws Exception {
        Logger logger = mock(Logger.class);
        ILoggerFactory loggerFactory = mock(ILoggerFactory.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);
        CloudRequestEngine requestEngine =
            new CloudRequestEngineBuilder(loggerFactory, failingClient())
                .setResourceKey("resource_key")
                .build();
        DeviceDetectionCloudEngine deviceEngine =
            new DeviceDetectionCloudEngineBuilder(loggerFactory).build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(requestEngine)
                .addFlowElement(deviceEngine)
                .setAutoCloseElements(true)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();

            List<FlowError> errors = new ArrayList<>(data.getErrors());
            assertEquals(errors.toString(), 1, errors.size());
            assertSame(
                "The failure was recorded as another element's error",
                requestEngine,
                errors.get(0).getFlowElement());
            assertTrue(
                errors.get(0).getThrowable().toString(),
                errors.get(0).getThrowable() instanceof CloudRequestException);

            DeviceDataCloud device = data.getFromElement(deviceEngine);
            try {
                device.getHardwareVendor();
                fail("A PropertyMissingException should have been thrown");
            } catch (PropertyMissingException e) {
                assertEquals(
                    e.getMessage(),
                    "CloudRequestFailed",
                    e.getReason().name());
            }
        }
    }
}
