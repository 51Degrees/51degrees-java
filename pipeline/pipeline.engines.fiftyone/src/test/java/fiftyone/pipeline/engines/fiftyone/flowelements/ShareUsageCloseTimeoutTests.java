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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.DataUploader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static fiftyone.pipeline.core.Constants.EVIDENCE_CLIENTIP_KEY;
import static fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Closing waits up to 5 seconds for the data waiting to be sent. If it has
 * not all been sent by then, sending stops, the rest is discarded and a
 * warning gives the number discarded. See Cleanup in the usage sharing
 * element specification.
 */
public class ShareUsageCloseTimeoutTests {

    /**
     * The longest the test waits for something it expects to happen.
     */
    private static final long WAIT_LIMIT_MILLIS = 30000;

    /**
     * Entries processed, enough for several messages of at most two.
     */
    private static final int ENTRIES = 10;

    /**
     * An upload whose first answer does not come until the test releases
     * it, as with a remote service that has stopped answering. Interrupting
     * the waiting thread does not end the wait, as it would not end a
     * stalled connection.
     */
    private static class StalledUploader implements DataUploader {
        private final CountDownLatch sending = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicInteger sends = new AtomicInteger();

        @Override
        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }

        @Override
        public int getResponseCode() {
            sends.incrementAndGet();
            sending.countDown();
            while (release.getCount() > 0) {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                    // Keeps waiting, as a stalled connection would.
                }
            }
            return 200;
        }

        @Override
        public String getResponseContent() {
            return "";
        }
    }

    @Test
    public void ShareUsageElement_CloseTimesOut_StopsSendingAndDiscards()
        throws Exception {
        List<String> warnings = new CopyOnWriteArrayList<>();
        StalledUploader uploader = new StalledUploader();
        Pipeline pipeline = new PipelineBuilder().build();
        ShareUsageElement element = new ShareUsageElement(
            recordingLogger(warnings),
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
            DEFAULT_SESSION_COOKIE_NAME,
            flowData -> true);
        element.addPipeline(pipeline);
        element.dataUploader = uploader;
        try {
            for (int i = 1; i <= ENTRIES; i++) {
                process(pipeline, element, "10.0.0." + i);
            }
            assertTrue(
                uploader.sending.await(WAIT_LIMIT_MILLIS, TimeUnit.MILLISECONDS),
                "No send was started.");

            element.close();
            uploader.release.countDown();

            assertTrue(
                element.executor.awaitTermination(
                    WAIT_LIMIT_MILLIS,
                    TimeUnit.MILLISECONDS),
                "Sending did not stop after closing.");
            assertEquals(1, uploader.sends.get(),
                "Data was sent after closing gave up waiting.");
            assertTrue(
                warnings.stream().anyMatch(warning ->
                    warning.contains("were not sent") &&
                    warning.matches(".*\\b[1-9][0-9]*\\b.*")),
                "Discarding the data left was not logged with its count. " +
                    "Warnings: " + warnings);
        }
        finally {
            uploader.release.countDown();
            if (element.executor.isShutdown() == false) {
                element.close();
            }
            pipeline.close();
        }
    }

    private static void process(
        Pipeline pipeline,
        ShareUsageElement element,
        String ip) throws Exception {
        Map<String, Object> evidence = new HashMap<>();
        evidence.put(EVIDENCE_CLIENTIP_KEY, ip);
        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidence);
            element.process(data);
        }
    }

    /**
     * A logger that records the warnings it is given.
     */
    private static Logger recordingLogger(List<String> warnings) {
        return (Logger) Proxy.newProxyInstance(
            Logger.class.getClassLoader(),
            new Class<?>[] {Logger.class},
            (proxy, method, args) -> {
                if (method.getName().equals("warn")) {
                    warnings.add(Arrays.deepToString(args));
                    return null;
                }
                if (method.getReturnType() == boolean.class) {
                    return false;
                }
                if (method.getReturnType() == int.class) {
                    return 0;
                }
                if (method.getReturnType() == String.class) {
                    return "test";
                }
                return null;
            });
    }
}
