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

package fiftyone.devicedetection.hash.engine.onpremise.flowelements;

import fiftyone.pipeline.engines.fiftyone.data.FiftyOneAspectPropertyMetaData;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Metadata for the match metric Properties MUST be added, as they will not be
 * included in the metadata exposed by the native code
 * (device-detection-specification/pipeline-elements/device-detection-on-premise.md
 * line 452 at 706de1f0). The table that follows gives each one's Category,
 * Available With, Component, type and default value. The native engine here
 * is a mock with no properties, so every property the engine lists is one it
 * added itself.
 */
public class MatchMetricMetaDataTests {

    private static final List<String> NAMES = Arrays.asList(
        "MatchedNodes", "Difference", "Drift", "DeviceId", "UserAgents",
        "Iterations", "Method");

    private static List<FiftyOneAspectPropertyMetaData> metrics()
        throws Exception {
        return new EngineOverMockNativeCode().engine.getProperties();
    }

    /**
     * The engine lists the seven, in the order of the table.
     */
    @Test
    public void MatchMetrics_AllListedInTableOrder() throws Exception {
        List<FiftyOneAspectPropertyMetaData> metrics = metrics();
        assertEquals(NAMES.size(), metrics.size());
        for (int i = 0; i < NAMES.size(); i++) {
            assertEquals(NAMES.get(i), metrics.get(i).getName());
        }
    }

    /**
     * Category = "Device Metrics" and Available With = "Lite", "Premium",
     * "Enterprise", "TAC".
     */
    @Test
    public void MatchMetrics_CategoryAndAvailableWith() throws Exception {
        for (FiftyOneAspectPropertyMetaData property : metrics()) {
            assertEquals(property.getName(),
                "Device Metrics", property.getCategory());
            assertEquals(property.getName(),
                Arrays.asList("Lite", "Premium", "Enterprise", "TAC"),
                property.getDataTiersWherePresent());
        }
    }

    /**
     * Component = "Metrics".
     */
    @Test
    public void MatchMetrics_ComponentIsMetrics() throws Exception {
        for (FiftyOneAspectPropertyMetaData property : metrics()) {
            assertNotNull(
                property.getName() + " has no component",
                property.getComponent());
            assertEquals(property.getName(),
                "Metrics", property.getComponent().getName());
        }
    }

    /**
     * The types in the table: int, string, and list of strings for
     * UserAgents only, so only UserAgents is a list.
     */
    @Test
    public void MatchMetrics_TypesAsTheTableGives() throws Exception {
        for (FiftyOneAspectPropertyMetaData property : metrics()) {
            switch (property.getName()) {
                case "MatchedNodes":
                case "Difference":
                case "Drift":
                case "Iterations":
                    assertEquals(property.getName(),
                        Integer.class, property.getType());
                    assertFalse(property.getName(), property.getList());
                    break;
                case "DeviceId":
                case "Method":
                    assertEquals(property.getName(),
                        String.class, property.getType());
                    assertFalse(property.getName() + " is a string, " +
                        "not a list", property.getList());
                    break;
                case "UserAgents":
                    assertEquals(property.getName(),
                        List.class, property.getType());
                    break;
                default:
                    throw new AssertionError(
                        "Unexpected property " + property.getName());
            }
        }
    }

    /**
     * The default values in the table, other than the list-valued UserAgents
     * whose description in Property metadata is an open question.
     */
    @Test
    public void MatchMetrics_DefaultValues() throws Exception {
        for (FiftyOneAspectPropertyMetaData property : metrics()) {
            String expected;
            switch (property.getName()) {
                case "DeviceId": expected = "0-0-0-0"; break;
                case "Method": expected = "NONE"; break;
                case "UserAgents": continue;
                default: expected = "0"; break;
            }
            assertEquals(property.getName(),
                expected, property.getDefaultValue().getName());
        }
    }
}
