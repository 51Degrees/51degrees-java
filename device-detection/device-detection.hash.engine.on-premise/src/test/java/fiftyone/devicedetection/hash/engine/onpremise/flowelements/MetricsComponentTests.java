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

import fiftyone.pipeline.engines.fiftyone.data.CloseableIterable;
import fiftyone.pipeline.engines.fiftyone.data.ComponentMetaData;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneAspectPropertyMetaData;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Component = "Metrics" - This component MUST also be added to the list of
 * components returned by the Engine
 * (device-detection-specification/pipeline-elements/device-detection-on-premise.md
 * line 464 at 706de1f0). The native engine here is a mock with no components,
 * so every component the engine lists is one it added itself.
 */
public class MetricsComponentTests {

    private static List<ComponentMetaData> components(
        DeviceDetectionHashEngine engine) throws Exception {
        List<ComponentMetaData> result = new ArrayList<>();
        try (CloseableIterable<ComponentMetaData> components =
                 engine.getComponents()) {
            for (ComponentMetaData component : components) {
                result.add(component);
            }
        }
        return result;
    }

    /**
     * The engine's components include Metrics.
     */
    @Test
    public void Components_IncludeMetrics() throws Exception {
        List<ComponentMetaData> components =
            components(new EngineOverMockNativeCode().engine);
        assertEquals(1, components.size());
        assertEquals("Metrics", components.get(0).getName());
    }

    /**
     * The engine finds Metrics by its name.
     */
    @Test
    public void Components_GetByName() throws Exception {
        ComponentMetaData metrics =
            new EngineOverMockNativeCode().engine.getComponent("Metrics");
        assertNotNull("No component named Metrics", metrics);
        assertEquals("Metrics", metrics.getName());
    }

    /**
     * The Metrics component holds the match metric properties, each of which
     * names Metrics as its component.
     */
    @Test
    public void Components_MetricsHoldsTheMatchMetrics() throws Exception {
        DeviceDetectionHashEngine engine =
            new EngineOverMockNativeCode().engine;
        ComponentMetaData metrics = components(engine).get(0);
        List<String> names = new ArrayList<>();
        try (CloseableIterable<FiftyOneAspectPropertyMetaData> properties =
                 metrics.getProperties()) {
            for (FiftyOneAspectPropertyMetaData property : properties) {
                names.add(property.getName());
                assertEquals(property.getName(),
                    "Metrics", property.getComponent().getName());
            }
        }
        assertEquals(
            Arrays.asList("MatchedNodes", "Difference", "Drift", "DeviceId",
                "UserAgents", "Iterations", "Method"),
            names);
        assertNotNull(metrics.getProperty("DeviceId"));
    }
}
