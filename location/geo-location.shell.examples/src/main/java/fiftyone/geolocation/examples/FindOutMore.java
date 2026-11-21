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

package fiftyone.geolocation.examples;

import java.io.PrintStream;

/**
 * Writes the "Find out more" section that ends each Location example's
 * output, set apart by a rule above and below. It gives the example's page
 * and the Location page on 51degrees.com, each tagged with the example, then
 * the repository.
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
     * The repository the examples come from.
     */
    public static final String REPOSITORY =
        "https://github.com/51Degrees/51degrees-java";

    private FindOutMore() {
    }

    /**
     * Writes the section.
     * @param output where to write, flushed afterwards
     * @param examplePage the address of the example's page, with its
     *                    campaign tags
     * @param locationPage the address of the Location page, with its
     *                     campaign tags
     */
    public static void write(
        PrintStream output,
        String examplePage,
        String locationPage) {
        output.println();
        output.println(RULE);
        output.println(HEADING);
        output.println("\tThis example: " + examplePage);
        output.println("\tLocation: " + locationPage);
        output.println("\t" + REPOSITORY);
        output.println(RULE);
        output.flush();
    }
}
