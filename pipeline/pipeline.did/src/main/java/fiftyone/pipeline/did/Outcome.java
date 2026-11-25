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


package fiftyone.pipeline.did;

import java.util.Locale;

/**
 * What happened when a sealed result was redeemed, as the cloud's redeem
 * endpoint gives it in {@code status}. Only {@link #SUCCESS} carries an
 * unsealed result. The four other outcomes are failures, and the cloud
 * still writes the same word to {@code context} as it did before
 * {@code status} existed.
 */
public enum Outcome {

    /** The sealed result was redeemed, and the unsealed result stands. */
    SUCCESS("success"),

    /** The sealed result is genuine, but was redeemed outside its window. */
    EXPIRED("expired"),

    /** The sealed result is genuine, but had already been redeemed. */
    REPLAYED("replayed"),

    /**
     * First use could not be confirmed, answered with HTTP 503. Not a
     * verdict, and the caller may retry.
     */
    UNCONFIRMED("unconfirmed"),

    /**
     * The sealed result could not be read: tampered, made for another
     * 51Did, sealed under another transaction or License Key, or an answer
     * this client did not recognise.
     */
    UNREADABLE("unreadable");

    private final String value;

    Outcome(String value) {
        this.value = value;
    }

    /** @return the word the cloud uses for this outcome in {@code status} */
    public String getValue() {
        return value;
    }

    /**
     * Maps the cloud's {@code status} word to an outcome. The comparison
     * ignores case. Anything unrecognised, including a missing value, is
     * {@link #UNREADABLE}, so an answer this client does not know fails
     * closed.
     *
     * @param value the {@code status} string from the cloud, or null
     * @return the matching outcome, or {@link #UNREADABLE}
     */
    public static Outcome fromValue(String value) {
        if (value != null) {
            String word = value.toLowerCase(Locale.ROOT);
            for (Outcome outcome : values()) {
                if (outcome.value.equals(word)) {
                    return outcome;
                }
            }
        }
        return UNREADABLE;
    }
}
