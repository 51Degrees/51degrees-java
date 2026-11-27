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

public class NativeModelCloudTest {
    /**
     * The example runs as a customer runs it, in a process of its own
     * that reads its key from _51DEGREES_RESOURCE_KEY, and the cloud that
     * _51DEGREES_CLOUD_ENDPOINT names when that is set. The test gives that
     * process the key with every property, and fails unless the example
     * lists a device with a vendor and a model for each native model name,
     * so a key that does not include HardwarePlatform.Profiles fails it.
     */
    @Test
    public void run() throws Exception {
        String resourceKey = KeyHelper.requireResourceKey(
            KeyHelper.BESPOKE_RESOURCE_KEY_VARIABLE,
            "the devices that match a native model name", "hardware");
        ExampleProcess.Result result = ExampleProcess.runWithResourceKey(
            NativeModelCloud.class, resourceKey);
        System.out.println(result.output);
        HardwareLookupOutput.requireDevices(NativeModelCloud.class, result,
            "native model lookup", "the native model name",
            NativeModelCloud.NATIVE_MODEL_1, NativeModelCloud.NATIVE_MODEL_2);
    }
}