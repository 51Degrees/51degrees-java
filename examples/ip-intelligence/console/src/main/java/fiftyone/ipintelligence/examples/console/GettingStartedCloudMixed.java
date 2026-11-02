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
 * @example console/GettingStartedCloudMixed.java
 *
 * This example demonstrates using both Device Detection and IP Intelligence from the 51Degrees Cloud service.
 *
 * You will learn:
 *
 * 1. How to create a Pipeline that uses both 51Degrees Cloud Device Detection and IP Intelligence
 * 2. How to pass input data (User-Agent and IP address) to the Pipeline
 * 3. How to retrieve device and IP intelligence results from a single pipeline
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/ip-intelligence-java-examples/blob/main/console/src/main/java/fiftyone/ipintelligence/examples/console/GettingStartedCloudMixed.java).
 *
 * To run this example, you will eventually need to create a Resource Key. A Resource Key is used as
 * shorthand to store the particular set of properties you are interested in as well as any
 * associated License Keys that entitle you to increased request limits and/or paid-for properties,
 * but it is not yet available for IP Intelligence. For now you can run a custom hosted Cloud
 * service, such as the one provided by the GettingStartedAPI example in
 * ip-intelligence-dotnet-examples, and point this example to its endpoint.
 *
 * Required Maven Dependencies:
 * - [com.51degrees:ip-intelligence](https://central.sonatype.com/artifact/com.51degrees/ip-intelligence)
 * - [com.51degrees:device-detection](https://central.sonatype.com/artifact/com.51degrees/device-detection)
 */

package fiftyone.ipintelligence.examples.console;

import fiftyone.devicedetection.cloud.flowelements.DeviceDetectionCloudEngine;
import fiftyone.devicedetection.cloud.flowelements.DeviceDetectionCloudEngineBuilder;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.ipintelligence.cloud.flowelements.IPIntelligenceCloudEngine;
import fiftyone.ipintelligence.cloud.flowelements.IPIntelligenceCloudEngineBuilder;
import fiftyone.ipintelligence.examples.shared.KeyHelper;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.ipintelligence.shared.testhelpers.KeyUtils;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngineBuilder;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.asFloatProperty;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.asIPAddressProperty;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.asIntegerProperty;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.asString;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.asStringProperty;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.tryGet;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * Demonstrates using both Device Detection and IP Intelligence from the 51Degrees Cloud
 * service in a single pipeline. Both engines process the same evidence, providing
 * comprehensive device and location information from a single pipeline invocation.
 * <p>
 * Access to the Cloud service is configured using a "resource key". The resource key can be
 * supplied as a command line argument, or via the environment variable or system property
 * "TestResourceKey". An optional second command line argument, or the environment variable or
 * system property "TestCloudEndPoint", can be used to direct the example at a custom hosted
 * Cloud service.
 */
public class GettingStartedCloudMixed {
    private static final Logger logger = LoggerFactory.getLogger(GettingStartedCloudMixed.class);

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));

        // Use the resource key from the command line, environment variable or
        // system property.
        String resourceKey = args.length > 0 ? args[0] :
                KeyHelper.getOrSetTestResourceKey();
        // An optional custom endpoint for a self-hosted Cloud service.
        String endPoint = args.length > 1 ? args[1] :
                KeyUtils.getNamedKey(GettingStartedCloud.CLOUD_END_POINT_NAME);

        // Use the combined evidence from the on-premise mixed example which
        // includes both a User-Agent header and an IP address.
        List<Map<String, String>> evidence = GettingStartedMixed.setUpCombinedEvidence();
        run(resourceKey, endPoint, evidence, System.out);
    }

    /**
     * Run the example
     * @param resourceKey a 51Degrees resource key
     * @param endPoint optional custom Cloud service endpoint, or null
     * @param evidenceList combined evidence with User-Agent and IP address
     * @param outputStream somewhere for the results
     */
    public static void run(String resourceKey,
                           String endPoint,
                           List<Map<String, String>> evidenceList,
                           OutputStream outputStream) throws Exception {
        logger.info("Running GettingStartedCloudMixed example");

        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

        /* There is no pre-packaged builder for a pipeline containing both the Cloud Device
        Detection and Cloud IP Intelligence engines so the engines are constructed
        individually and added to a pipeline.

        The CloudRequestEngine makes the request to the Cloud service. The
        DeviceDetectionCloudEngine and IPIntelligenceCloudEngine interpret the JSON response. */
        CloudRequestEngineBuilder cloudRequestEngineBuilder =
                new CloudRequestEngineBuilder(loggerFactory)
                        .setResourceKey(resourceKey);
        // If a cloud endpoint has been provided then set the cloud request
        // engine endpoint.
        if (endPoint != null && endPoint.isEmpty() == false) {
            cloudRequestEngineBuilder.setEndpoint(endPoint);
        }
        CloudRequestEngine cloudRequestEngine = cloudRequestEngineBuilder.build();
        DeviceDetectionCloudEngine deviceDetectionEngine =
                new DeviceDetectionCloudEngineBuilder(loggerFactory).build();
        IPIntelligenceCloudEngine ipIntelligenceEngine =
                new IPIntelligenceCloudEngineBuilder(loggerFactory).build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(cloudRequestEngine)
                .addFlowElement(deviceDetectionEngine)
                .addFlowElement(ipIntelligenceEngine)
                .build()) {

            // carry out some sample detections
            for (Map<String, String> evidence : evidenceList) {
                analyzeEvidence(evidence, pipeline, outputStream);
            }

            logger.info("All done");
        }
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
            writer.println("\tCountry: " + asStringProperty(tryGet(ipData::getCountry)));
            writer.println("\tCountryCode: " + asStringProperty(tryGet(ipData::getCountryCode)));
            writer.println("\tRegion: " + asStringProperty(tryGet(ipData::getRegion)));
            writer.println("\tState: " + asStringProperty(tryGet(ipData::getState)));
            writer.println("\tTown: " + asStringProperty(tryGet(ipData::getTown)));
            writer.println("\tLatitude: " + asFloatProperty(tryGet(ipData::getLatitude)));
            writer.println("\tLongitude: " + asFloatProperty(tryGet(ipData::getLongitude)));
            writer.println("\tRegisteredName: " + asStringProperty(tryGet(ipData::getRegisteredName)));
            writer.println("\tRegisteredOwner: " + asStringProperty(tryGet(ipData::getRegisteredOwner)));
            writer.println("\tRegisteredCountry: " + asStringProperty(tryGet(ipData::getRegisteredCountry)));
            writer.println("\tIpRangeStart: " + asIPAddressProperty(tryGet(ipData::getIpRangeStart)));
            writer.println("\tIpRangeEnd: " + asIPAddressProperty(tryGet(ipData::getIpRangeEnd)));
            writer.println("\tAccuracyRadiusMin: " + asIntegerProperty(tryGet(ipData::getAccuracyRadiusMin)));
            writer.println("\tTimeZoneOffset: " + asIntegerProperty(tryGet(ipData::getTimeZoneOffset)));
        }
        writer.println();
        writer.flush();
    }
}
