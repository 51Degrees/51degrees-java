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

import fiftyone.pipeline.cloudrequestengine.data.CloudPassThroughData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The parts of the cloud response a server passes on to the page, and the
 * page's answers it passes back to the cloud service.
 */
public class CloudPassThroughEngineTests extends CloudRequestEngineTestsBase {

    /** The browser tests' script, as the cloud service hands it out. */
    private static final String SCRIPT =
        "window.__tests={token:'t'};/*51D replace this comment*/";

    public CloudPassThroughEngineTests() throws MalformedURLException {
        super();
        evidenceKeysResponse = new JSONArray(Arrays.asList(
            "query.User-Agent", "query.ja4", "query.h2", "query.client-ip",
            "cookie.51D_ProfileIds")).toString();
        accessiblePropertiesResponse = "{'Products': {"
            + "'browserderived': {'DataTier': 'tier','Properties': ["
            + "{'Name': 'BrowserTestsJavaScript','Type': 'JavaScript',"
            + "'Category': 'Browser'}]},"
            + "'derived': {'DataTier': 'tier','Properties': ["
            + "{'Name': 'HumanConfidence','Type': 'String',"
            + "'Category': 'Derived'}]},"
            + "'FODid': {'DataTier': 'tier','Properties': ["
            + "{'Name': 'IdProbGlobal','Type': 'String',"
            + "'Category': 'Id'}]}}}";
        jsonResponse = "{"
            + "\"browserderived\":{\"browsertestsjavascript\":"
            + JSONObject.quote(SCRIPT)
            + "},"
            + "\"derived\":{\"humanconfidence\":\"Low\"},"
            + "\"fodid\":{\"idprobglobal\":null,"
            + "\"idprobglobalnullreason\":\"Can not form IdProbGlobal. The "
            + "browser test answers have not come back yet.\"},"
            + "\"javascriptProperties\":"
            + "[\"browserderived.browsertestsjavascript\"]}";
    }

    @Test
    public void filter_IncludesTheAnswersOfTheCloudScripts()
            throws Exception {
        configureMockedClient();
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();
        EvidenceKeyFilter filter = engine.getEvidenceKeyFilter();

        assertTrue(filter.include("query.51D_x7Qa"));
        assertTrue(filter.include("Query.51d_x7Qa"));
        assertTrue(filter.include("cookie.51D_x7Qa"));
        assertNotNull(filter.order("query.51D_x7Qa"));
        // Keys the cloud service lists still read as before.
        assertTrue(filter.include("query.ja4"));
        assertTrue(filter.include("cookie.51D_ProfileIds"));
        // The prefix alone, another source and other names are not
        // answers.
        assertFalse(filter.include("query.51D_"));
        assertFalse(filter.include("header.51D_x7Qa"));
        assertFalse(filter.include("query.x51D_a"));
        assertFalse(filter.include("query.other"));
    }

    /**
     * Through the Pipeline's own filter, which a web integration uses to
     * decide what to take from a request, the answers, the session id and
     * the sequence of the page's next iteration, and the values a server
     * supplies, all reach the cloud service.
     */
    @Test
    public void process_PostsTheAnswersAndTheSuppliedValues()
            throws Exception {
        configureMockedClient();
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build();
             FlowData data = pipeline.createFlowData()) {
            String[][] evidence = {
                {"query.51D_x7Qa", "3z|answers"},
                {"query.session-id", "abc-123"},
                {"query.sequence", "2"},
                {"query.ja4", "t13d1516h2_8daaf6152771_02713d6af862"},
                {"query.h2", "1:65536;2:0;4:6291456;6:262144|15663105|0|m,a,s,p"},
                {"query.client-ip", "192.0.2.10"},
            };
            for (String[] item : evidence) {
                if (item[0].startsWith("query.51D_") ||
                    item[0].equals("query.ja4") ||
                    item[0].equals("query.h2") ||
                    item[0].equals("query.client-ip")) {
                    assertTrue(pipeline.getEvidenceKeyFilter()
                        .include(item[0]), item[0]);
                }
                data.addEvidence(item[0], item[1]);
            }
            data.process();
        }

        ArgumentCaptor<byte[]> body = ArgumentCaptor.forClass(byte[].class);
        verify(httpClient, times(1)).postData(
            argThat(c -> c.getURL().getPath().endsWith("properties") == false),
            any(),
            body.capture());
        List<String> items = Arrays.asList(
            new String(body.getValue(), StandardCharsets.UTF_8).split("&"));
        assertTrue(items.contains("51D_x7Qa=3z%7Canswers"), items.toString());
        assertTrue(items.contains("session-id=abc-123"), items.toString());
        assertTrue(items.contains("sequence=2"), items.toString());
        assertTrue(items.contains(
            "ja4=t13d1516h2_8daaf6152771_02713d6af862"), items.toString());
        assertTrue(items.contains("client-ip=192.0.2.10"), items.toString());
        assertTrue(items.stream().anyMatch(i -> i.startsWith("h2=1%3A65536")),
            items.toString());
    }

    /**
     * Once the answers have come back the cloud service leaves the
     * browser tests' script out. Its property then has no value, with the
     * reason, so the JSON builder can still read it.
     */
    @Test
    public void process_APartLeftOutHasNoValue() throws Exception {
        jsonResponse = "{\"fodid\":{\"idprobglobal\":\"AzUx\"},"
            + "\"javascriptProperties\":[]}";
        configureMockedClient();
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();
        CloudPassThroughEngine tests =
            new CloudPassThroughEngineBuilder(loggerFactory, "browserderived")
                .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .addFlowElement(tests)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.process();

            AspectPropertyValue<?> value = (AspectPropertyValue<?>)
                data.get(tests.getTypedDataKey())
                    .get("BrowserTestsJavaScript");
            assertFalse(value.hasValue());
            assertEquals(String.format(CloudPassThroughEngine.NOT_RETURNED,
                "BrowserTestsJavaScript", "browserderived"),
                value.getNoValueMessage());
        }
    }

    @Test
    public void process_ReadsEachPartOfTheResponse() throws Exception {
        configureMockedClient();
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();
        CloudPassThroughEngine tests =
            new CloudPassThroughEngineBuilder(loggerFactory, "browserderived")
                .build();
        CloudPassThroughEngine derived =
            new CloudPassThroughEngineBuilder(loggerFactory, "derived")
                .build();
        CloudPassThroughEngine fodid =
            new CloudPassThroughEngineBuilder(loggerFactory, "FODid")
                .build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .addFlowElement(tests)
                .addFlowElement(derived)
                .addFlowElement(fodid)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.process();

            assertEquals("fodid", fodid.getElementDataKey());
            AspectPropertyMetaData script = null;
            for (AspectPropertyMetaData property : tests.getProperties()) {
                if (property.getName().equals("BrowserTestsJavaScript")) {
                    script = property;
                }
            }
            assertNotNull(script);
            assertEquals(JavaScript.class, script.getType());
            assertEquals(1, fodid.getProperties().size(),
                "The FODid product is matched without regard to case.");

            CloudPassThroughData testsData = data.get(tests.getTypedDataKey());
            AspectPropertyValue<?> value = (AspectPropertyValue<?>)
                testsData.get("browsertestsjavascript");
            assertTrue(value.hasValue());
            assertTrue(value.getValue() instanceof JavaScript);
            assertEquals(SCRIPT, value.getValue().toString());

            AspectPropertyValue<?> confidence = (AspectPropertyValue<?>)
                data.get(derived.getTypedDataKey()).get("humanconfidence");
            assertEquals("Low", confidence.getValue());

            AspectPropertyValue<?> did = (AspectPropertyValue<?>)
                data.get(fodid.getTypedDataKey()).get("idprobglobal");
            assertFalse(did.hasValue());
            assertTrue(did.getNoValueMessage().contains(
                "browser test answers have not come back"),
                did.getNoValueMessage());
        }
    }
}
