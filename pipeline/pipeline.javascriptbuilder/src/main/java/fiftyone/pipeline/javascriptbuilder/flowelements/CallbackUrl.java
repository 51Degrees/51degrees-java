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
 * script. A URL that is not valid is refused. In the user information, in
 * anything after the host and port, and in the endpoint, the characters a
 * URL cannot hold or that could end the string or the script are
 * percent-encoded.
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
     * A scheme as RFC 3986 section 3.1 defines one, being a letter followed
     * by letters, digits, '+', '-' and '.'.
     */
    private static final Pattern SCHEME =
        Pattern.compile("[A-Za-z][A-Za-z0-9+.-]*");

    /**
     * A label of a host name, being ASCII letters, digits, '-' and '_'.
     */
    private static final Pattern LABEL = Pattern.compile("[A-Za-z0-9_-]+");

    /**
     * An IPv6 zone identifier, being unreserved characters as RFC 3986
     * section 2.3 defines them.
     */
    private static final Pattern ZONE = Pattern.compile("[A-Za-z0-9._~-]+");

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
     * Whether the URL can be written into the script.
     */
    final boolean valid;

    /**
     * The URL to write into the script when it is valid, and otherwise the
     * URL as it was built, for the log.
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
     * @param protocol the protocol, such as https
     * @param host the host, which can start with user information and end
     *             with a port, a path, a query or a fragment
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
        String built = protocol + "://" + host + endpoint;
        if (SCHEME.matcher(protocol).matches() == false) {
            return new CallbackUrl(false, built);
        }
        int end = authorityEnd(host);
        String authority = checkedAuthority(host.substring(0, end));
        if (authority == null) {
            return new CallbackUrl(false, built);
        }
        return new CallbackUrl(
            true,
            protocol.toLowerCase(Locale.ROOT) + "://" + authority +
                encoded(host.substring(end)) + encoded(endpoint));
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
     * Where the authority ends, which is at the first '/', '?' or '#' of the
     * host, or at its end.
     */
    private static int authorityEnd(String host) {
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                return i;
            }
        }
        return host.length();
    }

    /**
     * The user information, host and port as they are written into the URL,
     * or null when they are not valid. The user information ends at the
     * first '@', so a second '@' is part of the host and refused.
     */
    private static String checkedAuthority(String authority) {
        int at = authority.indexOf('@');
        if (at < 0) {
            return checkedHostPort(authority);
        }
        String hostPort = checkedHostPort(authority.substring(at + 1));
        return hostPort == null
            ? null
            : encoded(authority.substring(0, at)) + "@" + hostPort;
    }

    /**
     * The host and optional port in lower case, or null when they are not
     * valid. The host is an IPv6 address in brackets with an optional zone
     * identifier, or a name of labels separated by dots with a dot allowed
     * at the end. The port is ':' followed by digits up to 65535, or by
     * nothing.
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
            int zone = address.indexOf("%25");
            if (zone >= 0) {
                if (ZONE.matcher(address.substring(zone + 3)).matches() ==
                    false) {
                    return null;
                }
                address = address.substring(0, zone);
            }
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
            for (String label : name.split("\\.", -1)) {
                if (LABEL.matcher(label).matches() == false) {
                    return null;
                }
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
