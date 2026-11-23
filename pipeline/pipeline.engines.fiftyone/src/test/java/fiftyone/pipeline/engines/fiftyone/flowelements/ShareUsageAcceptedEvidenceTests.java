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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngine;
import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngineBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The usage sharing element accepts no Evidence, so the Pipeline's accepted
 * Evidence, from which cache keys, the ETag and the Vary header are made, is
 * that of its other elements alone.
 */
public class ShareUsageAcceptedEvidenceTests {

    @Test
    public void Pipeline_WithShareUsage_AcceptsOnlyTheOtherElementsEvidence()
        throws Exception {
        ShareUsageElement shareUsage = new ShareUsageBuilder(
            LoggerFactory.getILoggerFactory())
            .setSharePercentage(0)
            .build();
        EmptyEngine engine = new EmptyEngineBuilder(
            LoggerFactory.getILoggerFactory())
            .build();
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .addFlowElement(engine)
                .addFlowElement(shareUsage)
                .setAutoCloseElements(true)
                .build()) {
            EvidenceKeyFilter accepted = pipeline.getEvidenceKeyFilter();

            assertTrue(accepted.include("test.value"),
                "The other element's Evidence is not accepted.");
            assertFalse(accepted.include("header.if-none-match"),
                "If-None-Match is accepted, so a revalidation's ETag differs " +
                    "from the one it sends.");
            assertFalse(accepted.include("header.x-test"),
                "A header no element uses is accepted.");
            assertFalse(accepted.include("server.http-request"),
                "The web request is accepted.");
            assertFalse(shareUsage.getEvidenceKeyFilter().include("header.user-agent"),
                "The usage sharing element accepts Evidence.");
        }
    }
}
