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

package fiftyone.devicedetection.examples.shared;

import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FindOutMoreTest {

    /**
     * The campaign tags common-ci's scripts/utm-lint.ps1 requires of a link
     * printed by an example.
     */
    private static final Pattern TAGGED = Pattern.compile(
            "https://51degrees\\.com/[^?\\s]+\\?utm_source=code" +
            "&utm_medium=example&utm_campaign=51degrees-java" +
            "&utm_content=[a-z0-9][a-z0-9._-]*" +
            "&utm_term=[a-z0-9][a-z0-9._-]*");

    private static String[] write(String example, String... repositories) {
        StringWriter text = new StringWriter();
        FindOutMore.write(new PrintWriter(text), example, repositories);
        return text.toString().split("\r?\n");
    }

    /**
     * The section is a blank line, a rule, the heading, the three tagged
     * pages, the repositories and a closing rule.
     */
    @Test
    public void write_LinksThenRepositories() {
        String[] lines = write("console-example.java",
                FindOutMore.ON_PREMISE_REPOSITORIES);
        assertEquals(10, lines.length);
        assertEquals("", lines[0]);
        assertEquals(FindOutMore.RULE, lines[1]);
        assertEquals(FindOutMore.HEADING, lines[2]);
        assertTrue(lines[3].startsWith("\tDevice Detection: https://51degrees.com/device-detection?"));
        assertTrue(lines[4].startsWith("\tPricing: https://51degrees.com/pricing?"));
        assertTrue(lines[5].startsWith("\tDocumentation: https://51degrees.com/documentation/index.html?"));
        for (int i = 3; i <= 5; i++) {
            String link = lines[i].substring(lines[i].indexOf("https://"));
            assertTrue(link, TAGGED.matcher(link).matches());
            assertTrue(link, link.contains("&utm_content=console-example.java&"));
        }
        assertEquals("\thttps://github.com/51Degrees/51degrees-java", lines[6]);
        assertEquals("\thttps://github.com/51Degrees/51degrees-cxx", lines[7]);
        assertEquals("\thttps://github.com/51Degrees/data", lines[8]);
        assertEquals(FindOutMore.RULE, lines[9]);
    }

    /**
     * A cloud example lists this repository alone.
     */
    @Test
    public void write_Cloud_ThisRepository() {
        String[] lines = write("console-example.java",
                FindOutMore.CLOUD_REPOSITORIES);
        assertEquals(8, lines.length);
        assertEquals("\thttps://github.com/51Degrees/51degrees-java", lines[6]);
    }

    /**
     * Each line of the commented form starts with the comment marker, and
     * there is no blank line, so a YAML stream stays valid.
     */
    @Test
    public void writeCommented_EveryLineAComment() {
        StringWriter text = new StringWriter();
        FindOutMore.writeCommented(new PrintWriter(text), "# ",
                "console-example.java", FindOutMore.CLOUD_REPOSITORIES);
        String[] lines = text.toString().split("\\r?\\n");
        assertEquals(7, lines.length);
        for (String line : lines) {
            assertTrue(line, line.startsWith("# "));
        }
        assertEquals("# " + FindOutMore.HEADING, lines[1]);
    }

    /**
     * A tag that is not a lower case slug would fail the campaign tag
     * convention, so it is refused.
     */
    @Test
    public void link_NotASlug_Refused() {
        assertThrows(IllegalArgumentException.class,
                () -> FindOutMore.link("pricing", "Console-Example.java"));
        assertThrows(IllegalArgumentException.class,
                () -> FindOutMore.link("pricing", "console example"));
        assertThrows(IllegalArgumentException.class,
                () -> FindOutMore.link("pricing", null));
    }
}
