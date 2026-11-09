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

import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageElement.ReplacedString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.xml.stream.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the proprietary evidence string treatment class ...
 */
class ReplacedStringTest {

    // is transparent to the three control characters XML 1.0 allows
    @Test
    public void testNormal(){
        String aString = IntStream.of(0x9, 0xA, 0xD)
                .mapToObj(i -> Character.toString((char)i))
                .collect(joining());
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertFalse(replacedString.isReplaced());
        assertEquals(aString, replacedString.toString());
    }
    // is transparent to legal characters except \
    @Test
    public void testNormal2(){
        String aString =
                IntStream.range(0x20, 0x7f)
                        .mapToObj(i -> Character.toString((char)i))
                        .collect(joining());
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertFalse(replacedString.isReplaced());
        assertEquals(0x5f, replacedString.toString().length());
        assertEquals(aString, replacedString.toString());
    }
    // works with empty string
    @Test
    public void testEmpty(){
        String aString = "";
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertFalse(replacedString.isReplaced());
        assertEquals(0, replacedString.toString().length());
        assertEquals(aString, replacedString.toString());
    }
    // works with null string
    @Test
    public void testNull(){
        String aString = null;
        ReplacedString escapedString = new ReplacedString(aString);
        assertFalse(escapedString.isTruncated());
        assertFalse(escapedString.isReplaced());
        assertEquals(0, escapedString.toString().length());
        assertEquals("", escapedString.toString());
    }
    // truncates longer than 10,000 characters
    @Test
    public void testTruncate(){
        String aString = repeat("a", 10001);
        ReplacedString replacedString = new ReplacedString(aString);
        assertTrue(replacedString.isTruncated());
        assertFalse(replacedString.isReplaced());
        assertEquals(10000, replacedString.toString().length());
    }
    // keeps a value of exactly 10,000 characters whole
    @Test
    public void testNotTruncatedAtTheLimit(){
        String aString = repeat("a", 10000);
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertEquals(aString, replacedString.toString());
    }
    // the limit counts characters, so a character made of a pair of
    // surrogates at the limit is kept whole and the next one is cut
    @Test
    public void testTruncateNeverSplitsASurrogatePair(){
        String emoji = "\uD83D\uDE00";
        String aString = repeat("a", 9999) + emoji + "b";
        ReplacedString replacedString = new ReplacedString(aString);
        assertTrue(replacedString.isTruncated());
        assertFalse(replacedString.isReplaced());
        assertEquals(repeat("a", 9999) + emoji, replacedString.toString());
    }
    // a pair that would start after the limit is cut with the rest
    @Test
    public void testTruncateBeforeASurrogatePair(){
        String emoji = "\uD83D\uDE00";
        String aString = repeat("a", 10000) + emoji;
        ReplacedString replacedString = new ReplacedString(aString);
        assertTrue(replacedString.isTruncated());
        assertEquals(repeat("a", 10000), replacedString.toString());
    }
    // counts characters and not UTF-16 code units
    @Test
    public void testLimitCountsCharactersNotCodeUnits(){
        String emoji = "\uD83D\uDE00";
        ReplacedString atLimit = new ReplacedString(repeat(emoji, 10000));
        assertFalse(atLimit.isTruncated());
        assertEquals(20000, atLimit.toString().length());
        ReplacedString over = new ReplacedString(repeat(emoji, 10001));
        assertTrue(over.isTruncated());
        assertEquals(repeat(emoji, 10000), over.toString());
    }
    // the limit counts the value as supplied, so the escapes written for
    // invalid characters do not count towards it
    @Test
    public void testEscapesDoNotCountTowardsTheLimit(){
        String aString = "a" + (char) 0x1 + repeat("b", 9998);
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertTrue(replacedString.isReplaced());
        assertEquals(10000 - 1 + 6, replacedString.toString().length());
        // One character more is cut, whatever the escapes add.
        ReplacedString over = new ReplacedString(aString + "c");
        assertTrue(over.isTruncated());
        assertEquals(10000 - 1 + 6, over.toString().length());
    }
    private static String repeat(String text, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(text);
        }
        return builder.toString();
    }
    // escapes single digit Hex
    @Test
    public void testEscapedSmall(){
        String aString = IntStream.range(0,5).mapToObj(i -> "\u0001").collect(joining());
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertTrue(replacedString.isReplaced());
        assertEquals(repeat("\\x0001", 5), replacedString.toString());
    }
    // escapes 2 digit hex
    @Test
    public void testEscapedBigger(){
        String aString = IntStream.range(0,5).mapToObj(i -> "\u001F").collect(joining());
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertTrue(replacedString.isReplaced());
        assertEquals(repeat("\\x001f", 5), replacedString.toString());
    }
    // does not escape valid chars
    @Test
    public void testEscapedBiggerStill(){
        String aString = IntStream.range(0,5).mapToObj(i -> "\u0020").collect(joining());
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertFalse(replacedString.isReplaced());
        System.out.println(replacedString);
        assertEquals(5, replacedString.toString().length());
    }

    // does not replace characters above U+00FF, which XML 1.0 allows
    @Test
    public void testNonLatinKept(){
        String aString = "\u0100 \u0416 \u6F22\u5B57 \uD7FF \uE000 \uFFFD";
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isTruncated());
        assertFalse(replacedString.isReplaced());
        assertEquals(aString, replacedString.toString());
    }
    // does not replace the controls from U+007F to U+009F, which XML 1.0 allows
    @Test
    public void testC1ControlsKept(){
        String aString = IntStream.range(0x7F, 0xA0)
                .mapToObj(i -> Character.toString((char)i))
                .collect(joining());
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isReplaced());
        assertEquals(aString, replacedString.toString());
    }
    // keeps a pair of surrogates, which is one character above U+FFFF
    @Test
    public void testSurrogatePairKept(){
        String aString = "a\uD83D\uDE00b";
        ReplacedString replacedString = new ReplacedString(aString);
        assertFalse(replacedString.isReplaced());
        assertEquals(aString, replacedString.toString());
    }
    // writes a surrogate on its own, U+FFFE and U+FFFF as \x and four
    // lower-case hexadecimal digits
    @Test
    public void testForbiddenCharactersEscaped(){
        String[][] cases = {
            {"\uD83D", "\\xd83d"}, {"\uDE00", "\\xde00"},
            {"\uFFFE", "\\xfffe"}, {"\uFFFF", "\\xffff"},
            {"\u0000", "\\x0000"}, {"\u000B", "\\x000b"}};
        for (String[] c : cases) {
            ReplacedString replacedString = new ReplacedString("a" + c[0] + "b");
            assertTrue(replacedString.isReplaced());
            assertEquals("a" + c[1] + "b", replacedString.toString());
        }
        // A low surrogate before a high one is two surrogates on their own.
        ReplacedString reversed = new ReplacedString("\uDE00\uD83D");
        assertTrue(reversed.isReplaced());
        assertEquals("\\xde00\\xd83d", reversed.toString());
    }

    private final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
    private final XMLInputFactory xmlInputFactory =XMLInputFactory.newInstance();

    @Test
    public void testXmlSerialise() throws XMLStreamException {
        xmlOutputFactory.setProperty("escapeCharacters", true);
        String text = "\t\n a\u00E9\u0100\u0416\u6F22\u5B57\uD83D\uDE00" +
                "\u007F\u0080\u009F\uD7FF\uE000\uFFFD <&>";
        assertEquals(text, new ReplacedString(text).toString());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(os, "UTF-8");
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("test");
        writer.writeCharacters(text);
        writer.writeEndElement();
        writer.close();

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(is);
        reader.next();
        String readText = reader.getElementText();
        reader.close();

        assertEquals(readText, text);
    }
}