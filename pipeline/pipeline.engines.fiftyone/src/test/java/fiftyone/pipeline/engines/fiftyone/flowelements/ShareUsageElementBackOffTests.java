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
import fiftyone.pipeline.engines.trackers.Tracker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fiftyone.pipeline.core.Constants.EVIDENCE_CLIENTIP_KEY;
import static fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Usage sharing pauses sending after repeated failed sends, keeps queueing
 * evidence while paused, and drains the queue once a send succeeds. These
 * tests use the default settings, which pause sending for two seconds after
 * ten sends fail within 100 seconds.
 */
public class ShareUsageElementBackOffTests {

    /**
     * The longest a test waits for something it expects to happen.
     */
    private static final long WAIT_LIMIT_MILLIS = 30000;

    private static final int DEFAULT_FAILURES = 10;

    private static final long DEFAULT_PAUSE_MILLIS = 2000;

    private static final Pattern CLIENT_IP =
        Pattern.compile("<ClientIP>([^<]*)</ClientIP>");

    /**
     * Answers each send with the status the test sets, and records when each
     * send was answered and the client IPs of the sends it accepted.
     */
    private static class RecordingUploader implements DataUploader {
        private final long start = System.nanoTime();
        private final List<Long> times = new ArrayList<>();
        private final List<String> accepted = new ArrayList<>();
        private volatile int code = 500;
        private ByteArrayOutputStream body;

        @Override
        public synchronized OutputStream getOutputStream() {
            body = new ByteArrayOutputStream();
            return body;
        }

        @Override
        public synchronized int getResponseCode() {
            int answer = code;
            times.add(now());
            if (answer == 200) {
                Matcher matcher = CLIENT_IP.matcher(
                    new String(body.toByteArray(), StandardCharsets.UTF_8));
                while (matcher.find()) {
                    accepted.add(matcher.group(1));
                }
            }
            return answer;
        }

        long now() {
            return (System.nanoTime() - start) / 1000000;
        }

        synchronized int count() {
            return times.size();
        }

        synchronized List<Long> times() {
            return new ArrayList<>(times);
        }

        synchronized List<String> accepted() {
            return new ArrayList<>(accepted);
        }
    }

    /**
     * Shares every request.
     */
    private static class ShareAll implements Tracker {
        @Override
        public boolean track(FlowData flowData) {
            return true;
        }
    }

    private Pipeline pipeline;
    private RecordingUploader uploader;
    private ShareUsageElement element;
    private int requests;

    /**
     * Each entry is sent on its own, as the minimum entries per message is
     * one.
     */
    @BeforeEach
    public void init() throws Exception {
        pipeline = new PipelineBuilder().build();
        uploader = new RecordingUploader();
        requests = 0;
        element = new ShareUsageElement(
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
            DEFAULT_SESSION_COOKIE_NAME,
            new ShareAll());
        element.addPipeline(pipeline);
        element.dataUploader = uploader;
    }

    /**
     * Replaces the element with one that has its own back-off settings. The
     * failures window is long enough that every failure in a test counts.
     */
    private void useBackOff(
        int failuresToEnterRecovery,
        double initialDelaySeconds,
        double maxDelaySeconds,
        int maximumQueueSize) throws Exception {
        element.close();
        element = new ShareUsageElement(
            LoggerFactory.getLogger(ShareUsageElement.class),
            1,
            1,
            maximumQueueSize,
            100,
            100,
            1,
            true,
            "http://localhost/usage",
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            DEFAULT_SESSION_COOKIE_NAME,
            new ShareAll(),
            failuresToEnterRecovery,
            100,
            initialDelaySeconds,
            maxDelaySeconds,
            2);
        element.addPipeline(pipeline);
        element.dataUploader = uploader;
    }

    @AfterEach
    public void cleanUp() throws Exception {
        element.close();
        pipeline.close();
    }

    /**
     * Processes a request whose evidence no other request has, and returns
     * its client IP.
     */
    private String process() throws Exception {
        int number = requests++;
        String ip = "10.0." + (number / 250) + "." + (number % 250 + 1);
        Map<String, Object> evidence = new HashMap<>();
        evidence.put(EVIDENCE_CLIENTIP_KEY, ip);
        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidence);
            element.process(data);
        }
        return ip;
    }

    private static void waitFor(
        BooleanSupplier condition,
        Supplier<String> message) throws InterruptedException {
        long end = System.currentTimeMillis() + WAIT_LIMIT_MILLIS;
        while (condition.getAsBoolean() == false) {
            if (System.currentTimeMillis() > end) {
                fail(message.get());
            }
            Thread.sleep(20);
        }
    }

    /**
     * Processes requests one at a time, waiting for each to be sent, so each
     * is a separate failed send.
     */
    private void failSends(int count) throws Exception {
        for (int i = 1; i <= count; i++) {
            process();
            final int expected = i;
            waitFor(
                () -> uploader.count() == expected &&
                    element.isRunning() == false,
                () -> "Send " + expected + " was not made.");
        }
    }

    /**
     * Ten failed sends pause sending. Nothing is sent during the pause, the
     * evidence processed during it is queued, and all of it is sent once the
     * pause ends and the service answers, with no more requests processed to
     * start the sending.
     */
    @Test
    public void ShareUsageElement_BackOff_PausesThenDrainsQueue()
        throws Exception {
        failSends(DEFAULT_FAILURES);
        long pausedAt = uploader.now();

        List<String> queued = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            queued.add(process());
        }
        Thread.sleep(700);
        assertEquals(DEFAULT_FAILURES, uploader.count(),
            "Sends were made during the pause that ten failed sends should " +
                "have started.");

        uploader.code = 200;
        waitFor(
            () -> uploader.accepted().containsAll(queued),
            () -> "The evidence processed during the pause was not all " +
                "sent once the service answered. Sent: " + uploader.accepted());
        long waited = uploader.times().get(DEFAULT_FAILURES) - pausedAt;
        assertTrue(waited >= DEFAULT_PAUSE_MILLIS - 500,
            "Sending resumed " + waited + " ms after the pause started, " +
                "before the two second pause ended.");
    }

    /**
     * Closing while paused returns at once rather than waiting for the pause
     * to end, and sends nothing.
     */
    @Test
    public void ShareUsageElement_BackOff_CloseWhilePausedDoesNotWaitOrSend()
        throws Exception {
        failSends(DEFAULT_FAILURES);
        process();
        process();

        long start = System.currentTimeMillis();
        element.close();
        long took = System.currentTimeMillis() - start;

        assertTrue(took < 1500,
            "Closing took " + took + " ms, as it waited for the pause.");
        assertEquals(DEFAULT_FAILURES, uploader.count(),
            "A send was made during the pause or when closing.");
    }

    /**
     * Each failed send after a pause multiplies the delay, and the delay
     * stops growing at the maximum.
     */
    @Test
    public void ShareUsageElement_BackOff_DelayGrowsToMaximum()
        throws Exception {
        useBackOff(1, 0.5, 1.0, 100);
        for (int i = 0; i < 40; i++) {
            process();
        }

        waitFor(
            () -> uploader.count() >= 4,
            () -> "Fewer than four sends were made.");
        List<Long> times = uploader.times();
        long first = times.get(1) - times.get(0);
        long second = times.get(2) - times.get(1);
        long third = times.get(3) - times.get(2);
        String shown = first + ", " + second + ", " + third + " ms";
        assertTrue(first >= 400,
            "The first pause was not the initial delay. Gaps: " + shown);
        assertTrue(second >= 900,
            "The second pause was not doubled. Gaps: " + shown);
        assertTrue(third >= 900 && third < 1900,
            "The third pause was not held at the maximum. Gaps: " + shown);
    }

    /**
     * When the queue fills during a pause, new evidence is dropped and what
     * was queued first is sent once sending resumes.
     */
    @Test
    public void ShareUsageElement_BackOff_FullQueueDropsNewEvidence()
        throws Exception {
        useBackOff(1, 2, 2, 4);
        failSends(1);

        List<String> kept = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            kept.add(process());
        }
        List<String> dropped = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            dropped.add(process());
        }
        uploader.code = 200;

        waitFor(
            () -> uploader.accepted().containsAll(kept),
            () -> "The evidence queued during the pause was not all sent. " +
                "Sent: " + uploader.accepted());
        Thread.sleep(500);
        for (String ip : dropped) {
            assertTrue(uploader.accepted().contains(ip) == false,
                "Evidence processed when the queue was full was sent.");
        }
    }
}
