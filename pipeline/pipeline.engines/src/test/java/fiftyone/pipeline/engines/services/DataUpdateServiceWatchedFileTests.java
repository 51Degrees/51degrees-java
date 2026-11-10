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
import fiftyone.pipeline.engines.services.update.FutureFactory;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

/**
 * A new data file found by the file watcher is loaded, and then polling for
 * updates carries on, as the newFileAvailable steps of the data update
 * specification say ("resume polling, if any").
 */
public class DataUpdateServiceWatchedFileTests {

    private static final long WAIT_FOR_EVENT_MILLIS = 10000;

    private static final long RETRY_MILLIS = 100;

    @Test
    public void WatchedFileRefresh_PollingCarriesOn() throws Exception {
        assumeFalse("File watchers are not well implemented in OS X, " +
                "so don't run this unit test as it is unlikely to pass.",
            System.getProperty("os.name").contains("Mac OS X"));

        // The polling timers scheduled, and the watcher's task, which the
        // test runs itself.
        final List<Long> timers = new ArrayList<>();
        final AtomicReference<Runnable> watcher = new AtomicReference<>();
        FutureFactory futureFactory = mock(FutureFactory.class);
        when(futureFactory.schedule(any(Runnable.class), anyLong()))
            .then(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) {
                    timers.add((Long) invocation.getArgument(1));
                    return mock(ScheduledFuture.class);
                }
            });
        when(futureFactory.scheduleRepeating(any(Runnable.class), anyLong()))
            .then(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) {
                    watcher.set((Runnable) invocation.getArgument(0));
                    return mock(ScheduledFuture.class);
                }
            });
        FileWrapperFactory fileWrapperFactory = mock(FileWrapperFactory.class);
        when(fileWrapperFactory.build(anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return new FileWrapperDefault(
                    (String) invocation.getArgument(0));
            }
        });
        when(fileWrapperFactory.getLastModified(anyString()))
            .then(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) {
                    return new File((String) invocation.getArgument(0))
                        .lastModified();
                }
            });
        DataUpdateService dataUpdate = new DataUpdateServiceDefault(
            null,
            mock(HttpClient.class),
            fileWrapperFactory,
            futureFactory);

        @SuppressWarnings("unchecked")
        OnPremiseAspectEngine<? extends AspectData,
            ? extends AspectPropertyMetaData> engine =
            mock(OnPremiseAspectEngine.class);
        Path directory = Files.createTempDirectory("watched");
        Path dataFile = directory.resolve("data.dat");
        Files.write(dataFile, "Old".getBytes());

        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setAutomaticUpdatesEnabled(true);
        config.setFileSystemWatcherEnabled(true);
        config.setPollingIntervalSeconds(1);
        config.setDataFilePath(dataFile.toString());
        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setEngine(engine);
        file.setConfiguration(config);

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
            if (config.getWatchKey() != null) {
                config.getWatchKey().cancel();
            }
            Files.deleteIfExists(dataFile);
            Files.deleteIfExists(directory);
        }
    }

    private static long refreshCount(Object engine) {
        return mockingDetails(engine).getInvocations().stream()
            .filter(i -> i.getMethod().getName().equals("refreshData"))
            .count();
    }
}
