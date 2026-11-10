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

package fiftyone.pipeline.cloudrequestengine.flowelements;


import fiftyone.pipeline.annotations.DefaultValue;
import fiftyone.pipeline.annotations.ElementBuilder;
import fiftyone.pipeline.cloudrequestengine.Constants.Messages;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.configuration.CacheConfiguration;
import fiftyone.pipeline.engines.flowelements.AspectEngineBuilderBase;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.slf4j.ILoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Builder for the {@link CloudRequestEngine}.
 */
@ElementBuilder
public class CloudRequestEngineBuilder extends
    AspectEngineBuilderBase<CloudRequestEngineBuilder, CloudRequestEngine> {
	
    private final HttpClient httpClient;

    private String endPoint = null;
    private String dataEndpoint = null;
    private String propertiesEndpoint = null;
    private String evidenceKeysEndpoint = null;
    private String resourceKey = null;
    private String licenseKey = null;
    private final List<String> requestedProperties = new ArrayList<>();
    private String cloudRequestOrigin = null;
    private int timeoutMillis = Constants.DEFAULT_TIMEOUT_MILLIS;
    
    // Function to get environment variable value. This enable testable code.
    private Function<String, String> getEnvVar = (name) -> {
    	return System.getenv(name);
    };

    public CloudRequestEngineBuilder(ILoggerFactory loggerFactory) {
        this(loggerFactory, new HttpClientDefault());
    }

    public CloudRequestEngineBuilder(
        ILoggerFactory loggerFactory,
        HttpClient httpClient) {
        super(loggerFactory);
        this.httpClient = httpClient;
    }

    @Override
    protected CloudRequestEngine newEngine(List<String> properties) throws Exception {
        String trimmedResourceKey = trimmed(resourceKey);
        String trimmedLicenseKey = trimmed(licenseKey);
        checkCredentials(
            trimmedResourceKey,
            trimmedLicenseKey,
            requestedProperties);

        /*
         *  Check if endPoint has been explicitly set via setEndpoint.
         *  If not check for environment variable FOD_CLOUD_API_URL.
         *  If nothing else is set, use default value.
         */
        String explicitDataEndpoint = dataEndpoint;
        if (endPoint == null || endPoint.isEmpty()) {
        	String envVarEndPoint = getEnvVar.apply(
        		Constants.FOD_CLOUD_API_URL);
        	if (envVarEndPoint == null || envVarEndPoint.isEmpty()) {
        		setEndpoint(Constants.END_POINT_DEFAULT);
        	}
        	else {
        		setEndpoint(envVarEndPoint);
        	}
        }
        dataEndpoint = explicitDataEndpoint;
        
        // The route of the data endpoint carries the resource key, which a
        // license key alone does not have, so it is worked out here once
        // every setting is known.
        String data = dataEndpoint != null ? dataEndpoint :
            endPoint + (trimmedResourceKey.isEmpty() ?
                "" : trimmedResourceKey + ".") + "json";

        return new CloudRequestEngineDefault(
            loggerFactory.getLogger(CloudRequestEngine.class.getName()),
            new CloudRequestDataFactory(loggerFactory),
            httpClient,
            data,
            trimmedResourceKey,
            trimmedLicenseKey,
            propertiesEndpoint,
            evidenceKeysEndpoint,
            timeoutMillis,
            cloudRequestOrigin,
            requestedProperties);
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Refuse a combination of keys and requested properties that the cloud
     * service would refuse on every request, so that a deployment sees one
     * configuration error when the engine is built.
     */
    private static void checkCredentials(
        String resourceKey,
        String licenseKey,
        List<String> requestedProperties) {
        boolean hasResourceKey = resourceKey.isEmpty() == false;
        boolean hasLicenseKey = licenseKey.isEmpty() == false;
        boolean hasProperties = requestedProperties.isEmpty() == false;

        if (hasResourceKey == false && hasLicenseKey == false) {
            throw new PipelineConfigurationException(
                Messages.NoCredential);
        }
        if (hasResourceKey == false && hasProperties == false) {
            throw new PipelineConfigurationException(
                Messages.LicenseKeyNeedsProperties);
        }
        if (hasResourceKey && hasProperties) {
            throw new PipelineConfigurationException(
                Messages.PropertiesIgnoredWithResourceKey);
        }
    }
    
    private String getEnvironmentVariable(String name) {
    	return System.getenv(name);
    }

    public CloudRequestEngine build() throws Exception {
        return buildEngine();
    }

    /**
     * The root endpoint which the CloudRequestsEngine will query. This will set
     * the data, properties and evidence keys endpoints.
     * <p>
     * By default, "https://cloud.51degrees.com/api/v4"
     * @param uri root endpoint
     * @return this builder
     */
    @DefaultValue(Constants.END_POINT_DEFAULT)
    public CloudRequestEngineBuilder setEndpoint(String uri) {
        if (uri.endsWith("/") == false) {
            uri += '/';
        }
        endPoint = uri;
        dataEndpoint = null;
        return setPropertiesEndpoint(uri + "accessibleproperties")
            .setEvidenceKeysEndpoint(uri + "evidencekeys");

    }

    /**
     * The endpoint the CloudRequestEngine will query to get a processing result.
     * <p>
     * By default, this is the endpoint value suffixed with the resourcekey and
     * ".json", or with "json" alone when only a license key is set
     * @param uri data endpoint
     * @return this builder
     */
    @DefaultValue(Constants.END_POINT_DEFAULT + "{resourcekey}.json")
    public CloudRequestEngineBuilder setDataEndpoint(String uri) {
        dataEndpoint = uri;
        return this;
    }

    /**
     * The endpoint the cloudRequestEngine will query to get the available
     * properties.
     * <p>
     * By default, this is the endpoint value suffixed with "accessibleproperties"
     * @param uri properties endpoint
     * @return this builder
     */
    @DefaultValue(Constants.END_POINT_DEFAULT + "accessibleproperties")
    public CloudRequestEngineBuilder setPropertiesEndpoint(String uri) {
        propertiesEndpoint = uri;
        return this;
    }

    /**
     * The endpoint the cloudRequestEngine will query to get the required
     * evidence keys.
     * <p>
     * By default, this is the endpoint value suffixed with "evidencekeys"
     * @param uri evidence keys endpoint
     * @return this builder
     */
    @DefaultValue(Constants.END_POINT_DEFAULT + "evidencekeys")
    public CloudRequestEngineBuilder setEvidenceKeysEndpoint(String uri) {
        evidenceKeysEndpoint = uri;
        return this;
    }

    /**
     * The resource key to query the endpoint with.
     * <p>
     * No default. Either a resource key or a license key must be supplied.
     * @param resourceKey resource key
     * @return this builder
     */
    @DefaultValue("No default - a resource key or a license key must be supplied")
    public CloudRequestEngineBuilder setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
        return this;
    }

    /**
     * The license key to query the endpoint with.
     * <p>
     * A license key used without a resource key must be accompanied by the
     * properties wanted, set with {@link #setRequestedProperties(List)}.
     * There is no default
     * @param licenseKey license key
     * @return this builder
     */
    @DefaultValue("No default")
    public CloudRequestEngineBuilder setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
        return this;
    }

    /**
     * The fully qualified names of the properties each request asks the
     * cloud service for, for example {@code device.ismobile}. Names are
     * trimmed and lower-cased, and blank names are ignored.
     * <p>
     * The cloud service honors the list only when a license key is the sole
     * credential, and it is required then. A list set alongside a resource
     * key is refused when the engine is built.
     * <p>
     * By default no properties are requested
     * @param properties the fully qualified property names
     * @return this builder
     */
    @DefaultValue("None")
    public CloudRequestEngineBuilder setRequestedProperties(
        List<String> properties) {
        requestedProperties.clear();
        for (String property : properties) {
            String name = trimmed(property).toLowerCase(Locale.ROOT);
            if (name.isEmpty() == false) {
                requestedProperties.add(name);
            }
        }
        return this;
    }

    /**
     * As {@link #setRequestedProperties(List)}, taking the names as one
     * comma separated string.
     * @param properties comma separated fully qualified property names
     * @return this builder
     */
    @DefaultValue("None")
    public CloudRequestEngineBuilder setRequestedProperties(
        String properties) {
        return setRequestedProperties(
            Arrays.asList(trimmed(properties).split(",")));
    }

    /**
     * Timeout in seconds for the request to the endpoint.
     * <p>
     * Default value is 100 seconds
     * @param timeout in seconds
     * @return this builder
     */
    @DefaultValue(intValue = Constants.DEFAULT_TIMEOUT_MILLIS / 1000)
    public CloudRequestEngineBuilder setTimeOutSeconds(int timeout) {
        this.timeoutMillis = timeout * 1000;
        return this;
    }

    /**
     * The value to set for the Origin header when making requests
     * to the cloud service.
     * This is used by the cloud service to check that the request
     * is being made from a origin matching those allowed by the 
     * resource key.
     * For more detail, see the 'Request Headers' section in the 
     * <a href="https://cloud.51degrees.com/api-docs/index.html">cloud documentation</a>.
     * <p>
     * There is no default value
     * @param cloudRequestOrigin The value to use for the Origin header.
     * @return this builder
     */
    @DefaultValue("None")
    public CloudRequestEngineBuilder setCloudRequestOrigin(String cloudRequestOrigin) {
        this.cloudRequestOrigin = cloudRequestOrigin;
        return this;
    }

    private static class CloudRequestDataFactory
        implements ElementDataFactory<CloudRequestData> {

        private final ILoggerFactory loggerFactory;

        public CloudRequestDataFactory(ILoggerFactory loggerFactory) {
            this.loggerFactory = loggerFactory;
        }

        @Override
        public CloudRequestData create(
            FlowData flowData,
            FlowElement<CloudRequestData, ?> engine) {
            return new CloudRequestData(
                loggerFactory.getLogger(CloudRequestData.class.getName()),
                flowData,
                (CloudRequestEngine) engine);
        }
    }


    /**
     * Configure the size of a {@link fiftyone.caching.LruPutCache} cache to use.
     * <p>
     * Default is that there is no cache unless one is configured using this method
     * or by using {@link AspectEngineBuilderBase#setCache(CacheConfiguration)} - or
     * if it is set in a pipelineBuilder e.g.
     * {@link fiftyone.pipeline.engines.flowelements.PrePackagedPipelineBuilderBase#useResultsCache()}
     * @param size the size of the cache
     * @return this builder
     */
    @DefaultValue("No cache")
    public CloudRequestEngineBuilder setCacheSize(int size) {
        this.cacheConfig = new CacheConfiguration(size);
        return this;
    }
}
