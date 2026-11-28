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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.derivedproperty.data.IDerivedPropertyData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A request with a sealed Human Confidence result, being a non-empty
 * {@code query.humanconfidence}, gives HumanConfidence {@code Medium} and
 * HumanConfidenceSource {@code Active} from an element that writes
 * HumanConfidence, and never the High or Low its script works out. Every
 * other request is unchanged.
 */
public class SealedHumanConfidenceTests {

    private static final String SEALED = "sealed-result-0123456789";

    /**
     * A script that creates HumanConfidence, giving High or Low from one
     * source property.
     */
    private static final String HUMAN_CONFIDENCE_YAML = "Format: 1\n" +
        "Name: HumanConfidence\n" +
        "Version: 1.0.0\n" +
        "Output:\n" +
        "  Name: HumanConfidence\n" +
        "  Description: How far the signature supports a person.\n" +
        "  ValueType: string\n" +
        "  IsList: false\n" +
        "Rules:\n" +
        "  - When: { Property: signature.Verified, Eq: true }\n" +
        "    Then: High\n" +
        "  - Else: Low\n";

    /**
     * A script that creates a property other than HumanConfidence.
     */
    private static final String VERIFIED_YAML = "Format: 1\n" +
        "Name: Verified\n" +
        "Version: 1.0.0\n" +
        "Output:\n" +
        "  Name: IsVerified\n" +
        "  Description: Whether the signature is verified.\n" +
        "  ValueType: bool\n" +
        "  IsList: false\n" +
        "Rules:\n" +
        "  - When: { Property: signature.Verified, Eq: true }\n" +
        "    Then: true\n" +
        "  - Else: false\n";

    /**
     * A sealed result gives Medium and Active, whether the script would
     * give High or Low, and the evidence key's letter case does not
     * matter.
     */
    @Test
    public void aSealedResultGivesMediumAndActive() throws Exception {
        for (boolean verified : new boolean[] { true, false }) {
            for (String key : new String[] {
                "query.humanconfidence", "query.HumanConfidence" }) {
                try (Pipeline pipeline = pipeline("HumanConfidence",
                        HUMAN_CONFIDENCE_YAML, verified);
                    FlowData data = pipeline.createFlowData()) {
                    process(data, key, SEALED);
                    assertEquals("Medium", value(data, "HumanConfidence"),
                        key + " " + verified);
                    assertEquals("Active",
                        value(data, "HumanConfidenceSource"));
                    assertTrue(data.getErrors() == null ||
                        data.getErrors().isEmpty());
                }
            }
        }
    }

    /**
     * With no sealed result, an empty {@code query.humanconfidence}, or a
     * value named humanconfidence under another key, the script's value
     * stands and no HumanConfidenceSource is written.
     */
    @Test
    public void withoutASealedResultTheScriptsValueStands() throws Exception {
        String[][] requests = {
            { null, null },
            { "query.humanconfidence", "" },
            { "header.humanconfidence", SEALED },
        };
        for (boolean verified : new boolean[] { true, false }) {
            for (String[] request : requests) {
                try (Pipeline pipeline = pipeline("HumanConfidence",
                        HUMAN_CONFIDENCE_YAML, verified);
                    FlowData data = pipeline.createFlowData()) {
                    process(data, request[0], request[1]);
                    assertEquals(verified ? "High" : "Low",
                        value(data, "HumanConfidence"), request[0]);
                    assertFalse(data.get(IDerivedPropertyData.class)
                        .asKeyMap().containsKey("HumanConfidenceSource"));
                }
            }
        }
    }

    /**
     * An element that does not write HumanConfidence is unchanged by a
     * sealed result, and writes neither Property.
     */
    @Test
    public void anElementWithoutHumanConfidenceIsUnchanged()
        throws Exception {
        try (Pipeline pipeline = pipeline("Verified", VERIFIED_YAML, true);
            FlowData data = pipeline.createFlowData()) {
            process(data, "query.humanconfidence", SEALED);
            IDerivedPropertyData derived =
                data.get(IDerivedPropertyData.class);
            assertEquals(true,
                derived.getAs("IsVerified", Boolean.class).getValue());
            assertFalse(derived.asKeyMap().containsKey("HumanConfidence"));
            assertFalse(derived.asKeyMap().containsKey(
                "HumanConfidenceSource"));
        }
    }

    private static Pipeline pipeline(
        String name,
        String script,
        boolean verified) throws Exception {
        return new TestPipelineBuilder()
            .addFlowElement(new StubSourceElement("signature",
                Collections.singletonList("Verified"),
                DerivedPropertyElementTests.map("Verified", verified)))
            .addFlowElement(DerivedPropertyElementTests.builder()
                .addScript(name, script).build())
            .setAutoCloseElements(true)
            .build();
    }

    private static void process(FlowData data, String key, Object value) {
        if (key != null) {
            data.addEvidence(key, value);
        }
        data.process();
    }

    private static Object value(FlowData data, String name) {
        AspectPropertyValue<String> value = data
            .get(IDerivedPropertyData.class)
            .getAs(name, String.class);
        assertTrue(value.hasValue(), value::getNoValueMessage);
        return value.getValue();
    }
}
