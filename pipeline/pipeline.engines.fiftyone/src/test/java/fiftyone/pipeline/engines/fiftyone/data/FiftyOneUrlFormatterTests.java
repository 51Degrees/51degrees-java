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

import fiftyone.pipeline.engines.configuration.DataFileConfiguration;
import fiftyone.pipeline.engines.configuration.DataFileConfigurationDefault;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFileDefault;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The 51Degrees URL formatter says which product of the data update service
 * a data file needs, as a prefix for the download type of the data file
 * followed by the tier of the data the engine has loaded. A product named
 * when the formatter is
 * created is asked for in the URL and is the product needed.
 */
public class FiftyOneUrlFormatterTests {

    private static final String KEY = "TESTKEYNOTREALABCDEFGHIJ1234";
    private static final String URL =
        "https://distributor.example/api/v2/download";

    @Test
    public void Url_NamesNoProduct() {
        assertEquals(
            URL + "?LicenseKeys=" + KEY + "&Download=True&Type=HashV41",
            new FiftyOneUrlFormatter().getFormattedDataUpdateUrl(
                dataFile("HashV41", "TAC")));
    }

    @Test
    public void Url_NamesTheProductGiven() {
        assertEquals(
            URL + "?LicenseKeys=" + KEY +
                "&Download=True&Type=HashV41&Product=V4TAC",
            new FiftyOneUrlFormatter("V4TAC").getFormattedDataUpdateUrl(
                dataFile("HashV41", null)));
    }

    @Test
    public void Product_IsTheProductGiven() {
        assertEquals("V4TAC",
            new FiftyOneUrlFormatter("V4TAC").getDataUpdateProduct(
                dataFile("HashV41", "Enterprise")));
    }

    @Test
    public void Product_DeviceDetectionTac() {
        assertEquals("V4TAC",
            new FiftyOneUrlFormatter().getDataUpdateProduct(
                dataFile("HashV41", "TAC")));
    }

    @Test
    public void Product_DeviceDetectionEnterprise() {
        assertEquals("V4Enterprise",
            new FiftyOneUrlFormatter().getDataUpdateProduct(
                dataFile("HashV41", "Enterprise")));
    }

    @Test
    public void Product_IpIntelligenceEnterprise() {
        assertEquals("IPIV4Enterprise",
            new FiftyOneUrlFormatter().getDataUpdateProduct(
                dataFile("IPIV41", "Enterprise")));
    }

    @Test
    public void Product_DownloadTypeIsMatchedIgnoringCase() {
        assertEquals("V4TAC",
            new FiftyOneUrlFormatter().getDataUpdateProduct(
                dataFile("hashv41", "TAC")));
    }

    @Test
    public void Product_TierThatStartsWithThePrefixIsTheProduct() {
        assertEquals("IPIV4Enterprise",
            new FiftyOneUrlFormatter().getDataUpdateProduct(
                dataFile("IPIV41", "IPIV4Enterprise")));
    }

    @Test
    public void Product_AnotherTierIsNamedTheSameWay() {
        assertEquals("V4Lite",
            new FiftyOneUrlFormatter().getDataUpdateProduct(
                dataFile("HashV41", "Lite")));
    }

    @Test
    public void Product_NotKnownForAnotherDownloadType() {
        assertNull(new FiftyOneUrlFormatter().getDataUpdateProduct(
            dataFile("Other", "Enterprise")));
    }

    @Test
    public void Product_NotKnownForAnEmptyTier() {
        assertNull(new FiftyOneUrlFormatter().getDataUpdateProduct(
            dataFile("HashV41", "")));
        assertNull(new FiftyOneUrlFormatter().getDataUpdateProduct(
            dataFile("IPIV41", "")));
    }

    @Test
    public void Product_GivenIsUsedWithoutLookingUpTheTier() {
        FiftyOneDataFile file = dataFile("HashV41", "Enterprise");
        assertEquals("V4TAC",
            new FiftyOneUrlFormatter("V4TAC").getDataUpdateProduct(file));
        org.mockito.Mockito.verify(file.getEngine(),
            org.mockito.Mockito.never()).getDataSourceTier();
    }

    @Test
    public void Product_GivenAsEmptyNamesNoProduct() {
        assertEquals(
            URL + "?LicenseKeys=" + KEY + "&Download=True&Type=HashV41",
            new FiftyOneUrlFormatter("").getFormattedDataUpdateUrl(
                dataFile("HashV41", "TAC")));
    }

    @Test
    public void Product_GivenIsSentAsGiven() {
        assertEquals(
            URL + "?LicenseKeys=" + KEY +
                "&Download=True&Type=HashV41&Product=v4tac",
            new FiftyOneUrlFormatter("v4tac").getFormattedDataUpdateUrl(
                dataFile("HashV41", "TAC")));
        assertEquals("v4tac",
            new FiftyOneUrlFormatter("v4tac").getDataUpdateProduct(
                dataFile("HashV41", "TAC")));
    }

    @Test
    public void Product_GivenAsEmptyIsFoundFromTheTier() {
        assertEquals("V4TAC",
            new FiftyOneUrlFormatter("").getDataUpdateProduct(
                dataFile("HashV41", "TAC")));
    }

    @Test
    public void Product_NotKnownWithoutAnEngine() {
        assertNull(new FiftyOneUrlFormatter().getDataUpdateProduct(
            dataFile("HashV41", null)));
    }

    @Test
    public void Product_NotKnownForAnotherKindOfDataFile() {
        AspectEngineDataFileDefault file = new AspectEngineDataFileDefault();
        file.setConfiguration(configuration());
        assertNull(new FiftyOneUrlFormatter().getDataUpdateProduct(file));
    }

    /**
     * A 51Degrees data file of the given download type. Its engine has
     * loaded data of the given tier, or it has no engine when the tier is
     * null.
     */
    private static FiftyOneDataFile dataFile(String type, String tier) {
        FiftyOneDataFileDefault file = new FiftyOneDataFileDefault();
        file.setConfiguration(configuration());
        file.setDataUpdateDownloadType(type);
        if (tier != null) {
            OnPremiseAspectEngine<?, ?> engine =
                mock(OnPremiseAspectEngine.class);
            when(engine.getDataSourceTier()).thenReturn(tier);
            file.setEngine(engine);
        }
        return file;
    }

    private static DataFileConfiguration configuration() {
        DataFileConfiguration config = new DataFileConfigurationDefault();
        config.setDataUpdateUrl(URL);
        config.setDataUpdateLicenseKeys(Collections.singletonList(KEY));
        return config;
    }
}
