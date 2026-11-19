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

package fiftyone.pipeline.engines.fiftyone.data;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngineBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The usage sharing element finds the values it shares on every request, so
 * with no web request finding them reads the Evidence rather than copying
 * every value of it.
 */
public class ShareUsageEvidenceTests {

    private static Pipeline createPipeline() throws Exception {
        return new PipelineBuilder(LoggerFactory.getILoggerFactory())
            .addFlowElement(new EmptyEngineBuilder(
                LoggerFactory.getILoggerFactory()).build())
            .setAutoCloseElements(true)
            .build();
    }

    /**
     * With no web request the values found are the Evidence itself, so a
     * value added afterwards is among them. A copy would not hold it.
     */
    @Test
    public void Find_NoWebRequest_ReadsTheEvidenceWithoutCopying()
        throws Exception {
        try (Pipeline pipeline = createPipeline();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", "evidence-ua");
            Map<String, Object> found =
                ShareUsageEvidence.find(data.getEvidence());
            data.addEvidence("header.x-later", "later");

            assertEquals("later", found.get("header.x-later"),
                "The Evidence was copied, so every request pays for a copy " +
                    "of every Evidence value.");
        }
    }

    /**
     * With no web request the values found are still keyed without regard
     * to case.
     */
    @Test
    public void Find_NoWebRequest_KeyedWithoutRegardToCase()
        throws Exception {
        try (Pipeline pipeline = createPipeline();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.User-Agent", "evidence-ua");
            Map<String, Object> found =
                ShareUsageEvidence.find(data.getEvidence());

            assertEquals("evidence-ua", found.get("HEADER.USER-AGENT"));
            assertEquals(1, found.size());
        }
    }

    /**
     * With a web request its values are added under their own keys, the
     * Evidence value is kept for a key in both, and the web request itself
     * is not one of the values.
     */
    @Test
    public void Find_WebRequest_AddsItsValuesAndKeepsTheEvidenceValue()
        throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("header.User-Agent", "request-ua");
        request.put("header.X-Test", "shared");
        try (Pipeline pipeline = createPipeline();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", "evidence-ua");
            data.addEvidence(ShareUsageEvidence.HTTP_REQUEST_KEY,
                Collections.unmodifiableMap(request));
            Map<String, Object> found =
                ShareUsageEvidence.find(data.getEvidence());

            assertEquals("evidence-ua", found.get("HEADER.USER-AGENT"));
            assertEquals("shared", found.get("header.x-test"));
            assertFalse(found.containsKey(ShareUsageEvidence.HTTP_REQUEST_KEY));
            assertEquals(2, found.size());
        }
    }
}
