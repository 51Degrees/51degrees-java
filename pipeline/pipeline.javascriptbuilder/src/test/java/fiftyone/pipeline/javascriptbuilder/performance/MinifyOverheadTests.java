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

package fiftyone.pipeline.javascriptbuilder.performance;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptMinifier;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Performance tests MUST be in place to verify the performance impact of
 * this feature." (pipeline-specification/pipeline-elements/javascript-builder.md
 * line 109) The feature is the minification of the script. These tests time
 * the minification of a rendered script, and Process calls through the
 * JavaScript Builder with minification on and off.
 */
public class MinifyOverheadTests {

    private static final Map<String, Double> overheadMillis =
        new LinkedHashMap<>();

    /**
     * Writes the measured times to the results file, if one was asked for.
     */
    @AfterAll
    public static void writeResults() throws IOException {
        String path = System.getProperty("fiftyone.performance.json");
        if (path == null) {
            return;
        }
        JSONObject results = new JSONObject()
            .put("LowerIsBetter", new JSONObject(overheadMillis));
        Files.write(Paths.get(path),
            results.toString(2).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Rounds measured, and the number discarded first so that the JIT has
     * compiled the code. The median of the rounds is checked, so one round
     * slowed by a busy runner or a garbage collection does not fail a test,
     * whilst a cost on every call does.
     */
    private static final int MeasuredRounds = 7;
    private static final int WarmUpRounds = 2;

    /**
     * The most the median minification of one script may take. Two runs on
     * a busy Windows 11 x64 machine with JDK 21 measured medians of 0.20 ms
     * and 0.34 ms, so this is about ten times the slower and a runner
     * several times slower than that machine still passes.
     */
    private static final double MinifyLimitMillis = 3;

    /**
     * The most the median Process call through the JavaScript Builder with
     * minification on may take more than one with it off. The same two runs
     * measured 0.31 ms and 0.53 ms, with Process calls of 0.11 ms and 0.22 ms
     * with minification off, so this is about ten times the slower.
     */
    private static final double ProcessOverheadLimitMillis = 5;

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    private Pipeline buildPipeline(JavaScriptBuilderElement jsElement)
        throws Exception {
        return new PipelineBuilder(loggerFactory)
            .addFlowElement(new SequenceElementBuilder(loggerFactory).build())
            .addFlowElement(new JsonBuilderElementBuilder(loggerFactory).build())
            .addFlowElement(jsElement)
            .build();
    }

    private JavaScriptBuilderElement buildElement(boolean minify) {
        return new JavaScriptBuilderElementBuilder(loggerFactory)
            .setEndpoint("/json")
            .setHost("example.com")
            .setProtocol("https")
            .setMinify(minify)
            .build();
    }

    /**
     * The full script as the element renders it with minification off.
     */
    private String renderFull() throws Exception {
        JavaScriptBuilderElement jsElement = buildElement(false);
        try (Pipeline pipeline = buildPipeline(jsElement);
             FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence("query.user-agent", "iPhone");
            flowData.process();
            return flowData.getFromElement(jsElement).getJavaScript();
        }
    }

    private static double median(double[] rounds) {
        double[] sorted = rounds.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    /**
     * Fails when the median round is not below the limit. The message lists
     * every round, because the build log shows only the message, and the
     * rounds tell a runner slow for the whole test from a slow round or two.
     */
    private static void assertBelowLimit(
        String what,
        double[] rounds,
        double limitMillis) {
        double median = median(rounds);
        Assertions.assertTrue(median < limitMillis,
            what + " was " + median + "ms. Maximum permitted is " +
            limitMillis + ". Rounds in ms " + Arrays.toString(rounds));
    }

    private static double minifyRound(String script, int iterations) {
        long start = System.nanoTime();
        int length = 0;
        for (int i = 0; i < iterations; i++) {
            length += JavaScriptMinifier.minify(script).length();
        }
        long end = System.nanoTime();
        Assertions.assertTrue(length > 0);
        return (end - start) / 1_000_000.0 / iterations;
    }

    @Test
    public void MinifyOverhead_PerScript() throws Exception {
        String script = renderFull();
        int iterations = 500;
        for (int i = 0; i < WarmUpRounds; i++) {
            minifyRound(script, iterations);
        }
        double[] rounds = new double[MeasuredRounds];
        for (int i = 0; i < rounds.length; i++) {
            rounds[i] = minifyRound(script, iterations);
        }
        overheadMillis.put("MinifyOverhead_PerScript_ms", median(rounds));
        assertBelowLimit("Minifying one script", rounds, MinifyLimitMillis);
    }

    /**
     * Returns the milliseconds per Process call, for one round. The FlowData
     * are all made before the timing starts and closed after it ends.
     */
    @SuppressWarnings("PMD.FlowDataInTryWithResources")
    private static double processRound(Pipeline pipeline, int iterations)
        throws Exception {
        List<FlowData> data = new ArrayList<>();
        long start;
        long end;
        try {
            for (int i = 0; i < iterations; i++) {
                FlowData flowData = pipeline.createFlowData();
                data.add(flowData);
                flowData.addEvidence("query.user-agent", "iPhone");
            }
            start = System.nanoTime();
            for (FlowData flowData : data) {
                flowData.process();
            }
            end = System.nanoTime();
        } finally {
            for (FlowData flowData : data) {
                flowData.close();
            }
        }
        return (end - start) / 1_000_000.0 / iterations;
    }

    /**
     * Times Process calls with minification on and off in turn, round by
     * round, so that a runner that slows down part way through slows both
     * alike. The difference of each pair of rounds is the overhead.
     */
    @Test
    public void MinifyOverhead_PerProcessCall() throws Exception {
        int iterations = 200;
        try (Pipeline minified = buildPipeline(buildElement(true));
             Pipeline full = buildPipeline(buildElement(false))) {
            for (int i = 0; i < WarmUpRounds; i++) {
                processRound(minified, iterations);
                processRound(full, iterations);
            }
            double[] on = new double[MeasuredRounds];
            double[] off = new double[MeasuredRounds];
            double[] overhead = new double[MeasuredRounds];
            for (int i = 0; i < MeasuredRounds; i++) {
                on[i] = processRound(minified, iterations);
                off[i] = processRound(full, iterations);
                overhead[i] = on[i] - off[i];
            }
            overheadMillis.put("Process_MinifyOn_ms", median(on));
            overheadMillis.put("Process_MinifyOff_ms", median(off));
            overheadMillis.put("MinifyOverhead_PerProcessCall_ms",
                median(overhead));
            assertBelowLimit("Minification overhead per Process call " +
                "(on " + Arrays.toString(on) + ", off " +
                Arrays.toString(off) + ")",
                overhead, ProcessOverheadLimitMillis);
        }
    }
}
