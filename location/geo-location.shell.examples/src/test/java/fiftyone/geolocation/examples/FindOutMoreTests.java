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

import fiftyone.geolocation.examples.cloud.GettingStarted;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Each Location example ends its output with a "Find out more" section,
 * which lists pages on 51degrees.com, each with its campaign tags, and the
 * repository. With no resource key set, each example says where to create
 * one and stops, so these tests need no key. With the key with every
 * property set in _51DEGREES_RESOURCE_KEY_BESPOKE, the tests give it to each
 * example as the system property the example reads, and each example calls
 * the cloud service and writes what it found before the section.
 */
public class FindOutMoreTests {

    private static final String RULE =
        "----------------------------------------------------------------";

    private static final String HEADING = "Find out more";

    private static final String REPOSITORY =
        "https://github.com/51Degrees/51degrees-java";

    private static final String NO_KEY =
        "needs a resource key, and none is set";

    /**
     * The name the tests read the resource key with every property from.
     */
    private static final String TEST_KEY_NAME =
        "_51DEGREES_RESOURCE_KEY_BESPOKE";

    private static final Pattern TAGGED = Pattern.compile(
        "https://51degrees\\.com/[^?\\s]*\\?utm_source=code"
        + "&utm_medium=example&utm_campaign=51degrees-java"
        + "&utm_content=[a-z0-9][a-z0-9._-]*&utm_term=find-out-more$");

    /**
     * Runs a Location example as a person would.
     */
    private interface Example {
        void run() throws Exception;
    }

    @Test
    public void GettingStarted_EndsWithFindOutMore() throws Exception {
        assertFindOutMore(run(() -> GettingStarted.main(new String[0])));
    }

    @Test
    public void CombiningServices_EndsWithFindOutMore() throws Exception {
        assertFindOutMore(run(() -> CombiningServices.main(new String[0])));
    }

    private static void assertFindOutMore(String output) {
        int heading = output.lastIndexOf(RULE + "\n" + HEADING + "\n");
        assertTrue("No Find out more section:\n" + output, heading >= 0);
        String before = output.substring(0, heading);
        if (testKey() == null && ResourceKey.find(new String[0]) == null) {
            assertTrue(
                "The example did not say it needs a resource key before "
                + "the Find out more section:\n" + output,
                before.contains(NO_KEY));
        } else {
            assertFalse(
                "The example said no key is set, but it ran with one:\n"
                + output,
                before.contains(NO_KEY));
            assertFalse(
                "The example wrote nothing before the Find out more "
                + "section:\n" + output,
                before.trim().isEmpty());
        }
        String section = output.substring(
            heading + RULE.length() + HEADING.length() + 2);
        int end = section.indexOf(RULE + "\n");
        assertTrue("The section has no closing rule:\n" + output, end > 0);
        section = section.substring(0, end);
        int pages = 0;
        for (String line : section.split("\n")) {
            if (line.contains("51degrees.com")) {
                assertTrue(
                    "A 51degrees.com link without its campaign tags: "
                    + line,
                    TAGGED.matcher(line).find());
                pages++;
            }
        }
        assertEquals("The pages listed:\n" + section, 2, pages);
        assertTrue("The repository is not listed:\n" + section,
            section.contains(REPOSITORY));
    }

    /**
     * The key the tests give each example, read as an environment variable
     * and then as a system property, or null when there is none.
     */
    private static String testKey() {
        String value = System.getenv(TEST_KEY_NAME);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(TEST_KEY_NAME);
        }
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /**
     * Runs the example and returns what it wrote, with each line ending in
     * a line feed. The example reads its key from the name in
     * {@link ResourceKey#VARIABLE}, so the key the tests read is set there
     * as a system property while the example runs.
     */
    private static String run(Example example) throws Exception {
        String key = testKey();
        String saved = System.getProperty(ResourceKey.VARIABLE);
        if (key != null) {
            System.setProperty(ResourceKey.VARIABLE, key);
        }
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, "UTF-8"));
        try {
            example.run();
        } finally {
            System.setOut(original);
            if (saved == null) {
                System.clearProperty(ResourceKey.VARIABLE);
            } else {
                System.setProperty(ResourceKey.VARIABLE, saved);
            }
        }
        return new String(captured.toByteArray(), StandardCharsets.UTF_8)
            .replace("\r\n", "\n");
    }
}
