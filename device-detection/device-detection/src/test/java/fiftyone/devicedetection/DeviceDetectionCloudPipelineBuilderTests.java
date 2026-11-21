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

import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.services.HttpClient;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * The cloud Pipeline builder passes the keys and the requested properties
 * to the Cloud Request Engine, so a Pipeline can authenticate on a license
 * key alone. A fake cloud service records each data request.
 */
public class DeviceDetectionCloudPipelineBuilderTests {

    private static final String BASE = "https://cloud.example/api/v4/";
    private static final String LICENSE_KEY = "abcdefghijkl1234";
    private static final String RESOURCE_KEY = "resourcekey";
    private static final String FIRST = "device.ismobile";
    private static final String SECOND = "device.iscrawler";
    private static final String PROPERTIES_RESPONSE =
        "{\"Products\":{\"device\":{\"DataTier\":\"t\",\"Properties\":"
        + "[{\"Name\":\"IsMobile\",\"Type\":\"Boolean\","
        + "\"Category\":\"c\"}]}}}";
    private static final String DATA_RESPONSE = "{\"device\":{\"ismobile\":true}}";

    /** Records every data request and answers from fixed responses. */
    private static class FakeCloud implements HttpClient {
        final List<String> dataRequests =
            Collections.synchronizedList(new ArrayList<String>());

        @Override
        public HttpURLConnection connect(URL url) {
            return new HttpURLConnection(url) {
                @Override
                public void connect() {
                }

                @Override
                public void disconnect() {
                }

                @Override
                public boolean usingProxy() {
                    return false;
                }

                @Override
                public int getResponseCode() {
                    return 200;
                }
            };
        }

        @Override
        public String postData(
            HttpURLConnection connection,
            Map<String, String> headers,
            byte[] data) {
            return answer(
                connection.getURL(),
                data == null ? "" :
                    new String(data, StandardCharsets.UTF_8));
        }

        @Override
        public String getResponseString(HttpURLConnection connection) {
            return answer(connection.getURL(), "");
        }

        @Override
        public String getResponseString(
            HttpURLConnection connection,
            Map<String, String> headers) {
            return answer(connection.getURL(), "");
        }

        private String answer(URL url, String body) {
            String path = url.getPath();
            if (path.endsWith("accessibleproperties")) {
                return PROPERTIES_RESPONSE;
            }
            if (path.endsWith("evidencekeys")) {
                return "[\"header.user-agent\"]";
            }
            dataRequests.add(body);
            return DATA_RESPONSE;
        }
    }

    private FakeCloud cloud;

    @Before
    public void setUp() {
        cloud = new FakeCloud();
    }

    private DeviceDetectionCloudPipelineBuilder builder(String resourceKey) {
        return new DeviceDetectionPipelineBuilder(
            LoggerFactory.getILoggerFactory(), cloud)
            .useCloud(resourceKey)
            .setEndPoint(BASE);
    }

    /** Process one request and give the fields of the form it sent. */
    private Map<String, String> process(DeviceDetectionCloudPipelineBuilder builder)
        throws Exception {
        try (Pipeline pipeline = builder.build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", "iPhone").process();
            assertTrue(data.get(DeviceData.class)
                .getIsMobile().getValue());
        }
        assertEquals(1, cloud.dataRequests.size());
        return form(cloud.dataRequests.get(0));
    }

    private static Map<String, String> form(String body)
        throws UnsupportedEncodingException {
        Map<String, String> fields = new HashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            String[] parts = pair.split("=", 2);
            fields.put(
                URLDecoder.decode(parts[0], "UTF-8"),
                URLDecoder.decode(parts.length > 1 ? parts[1] : "", "UTF-8"));
        }
        return fields;
    }

    @Test
    public void LicenseKeyAlone_RequestedProperties_SentAsValues()
        throws Exception {
        Map<String, String> sent = process(builder(null)
            .setLicenseKey(LICENSE_KEY)
            .setRequestedProperties(Arrays.asList(FIRST, SECOND)));

        assertEquals(LICENSE_KEY, sent.get("license"));
        assertEquals(FIRST + "," + SECOND, sent.get("values"));
        assertFalse(sent.containsKey("resource"));
    }

    @Test
    public void LicenseKeyAlone_RequestedPropertiesAsText_SentAsValues()
        throws Exception {
        Map<String, String> sent = process(builder("")
            .setLicenseKey(LICENSE_KEY)
            .setRequestedProperties(FIRST + ", " + SECOND));

        assertEquals(LICENSE_KEY, sent.get("license"));
        assertEquals(FIRST + "," + SECOND, sent.get("values"));
        assertFalse(sent.containsKey("resource"));
    }

    @Test
    public void BothKeys_RequestedProperties_SendsAllThree()
        throws Exception {
        Map<String, String> sent = process(builder(RESOURCE_KEY)
            .setLicenseKey(LICENSE_KEY)
            .setRequestedProperties(Collections.singletonList(FIRST)));

        assertEquals(RESOURCE_KEY, sent.get("resource"));
        assertEquals(LICENSE_KEY, sent.get("license"));
        assertEquals(FIRST, sent.get("values"));
    }

    @Test
    public void ResourceKey_NoRequestedProperties_SendsNoValues()
        throws Exception {
        Map<String, String> sent = process(builder(RESOURCE_KEY));

        assertEquals(RESOURCE_KEY, sent.get("resource"));
        assertFalse(sent.containsKey("license"));
        assertFalse(sent.containsKey("values"));
    }

    @Test
    public void LicenseKeyAlone_NoRequestedProperties_Refused()
        throws Exception {
        try {
            builder(null).setLicenseKey(LICENSE_KEY).build().close();
            fail("A license key with no requested properties was built.");
        } catch (PipelineConfigurationException e) {
            assertTrue(e.getMessage(),
                e.getMessage().contains("answers 400"));
            assertTrue(e.getMessage(),
                e.getMessage().contains("setRequestedProperties"));
            assertFalse(e.getMessage().contains(LICENSE_KEY));
        }
        assertTrue(cloud.dataRequests.isEmpty());
    }

    @Test
    public void SetRequestedProperties_Null_Refused() {
        try {
            builder(RESOURCE_KEY).setRequestedProperties((List<String>) null);
            fail("A null list was accepted.");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("properties"));
        }
        try {
            builder(RESOURCE_KEY).setRequestedProperties((String) null);
            fail("A null string was accepted.");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("properties"));
        }
    }
}
