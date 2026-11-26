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

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The typed answer to {@link DidClient#redeem(FodId, String, String)}: the
 * {@link Outcome} of the redemption, and on success the unsealed result,
 * being the creator context verdict, the signature outcome, the per-factor
 * detail where the cloud gave it and the Human Confidence Value the 51Did
 * carries, together with when the verification behind the sealed result
 * happened.
 * <p>
 * Read {@link #getOutcome()} first. Only {@link Outcome#SUCCESS} carries an
 * unsealed result. On every other outcome the cloud writes the same word to
 * {@code context} as to {@code status}, as it did before {@code status}
 * existed, so {@link #getContext()} still reads those words, through the
 * obsolete members of {@link Context}.
 * <p>
 * Every cryptographic failure comes back from the cloud as the one word
 * {@code unreadable}, by design, so that a forger probing the endpoint
 * learns nothing about which part of a guess was wrong. This type does not
 * try to distinguish them either, and it maps any status or context value
 * it does not know to unreadable for the same reason, keeping the raw
 * values in {@link #getStatusValue()} and {@link #getContextValue()}.
 */
public final class RedeemResult {

    /** The creator context verdict. */
    public enum Context {
        /** The identifier is being presented from where it was created. */
        VERIFIED("verified"),
        /**
         * At least one factor differs from creation. {@link #getFactors()}
         * says which.
         */
        MISMATCH("mismatch"),
        /** The identifier carries no creator context to check. */
        NO_CONTEXT("nocontext"),
        /**
         * No longer sent by the service, and kept only because it has been
         * public since this enum was added. What used to give this answer
         * now gives {@link #MISCONFIGURED} where the service is at fault, or
         * {@link #INVALID_DATE} where the identifier could not have been
         * created.
         */
        NOT_CHECKABLE("notcheckable"),
        /**
         * The sealed result was redeemed outside the freshness window.
         *
         * @deprecated Not a verdict. Read {@link RedeemResult#getOutcome()},
         *             which is {@link Outcome#EXPIRED}. Still read from
         *             {@code context}, which the cloud still writes.
         */
        @Deprecated
        EXPIRED("expired"),
        /**
         * The sealed result had already been redeemed.
         *
         * @deprecated Not a verdict. Read {@link RedeemResult#getOutcome()},
         *             which is {@link Outcome#REPLAYED}. Still read from
         *             {@code context}, which the cloud still writes.
         */
        @Deprecated
        REPLAYED("replayed"),
        /**
         * The sealed result could not be read: tampered, made for another
         * identifier, sealed under another transaction or licence key, or an
         * answer this client did not recognise.
         *
         * @deprecated Not a verdict. Read {@link RedeemResult#getOutcome()},
         *             which is {@link Outcome#UNREADABLE}. Still read from
         *             {@code context}, which the cloud still writes, and
         *             still where an unrecognised {@code context} maps.
         */
        @Deprecated
        UNREADABLE("unreadable"),
        /**
         * First use could not be confirmed (answered with 503). Not a
         * verdict, and the caller may retry.
         *
         * @deprecated Not a verdict. Read {@link RedeemResult#getOutcome()},
         *             which is {@link Outcome#UNCONFIRMED}. Still read from
         *             {@code context}, which the cloud still writes.
         */
        @Deprecated
        UNCONFIRMED("unconfirmed"),
        /**
         * The service that checked the identifier could not complete the
         * check, and the reason is that service rather than the identifier.
         * It either compared nothing, or compared some factors and reports
         * at least one as {@link Factor#MISCONFIGURED}.
         *
         * <p>Nothing a caller sends can produce this. Against 51Degrees
         * public cloud it should not occur. Against a self-hosted service it
         * means that service is not reading the client's own connection, or
         * is missing an engine it needs, and its own logs name what to set.
         */
        MISCONFIGURED("misconfigured"),
        /**
         * The identifier's creation date is one the scheme could not have
         * produced, being in the future or before the creator context scheme
         * began. It says the identifier is fabricated rather than that
         * anything is wrong with the service.
         */
        INVALID_DATE("invaliddate");

        private final String value;

        Context(String value) {
            this.value = value;
        }

        /** @return the word the cloud uses for this verdict */
        public String getValue() {
            return value;
        }

        /**
         * Maps the cloud's word to a verdict. Anything unrecognised,
         * including a missing value, is {@link #UNREADABLE}, so an answer
         * this client does not know fails closed.
         *
         * @param value the {@code context} string from the cloud, or null
         * @return the matching verdict, or {@link #UNREADABLE}
         */
        public static Context fromValue(String value) {
            if (value != null) {
                for (Context context : values()) {
                    if (context.value.equals(value)) {
                        return context;
                    }
                }
            }
            return UNREADABLE;
        }
    }

    /** The signature outcome, reported by the cloud out of the seal. */
    public enum Signature {
        /** The identifier is a genuine 51Degrees identifier. */
        VERIFIED,
        /** The signature did not verify. */
        INVALID,
        /**
         * The cloud did not report the signature, which it only does on the
         * redeemed outcomes.
         */
        UNKNOWN;

        /**
         * Maps the cloud's word to an outcome. Absent is {@link #UNKNOWN},
         * {@code verified} is {@link #VERIFIED}, and any other word is
         * {@link #INVALID}, so a word this client does not know fails
         * closed.
         *
         * @param value the {@code signature} string from the cloud, or null
         * @return the matching outcome
         */
        public static Signature fromValue(String value) {
            if (value == null) {
                return UNKNOWN;
            }
            return "verified".equals(value) ? VERIFIED : INVALID;
        }
    }

    /** The outcome for one factor of the creator context. */
    public enum Factor {
        /** The factor matches creation. */
        VERIFIED("verified"),
        /** The factor differs from creation. */
        MISMATCH("mismatch"),
        /**
         * The service that checked the identifier is not configured to
         * determine this factor, so it could not have checked it for any
         * request. This is NOT a mismatch and must not be read as one, since
         * the identifier says nothing about it either way.
         */
        MISCONFIGURED("misconfigured"),
        /**
         * The service that created the identifier recorded no value for this
         * factor, so the identifier says nothing about it and there was
         * nothing to compare. This is neither a mismatch nor
         * {@link #MISCONFIGURED}, which says the checking service could not
         * determine the factor.
         */
        NOT_RECORDED("notrecorded");

        private final String value;

        Factor(String value) {
            this.value = value;
        }

        /** @return the word the cloud uses for this outcome */
        public String getValue() {
            return value;
        }

        /**
         * Verified, misconfigured and notrecorded are each read on their own,
         * because none of them is a mismatch. Anything else, including a word
         * this client does not know, is a mismatch, so an unexpected value
         * never reads as a pass. The comparison ignores case, so the word is
         * lower-cased in the root locale first.
         *
         * @param value the factor's string from the cloud, or null
         * @return the matching outcome, or {@link #MISMATCH}
         */
        public static Factor fromValue(String value) {
            if (value != null) {
                String word = value.toLowerCase(Locale.ROOT);
                for (Factor factor : values()) {
                    if (factor.value.equals(word)) {
                        return factor;
                    }
                }
            }
            return MISMATCH;
        }
    }

    /**
     * The factor names in the order the cloud reports them. Kept so that
     * {@link #getFactors()} iterates in that order whatever order the JSON
     * parser hands the keys back in.
     */
    private static final String[] FACTOR_ORDER = {
        "transport", "device", "browserip", "connectionip", "asn",
        "platformname", "platformversion", "browsername", "browserversion",
    };

    private final Outcome outcome;
    private final String statusValue;
    private final Context context;
    private final String contextValue;
    private final Signature signature;
    private final Map<String, Factor> factors;
    private final boolean hasFactors;
    private final HumanConfidence humanConfidence;
    private final String humanConfidenceValue;
    private final String humanConfidenceNullReason;
    private final Instant verifiedAt;
    private final Integer secondsSinceVerified;
    private final int statusCode;
    private final String raw;

    private RedeemResult(
            Outcome outcome,
            String statusValue,
            Context context,
            String contextValue,
            Signature signature,
            Map<String, Factor> factors,
            boolean hasFactors,
            HumanConfidence.Fields humanConfidenceFields,
            Instant verifiedAt,
            Integer secondsSinceVerified,
            int statusCode,
            String raw) {
        this.outcome = outcome;
        this.statusValue = statusValue;
        this.context = context;
        this.contextValue = contextValue;
        this.signature = signature;
        this.factors = factors;
        this.hasFactors = hasFactors;
        this.humanConfidence = humanConfidenceFields.value;
        this.humanConfidenceValue = humanConfidenceFields.raw;
        this.humanConfidenceNullReason = humanConfidenceFields.nullReason;
        this.verifiedAt = verifiedAt;
        this.secondsSinceVerified = secondsSinceVerified;
        this.statusCode = statusCode;
        this.raw = raw;
    }

    /**
     * Builds a result from the cloud's 200 or 503 answer. This is the one
     * place a 503 is handled. A 503 means first use could not be confirmed,
     * whatever the body says, so it is {@link Outcome#UNCONFIRMED} in both
     * the outcome and the context, and nothing is read from the body, which
     * may be empty, not JSON, or claim another outcome.
     *
     * @param statusCode the HTTP status, 200 or 503
     * @param json       the body parsed as a JSON object, which a 200 must
     *                   have, or null where it is empty or not JSON
     * @param raw        the body as received
     * @return the typed result
     */
    static RedeemResult fromResponse(
            int statusCode, JSONObject json, String raw) {
        if (statusCode == 503) {
            return unconfirmed(raw);
        }
        String contextValue = json.optString("context", null);
        Context context = Context.fromValue(contextValue);
        String statusValue = json.optString("status", null);
        Outcome outcome = statusValue == null
            ? outcomeFromContext(contextValue)
            : Outcome.fromValue(statusValue);
        Signature signature = Signature.fromValue(
            json.optString("signature", null));

        Map<String, Factor> factors = new LinkedHashMap<String, Factor>();
        JSONObject factorsJson = json.optJSONObject("factors");
        boolean hasFactors = factorsJson != null;
        if (hasFactors) {
            for (String name : FACTOR_ORDER) {
                if (factorsJson.has(name)) {
                    factors.put(name, Factor.fromValue(
                        factorsJson.optString(name, null)));
                }
            }
            for (String name : factorsJson.keySet()) {
                if (factors.containsKey(name) == false) {
                    factors.put(name, Factor.fromValue(
                        factorsJson.optString(name, null)));
                }
            }
        }

        Instant verifiedAt = null;
        String verifiedAtValue = json.optString("verifiedAt", null);
        if (verifiedAtValue != null) {
            try {
                verifiedAt = Instant.parse(verifiedAtValue);
            } catch (DateTimeParseException unreadable) {
                verifiedAt = null;
            }
        }
        Integer secondsSinceVerified = null;
        if (json.has("secondsSinceVerified")
                && json.isNull("secondsSinceVerified") == false) {
            secondsSinceVerified = json.optInt("secondsSinceVerified");
        }

        return new RedeemResult(
            outcome,
            statusValue,
            context,
            contextValue == null ? context.getValue() : contextValue,
            signature,
            Collections.unmodifiableMap(factors),
            hasFactors,
            HumanConfidence.read(json),
            verifiedAt,
            secondsSinceVerified,
            statusCode,
            raw);
    }

    /**
     * The result of a 503 answer: {@link Outcome#UNCONFIRMED}, with the
     * status and context words both {@code unconfirmed}, and nothing read
     * from the body. Only {@link #fromResponse} calls it.
     */
    private static RedeemResult unconfirmed(String raw) {
        @SuppressWarnings("deprecation")
        Context context = Context.UNCONFIRMED;
        return new RedeemResult(
            Outcome.UNCONFIRMED,
            Outcome.UNCONFIRMED.getValue(),
            context,
            context.getValue(),
            Signature.UNKNOWN,
            Collections.<String, Factor>emptyMap(),
            false,
            HumanConfidence.Fields.NONE,
            null,
            null,
            503,
            raw == null ? "" : raw);
    }

    /**
     * The outcome a cloud from before {@code status} gave in
     * {@code context}: one of the four failure words, or a verdict, which
     * it wrote only on success. Anything else, including no word at all,
     * is {@link Outcome#UNREADABLE}, so an answer this client does not know
     * fails closed.
     */
    private static Outcome outcomeFromContext(String contextValue) {
        if (contextValue == null) {
            return Outcome.UNREADABLE;
        }
        for (Outcome failure : Outcome.values()) {
            if (failure != Outcome.SUCCESS
                    && failure.getValue().equals(contextValue)) {
                return failure;
            }
        }
        for (Context verdict : Context.values()) {
            if (verdict.getValue().equals(contextValue)) {
                return Outcome.SUCCESS;
            }
        }
        return Outcome.UNREADABLE;
    }

    /**
     * What happened at redemption, read from {@code status}. Only
     * {@link Outcome#SUCCESS} carries an unsealed result. A word this
     * client does not know is {@link Outcome#UNREADABLE}. A cloud from
     * before {@code status} sent the outcome in {@code context} instead,
     * and it is read from there: a failure word gives that failure, a
     * verdict gives success, and anything else is unreadable.
     *
     * @return the outcome
     */
    public Outcome getOutcome() {
        return outcome;
    }

    /**
     * @return the {@code status} word exactly as the cloud sent it, so an
     *         unrecognised word (mapped to {@link Outcome#UNREADABLE}) is
     *         still visible, or null where the cloud sent none. On a 503
     *         it is {@code unconfirmed}, whatever the body says.
     */
    public String getStatusValue() {
        return statusValue;
    }

    /**
     * The creator context verdict on success. On any other outcome the
     * cloud writes the outcome's word to {@code context} too, and this is
     * then one of the obsolete failure members. Read
     * {@link #getOutcome()} for those.
     *
     * @return the creator context verdict
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return the {@code context} word exactly as the cloud sent it, so an
     *         unrecognised word (mapped to {@link Context#UNREADABLE}) is
     *         still visible, or the verdict's own word when the cloud sent
     *         none. On a 503 it is {@code unconfirmed}, whatever the body
     *         says.
     */
    public String getContextValue() {
        return contextValue;
    }

    /** @return the signature outcome */
    public Signature getSignature() {
        return signature;
    }

    /**
     * The per-factor outcomes, present only when the cloud sent
     * {@code factors}, which it does on the mismatch verdict, the one with
     * something to diagnose. Names are {@code transport}, {@code device},
     * {@code browserip}, {@code connectionip}, {@code asn},
     * {@code platformname}, {@code platformversion}, {@code browsername}
     * and {@code browserversion}, in that order.
     * <p>
     * The operating system and the browser are each two factors, a name and
     * a version. A version mismatch beside a verified name means the same
     * operating system or browser has been upgraded since creation, whilst
     * a mismatched name means a different one. Neither
     * {@link Factor#MISCONFIGURED} nor {@link Factor#NOT_RECORDED} is a
     * mismatch, and they say different things, because misconfigured means
     * the checking service could not determine the factor whilst not
     * recorded means the creating service recorded no value for it.
     * <p>
     * A name the cloud sends that is not in the list above is kept after
     * those, in the order the JSON parser gives it, so a factor added later
     * is still visible. Cloud releases before 4.4.38 sent a single
     * {@code browser} factor, which appears under that name and fills none
     * of the four that replaced it.
     *
     * @return factor name to outcome, read only, empty when the cloud sent
     *         none (see {@link #hasFactors()})
     */
    public Map<String, Factor> getFactors() {
        return factors;
    }

    /** @return whether the cloud sent {@code factors} */
    public boolean hasFactors() {
        return hasFactors;
    }

    /**
     * The Human Confidence Value the 51Did carries, part of the unsealed
     * result. The cloud returns it only on {@link Outcome#SUCCESS}, to a
     * call that presents a License Key valid for Human Confidence. A 51Did
     * made before the Value was added is {@link HumanConfidence#ABSENT}.
     *
     * @return the Value, or null where the cloud did not return one, in
     *         which case {@link #getHumanConfidenceNullReason()} says why on
     *         success
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
     * Why the cloud did not return the Human Confidence Value on success,
     * which is that the call did not present a License Key valid for Human
     * Confidence.
     *
     * @return the cloud's {@code humanConfidenceNullReason}, or null where
     *         it sent none
     */
    public String getHumanConfidenceNullReason() {
        return humanConfidenceNullReason;
    }

    /**
     * When the verify endpoint checked the context and sealed the result,
     * UTC to the second. Present on the redeemed and expired outcomes.
     *
     * @return the verification time, or null when the cloud did not send one
     */
    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    /**
     * How long before this redemption the verification happened, in whole
     * seconds by the cloud's clock. Present on the redeemed and expired
     * outcomes. A caller wanting a stricter freshness rule than the cloud's
     * own window applies it to this value.
     *
     * @return the age in seconds, or null when the cloud did not send one
     */
    public Integer getSecondsSinceVerified() {
        return secondsSinceVerified;
    }

    /** @return the HTTP status the cloud answered with, 200 or 503 */
    public int getStatusCode() {
        return statusCode;
    }

    /** @return the response body as received */
    public String getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return "RedeemResult " + statusCode + " outcome=" + outcome
            + " context=" + contextValue
            + " signature=" + signature
            + (hasFactors ? " factors=" + factors : "")
            + (humanConfidenceValue == null
                ? ""
                : " humanConfidence=" + humanConfidenceValue);
    }
}
