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

import static org.junit.Assert.assertEquals;

public class ExampleProcessTest {
    /**
     * A value of the form KeyHelper accepts as a key. It is not a real key.
     */
    private static final String KEY = "AQAAAAAAAAAAAAAAAAAAAAAA";

    /**
     * Writes the resource key its environment holds, as an example reads
     * it.
     */
    public static class Probe {
        public static void main(String[] args) {
            System.out.print(KeyHelper.findResourceKey(null,
                KeyHelper.RESOURCE_KEY_VARIABLE));
        }
    }

    /**
     * An example run with a resource key finds that key in the variable a
     * customer sets, whatever the environment of the test holds.
     */
    @Test
    public void runWithResourceKey_SetsTheVariableTheExampleReads()
        throws Exception {
        ExampleProcess.Result result =
            ExampleProcess.runWithResourceKey(Probe.class, KEY);
        assertEquals(0, result.exitCode);
        assertEquals(KEY, result.output);
    }

    @Test
    public void runWithoutKeys_LeavesTheVariableOut() throws Exception {
        ExampleProcess.Result result =
            ExampleProcess.runWithoutKeys(Probe.class);
        assertEquals(0, result.exitCode);
        assertEquals("null", result.output);
    }
}
