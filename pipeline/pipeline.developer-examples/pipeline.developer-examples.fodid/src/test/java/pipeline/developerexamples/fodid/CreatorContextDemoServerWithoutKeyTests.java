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

package pipeline.developerexamples.fodid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the 51Did creator context demo server as it runs from a fresh clone,
 * with no resource key anywhere, and checks that it ends without an error,
 * names the variable to set and links to where a key is created, then ends
 * with its Find out more section.
 */
class CreatorContextDemoServerWithoutKeyTests {

    private static final String CONFIGURE = "https://51degrees.com/configure";

    private static final String TAGS =
        "?utm_source=code&utm_medium=example&utm_campaign=51degrees-java" +
        "&utm_content=";

    private static void runWithoutKey(Class<?> example) throws Exception {
        ExampleProcess.Result result = ExampleProcess.runWithoutKeys(example);
        System.out.println(result.output);
        String output = result.output;
        assertEquals(0, result.exitCode,
            example.getSimpleName() + " should end with exit code 0");
        assertTrue(output.contains(
            "Set the _51DEGREES_RESOURCE_KEY environment variable"),
            "The output should name _51DEGREES_RESOURCE_KEY");
        int link = output.indexOf(CONFIGURE);
        assertTrue(link >= 0 && output.startsWith(TAGS,
                output.indexOf('?', link)),
            "The output should link to where a key is created");
        assertTrue(output.lastIndexOf("Find out more") > link,
            "The output should end with the Find out more " +
                "section after the link");
    }

    @Test
    void creatorContextDemoServer() throws Exception {
        runWithoutKey(CreatorContextDemoServer.class);
    }
}
