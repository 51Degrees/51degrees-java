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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TacCloudTest {

    /**
     * The example runs as a customer runs it, in a process of its own
     * that reads its key from _51DEGREES_RESOURCE_KEY. The test gives that
     * process the key with every property.
     */
    @Test
    public void run() throws Exception {
        String resourceKey = KeyHelper.requireResourceKey(
            KeyHelper.BESPOKE_RESOURCE_KEY_VARIABLE,
            "the devices that match a TAC", "hardware");
        ExampleProcess.Result result =
            ExampleProcess.runWithResourceKey(TacCloud.class, resourceKey);
        System.out.println(result.output);
        assertEquals("TacCloud should end with exit code 0",
            0, result.exitCode);
        assertTrue("The output should hold the example's results",
            result.output.contains("This example shows the details of " +
                "devices associated with a given 'Type Allocation Code'"));
    }
}