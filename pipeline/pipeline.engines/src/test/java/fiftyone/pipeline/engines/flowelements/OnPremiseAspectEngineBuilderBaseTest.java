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

import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.configuration.DataFileConfigurationDefault;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.services.DataUpdateService;
import fiftyone.pipeline.engines.services.DataUpdateServiceDefault;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.engines.services.update.FutureFactory;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("rawtypes")
public class OnPremiseAspectEngineBuilderBaseTest {

    private OnPremiseAspectEngineBuilderBase aspectEngineBuilder;

    @Before
    public void Init() {
        aspectEngineBuilder = createEngineBuilderBase();
        deleteTempDir();
    }

    private OnPremiseAspectEngineBuilderBase createEngineBuilderBase() {
        return new OnPremiseAspectEngineBuilderBase() {
            @Override
            public OnPremiseAspectEngineBuilderBase setPerformanceProfile(Constants.PerformanceProfiles profile) {
                return null;
            }

            @Override
            protected AspectEngine newEngine(List properties) {
                return null;
            }
        };
    }

    @After
    public void Cleanup() {
        deleteTempDir();
    }

    private void deleteTempDir() {
        Path path = Paths.get(aspectEngineBuilder.tempDir);
        File tempDir = path.toFile();
        if (tempDir.exists()) {
            tempDir.delete();
        }
    }

    @Test
    public void CreateTemporaryFolder_VerifyWritePermissionsUnix() throws IOException {
        unixOnly();
        createTempDir();
        Set<PosixFilePermission> permissions = getTempDirectoryPermissions();
        assertFalse(permissions.contains(PosixFilePermission.OTHERS_WRITE));
        assertFalse(permissions.contains(PosixFilePermission.GROUP_WRITE));
        assertTrue(permissions.contains(PosixFilePermission.OWNER_WRITE));
    }

    private void unixOnly() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
    }

    private File createTempDir() {
        Path path = Paths.get(aspectEngineBuilder.tempDir);
        aspectEngineBuilder.createAndVerifyTempDir(path);
        return path.toFile();
    }

    private Set<PosixFilePermission> getTempDirectoryPermissions() throws IOException {
        Path path = Paths.get(aspectEngineBuilder.tempDir);
        return Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
    }

    @Test
    public void CreateTemporaryFolder_VerifyReadPermissionsUnix() throws IOException {
        unixOnly();
        createTempDir();
        Set<PosixFilePermission> permissions = getTempDirectoryPermissions();
        assertFalse(permissions.contains(PosixFilePermission.OTHERS_READ));
        assertTrue(permissions.contains(PosixFilePermission.GROUP_READ));
        assertTrue(permissions.contains(PosixFilePermission.OWNER_READ));
    }

    @Test
    public void CreateTemporaryFolder_VerifyExecutePermissionsUnix() throws IOException {
        unixOnly();
        createTempDir();
        Set<PosixFilePermission> permissions = getTempDirectoryPermissions();
        assertFalse(permissions.contains(PosixFilePermission.OTHERS_EXECUTE));
        assertTrue(permissions.contains(PosixFilePermission.GROUP_EXECUTE));
        assertTrue(permissions.contains(PosixFilePermission.OWNER_EXECUTE));
    }

    @Test
    public void CreateTemporaryFolder_VerifyPermissions() {
        File tempDir = createTempDir();
        assertTrue(tempDir.canRead());
        assertTrue(tempDir.canWrite());
        assertTrue(tempDir.canExecute());
    }

    /**
     * Every data file's license key is checked before any data file is
     * registered, so a build refused for a later data file leaves nothing
     * registered with the data update service.
     */
    @Test
    public void LaterDataFileHasNoLicenseKey_NothingRegistered() {
        DataUpdateService updateService = mock(DataUpdateService.class);
        OnPremiseAspectEngineBuilderBase builder =
            new OnPremiseAspectEngineBuilderBase(null, updateService) {
                @Override
                public OnPremiseAspectEngineBuilderBase setPerformanceProfile(
                    Constants.PerformanceProfiles profile) {
                    return this;
                }

                @Override
                protected AspectEngine newEngine(List properties) {
                    return null;
                }

                @Override
                protected boolean isLicenseKeyRequired(
                    DataFileConfiguration dataFileConfig) {
                    return true;
                }
            };
        builder.addDataFile(autoUpdateConfiguration("First", "SOMELICENSEKEY"));
        builder.addDataFile(autoUpdateConfiguration("Second", null));

        assertThrows(PipelineConfigurationException.class, builder::buildEngine);

        verify(updateService, never()).registerDataFile(any());
    }

    /**
     * The file system watcher runs on its own setting and needs no license
     * key. An engine whose update source needs a key it does not have is
     * built with automatic updates and the update on start-up off, and its
     * data file is still registered to be watched. With the watcher off as
     * well, nothing is registered.
     */
    @Test
    public void WatcherAlone_RegistersTheDataFile() throws Exception {
        String[] names = {
            "watcher on, automatic updates off, no license key",
            "watcher off, automatic updates off, no license key"};
        boolean[] watching = {true, false};
        for (int i = 0; i < names.length; i++) {
            checkWatcherAlone(names[i], watching[i]);
        }
    }

    private static void checkWatcherAlone(String name, boolean watching)
        throws Exception {
        FutureFactory futureFactory = mock(FutureFactory.class);
        when(futureFactory.scheduleRepeating(any(Runnable.class), anyLong()))
            .then(invocation -> mock(ScheduledFuture.class));
        DataUpdateService updateService = new DataUpdateServiceDefault(
            null, mock(HttpClient.class), null, futureFactory);
        OnPremiseAspectEngineBuilderBase builder =
            keyRequiredBuilder(updateService);

        Path directory = Files.createTempDirectory("watched");
        Path dataFile = directory.resolve("data.dat");
        Files.write(dataFile, "Data".getBytes());
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setIdentifier("Watched");
        config.setDataFilePath(dataFile.toString());
        config.setAutomaticUpdatesEnabled(false);
        config.setUpdateOnStartup(false);
        config.setFileSystemWatcherEnabled(watching);
        builder.addDataFile(config);

        try {
            builder.buildEngine();
            AspectEngineDataFile file =
                (AspectEngineDataFile) builder.dataFiles.get(0);
            assertEquals(name + ", the data file is registered",
                watching, file.getIsRegistered());
            assertEquals(name + ", a file system watcher was created",
                watching, config.getWatchKey() != null);
        }
        finally {
            for (Object file : builder.dataFiles) {
                updateService.unregisterDataFile((AspectEngineDataFile) file);
            }
            if (config.getWatchKey() != null) {
                config.getWatchKey().cancel();
            }
            directory.toFile().deleteOnExit();
            if (dataFile.toFile().delete() == false) {
                dataFile.toFile().deleteOnExit();
            }
            directory.toFile().delete();
        }
    }

    /**
     * A builder whose update source serves nothing without a license key.
     */
    private static OnPremiseAspectEngineBuilderBase keyRequiredBuilder(
        DataUpdateService updateService) {
        return new OnPremiseAspectEngineBuilderBase(null, updateService) {
            @Override
            public OnPremiseAspectEngineBuilderBase setPerformanceProfile(
                Constants.PerformanceProfiles profile) {
                return this;
            }

            @Override
            protected AspectEngine newEngine(List properties) {
                return null;
            }

            @Override
            protected boolean isLicenseKeyRequired(
                DataFileConfiguration dataFileConfig) {
                return true;
            }
        };
    }

    private static DataFileConfiguration autoUpdateConfiguration(
        String identifier,
        String licenseKey) {
        DataFileConfigurationDefault config = new DataFileConfigurationDefault();
        config.setIdentifier(identifier);
        config.setAutomaticUpdatesEnabled(true);
        if (licenseKey != null) {
            config.setDataUpdateLicenseKeys(
                Collections.singletonList(licenseKey));
        }
        return config;
    }
}