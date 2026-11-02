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
import fiftyone.ipintelligence.engine.onpremise.interop.swig.ConfigIpiSwig;
import fiftyone.ipintelligence.shared.testhelpers.FileUtils;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.DataUpdateUrlFormatter;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneDataFileDefault;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageElement;
import fiftyone.pipeline.engines.services.DataUpdateServiceDefault;
import fiftyone.pipeline.util.FileFinder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Ignore
@SuppressWarnings({"rawtypes", "deprecation"})
public class IPIntelligenceOnPremisePipelineBuilderTest {

    private static final String IPI_DATA_FILE_NAME = FileUtils.getHashFileName();
    private IPIntelligenceOnPremisePipelineBuilder builder;
    private File datafile = FileFinder.getFilePath(IPI_DATA_FILE_NAME);
    private ILoggerFactory loggerFactory;
    private Logger logger;

    @Before
    public void setup() throws Exception {
        loggerFactory = mock(ILoggerFactory.class);
        logger = mock(Logger.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);
        builder = new IPIntelligencePipelineBuilder(loggerFactory)
                .useOnPremise(datafile.getAbsolutePath(), false)
                .setShareUsage(false);
    }

    private IPIntelligenceOnPremiseEngine getEngine(Pipeline pipeline) {
        return (IPIntelligenceOnPremiseEngine) pipeline.getFlowElements().get(0);
    }

    private DataFileConfiguration getDataFileConfiguration(Pipeline pipeline) {
        IPIntelligenceOnPremiseEngine engine = getEngine(pipeline);
        List<AspectEngineDataFile> engineDataFiles = engine.getDataFiles();
        FiftyOneDataFileDefault fiftyOneDataFileDefault = (FiftyOneDataFileDefault) engineDataFiles.get(0);
        return fiftyOneDataFileDefault.getConfiguration();
    }

    private ConfigIpiSwig getConfigIpiSwig(Pipeline pipeline) throws NoSuchFieldException, IllegalAccessException {
        IPIntelligenceOnPremiseEngine engine = getEngine(pipeline);
        Field configSwigField = engine.getClass().getDeclaredField("config");
        configSwigField.setAccessible(true);
        return (ConfigIpiSwig) configSwigField.get(engine);
    }

    @Test
    public void setFilename_PropertyPropagatedToDataFileConfiguration() throws Exception {
        Pipeline pipeline = builder
                .setFilename(IPI_DATA_FILE_NAME, true)
                .build();
        DataFileConfiguration dataFileConfiguration = getDataFileConfiguration(pipeline);
        assertEquals(IPI_DATA_FILE_NAME, dataFileConfiguration.getDataFilePath());
        assertTrue(dataFileConfiguration.getCreateTempDataCopy());
    }

    @Test
    public void setFilename_DatExtension_ExceptionThrown() {
        try {
            builder.setFilename(IPI_DATA_FILE_NAME.replace(".ipi", ".dat"), true);
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unrecognised filename."));
        }
    }

    @Test
    public void setFilename_OtherExtension_ExceptionThrown() {
        try {
            builder.setFilename(IPI_DATA_FILE_NAME.replace(".ipi", ".any"), true);
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unrecognised filename."));
        }
    }

    @Test
    public void setShareUsage_False_SharedFlowElementNotAdded() throws Exception {
        Pipeline pipeline = new IPIntelligencePipelineBuilder(loggerFactory)
                .useOnPremise(datafile.getAbsolutePath(), false)
                .setShareUsage(false)
                .build();
        List<FlowElement> flowElements = pipeline.getFlowElements();
        assertEquals(1, flowElements.size());
        assertFalse(flowElements.get(0) instanceof ShareUsageElement);
    }

    @Test
    public void setShareUsage_True_SharedFlowElementAdded() throws Exception {
        Pipeline pipeline = new IPIntelligencePipelineBuilder(loggerFactory)
                .useOnPremise(datafile.getAbsolutePath(), false)
                .setShareUsage(true)
                .build();
        List<FlowElement> flowElements = pipeline.getFlowElements();
        assertEquals(2, flowElements.size());
        assertTrue(flowElements.get(0) instanceof ShareUsageElement);
    }

    @Test
    public void setAutoUpdate_PropertyPropagatedToDataFileConfiguration() throws Exception {
        Pipeline pipeline = builder
                .setAutoUpdate(true)
                .build();
        DataFileConfiguration dataFileConfiguration = getDataFileConfiguration(pipeline);
        assertTrue(dataFileConfiguration.getAutomaticUpdatesEnabled());
    }

    @Test
    public void setDataFileSystemWatcher_PropertyPropagatedToDataFileConfiguration() throws Exception {
        Pipeline pipeline = builder
                .setDataFileSystemWatcher(true)
                .build();
        DataFileConfiguration dataFileConfiguration = getDataFileConfiguration(pipeline);
        assertTrue(dataFileConfiguration.getFileSystemWatcherEnabled());
    }

    @Test
    public void setDataUpdateService_RegisterDataFileTriggered() throws Exception {
        DataUpdateServiceDefault dataUpdateServiceDefault = mock(DataUpdateServiceDefault.class);
        builder
                .setAutoUpdate(true)
                .setDataUpdateService(dataUpdateServiceDefault)
                .build();
        verify(dataUpdateServiceDefault).registerDataFile(any());
    }

    @Test
    public void setDataUpdateOnStartup_PropertyPropagatedToDataFileConfiguration() throws Exception {
        Pipeline pipeline = builder
                .setDataUpdateOnStartup(true)
                .build();
        DataFileConfiguration dataFileConfiguration = getDataFileConfiguration(pipeline);
        assertTrue(dataFileConfiguration.getUpdateOnStartup());
    }

    @Test
    public void setUpdatePollingInterval_PropertyPropagatedToDataFileConfiguration() throws Exception {
        Pipeline pipeline = builder
                .setUpdatePollingInterval(10)
                .build();
        DataFileConfiguration dataFileConfiguration = getDataFileConfiguration(pipeline);
        assertEquals(10, dataFileConfiguration.getPollingIntervalSeconds());
    }

    @Test
    public void setUpdatePollingIntervalMillis_PropertyPropagatedToDataFileConfiguration() throws Exception {
        Pipeline pipeline = builder
                .setUpdatePollingIntervalMillis(10000)
                .build();
        DataFileConfiguration dataFileConfiguration = getDataFileConfiguration(pipeline);
        assertEquals(10, dataFileConfiguration.getPollingIntervalSeconds());
    }

    @Test
    public void setUpdateRandomisationMax_PropertyPropagatedToDataFileConfiguration() throws Exception {
        Pipeline pipeline = builder
                .setUpdateRandomisationMax(123)
                .build();
        DataFileConfiguration dataFileConfiguration = getDataFileConfiguration(pipeline);
        assertEquals(123, dataFileConfiguration.getMaxRandomisationSeconds());
    }

    @Test
    public void setUpdateRandomisationMaxMillis_PropertyPropagatedToDataFileConfiguration() throws Exception {
        Pipeline pipeline = builder
                .setUpdateRandomisationMaxMillis(123000)
                .build();
        DataFileConfiguration dataFileConfiguration = getDataFileConfiguration(pipeline);
        assertEquals(123, dataFileConfiguration.getMaxRandomisationSeconds());
    }

    @Test
    public void setDataUpdateLicenseKey_PropertyPropagatedToDataFileConfiguration() throws Exception {
        String key = "licence_key";
        Pipeline pipeline = builder
                .setDataUpdateLicenseKey(key)
                .build();
        DataFileConfiguration dataFileConfiguration = getDataFileConfiguration(pipeline);
        assertTrue(dataFileConfiguration.getDataUpdateLicenseKeys().contains(key));
    }

    @Test
    public void setConcurrency_PropertyPropagatedSwigConfiguration() throws Exception {
        Pipeline pipeline = builder
                .setConcurrency(4)
                .build();
        ConfigIpiSwig swig = getConfigIpiSwig(pipeline);
        assertEquals(4, swig.getConcurrency());
    }

    @Test
    public void setDataUpdateUrl_PropertyPropagatedToDataFileConfiguration() throws Exception {
        String url = "https://update.me.com";
        Pipeline pipeline = builder
                .setDataUpdateUrl(url)
                .build();
        DataFileConfiguration dataFileConfiguration = getDataFileConfiguration(pipeline);
        assertEquals(url, dataFileConfiguration.getDataUpdateUrl());
    }

    @Test
    public void setDataUpdateVerifyMd5_PropertyPropagatedToDataFileConfiguration() throws Exception {
        Pipeline pipeline = builder
                .setDataUpdateVerifyMd5(true)
                .build();
        DataFileConfiguration dataFileConfiguration = getDataFileConfiguration(pipeline);
        assertTrue(dataFileConfiguration.getVerifyMd5());
    }

    @Test
    public void setDataUpdateUrlFormatter_PropertyPropagatedToDataFileConfiguration() throws Exception {
        DataUpdateUrlFormatter urlFormatter = mock(DataUpdateUrlFormatter.class);
        Pipeline pipeline = builder
                .setDataUpdateUrlFormatter(urlFormatter)
                .build();
        DataFileConfiguration dataFileConfiguration = getDataFileConfiguration(pipeline);
        assertEquals(urlFormatter, dataFileConfiguration.getUrlFormatter());
    }
}