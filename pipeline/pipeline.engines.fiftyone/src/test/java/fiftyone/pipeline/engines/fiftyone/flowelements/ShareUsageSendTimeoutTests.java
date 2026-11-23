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

import fiftyone.pipeline.engines.services.HttpClient;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A send through an {@link HttpClient} that never answers must end, or it
 * would stop all sending.
 */
public class ShareUsageSendTimeoutTests {

    /**
     * Opens a connection that records the timeouts set on it.
     */
    private static class RecordingHttpClient implements HttpClient {
        private HttpURLConnection connection;

        @Override
        public HttpURLConnection connect(URL url) {
            connection = new HttpURLConnection(url) {
                @Override
                public int getResponseCode() {
                    return 200;
                }

                @Override
                public String getResponseMessage() {
                    return "";
                }

                @Override
                public void disconnect() { }

                @Override
                public boolean usingProxy() {
                    return false;
                }

                @Override
                public void connect() { }
            };
            return connection;
        }

        @Override
        public String postData(
            HttpURLConnection connection,
            Map<String, String> headers,
            byte[] data) {
            return "";
        }

        @Override
        public String getResponseString(HttpURLConnection connection) {
            return "";
        }

        @Override
        public String getResponseString(
            HttpURLConnection connection,
            Map<String, String> headers) {
            return "";
        }
    }

    @Test
    public void ShareUsageElement_LegacySend_ConnectionHasTimeouts()
        throws Exception {
        RecordingHttpClient client = new RecordingHttpClient();
        ShareUsageElement element = new ShareUsageElement(
            LoggerFactory.getLogger(ShareUsageElement.class),
            1,
            1,
            100,
            100,
            100,
            1,
            true,
            "http://localhost/usage",
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME,
            flowData -> true);
        element.httpClient = client;
        try {
            element.legacySendAsXML(new ArrayList<>());

            assertEquals(
                element.httpSendTimeout,
                client.connection.getConnectTimeout(),
                "The connection has no connect timeout.");
            assertEquals(
                element.httpSendTimeout,
                client.connection.getReadTimeout(),
                "The connection has no read timeout.");
            assertTrue(element.httpSendTimeout > 0);
        } finally {
            element.close();
        }
    }
}
