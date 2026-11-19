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

package fiftyone.pipeline.translation.flowelements;

import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.configuration.PipelineOptionsFactory;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.exceptions.AggregateException;
import fiftyone.pipeline.translation.data.MissingTranslationBehavior;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that a configuration file builds a Translation Engine from a builder
 * that code gave its sources and translations, with the file setting the
 * options that take a single value, under the names the specification
 * gives them.
 */
public class TranslationEngineConfigurationTests {

    @TempDir
    Path folder;

    /**
     * A builder prepared in code with the one source and the one translation
     * a configuration file cannot give it.
     */
    private static TranslationEngineBuilder prepared() {
        return new TranslationEngineBuilder(LoggerFactory.getILoggerFactory())
            .addSource("countries.fr_FR.yml", "England: Angleterre")
            .addTranslation("Country", "CountryTranslated");
    }

    /**
     * Options read from an XML configuration file that puts a country
     * source in front of a Translation Engine and sets each of the
     * engine's single-value options.
     */
    private PipelineOptions options(String country, String behavior)
            throws Exception {
        String xml = "<PipelineOptions><Elements>"
            + "<Element>"
            + "<BuilderName>CountrySourceElement</BuilderName>"
            + "<BuildParameters><Country>" + country + "</Country>"
            + "</BuildParameters>"
            + "</Element>"
            + "<Element>"
            + "<BuilderName>TranslationEngine</BuilderName>"
            + "<BuildParameters>"
            + "<SourceElementDataKey>source</SourceElementDataKey>"
            + "<FixedLanguage>fr_FR</FixedLanguage>"
            + "<MissingTranslationBehavior>" + behavior
            + "</MissingTranslationBehavior>"
            + "</BuildParameters>"
            + "</Element>"
            + "</Elements></PipelineOptions>";
        Path file = folder.resolve("pipeline.xml");
        Files.write(file, xml.getBytes(StandardCharsets.UTF_8));
        return PipelineOptionsFactory.getOptionsFromFile(file.toFile());
    }

    /** Processes one request and gives the translated country. */
    private static Object translate(Pipeline pipeline) throws Exception {
        try (FlowData data = pipeline.createFlowData()) {
            data.process();
            return data.get("translation").get("CountryTranslated");
        }
    }

    @Test
    public void preparedBuilder_ConfigurationSetsTheOptionsAndTranslates()
            throws Exception {
        // No language is in the evidence, so the French translation shows
        // the fixed language from the file was used, and the translation
        // shows the source element data key from the file was used.
        try (Pipeline pipeline = new PipelineBuilder()
                .addElementBuilder(prepared())
                .buildFromConfiguration(options("England", "Original"))) {
            assertEquals("Angleterre", translate(pipeline));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Original", "original", "ORIGINAL", " Original "})
    public void originalSpellings_KeepAValueWithNoTranslation(String spelling)
            throws Exception {
        // The prepared builder says EmptyString, so the original value
        // shows the file's setting replaced it.
        TranslationEngineBuilder builder = prepared()
            .setMissingTranslationBehavior(
                MissingTranslationBehavior.EMPTY_STRING);
        try (Pipeline pipeline = new PipelineBuilder()
                .addElementBuilder(builder)
                .buildFromConfiguration(options("Wales", spelling))) {
            assertEquals("Wales", translate(pipeline));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "EmptyString", "emptystring", "EMPTY_STRING", "empty_string"})
    public void emptyStringSpellings_GiveAnEmptyString(String spelling)
            throws Exception {
        try (Pipeline pipeline = new PipelineBuilder()
                .addElementBuilder(prepared())
                .buildFromConfiguration(options("Wales", spelling))) {
            assertEquals("", translate(pipeline));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "FlowError", "flowerror", "FLOW_ERROR", "flow_error"})
    public void flowErrorSpellings_AddAFlowError(String spelling)
            throws Exception {
        try (Pipeline pipeline = new PipelineBuilder()
                .addElementBuilder(prepared())
                .buildFromConfiguration(options("Wales", spelling))) {
            AggregateException thrown = assertThrows(
                AggregateException.class, () -> translate(pipeline));
            // The errors processing added are the suppressed exceptions.
            StringBuilder errors = new StringBuilder();
            for (Throwable error : thrown.getSuppressed()) {
                errors.append(error.getMessage()).append(' ');
            }
            assertTrue(errors.toString().contains(
                "There was no translation found for the value 'Wales'"),
                errors.toString());
        }
    }

    @Test
    public void unknownBehavior_IsRefusedNamingTheAcceptedValues()
            throws Exception {
        PipelineOptions options = options("England", "Sometimes");
        PipelineConfigurationException thrown = assertThrows(
            PipelineConfigurationException.class,
            () -> new PipelineBuilder()
                .addElementBuilder(prepared())
                .buildFromConfiguration(options));
        assertTrue(thrown.getMessage().contains(
            "'Sometimes' is not a missing translation behavior. " +
            "Use Original, EmptyString or FlowError."),
            thrown.getMessage());
    }

    @Test
    public void noPreparedBuilder_SaysSourcesAndTranslationsComeFromCode()
            throws Exception {
        PipelineOptions options = options("England", "Original");
        PipelineConfigurationException thrown = assertThrows(
            PipelineConfigurationException.class,
            () -> new PipelineBuilder().buildFromConfiguration(options));
        assertTrue(thrown.getMessage().contains(
            "which are added in code with addSource and addTranslation"),
            thrown.getMessage());
    }
}
