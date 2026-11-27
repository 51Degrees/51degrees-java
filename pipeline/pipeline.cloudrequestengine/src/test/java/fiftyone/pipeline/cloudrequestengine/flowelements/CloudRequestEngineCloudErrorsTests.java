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

import fiftyone.pipeline.cloudrequestengine.CloudRequestException;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.exceptions.AggregateException;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static fiftyone.pipeline.cloudrequestengine.Constants.Messages.ExceptionCloudErrorsMultiple;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The entries of the errors array in a response from the cloud service are
 * thrown, one entry as a CloudRequestException carrying its text, and
 * several as one AggregateException holding a CloudRequestException for
 * each.
 */
public class CloudRequestEngineCloudErrorsTests
    extends CloudRequestEngineTestsBase {

    public CloudRequestEngineCloudErrorsTests() throws MalformedURLException {
        super();
    }

    @Test
    public void Process_SeveralErrors_OneAggregateException()
        throws Exception {
        jsonResponse = "{\"errors\":[\"first error\",\"second error\"]}";
        configureMockedClient();

        Throwable thrown = processOnce();

        assertInstanceOf(AggregateException.class, thrown);
        assertEquals(ExceptionCloudErrorsMultiple, thrown.getMessage());
        List<String> messages = new ArrayList<>();
        for (Throwable inner : thrown.getSuppressed()) {
            assertInstanceOf(CloudRequestException.class, inner);
            assertEquals(
                200,
                ((CloudRequestException) inner).getHttpStatusCode());
            messages.add(inner.getMessage());
        }
        List<String> expected = new ArrayList<>();
        expected.add("first error");
        expected.add("second error");
        assertEquals(expected, messages);
    }

    @Test
    public void Process_OneError_CloudRequestException() throws Exception {
        jsonResponse = "{\"errors\":[\"only error\"]}";
        configureMockedClient();

        Throwable thrown = processOnce();

        assertInstanceOf(CloudRequestException.class, thrown);
        assertTrue(
            thrown.getMessage().contains("only error"),
            thrown.getMessage());
        assertEquals(0, thrown.getSuppressed().length);
    }

    /**
     * Process one request, and give the one error it raised.
     */
    private Throwable processOnce() throws Exception {
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resource_key")
                .build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();
            // The response is a lazy property, so once processing has
            // finished its error reaches only the reader.
            Collection<FlowError> errors = data.getErrors();
            assertEquals(0, errors.size(), errors.toString());
            return assertThrows(RuntimeException.class,
                () -> data.getFromElement(engine).getJsonResponse());
        }
    }
}
