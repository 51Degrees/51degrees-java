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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectDataBase;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The check, for the Flow Element in this module, the
 * {@link CloudRequestEngineDefault}, that processes it, then writes to its
 * Element Data and expects the error. The cloud service is a stand-in that
 * gives a fixed response.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Specification</a>
 */
public class WritesAfterProcessingTests extends CloudRequestEngineTestsBase {

    public WritesAfterProcessingTests() throws MalformedURLException {
        super();
    }

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
    }

    /**
     * The write methods every Aspect Data has fail as well.
     */
    static void assertAspectWritesFail(AspectDataBase data) {
        assertThrows(
            IllegalStateException.class,
            data::setCloudRequestFailed,
            "setCloudRequestFailed");
        assertThrows(
            IllegalStateException.class,
            () -> data.addEngine(null),
            "addEngine");
        assertThrows(
            IllegalStateException.class,
            () -> data.addProcessCallable(null),
            "addProcessCallable");
    }

    @Test
    public void cloudRequestEngine_ElementDataRefusesWrites()
        throws Exception {
        configureMockedClient();
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resource_key")
                .build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .setAutoCloseElements(true)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();
            assertEquals(0, data.getErrors().size(),
                String.valueOf(data.getErrors()));

            CloudRequestData own = data.getFromElement(engine);
            assertEquals(jsonResponse, own.getJsonResponse());
            assertWritesFail(own);
            assertAspectWritesFail(own);
            assertThrows(
                IllegalStateException.class,
                () -> own.setJsonResponse("{}"),
                "setJsonResponse");
            assertThrows(
                IllegalStateException.class,
                () -> own.setProcessStarted(true),
                "setProcessStarted");
        }
    }

    @Test
    public void humanConfidenceElement_ElementDataRefusesWrites()
        throws Exception {
        configureMockedClient();
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resource_key")
                .build();
        HumanConfidenceElement element =
            new HumanConfidenceElementBuilder(loggerFactory)
                .setRequestEngine(engine)
                .build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .addFlowElement(element)
                .setAutoCloseElements(true)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();
            assertEquals(0, data.getErrors().size(),
                String.valueOf(data.getErrors()));
            assertWritesFail(data.getFromElement(element));
        }
    }
}
