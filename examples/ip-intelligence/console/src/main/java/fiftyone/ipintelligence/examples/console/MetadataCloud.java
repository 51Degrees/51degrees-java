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
 * @example console/MetadataCloud.java
 *
 * The Cloud service exposes metadata that can provide additional information about the various
 * properties that might be returned.
 * This example shows how to access this data and display the values available.
 *
 * A list of the properties will be displayed, along with some additional information about each
 * property. Note that this is the list of properties used by the supplied resource key, rather
 * than all properties that can be returned by the Cloud service.
 *
 * In addition, the evidence keys that are accepted by the service are listed. These are the
 * keys that, when added to the evidence collection in flow data, could have some impact on the
 * result that is returned.
 *
 * Bear in mind that this is a list of ALL evidence keys accepted by all products offered by the
 * cloud. If you are only using a single product (for example, IP intelligence) then not all
 * of these keys will be relevant.
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/ip-intelligence-java-examples/blob/main/console/src/main/java/fiftyone/ipintelligence/examples/console/MetadataCloud.java).
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
import fiftyone.ipintelligence.cloud.flowelements.IPIntelligenceCloudEngine;
import fiftyone.ipintelligence.examples.shared.KeyHelper;
import fiftyone.ipintelligence.shared.testhelpers.KeyUtils;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * This example shows how to access the metadata that the Cloud service exposes about the
 * properties available with the resource key supplied, along with the evidence keys that are
 * accepted by the service.
 * <p>
 * Access to the Cloud service is configured using a "resource key". The resource key can be
 * supplied as a command line argument, or via the environment variable or system property
 * "TestResourceKey". An optional second command line argument, or the environment variable or
 * system property "TestCloudEndPoint", can be used to direct the example at a custom hosted
 * Cloud service.
 */
public class MetadataCloud {
    private static final Logger logger = LoggerFactory.getLogger(MetadataCloud.class);

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
        logger.info("Running MetadataCloud example");

        IPIntelligenceCloudPipelineBuilder builder = new IPIntelligencePipelineBuilder()
                // Tell it that we want to use cloud and pass our resource key.
                .useCloud(resourceKey);
        // If a cloud endpoint has been provided then set the cloud pipeline
        // endpoint.
        if (endPoint != null && endPoint.isEmpty() == false) {
            builder.setEndPoint(endPoint);
        }

        try (Pipeline pipeline = builder.build()) {
            PrintWriter writer = new PrintWriter(outputStream);

            logger.info("Listing Properties");
            outputProperties(
                    pipeline.getElement(IPIntelligenceCloudEngine.class),
                    writer);
            writer.println();
            writer.flush();

            /* We use the CloudRequestEngine to get evidence key details, rather than the
            IPIntelligenceCloudEngine.

            This is because the IPIntelligenceCloudEngine doesn't actually make use of any
            evidence values. It simply processes the JSON that is returned by the call to the
            Cloud service that is made by the CloudRequestEngine.
            The CloudRequestEngine is actually taking the evidence values and passing them to
            the cloud, so that's the engine we want the keys from. */
            logger.info("Listing Evidence Key Details");
            outputEvidenceKeyDetails(
                    pipeline.getElement(CloudRequestEngine.class),
                    writer);
            writer.println();
            writer.flush();

            logger.info("All done");
        }
    }

    private static void outputEvidenceKeyDetails(CloudRequestEngine engine,
                                                 PrintWriter output) {
        output.println();
        if (engine.getEvidenceKeyFilter() instanceof EvidenceKeyFilterWhitelist) {
            // If the evidence key filter extends EvidenceKeyFilterWhitelist then we can
            // display a list of accepted keys.
            EvidenceKeyFilterWhitelist filter =
                    (EvidenceKeyFilterWhitelist) engine.getEvidenceKeyFilter();
            output.println("Accepted evidence keys:");
            for (Map.Entry<String, Integer> entry : filter.getWhitelist().entrySet()) {
                output.println("\t" + entry.getKey());
            }
        } else {
            output.format("The evidence key filter has type " +
                    "%s. As this does not extend " +
                    "EvidenceKeyFilterWhitelist, a list of accepted values cannot be " +
                    "displayed. As an alternative, you can pass evidence keys to " +
                    "filter.include(string) to see if a particular key will be included " +
                    "or not.\n", engine.getEvidenceKeyFilter().getClass().getName());
            output.println("For example, query.client-ip " +
                    (engine.getEvidenceKeyFilter().include("query.client-ip") ?
                            "is " : "is not ") + "accepted.");
        }
    }

    private static void outputProperties(IPIntelligenceCloudEngine engine,
                                         PrintWriter output) {
        for (AspectPropertyMetaData property : engine.getProperties()) {
            // Output some details about the property.
            output.format("Property - %s [Category: %s] (%s)\n",
                    property.getName(),
                    property.getCategory(),
                    property.getType().getSimpleName());
        }
    }
}
