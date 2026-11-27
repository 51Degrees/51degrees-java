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

import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.configuration.PipelineOptionsFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.fiftyone.flowelements.FiftyOnePipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageElement;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The on-premise getting started console example does not share usage
 * (device-detection-specification/required-examples.md, "Reference
 * implementations inhibit usage sharing for console examples by ensuring the
 * Usage Sharing Element is not added to the Pipeline, but enable it for web
 * examples", and "Every example in which usage sharing is inhibited needs to
 * have a comment saying that in normal operation usage sharing should be
 * enabled by adding a Usage Sharing Element to the Pipeline").
 *
 * The example builds its Pipeline from its options file with the
 * FiftyOnePipelineBuilder, which adds a Usage Sharing Element unless told
 * not to. The test builds the Pipeline from the same file in the same way,
 * without the engine, which needs a data file, and checks that no Usage
 * Sharing Element is added. Building shares nothing; only processing does.
 */
public class GettingStartedOnPremUsageSharingTest {

    private static final String OPTIONS =
        "examples/device-detection/console/src/main/resources/gettingStartedOnPrem.xml";

    private static final String SOURCE =
        "examples/device-detection/console/src/main/java/fiftyone/devicedetection/examples/console/GettingStartedOnPrem.java";

    private static String read(String path) throws Exception {
        File file = getFilePath(path);
        return new String(Files.readAllBytes(file.toPath()),
            StandardCharsets.UTF_8).replaceAll("\\s+", " ");
    }

    @Test
    public void pipelineHasNoUsageSharingElement() throws Exception {
        PipelineOptions options =
            PipelineOptionsFactory.getOptionsFromFile(getFilePath(OPTIONS));
        options.elements.clear();
        Pipeline pipeline = new FiftyOnePipelineBuilder()
            .buildFromConfiguration(options);
        try {
            for (FlowElement<?, ?> element : pipeline.getFlowElements()) {
                assertFalse("The example's Pipeline should not share usage",
                    element instanceof ShareUsageElement);
            }
        } finally {
            pipeline.close();
        }
    }

    @Test
    public void commentSaysToShareUsageInProduction() throws Exception {
        String expected = "in normal operation usage sharing should be " +
            "enabled by adding a Usage Sharing Element to the Pipeline";
        assertTrue("The options file should say " + expected,
            read(OPTIONS).contains(expected));
        assertTrue("The example should say " + expected,
            read(SOURCE).contains(expected));
    }
}
