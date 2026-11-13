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
 * This example is available in full on [GitHub](https://github.com/51Degrees/51degrees-java/blob/main/examples/ip-intelligence/console/src/main/java/fiftyone/ipintelligence/examples/console/GettingStartedMixed.java).
 *
 * By default the example uses the free Lite Device Detection and IP Intelligence data files,
 * which it downloads when they are missing, and shows each property only the Enterprise data
 * files have as needing them. To obtain the Enterprise data files, please
 * [contact us](https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-gettingstartedmixed.java&utm_term=header).
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
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneAspectPropertyMetaData;
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
import java.util.function.Supplier;

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

    public static final String DD_DATA_FILE_DEFAULT = "device-detection/51Degrees-LiteV4.1.hash";
    public static final String IPI_DATA_FILE_DEFAULT = DataFileHelper.LITE_DATA_FILE_REL_PATH;

    /**
     * Where to ask for a copy of the Enterprise data files, tagged with this
     * example.
     */
    private static final String CONTACT_US = "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-gettingstartedmixed.java&utm_term=enterprise-data-file";

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
                    ipiDataFile, DataFileHelper.IPI_PATH_VARIABLE);
            throw e;
        }

        // Load the pipeline configuration that includes both engines
        File optionsFile = getFilePath("gettingStartedMixed.xml");
        PipelineOptions pipelineOptions = PipelineOptionsFactory.getOptionsFromFile(optionsFile);

        try (Pipeline pipeline = new FiftyOnePipelineBuilder()
                .buildFromConfiguration(pipelineOptions)) {

            // The engines say which properties their data files have.
            DeviceDetectionHashEngine ddEngine = pipeline.getElement(DeviceDetectionHashEngine.class);
            IPIntelligenceOnPremiseEngine ipiEngine = pipeline.getElement(IPIntelligenceOnPremiseEngine.class);

            // Process each set of evidence
            boolean needsEnterprise = false;
            for (Map<String, String> evidence : evidenceList) {
                needsEnterprise |= analyzeEvidence(
                        evidence, pipeline, ddEngine, ipiEngine, outputStream);
            }
            if (needsEnterprise) {
                DataFileHelper.writeEnterpriseNote(new PrintWriter(outputStream), CONTACT_US);
            }

            // Log data file info for both engines
            if (ddEngine != null) {
                logger.info("Device Detection data file: '{}', tier: '{}'",
                        ddEngine.getDataFileMetaData().getDataFilePath(),
                        ddEngine.getDataSourceTier());
            }

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

    /**
     * Processes the evidence and writes both engines' results, showing each
     * property the data files in use do not have as needing the Enterprise
     * data file.
     * @return true if a property needed the Enterprise data file
     */
    private static boolean analyzeEvidence(Map<String, String> evidence,
                                           Pipeline pipeline,
                                           DeviceDetectionHashEngine ddEngine,
                                           IPIntelligenceOnPremiseEngine ipiEngine,
                                           OutputStream out) throws Exception {
        boolean needsEnterprise = false;
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
            needsEnterprise |= writeValue(writer, "Mobile Device",
                    hasProperty(ddEngine, "IsMobile"), device::getIsMobile);
            needsEnterprise |= writeValue(writer, "Platform Name",
                    hasProperty(ddEngine, "PlatformName"), device::getPlatformName);
            needsEnterprise |= writeValue(writer, "Platform Version",
                    hasProperty(ddEngine, "PlatformVersion"), device::getPlatformVersion);
            needsEnterprise |= writeValue(writer, "Browser Name",
                    hasProperty(ddEngine, "BrowserName"), device::getBrowserName);
            needsEnterprise |= writeValue(writer, "Browser Version",
                    hasProperty(ddEngine, "BrowserVersion"), device::getBrowserVersion);
            needsEnterprise |= writeValue(writer, "Hardware Vendor",
                    hasProperty(ddEngine, "HardwareVendor"), device::getHardwareVendor);
            needsEnterprise |= writeValue(writer, "Hardware Name",
                    hasProperty(ddEngine, "HardwareName"), device::getHardwareName);
            needsEnterprise |= writeValue(writer, "Device Type",
                    hasProperty(ddEngine, "DeviceType"), device::getDeviceType);
            needsEnterprise |= writeValue(writer, "Screen Width",
                    hasProperty(ddEngine, "ScreenPixelsWidth"), device::getScreenPixelsWidth);
            needsEnterprise |= writeValue(writer, "Screen Height",
                    hasProperty(ddEngine, "ScreenPixelsHeight"), device::getScreenPixelsHeight);

            // Get IP Intelligence results
            writer.println("\nIP Intelligence Results:");
            writer.println("------------------------");
            IPIntelligenceData ipData = data.get(IPIntelligenceData.class);
            needsEnterprise |= writeValue(writer, "RegisteredName",
                    DataFileHelper.hasProperty(ipiEngine, "RegisteredName"), ipData::getRegisteredName);
            needsEnterprise |= writeValue(writer, "RegisteredOwner",
                    DataFileHelper.hasProperty(ipiEngine, "RegisteredOwner"), ipData::getRegisteredOwner);
            needsEnterprise |= writeValue(writer, "RegisteredCountry",
                    DataFileHelper.hasProperty(ipiEngine, "RegisteredCountry"), ipData::getRegisteredCountry);
            needsEnterprise |= writeValue(writer, "IpRangeStart",
                    DataFileHelper.hasProperty(ipiEngine, "IpRangeStart"), ipData::getIpRangeStart);
            needsEnterprise |= writeValue(writer, "IpRangeEnd",
                    DataFileHelper.hasProperty(ipiEngine, "IpRangeEnd"), ipData::getIpRangeEnd);
            needsEnterprise |= writeValue(writer, "Country",
                    DataFileHelper.hasProperty(ipiEngine, "Country"), ipData::getCountry);
            needsEnterprise |= writeValue(writer, "CountryCode",
                    DataFileHelper.hasProperty(ipiEngine, "CountryCode"), ipData::getCountryCode);
            needsEnterprise |= writeValue(writer, "CountryCode3",
                    DataFileHelper.hasProperty(ipiEngine, "CountryCode3"), ipData::getCountryCode3);
            needsEnterprise |= writeValue(writer, "Region",
                    DataFileHelper.hasProperty(ipiEngine, "Region"), ipData::getRegion);
            needsEnterprise |= writeValue(writer, "State",
                    DataFileHelper.hasProperty(ipiEngine, "State"), ipData::getState);
            needsEnterprise |= writeValue(writer, "Town",
                    DataFileHelper.hasProperty(ipiEngine, "Town"), ipData::getTown);
            needsEnterprise |= writeValue(writer, "Latitude",
                    DataFileHelper.hasProperty(ipiEngine, "Latitude"), ipData::getLatitude);
            needsEnterprise |= writeValue(writer, "Longitude",
                    DataFileHelper.hasProperty(ipiEngine, "Longitude"), ipData::getLongitude);
            needsEnterprise |= writeValue(writer, "Areas",
                    DataFileHelper.hasProperty(ipiEngine, "Areas"), ipData::getAreas);
            needsEnterprise |= writeValue(writer, "AccuracyRadiusMin",
                    DataFileHelper.hasProperty(ipiEngine, "AccuracyRadiusMin"), ipData::getAccuracyRadiusMin);
            needsEnterprise |= writeValue(writer, "TimeZoneOffset",
                    DataFileHelper.hasProperty(ipiEngine, "TimeZoneOffset"), ipData::getTimeZoneOffset);
        }
        writer.println();
        writer.flush();
        return needsEnterprise;
    }

    /**
     * Writes a property, or that it needs the Enterprise data file when the
     * data file in use does not have it. The value is only read when the
     * data file has the property.
     * @return true if the property needs the Enterprise data file
     */
    private static boolean writeValue(PrintWriter writer,
                                      String name,
                                      boolean hasProperty,
                                      Supplier<AspectPropertyValue<?>> value) {
        if (hasProperty) {
            writer.println("\t" + name + ": " + asString(value.get()));
            return false;
        }
        writer.println("\t" + name + ": " + DataFileHelper.NEEDS_ENTERPRISE);
        return true;
    }

    /**
     * True when the Device Detection engine's data file has the property.
     * The Lite data file leaves out the properties only the Enterprise data
     * file has, such as HardwareVendor, HardwareName and DeviceType.
     */
    private static boolean hasProperty(DeviceDetectionHashEngine engine, String name) {
        for (FiftyOneAspectPropertyMetaData property : engine.getProperties()) {
            if (property.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
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
        // Search within the project directory, downloading the free Lite
        // file if it is missing
        try {
            return DataFileHelper.findDataFile(dataFile).getAbsolutePath();
        } catch (Exception e) {
            throw new IOException("Data file not found: " + dataFile, e);
        }
    }
}
