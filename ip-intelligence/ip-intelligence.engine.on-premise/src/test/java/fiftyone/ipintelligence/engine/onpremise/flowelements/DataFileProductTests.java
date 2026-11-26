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

package fiftyone.ipintelligence.engine.onpremise.flowelements;

import fiftyone.ipintelligence.engine.onpremise.interop.swig.EngineIpiSwig;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.configuration.DataFileConfigurationDefault;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneDataFileDefault;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static fiftyone.ipintelligence.shared.testhelpers.FileUtils.LITE_IPI_DATA_FILE_NAME;
import static fiftyone.ipintelligence.shared.testhelpers.FileUtils.getHashFile;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * The product of an IP Intelligence data file, which the data update service asks for
 * when a license key holds several products, is read from the header of the
 * file until an engine has loaded it, and from the engine after that.
 */
public class DataFileProductTests {

    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    private static final String DOWNLOAD_TYPE = "IPIV41";

    /**
     * The header of the free Lite data file names the Lite product.
     */
    @Test
    public void ProductFromFile_LiteFile() {
        assertEquals("IPIV4Lite",
            EngineIpiSwig.getProductFromFile(lite().getPath()));
    }

    /**
     * The header of the data file the tests use, the Enterprise file where the
     * build fetched it, gives the product that an engine which has loaded
     * the file gives.
     */
    @Test
    public void ProductFromFile_IsTheProductOfTheLoadedEngine()
        throws Exception {
        File file = getHashFile();
        assumeTrue("No IP Intelligence data file was found", file != null);
        String fromHeader = EngineIpiSwig.getProductFromFile(file.getPath());
        // The Enterprise file is several gigabytes, so it is not loaded into
        // memory.
        try (IPIntelligenceOnPremiseEngine engine = build(file.getPath(),
            Constants.PerformanceProfiles.LowMemory)) {
            assertEquals(
                engine.getDataFileMetaData().getDataUpdateProduct(),
                fromHeader);
        }
        assertTrue(fromHeader, fromHeader.startsWith("IPIV4"));
    }

    /**
     * A file that is not there cannot be read.
     */
    @Test
    public void ProductFromFile_NoFile_Throws() throws Exception {
        Path folder = Files.createTempDirectory("51Degrees-Product-");
        try {
            EngineIpiSwig.getProductFromFile(
                folder.resolve("missing.ipi").toString());
            fail("Reading the product of a missing file should throw");
        } catch (RuntimeException e) {
            assertNotNull(e.getMessage());
        } finally {
            Files.deleteIfExists(folder);
        }
    }

    /**
     * Before an engine has loaded the data file, as at the update on
     * start-up, the data file reads its product from the header of the file
     * with the reader the builder gives it.
     */
    @Test
    public void DataFile_NoEngine_ReadsTheProductFromTheHeader() {
        assertEquals("IPIV4Lite",
            dataFileWithoutEngine(lite().getPath()).getDataUpdateProduct());
    }

    /**
     * Where there is no file at the configured location, no product is
     * known.
     */
    @Test
    public void DataFile_NoEngineAndNoFile_GivesNoProduct() throws Exception {
        Path folder = Files.createTempDirectory("51Degrees-Product-");
        try {
            assertNull(dataFileWithoutEngine(
                folder.resolve("missing.ipi").toString())
                .getDataUpdateProduct());
        } finally {
            Files.deleteIfExists(folder);
        }
    }

    /**
     * Once an engine has loaded the data file, the product comes from the
     * engine and not from the file, so a file that has changed since it was
     * loaded does not change the product.
     */
    @Test
    public void DataFile_LoadedEngine_GivesTheProduct() throws Exception {
        Path folder = Files.createTempDirectory("51Degrees-Product-");
        Path copy = folder.resolve("data.ipi");
        try {
            Files.copy(lite().toPath(), copy,
                StandardCopyOption.REPLACE_EXISTING);
            // The whole file is loaded into memory, so the engine has no need
            // of the file once it is built.
            try (IPIntelligenceOnPremiseEngine engine = build(copy.toString(),
                Constants.PerformanceProfiles.MaxPerformance)) {
                // A file with no header in place of the one loaded.
                Files.write(copy, new byte[] {0});
                assertNull("The header of the file should not be readable",
                    dataFileWithoutEngine(copy.toString())
                        .getDataUpdateProduct());
                assertEquals("IPIV4Lite",
                    engine.getDataFileMetaData().getDataUpdateProduct());
            }
        } finally {
            Files.deleteIfExists(copy);
            Files.deleteIfExists(folder);
        }
    }

    private static File lite() {
        try {
            return getFilePath(LITE_IPI_DATA_FILE_NAME);
        } catch (IllegalArgumentException e) {
            throw new AssertionError("The test needs the free Lite data " +
                "file, which the build fetches into cxx/data.", e);
        }
    }

    private static FiftyOneDataFileDefault dataFileWithoutEngine(String path) {
        DataFileConfigurationDefault configuration =
            new DataFileConfigurationDefault();
        configuration.setDataFilePath(path);
        FiftyOneDataFileDefault dataFile = new FiftyOneDataFileDefault();
        dataFile.setDataUpdateDownloadType(DOWNLOAD_TYPE);
        dataFile.setConfiguration(configuration);
        dataFile.setProductReader(
            new IPIntelligenceOnPremiseEngineBuilder(loggerFactory)
                .getProductReader());
        return dataFile;
    }

    private static IPIntelligenceOnPremiseEngine build(String path,
        Constants.PerformanceProfiles profile) throws Exception {
        return new IPIntelligenceOnPremiseEngineBuilder(loggerFactory)
            .setPerformanceProfile(profile)
            .setAutoUpdate(false)
            .setDataFileSystemWatcher(false)
            .setDataUpdateOnStartup(false)
            .build(path, false);
    }
}
