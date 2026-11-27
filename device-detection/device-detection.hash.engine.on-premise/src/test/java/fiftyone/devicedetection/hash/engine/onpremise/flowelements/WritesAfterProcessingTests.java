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

package fiftyone.devicedetection.hash.engine.onpremise.flowelements;

import fiftyone.devicedetection.hash.engine.onpremise.TestsBase;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectDataBase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

/**
 * The check, for the Flow Element in this module, the
 * {@link DeviceDetectionHashEngine}, that processes it, then writes to its
 * Element Data and expects the error. Values the Element Data keeps once it
 * has read them from the results are not writes, so reading every value
 * still works.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Specification</a>
 */
public class WritesAfterProcessingTests extends TestsBase {

    private static final String MOBILE =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 7_1 like Mac OS X) " +
        "AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 " +
        "Mobile/11D167 Safari/9537.53";

    @Before
    public void init() throws Exception {
        testInitialize(Constants.PerformanceProfiles.HighPerformance);
    }

    @After
    public void cleanup() {
        testCleanup();
    }

    private static void assertFails(String what, ThrowingRunnable write) {
        assertThrows(what, IllegalStateException.class, write);
    }

    /**
     * Every write method of Element Data, and of Aspect Data, fails once its
     * Flow Element has finished.
     * @param data the Element Data of a finished Flow Element
     */
    static void assertWritesFail(ElementData data) {
        assertFails("put", () -> data.put("after", "value"));
        assertFails("populateFromMap", () -> data.populateFromMap(
            Collections.<String, Object>singletonMap("after", "value")));
        if (data instanceof AspectDataBase) {
            AspectDataBase aspect = (AspectDataBase) data;
            assertFails("setCloudRequestFailed", aspect::setCloudRequestFailed);
            assertFails("addEngine", () -> aspect.addEngine(null));
            assertFails("addProcessCallable",
                () -> aspect.addProcessCallable(null));
        }
    }

    @Test
    public void deviceDetectionHashEngine_ElementDataRefusesWrites()
        throws Exception {
        try (FlowData data = getWrapper().getPipeline().createFlowData()) {
            data.addEvidence("header.user-agent", MOBILE);
            data.process();
            assertEquals(String.valueOf(data.getErrors()),
                0, data.getErrors().size());

            DeviceData own = data.getFromElement(getWrapper().getEngine());
            assertFalse(own.asKeyMap().isEmpty());
            assertWritesFail(own);
        }
    }
}
