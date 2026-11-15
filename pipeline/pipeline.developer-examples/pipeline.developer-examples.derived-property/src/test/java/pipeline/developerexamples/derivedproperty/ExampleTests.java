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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the Derived Property Element example as a person would, with no
 * arguments, no resource key, no data file and no network connection
 * (pipeline-specification/required-examples.md, "Every reference
 * implementation MUST provide one example for the Derived Property
 * Element").
 */
public class ExampleTests {

    private static final String RULE =
        "----------------------------------------------------------------";

    /**
     * Runs the example's main and returns what it wrote, with each line
     * ending in a line feed.
     */
    static String runMain() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, "UTF-8"));
        try {
            Main.main(new String[0]);
        } finally {
            System.setOut(original);
        }
        String output = new String(captured.toByteArray(),
            StandardCharsets.UTF_8).replace("\r\n", "\n");
        original.println(output);
        return output;
    }

    /**
     * The part of the output after a heading, up to the next numbered
     * heading or the Find out more section.
     */
    private static String part(String output, String heading) {
        int start = output.indexOf(heading);
        assertTrue(start >= 0, "The output has no part headed '" + heading +
            "':\n" + output);
        int end = output.length();
        for (String next : new String[]{"\n1. ", "\n2. ", "\n3. ", "\n4. ",
            "\n" + RULE}) {
            int at = output.indexOf(next, start + heading.length());
            if (at > 0 && at < end) {
                end = at;
            }
        }
        return output.substring(start, end);
    }

    /**
     * The example shows the first three of the four things the
     * specification lists (pipeline-specification/required-examples.md,
     * "The example MUST show all four of the following"). The fourth,
     * a source property that is not available, has its own test.
     */
    @Test
    public void Main_ShowsScriptInCode() throws Exception {
        String part = part(runMain(), "1. A script in the code");
        assertTrue(part.contains("derived.Comfort = Comfortable"), part);
    }

    @Test
    public void Main_ShowsScriptFromFile() throws Exception {
        String part = part(runMain(), "2. A script read from a file");
        assertTrue(part.contains("FrostRisk.yaml"),
            "The part should name the file beside the example:\n" + part);
        assertTrue(part.contains("derived.FrostRisk = true"), part);
    }

    @Test
    public void Main_ShowsConfigurationFile() throws Exception {
        String part = part(runMain(),
            "3. The same pipeline built from a configuration file");
        assertTrue(part.contains("derivedProperty.xml"),
            "The part should name the configuration file:\n" + part);
        assertTrue(part.contains("derived.FrostRisk = "), part);
    }

    /**
     * The example shows, rather than only describes, what the caller sees
     * when a source property a script names is not available: a property
     * with no value and a message naming what was missing
     * (pipeline-specification/required-examples.md, "This case MUST be
     * shown rather than only described").
     */
    @Test
    public void Main_ShowsAMissingSourceProperty() throws Exception {
        String part = part(runMain(),
            "4. A source property that is not available");
        assertTrue(part.contains("derived.FrostRisk = no value. Derived " +
            "property 'FrostRisk' has no value because 1 source property " +
            "was not available. 'sensor.Humidity' (element 'sensor' has no " +
            "value for 'Humidity': property not present on this request)."),
            part);
    }

    @Test
    public void Main_Runs() throws Exception {
        String output = runMain();
        assertTrue(output.contains("derived."),
            "The example should show a derived property value:\n" + output);
        int heading = output.lastIndexOf(RULE + "\nFind out more\n");
        assertTrue(heading > output.lastIndexOf("derived."),
            "The output should end with the Find out more section:\n" +
            output);
    }
}
