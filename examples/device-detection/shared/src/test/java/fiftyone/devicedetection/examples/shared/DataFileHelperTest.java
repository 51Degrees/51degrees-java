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

package fiftyone.devicedetection.examples.shared;

import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests of {@link DataFileHelper#useDataFile}.
 */
public class DataFileHelperTest {

    private static final String ENGINE = "DeviceDetectionHashEngine";

    /**
     * The engine with the builder name is given the data file.
     */
    @Test
    public void useDataFile_GivesTheEngineTheFile() {
        PipelineOptions options = optionsWith(ENGINE);
        DataFileHelper.useDataFile(options, ENGINE, "given.dat");
        assertEquals("given.dat", options.find(ENGINE, "DataFile"));
    }

    /**
     * With no engine of that builder name the data file would be dropped
     * silently, so the helper throws, naming the builder.
     */
    @Test
    public void useDataFile_ThrowsWhenNoEngineMatches() {
        PipelineOptions options = optionsWith("OtherEngine");
        try {
            DataFileHelper.useDataFile(options, ENGINE, "given.dat");
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(ENGINE));
        }
        assertEquals("options.dat", options.find("OtherEngine", "DataFile"));
    }

    private static PipelineOptions optionsWith(String builderName) {
        ElementOptions element = new ElementOptions();
        element.builderName = builderName;
        element.buildParameters.put("DataFile", "options.dat");
        PipelineOptions options = new PipelineOptions();
        options.elements.add(element);
        return options;
    }
}
