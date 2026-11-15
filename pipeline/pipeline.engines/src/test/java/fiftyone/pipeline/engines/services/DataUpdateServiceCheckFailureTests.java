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
import ch.qos.logback.core.read.ListAppender;
import fiftyone.common.wrappers.io.FileWrapperFactoryDefault;
import fiftyone.pipeline.engines.configuration.DataFileConfigurationDefault;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import fiftyone.pipeline.engines.services.update.FutureFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A failure in a check started by update polling or by the file watcher, as
 * the Errors section of the data update specification describes. The tasks
 * the service schedules are kept and run here directly, so nothing waits on
 * a timer.
 * @see <a href="https://github.com/51Degrees/specifications/blob/rc/pipeline-specification/features/data-updates.md#errors">Specification</a>
 */
@SuppressWarnings("unchecked")
public class DataUpdateServiceCheckFailureTests {

    private ch.qos.logback.classic.Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private FutureFactory futureFactory;
    private final List<Runnable> scheduled = new CopyOnWriteArrayList<>();
    private final List<Runnable> repeating = new CopyOnWriteArrayList<>();
    private DataUpdateServiceDefault dataUpdate;
    private OnPremiseAspectEngine<? extends AspectData,
        ? extends AspectPropertyMetaData> engine;
    private String engineType;
    private File dataFile;
    private File tempFile;

    @Before
    public void init() throws IOException {
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
            "DataUpdateServiceCheckFailureTests");
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        futureFactory = mock(FutureFactory.class);
        when(futureFactory.schedule(any(Runnable.class), anyLong()))
            .then(invocation -> {
                scheduled.add(invocation.getArgument(0));
                return mock(ScheduledFuture.class);
            });
        when(futureFactory.scheduleRepeating(any(Runnable.class), anyLong()))
            .then(invocation -> {
                repeating.add(invocation.getArgument(0));
                return mock(ScheduledFuture.class);
            });

        dataUpdate = new DataUpdateServiceDefault(
            logger, null, new FileWrapperFactoryDefault(), futureFactory);

        engine = mock(OnPremiseAspectEngine.class);
        engineType = engine.getClass().getSimpleName();
        dataFile = File.createTempFile("check-failure", ".dat");
        tempFile = File.createTempFile("check-failure", ".tmp");
    }

    @After
    public void cleanup() {
        logger.detachAppender(appender);
        dataFile.delete();
        tempFile.delete();
    }

    private AspectEngineDataFileDefault newDataFile(boolean watcher) {
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(watcher == false);
        config.setFileSystemWatcherEnabled(watcher);
        config.setUpdateOnStartup(false);
        config.setPollingIntervalSeconds(86400);
        config.setMaxRandomisationSeconds(0);
        config.setDataFilePath(dataFile.getAbsolutePath());
        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setIdentifier("default");
        file.setEngine(engine);
        file.setConfiguration(config);
        file.setTempDataFilePath(tempFile.getAbsolutePath());
        when(engine.getDataFileMetaData()).thenReturn(file);
        return file;
    }

    private String unhandled() {
        return "An unhandled error occurred while checking for automatic " +
            "updates for engine '" + engineType + "'";
    }

    private void assertLoggedAtError(String message, Throwable attached) {
        List<String> errors = new ArrayList<>();
        for (ILoggingEvent event : appender.list) {
            if (event.getLevel() == Level.ERROR) {
                errors.add(event.getFormattedMessage());
                if (event.getFormattedMessage().equals(message) &&
                    event.getThrowableProxy() != null &&
                    event.getThrowableProxy().getMessage()
                        .equals(attached.getMessage())) {
                    return;
                }
            }
        }
        fail("Expected '" + message + "' at ERROR with '" +
            attached.getMessage() + "' attached, but got " + errors);
    }

    /**
     * Run a scheduled task as the executor would. A task that throws has its
     * error kept in its future, where nothing reads it, and a repeating task
     * that throws never runs again.
     */
    private static void runAsExecutor(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            fail("The scheduled task threw, so its error is lost and a " +
                "repeating task stops: " + t);
        }
    }

    /**
     * "A failure in a check started by update polling or by the file watcher
     * MUST be caught and logged, rather than being lost or ending the
     * process, and is then reported to onUpdateCallbacks."
     * (data-updates.md line 474) A callback that throws on update polling is
     * logged at Error with the last row's message.
     */
    @Test
    public void polling_CallbackThrows_IsCaughtAndLogged() {
        AspectEngineDataFileDefault file = newDataFile(false);
        assertTrue(dataFile.setLastModified(tempFile.lastModified() + 60000));
        final RuntimeException thrown =
            new IllegalStateException("callback failed");
        final List<DataUpdateService.AutoUpdateStatus> reported =
            new CopyOnWriteArrayList<>();
        dataUpdate.onUpdateComplete((sender, args) -> {
            reported.add(args.getStatus());
            throw thrown;
        });
        dataUpdate.registerDataFile(file);
        assertEquals(1, scheduled.size());

        runAsExecutor(scheduled.get(0));

        verify(engine, times(1)).refreshData("default");
        assertEquals(Collections.singletonList(
            DataUpdateService.AutoUpdateStatus.AUTO_UPDATE_SUCCESS), reported);
        assertLoggedAtError(unhandled(), thrown);
    }

    /**
     * "A failure in a check started by update polling or by the file watcher
     * MUST be caught and logged, rather than being lost or ending the
     * process, and is then reported to onUpdateCallbacks."
     * (data-updates.md line 474) and "A failure MUST NOT stop later checks
     * for updates" (line 476). An unexpected error in a polling check is
     * logged, reported with AUTO_UPDATE_UNKNOWN_ERROR, and the next check is
     * still scheduled.
     */
    @Test
    public void polling_CheckThrows_IsLoggedReportedAndRescheduled() {
        final RuntimeException thrown =
            new IllegalStateException("check broke");
        // An unchecked error that escapes the check itself.
        FailingFile failing = new FailingFile(newDataFile(false), thrown);
        final List<String> reported = new CopyOnWriteArrayList<>();
        dataUpdate.onUpdateComplete(
            (sender, args) -> reported.add(args.getStatus().name()));
        dataUpdate.registerDataFile(failing);
        assertEquals(1, scheduled.size());
        Runnable check = scheduled.get(0);
        int before = scheduled.size();

        runAsExecutor(check);

        assertLoggedAtError(unhandled(), thrown);
        assertEquals(Collections.singletonList("AUTO_UPDATE_UNKNOWN_ERROR"),
            reported);
        assertTrue("No later check was scheduled",
            scheduled.size() > before);
    }

    /**
     * "A failure MUST NOT stop later checks for updates, so a later good
     * data file is still applied." (data-updates.md line 476) A callback
     * that throws during a watcher check must not end the repeating watcher
     * task, and the watch key is reset so the next change is seen.
     */
    @Test
    public void watcher_CallbackThrows_LaterFileStillApplied() {
        AspectEngineDataFileDefault file = newDataFile(true);
        final RuntimeException thrown =
            new IllegalStateException("callback failed");
        final boolean[] fail = {true};
        dataUpdate.onUpdateComplete((sender, args) -> {
            if (fail[0]) {
                throw thrown;
            }
        });
        dataUpdate.registerDataFile(file);
        assertEquals(1, repeating.size());
        assertNotNull(file.getConfiguration().getWatchKey());
        file.getConfiguration().getWatchKey().cancel();

        WatchEvent<Object> event = mock(WatchEvent.class);
        doReturn(StandardWatchEventKinds.ENTRY_MODIFY).when(event).kind();
        when(event.context()).thenReturn(
            Paths.get(dataFile.getName()));
        WatchKey key = mock(WatchKey.class);
        when(key.pollEvents()).thenReturn(
            Collections.<WatchEvent<?>>singletonList(event));
        file.getConfiguration().setWatchKey(key);

        assertTrue(dataFile.setLastModified(
            System.currentTimeMillis() + 60000));
        runAsExecutor(repeating.get(0));
        verify(key, atLeastOnce()).reset();
        assertLoggedAtError(unhandled(), thrown);

        // A later good file is applied by the next run.
        fail[0] = false;
        assertTrue(dataFile.setLastModified(
            System.currentTimeMillis() + 120000));
        runAsExecutor(repeating.get(0));
        verify(engine, times(2)).refreshData(anyString());
    }

    /**
     * A data file whose check throws an unchecked error that the service
     * does not expect, to stand for any such error in a polling check.
     */
    private static class FailingFile extends AspectEngineDataFileDefault {
        private final RuntimeException error;
        private volatile boolean armed = false;

        FailingFile(AspectEngineDataFileDefault from, RuntimeException error) {
            this.error = error;
            setIdentifier(from.getIdentifier());
            setEngine(from.getEngine());
            setConfiguration(from.getConfiguration());
            setTempDataFilePath(from.getTempDataFilePath());
        }

        @Override
        public void setFuture(ScheduledFuture<?> future) {
            super.setFuture(future);
            armed = true;
        }

        @Override
        public String getTempDataFilePath() {
            if (armed) {
                throw error;
            }
            return super.getTempDataFilePath();
        }
    }
}
