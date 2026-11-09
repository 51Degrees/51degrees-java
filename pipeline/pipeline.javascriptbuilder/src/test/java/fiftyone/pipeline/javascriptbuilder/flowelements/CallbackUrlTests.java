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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the callback URL against the cases every implementation of the
 * JavaScript builder shares, in the same order, so that each one refuses
 * and writes the same URLs.
 */
public class CallbackUrlTests {

    private static final String ENDPOINT = "/51dpipeline/json";

    private static CallbackUrl url(String protocol, String host) {
        return CallbackUrl.of(protocol, host, ENDPOINT);
    }

    private static void assertValid(String expected, CallbackUrl url,
        String given) {
        assertNotNull(url, given + " gave no URL");
        assertTrue(url.valid, given + " was refused as " + url.url);
        assertEquals(expected, url.url, given);
    }

    private static void assertInvalid(CallbackUrl url, String given) {
        assertNotNull(url, given + " gave no URL");
        assertFalse(url.valid, given + " was accepted as " + url.url);
    }

    @Test
    public void CallbackUrl_AddsASingleSlash() {
        assertValid(
            "https://example.com/51dpipeline/json",
            CallbackUrl.of("https", "example.com", "51dpipeline/json"),
            "an endpoint without a slash");
    }

    @Test
    public void CallbackUrl_KeepsASingleSlashOnTheEndpoint() {
        assertValid(
            "https://example.com/51dpipeline/json",
            CallbackUrl.of("https", "example.com", "/51dpipeline/json"),
            "an endpoint with a slash");
    }

    @Test
    public void CallbackUrl_CollapsesADoubleSlash() {
        assertValid(
            "http://example.com/json",
            CallbackUrl.of("http", "example.com/", "/json"),
            "a slash on both");
    }

    @Test
    public void CallbackUrl_MissingWhenAPartIsEmpty() {
        assertNull(CallbackUrl.of("https", "", "/json"));
        assertNull(CallbackUrl.of("", "example.com", "/json"));
        assertNull(CallbackUrl.of("https", "example.com", ""));
        assertNull(CallbackUrl.of("https", "  ", "/json"));
    }

    static Stream<Arguments> acceptedHosts() {
        return Stream.of(
            Arguments.of("EXAMPLE.com",
                "https://example.com/51dpipeline/json"),
            Arguments.of("example.com:8080",
                "https://example.com:8080/51dpipeline/json"),
            Arguments.of("example.com:65535",
                "https://example.com:65535/51dpipeline/json"),
            Arguments.of("a_b.com",
                "https://a_b.com/51dpipeline/json"),
            Arguments.of("-ab.com",
                "https://-ab.com/51dpipeline/json"),
            Arguments.of("127.0.0.1:80",
                "https://127.0.0.1:80/51dpipeline/json"),
            Arguments.of("example.com",
                "https://example.com/51dpipeline/json"),
            Arguments.of("1.2.3.4",
                "https://1.2.3.4/51dpipeline/json"),
            Arguments.of("1.2.3.4:8080",
                "https://1.2.3.4:8080/51dpipeline/json"),
            Arguments.of("1.2.3.4/path",
                "https://1.2.3.4/path/51dpipeline/json"),
            Arguments.of("0.0.0.0",
                "https://0.0.0.0/51dpipeline/json"),
            Arguments.of("101.2.3.4",
                "https://101.2.3.4/51dpipeline/json"),
            Arguments.of("example.co1",
                "https://example.co1/51dpipeline/json"),
            Arguments.of("123.example.com",
                "https://123.example.com/51dpipeline/json"),
            Arguments.of("[::1]",
                "https://[::1]/51dpipeline/json"),
            Arguments.of("[::1]:8080",
                "https://[::1]:8080/51dpipeline/json"),
            Arguments.of("[1:2:3:4:5:6:7:8]",
                "https://[1:2:3:4:5:6:7:8]/51dpipeline/json"),
            Arguments.of("[::1.2.3.4]",
                "https://[::1.2.3.4]/51dpipeline/json"),
            Arguments.of("example.com.",
                "https://example.com./51dpipeline/json"),
            Arguments.of("example.com/path",
                "https://example.com/path/51dpipeline/json"),
            Arguments.of("example.com/a b",
                "https://example.com/a%20b/51dpipeline/json"),
            Arguments.of("example.com/a\"b",
                "https://example.com/a%22b/51dpipeline/json"),
            Arguments.of("example.com/a%zzb",
                "https://example.com/a%25zzb/51dpipeline/json"),
            Arguments.of("example.com/a%41b",
                "https://example.com/a%41b/51dpipeline/json"),
            Arguments.of("example.com/\u00fc",
                "https://example.com/%C3%BC/51dpipeline/json"),
            Arguments.of("example.com/a\nb",
                "https://example.com/a%0Ab/51dpipeline/json"),
            Arguments.of("example.com/a\u2028b",
                "https://example.com/a%E2%80%A8b/51dpipeline/json"));
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("acceptedHosts")
    public void CallbackUrl_AcceptsHost(String host, String expected) {
        assertValid(expected, url("https", host), "host " + host);
    }

    static Stream<String> refusedHosts() {
        return Stream.of(
            "example.com:65536",
            "example.com:99999",
            "example.com:abc",
            "exa mple.com",
            "example.com');alert(1);//",
            "a\"b.com",
            "a'b.com",
            "a<b.com",
            "::1",
            "[::1",
            "[v1.x]",
            "[1.2.3.4]",
            "a@b@example.com",
            "a..b.com",
            "%41.com",
            "a\\b.com",
            "a`b.com",
            "a{b}.com",
            "a|b.com",
            "a^b.com",
            "a(b).com",
            "a;b.com",
            "a,b.com",
            "a!b.com",
            "a*b.com",
            "a+b.com",
            "a=b.com",
            "a$b.com",
            "a&b.com",
            "a~b.com",
            "a\tb.com",
            "a\nb.com",
            " example.com",
            "example.com ",
            "example.com\n",
            "example.com:80\n",
            "[::1]\n",
            "example.com:8080');alert(1);//",
            // User information, or an @ anywhere in the host value.
            "user@example.com",
            "user:pw@example.com",
            "@example.com",
            ":@example.com",
            "user@example.com:8080",
            "user@[::1]",
            "user@example.com/path",
            "u%41@example.com",
            "u x@example.com",
            "u\tx@example.com",
            "u\"x@example.com",
            "u'x@example.com",
            "u\\x@example.com",
            "u\nx@example.com",
            "u\u0001x@example.com",
            "example.com/a@b",
            "user:12/ss@example.com",
            "user:pa/ss@example.com",
            "user:pa?ss@example.com",
            "exa mple.com/a@b",
            // A query or fragment.
            "example.com?x",
            "example.com#x",
            "example.com?",
            "example.com#",
            "example.com:8080?x",
            // A port that is present but empty.
            "example.com:",
            "example.com:/path",
            "[::1]:",
            "https://example.com",
            "http://example.com",
            "[::1]#x",
            "example.com/path?x",
            "example.com/path#x",
            "example.com?');alert(1);//",
            "example.com#');alert(1);//",
            // A name whose last label is a number must be an IPv4 address.
            "example.123",
            "example.0x1f",
            "example.0X",
            "1.2.3.999",
            "1.2.3",
            "127.1",
            "0x7f.0.0.1",
            "01.2.3.4",
            "example.123.",
            "1.2.3.4.",
            "1.2.3.4.5",
            "1",
            "300.1.1.1",
            "0x7f.1");
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("refusedHosts")
    public void CallbackUrl_RefusesHost(String host) {
        assertInvalid(url("https", host), "host " + host);
    }

    static Stream<String> refusedZonesAndTextAfterTheBracket() {
        return Stream.of(
            "[fe80::1%25]",
            "[fe80::1%25eth0]",
            "[fe80::1%25eth0]:8080",
            "[FE80::1%25ETH0]",
            "[fe80::1%251]",
            "[::1%25lo]",
            "[fe80::1%]",
            "[fe80::1%25eth0]/path",
            "[fe80::1%25'+alert(1)+']",
            "[fe80::1%eth0]",
            "[fe80::1%25eth%0]",
            "[::1]x",
            "[::1]80",
            "[::1]:80x",
            "[::1]]",
            "[1::2::3]",
            "[12345::1]");
    }

    /**
     * Inside the brackets only an IPv6 address, so a zone identifier is
     * refused, and after them only a port.
     */
    @ParameterizedTest(name = "[{index}]")
    @MethodSource("refusedZonesAndTextAfterTheBracket")
    public void CallbackUrl_RefusesZoneOrTextAfterTheBracket(String host) {
        assertInvalid(url("https", host), "host " + host);
    }

    /**
     * A port goes by its value, so leading zeros are allowed, and a port too
     * long to read is refused rather than read as small enough.
     */
    @Test
    public void CallbackUrl_ChecksThePortByItsValue() {
        assertValid(
            "https://example.com:00080/51dpipeline/json",
            url("https", "example.com:00080"),
            "host example.com:00080");
        assertValid(
            "https://example.com:0065535/51dpipeline/json",
            url("https", "example.com:0065535"),
            "host example.com:0065535");
        for (String host : new String[] {
            "example.com:0065536",
            "example.com:123456",
            "example.com:4294967296",
            "example.com:99999999999999999999"}) {
            assertInvalid(url("https", host), "host " + host);
        }
    }

    /**
     * Each character rule 8 lists, after the host and in the endpoint, and a
     * '%' without two hexadecimal digits after it, even at the end.
     */
    @Test
    public void CallbackUrl_EncodesTheCharactersRule8Lists() {
        assertValid(
            "https://example.com/%22%5E%60%7B%7C%7D/j%22%5E%60%7B%7C%7D",
            CallbackUrl.of("https", "example.com/\"^`{|}", "/j\"^`{|}"),
            "rule 8 characters");
        assertValid(
            "https://example.com/a%25/51dpipeline/json",
            url("https", "example.com/a%"),
            "host example.com/a%");
        assertValid(
            "https://example.com/a%254/51dpipeline/json",
            url("https", "example.com/a%4"),
            "host example.com/a%4");
        assertValid(
            "https://example.com/a%41/51dpipeline/json",
            url("https", "example.com/a%41"),
            "host example.com/a%41");
        assertValid(
            "https://example.com/json%25",
            CallbackUrl.of("https", "example.com", "/json%"),
            "endpoint /json%");
    }

    static Stream<Arguments> refusedUrlsForTheLog() {
        return Stream.of(
            Arguments.of("user:secret@example.com",
                "https://***@example.com/51dpipeline/json"),
            Arguments.of("a@b@example.com",
                "https://***@example.com/51dpipeline/json"),
            Arguments.of("u@[::1]:80x",
                "https://***@[::1]:80x/51dpipeline/json"),
            Arguments.of("user:pa/ss@example.com",
                "https://***@example.com/51dpipeline/json"),
            Arguments.of("user:pa?ss@example.com",
                "https://***@example.com/51dpipeline/json"),
            Arguments.of("exa mple.com/a@b",
                "https://***@b/51dpipeline/json"),
            Arguments.of("@example.com",
                "https://***@example.com/51dpipeline/json"));
    }

    /**
     * The URL kept for the log shows *** in place of everything in the host
     * value before its last '@'.
     */
    @ParameterizedTest(name = "[{index}]")
    @MethodSource("refusedUrlsForTheLog")
    public void CallbackUrl_RefusedUrlForTheLogHasNoUserInformation(
        String host, String expected) {
        CallbackUrl url = url("https", host);
        assertInvalid(url, "host " + host);
        assertEquals(expected, url.url, "host " + host);
    }

    @Test
    public void CallbackUrl_InvalidUrlIsTheOneBuilt() {
        CallbackUrl url = url("https", "exa mple.com");
        assertInvalid(url, "host exa mple.com");
        assertEquals("https://exa mple.com/51dpipeline/json", url.url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https", "http", "HtTp"})
    public void CallbackUrl_AcceptsScheme(String protocol) {
        CallbackUrl url = url(protocol, "example.com");
        assertNotNull(url);
        assertTrue(url.valid, "protocol " + protocol);
    }

    @Test
    public void CallbackUrl_WritesTheSchemeInLowerCase() {
        assertValid(
            "https://example.com/51dpipeline/json",
            url("HTTPS", "example.com"),
            "protocol HTTPS");
    }

    /**
     * Only http and https, ignoring the case of ASCII letters only, so a
     * letter outside ASCII that a case-insensitive comparison can match to an
     * ASCII one, such as the long s, is refused.
     */
    @ParameterizedTest
    @ValueSource(strings = {"ftp", "h+t-p.s", "ws", "javascript", "httpx",
        "https ", "1http", "h t", "x');alert(1);//", "a://b", "http\u017f",
        "HTTP\u017f"})
    public void CallbackUrl_RefusesScheme(String protocol) {
        assertInvalid(url(protocol, "example.com"), "protocol " + protocol);
    }

    static Stream<String> hostsThatCouldEndTheString() {
        return Stream.of(
            "example.com/');alert(1);//",
            "example.com/a\"b",
            "example.com/a\\b",
            "example.com/</script>");
    }

    /**
     * A quote, a backslash or a line break never reaches the script, wherever
     * in the URL it was given.
     */
    @ParameterizedTest(name = "[{index}]")
    @MethodSource("hostsThatCouldEndTheString")
    public void CallbackUrl_NothingGivenCanEndTheString(String host) {
        CallbackUrl url = url("https", host);
        assertNotNull(url, "host " + host);
        assertTrue(url.valid, "host " + host + " was refused");
        for (char c : new char[] {
            '\'', '"', '\\', '<', '>', '\n', '\r', '\u2028', '\u2029'}) {
            assertTrue(url.url.indexOf(c) < 0,
                "host " + host + " gave " + url.url);
        }
    }

    @Test
    public void CallbackUrl_EncodesTheEndpoint() {
        assertValid(
            "https://example.com/json%27x",
            CallbackUrl.of("https", "example.com", "/json'x"),
            "the endpoint /json'x");
    }

    /**
     * A host must be ASCII, so an internationalized name is given as
     * A-labels, and anything else outside ASCII is refused.
     */
    @ParameterizedTest(name = "[{index}]")
    @ValueSource(strings = {
        "b\u00fccher.de", "a\u00a0b.com", "a\u200bb.com", "a\u2028b.com"})
    public void CallbackUrl_RefusesHostOutsideAscii(String host) {
        assertInvalid(url("https", host), "host " + host);
    }

    /**
     * Not one of the shared cases, because only some languages' strings can
     * hold a lone surrogate. It has no UTF-8 encoding, so it is written as
     * U+FFFD, as a browser writes it.
     */
    @Test
    public void CallbackUrl_WritesALoneSurrogateAsTheReplacement() {
        assertValid(
            "https://example.com/%EF%BF%BD/51dpipeline/json",
            url("https", "example.com/" + (char) 0xD800),
            "a lone surrogate");
    }
}
