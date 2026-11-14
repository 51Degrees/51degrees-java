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

package pipeline.developerexamples.derivedproperty;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the example and checks it shows the four things the specification
 * asks of it, being a script held as text, a script read from a file, the
 * same Pipeline built from a configuration file, and a property with no
 * value naming the source property that was missing.
 */
public class ExampleTests {

    private static final String NO_VALUE =
        "no value, because Derived property '%s' has no value because 1 " +
            "source property was not available. 'session.PointerMoved' " +
            "(element 'session' has no value for 'PointerMoved': the " +
            "request did not carry the evidence 'query.pointermoved').";

    private static String run() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, "UTF-8")) {
            new Main.Example().run(output);
        }
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8)
            .replace("\r\n", "\n");
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int at = text.indexOf(part); at >= 0;
             at = text.indexOf(part, at + 1)) {
            count++;
        }
        return count;
    }

    @Test
    public void Example_ShowsTheFourThings() throws Exception {
        String output = run();
        int code = output.indexOf("1. Configured with the builder in code");
        int file = output.indexOf("2. The same Pipeline built from " +
            "derived-property.xml");
        assertTrue(code >= 0 && file > code, output);
        String fromCode = output.substring(code, file);
        String fromFile = output.substring(file);

        // The script held as text and the script read from a file each
        // give a value from the source properties.
        assertTrue(fromCode.contains(
            "derived.ReaderEngagementIsHigh    true"), fromCode);
        assertTrue(fromCode.contains(
            "derived.ReaderEngagement          High"), fromCode);
        // The configuration file builds the same Pipeline with the file.
        assertTrue(fromFile.contains(
            "derived.ReaderEngagement          High"), fromFile);
        assertTrue(fromFile.contains("The definition the script " +
            "'ReaderEngagement' version 1.0.0 carries."), fromFile);
        // A source property that is not available leaves no value, and
        // the message names it.
        assertEquals(1, count(fromCode,
            String.format(NO_VALUE, "ReaderEngagementIsHigh")), fromCode);
        assertEquals(1, count(fromCode,
            String.format(NO_VALUE, "ReaderEngagement")), fromCode);
        assertEquals(1, count(fromFile,
            String.format(NO_VALUE, "ReaderEngagement")), fromFile);
    }
}
