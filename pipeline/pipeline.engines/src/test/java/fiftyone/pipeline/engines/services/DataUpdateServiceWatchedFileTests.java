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

import fiftyone.common.wrappers.data.BinaryReader;
import fiftyone.common.wrappers.io.FileWrapper;
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
import org.mockito.Answers;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * A new data file found by the file watcher is loaded, and then polling for
 * updates carries on, as the newFileAvailable steps of the data update
 * specification say ("resume polling, if any"). A new file that cannot be
 * opened is given up on after a time, as its Errors table says, or at
 * once when the wait is interrupted.
 */
public class DataUpdateServiceWatchedFileTests {

    private static final long WAIT_FOR_EVENT_MILLIS = 10000;

    private static final long RETRY_MILLIS = 100;

    /**
     * The wait for a file that cannot be opened, shortened from ten minutes.
     */
    private static final long OPEN_TIMEOUT_MILLIS = 1000;

    /**
     * The most tries that wait allows, being one at the start, one after
     * each pause and one more for a slow runner.
     */
    private static final long MAX_TRIES = OPEN_TIMEOUT_MILLIS /
        DataUpdateServiceDefault.WATCHED_FILE_OPEN_PAUSE_MILLIS + 2;

    /**
     * A wait long enough that only an interrupt can end it within
     * {@link #INTERRUPTED_WAIT_MILLIS}.
     */
    private static final long LONG_OPEN_TIMEOUT_MILLIS = 60000;

    private static final long INTERRUPTED_WAIT_MILLIS = 5000;

    private static final String MAC_MESSAGE =
        "File watchers are not well implemented in OS X, " +
        "so don't run this unit test as it is unlikely to pass.";

    @Test
    public void WatchedFileRefresh_PollingCarriesOn() throws Exception {
        assumeFalse(MAC_MESSAGE, isMac());

        final List<Long> timers = new ArrayList<>();
        final AtomicReference<Runnable> watcher = new AtomicReference<>();
        FileWrapperFactory fileWrapperFactory = mock(FileWrapperFactory.class);
        when(fileWrapperFactory.build(anyString())).then(invocation ->
            new FileWrapperDefault((String) invocation.getArgument(0)));
        when(fileWrapperFactory.getLastModified(anyString())).then(
            invocation -> lastModified(invocation.getArgument(0)));
        DataUpdateService dataUpdate = new DataUpdateServiceDefault(
            null,
            mock(HttpClient.class),
            fileWrapperFactory,
            futureFactory(timers, watcher));

        OnPremiseAspectEngine<? extends AspectData,
            ? extends AspectPropertyMetaData> engine = engine();
        Path directory = Files.createTempDirectory("watched");
        Path dataFile = directory.resolve("data.dat");
        Files.write(dataFile, "Old".getBytes());
        AspectEngineDataFileDefault file = watchedFile(engine, dataFile);

        try {
            dataUpdate.registerDataFile(file);
            assertEquals("Registering sets one polling timer",
                1, timers.size());
            assertNotNull("Registering sets up the file watcher",
                watcher.get());

            Files.write(dataFile, "New".getBytes());
            long end = System.currentTimeMillis() + WAIT_FOR_EVENT_MILLIS;
            while (refreshCount(engine) == 0 &&
                System.currentTimeMillis() < end) {
                watcher.get().run();
                Thread.sleep(RETRY_MILLIS);
            }

            assertEquals("The watched file refreshes the engine once",
                1, refreshCount(engine));
            assertEquals("A new polling timer is set after the refresh",
                2, timers.size());
            assertNotNull("The data file holds the new polling timer",
                file.getFuture());
        }
        finally {
            cleanUp(file, directory, dataFile);
        }
    }

    /**
     * A watched file that cannot be opened is tried, with a pause between
     * tries, until the wait ends. The update then reports
     * AUTO_UPDATE_TIMEOUT, logs nothing, and leaves the engine and the
     * polling timer as they were.
     */
    @Test
    public void WatchedFileCannotBeOpened_GivesUpAfterTheWait()
        throws Exception {
        assumeFalse(MAC_MESSAGE, isMac());
        try (LockedFile locked = new LockedFile(OPEN_TIMEOUT_MILLIS)) {
            locked.start();
            boolean gaveUp = locked.done.await(
                WAIT_FOR_EVENT_MILLIS, TimeUnit.MILLISECONDS);

            assertTrue("The watcher tries to open the new file",
                locked.tries.get() > 0);
            assertTrue("The update gives up on a file it cannot open",
                gaveUp);
            assertEquals(AutoUpdateStatus.AUTO_UPDATE_TIMEOUT,
                locked.status.get());
            assertTrue("The tries are paused, but " + locked.tries.get() +
                " were made", locked.tries.get() <= MAX_TRIES);
            locked.assertLeftAsItWas();
        }
    }

    /**
     * An interrupt, as unregistering the data file gives the watcher's task,
     * ends the wait at once. The update is left unfinished, so it reports
     * AUTO_UPDATE_IN_PROGRESS rather than a timeout, logs nothing, and leaves
     * the engine and the polling timer as they were.
     */
    @Test
    public void WatchedFileWaitInterrupted_LeavesTheUpdateUnfinished()
        throws Exception {
        assumeFalse(MAC_MESSAGE, isMac());
        try (LockedFile locked = new LockedFile(LONG_OPEN_TIMEOUT_MILLIS)) {
            locked.start();
            long end = System.currentTimeMillis() + WAIT_FOR_EVENT_MILLIS;
            while (locked.tries.get() == 0 &&
                System.currentTimeMillis() < end) {
                Thread.sleep(RETRY_MILLIS);
            }
            assertTrue("The watcher tries to open the new file",
                locked.tries.get() > 0);

            locked.runner.interrupt();
            boolean ended = locked.done.await(
                INTERRUPTED_WAIT_MILLIS, TimeUnit.MILLISECONDS);

            assertTrue("The wait ends when it is interrupted", ended);
            assertEquals(AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS,
                locked.status.get());
            locked.assertLeftAsItWas();
        }
    }

    /**
     * A watched data file that throws on every open, the service watching
     * it, and the thread that runs the watcher's task.
     */
    private static class LockedFile implements AutoCloseable {

        final List<Long> timers = new ArrayList<>();
        final AtomicReference<Runnable> watcher = new AtomicReference<>();
        final AtomicBoolean locked = new AtomicBoolean(true);
        final AtomicInteger tries = new AtomicInteger();
        final AtomicInteger logged = new AtomicInteger();
        final AtomicReference<AutoUpdateStatus> status =
            new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicBoolean stop = new AtomicBoolean(false);
        final OnPremiseAspectEngine<? extends AspectData,
            ? extends AspectPropertyMetaData> engine = engine();
        final DataUpdateServiceDefault service;
        final Path directory;
        final Path dataFile;
        final AspectEngineDataFileDefault file;
        final Thread runner;
        int loggedAtStart;

        LockedFile(long timeoutMillis) throws Exception {
            // The service may call these many times while it waits, so they
            // are stub only mocks, which keep no record of their calls.
            final BinaryReader reader =
                mock(BinaryReader.class, withSettings().stubOnly());
            FileWrapper wrapper =
                mock(FileWrapper.class, withSettings().stubOnly());
            when(wrapper.getReader()).then(invocation -> {
                tries.incrementAndGet();
                if (locked.get()) {
                    // Stops a loop with no pause using a whole processor,
                    // and leaves an interrupt for the service to see.
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                    throw new IOException("The file is locked");
                }
                return reader;
            });
            FileWrapperFactory fileWrapperFactory =
                mock(FileWrapperFactory.class, withSettings().stubOnly());
            when(fileWrapperFactory.build(anyString())).thenReturn(wrapper);
            when(fileWrapperFactory.getLastModified(anyString())).then(
                invocation -> lastModified(invocation.getArgument(0)));
            Logger logger = mock(Logger.class, withSettings().stubOnly()
                .defaultAnswer(invocation -> {
                    String name = invocation.getMethod().getName();
                    if (name.equals("warn") || name.equals("error")) {
                        logged.incrementAndGet();
                    }
                    return Answers.RETURNS_DEFAULTS.answer(invocation);
                }));
            service = new DataUpdateServiceDefault(
                logger,
                mock(HttpClient.class),
                fileWrapperFactory,
                futureFactory(timers, watcher));
            service.watchedFileOpenTimeoutMillis = timeoutMillis;
            // The first update after the change is the one that waits,
            // because only the watcher sets the time it compares with.
            service.onUpdateComplete((sender, args) -> {
                if (status.compareAndSet(null, args.getStatus())) {
                    done.countDown();
                }
            });

            directory = Files.createTempDirectory("watched");
            dataFile = directory.resolve("data.dat");
            Files.write(dataFile, "Old".getBytes());
            file = watchedFile(engine, dataFile);

            // The watcher runs on its own thread, so the test can stop
            // waiting for an update that does not end.
            runner = new Thread(() -> {
                while (stop.get() == false && done.getCount() > 0) {
                    watcher.get().run();
                    try {
                        Thread.sleep(RETRY_MILLIS);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            });
            runner.setDaemon(true);
        }

        /**
         * Registers the file, changes it, and starts the watcher's thread.
         */
        void start() throws Exception {
            service.registerDataFile(file);
            assertNotNull("Registering sets up the file watcher",
                watcher.get());
            loggedAtStart = logged.get();
            Files.write(dataFile, "New".getBytes());
            runner.start();
        }

        void assertLeftAsItWas() {
            assertEquals("The engine is not refreshed",
                0, refreshCount(engine));
            assertEquals("Nothing is logged", loggedAtStart, logged.get());
            assertEquals("The polling timer is kept", 1, timers.size());
            assertNotNull("The data file holds the polling timer",
                file.getFuture());
        }

        @Override
        public void close() throws Exception {
            // Lets an update that would not give up end, so the thread stops.
            stop.set(true);
            locked.set(false);
            if (runner.isAlive()) {
                runner.join(WAIT_FOR_EVENT_MILLIS);
            }
            cleanUp(file, directory, dataFile);
        }
    }

    /**
     * Records the polling timers scheduled, and keeps the watcher's task for
     * the test to run itself.
     */
    private static FutureFactory futureFactory(
        final List<Long> timers,
        final AtomicReference<Runnable> watcher) {
        FutureFactory futureFactory = mock(FutureFactory.class);
        when(futureFactory.schedule(any(Runnable.class), anyLong()))
            .then(invocation -> {
                timers.add((Long) invocation.getArgument(1));
                return mock(ScheduledFuture.class);
            });
        when(futureFactory.scheduleRepeating(any(Runnable.class), anyLong()))
            .then(invocation -> {
                watcher.set((Runnable) invocation.getArgument(0));
                return mock(ScheduledFuture.class);
            });
        return futureFactory;
    }

    @SuppressWarnings("unchecked")
    private static OnPremiseAspectEngine<? extends AspectData,
        ? extends AspectPropertyMetaData> engine() {
        return mock(OnPremiseAspectEngine.class);
    }

    private static AspectEngineDataFileDefault watchedFile(
        OnPremiseAspectEngine<? extends AspectData,
            ? extends AspectPropertyMetaData> engine,
        Path dataFile) {
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setFileSystemWatcherEnabled(true);
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
        // Windows keeps the file until the reader's mapped buffer is
        // collected, so whatever cannot be deleted now is deleted when the
        // test run ends. Those are deleted in the reverse order they are
        // registered, so the folder is registered first.
        directory.toFile().deleteOnExit();
        if (dataFile.toFile().delete() == false) {
            dataFile.toFile().deleteOnExit();
        }
        directory.toFile().delete();
    }

    private static boolean isMac() {
        return System.getProperty("os.name").contains("Mac OS X");
    }

    private static long lastModified(Object path) {
        return new File((String) path).lastModified();
    }

    private static long refreshCount(Object engine) {
        return mockingDetails(engine).getInvocations().stream()
            .filter(i -> i.getMethod().getName().equals("refreshData"))
            .count();
    }
}
