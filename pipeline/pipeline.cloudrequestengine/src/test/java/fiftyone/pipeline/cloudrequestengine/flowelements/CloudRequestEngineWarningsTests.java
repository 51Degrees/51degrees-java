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

import fiftyone.common.testhelpers.TestLogger;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Each entry in the warnings array of a response from the cloud service is
 * logged as a warning, for the requests made when the engine is built and
 * for those made when it processes.
 */
public class CloudRequestEngineWarningsTests
    extends CloudRequestEngineTestsBase {

    private static final String LICENSE_KEY = "TESTKEYNOTREALABCDEFGHIJ1234";

    private static final String HIDDEN = "[hidden]";

    public CloudRequestEngineWarningsTests() throws MalformedURLException {
        super();
    }

    @Test
    public void Process_EachWarningLoggedAsAWarning() throws Exception {
        jsonResponse = "{\"device\":{\"value\":\"1\"}," +
            "\"warnings\":[\"first warning\",\"second warning\"]}";
        configureMockedClient();

        process(new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey("resource_key")
            .build());

        assertLoggedOnce("first warning");
        assertLoggedOnce("second warning");
    }

    @Test
    public void Build_WarningInAccessibleProperties_LoggedAsAWarning()
        throws Exception {
        accessiblePropertiesResponse =
            "{\"Products\": {\"device\": {\"DataTier\": \"tier\"," +
            "\"Properties\": [{\"Name\": \"value\",\"Type\": \"String\"," +
            "\"Category\": \"Device\"}]}}," +
            "\"warnings\":[\"start-up warning\"]}";
        configureMockedClient();

        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resource_key")
                .build();

        assertNotNull(engine.getPublicProperties());
        assertLoggedOnce("start-up warning");
    }

    /**
     * The warnings are logged when the response also holds errors, which
     * are thrown as before.
     */
    @Test
    public void Process_WarningBesideAnError_LoggedAndErrorRaised()
        throws Exception {
        jsonResponse = "{\"errors\":[\"an error\"]," +
            "\"warnings\":[\"a warning\"]}";
        configureMockedClient();

        String errors = process(
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resource_key")
                .build());

        assertTrue(errors.contains("an error"), errors);
        assertLoggedOnce("a warning");
    }

    /**
     * A warning that names the license key shows no part of it, as an
     * error does.
     */
    @Test
    public void Process_LicenseKeyInWarning_Hidden() throws Exception {
        jsonResponse = "{\"device\":{\"value\":\"1\"}," +
            "\"warnings\":[\"License key " + LICENSE_KEY +
            " expires soon\"]}";
        configureMockedClient();

        process(new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey("resource_key")
            .setLicenseKey(LICENSE_KEY)
            .build());

        for (String warning : warnings()) {
            assertFalse(warning.contains(LICENSE_KEY), warning);
        }
        assertLoggedOnce("License key " + HIDDEN + " expires soon");
    }

    /**
     * Process one request, and give the text of each error it raised.
     */
    private String process(CloudRequestEngine engine) throws Exception {
        StringBuilder errors = new StringBuilder();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();
            data.getErrors().forEach(e ->
                errors.append(e.getThrowable().getMessage()).append('\n'));
            // The response is a lazy property, so once processing has
            // finished its error reaches only the reader.
            try {
                data.getFromElement(engine).getJsonResponse();
            } catch (RuntimeException e) {
                errors.append(e.getMessage()).append('\n');
            }
        }
        return errors.toString();
    }

    private List<String> warnings() {
        List<String> warnings = new ArrayList<>();
        for (TestLogger logger : loggerFactory.loggers) {
            warnings.addAll(logger.warningsLogged);
        }
        return warnings;
    }

    private void assertLoggedOnce(String expected) {
        List<String> warnings = warnings();
        long count = warnings.stream().filter(expected::equals).count();
        assertEquals(1, count,
            "Expected the warning '" + expected + "' once in " + warnings);
    }
}
