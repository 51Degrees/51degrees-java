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

package fiftyone.pipeline.engines.fiftyone.performance;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageElement;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.DataUploader;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngineBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Times Process calls through the usage sharing element. Every call in a
 * test carries the same Evidence, so after the first call the tracker turns
 * each one away as a repeat and nothing more is queued. The measured rounds
 * time that path, which finds the values to share and checks the tracker.
 */
public class ShareUsageOverheadTests {
    private Pipeline pipeline;

    private static final Map<String, Double> overheadMillis = new LinkedHashMap<>();

    /**
     * Writes the measured overheads to the results file, if one was asked for.
     */
    @AfterAll
    public static void writeResults() throws IOException {
        String path = System.getProperty("fiftyone.performance.json");
        if (path == null) {
            return;
        }
        JSONObject results = new JSONObject()
            .put("LowerIsBetter", new JSONObject(overheadMillis));
        Files.write(Paths.get(path), results.toString(2).getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void Initialise() throws Exception {

        HttpClient httpClient = mock(HttpClient.class);
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(httpClient.connect(any(URL.class))).thenReturn(connection);
        when(httpClient.postData(any(HttpURLConnection.class), any(Map.class), any(byte[].class)))
            .thenReturn(null);
        when(httpClient.getResponseString(any(HttpURLConnection.class))).thenReturn(null);
        when(connection.getResponseCode()).thenReturn(200);
        when(connection.getResponseMessage()).thenReturn("");

        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine =
                new EmptyEngineBuilder(loggerFactory)
                .setProcessCost(0)
                .build();

        // Closing the element sends what is still queued, so it is given an
        // uploader that sends nothing anywhere.
        ShareUsageElement shareUsage =
            new ShareUsageBuilder(loggerFactory, mock(DataUploader.class))
            .build();

        pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(engine)
            .addFlowElement(shareUsage)
            .setAutoCloseElements(true)
            .build();
    }

    @AfterEach
    public void cleanUp() throws Exception {
        pipeline.close();
    }

    private static final String USER_AGENT =
        "Mozilla/5.0 (iPad; U; CPU OS 3_2_1 like Mac OS X; en-us) " +
        "AppleWebKit/531.21.10 (KHTML, like Gecko) Mobile/7B405";

    /**
     * Rounds measured, and the number discarded first so that the JIT has
     * compiled the code. The median of the rounds is checked, so one round
     * slowed by a busy runner or a garbage collection does not fail a test,
     * whilst a cost on every call does.
     */
    private static final int MeasuredRounds = 7;
    private static final int WarmUpRounds = 2;

    /**
     * The most the median Process call may take. Each limit is ten times the
     * highest median measured on any CI runner, so a runner several times
     * slower than usual still passes.
     */
    private static final double SingleEvidenceLimitMillis = 0.06;
    private static final double ThousandEvidenceLimitMillis = 30;

    /**
     * Returns the milliseconds per Process call, for one round of calls with
     * the evidence given. The FlowData are all processed before any is closed,
     * so that closing them is not part of the time measured. They are closed
     * together in a finally block rather than one by one with
     * try-with-resources.
     */
    @SuppressWarnings("PMD.FlowDataInTryWithResources")
    private double measureRound(int iterations, int evidenceCount)
            throws Exception {
        List<FlowData> data = new ArrayList<>();
        long start;
        long end;
        try {
            for (int i = 0; i < iterations; i++) {
                FlowData flowData = pipeline.createFlowData();
                data.add(flowData);
                for (int j = 0; j < evidenceCount; j++) {
                    flowData.addEvidence("header." + j, j);
                }
                flowData.addEvidence("header.user-agent", USER_AGENT);
            }
            start = System.nanoTime();
            for (FlowData entry : data) {
                entry.process();
            }
            end = System.nanoTime();
        } finally {
            closeAll(data);
        }
        return (end - start) / 1_000_000.0 / iterations;
    }

    /**
     * Returns the milliseconds per Process call of each measured round,
     * lowest first, after the warm up rounds.
     */
    private double[] measureRounds(int iterations, int evidenceCount)
            throws Exception {
        for (int i = 0; i < WarmUpRounds; i++) {
            measureRound(iterations, evidenceCount);
        }
        double[] rounds = new double[MeasuredRounds];
        for (int i = 0; i < rounds.length; i++) {
            rounds[i] = measureRound(iterations, evidenceCount);
        }
        Arrays.sort(rounds);
        return rounds;
    }

    /**
     * Fails when the median round is not below the limit. The message lists
     * every round, because the build log shows only the message, and the
     * rounds tell a runner slow for the whole test from a slow round or two.
     */
    private static void assertBelowLimit(double[] rounds, double limitMillis) {
        double median = rounds[rounds.length / 2];
        Assertions.assertTrue(median < limitMillis,
            "Pipeline with share usage overhead per Process call was " +
            median + "ms. Maximum permitted is " + limitMillis +
            ". Rounds in ms per call " + Arrays.toString(rounds));
    }

    @Test
    public void ShareUsageOverhead_RepeatedSingleEvidence() throws Exception {
        int iterations = 10_000;

        double[] rounds = measureRounds(iterations, 0);
        overheadMillis.put("ShareUsageOverhead_SingleEvidence_ms",
            rounds[rounds.length / 2]);
        assertBelowLimit(rounds, SingleEvidenceLimitMillis);
    }

    @Test
    public void ShareUsageOverhead_RepeatedThousandEvidence() throws Exception {
        int iterations = 250;
        int evidenceCount = 1_000;

        double[] rounds = measureRounds(iterations, evidenceCount);
        overheadMillis.put("ShareUsageOverhead_HundredEvidence_ms",
            rounds[rounds.length / 2]);
        assertBelowLimit(rounds, ThousandEvidenceLimitMillis);
    }

    private static void closeAll(List<FlowData> data) throws Exception {
        for (FlowData flowData : data) {
            flowData.close();
        }
    }
}
