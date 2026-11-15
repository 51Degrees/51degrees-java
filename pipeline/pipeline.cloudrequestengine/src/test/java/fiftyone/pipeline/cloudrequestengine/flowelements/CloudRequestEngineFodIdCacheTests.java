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
import fiftyone.pipeline.engines.caching.FlowCache;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * A response that holds a value created for the one request that asked for
 * it, such as a 51Did, is neither stored in nor served from the cache.
 * See the Values created for one request section of
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/caching.md#L144">caching.md</a>:
 * "A cache MUST NOT store a result that holds such a value, and MUST NOT
 * serve one that it finds."
 */
public class CloudRequestEngineFodIdCacheTests
    extends CloudRequestEngineTestsBase {

    public CloudRequestEngineFodIdCacheTests() throws MalformedURLException {
        super();
    }

    /**
     * Process the same Evidence twice with an engine that has a cache, the
     * service answering each request with the next response in the list.
     * @param responses the responses, one a request
     * @return the JSON each request's Flow Data holds, in order
     */
    private List<String> processTwice(String... responses) throws Exception {
        evidenceKeysResponse =
            new JSONArray(Arrays.asList("query.User-Agent")).toString();
        configureMockedClient();
        final List<String> remaining = new ArrayList<>(Arrays.asList(responses));
        doAnswer(invocation -> {
            java.net.HttpURLConnection connection = invocation.getArgument(0);
            String path = connection.getURL().getPath();
            if (path.endsWith("properties")) {
                return accessiblePropertiesResponse;
            }
            if (path.endsWith("evidencekeys")) {
                return evidenceKeysResponse;
            }
            return remaining.remove(0);
        }).when(httpClient).postData(any(), anyMap(), (byte[]) any());

        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .setCacheSize(10)
                .build();
        List<String> answers = new ArrayList<>();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build()) {
            for (int i = 0; i < 2; i++) {
                try (FlowData data = pipeline.createFlowData()) {
                    data.addEvidence("query.User-Agent", "iPhone");
                    data.process();
                    answers.add(data.get(CloudRequestData.class)
                        .getJsonResponse());
                }
            }
        }
        return answers;
    }

    /**
     * A response whose JSON has a top-level member named fodid, in any case,
     * is not stored, so the second identical request goes to the cloud
     * service and gets its own 51Did.
     */
    @ParameterizedTest
    @ValueSource(strings = { "fodid", "FodId", "FODID" })
    public void process_FodIdResponseIsNotStored(String name) throws Exception {
        String first = "{'device':{'value':'1'},'" + name + "':'first'}";
        String second = "{'device':{'value':'1'},'" + name + "':'second'}";

        List<String> answers = processTwice(first, second);

        assertEquals(Arrays.asList(first, second), answers,
            "A response with a 51Did was served from the cache.");
        verify(httpClient, times(2)).postData(
            argThat(c -> c.getURL().getPath().endsWith("properties") == false),
            any(),
            any());
    }

    /**
     * Every other response is cached as before, including one whose fodid
     * member is not at the top level.
     */
    @Test
    public void process_ResponseWithoutTopLevelFodIdIsCached()
        throws Exception {
        String first = "{'device':{'value':'1','fodid':'nested'}}";

        List<String> answers = processTwice(first);

        assertEquals(Arrays.asList(first, first), answers);
        verify(httpClient, times(1)).postData(
            argThat(c -> c.getURL().getPath().endsWith("properties") == false),
            any(),
            any());
    }

    /**
     * A result with a 51Did that the cache already holds, such as one an
     * earlier version stored, is not served. The request goes to the cloud
     * service instead.
     */
    @Test
    public void process_FodIdResultFoundInTheCacheIsNotServed()
        throws Exception {
        evidenceKeysResponse =
            new JSONArray(Arrays.asList("query.User-Agent")).toString();
        configureMockedClient();
        CloudRequestEngine engine =
            new CloudRequestEngineBuilder(loggerFactory, httpClient)
                .setResourceKey("resourcekey")
                .build();
        FlowCache cache = mock(FlowCache.class);
        engine.setCache(cache);

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine).build();
             FlowData earlier = pipeline.createFlowData();
             FlowData data = pipeline.createFlowData()) {
            CloudRequestData cached = new CloudRequestData(
                loggerFactory.getLogger("test"), earlier, engine);
            cached.setJsonResponse(
                "{'device':{'value':'1'},'fodid':'stale'}");
            when(cache.get(any())).thenReturn(cached);
            data.addEvidence("query.User-Agent", "iPhone");
            data.process();
            assertEquals(jsonResponse,
                data.get(CloudRequestData.class).getJsonResponse(),
                "A result with a 51Did found in the cache was served.");
            verify(cache, never()).put(any(), same(cached));
        }
    }
}
