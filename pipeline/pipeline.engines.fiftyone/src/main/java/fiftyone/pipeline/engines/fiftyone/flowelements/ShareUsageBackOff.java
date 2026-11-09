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

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Decides when usage sharing may send. Sending pauses for the initial delay
 * once the set number of sends fail within the window. A failed send after a
 * pause multiplies the delay, up to the maximum, and a successful send ends
 * the back-off. Only sending pauses, so evidence is still queued while
 * paused. Each method takes the time from the caller, in milliseconds on
 * the clock that {@link #now()} reads, so a change to the system time cannot
 * lengthen or skip a pause.
 */
class ShareUsageBackOff {

    private final int failuresToEnterRecovery;
    private final long failuresWindowMillis;
    private final double initialDelaySeconds;
    private final double maxDelaySeconds;
    private final double multiplier;

    /**
     * When each recent failed send happened, oldest first. Empty while
     * backing off.
     */
    private final Deque<Long> failures = new ArrayDeque<>();

    /**
     * The length of the current pause in seconds, or zero when not backing
     * off.
     */
    private double delaySeconds = 0;

    /**
     * When sending may start again.
     */
    private long resumeAt = Long.MIN_VALUE;

    /**
     * @return the time in milliseconds on a monotonic clock, for the caller to
     * pass to the other methods. Its zero is not a calendar time.
     */
    static long now() {
        return System.nanoTime() / 1000000;
    }

    ShareUsageBackOff(
        int failuresToEnterRecovery,
        int failuresWindowSeconds,
        double initialDelaySeconds,
        double maxDelaySeconds,
        double multiplier) {
        if (failuresToEnterRecovery <
                Constants.SHARE_USAGE_FAILURES_TO_ENTER_RECOVERY_MIN ||
            failuresToEnterRecovery >
                Constants.SHARE_USAGE_FAILURES_TO_ENTER_RECOVERY_MAX) {
            throw new IllegalArgumentException(
                "failuresToEnterRecovery must be from " +
                    Constants.SHARE_USAGE_FAILURES_TO_ENTER_RECOVERY_MIN +
                    " to " +
                    Constants.SHARE_USAGE_FAILURES_TO_ENTER_RECOVERY_MAX + ".");
        }
        if (failuresWindowSeconds < 1) {
            throw new IllegalArgumentException(
                "failuresWindowSeconds must be 1 or more.");
        }
        if ((initialDelaySeconds > 0) == false) {
            throw new IllegalArgumentException(
                "initialDelaySeconds must be more than 0.");
        }
        if ((maxDelaySeconds > 0) == false) {
            throw new IllegalArgumentException(
                "maxDelaySeconds must be more than 0.");
        }
        if ((multiplier >= 1) == false) {
            throw new IllegalArgumentException(
                "multiplier must be 1 or more.");
        }
        this.failuresToEnterRecovery = failuresToEnterRecovery;
        this.failuresWindowMillis = failuresWindowSeconds * 1000L;
        this.initialDelaySeconds = initialDelaySeconds;
        this.maxDelaySeconds = maxDelaySeconds;
        this.multiplier = multiplier;
    }

    /**
     * @return true from the start of a pause until a send succeeds
     */
    synchronized boolean isBackingOff() {
        return delaySeconds > 0;
    }

    /**
     * @param now the time now in milliseconds
     * @return the milliseconds left before sending may start again, or zero
     * if it may start now
     */
    synchronized long remaining(long now) {
        return resumeAt > now ? resumeAt - now : 0;
    }

    /**
     * Records a failed send.
     * @param now the time now in milliseconds
     * @return the new pause in milliseconds if this failure started or
     * lengthened one, otherwise zero
     */
    synchronized long failed(long now) {
        if (delaySeconds > 0) {
            // The send tried when the last pause ended failed.
            delaySeconds = Math.min(
                delaySeconds * multiplier,
                maxDelaySeconds);
        } else {
            failures.addLast(now);
            while (failures.peekFirst() < now - failuresWindowMillis) {
                failures.removeFirst();
            }
            if (failures.size() < failuresToEnterRecovery) {
                return 0;
            }
            failures.clear();
            delaySeconds = Math.min(initialDelaySeconds, maxDelaySeconds);
        }
        long pause = Math.max(1, Math.round(delaySeconds * 1000));
        resumeAt = now + pause;
        return pause;
    }

    /**
     * Records a successful send.
     * @return true if this ended a back-off
     */
    synchronized boolean succeeded() {
        if (delaySeconds == 0) {
            return false;
        }
        delaySeconds = 0;
        resumeAt = Long.MIN_VALUE;
        return true;
    }
}
