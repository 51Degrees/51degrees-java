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
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * An engine with a cache treats two requests whose Evidence differs only
 * in the case of a key as the same request, and two whose Evidence differs
 * only in the case of a value as different ones. See the Generation of keys
 * section of
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/caching.md#L71">caching.md</a>:
 * "Comparison of Evidence keys MUST be case-insensitive."
 */
public class CloudRequestEngineCacheKeyCaseTests
    extends CloudRequestEngineTestsBase {

    public CloudRequestEngineCacheKeyCaseTests() throws MalformedURLException {
        super();
    }

    /**
     * Process the two pieces of Evidence in turn with one engine that has a
     * cache, and count the requests that reach the cloud service.
     */
    private int requestsFor(String[] first, String[] second) throws Exception {
        evidenceKeysResponse =
            new JSONArray(Arrays.asList("query.User-Agent")).toString();
        configureMockedClient();
        final List<String> answers = new ArrayList<>(Arrays.asList(
            "{'device':{'value':'1'}}",
            "{'device':{'value':'2'}}"));
        doAnswer(invocation -> {
            java.net.HttpURLConnection connection = invocation.getArgument(0);
            String path = connection.getURL().getPath();
            if (path.endsWith("properties")) {
                return accessiblePropertiesResponse;
            }
            if (path.endsWith("evidencekeys")) {
                return evidenceKeysResponse;
            }
            return answers.remove(0);
        }).when(httpClient).postData(any(), anyMap(), (byte[]) any());

        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .setCacheSize(10)
                .build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build()) {
            for (String[] evidence : new String[][] { first, second }) {
                try (FlowData data = pipeline.createFlowData()) {
                    data.addEvidence(evidence[0], evidence[1]);
                    data.process();
                    data.get(CloudRequestData.class).getJsonResponse();
                }
            }
        }
        return 2 - answers.size();
    }

    @Test
    public void keyInAnotherCase_IsServedFromTheCache() throws Exception {
        assertEquals(1, requestsFor(
            new String[] { "query.User-Agent", "iPhone" },
            new String[] { "query.user-agent", "iPhone" }),
            "A key in another case was not taken as the same Evidence.");
        verify(httpClient, times(1)).postData(
            argThat(c -> c.getURL().getPath().endsWith("properties") == false &&
                c.getURL().getPath().endsWith("evidencekeys") == false),
            any(),
            any());
    }

    @Test
    public void valueInAnotherCase_IsNotServedFromTheCache() throws Exception {
        assertEquals(2, requestsFor(
            new String[] { "query.User-Agent", "iPhone" },
            new String[] { "query.User-Agent", "IPHONE" }),
            "A value in another case was taken as the same Evidence.");
    }
}
