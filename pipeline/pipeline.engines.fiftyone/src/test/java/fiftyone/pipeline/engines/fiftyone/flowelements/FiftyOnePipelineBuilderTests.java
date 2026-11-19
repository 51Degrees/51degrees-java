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

import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Usage sharing is on unless the builder is told otherwise, as
 * {@link Constants#SHARE_USAGE_DEFAULT_ENABLED} says.
 */
public class FiftyOnePipelineBuilderTests {

    @Test
    public void ShareUsage_DefaultValue() {
        assertTrue(Constants.SHARE_USAGE_DEFAULT_ENABLED);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void ShareUsage_NotSet_FollowsDefault() throws Exception {
        try (Pipeline pipeline = new FiftyOnePipelineBuilder()
            .setAutoCloseElements(true)
            .build()) {
            int shareUsage = 0;
            for (FlowElement element : pipeline.getFlowElements()) {
                if (element instanceof ShareUsageElement) {
                    shareUsage++;
                }
            }
            assertEquals(
                Constants.SHARE_USAGE_DEFAULT_ENABLED ? 1 : 0,
                shareUsage);
        }
    }
}
