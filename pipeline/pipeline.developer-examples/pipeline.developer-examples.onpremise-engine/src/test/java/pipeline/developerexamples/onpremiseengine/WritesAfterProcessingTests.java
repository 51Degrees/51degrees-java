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

package pipeline.developerexamples.onpremiseengine;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectDataBase;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import pipeline.developerexamples.onpremiseengine.flowelements.SimpleOnPremiseEngine;
import pipeline.developerexamples.onpremiseengine.flowelements.SimpleOnPremiseEngineBuilder;

import java.util.Calendar;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The check, for the Flow Element in this module, the
 * {@link SimpleOnPremiseEngine}, that processes it, then writes to its
 * Element Data and expects the error.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Specification</a>
 */
public class WritesAfterProcessingTests {

    private static final ILoggerFactory LOGGER_FACTORY =
        LoggerFactory.getILoggerFactory();

    /**
     * Every write method of Element Data, and of Aspect Data, fails once its
     * Flow Element has finished.
     * @param data the Element Data of a finished Flow Element
     */
    static void assertWritesFail(ElementData data) {
        assertThrows(
            IllegalStateException.class,
            () -> data.put("after", "value"),
            "put");
        assertThrows(
            IllegalStateException.class,
            () -> data.populateFromMap(
                Collections.<String, Object>singletonMap("after", "value")),
            "populateFromMap");
        if (data instanceof AspectDataBase) {
            AspectDataBase aspect = (AspectDataBase) data;
            assertThrows(
                IllegalStateException.class,
                aspect::setCloudRequestFailed,
                "setCloudRequestFailed");
            assertThrows(
                IllegalStateException.class,
                () -> aspect.addEngine(null),
                "addEngine");
            assertThrows(
                IllegalStateException.class,
                () -> aspect.addProcessCallable(null),
                "addProcessCallable");
        }
    }

    @Test
    public void simpleOnPremiseEngine_ElementDataRefusesWrites()
        throws Exception {
        SimpleOnPremiseEngine engine =
            new SimpleOnPremiseEngineBuilder(LOGGER_FACTORY)
                .setAutoUpdate(false)
                .build(
                    WritesAfterProcessingTests.class.getClassLoader()
                        .getResource("starsigns.csv").getPath(),
                    false);
        Calendar dob = Calendar.getInstance();
        dob.set(1992, Calendar.DECEMBER, 18);
        try (Pipeline pipeline = new PipelineBuilder(LOGGER_FACTORY)
                .addFlowElement(engine)
                .setAutoCloseElements(true)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("date-of-birth", dob.getTime());
            data.process();
            assertEquals(0, data.getErrors().size(),
                String.valueOf(data.getErrors()));

            ElementData own = data.getFromElement(engine);
            assertWritesFail(own);
        }
    }
}
