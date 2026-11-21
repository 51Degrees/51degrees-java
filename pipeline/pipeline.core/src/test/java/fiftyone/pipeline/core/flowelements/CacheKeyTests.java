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

package fiftyone.pipeline.core.flowelements;

import fiftyone.pipeline.core.data.DataKey;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByFiveElement;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;

/**
 * Cache keys are built from the Evidence that the element advertises, with
 * values compared with their case. See the Generation of keys section of
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/caching.md#L57">caching.md</a>.
 */
public class CacheKeyTests {

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    private final MultiplyByFiveElement element =
        new MultiplyByFiveElement(mock(Logger.class));

    private DataKey keyFor(Map<String, Object> evidence) throws Exception {
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(element)
                .build();
             FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence(evidence);
            EvidenceKeyFilter filter = element.getEvidenceKeyFilter();
            return flowData.generateKey(filter);
        }
    }

    private static Map<String, Object> evidence(Object... pairs) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    /**
     * caching.md line 59 at 706de1f0: "All Flow Elements MUST advertize the
     * Evidence keys that they make use of." The key is built from those keys
     * alone, so Evidence the element does not use does not change it, and
     * Evidence it uses does.
     */
    @Test
    public void key_IsBuiltFromTheAdvertisedEvidenceOnly() throws Exception {
        String used = element.evidenceKeys.get(0);
        DataKey plain = keyFor(evidence(used, 2));
        DataKey withOther = keyFor(evidence(used, 2, "query.other", "x"));
        DataKey otherValue = keyFor(evidence(used, 3));

        assertEquals(
            "Evidence the element does not use changed the key.",
            plain, withOther);
        assertEquals(plain.hashCode(), withOther.hashCode());
        assertNotEquals(
            "Evidence the element uses did not change the key.",
            plain, otherValue);
    }

    /**
     * caching.md line 82 at 706de1f0: "Comparison of Evidence values MUST
     * be case-sensitive."
     */
    @Test
    public void values_AreComparedWithCase() throws Exception {
        String used = element.evidenceKeys.get(0);
        assertNotEquals(
            "Two values that differ only in case gave the same key.",
            keyFor(evidence(used, "abc")),
            keyFor(evidence(used, "ABC")));
        assertEquals(
            keyFor(evidence(used, "abc")),
            keyFor(evidence(used, "abc")));
    }
}
