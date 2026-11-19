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

package fiftyone.devicedetection;

import fiftyone.devicedetection.cloud.flowelements.DeviceDetectionCloudEngine;
import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.core.flowelements.FlowElement;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

/**
 * The cloud and on-premise Device Detection engines put their results in
 * Flow Data under the same key and as the same interface, so a consuming
 * application reads them in the same way whichever engine is in the
 * Pipeline. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/README.md#L95">README.md line 95</a>:
 * "Where both Cloud and On-Premise variants of an Engine are available,
 * they MUST arrange that the Properties and values produced are compatible
 * with each other so that the Engines can be substituted in the Pipeline
 * without alteration to the consuming application."
 * <p>
 * Comparing the values each engine gives needs a resource key and the
 * native engine with a data file, so it is left to the example tests.
 */
public class CloudOnPremiseSubstitutionTests {

    /**
     * The engines are created without running a constructor, as only the
     * data key, which is a constant, is read from them.
     */
    @Test
    public void elementDataKey_IsTheSame() {
        FlowElement<?, ?> cloud =
            mock(DeviceDetectionCloudEngine.class, CALLS_REAL_METHODS);
        FlowElement<?, ?> onPremise =
            mock(DeviceDetectionHashEngine.class, CALLS_REAL_METHODS);
        assertEquals(
            onPremise.getElementDataKey(),
            cloud.getElementDataKey());
    }

    /**
     * Each engine's Element Data is a {@link DeviceData}, which the
     * consuming application asks Flow Data for.
     */
    @Test
    public void elementData_IsDeviceData() {
        assertTrue(DeviceData.class.isAssignableFrom(
            elementDataType(DeviceDetectionCloudEngine.class)));
        assertTrue(DeviceData.class.isAssignableFrom(
            elementDataType(DeviceDetectionHashEngine.class)));
    }

    /**
     * The first type argument of the nearest generic superclass, which is
     * the engine's Element Data type.
     */
    private static Class<?> elementDataType(Class<?> engine) {
        for (Class<?> c = engine; c != null; c = c.getSuperclass()) {
            Type type = c.getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                Type first =
                    ((ParameterizedType) type).getActualTypeArguments()[0];
                if (first instanceof Class) {
                    return (Class<?>) first;
                }
            }
        }
        assertNotNull("No Element Data type found for " + engine, null);
        return null;
    }
}
