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

import java.util.Locale;

/**
 * The Human Confidence Value a 51Did carries, as the cloud's 51Did
 * endpoints return it in {@code humanConfidence}: how confident 51Degrees
 * was, when the 51Did was made, that the device was being used by a human.
 * <p>
 * The cloud returns the Value only to a call that presents a License Key
 * valid for Human Confidence. Any other call gets
 * {@code humanConfidenceNullReason} in its place, saying why. A cloud from
 * before the Value was added sends neither, and every 51Did made before then
 * reads as {@link #ABSENT}.
 */
public enum HumanConfidence {

    /** High confidence that a human was using the device. */
    HIGH("High"),

    /** Medium confidence that a human was using the device. */
    MEDIUM("Medium"),

    /** Low confidence that a human was using the device. */
    LOW("Low"),

    /**
     * The 51Did carries no Value, because Human Confidence was not available
     * when it was made.
     */
    ABSENT("Absent");

    private final String value;

    HumanConfidence(String value) {
        this.value = value;
    }

    /** @return the word the cloud uses for this Value */
    public String getValue() {
        return value;
    }

    /**
     * Maps the cloud's word to a Value. The comparison ignores case. A word
     * this client does not know is {@link #ABSENT}, so an answer this client
     * does not understand never reads as confidence it does not give.
     *
     * @param value the {@code humanConfidence} string from the cloud, or
     *              null
     * @return the matching Value, or null where the cloud sent none
     */
    public static HumanConfidence fromValue(String value) {
        if (value == null) {
            return null;
        }
        String word = value.toLowerCase(Locale.ROOT);
        for (HumanConfidence confidence : values()) {
            if (confidence.value.toLowerCase(Locale.ROOT).equals(word)) {
                return confidence;
            }
        }
        return ABSENT;
    }

    /**
     * Reads the Human Confidence fields of a verify or redeem answer: the
     * Value, the word exactly as sent and the reason the Value is missing.
     * Both answers read them here, so they always read the same way.
     *
     * @param json the answer body as JSON, or null where nothing should be
     *             read from it
     * @return the fields, each null where the answer did not send it
     */
    static Fields read(JSONObject json) {
        if (json == null) {
            return Fields.NONE;
        }
        return new Fields(
            json.optString("humanConfidence", null),
            json.optString("humanConfidenceNullReason", null));
    }

    /**
     * The Human Confidence fields of one verify or redeem answer, as
     * {@link #read(JSONObject)} gives them.
     */
    static final class Fields {

        /** No fields at all, for an answer nothing is read from. */
        static final Fields NONE = new Fields(null, null);

        /** The Value, or null where the answer sent none. */
        final HumanConfidence value;

        /** The {@code humanConfidence} word as sent, or null. */
        final String raw;

        /** The {@code humanConfidenceNullReason} as sent, or null. */
        final String nullReason;

        private Fields(String raw, String nullReason) {
            this.value = fromValue(raw);
            this.raw = raw;
            this.nullReason = nullReason;
        }
    }
}
