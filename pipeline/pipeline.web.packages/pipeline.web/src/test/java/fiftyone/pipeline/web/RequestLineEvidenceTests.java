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

package fiftyone.pipeline.web;

import fiftyone.pipeline.core.data.Evidence;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.web.services.WebRequestEvidenceServiceCore;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The request line Evidence of the web integration specification's
 * Populating Evidence section. An integration that supplies these keys
 * supplies all three on every request, with server.request-query empty
 * when the request has no query string, and each exactly as it appeared on
 * the request line.
 */
public class RequestLineEvidenceTests {

    private final WebRequestEvidenceServiceCore evidenceService =
        new WebRequestEvidenceServiceCore.Default();

    /**
     * Flow Data whose Pipeline has an element that accepts the three request
     * line keys.
     */
    private static FlowData flowData() {
        FlowData flowData = mock(FlowData.class);
        Evidence evidence = mock(Evidence.class);
        when(evidence.asKeyMap()).thenReturn(new HashMap<String, Object>());
        when(flowData.getEvidence()).thenReturn(evidence);
        when(flowData.getEvidenceKeyFilter()).thenReturn(
            new EvidenceKeyFilterWhitelist(Arrays.asList(
                "server.request-method",
                "server.request-path",
                "server.request-query")));
        return flowData;
    }

    private static HttpServletRequest request(String path, String query) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn(path);
        when(request.getQueryString()).thenReturn(query);
        when(request.getHeaderNames())
            .thenReturn(Collections.<String>emptyEnumeration());
        when(request.getParameterNames())
            .thenReturn(Collections.<String>emptyEnumeration());
        return request;
    }

    /**
     * The query string is supplied whole and undecoded, beside the method
     * and the path.
     */
    @Test
    public void RequestQuery_SuppliedWholeAndUndecoded() {
        FlowData flowData = flowData();

        evidenceService.addEvidenceFromRequest(
            flowData, request("/a", "x=%2F&y=1"));

        verify(flowData).addEvidence("server.request-method", "GET");
        verify(flowData).addEvidence("server.request-path", "/a");
        verify(flowData).addEvidence("server.request-query", "x=%2F&y=1");
    }

    /**
     * A request with no query string still gets server.request-query, empty,
     * so an absent key only ever means the request line is not supplied.
     */
    @Test
    public void RequestQuery_NoQueryString_SuppliedEmpty() {
        FlowData flowData = flowData();

        evidenceService.addEvidenceFromRequest(flowData, request("/a", null));

        verify(flowData).addEvidence("server.request-path", "/a");
        verify(flowData).addEvidence("server.request-query", "");
    }
}
