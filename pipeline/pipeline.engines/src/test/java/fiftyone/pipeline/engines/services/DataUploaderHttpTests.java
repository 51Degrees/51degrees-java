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

package fiftyone.pipeline.engines.services;

import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;

import org.junit.Test;

/**
 * An upload to a service that accepts the connection and never answers must
 * end, or it would stop all usage sharing.
 */
public class DataUploaderHttpTests {

    /**
     * How long the uploader waits for an answer.
     */
    private static final int TIMEOUT_MILLIS = 500;

    /**
     * How long the test service holds a connection open without answering.
     */
    private static final int HOLD_MILLIS = 30000;

    /**
     * The test fails if the upload has not ended by then.
     */
    private static final int LIMIT_MILLIS = 15000;

    @Test(timeout = LIMIT_MILLIS)
    public void DataUploaderHttp_NoAnswer_TimesOut() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread holder = new Thread(() -> {
                try (Socket socket = server.accept()) {
                    Thread.sleep(HOLD_MILLIS);
                } catch (Exception ignored) {
                    // The test has ended and closed the server.
                }
            });
            holder.setDaemon(true);
            holder.start();
            DataUploaderHttp uploader = new DataUploaderHttp(
                "http://localhost:" + server.getLocalPort() + "/usage",
                new HashMap<>(),
                TIMEOUT_MILLIS);
            try (OutputStream out = uploader.getOutputStream()) {
                out.write(new byte[] {1});
            }

            try {
                uploader.getResponseCode();
                fail("The upload was answered.");
            } catch (SocketTimeoutException expected) {
                // The wait for an answer ended.
            }
        }
    }
}
