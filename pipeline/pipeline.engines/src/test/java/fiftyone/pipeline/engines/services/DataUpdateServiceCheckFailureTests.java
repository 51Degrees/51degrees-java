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
import org.junit.Test;
import org.slf4j.Logger;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A failure in a check started by update polling or by the file watcher is
 * caught and logged at Error rather than lost with the scheduled task, is
 * reported to the update callbacks unless one of them raised it, and does
 * not stop later checks. See the Errors section of the data update
 * specification.
 */
public class DataUpdateServiceCheckFailureTests {

    private static final long WAIT_FOR_EVENT_MILLIS = 10000;

    private static final long RETRY_MILLIS = 100;

    /**
     * How far past now the second data file's modified time is set, so it
     * is later than the first on a file system that keeps whole seconds.
     */
    private static final long LATER_FILE_MILLIS = 2000;

    private static final String MAC_MESSAGE =
        "File watchers are not well implemented in OS X, " +
        "so don't run this unit test as it is unlikely to pass.";

    /**
     * The Errors table's message for an error no other row covers, and for
     * an error raised by an update callback.
     */
    private static final String UNHANDLED =
        "An unhandled error occurred while checking for automatic updates " +
        "for engine '%s'";

    /**
     * A callback that fails on a check by the file watcher is logged, and
     * the watcher goes on to apply a later data file. A repeating task that
     * fails is never run again, so the failure must not leave the task.
     */
    @Test
    public void WatcherCheck_CallbackFails_LoggedAndWatchingCarriesOn()
        throws Exception {
        assumeFalse(MAC_MESSAGE, isMac());
        Logger logger = mock(Logger.class);
        AtomicReference<Runnable> watcher = new AtomicReference<>();
        DataUpdateService dataUpdate = new DataUpdateServiceDefault(
            logger,
            mock(HttpClient.class),
            fileWrapperFactory(),
            futureFactory(watcher));
        RuntimeException failure =
            new IllegalStateException("The callback failed");
        dataUpdate.onUpdateComplete((sender, args) -> {
            throw failure;
        });

        OnPremiseAspectEngine<? extends AspectData,
            ? extends AspectPropertyMetaData> engine = engine();
        Path directory = Files.createTempDirectory("watched");
        Path dataFile = directory.resolve("data.dat");
        Files.write(dataFile, "Old".getBytes());
        AspectEngineDataFileDefault file = dataFile(engine, dataFile, true);

        try {
            dataUpdate.registerDataFile(file);
            assertNotNull("Registering sets up the file watcher",
                watcher.get());

            Files.write(dataFile, "New".getBytes());
            runWatcherUntilRefreshed(watcher.get(), engine, 1);
            assertEquals("The watched file refreshes the engine",
                1, refreshCount(engine));
            verify(logger, atLeastOnce()).error(
                eq(String.format(UNHANDLED, engineType(engine))),
                same(failure));

            Files.write(dataFile, "Newer".getBytes());
            Files.setLastModifiedTime(dataFile, FileTime.fromMillis(
                System.currentTimeMillis() + LATER_FILE_MILLIS));
            runWatcherUntilRefreshed(watcher.get(), engine, 2);
            assertEquals("A later data file is still applied",
                2, refreshCount(engine));
        }
        finally {
            cleanUp(file, directory, dataFile);
        }
    }

    /**
     * A callback that fails on a check by update polling is logged rather
     * than left with the timer's task, and polling goes on.
     */
    @Test
    public void PollingCheck_CallbackFails_LoggedAndPollingCarriesOn()
        throws Exception {
        Logger logger = mock(Logger.class);
        List<Runnable> timers = new ArrayList<>();
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode())
            .thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.connect(any(URL.class))).thenReturn(connection);
        DataUpdateService dataUpdate = new DataUpdateServiceDefault(
            logger,
            httpClient,
            fileWrapperFactory(),
            timerFactory(timers));
        RuntimeException failure =
            new IllegalStateException("The callback failed");
        List<AutoUpdateStatus> statuses = new ArrayList<>();
        dataUpdate.onUpdateComplete((sender, args) -> {
            statuses.add(args.getStatus());
            throw failure;
        });

        OnPremiseAspectEngine<? extends AspectData,
            ? extends AspectPropertyMetaData> engine = engine();
        Path directory = Files.createTempDirectory("polled");
        Path dataFile = directory.resolve("data.dat");
        Files.write(dataFile, "Old".getBytes());
        AspectEngineDataFileDefault file = dataFile(engine, dataFile, false);
        file.setTempDataDirPath(directory.toString());
        file.getConfiguration().setDataUpdateUrl("http://www.test.com");
        file.getConfiguration().setVerifyModifiedSince(false);

        try {
            dataUpdate.registerDataFile(file);
            assertEquals("Registering sets one polling timer",
                1, timers.size());

            timers.get(0).run();

            assertEquals("The check is reported once",
                Collections.singletonList(
                    AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED),
                statuses);
            verify(logger).error(
                eq(String.format(UNHANDLED, engineType(engine))),
                same(failure));
            assertEquals("The next check is scheduled", 2, timers.size());
        }
        finally {
            cleanUp(file, directory, dataFile);
        }
    }

    /**
     * A check by update polling that fails is logged, reported as an
     * unknown error, and the next check is still scheduled, so a later data
     * file is applied.
     */
    @Test
    public void PollingCheck_CheckFails_ReportedAndNextCheckScheduled()
        throws Exception {
        Logger logger = mock(Logger.class);
        List<Runnable> timers = new ArrayList<>();
        RuntimeException failure =
            new IllegalStateException("The file system failed");
        FileWrapperFactory fileWrapperFactory = fileWrapperFactory();
        when(fileWrapperFactory.getLastModified(anyString()))
            .thenThrow(failure);
        DataUpdateService dataUpdate = new DataUpdateServiceDefault(
            logger,
            mock(HttpClient.class),
            fileWrapperFactory,
            timerFactory(timers));
        List<String> statuses = new ArrayList<>();
        dataUpdate.onUpdateComplete((sender, args) ->
            statuses.add(args.getStatus().name()));

        OnPremiseAspectEngine<? extends AspectData,
            ? extends AspectPropertyMetaData> engine = engine();
        Path directory = Files.createTempDirectory("polled");
        Path dataFile = directory.resolve("data.dat");
        Files.write(dataFile, "Old".getBytes());
        AspectEngineDataFileDefault file = dataFile(engine, dataFile, false);
        file.setTempDataFilePath(
            directory.resolve("data-copy.dat").toString());

        try {
            dataUpdate.registerDataFile(file);
            assertEquals("Registering sets one polling timer",
                1, timers.size());

            timers.get(0).run();

            verify(logger).error(
                eq(String.format(UNHANDLED, engineType(engine))),
                same(failure));
            assertEquals("The failed check is reported as an unknown error",
                Collections.singletonList("AUTO_UPDATE_UNKNOWN_ERROR"),
                statuses);
            assertEquals("The next check is scheduled", 2, timers.size());
        }
        finally {
            cleanUp(file, directory, dataFile);
        }
    }

    /**
     * A programmatic check that fails is reported as an unknown error, and
     * the error then passes to the caller without being logged, as the
     * Programmatic update steps of the data update specification say.
     */
    @Test
    public void ProgrammaticCheck_CheckFails_ReportedAndPassedToCaller()
        throws Exception {
        Logger logger = mock(Logger.class);
        RuntimeException failure =
            new IllegalStateException("The file system failed");
        FileWrapperFactory fileWrapperFactory = fileWrapperFactory();
        when(fileWrapperFactory.getLastModified(anyString()))
            .thenThrow(failure);
        DataUpdateService dataUpdate = new DataUpdateServiceDefault(
            logger,
            mock(HttpClient.class),
            fileWrapperFactory,
            timerFactory(new ArrayList<>()));
        List<String> statuses = new ArrayList<>();
        dataUpdate.onUpdateComplete((sender, args) ->
            statuses.add(args.getStatus().name()));

        OnPremiseAspectEngine<? extends AspectData,
            ? extends AspectPropertyMetaData> engine = engine();
        Path directory = Files.createTempDirectory("checked");
        Path dataFile = directory.resolve("data.dat");
        Files.write(dataFile, "Old".getBytes());
        AspectEngineDataFileDefault file = dataFile(engine, dataFile, false);
        file.getConfiguration().setAutomaticUpdatesEnabled(false);
        file.setTempDataFilePath(
            directory.resolve("data-copy.dat").toString());
        doReturn(file).when(engine).getDataFileMetaData();

        try {
            RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> dataUpdate.checkForUpdate(engine));

            assertSame("The check's error passes to the caller",
                failure, thrown);
            assertEquals("The failed check is reported as an unknown error",
                Collections.singletonList("AUTO_UPDATE_UNKNOWN_ERROR"),
                statuses);
            verify(logger, never()).error(anyString(), any(Throwable.class));
        }
        finally {
            cleanUp(file, directory, dataFile);
        }
    }

    private static void runWatcherUntilRefreshed(
        Runnable watcher,
        Object engine,
        long count) throws InterruptedException {
        long end = System.currentTimeMillis() + WAIT_FOR_EVENT_MILLIS;
        while (refreshCount(engine) < count &&
            System.currentTimeMillis() < end) {
            watcher.run();
            Thread.sleep(RETRY_MILLIS);
        }
    }

    private static FutureFactory futureFactory(
        final AtomicReference<Runnable> watcher) {
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

    private static FutureFactory timerFactory(final List<Runnable> timers) {
        FutureFactory futureFactory = mock(FutureFactory.class);
        when(futureFactory.schedule(any(Runnable.class), anyLong()))
            .then(invocation -> {
                timers.add((Runnable) invocation.getArgument(0));
                return mock(ScheduledFuture.class);
            });
        when(futureFactory.scheduleRepeating(any(Runnable.class), anyLong()))
            .then(invocation -> mock(ScheduledFuture.class));
        return futureFactory;
    }

    private static FileWrapperFactory fileWrapperFactory() {
        FileWrapperFactory fileWrapperFactory = mock(FileWrapperFactory.class);
        when(fileWrapperFactory.build(anyString())).then(invocation ->
            new FileWrapperDefault((String) invocation.getArgument(0)));
        when(fileWrapperFactory.getLastModified(anyString())).then(
            invocation ->
                new File((String) invocation.getArgument(0)).lastModified());
        return fileWrapperFactory;
    }

    @SuppressWarnings("unchecked")
    private static OnPremiseAspectEngine<? extends AspectData,
        ? extends AspectPropertyMetaData> engine() {
        return mock(OnPremiseAspectEngine.class);
    }

    private static String engineType(Object engine) {
        return engine.getClass().getSimpleName();
    }

    private static AspectEngineDataFileDefault dataFile(
        OnPremiseAspectEngine<? extends AspectData,
            ? extends AspectPropertyMetaData> engine,
        Path dataFile,
        boolean watching) {
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setFileSystemWatcherEnabled(watching);
        config.setPollingIntervalSeconds(1);
        config.setDataFilePath(dataFile.toString());
        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);
        return file;
    }

    private static void cleanUp(
        AspectEngineDataFileDefault file,
        Path directory,
        Path dataFile) {
        if (file.getConfiguration().getWatchKey() != null) {
            file.getConfiguration().getWatchKey().cancel();
        }
        // Whatever Windows still holds is deleted when the test run ends,
        // in the reverse order it is registered, so the folder goes first.
        directory.toFile().deleteOnExit();
        if (dataFile.toFile().delete() == false) {
            dataFile.toFile().deleteOnExit();
        }
        directory.toFile().delete();
    }

    private static boolean isMac() {
        return System.getProperty("os.name").contains("Mac OS X");
    }

    private static long refreshCount(Object engine) {
        return mockingDetails(engine).getInvocations().stream()
            .filter(i -> i.getMethod().getName().equals("refreshData"))
            .count();
    }
}
