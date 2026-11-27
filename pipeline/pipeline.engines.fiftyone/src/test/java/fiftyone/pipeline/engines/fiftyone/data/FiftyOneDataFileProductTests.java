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


package fiftyone.pipeline.engines.fiftyone.data;

import fiftyone.pipeline.engines.configuration.DataFileConfigurationDefault;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A 51Degrees data file names the product of the data update service that
 * it needs, whichever URL formatter its configuration has. The name is a
 * prefix for the download type followed by the tier of the data the engine
 * has loaded. Until an engine has loaded the data file, the product is read
 * from the header of the file at the configured location.
 */
public class FiftyOneDataFileProductTests {

    @Test
    public void Product_DeviceDetectionTac() {
        assertEquals("V4TAC", dataFile("HashV41", "TAC").getDataUpdateProduct());
    }

    @Test
    public void Product_IpIntelligenceEnterprise() {
        assertEquals("IPIV4Enterprise",
            dataFile("IPIV41", "Enterprise").getDataUpdateProduct());
    }

    @Test
    public void Product_TierThatStartsWithThePrefixIsTheProduct() {
        assertEquals("IPIV4Enterprise",
            dataFile("IPIV41", "IPIV4Enterprise").getDataUpdateProduct());
    }

    @Test
    public void Product_NotKnownForAnEmptyTier() {
        assertNull(dataFile("HashV41", "").getDataUpdateProduct());
    }

    @Test
    public void Product_NotKnownWithoutAnEngine() {
        assertNull(dataFile("HashV41", null).getDataUpdateProduct());
    }

    @Test
    public void Product_NotKnownForAnotherDownloadType() {
        assertNull(dataFile("Other", "Enterprise").getDataUpdateProduct());
    }

    @Test
    public void ProductFromFile_ReadFromTheHeaderBeforeAnEngineIsSet(
        @TempDir Path folder) throws IOException {
        Path path = file(folder);
        List<String> read = new ArrayList<>();
        FiftyOneDataFileDefault file = dataFile("HashV41", null);
        file.setConfiguration(configuration(path.toString()));
        file.setProductReader(p -> {
            read.add(p);
            return "V4TAC";
        });

        assertEquals("V4TAC", file.getDataUpdateProduct());
        assertEquals(1, read.size());
        assertEquals(path.toString(), read.get(0));
    }

    @Test
    public void ProductFromFile_IpIntelligence(@TempDir Path folder)
        throws IOException {
        FiftyOneDataFileDefault file = dataFile("IPIV41", null);
        file.setConfiguration(configuration(file(folder).toString()));
        file.setProductReader(p -> "IPIV4Enterprise");

        assertEquals("IPIV4Enterprise", file.getDataUpdateProduct());
    }

    @Test
    public void ProductFromFile_ATierAloneTakesThePrefix(
        @TempDir Path folder) throws IOException {
        FiftyOneDataFileDefault file = dataFile("HashV41", null);
        file.setConfiguration(configuration(file(folder).toString()));
        file.setProductReader(p -> "TAC");

        assertEquals("V4TAC", file.getDataUpdateProduct());
    }

    @Test
    public void ProductFromFile_LoadedEngineWins(@TempDir Path folder)
        throws IOException {
        List<String> read = new ArrayList<>();
        FiftyOneDataFileDefault file = dataFile("HashV41", "Enterprise");
        file.setConfiguration(configuration(file(folder).toString()));
        file.setProductReader(p -> {
            read.add(p);
            return "V4TAC";
        });

        assertEquals("V4Enterprise", file.getDataUpdateProduct());
        assertTrue(read.isEmpty(),
            "The header should not be read once an engine has loaded the file");
    }

    @Test
    public void ProductFromFile_NoFileGivesNoProduct(@TempDir Path folder) {
        List<String> read = new ArrayList<>();
        FiftyOneDataFileDefault file = dataFile("HashV41", null);
        file.setConfiguration(configuration(
            folder.resolve("missing.hash").toString()));
        file.setProductReader(p -> {
            read.add(p);
            return "V4TAC";
        });

        assertNull(file.getDataUpdateProduct());
        assertTrue(read.isEmpty(), "No file should mean no read");
    }

    @Test
    public void ProductFromFile_NoPathGivesNoProduct() {
        FiftyOneDataFileDefault file = dataFile("HashV41", null);
        file.setConfiguration(configuration(null));
        file.setProductReader(p -> "V4TAC");

        assertNull(file.getDataUpdateProduct());
    }

    @Test
    public void ProductFromFile_ReadThatFailsGivesNoProduct(
        @TempDir Path folder) throws IOException {
        FiftyOneDataFileDefault file = dataFile("HashV41", null);
        file.setConfiguration(configuration(file(folder).toString()));
        file.setProductReader(p -> {
            throw new RuntimeException("The header could not be read");
        });

        assertNull(file.getDataUpdateProduct());
    }

    @Test
    public void ProductFromFile_ReaderThatCannotLoadGivesNoProduct(
        @TempDir Path folder) throws IOException {
        FiftyOneDataFileDefault file = dataFile("HashV41", null);
        file.setConfiguration(configuration(file(folder).toString()));
        file.setProductReader(p -> {
            throw new UnsatisfiedLinkError("No native library");
        });

        assertNull(file.getDataUpdateProduct());
    }

    @Test
    public void ProductFromFile_EmptyProductGivesNoProduct(
        @TempDir Path folder) throws IOException {
        FiftyOneDataFileDefault file = dataFile("HashV41", null);
        file.setConfiguration(configuration(file(folder).toString()));
        file.setProductReader(p -> "");

        assertNull(file.getDataUpdateProduct());
    }

    @Test
    public void ProductFromFile_NoReaderGivesNoProduct(@TempDir Path folder)
        throws IOException {
        FiftyOneDataFileDefault file = dataFile("HashV41", null);
        file.setConfiguration(configuration(file(folder).toString()));
        file.setProductReader(null);

        assertNull(file.getDataUpdateProduct());
    }

    private static Path file(Path folder) throws IOException {
        Path path = folder.resolve("data.hash");
        Files.write(path, new byte[] {1, 2, 3});
        return path;
    }

    private static DataFileConfigurationDefault configuration(String path) {
        DataFileConfigurationDefault configuration =
            new DataFileConfigurationDefault();
        configuration.setDataFilePath(path);
        return configuration;
    }

    private static FiftyOneDataFileDefault dataFile(String type, String tier) {
        FiftyOneDataFileDefault file = new FiftyOneDataFileDefault();
        file.setDataUpdateDownloadType(type);
        if (tier != null) {
            OnPremiseAspectEngine<?, ?> engine =
                mock(OnPremiseAspectEngine.class);
            when(engine.getDataSourceTier()).thenReturn(tier);
            file.setEngine(engine);
        }
        return file;
    }
}
