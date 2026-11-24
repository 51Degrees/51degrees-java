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

import fiftyone.common.wrappers.io.FileWrapperDefault;
import fiftyone.common.wrappers.io.FileWrapperFactory;
import fiftyone.pipeline.engines.configuration.DataFileConfigurationDefault;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import fiftyone.pipeline.engines.services.DataUpdateService.AutoUpdateStatus;
import fiftyone.pipeline.engines.services.update.FutureFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A watched, downloaded or changed data file is loaded only when the
 * published date in its header is later than the published date of the data
 * the engine has loaded. Otherwise the data is left as it is and the check
 * is reported as not needed, as the newFileAvailable and reporting each
 * check sections of the data update specification say.
 * <p>
 * The engine here reads a file's published date from the file, which holds
 * nothing but that date, and takes the date of a file it loads, as an
 * engine reads both from its data file's header.
 */
public class DataUpdateServicePublishedDateTests {

    private static final LocalDate LOADED = LocalDate.of(2026, 11, 20);

    private static final long WAIT_FOR_EVENT_MILLIS = 10000;

    private static final long RETRY_MILLIS = 100;

    /**
     * How far ahead of the data file a new file's modification time is set,
     * so it is newer whatever the file system's time resolution.
     */
    private static final long NEWER_BY_MILLIS = TimeUnit.MINUTES.toMillis(1);

    private static final String UPDATE_URL = "http://localhost/data";

    private static final String MAC_MESSAGE =
        "File watchers are not well implemented in OS X, " +
        "so don't run this unit test as it is unlikely to pass.";

    private final List<AutoUpdateStatus> reported = new ArrayList<>();
    private final AtomicReference<Runnable> watcher = new AtomicReference<>();
    private OnPremiseAspectEngine<? extends AspectData,
        ? extends AspectPropertyMetaData> engine;
    private HttpURLConnection connection;
    private DataUpdateServiceDefault service;
    private AspectEngineDataFileDefault file;
    private Path directory;
    private Path dataFile;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        directory = Files.createTempDirectory("published");
        dataFile = directory.resolve("data.dat");
        write(dataFile, LOADED, 0);

        connection = mock(HttpURLConnection.class);
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.connect(any(URL.class))).thenReturn(connection);
        FileWrapperFactory fileWrapperFactory = mock(FileWrapperFactory.class);
        when(fileWrapperFactory.build(anyString())).then(invocation ->
            new FileWrapperDefault((String) invocation.getArgument(0)));
        when(fileWrapperFactory.getLastModified(anyString())).then(
            invocation -> new File((String) invocation.getArgument(0))
                .lastModified());
        service = new DataUpdateServiceDefault(
            null,
            httpClient,
            fileWrapperFactory,
            futureFactory());
        service.onUpdateComplete((sender, args) -> {
            synchronized (reported) {
                reported.add(args.getStatus());
            }
        });

        engine = mock(OnPremiseAspectEngine.class);
        file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setDataPublishedDateTime(date(LOADED));
        when(engine.getDataFileMetaData()).thenReturn(file);
        when(engine.getTempDataDirPath()).thenReturn(directory.toString());
        when(engine.getPublishedDateFromFile(any(), anyString())).then(
            invocation -> date(read(invocation.getArgument(1))));
        doAnswer(invocation -> {
            file.setDataPublishedDateTime(date(read(dataFile.toString())));
            return null;
        }).when(engine).refreshData((String) any());
    }

    @After
    public void tearDown() {
        service.unregisterDataFile(file);
        service.close();
        // Windows keeps a file the watcher has read until the reader's
        // mapped buffer is collected, so whatever cannot be deleted now is
        // deleted when the test run ends. Those are deleted in the reverse
        // order they are registered, so the folder is registered first.
        directory.toFile().deleteOnExit();
        File[] files = directory.toFile().listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.delete() == false) {
                    f.deleteOnExit();
                }
            }
        }
        directory.toFile().delete();
    }

    @Test
    public void WatchedFile_Later_Loaded() throws Exception {
        assertWatched(LOADED.plusDays(1), AutoUpdateStatus.AUTO_UPDATE_SUCCESS);
    }

    @Test
    public void WatchedFile_Same_NotLoaded() throws Exception {
        assertWatched(LOADED, AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED);
    }

    @Test
    public void WatchedFile_Earlier_NotLoaded() throws Exception {
        assertWatched(LOADED.minusDays(1),
            AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED);
    }

    @Test
    public void DownloadedFile_Later_Loaded() throws Exception {
        assertDownloaded(LOADED.plusDays(1),
            AutoUpdateStatus.AUTO_UPDATE_SUCCESS);
    }

    @Test
    public void DownloadedFile_Same_NotLoaded() throws Exception {
        assertDownloaded(LOADED, AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED);
    }

    @Test
    public void DownloadedFile_Earlier_NotLoaded() throws Exception {
        assertDownloaded(LOADED.minusDays(1),
            AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED);
    }

    @Test
    public void ChangedFile_Later_Loaded() throws Exception {
        assertChanged(LOADED.plusDays(1), AutoUpdateStatus.AUTO_UPDATE_SUCCESS);
    }

    @Test
    public void ChangedFile_Same_NotLoaded() throws Exception {
        assertChanged(LOADED, AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED);
    }

    @Test
    public void ChangedFile_Earlier_NotLoaded() throws Exception {
        assertChanged(LOADED.minusDays(1),
            AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED);
    }

    /**
     * An engine that cannot give a file's published date has the file
     * loaded, as it was before engines gave the date.
     */
    @Test
    public void DownloadedFile_EngineGivesNoDate_Loaded() throws Exception {
        doReturn(null).when(engine)
            .getPublishedDateFromFile(any(), anyString());
        assertDownloaded(LOADED, AutoUpdateStatus.AUTO_UPDATE_SUCCESS);
    }

    /**
     * A file whose published date cannot be read is loaded, so a file that
     * cannot be used fails when it is loaded, as it did before.
     */
    @Test
    public void ChangedFile_DateCannotBeRead_Loaded() throws Exception {
        doThrow(new IllegalStateException("Not a data file")).when(engine)
            .getPublishedDateFromFile(any(), anyString());
        assertChanged(LOADED, AutoUpdateStatus.AUTO_UPDATE_SUCCESS);
    }

    /**
     * A check that finds no changed file and has no update URL is reported
     * as not needed.
     */
    @Test
    public void NoChangedFileNoUrl_NotNeeded() throws Exception {
        configure(false);
        Path tempFile = directory.resolve("temp.dat");
        write(tempFile, LOADED, NEWER_BY_MILLIS);
        file.setTempDataFilePath(tempFile.toString());

        assertEquals(AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED,
            service.checkForUpdate(engine));
        verify(engine, never()).refreshData((String) any());
        assertReported(AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED);
    }

    /**
     * The file watcher finds the data file replaced by one with the
     * published date given and a newer modification time.
     */
    private void assertWatched(LocalDate published, AutoUpdateStatus expected)
        throws Exception {
        assumeFalse(MAC_MESSAGE,
            System.getProperty("os.name").contains("Mac OS X"));
        configure(true);
        service.registerDataFile(file);
        assertNotNull("Registering sets up the file watcher", watcher.get());

        write(dataFile, published, NEWER_BY_MILLIS);
        long end = System.currentTimeMillis() + WAIT_FOR_EVENT_MILLIS;
        while (firstReported() == null && System.currentTimeMillis() < end) {
            watcher.get().run();
            Thread.sleep(RETRY_MILLIS);
        }

        assertLoaded(published, expected);
        assertEquals(expected, firstReported());
    }

    /**
     * A programmatic check downloads a file with the published date given.
     * Polling makes the same check on a timer.
     */
    private void assertDownloaded(
        LocalDate published,
        AutoUpdateStatus expected) throws Exception {
        configure(false);
        file.getConfiguration().setDataUpdateUrl(UPDATE_URL);
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(
            published.toString().getBytes(StandardCharsets.UTF_8)));

        assertEquals(expected, service.checkForUpdate(engine));

        assertLoaded(published, expected);
        assertEquals("The data file is replaced only by a later file",
            expected == AutoUpdateStatus.AUTO_UPDATE_SUCCESS ? published : LOADED,
            read(dataFile.toString()));
        assertReported(expected);
    }

    /**
     * A programmatic check finds the data file newer than the temporary
     * copy the engine uses, and holding the published date given.
     */
    private void assertChanged(
        LocalDate published,
        AutoUpdateStatus expected) throws Exception {
        configure(false);
        Path tempFile = directory.resolve("temp.dat");
        write(tempFile, LOADED, 0);
        file.setTempDataFilePath(tempFile.toString());
        write(dataFile, published, NEWER_BY_MILLIS);

        assertEquals(expected, service.checkForUpdate(engine));

        assertLoaded(published, expected);
        assertReported(expected);
    }

    private void assertLoaded(LocalDate published, AutoUpdateStatus expected) {
        if (expected == AutoUpdateStatus.AUTO_UPDATE_SUCCESS) {
            verify(engine, times(1)).refreshData((String) any());
            assertEquals(date(published), file.getDataPublishedDateTime());
        } else {
            verify(engine, never()).refreshData((String) any());
            assertEquals("The loaded data is left as it is",
                date(LOADED), file.getDataPublishedDateTime());
        }
    }

    private void assertReported(AutoUpdateStatus expected) {
        synchronized (reported) {
            assertEquals("The check is reported once", 1, reported.size());
            assertEquals(expected, reported.get(0));
        }
    }

    private AutoUpdateStatus firstReported() {
        synchronized (reported) {
            return reported.isEmpty() ? null : reported.get(0);
        }
    }

    private void configure(boolean watching) {
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(false);
        config.setFileSystemWatcherEnabled(watching);
        config.setDecompressContent(false);
        config.setVerifyMd5(false);
        config.setDataFilePath(dataFile.toString());
        file.setConfiguration(config);
    }

    /**
     * Keeps the watcher's task for the test to run itself, and runs no
     * timer.
     */
    private FutureFactory futureFactory() {
        FutureFactory futureFactory = mock(FutureFactory.class);
        when(futureFactory.schedule(any(Runnable.class), anyLong()))
            .then(invocation -> mock(ScheduledFuture.class));
        when(futureFactory.scheduleRepeating(any(Runnable.class), anyLong()))
            .then(invocation -> {
                watcher.set((Runnable) invocation.getArgument(0));
                return mock(ScheduledFuture.class);
            });
        return futureFactory;
    }

    /**
     * Write a file holding the published date, modified the time given
     * after now.
     */
    private static void write(Path path, LocalDate published, long laterMillis)
        throws IOException {
        Files.write(path, published.toString().getBytes(StandardCharsets.UTF_8));
        Files.setLastModifiedTime(path, FileTime.fromMillis(
            System.currentTimeMillis() + laterMillis));
    }

    private static LocalDate read(String path) throws IOException {
        return LocalDate.parse(new String(
            Files.readAllBytes(new File(path).toPath()),
            StandardCharsets.UTF_8));
    }

    private static Date date(LocalDate value) {
        return Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
