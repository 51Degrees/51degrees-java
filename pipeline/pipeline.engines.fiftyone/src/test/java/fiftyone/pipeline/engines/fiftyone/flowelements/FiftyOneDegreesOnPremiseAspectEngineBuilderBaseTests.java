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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneDataFile;
import fiftyone.pipeline.engines.services.DataUpdateService;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class FiftyOneDegreesOnPremiseAspectEngineBuilderBaseTests {
    final String licenseKey = "somelicensekey";
    final String downloadType = "sometype";

    @SuppressWarnings("unchecked")
    private FiftyOneAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine = mock(FiftyOneAspectEngine.class);

    private DataUpdateService updateService = mock(DataUpdateService.class);

    private class ImplementedBuilder extends
        FiftyOneOnPremiseAspectEngineBuilderBase<ImplementedBuilder, FiftyOneAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData>> {

        @SuppressWarnings("unused")
        public ImplementedBuilder() {
            super();
        }

        @SuppressWarnings("unused")
        public ImplementedBuilder(ILoggerFactory loggerFactory) {
            super(loggerFactory);
        }

        public ImplementedBuilder(ILoggerFactory loggerFactory, DataUpdateService dataUpdateService) {
            super(loggerFactory, dataUpdateService);
        }

        @Override
        public ImplementedBuilder setConcurrency(int concurrency) {
            return this;
        }

        @Override
        public ImplementedBuilder setPerformanceProfile(Constants.PerformanceProfiles profile) {
            return this;
        }

        @Override
        protected FiftyOneAspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> newEngine(List<String> properties) {
            // Follow the logic used by 51Degrees engines.
            AspectEngineDataFile dataFile = dataFiles.get(0);
            dataFiles.remove(dataFile);
            engine.addDataFile(dataFile);
            dataFile.setEngine(engine);
            return engine;
        }

        @Override
        protected String getDefaultDataDownloadType() {
            return downloadType;
        }
    }

    @BeforeEach
    public void init() {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                // Follow the logic used by 51Degrees engines.
                FiftyOneDataFile dataFile = invocationOnMock.getArgument(0);
                dataFile.setDataUpdateDownloadType(downloadType);
                when(engine.getDataFileMetaData()).thenReturn(dataFile);
                return null;
            }
        }).when(engine).addDataFile(any(AspectEngineDataFile.class));
    }

    /**
     * Check that the builder constructs a data file for the engine which
     * has the engine set. This is done by the engine's constructor.
     * @throws Exception
     */
    @Test
    public void DataFile_SetEngine() throws Exception {
        ImplementedBuilder builder = new ImplementedBuilder(
            LoggerFactory.getILoggerFactory(),
            updateService);
        builder.setAutoUpdate(false);
        builder.build("", false);

        assertNotNull(engine.getDataFileMetaData());
        assertEquals(engine, engine.getDataFileMetaData().getEngine());
    }

    /**
     * Check that the builder constructs a data file for the engine which
     * builds the correct URL for updates. This should be a 51Degrees specific
     * URL.
     * @throws Exception
     */
    @Test
    public void DataFile_UrlFormatter() throws Exception {
        LoggerFactory.getLogger(this.getClass()).info("OS name is {}",
                System.getProperty("os.name"));
        LoggerFactory.getLogger(this.getClass()).info("OS arch is {}",
                System.getProperty("os.arch"));
        ImplementedBuilder builder = new ImplementedBuilder(
            LoggerFactory.getILoggerFactory(),
            updateService);
        builder.setDataUpdateLicenseKey(licenseKey);
        builder.build("", false);
        assertEquals(
            "https://distributor.51degrees.com/api/v2/download?LicenseKeys="+licenseKey+"&Download=True&Type="+downloadType,
            engine.getDataFileMetaData().getFormattedUrl());
    }

    private ImplementedBuilder newBuilder() {
        return new ImplementedBuilder(
            LoggerFactory.getILoggerFactory(),
            updateService);
    }

    /**
     * The data file is registered with the update service when update on
     * start-up is on, even though auto update is off.
     */
    @Test
    public void UpdateOnStartup_AutoUpdateOff_DataFileRegistered()
        throws Exception {
        ImplementedBuilder builder = newBuilder()
            .setAutoUpdate(false)
            .setDataUpdateOnStartup(true)
            .setDataUpdateLicenseKey(licenseKey);

        builder.build("", false);

        verify(updateService, times(1)).registerDataFile(any());
    }

    /**
     * The engine does not exist when the data file is registered, so the
     * data file carries the engine's type name for the update service's log.
     */
    @Test
    public void UpdateOnStartup_DataFileCarriesTheEngineTypeName()
        throws Exception {
        ImplementedBuilder builder = newBuilder()
            .setAutoUpdate(false)
            .setDataUpdateOnStartup(true)
            .setDataUpdateLicenseKey(licenseKey);

        builder.build("", false);

        ArgumentCaptor<AspectEngineDataFile> registered =
            ArgumentCaptor.forClass(AspectEngineDataFile.class);
        verify(updateService).registerDataFile(registered.capture());
        assertEquals("Implemented",
            ((AspectEngineDataFileDefault) registered.getValue())
                .getEngineTypeName());
    }

    @Test
    public void AutoUpdateAndUpdateOnStartupOff_DataFileNotRegistered()
        throws Exception {
        ImplementedBuilder builder = newBuilder()
            .setAutoUpdate(false)
            .setDataUpdateOnStartup(false);

        builder.build("", false);

        verify(updateService, never()).registerDataFile(any());
    }

    /**
     * The default update URL is the Distributor's, which serves nothing
     * without a license key.
     */
    @Test
    public void AutoUpdate_DefaultUrlNoLicenseKey_BuildRefused() {
        ImplementedBuilder builder = newBuilder().setAutoUpdate(true);

        PipelineConfigurationException e = assertThrows(
            PipelineConfigurationException.class,
            () -> builder.build("", false));

        assertTrue(e.getMessage().contains("setAutoUpdate(false)"));
        assertFalse(e.getMessage().contains("setDataUpdateOnStartup(false)"));
        verify(updateService, never()).registerDataFile(any());
    }

    @Test
    public void UpdateOnStartup_DefaultUrlNoLicenseKey_BuildRefused() {
        ImplementedBuilder builder = newBuilder()
            .setAutoUpdate(false)
            .setDataUpdateOnStartup(true);

        PipelineConfigurationException e = assertThrows(
            PipelineConfigurationException.class,
            () -> builder.build("", false));

        assertTrue(e.getMessage().contains("setDataUpdateOnStartup(false)"));
        assertFalse(e.getMessage().contains("setAutoUpdate(false)"));
    }

    @Test
    public void AutoUpdate_LicenseKeySet_BuildAllowed() throws Exception {
        ImplementedBuilder builder = newBuilder()
            .setAutoUpdate(true)
            .setDataUpdateLicenseKey(licenseKey);

        builder.build("", false);

        verify(updateService, times(1)).registerDataFile(any());
    }

    @Test
    public void AutoUpdate_OtherUrlNoLicenseKey_BuildAllowed()
        throws Exception {
        ImplementedBuilder builder = newBuilder()
            .setAutoUpdate(true)
            .setDataUpdateUrl("https://example.com/data.file");

        builder.build("", false);

        verify(updateService, times(1)).registerDataFile(any());
    }

    @Test
    public void AutoUpdateOff_NoLicenseKey_BuildAllowed() throws Exception {
        ImplementedBuilder builder = newBuilder().setAutoUpdate(false);

        builder.build("", false);

        verify(updateService, never()).registerDataFile(any());
    }
}
