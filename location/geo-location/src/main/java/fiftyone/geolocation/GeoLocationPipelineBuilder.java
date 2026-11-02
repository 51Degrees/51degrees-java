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

package fiftyone.geolocation;

import fiftyone.geolocation.core.Enums;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

public class GeoLocationPipelineBuilder {
    private ILoggerFactory loggerFactory;
    private HttpClient httpClient;

    public GeoLocationPipelineBuilder() {
        this(LoggerFactory.getILoggerFactory());
    }

    /**
     * Constructor
     * @param loggerFactory the factory to use for creating loggers within the
     *                      pipeline
     */
    public GeoLocationPipelineBuilder(
        ILoggerFactory loggerFactory) {
        this(loggerFactory, new HttpClientDefault());
    }

    /**
     * Constructor
     * @param loggerFactory the factory to use for creating loggers within the
     *                      pipeline
     * @param httpClient the HTTP client to use within the pipeline
     */
    public GeoLocationPipelineBuilder(
        ILoggerFactory loggerFactory,
        HttpClient httpClient) {
        this.loggerFactory = loggerFactory;
        this.httpClient = httpClient;
    }

    /**
     * Use the 51Degrees cloud service to perform Geo Location.
     * @param resourceKey the resource key to use when querying the
     *                    service.
     *                    Obtain one from https://configure.51degrees.com?utm_source=code&utm_medium=comment&utm_campaign=location-java&utm_content=geo-location-src-main-java-fiftyone-geolocation-geolocationpipelinebuilder.java&utm_term=usecloud
     * @param provider the Geo Location provider to use
     * @return builder that can be used to configure and build a pipeline
     * that will use the cloud Geo Location engine
     * </returns>
     */
    public GeoLocationCloudPipelineBuilder useCloud(
        String resourceKey,
        Enums.GeoLocationProvider provider) {
        return new GeoLocationCloudPipelineBuilder(loggerFactory, httpClient, provider)
            .setResourceKey(resourceKey);
    }

    /**
     * Use the 51Degrees cloud service to perform Geo Location
     * @param resourceKey the resource to use when querying the cloud service.
     *                    Obtain one from https://configure.51degrees.com?utm_source=code&utm_medium=comment&utm_campaign=location-java&utm_content=geo-location-src-main-java-fiftyone-geolocation-geolocationpipelinebuilder.java&utm_term=usecloud-2
     * @param endpoint the 51Degrees cloud URL
     * @param provider the Geo Location provider to use
     * @return a builder that can be used to configure and build a pipeline that
     * will use the cloud Geo Location engine
     */
    public GeoLocationCloudPipelineBuilder useCloud(
        String resourceKey,
        String endpoint,
        Enums.GeoLocationProvider provider) {
        return useCloud(resourceKey, provider)
            .setEndPoint(endpoint);
    }
}
