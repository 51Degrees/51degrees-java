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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.pipeline.annotations.DefaultValue;
import org.slf4j.ILoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for pipeline builders that will produce a pipeline with specific
 * flow elements.
 * @param <TBuilder> the builder type
 */
public abstract class CloudPipelineBuilderBase<
    TBuilder extends CloudPipelineBuilderBase<TBuilder>>
    extends PrePackagedPipelineBuilderBase<TBuilder> {

    /** The cloud service endpoint, or empty for the default. */
    protected String url = "";

    /** The data endpoint, or empty for the default. */
    protected String dataEndpoint = "";

    /** The properties endpoint, or empty for the default. */
    protected String propertiesEndpoint = "";

    /** The evidence keys endpoint, or empty for the default. */
    protected String evidenceKeysEndpoint = "";

    /** The resource key sent with each request. */
    protected String resourceKey = "";

    /** The license key sent with each request. */
    protected String licenseKey = "";

    /**
     * The properties to ask the cloud service for, or empty for all the key
     * gives.
     */
    protected final List<String> requestedProperties = new ArrayList<>();

    /**
     * The value of the Origin header sent with each request, or empty for none.
     */
    protected String cloudRequestOrigin = "";

    /**
     * Construct a new instance.
     * @param loggerFactory the {@link ILoggerFactory} used to create any
     *                      loggers required by instances being built by the
     *                      builder
     */
    public CloudPipelineBuilderBase(ILoggerFactory loggerFactory) {
        super(loggerFactory);
    }

    /**
     * Set the endpoint to use when calling the cloud service. This will also
     * set the data, properties, and evidence keys endpoints using this as the
     * base URL.
     * @param url endpoint URL
     * @return this builder
     */
    @DefaultValue("https://cloud.51degrees.com/api/v4/, the Cloud Request " +
        "Engine's default")
    public TBuilder setEndPoint(String url) {
        if (url.endsWith("/") == false) {
            url += '/';
        }
        this.url = url;
        return self();
    }

    /**
     * Set the URL for the data endpoint to be called during processing.
     * @param url data URL
     * @return this builder
     */
    @DefaultValue("The endpoint followed by {resourcekey}.json")
    public TBuilder setDataEndpoint(String url) {
        this.dataEndpoint = url;
        return self();
    }

    /**
     * Set the URL for the properties endpoint to be called when setting up the
     * engine.
     * @param propertiesEndpoint properties URL
     * @return this builder
     */
    @DefaultValue("The endpoint followed by accessibleproperties")
    public TBuilder setPropertiesEndpoint(String propertiesEndpoint) {
        this.propertiesEndpoint = propertiesEndpoint;
        return self();
    }

    /**
     * Set the URL for the evidence keys endpoint to be called when setting up
     * the engine.
     * @param evidenceKeysEndpoint evidence keys URL
     * @return this builder
     */
    @DefaultValue("The endpoint followed by evidencekeys")
    public TBuilder setEvidenceKeysEndpoint(String evidenceKeysEndpoint) {
        this.evidenceKeysEndpoint = evidenceKeysEndpoint;
        return self();
    }

    /**
     * Set the resource key to be used when calling the endpoints.
     * @param key resource key
     * @return this builder
     */
    @DefaultValue("No default - a resource key or a license key must be " +
        "supplied")
    public TBuilder setResourceKey(String key) {
        this.resourceKey = key;
        return self();
    }

    /**
     * Set the license key to be used when calling the endpoints. A license
     * key used without a resource key needs the properties wanted, named
     * with {@link #setRequestedProperties(List)}. Beside a resource key it
     * adds the requested properties the license grants to what the resource
     * key carries.
     * @param key license key
     * @return this builder
     */
    @DefaultValue("No default")
    public TBuilder setLicenseKey(String key) {
        this.licenseKey = key;
        return self();
    }

    /**
     * Name the properties each request asks the cloud service for, as fully
     * qualified names such as {@code device.ismobile}. Required with a
     * license key alone. Beside a resource key the cloud service adds them
     * to the properties the resource key carries.
     * @param properties the fully qualified property names
     * @return this builder
     */
    @DefaultValue("None - required with a license key alone")
    public TBuilder setRequestedProperties(List<String> properties) {
        if (properties == null) {
            throw new IllegalArgumentException(
                "properties must not be null");
        }
        requestedProperties.addAll(properties);
        return self();
    }

    /**
     * As {@link #setRequestedProperties(List)}, taking the names as one
     * comma separated string.
     * @param properties comma separated fully qualified property names
     * @return this builder
     */
    @DefaultValue("None - required with a license key alone")
    public TBuilder setRequestedProperties(String properties) {
        if (properties == null) {
            throw new IllegalArgumentException(
                "properties must not be null");
        }
        return setRequestedProperties(Arrays.asList(properties.split(",")));
    }

    /**
     * The value to set for the Origin header when making requests
     * to the cloud service.
     * This is used by the cloud service to check that the request
     * is being made from a origin matching those allowed by the 
     * resource key.
     * For more detail, see the 'Request Headers' section in the 
     * <a href="https://cloud.51degrees.com/api-docs/index.html">cloud documentation</a>.
     * @param cloudRequestOrigin the value to set the origin header to
     * @return this builder
     */
    @DefaultValue("None")
    public TBuilder setCloudRequestOrigin(String cloudRequestOrigin) {
        this.cloudRequestOrigin = cloudRequestOrigin;
        return self();
    }
}
