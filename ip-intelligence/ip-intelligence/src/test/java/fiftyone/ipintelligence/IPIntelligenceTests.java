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

package fiftyone.ipintelligence;

import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.ipintelligence.shared.testhelpers.IpAddressGenerator;
import fiftyone.ipintelligence.shared.testhelpers.FileUtils;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.services.DataUpdateService;
import fiftyone.pipeline.engines.services.HttpClientDefault;

import fiftyone.pipeline.util.FileFinder;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static fiftyone.ipintelligence.shared.testhelpers.FileUtils.IP_ADDRESSES_FILE_NAME;
import static fiftyone.pipeline.engines.Constants.PerformanceProfiles.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IPIntelligenceTests {

    private static final Logger logger = LoggerFactory.getLogger(IPIntelligenceTests.class);

    private static final String IPI_DATA_FILE_NAME = FileUtils.getHashFileName();

    private static IpAddressGenerator ipAddresses;
    TestConfig[] ipiConfigs = {
        // ******** IPI with a single thread *********
        new TestConfig(IPI_DATA_FILE_NAME, MaxPerformance, false, false, "IPI-MaxPerformance-NoCache-SingleThread"),
        new TestConfig(IPI_DATA_FILE_NAME, MaxPerformance, true, false, "IPI-MaxPerformance-Cache-SingleThread"),
        // ******** IPI with multiple threads *********
        new TestConfig(IPI_DATA_FILE_NAME, MaxPerformance, false, true, "IPI-MaxPerformance-NoCache-MultiThread"),
        new TestConfig(IPI_DATA_FILE_NAME, MaxPerformance, true, true, "IPI-MaxPerformance-Cache-MultiThread"),
    };

    @BeforeClass
    public static void initClass() throws IOException {
        ipAddresses = new IpAddressGenerator(
            FileFinder.getFilePath(IP_ADDRESSES_FILE_NAME));
    }

    private static String reportErrors(Collection<FlowError> errors) {
        StringBuilder result = new StringBuilder();
        for (FlowError error : errors) {
            result.append("Error in element '" +
                error.getFlowElement().getClass().getSimpleName() + "'\n");
            AddExceptionToMessage(result, error.getThrowable(), 0);
        }
        return result.toString();
    }

    private static void AddExceptionToMessage(
        StringBuilder message,
        Throwable ex,
        int depth) {
        AddToMessage(message, ex.getClass().getSimpleName() + " - " + ex.getMessage(), depth);
        AddToMessage(message, ex.getStackTrace().toString(), depth);
        if (ex.getSuppressed() != null) {
            AddExceptionToMessage(message, ex.getSuppressed()[0], depth++);
        }
    }

    private static void AddToMessage(StringBuilder message, String textToAdd, int depth) {
        for (int i = 0; i < depth; i++) {
            message.append("   ");
        }
        message.append(textToAdd + "\n");
    }

    @Test
    public void IPI_AllConfigurations_100_IpAddresses() throws Exception {
        for (TestConfig config : ipiConfigs) {
            logger.info("Testing '" + config.name + "'");
            TestOnPremise_AllConfigurations_100_IpAddresses(
                config.dataFileName,
                config.performanceProfile,
                config.useCache,
                config.multiThreaded);
        }
    }

    public void TestOnPremise_AllConfigurations_100_IpAddresses(
        String datafileName,
        Constants.PerformanceProfiles performanceProfile,
        boolean useCache,
        boolean multiThreaded) throws Exception {
        File datafile = FileFinder.getFilePath(datafileName);

        // Configure the pipeline builder based on the
        // parameters passed to this method.
        ILoggerFactory loggerFactory = mock(ILoggerFactory.class);
        Logger logger = mock(Logger.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);
        IPIntelligenceOnPremisePipelineBuilder builder =
            new IPIntelligencePipelineBuilder(loggerFactory)
                .useOnPremise(datafile.getAbsolutePath(), false)
                .setPerformanceProfile(performanceProfile)
                .setShareUsage(false)
                .setAutoUpdate(false);
        if (useCache) {
            builder.useResultsCache();
        }

        try (final Pipeline pipeline = builder.build()) {
            int threadCount = multiThreaded ? 8 : 1;
            ExecutorService service = Executors.newFixedThreadPool(threadCount);
            List<Callable<Void>> callables = new ArrayList<>(threadCount);
            for (int i = 0; i < threadCount; i++) {
                callables.add(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        for (String ipAddress : ipAddresses.getRandomIpAddresses(100)) {
                            try (FlowData flowData = pipeline.createFlowData()) {
                                flowData.addEvidence(
                                    "server.client-ip",
                                    ipAddress)
                                    .process();
                                if (flowData.getErrors() != null) {
                                    assertEquals(
                                        "Expected no errors but got " +
                                            flowData.getErrors().size() + "\n" +
                                            reportErrors(flowData.getErrors()),
                                        0,
                                        flowData.getErrors().size());
                                }
                                IPIntelligenceData ipiData = flowData.get(IPIntelligenceData.class);
                                ipiData.getRegisteredName();
                            }
                        }
                        return null;
                    }
                });
            }

            List<Future<Void>> futures = service.invokeAll(callables);

            for (Future<Void> future : futures) {
                future.get();
            }
        }
    }
    
    /**
     * This tests that the default constructor of the
     * IPIntelligencePipelineBuilder does pass the DataUpdateService to the
     * resulting pipeline.
     * @throws Exception
     */
    @Test
    public void TestOnPremiseBuilder_DataUpdateService_Default() throws Exception {
    	// Configure the pipeline builder based on the
        // parameters passed to this method.
        ILoggerFactory loggerFactory = mock(ILoggerFactory.class);
        Logger logger = mock(Logger.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);
    	IPIntelligenceOnPremisePipelineBuilder builder =
                new IPIntelligencePipelineBuilder(loggerFactory,
                		new HttpClientDefault())
                    .useOnPremise(IPI_DATA_FILE_NAME, false)
                    .setPerformanceProfile(MaxPerformance)
                    .setShareUsage(false)
                    .setAutoUpdate(false);
    	try (Pipeline pipeline = builder.build()) {
    		assertEquals(1, pipeline.getServices().size());
    	}
    }

    @Test
    public void TestOnPremiseBuilder_CreateTempFile_LowMemory() throws Exception {
        TestOnPremiseBuilder_CreateTempFile(LowMemory);
    }

    @Test
    public void TestOnPremiseBuilder_CreateTempFile_MaxPerformance() throws Exception {
        TestOnPremiseBuilder_CreateTempFile(MaxPerformance);
    }

    /**
     * This tests that creating a temp file works
     */
    public void TestOnPremiseBuilder_CreateTempFile(
        Constants.PerformanceProfiles performanceProfile) throws Exception {
        int i;
        for (i=0; i < 10; i++) {
            IPIntelligenceOnPremisePipelineBuilder builder =
                    new IPIntelligencePipelineBuilder()
                            .useOnPremise(IPI_DATA_FILE_NAME, true)
                            .setPerformanceProfile(performanceProfile)
                            .setShareUsage(false)
                            .setAutoUpdate(false);
            try (Pipeline pipeline = builder.build()) {
                assertEquals(1, pipeline.getServices().size());
                IPIntelligenceOnPremiseEngine engine =
                        pipeline.getElement(IPIntelligenceOnPremiseEngine.class);
                String tempDir = engine.getTempDataDirPath();
                Path tempPath = Paths.get(tempDir);
                assertTrue("A temp data file should have been created under " + tempPath,
                        Files.exists(tempPath));
            }
        }
        logger.info("Completed {} creations", i);
    }
        /**
         * This tests that DataUpdateService is closed when its corresponding
         * pipeline created by IPIntelligencePipelineBuilder is closed.
         * @throws Exception
         */
    @Test
    @Ignore
    public void TestOnPremiseBuilder_DataUpdateService_Close() throws Exception {
    	// Configure the pipeline builder based on the
        // parameters passed to this method.
        ILoggerFactory loggerFactory = mock(ILoggerFactory.class);
        Logger logger = mock(Logger.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);
        DataUpdateService updateService = mock(DataUpdateService.class);
    	IPIntelligenceOnPremisePipelineBuilder builder =
                new IPIntelligencePipelineBuilder(loggerFactory,
                		new HttpClientDefault(), updateService)
                    .useOnPremise(IPI_DATA_FILE_NAME, false)
                    .setPerformanceProfile(MaxPerformance)
                    .setShareUsage(false)
                    .setAutoUpdate(true);
    	try (Pipeline pipeline = builder.build()) {
    		assertEquals(1, pipeline.getServices().size());
    	}
    	verify(updateService, times(1)).close();
    }

    private class TestConfig {
        final String dataFileName;
        final Constants.PerformanceProfiles performanceProfile;
        final boolean useCache;
        final boolean multiThreaded;
        final String name;
        TestConfig(
            String dataFileName,
            Constants.PerformanceProfiles performanceProfile,
            boolean useCache,
            boolean multiThreaded,
            String name) {
            this.dataFileName = dataFileName;
            this.performanceProfile = performanceProfile;
            this.useCache = useCache;
            this.multiThreaded = multiThreaded;
            this.name = name;
        }
    }
}
