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

package fiftyone.devicedetection.examples.console;

import fiftyone.devicedetection.examples.shared.ExampleProcess;
import fiftyone.devicedetection.examples.shared.KeyHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Checks what the TAC and native model examples print for the values they
 * look up, being a heading for each value with a line for each device under
 * it, indented by a tab, in the form {@code <vendor> <name> (<model>)}.
 */
final class HardwareLookupOutput {

    /**
     * What the examples print when the resource key lacks the lookup.
     */
    static final String NOT_INCLUDED =
        "does not include HardwarePlatform.Profiles";

    /**
     * How the examples render a property with no value.
     */
    private static final String NO_VALUE = "Unknown";

    private HardwareLookupOutput() {
    }

    /**
     * Fails unless the example listed at least one device with a vendor and
     * a model for each value, and then ended with exit code 0.
     * @param example the example that ran
     * @param result what the example wrote and how it ended
     * @param lookup the lookup the example makes, such as "TAC lookup"
     * @param subject what each value is, such as "the TAC"
     * @param values the values the example looks up
     */
    static void requireDevices(Class<?> example,
                               ExampleProcess.Result result,
                               String lookup,
                               String subject,
                               String... values) {
        String output = result.output;
        if (output.contains(NOT_INCLUDED)) {
            fail("The resource key in " +
                KeyHelper.BESPOKE_RESOURCE_KEY_VARIABLE +
                " does not include HardwarePlatform.Profiles, which the " +
                lookup + " needs.\n" + output);
        }
        for (String value : values) {
            String heading = "Which devices are associated with " +
                subject + " '" + value + "'?";
            if (listsDevice(output, heading) == false) {
                fail("No device with a vendor and a model was listed " +
                    "for " + subject + " '" + value + "'.\n" + output);
            }
        }
        assertEquals(example.getSimpleName() +
                " should end with exit code 0.\n" + output,
            0, result.exitCode);
    }

    /**
     * Whether a tab indented line under the heading is a device with a
     * vendor and a model.
     */
    private static boolean listsDevice(String output, String heading) {
        String[] lines = output.split("\r?\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].equals(heading)) {
                for (int j = i + 1;
                     j < lines.length && lines[j].startsWith("\t"); j++) {
                    if (isDevice(lines[j].substring(1))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Whether the line has the form {@code <vendor> <name> (<model>)}, where
     * the vendor is the text before the first space and the model is the
     * text in the final parentheses, and neither is empty or a no value.
     */
    static boolean isDevice(String line) {
        if (line.endsWith(")") == false) {
            return false;
        }
        // The parenthesis that opens the final one, counting any nested in
        // the model.
        int depth = 0;
        int open = -1;
        for (int i = line.length() - 1; i >= 0 && open < 0; i--) {
            char c = line.charAt(i);
            if (c == ')') {
                depth++;
            } else if (c == '(' && --depth == 0) {
                open = i;
            }
        }
        int space = line.indexOf(' ');
        if (open < 1 || line.charAt(open - 1) != ' ' ||
            space < 1 || space >= open - 1) {
            return false;
        }
        return hasValue(line.substring(0, space)) &&
            hasValue(line.substring(open + 1, line.length() - 1));
    }

    private static boolean hasValue(String text) {
        String value = text.trim();
        return value.isEmpty() == false && value.startsWith(NO_VALUE) == false;
    }
}
