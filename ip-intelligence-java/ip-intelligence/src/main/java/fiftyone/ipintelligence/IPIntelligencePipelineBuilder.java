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

import fiftyone.pipeline.engines.services.DataUpdateService;
import fiftyone.pipeline.engines.services.DataUpdateServiceDefault;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Builder used to create a Pipeline with a IP Intelligence engine.
 */
public class IPIntelligencePipelineBuilder {

    protected final ILoggerFactory loggerFactory;
    private final DataUpdateService dataUpdateService;
    private final HttpClient httpClient;

    /**
     * Constructor
     */
    public IPIntelligencePipelineBuilder() {
        this(LoggerFactory.getILoggerFactory());
    }

    /**
     * Constructor
     * @param loggerFactory The factory to use for creating loggers within the 
     * pipeline.
     */
    public IPIntelligencePipelineBuilder(
            ILoggerFactory loggerFactory) {
        this(loggerFactory, new HttpClientDefault());
    }

    /**
     * Constructor
     * @param loggerFactory The factory to use for creating loggers within the 
     * pipeline.
     * @param httpClient The HTTP Client to use within the pipeline.
     */
    public IPIntelligencePipelineBuilder(
        ILoggerFactory loggerFactory,
        HttpClient httpClient) {
        this(loggerFactory, httpClient, new DataUpdateServiceDefault(
            loggerFactory.getLogger(DataUpdateServiceDefault.class.getName()),
            httpClient));
    }
    
    /**
     * Constructor
     * @param loggerFactory The factory to use for creating loggers within the 
     * pipeline.
     * @param httpClient The HTTP Client to use within the pipeline.
     * @param dataUpdateService The DataUpdateService to use when checking for 
     * data updates.
     */
    public IPIntelligencePipelineBuilder(
        ILoggerFactory loggerFactory,
        HttpClient httpClient,
        DataUpdateService dataUpdateService) {
        this.httpClient = httpClient;
        this.loggerFactory = loggerFactory;
        this.dataUpdateService = dataUpdateService;
    }

    /**
     * Use a 51Degrees on-premise IP Intelligence engine to
     * perform IP Intelligence.
     * @param datafile The full path to the IP Intelligence data file.
     * @param createTempDataCopy If true, the engine will create a temporary 
     * copy of the data file rather than using the data file directly.
     * @return A builder that can be used to configure and build a pipeline
     * that will use the on-premise IP intelligence engine.
     * @throws Exception Thrown if a required parameter is null.
     */
    public IPIntelligenceOnPremisePipelineBuilder useOnPremise(
        String datafile,
        boolean createTempDataCopy) throws Exception {
        IPIntelligenceOnPremisePipelineBuilder builder =
            new IPIntelligenceOnPremisePipelineBuilder(
                loggerFactory,
                dataUpdateService);
        builder.setFilename(datafile, createTempDataCopy);
        return builder;
    }

    /**
     * Use the 51Degrees Cloud service to perform IP Intelligence.
     * @param resourceKey The resource key to use when querying the Cloud service. 
     * Obtain one from https://configure.51degrees.com?utm_source=code&amp;utm_medium=comment&amp;utm_campaign=ip-intelligence-java&amp;utm_content=ip-intelligence-src-main-java-fiftyone-ipintelligence-ipintelligencepipelinebuilder.java&amp;utm_term=usecloud
     * @return A builder that can be used to configure and build a pipeline
     * that will use the Cloud IP Intelligence engine.
     */
    public IPIntelligenceCloudPipelineBuilder useCloud(String resourceKey) {
        IPIntelligenceCloudPipelineBuilder builder =
            new IPIntelligenceCloudPipelineBuilder(loggerFactory, httpClient);
        builder.setResourceKey(resourceKey);
        return builder;
    }
}
