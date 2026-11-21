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

package fiftyone.devicedetection.examples.console;

import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.configuration.PipelineOptionsFactory;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The console examples show both ways of making a pipeline, for both
 * on-premise and cloud: the fluent builder (GettingStartedCloud and the
 * other on-premise consoles, run by their own tests) and an options file,
 * checked here without a data file or a key, as the tests that run the
 * examples need one or the other
 * (device-detection-specification/required-examples.md, "Examples MUST be
 * provided that illustrate basic usage of the API for both Cloud and
 * On-Prem, using fluent builder and options file configuration, when
 * executed as a console (command line) application").
 */
public class ConsoleConfigurationCoverageTest {

    /**
     * The builders an options file names, reading the file this example
     * module holds in its resources, so that a file of the same name in
     * another example cannot be read in its place.
     */
    private static List<String> builders(String file) throws Exception {
        URL resource =
            ConsoleConfigurationCoverageTest.class.getResource("/" + file);
        assertNotNull("The example's resources hold no " + file, resource);
        PipelineOptions options = PipelineOptionsFactory.getOptionsFromFile(
            new File(resource.toURI()));
        List<String> names = new ArrayList<>();
        for (ElementOptions element : options.elements) {
            names.add(element.builderName);
        }
        return names;
    }

    @Test
    public void onPremiseOptionsFile() throws Exception {
        List<String> builders = builders("gettingStartedOnPrem.xml");
        assertTrue(builders.toString(),
            builders.contains("DeviceDetectionHashEngine"));
    }

    @Test
    public void cloudOptionsFile() throws Exception {
        List<String> builders = builders("tacCloud.xml");
        assertTrue(builders.toString(), builders.contains("CloudRequestEngine"));
    }
}
