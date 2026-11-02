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

package fiftyone.geolocation.examples.cloud;

import fiftyone.geolocation.GeoLocationPipelineBuilder;
import fiftyone.geolocation.core.Enums;
import fiftyone.geolocation.core.data.GeoData;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

import static fiftyone.geolocation.core.Constants.EVIDENCE_GEO_LAT_PARAM_KEY;
import static fiftyone.geolocation.core.Constants.EVIDENCE_GEO_LON_PARAM_KEY;

/**
 *  @example cloud/GettingStarted.java
 *
 * Getting started example of using cloud geo-location.
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/location-java/blob/master/geo-location.shell.examples/src/main/java/fiftyone/geolocation/examples/cloud/GettingStarted.java).
 *
 * Required Maven Dependencies:
 * - [com.51degrees.geo-location](https://central.sonatype.com/artifact/com.51degrees/geo-location/)
 *
 * 1. Build a new Pipeline to use the 51Degrees cloud service, with lazy loading
 * configured to allow up to a second for a response from the service.
 * ```
 * Pipeline pipeline = new GeoLocationPipelineBuilder(loggerFactory)
 *     .useCloud(resourceKey, Enums.GeoLocationProvider.FiftyOneDegrees)
 *     .useLazyLoading(1000)
 *     .build();
 * ```
 *
 * 2. Create a new FlowData instance ready to be populated with evidence for
 * the Pipeline.
 * ```
 * FlowData data = pipeline.createFlowData();
 * ```
 *
 * 3. Process a longitude and latitude to retrieve the values associated with
 * with the location for the selected properties.
 * ```
 * data
 *     .addEvidence(EVIDENCE_GEO_LAT_PARAM_KEY, "51.458048")
 *     .addEvidence(EVIDENCE_GEO_LON_PARAM_KEY, "-0.9822207999999999")
 *     .process();
 * ```
 *
 * 4. Extract the value of a property as a string from the results.
 * ```
 * AspectPropertyValue<String> country = data.get(GeoData.class).getCoutnry();
 * ```
 */
public class GettingStarted {

    private static ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

    public static void main(String[] args) throws Exception {
        // Obtain a resource key for free at https://configure.51degrees.com?utm_source=code&utm_medium=example&utm_campaign=location-java&utm_content=geo-location.shell.examples-src-main-java-fiftyone-geolocation-examples-cloud-gettingstarted.java&utm_term=main
        // Make sure to include the 'Country' property as it is used by this example.
        String resourceKey = "!!Your resource license key!!";

        if (resourceKey.startsWith("!!")) {
            System.out.println("You need to create a resource key at " +
                "https://configure.51degrees.com?utm_source=code&utm_medium=example&utm_campaign=location-java&utm_content=geo-location.shell.examples-src-main-java-fiftyone-geolocation-examples-cloud-gettingstarted.java&utm_term=resource-key-required and paste it into this example.");
            System.out.println("Make sure to include the 'Country' " +
                "property as it is used by this example.");
        }
        else {
            Pipeline pipeline = new GeoLocationPipelineBuilder(loggerFactory)
                .useCloud(resourceKey, Enums.GeoLocationProvider.FiftyOneDegrees)
                .setEndPoint("https://cloud.51degrees.com/api/v4")
                .useLazyLoading(1000)
                .setAutoCloseElements(true)
                .build();

            try (FlowData flowData = pipeline.createFlowData()) {
                flowData.addEvidence(EVIDENCE_GEO_LAT_PARAM_KEY, "51.458048")
                    .addEvidence(EVIDENCE_GEO_LON_PARAM_KEY, "-0.9822207999999999")
                    .process();

                AspectPropertyValue<String> country = flowData.get(GeoData.class).getCountry();

                Future<?> future = flowData.get(GeoData.class).getProcessFuture();
                System.out.print("Awaiting response");
                outputUntilCancelled(".", 1000, future);

                System.out.println(flowData.get(CloudRequestData.class).getJsonResponse());

                System.out.println("Country: " + country.toString());

                System.out.println(flowData.get(GeoData.class).getRegion());
            }          
            pipeline.close();
        }
    }

    private static void outputUntilCancelled(
        String text,
        int intervalMs,
        Future<?> future) throws InterruptedException {
        while (future.isDone() == false) {
            System.out.print(text);
            Thread.sleep(intervalMs);
        }
        future.cancel(true);
    }
}
