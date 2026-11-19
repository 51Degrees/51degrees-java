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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fiftyone.pipeline.core.Constants.EVIDENCE_CLIENTIP_KEY;
import static fiftyone.pipeline.engines.Constants.DEFAULT_SESSION_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
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
        private volatile String content = "";
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

        public String getResponseContent() {
            return content;
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
    private Logger logger = LoggerFactory.getLogger(ShareUsageElement.class);
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
            logger,
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
            2,
            false,
            false);
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

    /**
     * Evidence discarded because the queue is full is logged, but not with a
     * message for each entry discarded.
     */
    @Test
    public void ShareUsageElement_FullQueue_LogsWithoutAMessagePerEntry()
        throws Exception {
        List<String> warnings = new CopyOnWriteArrayList<>();
        logger = recordingLogger(warnings);
        useBackOff(1, 2, 2, 4);
        failSends(1);
        warnings.clear();

        for (int i = 0; i < 10; i++) {
            process();
        }

        long full = warnings.stream()
            .filter(warning -> warning.contains("full"))
            .count();
        assertTrue(full >= 1, "Discarding entries was not logged.");
        assertTrue(full < 6, "A message was logged for each entry discarded.");
    }

    /**
     * A send answered with anything other than 200 reports the status code
     * and the content of the answer.
     */
    @Test
    public void ShareUsageElement_FailedAnswer_ReportsStatusAndContent() {
        uploader.content = "the service says no";

        Exception failure = assertThrows(
            Exception.class,
            () -> element.sendAsXML(new ArrayList<>()));

        assertTrue(failure.getMessage().contains("500"));
        assertTrue(failure.getMessage().contains("the service says no"));
    }

    /**
     * A logger that records each error, warning and information message it
     * is given as its level followed by its arguments.
     */
    private static Logger levelLogger(List<String> records) {
        return (Logger) Proxy.newProxyInstance(
            Logger.class.getClassLoader(),
            new Class<?>[] {Logger.class},
            (proxy, method, args) -> {
                String name = method.getName();
                if (name.equals("error") ||
                    name.equals("warn") ||
                    name.equals("info")) {
                    records.add(name.toUpperCase(Locale.ROOT) + " " +
                        Arrays.deepToString(args));
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

    /**
     * A refused send is logged at Error with the status code and the
     * content of the answer, and processing goes on, so evidence processed
     * after it is still sent once the service answers.
     */
    @Test
    public void ShareUsageElement_RefusedSend_LoggedAndProcessingGoesOn()
        throws Exception {
        List<String> records = new CopyOnWriteArrayList<>();
        logger = levelLogger(records);
        useBackOff(DEFAULT_FAILURES, 2, 2, 100);
        uploader.content = "the service says no";

        failSends(1);

        assertTrue(records.stream().anyMatch(record ->
                record.startsWith("ERROR") &&
                record.contains("500") &&
                record.contains("the service says no")),
            "The refused send was not logged with its status and content. " +
                "Logged: " + records);
        uploader.code = 200;
        String later = process();
        waitFor(
            () -> uploader.accepted().contains(later),
            () -> "Evidence processed after the refused send was not sent.");
    }

    /**
     * Pausing, and each lengthening of the pause, is logged as a warning
     * that gives the delay, and resuming is logged as information.
     */
    @Test
    public void ShareUsageElement_PauseAndResume_AreLogged() throws Exception {
        List<String> records = new CopyOnWriteArrayList<>();
        logger = levelLogger(records);
        useBackOff(1, 2, 8, 100);

        element.recordSendResult(false);
        element.recordSendResult(false);
        element.recordSendResult(true);

        int paused = indexOf(records, "WARN", "paused for", "2.0]");
        int lengthened = indexOf(records, "WARN", "paused for", "4.0]");
        int resumed = indexOf(records, "INFO", "resumed", "]");
        assertTrue(paused >= 0 && lengthened > paused && resumed > lengthened,
            "The pause, its lengthening and the resumption were not logged " +
                "in turn. Logged: " + records);
    }

    /**
     * Closing while paused discards what is queued and logs how many
     * entries were not sent.
     */
    @Test
    public void ShareUsageElement_CloseWhilePaused_LogsTheNumberNotSent()
        throws Exception {
        List<String> records = new CopyOnWriteArrayList<>();
        logger = levelLogger(records);
        useBackOff(1, 60, 60, 100);
        failSends(1);
        process();
        process();
        process();

        element.close();

        assertTrue(indexOf(records, "", "were not sent", ", 3]") >= 0,
            "The three entries not sent were not counted in the log. " +
                "Logged: " + records);
    }

    /**
     * Processing during a pause neither waits for the pause nor is stopped
     * by it, so the evidence is queued. The pause is two minutes, so
     * processing that waited for it could not finish within the limit.
     */
    @Test
    public void ShareUsageElement_ProcessDuringPause_DoesNotWait()
        throws Exception {
        useBackOff(1, 120, 120, 100);
        failSends(1);

        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            for (int i = 0; i < 5; i++) {
                process();
            }
        });

        assertEquals(1, uploader.count(), "A send was made during the pause.");
        assertEquals(5, element.evidenceCollection.size(),
            "The evidence processed during the pause was not queued.");
    }

    /**
     * The index of the first record at the level that holds the text and
     * ends with the given end, or -1 if there is none.
     */
    private static int indexOf(
        List<String> records,
        String level,
        String text,
        String end) {
        for (int i = 0; i < records.size(); i++) {
            String record = records.get(i);
            if (record.startsWith(level) &&
                record.contains(text) &&
                record.endsWith(end)) {
                return i;
            }
        }
        return -1;
    }
}
