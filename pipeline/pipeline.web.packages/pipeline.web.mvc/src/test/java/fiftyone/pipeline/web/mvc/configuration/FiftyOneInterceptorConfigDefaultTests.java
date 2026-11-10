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

package fiftyone.pipeline.web.mvc.configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FiftyOneInterceptorConfigDefaultTests {

    /**
     * A negative max-age is refused where it is set, rather than when the
     * interceptor starts.
     */
    @Test
    public void setClientsideCacheMaxAge_Negative_Throws() {
        FiftyOneInterceptorConfigDefault config =
            new FiftyOneInterceptorConfigDefault();
        try {
            config.setClientsideCacheMaxAge(-1);
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("-1"));
        }
    }

    /**
     * A max-age of 0 is kept, as it tells the browser to ask again on each
     * use.
     */
    @Test
    public void setClientsideCacheMaxAge_Zero_Kept() {
        FiftyOneInterceptorConfigDefault config =
            new FiftyOneInterceptorConfigDefault();
        config.setClientsideCacheMaxAge(0);
        assertEquals(0, config.getClientsideCacheMaxAge());
    }
}
