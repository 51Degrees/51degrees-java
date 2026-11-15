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

package fiftyone.pipeline.core.data;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * "Keys MUST be case-insensitive and will usually be split into
 * &lt;prefix&gt;.&lt;field&gt;." (pipeline-specification/features/evidence.md
 * line 10)
 */
public class EvidenceKeyFilterWhitelistCaseTests {

    @Test
    public void listConstructor_KeysAreCaseInsensitive() {
        EvidenceKeyFilterWhitelist filter = new EvidenceKeyFilterWhitelist(
            Arrays.asList("header.User-Agent", "query.51D_Pixels"));

        assertTrue(filter.include("header.user-agent"));
        assertTrue(filter.include("HEADER.USER-AGENT"));
        assertTrue(filter.include("Query.51d_pixels"));
        assertEquals(Integer.valueOf(0), filter.order("HEADER.user-agent"));
        assertFalse(filter.include("header.accept"));
    }

    @Test
    public void mapConstructor_KeysAreCaseInsensitive() {
        Map<String, Integer> keys = new HashMap<>();
        keys.put("header.User-Agent", 1);
        keys.put("cookie.51D_ScreenPixelsWidth", 2);
        EvidenceKeyFilterWhitelist filter = new EvidenceKeyFilterWhitelist(keys);

        assertTrue(filter.include("header.user-agent"));
        assertTrue(filter.include("COOKIE.51d_screenpixelswidth"));
        assertEquals(Integer.valueOf(2),
            filter.order("Cookie.51D_SCREENPIXELSWIDTH"));
    }

    @Test
    public void aggregator_DefaultWhitelistKeysAreCaseInsensitive() {
        EvidenceKeyFilterAggregator filter = new EvidenceKeyFilterAggregator();
        filter.addFilter(new EvidenceKeyFilterWhitelist(
            Arrays.asList("key2")));

        assertTrue(filter.include("key2"));
        assertTrue(filter.include("KEY2"));
    }
}
