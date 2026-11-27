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
 * @example console/GettingStartedCloud.java
 *
 * This example shows how to use 51Degrees Cloud IP Intelligence to determine location and network details from IP addresses.
 *
 * You will learn:
 *
 * 1. How to create a Pipeline that uses 51Degrees Cloud IP Intelligence
 * 2. How to pass input data (evidence) to the Pipeline
 * 3. How to retrieve the results
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/51degrees-java/blob/main/examples/ip-intelligence/console/src/main/java/fiftyone/ipintelligence/examples/console/GettingStartedCloud.java).
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

import fiftyone.ipintelligence.examples.shared.FindOutMore;
import fiftyone.ipintelligence.IPIntelligenceCloudPipelineBuilder;
import fiftyone.ipintelligence.IPIntelligencePipelineBuilder;
import fiftyone.ipintelligence.examples.shared.EvidenceHelper;
import fiftyone.ipintelligence.examples.shared.KeyHelper;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.ipintelligence.shared.testhelpers.KeyUtils;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.asWeightedString;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.tryGet;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.weightedValues;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * Provides an illustration of the fundamental elements of carrying out IP Intelligence using
 * the 51Degrees Cloud service. The Cloud service carries out the IP Intelligence lookup
 * remotely, so no data file is needed on your server.
 * <p>
 * Access to the Cloud service is configured using a "resource key". The resource key can be
 * supplied as a command line argument, or via the environment variable or system property
 * "_51DEGREES_RESOURCE_KEY". An optional second command line argument, or the environment variable or
 * system property "_51DEGREES_CLOUD_ENDPOINT", can be used to direct the example at a custom hosted
 * Cloud service.
 * <p>
 * The concepts of "pipeline", "flow data", "evidence" and "results" are illustrated.
 */
public class GettingStartedCloud {
    /**
     * This example's tag in the links of its "Find out more" section.
     */
    private static final String EXAMPLE_TAG =
            "console-src-main-java-fiftyone-ipintelligence-examples-console-gettingstartedcloud.java";

    private static final Logger logger = LoggerFactory.getLogger(GettingStartedCloud.class);

    /**
     * Name of the environment variable or system property that can hold a
     * custom Cloud service endpoint URL.
     */
    public static final String CLOUD_END_POINT_NAME = "_51DEGREES_CLOUD_ENDPOINT";

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));

        // Use the resource key from the command line, environment variable or
        // system property.
        String resourceKey = KeyHelper.findResourceKey(
                args.length > 0 ? args[0] : null);
        if (resourceKey == null) {
            KeyHelper.writeNoResourceKey(System.out, EXAMPLE_TAG);
            FindOutMore.write(System.out, EXAMPLE_TAG,
                    FindOutMore.CLOUD_REPOSITORIES);
            return;
        }
        // An optional custom endpoint for a self-hosted Cloud service.
        String endPoint = args.length > 1 ? args[1] :
                KeyUtils.getNamedKey(CLOUD_END_POINT_NAME);

        // prepare 'evidence' for use in pipeline (see below)
        List<Map<String, String>> evidence = EvidenceHelper.setUpEvidence();
        run(resourceKey, endPoint, evidence, System.out);
        FindOutMore.write(System.out, EXAMPLE_TAG,
                FindOutMore.CLOUD_REPOSITORIES);
    }

    /**
     * Run the example
     * @param resourceKey a 51Degrees resource key
     * @param endPoint optional custom Cloud service endpoint, or null
     * @param evidenceList a List&lt;Map&lt;String, String>> representing evidence
     * @param outputStream somewhere for the results
     */
    public static void run(String resourceKey,
                           String endPoint,
                           List<Map<String, String>> evidenceList,
                           OutputStream outputStream) throws Exception {
        logger.info("Running GettingStartedCloud example");

        /* In this example, we use the IPIntelligencePipelineBuilder and configure it in code.

        For more information about pipelines in general see the documentation at
        https://51degrees.com/documentation/_pipeline_api__concepts__configuration__builders__index.html?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-gettingstartedcloud.java&utm_term=configuration-builders

        Note that we close the pipeline in a finally block to control its lifecycle */
        IPIntelligenceCloudPipelineBuilder builder = new IPIntelligencePipelineBuilder()
                // Tell it that we want to use cloud and pass our resource key.
                .useCloud(resourceKey);
        // If a cloud endpoint has been provided then set the cloud pipeline
        // endpoint.
        if (endPoint != null && endPoint.isEmpty() == false) {
            builder.setEndPoint(endPoint);
        }
        Pipeline pipeline = builder.build();
        try {

            // carry out some sample lookups
            for (Map<String, String> evidence : evidenceList) {
                analyzeEvidence(evidence, pipeline, outputStream);
            }

            logger.info("All done");
        } finally {
            pipeline.close();
        }
    }

    /**
     * Taking a map of evidence as a parameter, process it in the pipeline
     * supplied and output the IP Intelligence results to the output stream.
     * @param evidence a map representing evidence
     * @param pipeline a pipeline set up to process the evidence
     * @param out somewhere to send the lookup results
     */
    private static void analyzeEvidence(Map<String, String> evidence,
                                        Pipeline pipeline,
                                        OutputStream out) throws Exception {
        PrintWriter writer = new PrintWriter(out);
        /* FlowData is a data structure that is used to convey information required for a lookup
        and the results of the lookup through the pipeline. Information required for
        a lookup is called "evidence", in this case represented by a Map<String, String> of
        evidence key/value entries.

        FlowData is closed in a finally block in order to ensure that the resources
        are freed in a timely manner */
        FlowData data = pipeline.createFlowData();
        try {

            // list the evidence
            writer.println("Input values:");
            for (Map.Entry<String, String> entry : evidence.entrySet()) {
                writer.format("\t%s: %s\n", entry.getKey(), entry.getValue());
            }

            // Add the evidence values to the flow data
            data.addEvidence(evidence);

            // Process the flow data.
            data.process();

            writer.println("Results:");
            /* Now that it has been processed, the flow data will have been populated with the
            result.

            In this case, we want information about the IP address, which we can get by asking
            for a result matching the "IPIntelligenceData" interface. */
            IPIntelligenceData ipData = data.get(IPIntelligenceData.class);

            /* Display the results of the lookup, which are called IP Intelligence properties.
            See the property dictionary at https://51degrees.com/developers/property-dictionary?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-gettingstartedcloud.java&utm_term=property-dictionary
            for details of all available properties. */
            writeValue(writer, "RegisteredName", tryGet(ipData::getRegisteredName));
            writeValue(writer, "RegisteredOwner", tryGet(ipData::getRegisteredOwner));
            writeValue(writer, "RegisteredCountry", tryGet(ipData::getRegisteredCountry));
            writeValue(writer, "IpRangeStart", tryGet(ipData::getIpRangeStart));
            writeValue(writer, "IpRangeEnd", tryGet(ipData::getIpRangeEnd));
            writeValue(writer, "Country", tryGet(ipData::getCountry));
            writeValue(writer, "CountryCode", tryGet(ipData::getCountryCode));
            writeValue(writer, "CountryCode3", tryGet(ipData::getCountryCode3));
            writeValue(writer, "Region", tryGet(ipData::getRegion));
            writeValue(writer, "State", tryGet(ipData::getState));
            writeValue(writer, "Town", tryGet(ipData::getTown));
            writeValue(writer, "Latitude", tryGet(ipData::getLatitude));
            writeValue(writer, "Longitude", tryGet(ipData::getLongitude));
            writeValue(writer, "Areas", tryGet(ipData::getAreas));
            writeValue(writer, "AccuracyRadiusMin", tryGet(ipData::getAccuracyRadiusMin));
            writeValue(writer, "TimeZoneOffset", tryGet(ipData::getTimeZoneOffset));
        } finally {
            data.close();
        }
        writer.println();
        writer.flush();
    }

    /**
     * Writes each value of a property with its weighting, or why it has no
     * value.
     */
    private static void writeValue(PrintWriter writer,
                                   String property,
                                   AspectPropertyValue<?> result) {
        if (result.hasValue() == false) {
            writer.println("\t" + property + ": " + asWeightedString(result));
            return;
        }
        /* IP Intelligence properties return weighted values. Each value comes
        with a weighting between 0 and 1, which says how much of the IP range
        the value applies to, and a property with a single answer returns it
        with a weighting of 1. */
        StringBuilder line = new StringBuilder();
        for (IWeightedValue<?> weighted : weightedValues(result)) {
            if (line.length() > 0) {
                line.append(", ");
            }
            line.append(asWeightedString(
                    weighted.getValue(),
                    weighted.getWeighting()));
        }
        writer.println("\t" + property + ": " + line);
    }
}
