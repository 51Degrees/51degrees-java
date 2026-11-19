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

package fiftyone.pipeline.core.configuration;

import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;

/**
 * A configuration file written with the structure and names the .NET sample
 * configuration file uses (PipelineOptions, Elements, Element, BuilderName,
 * BuildParameters, a builder name ending 'Builder' and builder method names
 * starting 'Set', at element and Pipeline level) is read and builds a
 * Pipeline.
 * <p>
 * Specification: pipeline-specification/features/pipeline-configuration.md
 * line 40 at 51Degrees/specifications 706de1f0, "The configuration file
 * definition MUST be as similar as possible to configuration files for other
 * languages."
 */
public class ConfigurationFileLanguagesTest {

    @Test
    public void dotNetNames_AreRead() throws Exception {
        PipelineOptions options = PipelineOptionsFactory.getOptionsFromFile(
            getFilePath("options-dotnet-names.xml"));

        assertEquals(1, options.elements.size());
        assertEquals(
            "MultiplyByElementBuilder",
            options.elements.get(0).builderName);
        assertEquals("8", options.elements.get(0).buildParameters.get("multiple"));
        assertEquals(
            "true",
            options.pipelineBuilderParameters.get("SetAutoDisposeElements"));
    }

    @Test
    public void dotNetNames_BuildAPipeline() throws Exception {
        PipelineOptions options = PipelineOptionsFactory.getOptionsFromFile(
            getFilePath("options-dotnet-names.xml"));

        try (Pipeline pipeline = new PipelineBuilder(
            LoggerFactory.getILoggerFactory())
            .buildFromConfiguration(options)) {
            assertEquals(1, pipeline.getFlowElements().size());
        }
    }
}
