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

package fiftyone.ipintelligence.examples.shared;

import fiftyone.pipeline.engines.Constants.PerformanceProfiles;
import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class DataFileHelperTest {

    private static final String IPI_ENGINE = "IPIntelligenceOnPremiseEngine";

    @Test
    public void findPerformanceProfile_IgnoresCase() {
        StringWriter written = new StringWriter();
        assertEquals(PerformanceProfiles.MaxPerformance,
                DataFileHelper.findPerformanceProfile("maxperformance",
                        new PrintWriter(written)));
        assertEquals(PerformanceProfiles.LowMemory,
                DataFileHelper.findPerformanceProfile("LOWMEMORY",
                        new PrintWriter(written)));
        assertEquals("", written.toString());
    }

    @Test
    public void findPerformanceProfile_Unknown_WritesNamesAccepted() {
        StringWriter written = new StringWriter();
        assertNull(DataFileHelper.findPerformanceProfile("Fastest",
                new PrintWriter(written)));
        for (PerformanceProfiles profile : PerformanceProfiles.values()) {
            assertTrue(written.toString(),
                    written.toString().contains(profile.name()));
        }
    }

    private static final String PATH_NAME = "_51DEGREES_IPI_PATH";

    /**
     * System properties stand in for the environment variables, because
     * the lookup reads either and a test cannot set an environment
     * variable.
     */
    @Test
    public void getDataFileLocation_ReadsTheVariable() throws Exception {
        assumeTrue(System.getenv(PATH_NAME) == null);
        String saved = System.clearProperty(PATH_NAME);
        File file = File.createTempFile("path-name", ".ipi");
        try {
            System.setProperty(PATH_NAME, file.getPath());
            assertEquals(file.getPath(),
                    DataFileHelper.getDataFileLocation(null));
        } finally {
            restore(PATH_NAME, saved);
            file.delete();
        }
    }

    private static void restore(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    /**
     * The engine with the builder name is given the data file.
     */
    @Test
    public void useDataFile_GivesTheEngineTheFile() {
        PipelineOptions options = optionsWith(IPI_ENGINE);
        DataFileHelper.useDataFile(options, IPI_ENGINE, "given.dat");
        assertEquals("given.dat", options.find(IPI_ENGINE, "DataFile"));
    }

    /**
     * With no engine of that builder name the data file would be dropped
     * silently, so the helper throws, naming the builder.
     */
    @Test
    public void useDataFile_ThrowsWhenNoEngineMatches() {
        PipelineOptions options = optionsWith("OtherEngine");
        try {
            DataFileHelper.useDataFile(options, IPI_ENGINE, "given.dat");
            fail("Expected an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(IPI_ENGINE));
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
