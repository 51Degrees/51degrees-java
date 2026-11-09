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

package fiftyone.pipeline.javascriptbuilder.flowelements;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The callback URL the client script posts to for updated JSON. The protocol
 * and host usually come from the request, and the script holds the URL in a
 * single-quoted string, so the URL is checked before it is written into the
 * script. A URL that is not valid, including one whose protocol is not http
 * or https, is refused. In anything after the host and port, and in the
 * endpoint, the characters a URL cannot hold or that could end the string or
 * the script are percent-encoded. The rules are applied to the text given,
 * and not to the result of parsing it, because a parser can accept hosts the
 * rules refuse and rewrite others, such as 127.1.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/javascript-builder.md#callback-url">Specification</a>
 */
final class CallbackUrl {

    /**
     * The largest port number.
     */
    private static final int MAX_PORT = 65535;

    /**
     * The most digits a port can have once its leading zeros are dropped.
     */
    private static final int MAX_PORT_DIGITS = 5;

    /**
     * Written in place of a lone surrogate, which has no UTF-8 encoding, as a
     * browser writes it.
     */
    private static final int REPLACEMENT_CHARACTER = 0xFFFD;

    /**
     * The printable ASCII characters that are percent-encoded, besides the
     * control characters, the space and everything outside ASCII.
     */
    private static final String ENCODED = "\"'<>\\^`{|}";

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    /**
     * Text made only of the Unicode White_Space characters, which counts as
     * a part that is not set.
     */
    private static final Pattern BLANK = Pattern.compile(
        "[\\t\\n\\x0B\\f\\r \\u0085\\u00A0\\u1680\\u2000-\\u200A" +
        "\\u2028\\u2029\\u202F\\u205F\\u3000]*");

    /**
     * The schemes the script can send its request with. It uses the URL only
     * with fetch and XMLHttpRequest, which reach a server with no other
     * scheme.
     */
    private static final String[] SCHEMES = {"http", "https"};

    /**
     * The characters the host value cannot hold. A '?' or '#' would make the
     * endpoint part of the query or fragment, and an '@' means user
     * information, which fetch refuses and XMLHttpRequest sends.
     */
    private static final String NOT_IN_HOST = "?#@";

    /**
     * A label of a host name, being ASCII letters, digits, '-' and '_'.
     */
    private static final Pattern LABEL = Pattern.compile("[A-Za-z0-9_-]+");

    /**
     * A port as it follows the host, being ':' and any number of digits.
     */
    private static final Pattern PORT = Pattern.compile(":[0-9]*");

    /**
     * One group of an IPv6 address.
     */
    private static final Pattern HEX_GROUP =
        Pattern.compile("[0-9A-Fa-f]{1,4}");

    /**
     * An IPv4 address as RFC 3986 section 3.2.2 defines one, being four
     * numbers from 0 to 255 with no leading zeros.
     */
    private static final Pattern IPV4 = Pattern.compile(
        "(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])" +
        "(\\.(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])){3}");

    /**
     * A label that the URL Standard reads as a number, being decimal digits
     * or "0x" followed by hexadecimal digits.
     */
    private static final Pattern NUMBER =
        Pattern.compile("[0-9]+|0[xX][0-9A-Fa-f]*");

    /**
     * Whether the URL can be written into the script.
     */
    final boolean valid;

    /**
     * The URL to write into the script when it is valid, and otherwise the
     * URL as it was built, for the log, with *** in place of everything in
     * the host value before its last '@'.
     */
    final String url;

    private CallbackUrl(boolean valid, String url) {
        this.valid = valid;
        this.url = url;
    }

    /**
     * Builds the callback URL from its parts and checks it. One slash
     * separates the host and the endpoint, being added when neither has one
     * and removed when both do. The protocol and the host are written in
     * lower case and the rest as given, apart from the percent-encoding.
     * @param protocol the protocol, http or https in any case
     * @param host the host, which can end with a port and a path
     * @param endpoint the endpoint
     * @return the callback URL, or null when the protocol, host or endpoint
     * is not set
     */
    static CallbackUrl of(String protocol, String host, String endpoint) {
        if (isBlank(protocol) || isBlank(host) || isBlank(endpoint)) {
            return null;
        }
        boolean hostSlash = host.endsWith("/");
        boolean endpointSlash = endpoint.startsWith("/");
        if (hostSlash == false && endpointSlash == false) {
            endpoint = "/" + endpoint;
        } else if (hostSlash && endpointSlash) {
            endpoint = endpoint.substring(1);
        }
        int end = authorityEnd(host);
        String authority = isScheme(protocol) && hasNoForbiddenCharacter(host)
            ? checkedHostPort(host.substring(0, end))
            : null;
        if (authority == null) {
            return new CallbackUrl(
                false,
                protocol + "://" + withoutUserInformation(host) + endpoint);
        }
        return new CallbackUrl(
            true,
            asciiLowerCase(protocol) + "://" + authority +
                encoded(host.substring(end)) + encoded(endpoint));
    }

    /**
     * Whether the host value holds none of '?', '#' and '@'.
     */
    private static boolean hasNoForbiddenCharacter(String host) {
        for (int i = 0; i < host.length(); i++) {
            if (NOT_IN_HOST.indexOf(host.charAt(i)) >= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * The host value with everything before its last '@' replaced by ***.
     * The last '@' of the whole value is used, and not that of the
     * authority, because a password can hold a '/', '?' or '#'.
     */
    private static String withoutUserInformation(String host) {
        int at = host.lastIndexOf('@');
        return at < 0 ? host : "***" + host.substring(at);
    }

    /**
     * Whether the protocol is http or https, ignoring the case of its ASCII
     * letters only. String.equalsIgnoreCase also matches a letter outside
     * ASCII to an ASCII one, such as the long s (U+017F) to 's', so it is
     * not used.
     * @param protocol the protocol
     * @return true if the protocol is http or https
     */
    static boolean isScheme(String protocol) {
        if (protocol == null) {
            return false;
        }
        String lower = asciiLowerCase(protocol);
        for (String scheme : SCHEMES) {
            if (scheme.equals(lower)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The text with the ASCII letters A to Z in lower case and every other
     * character as it is.
     * @param text the text
     * @return the text with A to Z in lower case
     */
    private static String asciiLowerCase(String text) {
        StringBuilder lower = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            lower.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
        }
        return lower.toString();
    }

    /**
     * Whether a part of the URL is not set, being null, empty or only
     * whitespace.
     * @param text the part
     * @return true if the part is not set
     */
    static boolean isBlank(String text) {
        return text == null || BLANK.matcher(text).matches();
    }

    /**
     * Where the authority ends, which is at the first '/' of the host, or at
     * its end.
     */
    private static int authorityEnd(String host) {
        int end = host.indexOf('/');
        return end < 0 ? host.length() : end;
    }

    /**
     * The host and optional port in lower case, or null when they are not
     * valid. The host is an IPv6 address in brackets, or a name of labels
     * separated by dots with a dot allowed at the end, which must be an IPv4
     * address when its last label is a number. The port is ':' followed by
     * digits up to 65535, or by nothing.
     */
    private static String checkedHostPort(String hostPort) {
        String host;
        String port;
        if (hostPort.startsWith("[")) {
            int close = hostPort.indexOf(']');
            if (close < 0) {
                return null;
            }
            String address = hostPort.substring(1, close);
            if (isIpv6(address) == false) {
                return null;
            }
            host = hostPort.substring(0, close + 1);
            port = hostPort.substring(close + 1);
        } else {
            int colon = hostPort.indexOf(':');
            int end = colon < 0 ? hostPort.length() : colon;
            host = hostPort.substring(0, end);
            port = hostPort.substring(end);
            String name = host.endsWith(".")
                ? host.substring(0, host.length() - 1)
                : host;
            String[] labels = name.split("\\.", -1);
            for (String label : labels) {
                if (LABEL.matcher(label).matches() == false) {
                    return null;
                }
            }
            if (NUMBER.matcher(labels[labels.length - 1]).matches() &&
                IPV4.matcher(host).matches() == false) {
                return null;
            }
        }
        if (port.isEmpty() == false && isPort(port) == false) {
            return null;
        }
        return host.toLowerCase(Locale.ROOT) + port;
    }

    /**
     * Whether the text is ':' followed by digits whose value is no more than
     * 65535, or by nothing.
     */
    private static boolean isPort(String text) {
        if (PORT.matcher(text).matches() == false) {
            return false;
        }
        String digits = text.substring(1).replaceFirst("^0+", "");
        return digits.length() <= MAX_PORT_DIGITS &&
            (digits.isEmpty() || Integer.parseInt(digits) <= MAX_PORT);
    }

    /**
     * Whether the text is an IPv6 address as RFC 3986 section 3.2.2 defines
     * one. That is eight groups of one to four hexadecimal digits separated
     * by ':', where one '::' can stand for one or more groups of zeros and
     * the last two groups can be written as an IPv4 address.
     */
    private static boolean isIpv6(String text) {
        int gap = text.indexOf("::");
        if (gap < 0) {
            return groups(text, true) == 8;
        }
        if (text.indexOf("::", gap + 1) >= 0) {
            return false;
        }
        int head = groups(text.substring(0, gap), false);
        int tail = groups(text.substring(gap + 2), true);
        return head >= 0 && tail >= 0 && head + tail <= 7;
    }

    /**
     * The number of 16 bit groups in a run of groups separated by ':', or -1
     * when the run is not valid. An empty run has none, and an IPv4 address
     * at the end of the run, where one is allowed, counts as two.
     */
    private static int groups(String run, boolean ipv4AtEnd) {
        if (run.isEmpty()) {
            return 0;
        }
        String[] parts = run.split(":", -1);
        int count = 0;
        for (int i = 0; i < parts.length; i++) {
            if (HEX_GROUP.matcher(parts[i]).matches()) {
                count++;
            } else if (ipv4AtEnd && i == parts.length - 1 &&
                IPV4.matcher(parts[i]).matches()) {
                count += 2;
            } else {
                return -1;
            }
        }
        return count;
    }

    /**
     * The text with each byte of its UTF-8 encoding percent-encoded where
     * the byte is a control character, a space, outside ASCII or one of
     * ENCODED, and with a '%' encoded where two hexadecimal digits do not
     * follow it.
     */
    private static String encoded(String text) {
        byte[] bytes = utf8(text);
        StringBuilder out = new StringBuilder(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            boolean keep = b == '%'
                ? i + 2 < bytes.length &&
                    isHexDigit(bytes[i + 1]) && isHexDigit(bytes[i + 2])
                : b > ' ' && b < 0x7F && ENCODED.indexOf(b) < 0;
            if (keep) {
                out.append((char) b);
            } else {
                out.append('%')
                    .append(HEX_DIGITS[b >> 4])
                    .append(HEX_DIGITS[b & 0x0F]);
            }
        }
        return out.toString();
    }

    private static boolean isHexDigit(byte b) {
        return (b >= '0' && b <= '9') ||
            (b >= 'A' && b <= 'F') ||
            (b >= 'a' && b <= 'f');
    }

    /**
     * The UTF-8 encoding of the text, with each lone surrogate written as
     * U+FFFD.
     */
    private static byte[] utf8(String text) {
        StringBuilder whole = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int c = text.codePointAt(i);
            i += Character.charCount(c);
            whole.appendCodePoint(
                c >= Character.MIN_SURROGATE && c <= Character.MAX_SURROGATE
                    ? REPLACEMENT_CHARACTER
                    : c);
        }
        return whole.toString().getBytes(StandardCharsets.UTF_8);
    }
}
