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

package fiftyone.pipeline.core.exceptions;

import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * CloseException.of gives back an unchecked exception as it is, wraps a
 * checked one, and sets the interrupt status again for an
 * InterruptedException.
 */
public class CloseExceptionTests {

    private static final String MESSAGE = "closing failed";

    @After
    public void clearInterrupt() {
        // A test that fails part way must not leave the interrupt for the
        // next test on this thread.
        Thread.interrupted();
    }

    @Test
    public void of_Unchecked_ReturnedUnchanged() {
        RuntimeException cause = new IllegalStateException("unchecked");

        assertSame(cause, CloseException.of(MESSAGE, cause));
        assertFalse(Thread.interrupted());
    }

    @Test
    public void of_Checked_Wrapped() {
        IOException cause = new IOException("checked");

        RuntimeException result = CloseException.of(MESSAGE, cause);

        assertTrue(result instanceof CloseException);
        assertEquals(MESSAGE, result.getMessage());
        assertSame(cause, result.getCause());
        assertFalse(Thread.interrupted());
    }

    @Test
    public void of_Interrupted_WrappedAndInterruptKept() {
        InterruptedException cause = new InterruptedException("interrupted");

        RuntimeException result = CloseException.of(MESSAGE, cause);

        assertTrue(result instanceof CloseException);
        assertSame(cause, result.getCause());
        assertTrue("The interrupt was lost.", Thread.interrupted());
    }
}
