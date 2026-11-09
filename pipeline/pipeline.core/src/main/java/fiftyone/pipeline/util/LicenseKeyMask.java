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

package fiftyone.pipeline.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Masks license keys in text written to a log entry or an error message. A
 * masked key shows only its last 4 characters, with every character before
 * them replaced by '*', so a reader can tell two keys apart but cannot use
 * either. A key of 4 characters or fewer is replaced in full, because its
 * last 4 characters would be the whole key.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/logging.md#license-keys">Specification</a>
 */
public final class LicenseKeyMask {

    /**
     * How many characters at the end of a key stay visible.
     */
    public static final int VISIBLE_CHARACTERS = 4;

    /**
     * The character that replaces each hidden character of a key.
     */
    public static final char MASK_CHARACTER = '*';

    private LicenseKeyMask() {
    }

    /**
     * The key with every character before its last 4 replaced.
     * @param key a license key, or null
     * @return the masked key, or null if the key is null
     */
    public static String mask(String key) {
        if (key == null) {
            return null;
        }
        int hidden = key.length() > VISIBLE_CHARACTERS ?
            key.length() - VISIBLE_CHARACTERS :
            key.length();
        StringBuilder masked = new StringBuilder(key.length());
        for (int i = 0; i < hidden; i++) {
            masked.append(MASK_CHARACTER);
        }
        return masked.append(key, hidden, key.length()).toString();
    }

    /**
     * The text with every occurrence of each key masked, both as the key is
     * written and as it is encoded in a URL.
     * @param text the text, such as a URL or an error message, or null
     * @param keys the license keys, or null
     * @return the masked text, or null if the text is null
     */
    public static String maskIn(String text, Iterable<String> keys) {
        if (text == null || keys == null) {
            return text;
        }
        String result = text;
        for (String key : longestFirst(keys)) {
            result = result.replace(key, mask(key));
            String encoded = urlEncode(key);
            if (encoded.equals(key) == false) {
                result = result.replace(encoded, mask(encoded));
            }
        }
        return result;
    }

    /**
     * The throwable to log or report in place of the one given, so that no
     * message in it, its causes or its suppressed throwables holds a key.
     * When none does, the throwable given is returned. Otherwise a copy is
     * returned whose messages are masked, each starting with the name of the
     * class it stands for, and whose stack traces are the originals.
     * @param throwable the throwable, or null
     * @param keys the license keys, or null
     * @return a throwable that holds no key
     */
    public static Throwable maskIn(Throwable throwable, Iterable<String> keys) {
        if (throwable == null ||
            keys == null ||
            holdsKey(throwable, keys, new IdentityHashMap<Throwable, Boolean>())
                == false) {
            return throwable;
        }
        return copy(throwable, keys, new IdentityHashMap<Throwable, Throwable>());
    }

    private static boolean holdsKey(
        Throwable throwable,
        Iterable<String> keys,
        Map<Throwable, Boolean> seen) {
        if (throwable == null || seen.put(throwable, Boolean.TRUE) != null) {
            return false;
        }
        String message = throwable.getMessage();
        if (message != null && message.equals(maskIn(message, keys)) == false) {
            return true;
        }
        if (holdsKey(throwable.getCause(), keys, seen)) {
            return true;
        }
        for (Throwable suppressed : throwable.getSuppressed()) {
            if (holdsKey(suppressed, keys, seen)) {
                return true;
            }
        }
        return false;
    }

    private static Throwable copy(
        Throwable throwable,
        Iterable<String> keys,
        Map<Throwable, Throwable> copies) {
        Throwable existing = copies.get(throwable);
        if (existing != null) {
            return existing;
        }
        String message = maskIn(throwable.getMessage(), keys);
        MaskedThrowable masked = new MaskedThrowable(
            throwable.getClass().getName() +
                (message == null ? "" : ": " + message));
        copies.put(throwable, masked);
        masked.setStackTrace(throwable.getStackTrace());
        Throwable cause = throwable.getCause();
        if (cause != null) {
            Throwable maskedCause = copy(cause, keys, copies);
            if (maskedCause != masked) {
                masked.initCause(maskedCause);
            }
        }
        for (Throwable suppressed : throwable.getSuppressed()) {
            Throwable maskedSuppressed = copy(suppressed, keys, copies);
            if (maskedSuppressed != masked) {
                masked.addSuppressed(maskedSuppressed);
            }
        }
        return masked;
    }

    /**
     * The keys that are not null or empty, longest first, so that a key
     * that holds another is masked whole.
     */
    private static List<String> longestFirst(Iterable<String> keys) {
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            if (key != null && key.isEmpty() == false) {
                result.add(key);
            }
        }
        Collections.sort(result, (a, b) -> b.length() - a.length());
        return result;
    }

    private static String urlEncode(String key) {
        try {
            return URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Every Java platform supports UTF-8.
            throw new IllegalStateException(e);
        }
    }

    /**
     * Stands for a throwable whose message held a license key. Its message
     * starts with the name of the class it stands for, and it prints as
     * that message alone.
     */
    private static final class MaskedThrowable extends Exception {
        private static final long serialVersionUID = 1L;

        MaskedThrowable(String message) {
            super(message);
        }

        @Override
        public String toString() {
            return getMessage();
        }
    }
}
