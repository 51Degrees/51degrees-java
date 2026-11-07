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

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules that decide when usage sharing may send after failed sends,
 * with each test setting the time, and the builder settings for them.
 */
public class ShareUsageBackOffTests {

    private static final long START = 1767225600000L;

    private static long at(double seconds) {
        return START + Math.round(seconds * 1000);
    }

    /**
     * Three failures within a ten second window start a two second pause,
     * rising by two each time up to five.
     */
    private static ShareUsageBackOff create(
        int failuresToEnterRecovery,
        double initialDelaySeconds,
        double maxDelaySeconds) {
        return new ShareUsageBackOff(
            failuresToEnterRecovery,
            10,
            initialDelaySeconds,
            maxDelaySeconds,
            2);
    }

    private static ShareUsageBackOff create() {
        return create(3, 2, 5);
    }

    @Test
    public void ShareUsageBackOff_PausesAfterFailuresWithinWindow() {
        ShareUsageBackOff backOff = create();

        assertEquals(0, backOff.failed(at(0)));
        assertEquals(0, backOff.failed(at(1)));
        assertEquals(0, backOff.remaining(at(1)),
            "Sending paused before enough sends failed.");
        assertFalse(backOff.isBackingOff());

        assertEquals(2000, backOff.failed(at(2)));
        assertTrue(backOff.isBackingOff());
        assertEquals(1000, backOff.remaining(at(3)));
        assertEquals(0, backOff.remaining(at(4)));
    }

    @Test
    public void ShareUsageBackOff_FailuresOutsideWindowDoNotCount() {
        ShareUsageBackOff backOff = create();

        backOff.failed(at(0));
        backOff.failed(at(1));

        assertEquals(0, backOff.failed(at(11.5)),
            "Failures older than the window were counted.");
        assertFalse(backOff.isBackingOff());
    }

    /**
     * The window counts failures, not failures in a row, so a high failure
     * rate pauses sending even when some sends succeed.
     */
    @Test
    public void ShareUsageBackOff_SuccessesBetweenFailuresDoNotReset() {
        ShareUsageBackOff backOff = create();

        backOff.failed(at(0));
        assertFalse(backOff.succeeded());
        backOff.failed(at(1));
        assertFalse(backOff.succeeded());

        assertEquals(2000, backOff.failed(at(2)));
    }

    @Test
    public void ShareUsageBackOff_FailedSendAfterPauseMultipliesDelay() {
        ShareUsageBackOff backOff = create(1, 2, 5);

        assertEquals(2000, backOff.failed(at(0)));
        assertEquals(4000, backOff.failed(at(2)));
        assertEquals(5000, backOff.failed(at(6)),
            "The delay grew past the maximum.");
        assertEquals(5000, backOff.failed(at(11)));
        assertEquals(5000, backOff.remaining(at(11)));
    }

    @Test
    public void ShareUsageBackOff_SuccessEndsBackOff() {
        ShareUsageBackOff backOff = create(2, 2, 5);
        backOff.failed(at(0));
        backOff.failed(at(1));
        backOff.failed(at(3));

        assertTrue(backOff.succeeded(),
            "A success did not end the back-off.");
        assertFalse(backOff.isBackingOff());
        assertEquals(0, backOff.remaining(at(4)));

        // The next pause needs the full number of failures again, and starts
        // from the initial delay.
        assertEquals(0, backOff.failed(at(8)));
        assertEquals(2000, backOff.failed(at(9)));
    }

    @Test
    public void ShareUsageBackOff_MaximumBelowInitialLimitsFirstPause() {
        ShareUsageBackOff backOff = create(1, 10, 3);

        assertEquals(3000, backOff.failed(at(0)));
    }

    @Test
    public void ShareUsageBackOff_InvalidSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> new ShareUsageBackOff(0, 10, 2, 5, 2));
        assertThrows(IllegalArgumentException.class,
            () -> new ShareUsageBackOff(101, 10, 2, 5, 2));
        assertThrows(IllegalArgumentException.class,
            () -> new ShareUsageBackOff(1, 0, 2, 5, 2));
        assertThrows(IllegalArgumentException.class,
            () -> new ShareUsageBackOff(1, 10, 0, 5, 2));
        assertThrows(IllegalArgumentException.class,
            () -> new ShareUsageBackOff(1, 10, Double.NaN, 5, 2));
        assertThrows(IllegalArgumentException.class,
            () -> new ShareUsageBackOff(1, 10, 2, 0, 2));
        assertThrows(IllegalArgumentException.class,
            () -> new ShareUsageBackOff(1, 10, 2, 5, 0.5));
    }

    /**
     * The builder settings that pause sending after failed sends reject
     * values the pause cannot work with.
     */
    @Test
    public void ShareUsageBuilder_RecoverySettings_RejectValuesOutOfRange() {
        ShareUsageBuilder builder =
            new ShareUsageBuilder(LoggerFactory.getILoggerFactory());

        assertThrows(IllegalArgumentException.class,
            () -> builder.setFailuresToEnterRecovery(0));
        assertThrows(IllegalArgumentException.class,
            () -> builder.setFailuresToEnterRecovery(101));
        assertThrows(IllegalArgumentException.class,
            () -> builder.setFailuresWindowSeconds(0));
        assertThrows(IllegalArgumentException.class,
            () -> builder.setInitialRecoveryDelaySeconds(0));
        assertThrows(IllegalArgumentException.class,
            () -> builder.setMaxRecoveryDelaySeconds(-1));
        assertThrows(IllegalArgumentException.class,
            () -> builder.setRecoveryMultiplier(0.5));
    }
}
