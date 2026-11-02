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

import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngineBuilder;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.DataUpdateUrlFormatter;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageBuilder;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.PrePackagedPipelineBuilderBase;
import fiftyone.pipeline.engines.services.DataUpdateService;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder used to create pipelines with an on-premise
 * IP Intelligence engine.
 */
public class IPIntelligenceOnPremisePipelineBuilder
    extends PrePackagedPipelineBuilderBase<IPIntelligenceOnPremisePipelineBuilder> {

    protected boolean shareUsageEnabled = true;
    private String filename;
    private boolean createTempDataCopy;
    private int concurrency = -1;
    private final List<String> properties = new ArrayList<String>();
    private Boolean autoUpdateEnabled = null;
    private Boolean dataFileSystemWatcher = null;
    private Boolean dataUpdateOnStartup = null;
    private Long updatePollingInterval = null;
    private Long updateRandomisationMax = null;
    private String dataUpdateLicenseKey = null;
    private String dataUpdateUrl = null;
    private Boolean dataUpdateVerifyMd5 = null;
    private DataUpdateUrlFormatter dataUpdateUrlFormatter = null;
    private Constants.PerformanceProfiles performanceProfile =
        Constants.PerformanceProfiles.MaxPerformance;

    private DataUpdateService dataUpdateService;

    IPIntelligenceOnPremisePipelineBuilder(
        DataUpdateService dataUpdateService) {
        this(
            LoggerFactory.getILoggerFactory(),
            dataUpdateService);
    }

    IPIntelligenceOnPremisePipelineBuilder(
        ILoggerFactory loggerFactory,
        DataUpdateService dataUpdateService) {
        super(loggerFactory);
        this.dataUpdateService = dataUpdateService;
        // Make sure to add dataUpdateService to the list of managed services
        this.addService(dataUpdateService);
    }

    /**
     * Set the filename of the IP Intelligence data file that the
     * engine should use.
     * @param filename The data file.
     * @param createTempDataCopy If true, the engine will create a temporary
     * copy of the data file rather than using the data file directly.
     * @return This builder instance.
     * @throws Exception Thrown if the filename has an unknown extension.
     */
    IPIntelligenceOnPremisePipelineBuilder setFilename(
        String filename,
        boolean createTempDataCopy) throws Exception {
        this.filename = filename;
        this.createTempDataCopy = createTempDataCopy;
        if (!filename.endsWith(".ipi")) {
            throw new Exception("Unrecognised filename. " +
                "Expected a '*.ipi' IP Intelligence data file.");
        }
        return this;
    }

    /**
     * Set share usage enabled/disabled.
     * Defaults to enabled.
     * @param enabled True to enable usage sharing. False to disable.
     * @return This builder instance.
     */
    public IPIntelligenceOnPremisePipelineBuilder setShareUsage(
        boolean enabled) {
        shareUsageEnabled = enabled;
        return this;
    }

    /**
     * Enable/Disable auto update.
     * Defaults to enabled.
     * If enabled, the auto update system will automatically download
     * and apply new data files for IP Intelligence.
     * @param enabled True to enable auto update. False to disable.
     * @return This builder instance.
     */
    public IPIntelligenceOnPremisePipelineBuilder setAutoUpdate(
        boolean enabled) {
        autoUpdateEnabled = enabled;
        return this;
    }

    /**
     * The DataUpdateService has the ability to watch a 
     * file on disk and refresh the engine as soon as that file is 
     * updated.
     * This setting enables/disables that feature.
     * @param enabled True to enable file system watcher. False to disable.
     * @return This builder instance.
     */
    public IPIntelligenceOnPremisePipelineBuilder setDataFileSystemWatcher(
            boolean enabled) {
        dataFileSystemWatcher = enabled;
        return this;
    }

    /**
     * Automatic updates require a {@link DataUpdateService}.
     * @param dataUpdateService an instance of a dataUpdateService
     * @return This builder instance.
     */
    public IPIntelligenceOnPremisePipelineBuilder setDataUpdateService(
            DataUpdateService dataUpdateService) {
        this.dataUpdateService = dataUpdateService;
        return this;
    }

    /**
     * Enable/Disable update on startup.
     * Defaults to enabled.
     * If enabled, the auto update system will be used to check for
     * an update before the IP Intelligence engine is created.
     * If an update is available, it will be downloaded and applied
     * before the pipeline is built and returned for use so this may 
     * take some time.
     * @param enabled True to enable update on startup. False to disable.
     * @return This builder instance.
     */
    public IPIntelligenceOnPremisePipelineBuilder setDataUpdateOnStartup(
        boolean enabled) {
        dataUpdateOnStartup = enabled;
        return this;
    }
    
    /**
     * Set the time between checks for a new data file made by the 
     * DataUpdateService in seconds.
     * Default = 30 minutes.
     * @param pollingIntervalSeconds The number of seconds between checks.
     * @return This builder instance.
     */
    public IPIntelligenceOnPremisePipelineBuilder setUpdatePollingInterval(
        int pollingIntervalSeconds) {
        
        updatePollingInterval = (long)pollingIntervalSeconds * 1000;
        return this;
    }
    
    /**
     * Set the time between checks for a new data file made by the 
     * DataUpdateService in milliseconds.
     * @param pollingIntervalMillis The number of milliseconds between checks.
     * @return This builder instance.
     */
    public IPIntelligenceOnPremisePipelineBuilder setUpdatePollingIntervalMillis(
        long pollingIntervalMillis) {
        updatePollingInterval = pollingIntervalMillis;
        return this;
    }
    
    /**
     * A random element can be added to the DataUpdateService polling interval.
     * This option sets the maximum length of this random addition.
     * Default = 10 minutes.
     * @param randomisationMaxSeconds The maximum time added to the data update 
     * polling interval in seconds.
     * @return This builder instance.
     */
    public IPIntelligenceOnPremisePipelineBuilder setUpdateRandomisationMax(
        int randomisationMaxSeconds) {
        updateRandomisationMax = (long)randomisationMaxSeconds * 1000;
        return this;
    }
    
    /**
     * A random element can be added to the DataUpdateService polling interval.
     * This option sets the maximum length of this random addition.
     * Default = 10 minutes.
     * @param randomisationMaxMillis The maximum time added to the data update 
     * polling interval in milliseconds.
     * @return This builder instance.
     */
    public IPIntelligenceOnPremisePipelineBuilder setUpdateRandomisationMaxMillis(
        long randomisationMaxMillis) {
        updateRandomisationMax = randomisationMaxMillis;
        return this;
    }

    /**
     * Set the license key used when checking for new 
     * IP Intelligence data files.
     * Defaults to null.
     * @param key The license key.
     * @return This builder instance.
     */
    public IPIntelligenceOnPremisePipelineBuilder setDataUpdateLicenseKey(
        String key) {
        dataUpdateLicenseKey = key;
        return this;
    }

    /**
     * Set the performance profile for the IP Intelligence engine.
     * Defaults to MaxPerformance.
     * @param profile The performance profile to use.
     * @return This builder instance.
     */
    public IPIntelligenceOnPremisePipelineBuilder setPerformanceProfile(
        Constants.PerformanceProfiles profile) {
        performanceProfile = profile;
        return this;
    }

    /**
     * Set the expected number of concurrent operations using the engine.
     * This sets the concurrency of the internal caches to avoid excessive
     * locking.
     * @param concurrency expected concurrent accesses
     * @return this builder
     */
    public IPIntelligenceOnPremisePipelineBuilder setConcurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    /**
     * Add a property to the list of properties that the engine will populate in
     * the response. By default all properties will be populated.
     * @param property the property that we want the engine to populate
     * @return this builder
     */
    public IPIntelligenceOnPremisePipelineBuilder setProperty(String property) {
        this.properties.add(property);
        return this;
    }

    /**
     * Configure the engine to use the specified URL when looking for
     * an updated data file.
     * <p>
     * Default is the 51Degrees update URL
     *
     * @param url the URL to check for a new data file
     * @return this builder
     */
    public IPIntelligenceOnPremisePipelineBuilder setDataUpdateUrl(String url) {
        dataUpdateUrl = url;
        return this;
    }

    /**
     * Set a value indicating if the {@link DataUpdateService} should expect the
     * response from the data update URL to contain a 'content-md5' HTTP header
     * that can be used to verify the integrity of the content.
     * <p>
     * Default true
     *
     * @param verify true if the content should be verified with the Md5 hash.
     *               False otherwise
     * @return this builder
     */
    public IPIntelligenceOnPremisePipelineBuilder setDataUpdateVerifyMd5(Boolean verify) {
        dataUpdateVerifyMd5 = verify;
        return this;
    }

    /**
     * Specify a {@link DataUpdateUrlFormatter} to be used by the
     * {@link DataUpdateService} when building the complete URL to query for
     * updated data.
     *
     * @param formatter the formatter to use
     * @return this builder
     */
    public IPIntelligenceOnPremisePipelineBuilder setDataUpdateUrlFormatter(DataUpdateUrlFormatter formatter) {
        dataUpdateUrlFormatter = formatter;
        return this;
    }

    /**
     * Build and return a pipeline that can perform IP Intelligence.
     * @return the built pipeline
     * @throws Exception on error
     */
    @Override
    public Pipeline build() throws Exception {
        AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> ipIntelligenceEngine;

        IPIntelligenceOnPremiseEngineBuilder engineBuilder =
                new IPIntelligenceOnPremiseEngineBuilder(loggerFactory, dataUpdateService);
        ipIntelligenceEngine = configureAndBuild(engineBuilder);

        if (ipIntelligenceEngine != null) {
            // Add the share usage element to the list if enabled
            if (shareUsageEnabled) {
                getFlowElements().add(
                    new ShareUsageBuilder(loggerFactory).build());
            }
            // Add the IP Intelligence engine to the list
            getFlowElements().add(ipIntelligenceEngine);
        } else {
            throw new RuntimeException("Unexpected error creating IP Intelligence engine.");
        }

        setAutoCloseElements(true);

        // Create and return the pipeline
        return super.build();
    }

    /**
     * Private method used to set configuration options on the
     * engine builder and build the engine.
     * @param builder The builder to configure.
     * @return A new IP Intelligence engine instance.
     * @throws Exception 
     */
    private IPIntelligenceOnPremiseEngine configureAndBuild(
        IPIntelligenceOnPremiseEngineBuilder builder) throws Exception {
        // Configure auto update.
        if(autoUpdateEnabled != null) {
            builder.setAutoUpdate(autoUpdateEnabled);
        }
        // Configure file system watcher.
        if(dataFileSystemWatcher != null) {
            builder.setDataFileSystemWatcher(dataFileSystemWatcher);
        }
        // Configure update on startup.
        if(dataUpdateOnStartup != null) {
            builder.setDataUpdateOnStartup(dataUpdateOnStartup);
        }
        // Configure update polling interval.
        if(updatePollingInterval != null) {
            builder.setUpdatePollingIntervalMillis(updatePollingInterval);
        }
        // Configure update polling interval randomisation.
        if(updateRandomisationMax != null) {
            builder.setUpdateRandomisationMaxMillis(updateRandomisationMax);
        }
        // Configure data update license key.
        if (dataUpdateLicenseKey != null) {
            builder.setDataUpdateLicenseKey(dataUpdateLicenseKey);
        }
        // Configure the available properties.
        if (properties.size() > 0) {
            for (String property : properties) {
                builder.setProperty(property);
            }
        }
        // Configure performance profile
        builder.setPerformanceProfile(performanceProfile);

        // Configure the concurrency
        if (concurrency > 1) {
            builder.setConcurrency(concurrency);
        }
        // Configure update url
        if (dataUpdateUrl != null) {
            builder.setDataUpdateUrl(dataUpdateUrl);
        }
        // Configure md5 verification
        if (dataUpdateVerifyMd5 != null) {
            builder.setDataUpdateVerifyMd5(dataUpdateVerifyMd5);
        }
        // Configure data url formatter
        if (dataUpdateUrlFormatter != null) {
            builder.setDataUpdateUrlFormatter(dataUpdateUrlFormatter);
        }

        // Build the engine
        IPIntelligenceOnPremiseEngine engine;
        if (filename != null && filename.isEmpty() == false) {
            engine = builder.build(filename, createTempDataCopy);
        } else {
            throw new PipelineConfigurationException(
                "No source for engine data. " +
                    "Use setFilename to configure this.");
        }

        return engine;
    }
}
