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

package fiftyone.ipintelligence.examples.shared;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.regex.Pattern;

/**
 * Writes the "Find out more" section that ends every example's output, set
 * apart from the example's own output by a rule above and below. It gives
 * pages on 51degrees.com, each tagged with the example that wrote it, then
 * the repositories behind the example.
 */
public final class FindOutMore {

    /**
     * The heading the section starts with.
     */
    public static final String HEADING = "Find out more";

    /**
     * The rule above and below the section.
     */
    public static final String RULE =
            "----------------------------------------------------------------";

    /**
     * The repositories an on-premise example lists, being this SDK, the C
     * engines it builds on and the free data files.
     */
    public static final String[] ON_PREMISE_REPOSITORIES = {
            "https://github.com/51Degrees/51degrees-java",
            "https://github.com/51Degrees/51degrees-cxx",
            "https://github.com/51Degrees/data"
    };

    /**
     * The repositories a cloud example lists.
     */
    public static final String[] CLOUD_REPOSITORIES = {
            "https://github.com/51Degrees/51degrees-java"
    };

    /**
     * Each page's label and its path on 51degrees.com.
     */
    private static final String[][] PAGES = {
            {"IP Intelligence", "ip-intelligence"},
            {"Pricing", "pricing"},
            {"Documentation", "documentation/index.html"}
    };

    /**
     * What a campaign tag value may hold.
     */
    private static final Pattern SLUG =
            Pattern.compile("[a-z0-9][a-z0-9._-]*");

    private FindOutMore() {
    }

    /**
     * Writes the section.
     * @param output where to write, flushed afterwards
     * @param example the example's tag, a lower case slug such as
     *                "console-gettingstartedonprem.java"
     * @param repositories the repositories to list, such as
     *                     {@link #ON_PREMISE_REPOSITORIES}
     */
    public static void write(
            PrintWriter output,
            String example,
            String... repositories) {
        output.println();
        writeLines(output, "", example, repositories);
    }

    /**
     * Writes the section with each line a comment, for output that has to
     * stay valid in its own format, such as a YAML stream.
     * @param output where to write, flushed afterwards
     * @param comment what starts a comment line, such as "# "
     * @param example the example's tag
     * @param repositories the repositories to list
     */
    public static void writeCommented(
            PrintWriter output,
            String comment,
            String example,
            String... repositories) {
        writeLines(output, comment, example, repositories);
    }

    private static void writeLines(
            PrintWriter output,
            String start,
            String example,
            String... repositories) {
        output.println(start + RULE);
        output.println(start + HEADING);
        for (String[] page : PAGES) {
            output.println(start + "\t" + page[0] + ": " +
                    link(page[1], example));
        }
        for (String repository : repositories) {
            output.println(start + "\t" + repository);
        }
        output.println(start + RULE);
        output.flush();
    }

    /**
     * Writes the section to a stream. See
     * {@link #write(PrintWriter, String, String...)}.
     */
    public static void write(
            OutputStream output,
            String example,
            String... repositories) {
        write(new PrintWriter(output), example, repositories);
    }

    /**
     * The address of a page on 51degrees.com, tagged with the example.
     * @param page the path of the page
     * @param example the example's tag
     * @return the address
     * @throws IllegalArgumentException if the tag is not a lower case slug
     */
    public static String link(String page, String example) {
        if (example == null || SLUG.matcher(example).matches() == false) {
            throw new IllegalArgumentException(
                    "'" + example + "' is not a lower case slug.");
        }
        return "https://51degrees.com/" +
                page + "?utm_source=code&utm_medium=example" +
                "&utm_campaign=51degrees-java&utm_content=" + example +
                "&utm_term=find-out-more";
    }
}
