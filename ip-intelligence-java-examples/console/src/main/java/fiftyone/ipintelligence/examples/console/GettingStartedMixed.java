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
 * @example console/GettingStartedMixed.java
 *
 * This example shows how to use both 51Degrees Device Detection and IP Intelligence
 * engines in a single pipeline to determine device properties and location/network
 * details simultaneously.
 *
 * You will learn:
 *
 * 1. How to create a Pipeline that uses both Device Detection and IP Intelligence engines
 * 2. How to pass combined evidence (User-Agent and IP address) to the Pipeline
 * 3. How to retrieve results from both engines
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/ip-intelligence-java-examples/blob/main/console/src/main/java/fiftyone/ipintelligence/examples/console/GettingStartedMixed.java).
 *
 * This example requires:
 * - A Device Detection data file (.hash format)
 * - An IP Intelligence data file (.ipi format)
 *
 * Required Maven Dependencies:
 * - [com.51degrees:ip-intelligence](https://central.sonatype.com/artifact/com.51degrees/ip-intelligence)
 * - [com.51degrees:device-detection](https://central.sonatype.com/artifact/com.51degrees/device-detection)
 */

package fiftyone.ipintelligence.examples.console;

import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.configuration.PipelineOptionsFactory;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.fiftyone.flowelements.FiftyOnePipelineBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.*;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * Demonstrates using both Device Detection and IP Intelligence engines in a single pipeline.
 * Both engines process the same evidence in parallel, providing comprehensive device and
 * location information from a single pipeline invocation.
 * <p>
 * This example uses a pipeline configuration file that configures both engines.
 * The configuration file is <code>src/main/resources/gettingStartedMixed.xml</code>.
 */
public class GettingStartedMixed {
    private static final Logger logger = LoggerFactory.getLogger(GettingStartedMixed.class);

    public static final String DD_DATA_FILE_DEFAULT = "device-detection-data/51Degrees-LiteV4.1.hash";
    public static final String IPI_DATA_FILE_DEFAULT = DataFileHelper.ENTERPRISE_DATA_FILE_REL_PATH;

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));

        // Accept two command line arguments: device detection data file and IP intelligence data file
        String ddDataFile = args.length > 0 ? args[0] : DD_DATA_FILE_DEFAULT;
        String ipiDataFile = args.length > 1 ? args[1] : IPI_DATA_FILE_DEFAULT;

        List<Map<String, String>> evidence = setUpCombinedEvidence();
        run(ddDataFile, ipiDataFile, evidence, System.out);
    }

    /**
     * Run the example
     * @param ddDataFile path to a Device Detection data file (.hash)
     * @param ipiDataFile path to an IP Intelligence data file (.ipi)
     * @param evidenceList combined evidence with User-Agent and IP address
     * @param outputStream somewhere for the results
     */
    public static void run(String ddDataFile,
                           String ipiDataFile,
                           List<Map<String, String>> evidenceList,
                           OutputStream outputStream) throws Exception {
        logger.info("Running GettingStartedMixed example");

        // Resolve and set the Device Detection data file location
        try {
            String ddLocation = resolveDataFile(ddDataFile);
            System.setProperty("TestDeviceDetectionDataFile", ddLocation);
        } catch (Exception e) {
            logger.error("Failed to find Device Detection data file at '{}'. " +
                    "Please provide a valid path to a Device Detection data file (.hash).", ddDataFile);
            throw e;
        }

        // Resolve and set the IP Intelligence data file location
        try {
            String ipiLocation = DataFileHelper.getDataFileLocation(ipiDataFile);
            System.setProperty("TestDataFile", ipiLocation);
        } catch (Exception e) {
            logger.error("Failed to find IP Intelligence data file at '{}'. " +
                    "Please provide a valid path to an IP Intelligence data file (.ipi). " +
                    "An explicit path can be supplied via the {} environment variable.",
                    ipiDataFile, DataFileHelper.IPI_PATH_ENV_VAR);
            throw e;
        }

        // Load the pipeline configuration that includes both engines
        File optionsFile = getFilePath("gettingStartedMixed.xml");
        PipelineOptions pipelineOptions = PipelineOptionsFactory.getOptionsFromFile(optionsFile);

        try (Pipeline pipeline = new FiftyOnePipelineBuilder()
                .buildFromConfiguration(pipelineOptions)) {

            // Process each set of evidence
            for (Map<String, String> evidence : evidenceList) {
                analyzeEvidence(evidence, pipeline, outputStream);
            }

            // Log data file info for both engines
            DeviceDetectionHashEngine ddEngine = pipeline.getElement(DeviceDetectionHashEngine.class);
            if (ddEngine != null) {
                logger.info("Device Detection data file: '{}', tier: '{}'",
                        ddEngine.getDataFileMetaData().getDataFilePath(),
                        ddEngine.getDataSourceTier());
            }

            IPIntelligenceOnPremiseEngine ipiEngine = pipeline.getElement(IPIntelligenceOnPremiseEngine.class);
            if (ipiEngine != null) {
                DataFileHelper.logDataFileInfo(ipiEngine);
            }

            logger.info("All done");
        }
    }

    /**
     * Create combined evidence that includes both User-Agent (for device detection)
     * and IP address (for IP intelligence).
     */
    public static List<Map<String, String>> setUpCombinedEvidence() {
        List<Map<String, String>> evidence = new ArrayList<>();

        // Mobile device from China
        Map<String, String> evidence1 = new LinkedHashMap<>();
        evidence1.put("header.user-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Mobile/15E148 Safari/604.1");
        evidence1.put("query.client-ip", "62.61.32.31");
        evidence.add(evidence1);

        // Desktop from Chile
        Map<String, String> evidence2 = new LinkedHashMap<>();
        evidence2.put("header.user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        evidence2.put("query.client-ip", "45.236.48.61");
        evidence.add(evidence2);

        // Tablet with IPv6
        Map<String, String> evidence3 = new LinkedHashMap<>();
        evidence3.put("header.user-agent", "Mozilla/5.0 (iPad; CPU OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/91.0.4472.80 Mobile/15E148 Safari/604.1");
        evidence3.put("query.client-ip", "2001:0db8:085a:0000:0000:8a2e:0370:7334");
        evidence.add(evidence3);

        // Android device from USA
        Map<String, String> evidence4 = new LinkedHashMap<>();
        evidence4.put("header.user-agent", "Mozilla/5.0 (Linux; Android 11; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");
        evidence4.put("query.client-ip", "8.8.8.8");
        evidence.add(evidence4);

        return evidence;
    }

    private static void analyzeEvidence(Map<String, String> evidence,
                                        Pipeline pipeline,
                                        OutputStream out) throws Exception {
        PrintWriter writer = new PrintWriter(out);

        try (FlowData data = pipeline.createFlowData()) {

            // List the evidence
            writer.println("===============================================================================");
            writer.println("Input values:");
            for (Map.Entry<String, String> entry : evidence.entrySet()) {
                writer.format("\t%s: %s\n", entry.getKey(), entry.getValue());
            }

            // Add the evidence values to the flow data
            data.addEvidence(evidence);

            // Process the flow data - both engines will run
            data.process();

            // Get Device Detection results
            writer.println("\nDevice Detection Results:");
            writer.println("-------------------------");
            DeviceData device = data.get(DeviceData.class);
            writer.println("\tMobile Device: " + asString(tryGet(device::getIsMobile)));
            writer.println("\tPlatform Name: " + asString(tryGet(device::getPlatformName)));
            writer.println("\tPlatform Version: " + asString(tryGet(device::getPlatformVersion)));
            writer.println("\tBrowser Name: " + asString(tryGet(device::getBrowserName)));
            writer.println("\tBrowser Version: " + asString(tryGet(device::getBrowserVersion)));
            writer.println("\tHardware Vendor: " + asString(tryGet(device::getHardwareVendor)));
            writer.println("\tHardware Name: " + asString(tryGet(device::getHardwareName)));
            writer.println("\tDevice Type: " + asString(tryGet(device::getDeviceType)));
            writer.println("\tScreen Width: " + asString(tryGet(device::getScreenPixelsWidth)));
            writer.println("\tScreen Height: " + asString(tryGet(device::getScreenPixelsHeight)));

            // Get IP Intelligence results
            writer.println("\nIP Intelligence Results:");
            writer.println("------------------------");
            IPIntelligenceData ipData = data.get(IPIntelligenceData.class);
            writer.println("\tRegisteredName: " + asStringProperty(tryGet(ipData::getRegisteredName)));
            writer.println("\tRegisteredOwner: " + asStringProperty(tryGet(ipData::getRegisteredOwner)));
            writer.println("\tRegisteredCountry: " + asStringProperty(tryGet(ipData::getRegisteredCountry)));
            writer.println("\tIpRangeStart: " + asIPAddressProperty(tryGet(ipData::getIpRangeStart)));
            writer.println("\tIpRangeEnd: " + asIPAddressProperty(tryGet(ipData::getIpRangeEnd)));
            writer.println("\tCountry: " + asStringProperty(tryGet(ipData::getCountry)));
            writer.println("\tCountryCode: " + asStringProperty(tryGet(ipData::getCountryCode)));
            writer.println("\tCountryCode3: " + asStringProperty(tryGet(ipData::getCountryCode3)));
            writer.println("\tRegion: " + asStringProperty(tryGet(ipData::getRegion)));
            writer.println("\tState: " + asStringProperty(tryGet(ipData::getState)));
            writer.println("\tTown: " + asStringProperty(tryGet(ipData::getTown)));
            writer.println("\tLatitude: " + asFloatProperty(tryGet(ipData::getLatitude)));
            writer.println("\tLongitude: " + asFloatProperty(tryGet(ipData::getLongitude)));
            writer.println("\tAreas: " + asWktStringProperty(tryGet(ipData::getAreas)));
            writer.println("\tAccuracyRadiusMin: " + asIntegerProperty(tryGet(ipData::getAccuracyRadiusMin)));
            writer.println("\tTimeZoneOffset: " + asIntegerProperty(tryGet(ipData::getTimeZoneOffset)));
        }
        writer.println();
        writer.flush();
    }

    /**
     * Resolve a data file path that may be absolute, relative, or within the project.
     * Unlike FileFinder.getFilePath() which only searches within the project directory,
     * this also handles paths outside the project (e.g. ../assets/51Degrees-LiteV4.1.hash).
     */
    private static String resolveDataFile(String dataFile) throws IOException {
        Path dataPath = Paths.get(dataFile);
        // Try absolute path
        if (dataPath.isAbsolute() && Files.exists(dataPath)) {
            return dataPath.toString();
        }
        // Try relative to current working directory
        Path relativePath = Paths.get(System.getProperty("user.dir"), dataFile);
        if (Files.exists(relativePath)) {
            return relativePath.toAbsolutePath().toString();
        }
        // Try FileFinder (searches within project directory)
        try {
            return getFilePath(dataFile).getAbsolutePath();
        } catch (Exception e) {
            throw new IOException("Data file not found: " + dataFile);
        }
    }
}
