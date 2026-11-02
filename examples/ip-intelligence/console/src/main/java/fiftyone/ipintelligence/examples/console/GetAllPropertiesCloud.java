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
 * @example console/GetAllPropertiesCloud.java
 *
 * This example shows how to retrieve all available IP Intelligence properties from the 51Degrees Cloud service.
 *
 * You will learn:
 *
 * 1. How to create a Pipeline that uses 51Degrees Cloud IP Intelligence
 * 2. How to process an IP address and retrieve all available properties
 * 3. How to enumerate through all the properties returned by the service
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/ip-intelligence-java-examples/blob/main/console/src/main/java/fiftyone/ipintelligence/examples/console/GetAllPropertiesCloud.java).
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
 */

package fiftyone.ipintelligence.examples.console;

import fiftyone.ipintelligence.IPIntelligenceCloudPipelineBuilder;
import fiftyone.ipintelligence.IPIntelligencePipelineBuilder;
import fiftyone.ipintelligence.examples.shared.KeyHelper;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.ipintelligence.shared.testhelpers.KeyUtils;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * This example shows how to process a single IP address with the 51Degrees Cloud service and
 * then iterate through all the properties returned, displaying each name and value.
 * <p>
 * Access to the Cloud service is configured using a "resource key". The resource key can be
 * supplied as a command line argument, or via the environment variable or system property
 * "TestResourceKey". An optional second command line argument, or the environment variable or
 * system property "TestCloudEndPoint", can be used to direct the example at a custom hosted
 * Cloud service.
 */
public class GetAllPropertiesCloud {
    private static final Logger logger = LoggerFactory.getLogger(GetAllPropertiesCloud.class);

    /**
     * The IP address that the example will look up the properties for.
     */
    public static final String SOME_IP_ADDRESS = "8.8.8.8";

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));

        // Use the resource key from the command line, environment variable or
        // system property.
        String resourceKey = args.length > 0 ? args[0] :
                KeyHelper.getOrSetTestResourceKey();
        // An optional custom endpoint for a self-hosted Cloud service.
        String endPoint = args.length > 1 ? args[1] :
                KeyUtils.getNamedKey(GettingStartedCloud.CLOUD_END_POINT_NAME);

        run(resourceKey, endPoint, System.out);
    }

    /**
     * Run the example
     * @param resourceKey a 51Degrees resource key
     * @param endPoint optional custom Cloud service endpoint, or null
     * @param outputStream somewhere for the results
     */
    public static void run(String resourceKey,
                           String endPoint,
                           OutputStream outputStream) throws Exception {
        logger.info("Running GetAllPropertiesCloud example");

        IPIntelligenceCloudPipelineBuilder builder = new IPIntelligencePipelineBuilder()
                // Tell it that we want to use cloud and pass our resource key.
                .useCloud(resourceKey);
        // If a cloud endpoint has been provided then set the cloud pipeline
        // endpoint.
        if (endPoint != null && endPoint.isEmpty() == false) {
            builder.setEndPoint(endPoint);
        }

        // Create the pipeline
        try (Pipeline pipeline = builder.build()) {
            // Output details for the IP address
            analyzeEvidence(SOME_IP_ADDRESS, pipeline, outputStream);
            logger.info("All done");
        }
    }

    private static void analyzeEvidence(String ipAddress,
                                        Pipeline pipeline,
                                        OutputStream out) throws Exception {
        PrintWriter writer = new PrintWriter(out);
        // Create the FlowData instance.
        try (FlowData data = pipeline.createFlowData()) {
            // Add the client IP as evidence.
            data.addEvidence("query.client-ip", ipAddress);
            // Process the supplied evidence.
            data.process();
            // Get the IP data from the flow data.
            IPIntelligenceData ipData = data.get(IPIntelligenceData.class);
            writer.format("What property values are associated with " +
                    "the IP '%s'?\n", ipAddress);

            // Iterate through the results, displaying all values in order of
            // the property name.
            Map<String, Object> properties = new TreeMap<>(ipData.asKeyMap());
            for (Map.Entry<String, Object> property : properties.entrySet()) {
                writer.format("%s = %s\n",
                        property.getKey(),
                        getValueToOutput(property.getValue()));
            }
        }
        writer.println();
        writer.flush();
    }

    /**
     * Convert the given value into a human-readable string representation.
     * @param propertyValue property value object to be converted
     * @return a string representation of the value
     */
    private static String getValueToOutput(Object propertyValue) {
        if (propertyValue == null) {
            return "NULL";
        }

        Object value = propertyValue;
        if (propertyValue instanceof AspectPropertyValue) {
            AspectPropertyValue<?> aspectPropertyValue =
                    (AspectPropertyValue<?>) propertyValue;
            if (aspectPropertyValue.hasValue()) {
                value = aspectPropertyValue.getValue();
            }
            else {
                // The property has no value so output the reason.
                return "NO VALUE (" +
                        aspectPropertyValue.getNoValueMessage() + ")";
            }
        }

        if (value instanceof Iterable && value instanceof String == false) {
            // Property is an Iterable (that is not a string) so return a
            // comma-separated list of values.
            StringBuilder output = new StringBuilder();
            for (Object entry : (Iterable<?>) value) {
                if (output.length() > 0) {
                    output.append(",");
                }
                output.append(entry);
            }
            return output.toString();
        }
        else {
            String str = value.toString();
            // Truncate any long strings to 200 characters
            if (str.length() > 200) {
                str = str.substring(0, 200) + "...";
            }
            return str;
        }
    }
}
