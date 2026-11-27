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

package fiftyone.ipintelligence.examples.console;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * "Examples MUST demonstrate accessing both the value and its associated
 * weighting" (ip-intelligence-specification/required-examples.md line 107).
 * The getting started examples are where a developer first reads how to get
 * a result, so their own code reads each value with its weighting, rather
 * than leaving it to code in another file the developer does not see.
 */
public class WeightingShownTest {

    private static String source(String name) throws IOException {
        Path path = Paths.get("src", "main", "java", "fiftyone",
            "ipintelligence", "examples", "console", name + ".java");
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void assertShowsWeighting(String name) throws IOException {
        String source = source(name);
        assertTrue(name + " does not read a weighting",
            source.contains(".getWeighting()"));
        assertTrue(name + " does not read the weighted value",
            source.contains("weightedValues(") &&
            source.contains(".getValue()"));
    }

    @Test
    public void gettingStartedOnPrem_ShowsValueAndWeighting()
        throws IOException {
        assertShowsWeighting("GettingStartedOnPrem");
    }

    @Test
    public void gettingStartedCloud_ShowsValueAndWeighting()
        throws IOException {
        assertShowsWeighting("GettingStartedCloud");
    }
}
