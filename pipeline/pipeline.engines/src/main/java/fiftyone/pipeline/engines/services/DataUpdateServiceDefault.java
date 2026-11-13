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
import fiftyone.common.wrappers.io.FileWrapperFactory;
import fiftyone.common.wrappers.io.FileWrapperFactoryDefault;
import fiftyone.common.wrappers.io.FileWrapperMemory;
import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import fiftyone.pipeline.engines.services.update.FutureFactory;
import fiftyone.pipeline.engines.services.update.FutureFactoryDefault;
import fiftyone.pipeline.util.Check;
import fiftyone.pipeline.util.LicenseKeyMask;
import jakarta.xml.bind.DatatypeConverter;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.*;
import java.time.Instant;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static fiftyone.pipeline.engines.services.DataUpdateService.AutoUpdateStatus.AUTO_UPDATE_REFRESH_FAILED;
import static fiftyone.pipeline.engines.services.DataUpdateService.AutoUpdateStatus.AUTO_UPDATE_SUCCESS;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

/**
 * Default singleton implementation of the {@link DataUpdateService}.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/data-updates.md">Specification</a>
 */
public class DataUpdateServiceDefault implements DataUpdateService {

    /**
     * The query parameter of a data update URL that holds the license keys,
     * as the 51Degrees URL formatter writes it.
     */
    private static final String LICENSE_KEYS_PARAMETER = "LicenseKeys";

    /**
     * How long a watched data file is tried before the update gives up with
     * {@link AutoUpdateStatus#AUTO_UPDATE_TIMEOUT}, as a copy over it may
     * still be in progress.
     */
    static final long WATCHED_FILE_OPEN_TIMEOUT_MILLIS =
            TimeUnit.MINUTES.toMillis(10);

    /**
     * The pause between tries to open a watched data file.
     */
    static final long WATCHED_FILE_OPEN_PAUSE_MILLIS = 500;

    /**
     * The wait for a watched data file, which tests shorten.
     */
    long watchedFileOpenTimeoutMillis = WATCHED_FILE_OPEN_TIMEOUT_MILLIS;

    final List<AspectEngineDataFile> configurations = new ArrayList<>();
    /**
     * The watch service of each watched data file, held under the
     * configuration lock. A watch service runs a thread of its own, so it is
     * closed when its data file is unregistered.
     */
    private final Map<AspectEngineDataFile, WatchService> watchServices =
            new IdentityHashMap<>();
    private final Logger logger;
    private final boolean closeFutureFactory;
    private final List<OnUpdateComplete> onUpdateCompleteList = new ArrayList<>();
    private final Object configLock = new Object();
    /**
     * Held for every read and write this service makes of a data file's
     * future, so a check cannot read or clear its future before
     * {@link #registerDataFile} has stored it. Only the future is scheduled,
     * cancelled and stored while holding it, so it is never held while
     * waiting for the configuration lock or a data file's update lock.
     */
    private final Object futureLock = new Object();
    /**
     * HTTP client used to download new data files.
     */
    private final HttpClient httpClient;
    /**
     * Random number generator used to vary the scheduled times.
     */
    private final Random random = new Random(new Date().getTime());
    /**
     * Data file factory used to access data files.
     */
    private final FileWrapperFactory fileWrapperFactory;
    private final FutureFactory futureFactory;

    /**
     * Constructor with default for everything
     */
    @SuppressWarnings("unused")
    public DataUpdateServiceDefault() {
        this(null, null, null, null);
    }

    /**
     * Construct a new instance of {@link DataUpdateService}.
     *
     * @param logger     the logger to use for logging
     * @param httpClient the HTTP client used to download new data files
     */
    public DataUpdateServiceDefault(
            Logger logger,
            HttpClient httpClient) {
        this(logger, httpClient, null, null);
    }

    /**
     * Construct a new instance of {@link DataUpdateService}.
     *
     * @param logger             the logger to use for logging
     * @param httpClient         the HTTP client used to download new data files
     * @param fileWrapperFactory the factory to create the file wrappers used to
     *                           access files
     * @param futureFactory      the future factory used to create update threads.
     *                           NOTE: This factory will be closed when the Data Update Service gets
     *                           closed so it is recommended to not share this factory, as
     *                           scheduled tasks from other objects might be shutdown unexpectedly.
     */
    public DataUpdateServiceDefault(
            Logger logger,
            HttpClient httpClient,
            FileWrapperFactory fileWrapperFactory,
            FutureFactory futureFactory) {
        if (Objects.nonNull(logger)) {
            this.logger = logger;
        } else {
            this.logger = LoggerFactory.getLogger(this.getClass());
        }
        if (fileWrapperFactory != null) {
            this.fileWrapperFactory = fileWrapperFactory;
        } else {
            this.fileWrapperFactory = new FileWrapperFactoryDefault();
        }
        if (httpClient != null) {
            this.httpClient = httpClient;
        } else {
            this.httpClient = new HttpClientDefault();
        }
        if (futureFactory != null) {
            closeFutureFactory = false;
            this.futureFactory = futureFactory;
        } else {
            closeFutureFactory = true;
            this.futureFactory = new FutureFactoryDefault();
        }
    }

    @Override
    public void onUpdateComplete(OnUpdateComplete onUpdateComplete) {
        onUpdateCompleteList.add(onUpdateComplete);
    }

    @Override
    public AutoUpdateStatus checkForUpdate(OnPremiseAspectEngine<? extends AspectData, ?
            extends AspectPropertyMetaData> engine) {
        return checkForUpdate(engine.getDataFileMetaData(), true);
    }

    @Override
    public AutoUpdateStatus checkForUpdate(OnPremiseAspectEngine<? extends AspectData, ?
            extends AspectPropertyMetaData> engine, String identifier) {
        return checkForUpdate(engine.getDataFileMetaData(identifier), true);
    }

    /**
     * Private method that performs the following actions:
     * 1. Checks for an update to the data file on disk.
     * 2. Checks for an update using the update URL.
     * 3. Refresh engine with new data if available.
     * 4. Schedule the next update check if needed.
     *
     * @param state        the data file meta data
     * @param manualUpdate true if the checkForUpdate was manually called, false
     *                     if it was called as the result of an automated update
     * @return {@link AutoUpdateStatus#AUTO_UPDATE_SUCCESS} if the data file was
     * successfully updated
     */
    private AutoUpdateStatus checkForUpdate(Object state, boolean manualUpdate) {

        AutoUpdateStatus result = AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS;
        final AspectEngineDataFileDefault dataFile =
                state instanceof AspectEngineDataFileDefault ?
                        (AspectEngineDataFileDefault) state :
                        null;
        boolean newDataAvailable = false;
        if (dataFile != null) {
            logger.info("Checking for update");
            logger.debug("CheckForUpdate {}", dataFile.getDataFilePath());
            // Compare the data file with the temporary file the engine uses.
            // A programmatic update always does, whether or not Automatic
            // Update from File is enabled. The timer does only when no file
            // system watcher exists to notice a new file.
            if ((manualUpdate ||
                    dataFile.getConfiguration().getWatchKey() == null) &&
                    dataFile.getDataFilePath() != null &&
                    dataFile.getDataFilePath().isEmpty() == false &&
                    dataFile.getTempDataFilePath() != null &&
                    dataFile.getTempDataFilePath().isEmpty() == false) {
                long fileModified = fileWrapperFactory.getLastModified(
                        dataFile.getDataFilePath());
                long tempFileModified = fileWrapperFactory.getLastModified(
                        dataFile.getTempDataFilePath());

                // If the data file is newer than the temp file currently
                // being used by the engine then we need to tell the engine
                // to refresh itself.
                if (fileModified > tempFileModified) {
                    logger.debug("Specified file is newer than existing temp file");
                    newDataAvailable = true;
                }
            }
            if (newDataAvailable == false &&
                    dataFile.getConfiguration().getDataUpdateUrl() != null &&
                    dataFile.getConfiguration().getDataUpdateUrl().isEmpty() == false) {
                // A failed update is tried again only when the timer is on.
                result = checkForUpdateFromUrl(
                        dataFile,
                        manualUpdate == false &&
                                dataFile.getAutomaticUpdatesEnabled());
                newDataAvailable =
                        result == AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS ||
                                result == AutoUpdateStatus.AUTO_UPDATE_SUCCESS;
            }
            if (newDataAvailable &&
                    result == AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS) {
                // Data update was available but engine has not
                // yet been refreshed.
                logger.debug("Calling updatedFileAvailable");
                result = updatedFileAvailable(dataFile);
            } else if (result == AUTO_UPDATE_SUCCESS) {
                // The update was applied in memory. Clear the timer, as
                // updatedFileAvailable does, so a new one is registered.
                synchronized (futureLock) {
                    if (dataFile.getFuture() != null) {
                        dataFile.getFuture().cancel(true);
                        dataFile.setFuture(null);
                    }
                }
            } else {
                // No update available.
                // If this was a manual call to update then do nothing.
                // If it was triggered by the timer expiring then modify
                // the timer to check again after the configured interval.
                // This will repeat until the update is acquired.
                if (manualUpdate == false) {
                    logger.debug("Cancelling and resetting update timer");
                    synchronized (futureLock) {
                        if (dataFile.getFuture() != null) {
                            dataFile.getFuture().cancel(true);
                            dataFile.setFuture(futureFactory.schedule(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            checkForUpdate(dataFile, false);
                                        }
                                    },
                                    getIntervalMillis(dataFile.getConfiguration())));
                        }
                    }
                }
            }
            if (manualUpdate == false ||
                    ((result == AUTO_UPDATE_SUCCESS ||
                            result == AUTO_UPDATE_REFRESH_FAILED) &&
                            dataFile.getIsRegistered())) {
                // Re-register the engine with the data update service
                // so it knows when next set of data should be available.
                // A manual update of a registered data file does this too,
                // whether or not the refresh worked, as a refresh from the
                // data file clears the timer, so polling resumes after it.
                registerDataFile(dataFile);
            }

        }
        for (OnUpdateComplete onUpdateComplete : onUpdateCompleteList) {
            onUpdateComplete.call(state, new DataUpdateCompleteArgs(
                    result,
                    dataFile));
        }
        return result;
    }

    /**
     * Called when a data update is available and the file at
     * engine.DataFilePath contains this new data.
     * 1. Dispose of the existing update timer if there is one.
     * 2. Refresh the engine.
     * Where the data file is registered, the caller then registers it again,
     * which sets a new update timer.
     *
     * @param dataFile the data file to update
     * @return {@link AutoUpdateStatus#AUTO_UPDATE_SUCCESS} if the data file was
     * successfully updated
     */
    private AutoUpdateStatus updatedFileAvailable(AspectEngineDataFile dataFile) {
        AspectEngineDataFileDefault aspectDataFile = (AspectEngineDataFileDefault) dataFile;

        synchronized (futureLock) {
            if (aspectDataFile.getFuture() != null) {
                // Dispose of the old timer object
                aspectDataFile.getFuture().cancel(true);
                aspectDataFile.setFuture(null);
            }
        }

        if (dataFile.getEngine() == null) {
            // Engine not yet set so no need to refresh it.
            // We can consider the update a success.
            logger.debug("No engine to update so we are done");
            return AutoUpdateStatus.AUTO_UPDATE_SUCCESS;
        }

        logger.info("Attempting to refresh Engine '{}' with new data",
                getEngineType(dataFile));
        // Try to update the file multiple times to ensure the file is not locked.
        int tries = 0;
        Exception lastFailure = null;
        while (tries < 10) {
            try {
                dataFile.getEngine().refreshData(dataFile.getIdentifier());
                return AUTO_UPDATE_SUCCESS;
            } catch (Exception ex) {
                lastFailure = ex;
                logger.debug("Refresh attempt {} failed", tries + 1, ex);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
            }
            tries++;
        }
        logRefreshFailure(dataFile, lastFailure, tries);
        return AUTO_UPDATE_REFRESH_FAILED;
    }

    /**
     * Event handler that is called when the data file is updated. The
     * {@link WatchKey} will raise multiple events in many cases, for example,
     * if a file is copied over an existing file then 3 'changed' events will be
     * raised. This handler deals with the extra events by using synchronisation
     * with a double-check lock to ensure that the update will only be done once.
     *
     * @param sender the {@link AspectEngineDataFile}
     */
    private void dataFileUpdated(Object sender) {
        AutoUpdateStatus status = AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS;
        // Get the associated update configuration

        AspectEngineDataFile dataFile = null;
        for (AspectEngineDataFile current : configurations) {
            if (current.equals(sender)) {
                dataFile = current;
                break;
            }
        }

        if (dataFile != null) {
            AspectEngineDataFileDefault aspectDataFile = (AspectEngineDataFileDefault) dataFile;
            // Get the creation time of the new data file
            long modifiedTime = fileWrapperFactory.getLastModified(dataFile.getDataFilePath());
            // Use a lock with a double check on file creation time to make
            // sure we only run the update once even if multiple events fire
            // for a single file.
            if (aspectDataFile.getLastUpdateFileCreateTime() < modifiedTime) {
                synchronized (aspectDataFile.getUpdateSyncLock()) {
                    if (aspectDataFile.getLastUpdateFileCreateTime() < modifiedTime) {
                        aspectDataFile.setLastUpdateFileCreateTime(modifiedTime);

                        // Make sure we can actually open the file for reading
                        // before notifying the engine, otherwise the copy
                        // may still be in progress.
                        boolean fileLockable = false;
                        boolean interrupted = false;
                        long deadline = System.nanoTime() +
                                TimeUnit.MILLISECONDS.toNanos(
                                        watchedFileOpenTimeoutMillis);
                        while (fileLockable == false &&
                                System.nanoTime() - deadline < 0) {
                            try (BinaryReader reader = fileWrapperFactory.build(
                                    aspectDataFile.getDataFilePath()).getReader()) {
                                fileLockable = true;
                            } catch (IOException ignored) {
                                try {
                                    Thread.sleep(WATCHED_FILE_OPEN_PAUSE_MILLIS);
                                } catch (InterruptedException ex) {
                                    // Interrupted, as unregistering the data
                                    // file does, so stop trying.
                                    Thread.currentThread().interrupt();
                                    interrupted = true;
                                    break;
                                }
                            }
                        }
                        if (fileLockable) {
                            // Complete the update
                            status = updatedFileAvailable(dataFile);
                            // The refresh cancelled the polling timer, so
                            // register the file again to set a new one.
                            if (dataFile.getIsRegistered()) {
                                registerDataFile(dataFile);
                            }
                        } else if (interrupted == false) {
                            // The polling timer is left as it was.
                            status = AutoUpdateStatus.AUTO_UPDATE_TIMEOUT;
                        }
                        // An interrupted wait leaves the update unfinished,
                        // so the status stays AUTO_UPDATE_IN_PROGRESS.
                    }
                }
            }
        }
        for (OnUpdateComplete onUpdateComplete : onUpdateCompleteList) {
            onUpdateComplete.call(sender, new DataUpdateCompleteArgs(status, dataFile));
        }
    }

    @Override
    public AutoUpdateStatus updateFromMemory(AspectEngineDataFile dataFile, byte[] data) {
        AutoUpdateStatus result;
        if (dataFile.getDataFilePath() != null &&
                dataFile.getDataFilePath().isEmpty() == false) {
            // The engine has an associated data file so update it first.
            try {
                fileWrapperFactory.build(dataFile.getDataFilePath()).getWriter()
                        .writeBytes(data);
            } catch (Exception ex) {
                logger.error("An error occurred when writing to " +
                                dataFile.getDataFilePath() +
                                ". Error detail: " + ex.getMessage(), ex);
            }
        }

        try {
            // Refresh the engine using the new data.
            logger.info("Attempting to refresh Engine '{}' with new data",
                    getEngineType(dataFile));
            dataFile.getEngine().refreshData(dataFile.getIdentifier(), data);
            result = AUTO_UPDATE_SUCCESS;
        } catch (Exception ex) {
            logRefreshFailure(dataFile, ex, null);
            result = AutoUpdateStatus.AUTO_UPDATE_REFRESH_FAILED;
        }

        return result;
    }

    @Override
    public void registerDataFile(final AspectEngineDataFile dataFile) {
        logger.debug("Entering registerDataFile");
        if (dataFile == null) {
            throw new IllegalArgumentException("updateConfig");
        }
        boolean alreadyRegistered = dataFile.getIsRegistered();
        dataFile.setDataUpdateService(this);

        // If the data file is configured to refresh the data
        // file on startup then download an update immediately.
        // We also want to do this synchronously so that execution
        // will block until the engine is ready.
        if (dataFile.getConfiguration().getUpdateOnStartup() && isFalse(alreadyRegistered)) {
            logger.info("Updating on start-up");
            logger.debug("Checking for Update on Startup {}", dataFile.getDataFilePath());
            AutoUpdateStatus status = checkForUpdate(dataFile, false);
            if (ObjectUtils.notEqual(status, AUTO_UPDATE_SUCCESS) &&
                    Check.notFileExists(dataFile.getDataFilePath())){
                throw new IllegalStateException("Update on start up failed and there is no " +
                        "existing data file. Status was " + status);
            }
        } else {
            final AspectEngineDataFileDefault aspectDataFile =
                    (AspectEngineDataFileDefault) dataFile;

            // Only create an automatic update timer if auto updates are
            // enabled for this engine and there is not already an associated
            // timer. The lock is held from scheduling to storing, because
            // the check can start as soon as it is scheduled and must find
            // its own future.
            synchronized (futureLock) {
                if (aspectDataFile.getAutomaticUpdatesEnabled() &&
                        aspectDataFile.getFuture() == null) {
                    long delayMillis =
                            getIntervalMillis(aspectDataFile.getConfiguration());
                    if (aspectDataFile.getUpdateAvailableTime() != null) {
                        if (aspectDataFile.getUpdateAvailableTime().getTime()
                                > System.currentTimeMillis()) {
                            delayMillis = applyIntervalRandomisationMillis(
                                    aspectDataFile.getUpdateAvailableTime().getTime()
                                            - System.currentTimeMillis(),
                                    aspectDataFile.getConfiguration());
                        }
                    }
                    // Create a timer that will go off when the engine expects
                    // updated data to be available.
                    logger.debug("Scheduling update check with delay {} millis",
                            delayMillis);
                    try {
                        ScheduledFuture<?> future = futureFactory.schedule(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        logger.debug("running checkForUpdate {}",
                                                aspectDataFile.getDataFilePath());
                                        checkForUpdate(aspectDataFile, false);
                                        logger.debug("Returning from checkForUpdate");
                                    }
                                },
                                delayMillis);
                        logger.debug("Update check scheduled for {} millis",
                                future.getDelay(TimeUnit.MILLISECONDS));
                        aspectDataFile.setFuture(future);
                    } catch (Throwable t) {
                        logger.error("Exception when scheduling update check", t);
                    }
                }
            }

            // If file system watcher is enabled then set it up. The watcher
            // runs on its own setting. Watching a file downloads nothing, so
            // it needs neither automatic updates nor a license key.
            if (aspectDataFile.getConfiguration().getFileSystemWatcherEnabled() &&
                    aspectDataFile.getConfiguration().getWatchKey() == null &&
                    aspectDataFile.getDataFilePath() != null &&
                    aspectDataFile.getDataFilePath().isEmpty() == false) {
                logger.info("Creating file system watcher");
                logger.debug("Initialising watch service");
                final Path aspectDataFilePath =
                        Paths.get(aspectDataFile.getDataFilePath()).toAbsolutePath();

                try {
                    if (Check.notFileExists(aspectDataFilePath.getParent().toString())) {
                        logger.error("Parent directory {} for {} does not exist",
                                aspectDataFilePath.getParent(), aspectDataFile.getDataFilePath());
                        throw new IllegalStateException("Cannot find directory to watch");
                    }
                    logger.debug("Getting a watcher using {}", aspectDataFilePath);
                    WatchService watcher =
                            watchService(aspectDataFile, aspectDataFilePath);
                    logger.debug("Setting a watcher for {}", aspectDataFilePath.getParent());
                    aspectDataFile.getConfiguration().setWatchKey(aspectDataFilePath.getParent().register(
                            watcher,
                            StandardWatchEventKinds.ENTRY_MODIFY));
                } catch (Exception e) {
                    logger.error("File watcher for '{}' could not be initialised. " + e.getMessage(),
                            aspectDataFile.getDataFilePath(), e);
                }
                try {
                    long pollingInterval =
                            aspectDataFile.getConfiguration().getPollingIntervalSeconds() * 1000L;
                    logger.debug("Setting poll future {} millis", pollingInterval);
                    aspectDataFile.setPollFuture(futureFactory.scheduleRepeating(
                            new Runnable() {
                                @Override
                                public void run() {
                                    logger.debug("Watcher is running");
                                    // The watch is paused while a downloaded
                                    // file is copied into place. A repeating
                                    // task that fails never runs again, so
                                    // this run does nothing.
                                    WatchKey watchKey =
                                            aspectDataFile.getConfiguration().getWatchKey();
                                    if (watchKey == null) {
                                        return;
                                    }
                                    for (WatchEvent<?> event : watchKey.pollEvents()) {
                                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                            if (((Path) event.context()).endsWith(aspectDataFilePath.getFileName())) {
                                                dataFileUpdated(aspectDataFile);
                                            }
                                        }
                                    }
                                    // make sure to reset the watch key or we are not called again
                                    watchKey.reset();
                                }
                            },
                            pollingInterval));
                    logger.debug("Poll check scheduled for {} mins",
                            aspectDataFile.getPollFuture().getDelay(TimeUnit.MINUTES));
                } catch (Exception e) {
                    logger.error("Could not schedule watcher for {}.",
                            aspectDataFile.getDataFilePath(), e);
                }
            }

            synchronized (configLock) {
                // Add the configuration to the list of configurations.
                if (configurations.contains(aspectDataFile) == false) {
                    configurations.add(aspectDataFile);
                }
            }
        }
        logger.debug("Returning from registerDataFile");
    }

    @Override
    public void unregisterDataFile(AspectEngineDataFile dataFile) {
        synchronized (configLock) {
            for (Iterator<AspectEngineDataFile> iterator = configurations.iterator();
                 iterator.hasNext(); ) {
                AspectEngineDataFile config = iterator.next();
                if (config == dataFile) {
                    iterator.remove();
                }
                AspectEngineDataFileDefault aspectDataFile =
                        (AspectEngineDataFileDefault) dataFile;
                // The watcher's task is stopped even while the watch is
                // paused for a download.
                if (aspectDataFile.getPollFuture() != null) {
                    aspectDataFile.getPollFuture().cancel(true);
                    aspectDataFile.setPollFuture(null);
                }
                if (aspectDataFile.getConfiguration().getWatchKey() != null) {
                    aspectDataFile.getConfiguration().getWatchKey().cancel();
                    aspectDataFile.getConfiguration().setWatchKey(null);
                }
                synchronized (futureLock) {
                    if (aspectDataFile.getFuture() != null) {
                        aspectDataFile.getFuture().cancel(true);
                        aspectDataFile.setFuture(null);
                    }
                }
            }
            closeWatchService(dataFile);
        }
        if (dataFile != null) {
            dataFile.setDataUpdateService(null);
        }
    }

    /**
     * Get the data file's watch service, creating it the first time. The
     * data file keeps the one watch service until it is unregistered, so the
     * watch resumes on it after a download.
     *
     * @param dataFile the data file to watch
     * @param path the absolute path of the data file
     * @return the data file's watch service
     * @throws IOException if a watch service cannot be created
     */
    private WatchService watchService(
            AspectEngineDataFile dataFile,
            Path path) throws IOException {
        synchronized (configLock) {
            WatchService watcher = watchServices.get(dataFile);
            if (watcher == null) {
                watcher = path.getFileSystem().newWatchService();
                watchServices.put(dataFile, watcher);
            }
            return watcher;
        }
    }

    /**
     * Close the data file's watch service, if it has one, which ends the
     * watch service's thread.
     *
     * @param dataFile the data file no longer watched
     */
    private void closeWatchService(AspectEngineDataFile dataFile) {
        WatchService watcher;
        synchronized (configLock) {
            watcher = watchServices.remove(dataFile);
        }
        if (watcher != null) {
            try {
                watcher.close();
            } catch (IOException e) {
                logger.warn("Could not close the file watcher for '{}'",
                        dataFile.getDataFilePath(), e);
            }
        }
    }

    /**
     * Get the polling interval for a data file plus a random number of
     * seconds, no greater than the configured maximum randomisation.
     *
     * @param config to get the interval for
     * @return the interval in milliseconds.
     */
    private long getIntervalMillis(DataFileConfiguration config) {
        long intervalMillis = 0;
        if (config.getPollingIntervalSeconds() > 0) {
            intervalMillis = config.getPollingIntervalSeconds() * 1000L;
        }
        return applyIntervalRandomisationMillis(intervalMillis, config);
    }

    /**
     * Add a random number of whole seconds, up to the maximum in the
     * configuration, to the specified interval.
     *
     * @param intervalMillis the interval in milliseconds to add to
     * @param config         the {@link DataFileConfiguration} object that
     *                       specifies the maximum number of seconds to add
     * @return the new interval in milliseconds
     */
    private long applyIntervalRandomisationMillis(
            long intervalMillis,
            DataFileConfiguration config) {
        long randomSeconds = 0;
        if (config.getMaxRandomisationSeconds() > 0) {
            randomSeconds = random.nextInt(config.getMaxRandomisationSeconds());
        }
        return intervalMillis + randomSeconds * 1000;
    }

    /**
     * The license keys to mask in messages that name the data update URL,
     * being the data update license keys and any key in the URL's
     * LicenseKeys parameter, such as one typed straight into the data
     * update URL.
     * @param dataFile the data file being updated
     * @param url the formatted data update URL
     * @return the keys to mask
     */
    private static List<String> licenseKeysToMask(
            AspectEngineDataFile dataFile,
            String url) {
        List<String> keys = new ArrayList<>();
        List<String> configured =
                dataFile.getConfiguration().getDataUpdateLicenseKeys();
        if (configured != null) {
            keys.addAll(configured);
        }
        int query = url == null ? -1 : url.indexOf('?');
        if (query < 0) {
            return keys;
        }
        for (String parameter : url.substring(query + 1).split("&")) {
            int equals = parameter.indexOf('=');
            if (equals > 0 && parameter.substring(0, equals)
                    .equalsIgnoreCase(LICENSE_KEYS_PARAMETER)) {
                String value = parameter.substring(equals + 1);
                try {
                    value = URLDecoder.decode(value, "UTF-8");
                } catch (UnsupportedEncodingException |
                         IllegalArgumentException e) {
                    // Masked as it is written.
                }
                keys.addAll(Arrays.asList(value.split("\\|")));
            }
        }
        return keys;
    }

    /**
     * Get the most recent data file available from the configured update URL.
     * If the data currently used by the engine is the newest available then
     * nothing will be downloaded.
     *
     * @param dataFile  the data file to use
     * @param tempFile  the temp file to write the data to
     * @param automatic true if the update will be tried again if it fails
     * @return the {@link AutoUpdateStatus} value indicating the result
     */
    private AutoUpdateStatus downloadFile(
            AspectEngineDataFile dataFile,
            FileWrapper tempFile,
            boolean automatic) {

        String url = dataFile.getFormattedUrl();
        String engineType = getEngineType(dataFile);
        // The URL carries the license keys, so every message names it with
        // each key masked.
        List<String> licenseKeys = licenseKeysToMask(dataFile, url);
        String loggedUrl = LicenseKeyMask.maskIn(url, licenseKeys);
        logger.debug("downloadFile from {}", loggedUrl);

        try {
            logger.info("Checking for update from '{}' for Engine '{}'",
                    loggedUrl, engineType);
            HttpURLConnection connection = httpClient.connect(new URI(url.trim()).toURL());
            if (connection == null) {
                logger.warn(connectFailure(loggedUrl, engineType, automatic,
                        "No response from the data update service"));
                return AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR;
            }
            try {
                // Set last-modified header to ensure that a file will only
                // be downloaded if it is newer than the data we already have.
                long ifModifiedSince = 0;
                if (dataFile.getConfiguration().getVerifyModifiedSince() == true) {
                    ifModifiedSince = getIfModifiedSince(dataFile);
                    connection.setIfModifiedSince(ifModifiedSince);
                }

                connection.setInstanceFollowRedirects(true);

                switch (connection.getResponseCode()) {
                    case (HttpURLConnection.HTTP_OK):
                        AutoUpdateStatus downloaded;
                        try {
                            downloaded = downloadOkResponse(
                                    connection, dataFile, tempFile, automatic);
                        } catch (Exception e) {
                            // An HTTP library's exception often names the
                            // URL it requested.
                            logger.warn(downloadFailure(loggedUrl, engineType,
                                    automatic, LicenseKeyMask.maskIn(
                                            e.getMessage(), licenseKeys)),
                                    LicenseKeyMask.maskIn(e, licenseKeys));
                            return AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR;
                        }
                        if (downloaded == AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS) {
                            logger.info("Downloaded new data from '{}' for Engine '{}'",
                                    loggedUrl, engineType);
                        }
                        return downloaded;

                    // Note: needed because TooManyRequests is not available
                    // in some versions of the HttpStatusCode enum.
                    case (429):
                        logger.warn(downloadFailure(loggedUrl, engineType, automatic,
                                "HTTP status code 429 (too many requests)"));
                        return AutoUpdateStatus.AUTO_UPDATE_ERR_429_TOO_MANY_ATTEMPTS;
                    case HttpURLConnection.HTTP_NOT_MODIFIED:
                        logger.info("No data newer than {} found at '{}' for Engine '{}'",
                                Instant.ofEpochMilli(ifModifiedSince), loggedUrl,
                                engineType);
                        return AutoUpdateStatus.AUTO_UPDATE_NOT_NEEDED;
                    case HttpURLConnection.HTTP_FORBIDDEN:
                        logger.warn(downloadFailure(loggedUrl, engineType, automatic,
                                "HTTP status code 403 (access denied)"));
                        return AutoUpdateStatus.AUTO_UPDATE_ERR_403_FORBIDDEN;
                    default:
                        logger.warn(downloadFailure(loggedUrl, engineType, automatic,
                                "HTTP status code " + connection.getResponseCode()));
                        return AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR;
                }
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            // An HTTP library's exception often names the URL it requested.
            logger.warn(connectFailure(loggedUrl, engineType, automatic,
                    LicenseKeyMask.maskIn(e.getMessage(), licenseKeys)),
                    LicenseKeyMask.maskIn(e, licenseKeys));
            return AutoUpdateStatus.AUTO_UPDATE_HTTPS_ERR;
        }
    }

    /**
     * Get the time to send as If-Modified-Since. This is the publication date
     * of the data file, or the last modified time of the data file on disk
     * where the data file has no publication date.
     */
    private long getIfModifiedSince(AspectEngineDataFile dataFile) {
        Date published = dataFile.getDataPublishedDateTime();
        long publishedMillis = published == null ? 0 : published.getTime();
        if (publishedMillis > 0) {
            return publishedMillis;
        }
        String path = dataFile.getDataFilePath();
        if (path != null && path.isEmpty() == false) {
            return fileWrapperFactory.getLastModified(path);
        }
        return publishedMillis;
    }

    private static String retryNote(boolean automatic) {
        return automatic ? " Update will be attempted again later." : "";
    }

    /**
     * The warning for a failed connection. The URL and the detail are
     * given with each license key already masked.
     */
    private static String connectFailure(
            String loggedUrl,
            String engineType,
            boolean automatic,
            String detail) {
        return "An error occurred when connecting to " + loggedUrl +
                " in order to check for data file updates for " + engineType +
                "." + retryNote(automatic) + " Error detail: " + detail;
    }

    /**
     * The warning for a failed download. The URL and the detail are given
     * with each license key already masked.
     */
    private static String downloadFailure(
            String loggedUrl,
            String engineType,
            boolean automatic,
            String detail) {
        return "An error occurred while downloading a data file update for " +
                engineType + " from " + loggedUrl + "." + retryNote(automatic) +
                " Error detail: " + detail;
    }

    /**
     * Log the Errors table's message for a refresh that failed, with the
     * number of tries for a refresh from the file and without it for one
     * from data held in memory. The exception is passed to the logger, so
     * its detail is kept beside the message.
     */
    private void logRefreshFailure(
        AspectEngineDataFile dataFile,
        Exception ex,
        Integer tries) {
        logger.error("An error occurred when applying a data update to " +
                "engine '" + getEngineType(dataFile) + "'" +
                (tries == null ? "." : " after " + tries + " tries."), ex);
    }

    private String getEngineType(AspectEngineDataFile dataFile) {
        if (dataFile.getEngine() != null) {
            return dataFile.getEngine().getClass().getSimpleName();
        }
        // The engine is built after update on start-up has run.
        if (dataFile instanceof AspectEngineDataFileDefault &&
                ((AspectEngineDataFileDefault) dataFile).getEngineTypeName() != null) {
            return ((AspectEngineDataFileDefault) dataFile).getEngineTypeName();
        }
        return "No Engine";
    }

    /**
     * Persist the body of a 200 OK download to {@code tempFile}.
     * <p>
     * The response stream is wrapped via {@link #openResponseStream}
     * before being chained through the MD5 digest and (optionally) a
     * {@link GZIPInputStream}, so the JDK's concatenated-gzip-member
     * decoder behaves correctly on top of {@link HttpURLConnection} on
     * every JDK from 7 upwards. See that method for the gory details.
     * <p>
     * A failure reading the download is thrown, for the caller to report.
     * A failure writing {@code tempFile} is logged here as the
     * specification's write error.
     */
    private AutoUpdateStatus downloadOkResponse(
            HttpURLConnection connection,
            AspectEngineDataFile dataFile,
            FileWrapper tempFile,
            boolean automatic) throws IOException {
        logger.debug("HTTP Status is OK");
        boolean verifyMd5 = dataFile.getConfiguration().getVerifyMd5();
        boolean decompress = dataFile.getConfiguration().getDecompressContent();

        InputStream src = openResponseStream(connection);

        MessageDigest md = null;
        if (verifyMd5) {
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                logger.error("MD5 Algorithm not found");
                return AutoUpdateStatus.AUTO_UPDATE_ERR_MD5_VALIDATION_FAILED;
            }
            src = new DigestInputStream(src, md);
        }
        if (decompress) {
            src = new GZIPInputStream(src);
        }

        ReadFailureRecorder body = new ReadFailureRecorder(src);
        try {
            writeBodyTo(tempFile, body);
        } catch (IOException e) {
            if (body.readFailed) {
                // The download failed, which the caller reports.
                throw e;
            }
            logger.error("An error occurred when writing to " +
                    tempFile.getPath() + ". Error detail: " + e.getMessage(), e);
            return AutoUpdateStatus.AUTO_UPDATE_ERR_READING_STREAM;
        }

        if (verifyMd5 && !md5Matches(md, connection)) {
            logger.warn("An error occurred during the integrity check of new " +
                    "data file for " + getEngineType(dataFile) + "." +
                    retryNote(automatic) +
                    " Error detail: the MD5 hash of the download does not " +
                    "match the Content-MD5 header");
            return AutoUpdateStatus.AUTO_UPDATE_ERR_MD5_VALIDATION_FAILED;
        }
        return AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS;
    }

    /**
     * Wrap the HTTP response in a buffered stream whose
     * {@link InputStream#available()} never reports 0 before genuine
     * end-of-stream.
     * <p>
     * The Distributor serves the Enterprise hash and IPI payloads as
     * concatenated gzip members. On JDKs prior to 23,
     * {@code GZIPInputStream.readTrailer()} consults
     * {@code this.in.available()} to decide whether to look for the
     * next member; {@code HttpURLConnection}'s input stream reports 0
     * momentarily between TCP frames at a member boundary, causing
     * {@code GZIPInputStream} to declare end-of-stream and silently
     * drop the rest of the payload. Forcing a positive value here
     * makes the JDK attempt to read the next gzip header; when the
     * stream really is exhausted the underlying {@code read()} returns
     * {@code -1}, {@code readHeader()} throws {@code EOFException},
     * and {@code readTrailer()} catches it and reports end-of-stream
     * cleanly. JDK 23+ removed the gated check entirely, so this
     * wrapper is harmless there.
     */
    private static InputStream openResponseStream(HttpURLConnection connection)
            throws IOException {
        InputStream lying = new FilterInputStream(connection.getInputStream()) {
            @Override
            public int available() throws IOException {
                int a = super.available();
                return a > 0 ? a : 1;
            }
        };
        return new BufferedInputStream(lying);
    }

    /**
     * Records whether a read of the download failed, so that a failure to
     * write the temporary file can be told from a failure to download.
     */
    private static final class ReadFailureRecorder extends FilterInputStream {
        private boolean readFailed;

        ReadFailureRecorder(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            try {
                return super.read();
            } catch (IOException e) {
                readFailed = true;
                throw e;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            try {
                return super.read(b, off, len);
            } catch (IOException e) {
                readFailed = true;
                throw e;
            }
        }
    }

    private void writeBodyTo(FileWrapper tempFile, InputStream body) throws IOException {
        if (tempFile.getPath() != null) {
            logger.debug("Copying download stream");
            Files.copy(body, Paths.get(tempFile.getPath()),
                    StandardCopyOption.REPLACE_EXISTING);
            logger.debug("Copied downloaded stream");
            return;
        }
        // Writer-backed destination (in-process test path).
        byte[] buffer = new byte[8192];
        int n;
        while ((n = body.read(buffer)) != -1) {
            tempFile.getWriter().writeBytes(buffer, n);
        }
    }

    private static boolean md5Matches(MessageDigest md, HttpURLConnection connection) {
        String header = connection.getHeaderField("Content-MD5");
        String computed = DatatypeConverter.printHexBinary(md.digest());
        return header != null && header.equalsIgnoreCase(computed);
    }

    @Override
    public void close() {
        if (closeFutureFactory) {
            try {
                futureFactory.close();
            } catch (IOException e) {
                logger.error("Error closing future factory.", e);
            }
        }
    }

    private AutoUpdateStatus checkForUpdateFromUrl(
            AspectEngineDataFile dataFile,
            boolean automatic) {
        if (dataFile.getDataFilePath() == null || dataFile.getDataFilePath().isEmpty()) {
            // There is no data file path specified so perform the
            // update entirely in memory.
            try {
                FileWrapper uncompressedData = new FileWrapperMemory();

                AutoUpdateStatus memoryDownloadResult =
                        downloadFile(dataFile, uncompressedData, automatic);
                if (memoryDownloadResult != AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS) {
                    return memoryDownloadResult;
                }
                try (BinaryReader reader = uncompressedData.getReader()) {
                    if (dataFile.getEngine() != null) {
                        // Tell the engine to refresh itself with
                        // the new data.
                        logger.info("Attempting to refresh Engine '{}' with new data",
                                getEngineType(dataFile));
                        dataFile.getEngine().refreshData(dataFile.getIdentifier(),
                                reader.toByteArray());
                    } else {
                        // No associated engine at the moment so just set
                        // the value of the data array.
                        dataFile.getConfiguration().setData(reader.toByteArray());
                    }
                }
                // The engine, or the configuration, now holds the new data.
                return AUTO_UPDATE_SUCCESS;
            } catch (Exception ex) {
                logRefreshFailure(dataFile, ex, null);
                return AutoUpdateStatus.AUTO_UPDATE_REFRESH_FAILED;
            }
        }

        if (dataFile.getTempDataDirPath() == null ||
                dataFile.getTempDataDirPath().isEmpty()) {
            logger.error("The data file '" + dataFile.getIdentifier() +
                    "' is checking for updates but does not have a temporary " +
                    "file path configured.");
            return AutoUpdateStatus.AUTO_UPDATE_REFRESH_FAILED;
        }
        Path uncompressedTempFile = Paths.get(
                dataFile.getTempDataDirPath(),
                dataFile.getIdentifier() + "-" + randomUUID() + ".tmp");
        FileWrapper uncompressedData = fileWrapperFactory.build(uncompressedTempFile.toString());
        try {
            // Check if there is an update and download it if there is
            AutoUpdateStatus result =
                    downloadFile(dataFile, uncompressedData, automatic);

            if (result == AutoUpdateStatus.AUTO_UPDATE_IN_PROGRESS) {
                WatchKey watchKey = dataFile.getConfiguration().getWatchKey();
                // If this engine has a data file watcher then we need to
                // disable it while the update is occurring.
                if (dataFile.getConfiguration().getWatchKey() != null) {
                    watchKey.cancel();
                    dataFile.getConfiguration().setWatchKey(null);
                }

                try {
                    // Move the uncompressed file to the engine's data file location
                    logger.debug("Moving {} to {}",
                            uncompressedTempFile.toFile().getAbsolutePath(),
                            dataFile.getDataFilePath());
                    Files.copy(Paths.get(uncompressedTempFile.toFile().getAbsolutePath()),
                            Paths.get(dataFile.getDataFilePath()),
                            StandardCopyOption.REPLACE_EXISTING);
                    // check the file is readable
                    File f = Paths.get(dataFile.getDataFilePath()).toFile();
                    try (DataInputStream d =
                            new DataInputStream(new BufferedInputStream(new FileInputStream(f)))){
                        logger.debug("Reading file {}", d.read());
                    }
                } catch (Exception ex) {
                    logger.error("An error occurred when copying a data file to " +
                                    "replace the existing one at '{}'.",
                            dataFile.getDataFilePath(), ex);
                    result = AutoUpdateStatus.AUTO_UPDATE_NEW_FILE_CANT_RENAME;
                }
                // Make sure to enable the file watcher again if needed. The
                // watch resumes on the data file's own watch service, unless
                // the data file was unregistered during the download.
                if (watchKey != null) {
                    final Path filePath =
                            Paths.get(dataFile.getDataFilePath()).toAbsolutePath();
                    synchronized (configLock) {
                        if (configurations.contains(dataFile)) {
                            watchKey = filePath.getParent().register(
                                    watchService(dataFile, filePath),
                                    StandardWatchEventKinds.ENTRY_MODIFY);
                            dataFile.getConfiguration().setWatchKey(watchKey);
                        }
                    }
                }
            }
            return result;
        } catch (Throwable e) {
            logger.error("Exception thrown", e);
            return AutoUpdateStatus.AUTO_UPDATE_REFRESH_FAILED;
        } finally {
            // Make sure the temp files are cleaned up
            try {
                logger.debug("Deleting temp file {}", uncompressedTempFile);
                if (Files.exists(uncompressedTempFile)) {
                    Files.delete(uncompressedTempFile);
                }
            } catch (IOException e) {
                logger.warn("Could not delete temporary download file file");
            }
        }
    }
}
