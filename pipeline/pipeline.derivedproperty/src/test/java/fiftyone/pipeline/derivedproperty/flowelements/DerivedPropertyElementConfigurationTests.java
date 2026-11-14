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

package fiftyone.pipeline.derivedproperty.flowelements;

import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.configuration.PipelineOptionsFactory;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.derivedproperty.data.IDerivedPropertyData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A configuration file names the builder as DerivedPropertyElement and
 * passes built-in script names as Scripts and file paths as ScriptFiles,
 * each a comma separated string.
 * <p>
 * See the
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/pipeline-elements/derived-property-element.md#configuration">configuration
 * section</a> of the specification.
 */
public class DerivedPropertyElementConfigurationTests {

    @TempDir
    Path folder;

    /**
     * Writes the Band script into the folder and gives a pattern matching
     * it.
     */
    private String bandFiles() throws Exception {
        Files.write(folder.resolve("Band.yaml"),
            DerivedPropertyElementTests.BAND_YAML.getBytes(
                StandardCharsets.UTF_8));
        return folder.resolve("*.yaml").toString();
    }

    /**
     * Options read from an XML configuration file that puts two source
     * elements in front of a Derived Property Element with the build
     * parameters given.
     */
    private PipelineOptions options(String buildParameters)
        throws Exception {
        String xml = "<PipelineOptions><Elements>" +
            "<Element><BuilderName>StubSourceElement</BuilderName>" +
            "<BuildParameters><Key>device</Key>" +
            "<Values>BrowserReleaseAge=1,IsCrawler=False</Values>" +
            "</BuildParameters></Element>" +
            "<Element><BuilderName>StubSourceElement</BuilderName>" +
            "<BuildParameters><Key>ip</Key>" +
            "<Values>CountryCode=FR</Values>" +
            "</BuildParameters></Element>" +
            "<Element><BuilderName>DerivedPropertyElement</BuilderName>" +
            "<BuildParameters>" + buildParameters + "</BuildParameters>" +
            "</Element>" +
            "</Elements></PipelineOptions>";
        Path file = folder.resolve("pipeline.xml");
        Files.write(file, xml.getBytes(StandardCharsets.UTF_8));
        return PipelineOptionsFactory.getOptionsFromFile(file.toFile());
    }

    @Test
    public void scriptsAndScriptFilesFromAConfigurationFile()
        throws Exception {
        PipelineOptions options = options(
            "<Scripts> IsGdpr </Scripts>" +
            "<ScriptFiles>" + bandFiles() + "</ScriptFiles>");
        try (Pipeline pipeline =
                 new PipelineBuilder().buildFromConfiguration(options);
             FlowData data = pipeline.createFlowData()) {
            data.process();
            IDerivedPropertyData derived =
                data.get(IDerivedPropertyData.class);
            assertEquals(Boolean.TRUE,
                derived.getAs("IsGdpr", Boolean.class).getValue());
            assertEquals("New",
                derived.getAs("Band", String.class).getValue());
            assertEquals(2, pipeline.getElement(DerivedPropertyElement.class)
                .getScripts().size());
        }
    }

    @Test
    public void listsAreCommaSeparated() throws Exception {
        DerivedPropertyElement element = new DerivedPropertyElementBuilder(
            LoggerFactory.getILoggerFactory())
            .setScripts("HumanConfidence, IsGdpr,")
            .setScriptFiles(" ," + bandFiles())
            .build();
        assertEquals(3, element.getScripts().size());
    }

    @Test
    public void anUnknownBuildParameterIsRefused() throws Exception {
        PipelineOptions options = options("<Script>IsGdpr</Script>");
        assertThrows(PipelineConfigurationException.class,
            () -> new PipelineBuilder().buildFromConfiguration(options));
    }

    @Test
    public void aPatternMatchingNoFileIsRefused() throws Exception {
        PipelineOptions options = options("<ScriptFiles>" +
            folder.resolve("*.json") + "</ScriptFiles>");
        PipelineConfigurationException e = assertThrows(
            PipelineConfigurationException.class,
            () -> new PipelineBuilder().buildFromConfiguration(options));
        assertTrue(e.getMessage().contains("no file matches"),
            e.getMessage());
    }

    @Test
    public void anUnknownBuiltInScriptIsRefused() throws Exception {
        PipelineOptions options = options("<Scripts>NoSuchScript</Scripts>");
        PipelineConfigurationException e = assertThrows(
            PipelineConfigurationException.class,
            () -> new PipelineBuilder().buildFromConfiguration(options));
        assertTrue(e.getMessage().contains(
            "'NoSuchScript' is not a built-in script"), e.getMessage());
    }
}
