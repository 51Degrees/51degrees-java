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

package fiftyone.ipintelligence.translation;

import fiftyone.ipintelligence.translation.flowelements.CountriesTranslationEngine;
import fiftyone.ipintelligence.translation.flowelements.CountryCodeTranslationEngine;
import fiftyone.pipeline.core.configuration.PipelineOptionsFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.translation.flowelements.TranslationEngine;
import fiftyone.pipeline.translation.flowelements.TranslationEngineBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that a configuration file still builds the country translation
 * engines with their own builders when the general Translation Engine
 * builder can be named too, and a prepared one is given.
 */
public class CountryBuildersConfigurationTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    @SuppressWarnings("rawtypes")
    public void countryBuilders_AreUnaffectedByAPreparedGeneralBuilder()
            throws Exception {
        String xml = "<PipelineOptions><Elements>"
            + "<Element>"
            + "<BuilderName>CountryCodeTranslationEngine</BuilderName>"
            + "</Element>"
            + "<Element>"
            + "<BuilderName>CountriesTranslationEngine</BuilderName>"
            + "</Element>"
            + "<Element>"
            + "<BuilderName>TranslationEngine</BuilderName>"
            + "<BuildParameters>"
            + "<SourceElementDataKey>ip</SourceElementDataKey>"
            + "</BuildParameters>"
            + "</Element>"
            + "</Elements></PipelineOptions>";
        File file = folder.newFile("pipeline.xml");
        Files.write(file.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        TranslationEngineBuilder general =
            new TranslationEngineBuilder(LoggerFactory.getILoggerFactory())
                .addSource("countries.fr_FR.yml", "England: Angleterre")
                .addTranslation("Country", "CountryTranslated");

        try (Pipeline pipeline = new PipelineBuilder()
                .addElementBuilder(general)
                .buildFromConfiguration(
                    PipelineOptionsFactory.getOptionsFromFile(file))) {
            List<FlowElement> elements = pipeline.getFlowElements();
            assertEquals(3, elements.size());
            assertTrue(elements.get(0).getClass().getName(),
                elements.get(0) instanceof CountryCodeTranslationEngine);
            assertTrue(elements.get(1).getClass().getName(),
                elements.get(1) instanceof CountriesTranslationEngine);
            assertTrue(elements.get(2).getClass().getName(),
                elements.get(2) instanceof TranslationEngine);
        }
    }
}
