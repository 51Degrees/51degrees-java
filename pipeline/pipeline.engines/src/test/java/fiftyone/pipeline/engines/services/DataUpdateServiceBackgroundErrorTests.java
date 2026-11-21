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
import fiftyone.common.testhelpers.TestLogger;
import fiftyone.common.wrappers.io.FileWrapperFactory;
import fiftyone.pipeline.engines.configuration.DataFileConfigurationDefault;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import fiftyone.pipeline.engines.services.update.FutureFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.concurrent.ScheduledFuture;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * An unexpected error in the background update check, or in the file system
 * watch, is logged and kept inside the task, so it can neither end the
 * thread silently nor reach the process. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/exception-handling.md#L83">exception-handling.md line 83</a>:
 * "When errors do occur, they will be logged and MUST NOT cause the
 * process to crash."
 * <p>
 * Each scheduled task is captured and run on the test's thread, so nothing
 * depends on timing.
 */
public class DataUpdateServiceBackgroundErrorTests {

    private TestLogger logger;
    private FutureFactory futureFactory;
    private FileWrapperFactory fileWrapperFactory;
    private DataUpdateServiceDefault dataUpdate;

    @Before
    public void init() {
        ch.qos.logback.classic.Logger real =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
                getClass());
        real.setLevel(Level.INFO);
        logger = new TestLogger("test", real);
        futureFactory = mock(FutureFactory.class);
        doReturn(mock(ScheduledFuture.class)).when(futureFactory)
            .schedule(any(Runnable.class), anyLong());
        doReturn(mock(ScheduledFuture.class)).when(futureFactory)
            .scheduleRepeating(any(Runnable.class), anyLong());
        fileWrapperFactory = mock(FileWrapperFactory.class);
        dataUpdate = new DataUpdateServiceDefault(
            logger,
            mock(HttpClient.class),
            fileWrapperFactory,
            futureFactory);
    }

    @SuppressWarnings("unchecked")
    private AspectEngineDataFileDefault registerFile() {
        DataFileConfigurationDefault config =
            new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setFileSystemWatcherEnabled(false);
        config.setDataFilePath("data.dat");
        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(mock(OnPremiseAspectEngine.class));
        file.setConfiguration(config);
        file.setTempDataFilePath("temp.dat");
        dataUpdate.registerDataFile(file);
        return file;
    }

    private Runnable scheduledCheck() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(futureFactory, atLeastOnce()).schedule(task.capture(), anyLong());
        return task.getValue();
    }

    @Test
    public void scheduledCheck_UnexpectedError_IsLoggedAndKeptInTheTask() {
        registerFile();
        Runnable check = scheduledCheck();
        when(fileWrapperFactory.getLastModified(anyString()))
            .thenThrow(new IllegalStateException("disk went away"));
        int errorsBefore = logger.errorsLogged.size();

        try {
            check.run();
        } catch (RuntimeException e) {
            fail("The error left the background task: " + e);
        }

        assertFalse("The error was not logged.",
            logger.errorsLogged.size() == errorsBefore);
    }

    @Test
    public void scheduledCheck_ErrorInACallback_IsLoggedAndKeptInTheTask() {
        dataUpdate.onUpdateComplete((sender, args) -> {
            throw new IllegalStateException("callback failed");
        });
        registerFile();
        Runnable check = scheduledCheck();
        when(fileWrapperFactory.getLastModified(anyString())).thenReturn(0L);
        int errorsBefore = logger.errorsLogged.size();

        try {
            check.run();
        } catch (RuntimeException e) {
            fail("The error left the background task: " + e);
        }

        assertFalse("The error was not logged.",
            logger.errorsLogged.size() == errorsBefore);
    }

    /**
     * The file system watch is a repeating task, and a repeating task that
     * throws never runs again. An error from the watch key is outside the
     * handling of an update, so only the task's wrapper keeps it in.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void fileSystemWatch_UnexpectedError_IsLoggedAndKeptInTheTask()
        throws Exception {
        Path directory = Files.createTempDirectory("watched");
        Path dataFile = directory.resolve("data.dat");
        Files.write(dataFile, "Data".getBytes());
        DataFileConfigurationDefault config =
            new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(false);
        config.setFileSystemWatcherEnabled(true);
        config.setDataFilePath(dataFile.toString());
        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(mock(OnPremiseAspectEngine.class));
        file.setConfiguration(config);
        WatchKey realKey = null;

        try {
            dataUpdate.registerDataFile(file);
            ArgumentCaptor<Runnable> task =
                ArgumentCaptor.forClass(Runnable.class);
            verify(futureFactory).scheduleRepeating(
                task.capture(), anyLong());
            Runnable watch = task.getValue();
            realKey = config.getWatchKey();
            WatchKey failingKey = mock(WatchKey.class);
            when(failingKey.pollEvents())
                .thenThrow(new IllegalStateException("watch failed"));
            config.setWatchKey(failingKey);
            int errorsBefore = logger.errorsLogged.size();

            try {
                watch.run();
            } catch (RuntimeException e) {
                fail("The error left the background task: " + e);
            }

            assertFalse("The error was not logged.",
                logger.errorsLogged.size() == errorsBefore);
        } finally {
            // The real watch key goes back so that unregistering cancels it.
            if (realKey != null) {
                config.setWatchKey(realKey);
            }
            dataUpdate.unregisterDataFile(file);
            directory.toFile().deleteOnExit();
            if (dataFile.toFile().delete() == false) {
                dataFile.toFile().deleteOnExit();
            }
            directory.toFile().delete();
        }
    }
}
