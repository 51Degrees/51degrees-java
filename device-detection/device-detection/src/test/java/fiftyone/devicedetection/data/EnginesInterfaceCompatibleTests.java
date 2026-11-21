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

package fiftyone.devicedetection.data;

import fiftyone.devicedetection.cloud.data.DeviceDataCloud;
import fiftyone.devicedetection.cloud.data.MultiDeviceDataCloud;
import fiftyone.devicedetection.hash.engine.onpremise.data.DeviceDataHash;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Each of these Engines MUST return data that is interface compatible with
 * the others (device-detection-specification/data-model.md line 15 at
 * 706de1f0). The on-premise and cloud data share the DeviceData interface,
 * so code written against it reads either, and the cloud hardware profile
 * lookup gives its profiles as DeviceData too.
 */
public class EnginesInterfaceCompatibleTests {

    @Test
    public void Data_OnPremiseAndCloudAreDeviceData() {
        assertTrue("On-premise data is not DeviceData",
            DeviceData.class.isAssignableFrom(DeviceDataHash.class));
        assertTrue("Cloud data is not DeviceData",
            DeviceData.class.isAssignableFrom(DeviceDataCloud.class));
    }

    /**
     * Every property accessor of DeviceData is reached the same way, with the
     * same type, through the on-premise and the cloud data.
     */
    @Test
    public void Data_SameAccessorsAndTypes() throws Exception {
        int accessors = 0;
        for (Method method : DeviceData.class.getMethods()) {
            if (method.getDeclaringClass().equals(DeviceData.class) &&
                AspectPropertyValue.class.equals(method.getReturnType())) {
                for (Class<?> data : new Class<?>[] {
                    DeviceDataHash.class, DeviceDataCloud.class}) {
                    Method other = data.getMethod(
                        method.getName(), method.getParameterTypes());
                    assertEquals(data.getSimpleName() + "." + method.getName(),
                        method.getGenericReturnType(),
                        other.getGenericReturnType());
                }
                accessors++;
            }
        }
        assertTrue("DeviceData has no property accessors", accessors > 0);
    }

    /**
     * The hardware profile lookup gives a list of DeviceData.
     */
    @Test
    public void Data_HardwareProfilesAreDeviceData() throws Exception {
        Type type = MultiDeviceDataCloud.class.getMethod("getProfiles")
            .getGenericReturnType();
        assertTrue(type instanceof ParameterizedType);
        ParameterizedType list = (ParameterizedType) type;
        assertEquals(List.class, list.getRawType());
        assertEquals(DeviceData.class, list.getActualTypeArguments()[0]);
    }
}
