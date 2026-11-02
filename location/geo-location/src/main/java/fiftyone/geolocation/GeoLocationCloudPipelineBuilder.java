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
import fiftyone.geolocation.data.CloudGeoData;
import fiftyone.geolocation.flowelements.GeoLocationCloudEngineBuilder;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngineBuilder;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.configuration.LazyLoadingConfiguration;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.CloudPipelineBuilderBase;
import fiftyone.pipeline.engines.services.HttpClient;
import org.slf4j.ILoggerFactory;

public class GeoLocationCloudPipelineBuilder
    extends CloudPipelineBuilderBase<GeoLocationCloudPipelineBuilder> {
    private final HttpClient httpClient;
    private final Enums.GeoLocationProvider geoLocationProvider;

    public GeoLocationCloudPipelineBuilder(
        ILoggerFactory loggerFactory,
        HttpClient httpClient,
        Enums.GeoLocationProvider geoLocationProvider) {
        super(loggerFactory);
        this.httpClient = httpClient;
        this.geoLocationProvider = geoLocationProvider;
    }

    @Override
    public Pipeline build() throws Exception {
        // Configure and build the cloud request engine
        CloudRequestEngineBuilder cloudRequestEngineBuilder =
            new CloudRequestEngineBuilder(loggerFactory, httpClient);
        if (lazyLoading) {
            cloudRequestEngineBuilder.setLazyLoading(new LazyLoadingConfiguration(
                (int)lazyLoadingTimeoutMillis));
        }
        if (resultsCache) {
            cloudRequestEngineBuilder.setCache(
                new CacheConfiguration(resultsCacheSize));
        }
        if (url != null && url.isEmpty() == false) {
            cloudRequestEngineBuilder.setEndpoint(url);
        }
        if (propertiesEndpoint != null && propertiesEndpoint.isEmpty() == false) {
            cloudRequestEngineBuilder.setPropertiesEndpoint(propertiesEndpoint);
        }
        if (resourceKey != null && resourceKey.isEmpty() == false) {
            cloudRequestEngineBuilder.setResourceKey(resourceKey);
        }
        if (licenseKey != null && licenseKey.isEmpty() == false) {
            cloudRequestEngineBuilder.setLicenseKey(licenseKey);
        }
        if (cloudRequestOrigin != null && cloudRequestOrigin.isEmpty() == false) {
            cloudRequestEngineBuilder.setCloudRequestOrigin(cloudRequestOrigin);
        }
        CloudRequestEngine cloudRequestEngine = cloudRequestEngineBuilder.build();

        AspectEngine<CloudGeoData, AspectPropertyMetaData> geoLocationEngine = null;
        GeoLocationCloudEngineBuilder nomEngineBuilder =
            new GeoLocationCloudEngineBuilder(loggerFactory);
        if (lazyLoading) {
            nomEngineBuilder.setLazyLoading(new LazyLoadingConfiguration(
                (int)lazyLoadingTimeoutMillis));
        }
        geoLocationEngine = nomEngineBuilder.build(geoLocationProvider);

        flowElements.add(cloudRequestEngine);
        flowElements.add(geoLocationEngine);

        // Build and return the pipeline
        return super.build();
    }
}
