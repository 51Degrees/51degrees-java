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

package fiftyone.pipeline.engines.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import fiftyone.common.wrappers.io.FileWrapperFactoryDefault;
import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.configuration.DataFileConfigurationDefault;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.core.exceptions.EngineRefreshException;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import fiftyone.pipeline.engines.services.DataUpdateService.AutoUpdateStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The messages and levels in the Logging section of the data update
 * specification, and the If-Modified-Since time the update URL is sent.
 * @see <a href="https://github.com/51Degrees/specifications/blob/d8b3a9c5/pipeline-specification/features/data-updates.md#logging">Specification</a>
 */
@SuppressWarnings("unchecked")
public class DataUpdateServiceLoggingTests {

    private static final String UPDATE_URL = "http://test.com";
    // 13 September 2020, 12:26:40 UTC.
    private static final long FILE_MODIFIED_MILLIS = 1600000000000L;
    // A license key of the length and characters the Distributor issues.
    private static final String LICENSE_KEY = "QF8ZR2KD7NMB4XWT9HJC3LPA";
    private static final String MASKED_KEY = "********************3LPA";
    private static final String KEYED_URL = UPDATE_URL + "?LicenseKeys=" +
        LICENSE_KEY + "&Download=True&Type=HashV41";
    private static final String MASKED_URL = UPDATE_URL + "?LicenseKeys=" +
        MASKED_KEY + "&Download=True&Type=HashV41";

    private ch.qos.logback.classic.Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private HttpClient httpClient;
    private HttpURLConnection connection;
    private DataUpdateServiceDefault dataUpdate;
    private OnPremiseAspectEngine<? extends AspectData,
        ? extends AspectPropertyMetaData> engine;
    private String engineType;
    private File dataFile;

    @Before
    public void init() throws IOException {
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
            "DataUpdateServiceLoggingTests");
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        connection = mock(HttpURLConnection.class);
        httpClient = mock(HttpClient.class);
        when(httpClient.connect(any(URL.class))).thenReturn(connection);
        // The futures are real but their first run is a day away.
        dataUpdate = new DataUpdateServiceDefault(
            logger, httpClient, new FileWrapperFactoryDefault(), null);

        engine = mock(OnPremiseAspectEngine.class);
        engineType = engine.getClass().getSimpleName();
        dataFile = File.createTempFile("logging-test", ".tmp");
        assertTrue(dataFile.setLastModified(FILE_MODIFIED_MILLIS));
    }

    @After
    public void cleanup() {
        dataUpdate.close();
        logger.detachAppender(appender);
        dataFile.delete();
    }

    private AspectEngineDataFileDefault newDataFile(
        boolean inMemory,
        boolean autoUpdate,
        boolean updateOnStartup) {
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(autoUpdate);
        config.setUpdateOnStartup(updateOnStartup);
        config.setFileSystemWatcherEnabled(false);
        config.setDataUpdateUrl(UPDATE_URL);
        config.setVerifyMd5(false);
        config.setDecompressContent(false);
        config.setPollingIntervalSeconds(86400);
        config.setMaxRandomisationSeconds(0);
        if (inMemory == false) {
            config.setDataFilePath(dataFile.getAbsolutePath());
        }
        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setIdentifier("default");
        file.setEngine(engine);
        file.setConfiguration(config);
        file.setTempDataDirPath(System.getProperty("java.io.tmpdir"));
        when(engine.getDataFileMetaData()).thenReturn(file);
        return file;
    }

    private List<String> logged(Level level) {
        List<String> messages = new ArrayList<>();
        for (ILoggingEvent event : appender.list) {
            if (event.getLevel() == level) {
                messages.add(event.getFormattedMessage());
            }
        }
        return messages;
    }

    private void assertLogged(Level level, String message) {
        List<String> messages = logged(level);
        assertTrue(
            "Expected '" + message + "' at " + level + " but got " + messages,
            messages.contains(message));
    }

    @Test
    public void IfModifiedSince_NoPublishedDate_UsesFileLastModified()
        throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
        AspectEngineDataFileDefault file = newDataFile(false, false, false);
        file.getConfiguration().setVerifyModifiedSince(true);

        dataUpdate.checkForUpdate(engine);

        verify(connection).setIfModifiedSince(FILE_MODIFIED_MILLIS);
    }

    @Test
    public void IfModifiedSince_PublishedDate_UsedInsteadOfFileLastModified()
        throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
        AspectEngineDataFileDefault file = newDataFile(false, false, false);
        file.getConfiguration().setVerifyModifiedSince(true);
        file.setDataPublishedDateTime(new java.util.Date(1500000000000L));

        dataUpdate.checkForUpdate(engine);

        verify(connection).setIfModifiedSince(1500000000000L);
    }

    @Test
    public void Check_NotModified_LogsTheSpecifiedMessages() throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
        AspectEngineDataFileDefault file = newDataFile(false, false, false);
        file.getConfiguration().setVerifyModifiedSince(true);

        dataUpdate.checkForUpdate(engine);

        assertLogged(Level.INFO, "Checking for update");
        assertLogged(Level.INFO, "Checking for update from " + UPDATE_URL);
        assertLogged(Level.INFO, "No data newer than 2020-09-13T12:26:40Z " +
            "found at '" + UPDATE_URL + "' for engine '" + engineType + "'");
    }

    @Test
    public void Check_Downloaded_LogsDownloadAndRefresh() throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getInputStream())
            .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        newDataFile(true, false, false);

        dataUpdate.checkForUpdate(engine);

        assertLogged(Level.INFO, "Downloaded new data from '" + UPDATE_URL +
            "' for engine '" + engineType + "'");
        assertLogged(Level.INFO, "Attempting to refresh engine with new data");
    }

    /**
     * .NET's UpdateFromMemory logs nothing of its own, so Java logs nothing.
     */
    @Test
    public void UpdateFromMemory_LogsNothing() {
        AspectEngineDataFileDefault file = newDataFile(true, false, false);

        dataUpdate.updateFromMemory(file, new byte[]{1, 2, 3});

        assertTrue(logged(Level.INFO).isEmpty());
        assertTrue(logged(Level.WARN).isEmpty());
        assertTrue(logged(Level.ERROR).isEmpty());
    }

    /**
     * .NET throws the refresh failure out of UpdateFromMemory for the caller
     * to handle, and logs nothing.
     */
    @Test
    public void UpdateFromMemory_RefreshFails_ThrowsAndLogsNothing()
        throws Exception {
        AspectEngineDataFileDefault file = newDataFile(true, false, false);
        doThrow(new RuntimeException("no room"))
            .when(engine).refreshData(anyString(), any(byte[].class));

        EngineRefreshException thrown = null;
        try {
            dataUpdate.updateFromMemory(file, new byte[]{1, 2, 3});
        } catch (EngineRefreshException ex) {
            thrown = ex;
        }

        assertNotNull("updateFromMemory did not throw", thrown);
        assertEquals(
            "An error occurred when applying a data update to engine '" +
                engineType + "'.",
            thrown.getMessage());
        assertTrue(logged(Level.ERROR).isEmpty());
    }

    /**
     * .NET warns, and still refreshes the engine, when the data passed in
     * cannot be written to the data file.
     */
    @Test
    public void UpdateFromMemory_WriteFails_WarnsAndStillRefreshes()
        throws Exception {
        AspectEngineDataFileDefault file = newDataFile(false, false, false);
        String missing = new File(
            new File(dataFile.getParentFile(), "no-such-directory"),
            "data.bin").getAbsolutePath();
        file.getConfiguration().setDataFilePath(missing);

        AutoUpdateStatus status =
            dataUpdate.updateFromMemory(file, new byte[]{1, 2, 3});

        assertEquals(AutoUpdateStatus.AUTO_UPDATE_SUCCESS, status);
        assertLogged(Level.WARN, "An error occurred when writing to '" +
            missing + "'. The engine will be updated to use the new data " +
            "but the file on disk will still contain old data.");
    }

    @Test
    public void UpdateOnStartup_LogsUpdatingOnStartUp() throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
        AspectEngineDataFileDefault file = newDataFile(false, false, true);

        dataUpdate.registerDataFile(file);

        assertLogged(Level.INFO, "Updating on startup");
    }

    @Test
    public void UpdateOnStartup_EngineNotBuiltYet_LogsTheEngineTypeName()
        throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
        AspectEngineDataFileDefault file = newDataFile(false, false, true);
        file.setEngine(null);
        file.setEngineTypeName("ExampleEngine");

        dataUpdate.registerDataFile(file);

        assertLogged(Level.INFO, "Checking for update from " + UPDATE_URL);
        assertLogged(Level.INFO, "No data newer than 2020-09-13T12:26:40Z " +
            "found at '" + UPDATE_URL + "' for engine 'ExampleEngine'");
    }

    @Test
    public void UpdateOnStartup_AutoUpdateOff_StartsNoTimer() throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
        AspectEngineDataFileDefault file = newDataFile(false, false, true);

        dataUpdate.registerDataFile(file);

        assertEquals(null, file.getFuture());
    }

    @Test
    public void FileSystemWatcher_LogsCreatingFileSystemWatcher() {
        AspectEngineDataFileDefault file = newDataFile(false, true, false);
        file.getConfiguration().setFileSystemWatcherEnabled(true);

        dataUpdate.registerDataFile(file);

        assertLogged(Level.INFO, "Creating file system watcher");
        assertNotNull(file.getConfiguration().getWatchKey());
    }

    /**
     * The entry a timer or watcher check writes for a failure, which reads
     * "Error during check for data update" at Error with the cause attached.
     * The cause carries the message of the row in the Errors table.
     */
    private void assertCheckFailed(String causeMessage) {
        assertLogged(Level.ERROR, "Error during check for data update");
        boolean found = false;
        for (ILoggingEvent event : appender.list) {
            if (event.getLevel() == Level.ERROR &&
                event.getThrowableProxy() != null &&
                causeMessage.equals(event.getThrowableProxy().getMessage())) {
                found = true;
            }
        }
        assertTrue(
            "No error carrying " + causeMessage + " in " + appender.list,
            found);
    }

    @Test
    public void ConnectFails_Manual_ReportsTheAccessError() throws Exception {
        when(httpClient.connect(any(URL.class)))
            .thenThrow(new IOException("refused"));
        newDataFile(false, false, false);

        dataUpdate.checkForUpdate(engine);

        assertCheckFailed("Error accessing data update service at '" +
            UPDATE_URL + "' for engine '" + engineType + "'");
    }

    @Test
    public void ConnectFails_Automatic_ReportsTheAccessError()
        throws Exception {
        when(httpClient.connect(any(URL.class)))
            .thenThrow(new IOException("refused"));
        AspectEngineDataFileDefault file = newDataFile(false, true, true);

        dataUpdate.registerDataFile(file);

        assertCheckFailed("Error accessing data update service at '" +
            UPDATE_URL + "' for engine '" + engineType + "'");
    }

    @Test
    public void NoResponse_ReportsTheNoResponseError() throws Exception {
        when(httpClient.connect(any(URL.class))).thenReturn(null);
        newDataFile(false, false, false);

        dataUpdate.checkForUpdate(engine);

        assertCheckFailed("No response from data update service at '" +
            UPDATE_URL + "' for engine '" + engineType + "'");
    }

    private void assertStatusReported(int status, String causeMessage)
        throws Exception {
        when(connection.getResponseCode()).thenReturn(status);
        newDataFile(true, false, false);

        dataUpdate.checkForUpdate(engine);

        assertCheckFailed(causeMessage);
        assertTrue(logged(Level.WARN).isEmpty());
    }

    @Test
    public void TooManyRequests_ReportsTheError() throws Exception {
        assertStatusReported(429, "Too many requests to '" + UPDATE_URL +
            "' for engine '" + engineType + "'");
    }

    @Test
    public void Forbidden_ReportsTheError() throws Exception {
        assertStatusReported(403, "Access denied to data update service at '" +
            UPDATE_URL + "' for engine '" + engineType + "'");
    }

    @Test
    public void OtherStatus_ReportsTheError() throws Exception {
        assertStatusReported(500, "HTTP status code '500' from data update " +
            "service at '" + UPDATE_URL + "' for engine '" + engineType + "'");
    }

    /**
     * The download is written to a temporary file in a folder that does not
     * exist, so the write fails while the download itself succeeds.
     */
    @Test
    public void TemporaryFileCannotBeWritten_LogsTheUnhandledError()
        throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getInputStream())
            .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        AspectEngineDataFileDefault file = newDataFile(false, false, false);
        String missingDirectory = new File(
            System.getProperty("java.io.tmpdir"),
            "no-such-directory-" + System.nanoTime()).getAbsolutePath();
        file.setTempDataDirPath(missingDirectory);
        // The engine's copy is the data file itself, so the file on disk is
        // not newer and the update is downloaded.
        file.setTempDataFilePath(dataFile.getAbsolutePath());

        dataUpdate.checkForUpdate(engine);

        verify(connection).getInputStream();
        assertLogged(Level.ERROR, "An unhandled error occurred while " +
            "checking for automatic updates for engine '" + engineType + "'");
        assertTrue(logged(Level.WARN).isEmpty());
    }

    @Test
    public void Md5DoesNotMatch_WarnsAboutIntegrityCheck() throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getInputStream())
            .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(connection.getHeaderField("Content-MD5")).thenReturn("0");
        AspectEngineDataFileDefault file = newDataFile(true, false, false);
        file.getConfiguration().setVerifyMd5(true);

        dataUpdate.checkForUpdate(engine);

        assertCheckFailed("Integrity check failed. MD5 hash in HTTP " +
            "response '0' for '" + engineType + "' data update does not " +
            "match calculated hash for the downloaded file " +
            "'5289df737df57326fcdd22597afb1fac'.");
    }

    /**
     * .NET warns, and goes on with the check, when the response carries no
     * Content-MD5 header to verify the download against.
     */
    @Test
    public void NoMd5Header_WarnsAndGoesOn() throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getInputStream())
            .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(connection.getHeaderField("Content-MD5")).thenReturn(null);
        AspectEngineDataFileDefault file = newDataFile(true, false, false);
        file.getConfiguration().setVerifyMd5(true);

        dataUpdate.checkForUpdate(engine);

        assertLogged(Level.WARN, "No MD5 hash included in data update " +
            "response for '" + UPDATE_URL + "'. Unable to verify data " +
            "integrity");
        assertLogged(Level.INFO, "Attempting to refresh engine with new data");
    }

    @Test
    public void NoTemporaryFilePath_LogsError() {
        AspectEngineDataFileDefault file = newDataFile(false, false, false);
        file.setTempDataDirPath(null);
        when(engine.getTempDataDirPath()).thenReturn(null);

        dataUpdate.checkForUpdate(engine);

        assertCheckFailed("The data file 'default' is checking for " +
            "updates but does not have a temporary file path configured.");
    }

    /**
     * A data file whose formatted URL carries a license key, as the
     * Distributor's URL does, with every level logged.
     */
    private AspectEngineDataFileDefault newKeyedDataFile(boolean inMemory) {
        logger.setLevel(Level.DEBUG);
        AspectEngineDataFileDefault file = newDataFile(inMemory, false, false);
        DataFileConfiguration config = file.getConfiguration();
        config.setDataUpdateLicenseKeys(
            Collections.singletonList(LICENSE_KEY));
        config.setUrlFormatter(dataFile -> KEYED_URL);
        return file;
    }

    /**
     * Fails if any log entry, or any throwable logged with one, holds the
     * license key in full.
     */
    private void assertKeyOnlyMasked() {
        assertFalse(appender.list.isEmpty());
        for (ILoggingEvent event : appender.list) {
            String text = event.getFormattedMessage();
            if (event.getThrowableProxy() != null) {
                text += "\n" +
                    ThrowableProxyUtil.asString(event.getThrowableProxy());
            }
            assertFalse(
                "The license key is in full in: " + text,
                text.contains(LICENSE_KEY));
        }
    }

    @Test
    public void LicenseKey_NotModified_LogsTheMaskedUrl() throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
        AspectEngineDataFileDefault file = newKeyedDataFile(false);
        file.getConfiguration().setVerifyModifiedSince(true);

        dataUpdate.checkForUpdate(engine);

        assertLogged(Level.INFO, "Checking for update from " + MASKED_URL);
        assertLogged(Level.INFO, "No data newer than 2020-09-13T12:26:40Z " +
            "found at '" + MASKED_URL + "' for engine '" + engineType + "'");
        assertKeyOnlyMasked();
    }

    @Test
    public void LicenseKey_Downloaded_LogsTheMaskedUrl() throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getInputStream())
            .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        newKeyedDataFile(true);

        dataUpdate.checkForUpdate(engine);

        assertLogged(Level.INFO, "Downloaded new data from '" + MASKED_URL +
            "' for engine '" + engineType + "'");
        assertKeyOnlyMasked();
    }

    /**
     * An HTTP library's exception often names the URL it requested, so the
     * error detail and the logged exception are masked too.
     */
    @Test
    public void LicenseKey_ConnectFails_MasksTheUrlAndTheException()
        throws Exception {
        when(httpClient.connect(any(URL.class)))
            .thenThrow(new IOException("Connection refused: " + KEYED_URL));
        newKeyedDataFile(false);

        dataUpdate.checkForUpdate(engine);

        assertCheckFailed("Error accessing data update service at '" +
            MASKED_URL + "' for engine '" + engineType + "'");
        assertKeyOnlyMasked();
    }

    @Test
    public void LicenseKey_DownloadFails_MasksTheUrlAndTheException()
        throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getInputStream())
            .thenThrow(new IOException("Premature EOF from " + KEYED_URL));
        newKeyedDataFile(true);

        dataUpdate.checkForUpdate(engine);

        assertCheckFailed("Error accessing data update service at '" +
            MASKED_URL + "' for engine '" + engineType + "'");
        assertKeyOnlyMasked();
    }

    @Test
    public void LicenseKey_Forbidden_LogsTheMaskedUrl() throws Exception {
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
        newKeyedDataFile(true);

        dataUpdate.checkForUpdate(engine);

        assertCheckFailed("Access denied to data update service at '" +
            MASKED_URL + "' for engine '" + engineType + "'");
        assertKeyOnlyMasked();
    }

    @Test
    public void LicenseKey_NoResponse_LogsTheMaskedUrl() throws Exception {
        when(httpClient.connect(any(URL.class))).thenReturn(null);
        newKeyedDataFile(false);

        dataUpdate.checkForUpdate(engine);

        assertCheckFailed("No response from data update service at '" +
            MASKED_URL + "' for engine '" + engineType + "'");
        assertKeyOnlyMasked();
    }
}
