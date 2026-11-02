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

package fiftyone.ipintelligence;

import fiftyone.ipintelligence.cloud.flowelements.IPIntelligenceCloudEngineBuilder;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngineBuilder;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.configuration.LazyLoadingConfiguration;
import fiftyone.pipeline.engines.flowelements.CloudPipelineBuilderBase;
import fiftyone.pipeline.engines.services.HttpClient;
import org.slf4j.ILoggerFactory;

/**
 * Builder used to create pipelines with a cloud-based
 * IP Intelligence engine.
 */
public class IPIntelligenceCloudPipelineBuilder
    extends CloudPipelineBuilderBase<IPIntelligenceCloudPipelineBuilder> {

    private final HttpClient httpClient;

    /**
     * Internal Constructor.
     * This builder should only be created through the 
     * IPIntelligencePipelineBuilder
     * @param loggerFactory
     * @param httpClient 
     */
    public IPIntelligenceCloudPipelineBuilder(
        ILoggerFactory loggerFactory,
        HttpClient httpClient) {
        super(loggerFactory);
        this.httpClient = httpClient;
    }

    /**
     * Build the pipeline using the configured values.
     * @return A new pipeline instance that contains a cloud request engine for 
     * making requests to the 51Degrees Cloud service and a 
     * IPIntelligenceCloudEngine to interpret the IP Intelligence results.
     * @throws Exception 
     */
    @Override
    public Pipeline build() throws Exception {
        // Configure and build the cloud request engine
        CloudRequestEngineBuilder cloudRequestEngineBuilder =
            new CloudRequestEngineBuilder(loggerFactory, httpClient);
        if (lazyLoading) {
            cloudRequestEngineBuilder.setLazyLoading(new LazyLoadingConfiguration(
                (int) lazyLoadingTimeoutMillis));
        }
        if (resultsCache) {
            cloudRequestEngineBuilder.setCache(
                new CacheConfiguration(resultsCacheSize));
        }
        if (url != null && url.isEmpty() == false) {
            cloudRequestEngineBuilder.setEndpoint(url);
        }
        if (dataEndpoint != null && dataEndpoint.isEmpty() == false) {
            cloudRequestEngineBuilder.setDataEndpoint(dataEndpoint);
        }
        if (propertiesEndpoint != null && propertiesEndpoint.isEmpty() == false) {
            cloudRequestEngineBuilder.setPropertiesEndpoint(propertiesEndpoint);
        }
        if (evidenceKeysEndpoint != null && evidenceKeysEndpoint.isEmpty() == false){
            cloudRequestEngineBuilder.setEvidenceKeysEndpoint(evidenceKeysEndpoint);
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

        // Configure and build the IP Intelligence engine
        IPIntelligenceCloudEngineBuilder ipIntelligenceEngineBuilder =
            new IPIntelligenceCloudEngineBuilder(loggerFactory);
        if (lazyLoading) {
            ipIntelligenceEngineBuilder.setLazyLoading(new LazyLoadingConfiguration(
                (int) lazyLoadingTimeoutMillis));
        }

        // Add the elements to the list
        flowElements.add(cloudRequestEngine);
        flowElements.add(ipIntelligenceEngineBuilder.build());

        // Build and return the pipeline
        return super.build();
    }
}
