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

import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.OnPremiseAspectEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A 51Degrees data file names the product of the data update service that
 * it needs, whichever URL formatter its configuration has. The name is a
 * prefix for the download type followed by the tier of the data the engine
 * has loaded.
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

    @SuppressWarnings("unchecked")
    private static AspectEngineDataFile dataFile(String type, String tier) {
        FiftyOneDataFileDefault file = new FiftyOneDataFileDefault();
        file.setDataUpdateDownloadType(type);
        if (tier != null) {
            OnPremiseAspectEngine<? extends AspectData,
                ? extends AspectPropertyMetaData> engine =
                mock(OnPremiseAspectEngine.class);
            when(engine.getDataSourceTier()).thenReturn(tier);
            file.setEngine(engine);
        }
        return file;
    }
}
