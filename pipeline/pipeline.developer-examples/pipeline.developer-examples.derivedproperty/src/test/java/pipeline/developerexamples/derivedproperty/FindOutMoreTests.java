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
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The example ends its output with a "Find out more" section, which lists
 * pages on 51degrees.com, each with its campaign tags, and the
 * repositories.
 */
public class FindOutMoreTests {

    private static final String RULE =
        "----------------------------------------------------------------";

    private static final String HEADING = "Find out more";

    private static final String[] REPOSITORIES = {
        "https://github.com/51Degrees/51degrees-java",
        "https://github.com/51Degrees/derived-properties",
        "https://github.com/51Degrees/specifications"};

    private static final Pattern TAGGED = Pattern.compile(
        "https://51degrees\\.com/[^?\\s]*\\?utm_source=code"
        + "&utm_medium=example&utm_campaign=51degrees-java"
        + "&utm_content=[a-z0-9][a-z0-9._-]*&utm_term=find-out-more$");

    @Test
    public void Main_EndsWithFindOutMore() throws Exception {
        String output = runMain();
        int example = output.lastIndexOf("derived.ReaderEngagement");
        int heading = output.lastIndexOf(RULE + "\n" + HEADING + "\n");
        assertTrue(example >= 0, "The example wrote nothing:\n" + output);
        assertTrue(heading > example,
            "No Find out more section after the example's output:\n"
            + output);
        String section = output.substring(
            heading + RULE.length() + HEADING.length() + 2);
        int end = section.indexOf(RULE + "\n");
        assertTrue(end > 0, "The section has no closing rule:\n" + output);
        section = section.substring(0, end);
        int pages = 0;
        for (String line : section.split("\n")) {
            if (line.contains("51degrees.com")) {
                assertTrue(TAGGED.matcher(line).find(),
                    "A 51degrees.com link without its campaign tags: "
                    + line);
                pages++;
            }
        }
        assertEquals(2, pages, "The pages listed:\n" + section);
        for (String repository : REPOSITORIES) {
            assertTrue(section.contains(repository),
                repository + " is not listed:\n" + section);
        }
    }

    /**
     * Runs the example as a person would and returns what it wrote, with
     * each line ending in a line feed.
     */
    private static String runMain() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, "UTF-8"));
        try {
            Main.main(new String[0]);
        } finally {
            System.setOut(original);
        }
        return new String(captured.toByteArray(), StandardCharsets.UTF_8)
            .replace("\r\n", "\n");
    }
}
