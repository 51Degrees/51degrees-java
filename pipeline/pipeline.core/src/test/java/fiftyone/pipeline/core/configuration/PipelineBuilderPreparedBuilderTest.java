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

import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.core.testclasses.flowelements.ListSplitterElement;
import fiftyone.pipeline.core.testclasses.flowelements.ListSplitterElementBuilder;
import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByElementBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for element builders prepared in code and given to the pipeline
 * builder, which building from configuration uses in place of new ones.
 */
public class PipelineBuilderPreparedBuilderTest {

    /**
     * Options naming the list splitter and setting only its maximum length,
     * so the delimiter is whatever the builder already holds.
     */
    private static PipelineOptions listSplitterOptions(String maxLength) {
        ElementOptions element = new ElementOptions();
        element.builderName = "ListSplitterElement";
        element.buildParameters.put("MaxLength", maxLength);
        PipelineOptions options = new PipelineOptions();
        options.elements.add(element);
        return options;
    }

    private static ListSplitterElementBuilder splitter(String delimiter) {
        ListSplitterElementBuilder builder = new ListSplitterElementBuilder();
        builder.setDelimiter(delimiter);
        return builder;
    }

    @Test
    public void preparedBuilder_KeepsItsOptionsAndTakesTheConfiguredOnes()
            throws Exception {
        try (Pipeline pipeline = new PipelineBuilder()
                .addElementBuilder(splitter("|"))
                .buildFromConfiguration(listSplitterOptions("50"))) {
            ListSplitterElement element =
                pipeline.getElement(ListSplitterElement.class);
            assertEquals("|", element.getDelimiter());
            assertEquals(50, element.getMaxLength());
        }
    }

    @Test
    public void twoPreparedBuildersOfTheSameType_TheFirstIsUsed()
            throws Exception {
        try (Pipeline pipeline = new PipelineBuilder()
                .addElementBuilder(splitter("|"))
                .addElementBuilder(splitter(";"))
                .buildFromConfiguration(listSplitterOptions("50"))) {
            assertEquals("|", pipeline.getElement(ListSplitterElement.class)
                .getDelimiter());
        }
    }

    @Test
    public void preparedBuilderOfAnotherType_ANewBuilderIsConstructed()
            throws Exception {
        try (Pipeline pipeline = new PipelineBuilder()
                .addElementBuilder(new MultiplyByElementBuilder())
                .buildFromConfiguration(listSplitterOptions("50"))) {
            // A new list splitter builder starts with a comma.
            assertEquals(",", pipeline.getElement(ListSplitterElement.class)
                .getDelimiter());
        }
    }

    @Test
    public void nullBuilder_IsRefused() {
        assertThrows(IllegalArgumentException.class,
            () -> new PipelineBuilder().addElementBuilder(null));
    }

    @Test
    public void valueTheParseMethodRefuses_ErrorKeepsTheParseMessage() {
        PipelineConfigurationException thrown = assertThrows(
            PipelineConfigurationException.class,
            () -> new PipelineBuilder().buildFromConfiguration(
                listSplitterOptions("fifty")));
        assertTrue(thrown.getMessage(),
            thrown.getMessage().contains("For input string"));
    }
}
