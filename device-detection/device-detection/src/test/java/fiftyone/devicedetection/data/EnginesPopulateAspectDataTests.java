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

import fiftyone.devicedetection.cloud.data.MultiDeviceDataCloud;
import fiftyone.devicedetection.cloud.flowelements.DeviceDataCloudInternal;
import fiftyone.devicedetection.cloud.flowelements.DeviceDetectionCloudEngine;
import fiftyone.devicedetection.cloud.flowelements.HardwareProfileCloudEngine;
import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDataHashDefault;
import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * All Engines MUST populate data objects that implement Aspect Data as
 * defined in the Pipeline specification
 * (device-detection-specification/data-model.md line 10 at 706de1f0).
 * Each Device Detection engine is checked through the data type it is
 * declared with, and each class that is made to hold an engine's data.
 */
public class EnginesPopulateAspectDataTests {

    private static final List<Class<?>> ENGINES = Arrays.asList(
        DeviceDetectionHashEngine.class,
        DeviceDetectionCloudEngine.class,
        HardwareProfileCloudEngine.class);

    /**
     * Find the data type an engine is declared with, which is the first type
     * argument of the base class it extends.
     */
    private static Class<?> dataTypeOf(Class<?> engine) {
        Type type = engine.getGenericSuperclass();
        assertTrue(engine.getSimpleName() + " has no data type argument",
            type instanceof ParameterizedType);
        Type data = ((ParameterizedType) type).getActualTypeArguments()[0];
        assertTrue(engine.getSimpleName() + "'s data type is not a class",
            data instanceof Class);
        return (Class<?>) data;
    }

    @Test
    public void Engines_AreAspectEngines() {
        for (Class<?> engine : ENGINES) {
            assertTrue(engine.getSimpleName() + " is not an Aspect Engine",
                AspectEngine.class.isAssignableFrom(engine));
        }
    }

    @Test
    public void Engines_DeclaredDataIsAspectData() {
        for (Class<?> engine : ENGINES) {
            Class<?> data = dataTypeOf(engine);
            assertTrue(engine.getSimpleName() + " populates " +
                    data.getSimpleName() + ", which is not Aspect Data",
                AspectData.class.isAssignableFrom(data));
        }
    }

    /**
     * The classes the engines' factories make, which hold the values, take
     * the Pipeline's Aspect Data behavior from its base class.
     */
    @Test
    public void Engines_DataMadeIsAspectData() {
        for (Class<?> made : Arrays.<Class<?>>asList(
            DeviceDataHashDefault.class,
            DeviceDataCloudInternal.class,
            MultiDeviceDataCloud.class)) {
            assertTrue(made.getSimpleName() + " is not Aspect Data",
                AspectDataBase.class.isAssignableFrom(made));
        }
    }
}
