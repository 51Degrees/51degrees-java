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

package fiftyone.ipintelligence.engine.onpremise.flowelements;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * The native lists of weighted values are closed as a try-with-resources
 * statement closes its resources.
 */
public class UseAndCloseTests {

    @Test
    public void actionSucceeds_ClosesOnce() {
        List<String> closed = new ArrayList<>();
        IPIntelligenceDataHashDefault.useAndClose(
            "native", r -> { }, closed::add);
        assertEquals(1, closed.size());
        assertEquals("native", closed.get(0));
    }

    @Test
    public void nullResource_IsNotClosed() {
        List<String> closed = new ArrayList<>();
        IPIntelligenceDataHashDefault.<String>useAndClose(
            null, r -> { }, closed::add);
        assertEquals(0, closed.size());
    }

    @Test
    public void actionAndCloseThrow_CloseIsSuppressed() {
        IllegalStateException action = new IllegalStateException("action");
        IllegalArgumentException close =
            new IllegalArgumentException("close");
        try {
            IPIntelligenceDataHashDefault.useAndClose(
                "native",
                r -> { throw action; },
                r -> { throw close; });
            fail("The action's exception was not thrown");
        } catch (IllegalStateException e) {
            assertSame(action, e);
            assertEquals(1, e.getSuppressed().length);
            assertSame(close, e.getSuppressed()[0]);
        }
    }

    @Test
    public void onlyCloseThrows_CloseIsThrown() {
        IllegalArgumentException close =
            new IllegalArgumentException("close");
        try {
            IPIntelligenceDataHashDefault.useAndClose(
                "native",
                r -> { },
                r -> { throw close; });
            fail("The close's exception was not thrown");
        } catch (IllegalArgumentException e) {
            assertSame(close, e);
        }
    }
}
