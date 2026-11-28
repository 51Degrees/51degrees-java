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

import fiftyone.devicedetection.cloud.flowelements.DeviceDetectionCloudEngine;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * CombiningServices adds the Device Detection cloud engine to a pipeline,
 * so the examples need the Device Detection built with the pipeline they
 * run on. An engine from another version can lack a method the pipeline
 * calls, and the example then fails only when it builds its pipeline,
 * which needs a resource key. This test needs neither a key nor the
 * network.
 */
public class DeviceDetectionVersionTests {

    @Test
    public void cloudEngine_ImplementsThePipelinesDataType()
        throws NoSuchMethodException {
        assertEquals(
            DeviceDetectionCloudEngine.class,
            DeviceDetectionCloudEngine.class
                .getDeclaredMethod("getDataType")
                .getDeclaringClass());
    }
}
