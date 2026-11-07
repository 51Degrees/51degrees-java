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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageBuilder;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import fiftyone.pipeline.web.services.ClientsidePropertyServiceCore;
import fiftyone.pipeline.web.services.FlowDataProviderCore;
import fiftyone.pipeline.web.services.WebRequestEvidenceServiceCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The web integration gives usage sharing the whole web request, and the
 * usage sharing element accepts no Evidence, so the client-side endpoints'
 * ETag does not change with headers no element uses, If-None-Match among
 * them.
 */
public class UsageSharingEvidenceTests {

    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36";

    private Pipeline pipeline;

    private final WebRequestEvidenceServiceCore evidenceService =
        new WebRequestEvidenceServiceCore.Default();

    @Before
    public void init() throws Exception {
        pipeline = new PipelineBuilder(LoggerFactory.getILoggerFactory())
            .addFlowElement(new SequenceElementBuilder(
                LoggerFactory.getILoggerFactory()).build())
            .addFlowElement(new JsonBuilderElementBuilder(
                LoggerFactory.getILoggerFactory()).build())
            .addFlowElement(new ShareUsageBuilder(
                LoggerFactory.getILoggerFactory())
                .setSharePercentage(0)
                .build())
            .setAutoCloseElements(true)
            .build();
    }

    @After
    public void cleanUp() throws Exception {
        pipeline.close();
    }

    private static HttpServletRequest request(Map<String, String> headers) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaderNames()).thenReturn(
            Collections.enumeration(headers.keySet()));
        for (Map.Entry<String, String> header : headers.entrySet()) {
            when(request.getHeader(header.getKey()))
                .thenReturn(header.getValue());
        }
        when(request.getCookies()).thenReturn(new Cookie[]{
            new Cookie("51D_Profile", "123456")});
        when(request.getParameterNames()).thenReturn(
            Collections.enumeration(Collections.singletonList("q")));
        when(request.getParameter("q")).thenReturn("search");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(request.getScheme()).thenReturn("https");
        return request;
    }

    private static Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", USER_AGENT);
        headers.put("X-Test", "shared");
        headers.put("Cookie", "51D_Profile=123456");
        return headers;
    }

    /**
     * The web request is added as server.http-request on every request,
     * although no element accepts it, keyed the way Evidence is keyed. A
     * header no element accepts is not added as Evidence of its own.
     */
    @Test
    public void addEvidenceFromRequest_AddsTheWebRequest() throws Exception {
        try (FlowData flowData = pipeline.createFlowData()) {
            evidenceService.addEvidenceFromRequest(
                flowData, request(headers()));

            Map<String, Object> evidence = flowData.getEvidence().asKeyMap();
            assertTrue("The web request was not added.",
                evidence.get("server.http-request") instanceof Map);
            Map<?, ?> webRequest = (Map<?, ?>) evidence.get("server.http-request");
            assertEquals("shared", webRequest.get("header.X-Test"));
            assertEquals(USER_AGENT, webRequest.get("header.User-Agent"));
            assertEquals("123456", webRequest.get("cookie.51D_Profile"));
            assertEquals("search", webRequest.get("query.q"));
            assertEquals("1.2.3.4", webRequest.get("server.client-ip"));
            assertFalse("A header no element accepts was added as Evidence.",
                evidence.containsKey("header.X-Test"));
        }
    }

    /**
     * A browser revalidates with the ETag it was given in If-None-Match.
     * That header changes no result, so the ETag of the revalidation is the
     * one it sends, and it gets a 304.
     */
    @Test
    public void Revalidation_SameETag_Gets304() throws Exception {
        Map<String, String> revalidationHeaders = headers();
        HttpServletRequest first = request(headers());
        try (FlowData firstData = pipeline.createFlowData();
             FlowData revalidationData = pipeline.createFlowData()) {
            evidenceService.addEvidenceFromRequest(firstData, first);
            firstData.process();

            FlowDataProviderCore provider = mock(FlowDataProviderCore.class);
            when(provider.getFlowData(first)).thenReturn(firstData);
            ClientsidePropertyServiceCore service =
                new ClientsidePropertyServiceCore.Default(provider, pipeline);

            HttpServletResponse firstResponse = response();
            service.serveJson(first, firstResponse);
            ArgumentCaptor<String> etag = ArgumentCaptor.forClass(String.class);
            verify(firstResponse).setHeader(eq("ETag"), etag.capture());

            revalidationHeaders.put("If-None-Match", etag.getValue());
            HttpServletRequest revalidation = request(revalidationHeaders);
            evidenceService.addEvidenceFromRequest(
                revalidationData, revalidation);
            revalidationData.process();
            when(provider.getFlowData(revalidation))
                .thenReturn(revalidationData);

            assertEquals("The revalidation's ETag differs from the one it sends.",
                etag.getValue(),
                Integer.toString(revalidationData.generateKey(
                    pipeline.getEvidenceKeyFilter()).hashCode()));

            HttpServletResponse revalidationResponse = response();
            service.serveJson(revalidation, revalidationResponse);
            verify(revalidationResponse).setStatus(304);
            verify(revalidationResponse, never()).setStatus(200);
            verify(revalidationResponse, never()).setContentType(anyString());
        }
    }

    private static HttpServletResponse response() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(
            new PrintWriter(new StringWriter()));
        return response;
    }
}
