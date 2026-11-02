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

/*!
 * @example console/PerformanceBenchmark.java
 *
 * This example demonstrates how to run a performance benchmark using the IP Intelligence
 * On-premise API to see how fast IP address lookups can be performed.
 *
 * The example will process a list of IP addresses and output performance metrics including
 * lookup rate and processing time per IP address.
 *
 * This can help you optimize your IP Intelligence configuration for your specific use case
 * and understand the performance characteristics of different settings.
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/ip-intelligence-java-examples/blob/main/console/src/main/java/fiftyone/ipintelligence/examples/console/PerformanceBenchmark.java).
 *
 * This example requires an enterprise IP Intelligence data file (.ipi).
 * To obtain an enterprise data file for testing, please [contact us](https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-performancebenchmark.java&utm_term=header).
 *
 * Required Maven Dependencies:
 * - [com.51degrees:ip-intelligence](https://central.sonatype.com/artifact/com.51degrees/ip-intelligence)
 */

package fiftyone.ipintelligence.examples.console;

import fiftyone.common.testhelpers.LogbackHelper;
import fiftyone.ipintelligence.IPIntelligenceOnPremisePipelineBuilder;
import fiftyone.ipintelligence.IPIntelligencePipelineBuilder;
import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import fiftyone.ipintelligence.examples.shared.EvidenceHelper;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.util.FileFinder;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

import static fiftyone.ipintelligence.examples.shared.DataFileHelper.getDataFileLocation;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.getEvidenceFile;
import static fiftyone.pipeline.engines.Constants.PerformanceProfiles.*;

/**
 * The example illustrates the flexibility with which the 51Degrees pipeline can be configured
 * to achieve a range of outcomes relating to speed, accuracy and memory usage.
 * <p>
 */
public class PerformanceBenchmark {
    // the default number of threads if one is not provided.
    public static final int DEFAULT_NUMBER_OF_THREADS = 4;
    // the number of tests to execute.
    public static final int TESTS_PER_THREAD = 10000;

    public static final Logger logger = LoggerFactory.getLogger(PerformanceBenchmark.class);

    // where the results of the tests are gathered
    private List<Future<BenchmarkResult>> resultList;
    private int numberOfThreads = DEFAULT_NUMBER_OF_THREADS;
    private List<Map<String, String>> evidence;
    private String dataFileLocation;
    private PrintWriter writer;

    // a default set of configurations: (profile, allProperties)
    public static PerformanceConfiguration [] DEFAULT_PERFORMANCE_CONFIGURATIONS = {
            new PerformanceConfiguration(MaxPerformance, false),
            new PerformanceConfiguration(MaxPerformance, true)
    };


    public static void main(String[] args) throws Exception {
        LogbackHelper.configureLogback(FileFinder.getFilePath("logback.xml"));

        String dataFilename = args.length > 0 ? args[0] : null;
        String evidenceFilename = args.length > 1 ? args[1] : null;
        int numberOfThreads = DEFAULT_NUMBER_OF_THREADS;
        if (args.length > 2) {
            numberOfThreads = Integer.parseInt(args[2]);
        }

        new PerformanceBenchmark().runBenchmarks(DEFAULT_PERFORMANCE_CONFIGURATIONS,
                dataFilename,
                evidenceFilename,
                numberOfThreads,
                new PrintWriter(System.out,true));
    }

    /**
     * Runs benchmarks for various configurations.
     *
     * @param dataFilename     path to the 51Degrees IP Intelligence data file for testing
     * @param evidenceFilename path to a text file of evidence
     * @param numberOfThreads  number of concurrent threads
     * @throws Exception as a catch all
     */
    protected void runBenchmarks(PerformanceConfiguration[] performanceConfigurations,
                                 String dataFilename,
                                 String evidenceFilename,
                                 int numberOfThreads,
                                 PrintWriter writer) throws Exception {

        logger.info("Running Performance example");

        this.dataFileLocation = getDataFileLocation(dataFilename);

        File evidenceFile = getEvidenceFile(evidenceFilename);
        this.evidence = Collections.unmodifiableList(
                EvidenceHelper.getEvidenceList(evidenceFile, 20000));
        this.numberOfThreads = numberOfThreads;
        this.writer = writer;

        // run "from memory" benchmarks - the only profiles that really make sense
        // are maxPerformance
        for (PerformanceConfiguration config: performanceConfigurations){
            if (!config.profile.equals(MaxPerformance)) {
                // TODO: Remove this check
                continue;
            }
            executeBenchmark(config);
        }

        logger.info("Finished Performance example");
    }

    /**
     * Set up and execute a benchmark test
     * @param config the configuration to use for this benchmark
     * @throws Exception to satisfy undelying calls
     */
    private void executeBenchmark(PerformanceConfiguration config) throws Exception {
        logger.info(MarkerFactory.getMarker(config.profile.name() + " " +
                        config.allProperties),
                "Benchmarking with profile: {} AllProperties: {}",
                config.profile,
                config.allProperties);

        Pipeline pipeline = null;
        try {
            logger.info("Load from disk");
            IPIntelligenceOnPremisePipelineBuilder builder = new IPIntelligencePipelineBuilder()
                    // load from disk
                    .useOnPremise(dataFileLocation, false);

            setPipelinePerformanceProperties(builder, config);
            pipeline = builder.build();

            DataFileHelper.logDataFileInfo(pipeline.getElement(IPIntelligenceOnPremiseEngine.class));

            // run the benchmarks twice, once to warm up the JVM
            logger.info("Warming up");
            runTests(pipeline);
            System.gc();
            Thread.sleep(300);

            logger.info("Running");
            long executionTime = runTests(pipeline);
            logger.info("Finished - Execution time was {} ms", executionTime);
        } finally {
            if (Objects.nonNull(pipeline)) {
                pipeline.close();
            }
        }
        doReport();
    }

    /**
     * Helper to set the critical performance settings of the pipeline, shared between memory and
     * disk data source pipeline creation, to ensure consistency.
     * <p>
         * @param builder          the builder to configure
     * @param config benchmark configuration
     */
    private void setPipelinePerformanceProperties(
            IPIntelligenceOnPremisePipelineBuilder builder,
            PerformanceConfiguration config) {
        // the different profiles provide for trading off memory usage
        builder.setPerformanceProfile(config.profile)
        // set this to false for testing
        .setAutoUpdate(false)
        // set this to false for testing
        .setShareUsage(false)
        // hint for cache concurrency
        .setConcurrency(numberOfThreads);
        // performance is improved by selecting only the properties you intend to use
        // Requesting properties from a single component
        // reduces processing time compared with requesting properties from multiple components.
        // If you don't specify any properties, then all properties are returned,
        // here we choose "all properties" by specifying none, or just "RegisteredName"
        if (BooleanUtils.isFalse(config.allProperties)) {
            builder.setProperty("RegisteredName");
        }
    }

    /**
     * Report per thread and overall lookup performance
     * @throws Exception to satisfy needs of called APIs
     */
    private void doReport() throws Exception {
        long totalMillis = 0;
        long totalChecks = 0;
        int checksum = 0;
        for (Future<BenchmarkResult> result : resultList) {
            BenchmarkResult bmr = result.get();

            writer.format("Thread:  %,d detections, elapsed %f seconds, %,d Detections per second%n",
                    bmr.count,
                    bmr.elapsedMillis/1000.0,
                    (Math.round(1000.0 * bmr.count/ bmr.elapsedMillis)));

            totalMillis += bmr.elapsedMillis;
            totalChecks += bmr.count;
            checksum += bmr.checkSum;
        }

        // output the results from the benchmark to the console
        double millisPerTest = ((double) totalMillis / (numberOfThreads * totalChecks));
        writer.format("Overall: %,d detections, Average millisecs per detection: %f, Detections per second: %,d\n",
                totalChecks, millisPerTest, Math.round(1000.0/millisPerTest));
        writer.format("Overall: Concurrent threads: %d, Checksum: %x \n", numberOfThreads, checksum);
        writer.println();
    }

    /**
     * Execute lookups on specified number of threads
     * @param pipeline the pipeline to use
     * @return elapsed millis
     * @throws Exception to satisfy called APIs
     */
    private long runTests(Pipeline pipeline) throws Exception {

        // create a list of callables
        List<Callable<BenchmarkResult>> callables = new ArrayList<>();
        for (int i = 0; i < numberOfThreads; i++) {
            callables.add(new BenchmarkRunnable(pipeline, evidence));
        }
        // start multiple threads in a fixed pool
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        long start = System.currentTimeMillis();
        // start all the threads
        resultList = service.invokeAll(callables);
        // wait for all the threads to complete
        for (Future<BenchmarkResult> result : resultList) {
            result.get();
        }
        long duration = System.currentTimeMillis() - start;
        service.shutdown();
        return duration;
    }


    /**
     * Callable that implements the logic of the test for each thread
     */
    private static class BenchmarkRunnable implements Callable<BenchmarkResult> {

        // the benchmark that is being executed
        private final BenchmarkResult result;
        private final List<Map<String, String>> testList;
        private final Pipeline pipeline;

        BenchmarkRunnable(Pipeline pipeline, List<Map<String, String>> evidence) {
            this.testList = evidence;
            // initialise the benchmark variables
            this.pipeline = pipeline;
            this.result = new BenchmarkResult();

            result.elapsedMillis = 0;
            result.count = 0;
            result.checkSum = 0;
        }


        @Override
        public BenchmarkResult call() {
            result.checkSum = 0;
            long start = System.currentTimeMillis();
            for (Map<String, String> evidence : testList) {
                // the benchmark is for processing time only

                // A try-with-resource block MUST be used for the
                // FlowData instance. This ensures that native resources
                // created by the IP Intelligence engine are freed.
                try (FlowData flowData = pipeline.createFlowData()) {
                    flowData
                            .addEvidence(evidence)
                            .process();

                    // Calculate a checksum to compare different runs on
                    // the same data.
                    IPIntelligenceData ipData = flowData.get(IPIntelligenceData.class);
                    if (ipData != null) {
                        if (ipData.getRegisteredName().hasValue()) {
                            String value = ipData.getRegisteredName().getValue();
                            if (value != null) {
                                result.checkSum += value.hashCode();
//                                for (IWeightedValue<?> weightedValue : value) {
//                                    result.checkSum += weightedValue.getValue().hashCode();
//                                }
                            }
                        }
                    }
                    result.count++;
                    if (result.count >= TESTS_PER_THREAD) {
                        break;
                    }
                } catch (Exception e) {
                    logger.error("Exception getting flow data", e);
                }
            }
            result.elapsedMillis += System.currentTimeMillis() - start;
            return result;
        }
    }


    static class BenchmarkResult {

        // number of IP evidence processed to determine the result.
        private long count;

        // processing time in millis this thread
        private long elapsedMillis;

        // used to ensure compiler optimiser doesn't optimise out the very
        // method that the benchmark is testing.
        private int checkSum;


    }

    public static class PerformanceConfiguration {
        Constants.PerformanceProfiles profile;
        boolean allProperties;

        public PerformanceConfiguration(Constants.PerformanceProfiles profile,
                                        boolean allProperties) {
            this.profile = profile;
            this.allProperties = allProperties;
        }
    }
}

