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

package fiftyone.geolocation.examples;

import fiftyone.devicedetection.cloud.data.DeviceDataCloud;
import fiftyone.devicedetection.cloud.flowelements.DeviceDetectionCloudEngineBuilder;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.geolocation.core.Enums;
import fiftyone.geolocation.core.data.GeoData;
import fiftyone.geolocation.data.CloudGeoData;
import fiftyone.geolocation.flowelements.GeoLocationCloudEngineBuilder;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngineBuilder;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Future;

import static fiftyone.geolocation.core.Constants.EVIDENCE_GEO_LAT_PARAM_KEY;
import static fiftyone.geolocation.core.Constants.EVIDENCE_GEO_LON_PARAM_KEY;
import static fiftyone.pipeline.core.Constants.EVIDENCE_HEADER_USERAGENT_KEY;

/**
 *  @example CombiningServices.java
 *
 * Getting started example of using 51Degrees geo-location alongside 51Degrees
 * device detection.
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/location-java/blob/master/geo-location.shell.examples/src/main/java/fiftyone/geolocation/examples/CombiningServices.java).
 *
 * Required Maven Dependencies:
 * - [com.51degrees.geo-location](https://central.sonatype.com/artifact/com.51degrees/geo-location/)
 * - [com.51degrees.device-detection](https://central.sonatype.com/artifact/com.51degrees/device-detection/)
 *
 * 1. Build new cloud-based geo-location, and device detection engines, along
 * with the cloud request engine on which they rely.
 * ```
 * CloudRequestEngine cloudRequestEngine =
 *     new CloudRequestEngineBuilder(loggerFactory, httpClient)
 *     .setResourceKey(resourceKey)
 *     .build();
 * AspectEngine deviceDetectionEngine =
 *     new DeviceDetectionCloudEngineBuilder(loggerFactory, httpClient, cloudRequestEngine)
 *     .build();
 * AspectEngine locationEngine =
 *     new GeoLocationCloudEngineBuilder(loggerFactory, cloudRequestEngine)
 *     .build(Enums.GeoLocationProvider.FiftyOneDegrees);
 * ```
 *
 * 2. Build a new Pipeline containing the engines which were just created.
 * ```
 * Pipeline pipeline = new PipelineBuilder(loggerFactory)
 *     .addFlowElement(cloudRequestEngine)
 *     .addFlowElement(deviceDetectionEngine)
 *     .addFlowElement(locationEngine)
 *     .build();
 * ```
 *
 * Note that while the cloud request engine must be run before the others, the
 * device detection and geo-location engines can be run in parallel.
 * ```
 * Pipeline pipeline = new PipelineBuilder(loggerFactory)
 *     .addFlowElement(cloudRequestEngine)
 *     .addFlowElementsParallel(new FlowElement[]{deviceDetectionEngine, locationEngine})
 *     .build();
 * ```
 * 3. Create a new FlowData instance ready to be populated with evidence for
 * the Pipeline.
 * ```
 * FlowData data = pipeline.createFlowData();
 * ```
 *
 * 4. Process a longitude, latitude and User-Agent to retrieve the values
 * associated with with the location and device for the selected properties.
 * ```
 * data
 *     .addEvidence(EVIDENCE_HEADER_USERAGENT_KEY, userAgent)
 *     .addEvidence(EVIDENCE_GEO_LAT_PARAM_KEY, "51.458048")
 *     .addEvidence(EVIDENCE_GEO_LON_PARAM_KEY, "-0.9822207999999999")
 *     .process();
 * ```
 *
 * 5. Extract the value of a property as a string from the results.
 * ```
 * AspectPropertyValue<Boolean> isMobile = data.get(Device.class).getIsMobile();
 * AspectPropertyValue<String> country = data.get(GeoData.class).getCountry();
 * ```
 */
public class CombiningServices {

    private static ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

    private static HttpClient httpClient = new HttpClientDefault();

    private static String mobileUserAgent =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 7_1 like Mac OS X) " +
            "AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile" +
            "/11D167 Safari/9537.53";

    public static void main(String[] args) throws Exception {
        // Obtain a resource key for free at https://configure.51degrees.com?utm_source=code&utm_medium=example&utm_campaign=location-java&utm_content=geo-location.shell.examples-src-main-java-fiftyone-geolocation-examples-combiningservices.java&utm_term=main
        // Make sure to include the 'Country' and 'IsMobile' properties as they
        // are used by this example.
        String resourceKey = "!!Your resource license key!!";

        if (resourceKey.startsWith("!!")) {
            System.out.println("You need to create a resource key at " +
                "https://configure.51degrees.com?utm_source=code&utm_medium=example&utm_campaign=location-java&utm_content=geo-location.shell.examples-src-main-java-fiftyone-geolocation-examples-combiningservices.java&utm_term=resource-key-required and paste it into this example.");
            System.out.println("Make sure to include the 'Country' and " +
                "'IsMobile' properties as they are used by this example.");
        }
        else {
            CloudRequestEngine cloudRequestEngine =
                new CloudRequestEngineBuilder(loggerFactory)
                .setResourceKey(resourceKey)
                .build();
            AspectEngine<DeviceDataCloud, AspectPropertyMetaData> deviceDetectionEngine =
                new DeviceDetectionCloudEngineBuilder(loggerFactory)
                .build();
            AspectEngine<CloudGeoData, AspectPropertyMetaData> locationEngine =
                new GeoLocationCloudEngineBuilder(loggerFactory)
                .build(Enums.GeoLocationProvider.FiftyOneDegrees);

            Pipeline pipeline =
                new PipelineBuilder(loggerFactory)
                .addFlowElement(cloudRequestEngine)
                .addFlowElement(deviceDetectionEngine)
                .addFlowElement(locationEngine)
                .build();

            try (FlowData flowData = pipeline.createFlowData()) {
                flowData
                    .addEvidence(EVIDENCE_HEADER_USERAGENT_KEY, mobileUserAgent)
                    .addEvidence(EVIDENCE_GEO_LAT_PARAM_KEY, "51.458048")
                    .addEvidence(EVIDENCE_GEO_LON_PARAM_KEY, "-0.9822207999999999")
                    .process();

                AspectPropertyValue<String> country = flowData.get(GeoData.class).getCountry();

                AspectPropertyValue<Boolean> isMobile = flowData.get(DeviceData.class).getIsMobile();

                Future<?> future = flowData.get(GeoData.class).getProcessFuture();
                System.out.print("Awaiting response");
                outputUntilCancelled(".", 1000, future);
                System.out.println();
                System.out.println("Country: " + country.toString());
                System.out.println("IsMobile: " + isMobile.toString());
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
