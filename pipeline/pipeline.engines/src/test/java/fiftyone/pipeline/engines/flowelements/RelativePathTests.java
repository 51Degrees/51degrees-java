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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.common.wrappers.io.FileWrapperFactoryDefault;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.DataUpdateServiceDefault;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.services.update.FutureFactory;
import fiftyone.pipeline.engines.testhelpers.data.EmptyEngineData;
import fiftyone.pipeline.engines.testhelpers.flowelements.TextFileEngine;
import fiftyone.pipeline.engines.testhelpers.flowelements.TextFileEngineBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A data file path, and a temporary folder path, may be relative to the
 * working directory of the process.
 * <p>
 * Specification: pipeline-specification/features/file-system-paths.md lines
 * 3 to 5 at 51Degrees/specifications 706de1f0, "Where file system paths are
 * supplied as parameters, the Pipeline API MUST be able to work with relative
 * paths. A relative path is resolved against the working directory of the
 * process."
 */
public class RelativePathTests {

    /**
     * The longest a test waits for the file system watcher to see a change.
     * On macOS, Java's watch service polls, by default every 10 seconds, so
     * an event can take 10 seconds or more to arrive. With a 10 second wait
     * this test failed on both macOS jobs of java #190's release build
     * (run 44702767584) at 10.21 s and 10.13 s. 30 seconds is three polls.
     * The loop ends as soon as the change is seen, so a fast machine waits
     * no longer than before.
     */
    private static final long WAIT_FOR_EVENT_MILLIS = 30000;

    private static final long RETRY_MILLIS = 100;

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    /**
     * The folder for the test's files, relative to the working directory.
     */
    private Path folder;

    /**
     * The data file, relative to the working directory.
     */
    private String dataFile;

    @Before
    public void createDataFile() throws Exception {
        folder = Paths.get("target", "relative-" + UUID.randomUUID());
        Files.createDirectories(folder);
        dataFile = folder.resolve("data.txt").toString();
        Files.write(
            Paths.get(dataFile),
            "Old".getBytes(StandardCharsets.UTF_8));
        assertFalse(
            "The path in the test is relative",
            Paths.get(dataFile).isAbsolute());
    }

    @After
    public void deleteFiles() {
        File[] files = folder.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.delete() == false) {
                    file.deleteOnExit();
                }
            }
        }
        if (folder.toFile().delete() == false) {
            folder.toFile().deleteOnExit();
        }
    }

    @Test
    public void dataFile_Relative_EngineReadsIt() throws Exception {
        try (TextFileEngine engine =
                 new TextFileEngineBuilder(loggerFactory, null)
                     .setAutoUpdate(false)
                     .build(dataFile, false);
             Pipeline pipeline = new PipelineBuilder(loggerFactory)
                 .addFlowElement(engine)
                 .build();
             FlowData data = pipeline.createFlowData()) {
            assertEquals("Old", engine.getText());
            assertEquals(dataFile, engine.getDataFileMetaData().getDataFilePath());

            data.process();
            assertEquals(3, data.get(EmptyEngineData.class).getValueOne());
        }
    }

    @Test
    public void tempDir_Relative_ResolvedAgainstTheWorkingDirectory()
        throws Exception {
        String tempDir = folder.resolve("temp").toString();
        try (TextFileEngine engine =
                 new TextFileEngineBuilder(loggerFactory, null)
                     .setAutoUpdate(false)
                     .setTempDirPath(tempDir)
                     .build(dataFile, false)) {
            assertEquals(
                Paths.get(tempDir).toAbsolutePath().normalize(),
                Paths.get(engine.getTempDataDirPath())
                    .toAbsolutePath().normalize());
            assertEquals(
                Paths.get(tempDir, "data.txt").toAbsolutePath().normalize(),
                Paths.get(engine.getDataFileMetaData().getTempDataFilePath())
                    .toAbsolutePath().normalize());
        }
    }

    @Test
    public void dataFile_Relative_WatcherLoadsANewFile() throws Exception {
        AtomicReference<Runnable> watcher = new AtomicReference<>();
        FutureFactory futureFactory = mock(FutureFactory.class);
        when(futureFactory.schedule(any(Runnable.class), anyLong()))
            .then(invocation -> mock(ScheduledFuture.class));
        when(futureFactory.scheduleRepeating(any(Runnable.class), anyLong()))
            .then(invocation -> {
                watcher.set(invocation.getArgument(0));
                return mock(ScheduledFuture.class);
            });
        DataUpdateServiceDefault dataUpdate = new DataUpdateServiceDefault(
            loggerFactory.getLogger(DataUpdateServiceDefault.class.getName()),
            mock(HttpClient.class),
            new FileWrapperFactoryDefault(),
            futureFactory);

        try (TextFileEngine engine =
                 new TextFileEngineBuilder(loggerFactory, dataUpdate)
                     .setAutoUpdate(false)
                     .setDataFileSystemWatcher(true)
                     .setUpdatePollingIntervalSeconds(1)
                     .build(dataFile, false)) {
            assertNotNull(
                "A watcher is set up for the relative path",
                engine.getDataFileMetaData().getConfiguration().getWatchKey());
            assertNotNull(watcher.get());

            Files.write(
                Paths.get(dataFile),
                "New".getBytes(StandardCharsets.UTF_8));
            long end = System.currentTimeMillis() + WAIT_FOR_EVENT_MILLIS;
            while ("New".equals(engine.getText()) == false &&
                System.currentTimeMillis() < end) {
                watcher.get().run();
                Thread.sleep(RETRY_MILLIS);
            }

            assertEquals("New", engine.getText());
            dataUpdate.unregisterDataFile(engine.getDataFileMetaData());
        }
    }
}
