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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.derivedproperty.flowelements.DerivedPropertyElement;
import fiftyone.pipeline.derivedproperty.flowelements.DerivedPropertyElementBuilder;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;

/**
 * The {@link HumanConfidenceElement} and the Derived Property Element both
 * write the {@code derived} Element Data. In one Pipeline the Human
 * Confidence element comes after the Derived Property Element and writes
 * its two Properties into the same Element Data, so a consumer reads
 * every derived Property from one place.
 */
public class HumanConfidenceDerivedTests extends CloudRequestEngineTestsBase {

    /** A derived property that needs no source property. */
    private static final String ALWAYS_YAML = "Format: 1\n" +
        "Name: Always\n" +
        "Version: 1.0.0\n" +
        "Output: { Name: Always, Description: Always true., " +
        "ValueType: bool, IsList: false }\n" +
        "Rules:\n" +
        "  - Else: true\n";

    public HumanConfidenceDerivedTests() throws MalformedURLException {
        super();
    }

    private CloudRequestEngine engine() throws Exception {
        configureMockedClient();
        doAnswer(invocation -> {
            java.net.HttpURLConnection connection = invocation.getArgument(0);
            String path = connection.getURL().getPath();
            if (path.endsWith("properties")) {
                return accessiblePropertiesResponse;
            }
            return "{'derived':{'humanconfidence':'High',"
                + "'humanconfidencesource':'Passive'}}";
        }).when(httpClient).postData(any(), anyMap(), (byte[]) any());
        return new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey("resourcekey")
            .build();
    }

    private DerivedPropertyElement derived() {
        return new DerivedPropertyElementBuilder(loggerFactory)
            .addScript("Always", ALWAYS_YAML)
            .build();
    }

    /**
     * After the Derived Property Element, the Human Confidence element
     * writes its two Properties into the same {@code derived} Element
     * Data, and the derived property is still there.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void afterTheDerivedPropertyElementBothShareTheElementData()
        throws Exception {
        CloudRequestEngine engine = engine();
        HumanConfidenceElement element =
            new HumanConfidenceElementBuilder(loggerFactory)
                .setRequestEngine(engine).build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .addFlowElement(derived())
                .addFlowElement(element)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();
            assertTrue(data.getErrors().isEmpty(),
                String.valueOf(data.getErrors()));
            ElementData shared = data.get(
                DerivedPropertyElement.ELEMENT_DATA_KEY);
            assertEquals("High", ((AspectPropertyValue<String>) shared.get(
                HumanConfidenceElement.HUMAN_CONFIDENCE)).getValue());
            assertEquals("Passive", ((AspectPropertyValue<String>) shared.get(
                HumanConfidenceElement.HUMAN_CONFIDENCE_SOURCE)).getValue());
            assertEquals(true, ((AspectPropertyValue<Boolean>) shared.get(
                "Always")).getValue());
        }
    }

    /**
     * A Pipeline with the Derived Property Element after the Human
     * Confidence element is refused, because the Derived Property Element
     * can't write into Element Data another element made.
     */
    @Test
    public void beforeTheDerivedPropertyElementThePipelineIsRefused()
        throws Exception {
        CloudRequestEngine engine = engine();
        PipelineBuilder builder = new PipelineBuilder(loggerFactory)
            .addFlowElement(engine)
            .addFlowElement(new HumanConfidenceElementBuilder(loggerFactory)
                .setRequestEngine(engine).build())
            .addFlowElement(derived());
        PipelineConfigurationException e = assertThrows(
            PipelineConfigurationException.class, builder::build);
        assertTrue(e.getMessage().contains("derived"), e.getMessage());
    }
}
