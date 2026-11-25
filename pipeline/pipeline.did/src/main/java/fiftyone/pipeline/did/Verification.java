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

import org.json.JSONObject;

/**
 * The typed answer to {@link DidClient#verifyDetailed(FodId)}: whether the
 * cloud's verify endpoint found the 51Did's signature valid, and the Human
 * Confidence Value the 51Did carries where the cloud returned it.
 */
public final class Verification {

    private final boolean valid;
    private final HumanConfidence humanConfidence;
    private final String humanConfidenceValue;
    private final String humanConfidenceNullReason;
    private final int statusCode;
    private final String raw;

    private Verification(
            boolean valid,
            String humanConfidenceValue,
            String humanConfidenceNullReason,
            int statusCode,
            String raw) {
        this.valid = valid;
        this.humanConfidence = HumanConfidence.fromValue(humanConfidenceValue);
        this.humanConfidenceValue = humanConfidenceValue;
        this.humanConfidenceNullReason = humanConfidenceNullReason;
        this.statusCode = statusCode;
        this.raw = raw;
    }

    /**
     * Builds an answer from the cloud's verify endpoint answer, which
     * carries {@code valid}.
     *
     * @param statusCode the HTTP status, 200 or 400
     * @param json       the body parsed as a JSON object
     * @param raw        the body as received
     * @return the typed answer
     */
    static Verification parse(int statusCode, JSONObject json, String raw) {
        return new Verification(
            json.getBoolean("valid"),
            json.optString("humanConfidence", null),
            json.optString("humanConfidenceNullReason", null),
            statusCode,
            raw);
    }

    /** @return whether the cloud found the 51Did's signature valid */
    public boolean isValid() {
        return valid;
    }

    /**
     * The Human Confidence Value the 51Did carries. The cloud returns it
     * only for a 51Did whose signature verifies, to a call that presents a
     * License Key valid for Human Confidence. A 51Did made before the Value
     * was added is {@link HumanConfidence#ABSENT}.
     *
     * @return the Value, or null where the cloud did not return one, in
     *         which case {@link #getHumanConfidenceNullReason()} usually
     *         says why
     */
    public HumanConfidence getHumanConfidence() {
        return humanConfidence;
    }

    /**
     * @return the {@code humanConfidence} word exactly as the cloud sent
     *         it, or null where it sent none
     */
    public String getHumanConfidenceValue() {
        return humanConfidenceValue;
    }

    /**
     * Why the cloud did not return the Human Confidence Value, which is
     * usually that the call did not present a License Key valid for Human
     * Confidence.
     *
     * @return the cloud's {@code humanConfidenceNullReason}, or null where
     *         it sent none
     */
    public String getHumanConfidenceNullReason() {
        return humanConfidenceNullReason;
    }

    /** @return the HTTP status the cloud answered with, 200 or 400 */
    public int getStatusCode() {
        return statusCode;
    }

    /** @return the response body as received */
    public String getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return "Verification " + statusCode + " valid=" + valid
            + (humanConfidenceValue == null
                ? ""
                : " humanConfidence=" + humanConfidenceValue);
    }
}
