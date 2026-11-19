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
import fiftyone.pipeline.web.services.ClientsidePropertyServiceCore;
import fiftyone.pipeline.web.services.FiftyOneJSServiceCore;
import fiftyone.pipeline.web.services.PipelineResultServiceCore;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The paths the JavaScript and the JSON are served from.
 * @see <a href="https://github.com/51Degrees/specifications/blob/rc/pipeline-specification/features/web-integration.md#intercepted-urls">Specification</a>
 */
public class FiftyOneJSServiceCoreTests {

    private ClientsidePropertyServiceCore clientside;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void init() {
        clientside = mock(ClientsidePropertyServiceCore.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    private void requestPath(String path) {
        requestPath("", path);
    }

    /**
     * The request URI holds the context path and then the path inside the
     * application.
     */
    private void requestPath(String contextPath, String path) {
        when(request.getRequestURL()).thenReturn(
            new StringBuffer("http://example.com" + contextPath + path));
        when(request.getRequestURI()).thenReturn(contextPath + path);
        when(request.getContextPath()).thenReturn(contextPath);
    }

    @Test
    public void DefaultPaths_JavaScriptAndJsonServed() throws IOException {
        FiftyOneJSServiceCore service =
            new FiftyOneJSServiceCore.Default(clientside, true);

        requestPath("/51Degrees.core.js");
        assertTrue(service.serveJS(request, response));
        requestPath("/51dpipeline/json");
        assertTrue(service.serveJson(request, response));

        verify(clientside, times(1)).serveJavascript(request, response);
        verify(clientside, times(1)).serveJson(request, response);
    }

    /**
     * The path the JSON was served from before the specification's path
     * became the default is still answered.
     */
    @Test
    public void DefaultPaths_OldJsonPathStillServed() throws IOException {
        FiftyOneJSServiceCore service =
            new FiftyOneJSServiceCore.Default(clientside, true);

        requestPath("/51Degrees.core.json");
        assertTrue(service.serveJson(request, response));
        requestPath("/app", "/51Degrees.core.json");
        assertTrue(service.serveJson(request, response));

        verify(clientside, times(2)).serveJson(request, response);
    }

    @Test
    public void ConfiguredPaths_ServedInsteadOfTheDefaults()
        throws IOException {
        FiftyOneJSServiceCore service = new FiftyOneJSServiceCore.Default(
            clientside, true, "/scripts/51d.js", "/51dpipeline/json");

        requestPath("/scripts/51d.js");
        assertTrue(service.serveJS(request, response));
        requestPath("/51dpipeline/json");
        assertTrue(service.serveJson(request, response));

        requestPath("/51Degrees.core.js");
        assertFalse(service.serveJS(request, response));
        requestPath("/other/json");
        assertFalse(service.serveJson(request, response));

        verify(clientside, times(1)).serveJavascript(request, response);
        verify(clientside, times(1)).serveJson(request, response);
    }

    /**
     * The configured path is the whole path inside the application, so a
     * longer path that ends the same way is not answered.
     */
    @Test
    public void ConfiguredPath_OnlyAnExactMatchIsAnswered()
        throws IOException {
        for (String configured : new String[] {"json", "/json"}) {
            FiftyOneJSServiceCore service = new FiftyOneJSServiceCore.Default(
                clientside, true, configured, configured);

            requestPath("/json");
            assertTrue(configured, service.serveJson(request, response));
            assertTrue(configured, service.serveJS(request, response));
            requestPath("/anything/json");
            assertFalse(configured, service.serveJson(request, response));
            assertFalse(configured, service.serveJS(request, response));
            requestPath("/json/more");
            assertFalse(configured, service.serveJson(request, response));
            requestPath("/notjson");
            assertFalse(configured, service.serveJson(request, response));
        }
    }

    @Test
    public void ContextPath_IsNotPartOfThePath() throws IOException {
        FiftyOneJSServiceCore service =
            new FiftyOneJSServiceCore.Default(clientside, true);

        requestPath("/app", "/51dpipeline/json");
        assertTrue(service.serveJson(request, response));
        requestPath("/app", "/other/51dpipeline/json");
        assertFalse(service.serveJson(request, response));
        requestPath("/app", "/51Degrees.core.js");
        assertTrue(service.serveJS(request, response));
        requestPath("/app", "/other/51Degrees.core.js");
        assertFalse(service.serveJS(request, response));
    }

    @Test
    public void EmptyPaths_UseTheDefaults() throws IOException {
        FiftyOneJSServiceCore service = new FiftyOneJSServiceCore.Default(
            clientside, true, "", null);

        requestPath("/51Degrees.core.js");
        assertTrue(service.serveJS(request, response));
        requestPath("/51dpipeline/json");
        assertTrue(service.serveJson(request, response));
    }

    @Test
    public void ClientSideDisabled_NothingWritten() throws IOException {
        FiftyOneJSServiceCore service = new FiftyOneJSServiceCore.Default(
            clientside, false, "/51d.js", "/51dpipeline/json");

        requestPath("/51d.js");
        assertTrue(service.serveJS(request, response));
        requestPath("/51dpipeline/json");
        assertTrue(service.serveJson(request, response));

        verify(clientside, never()).serveJavascript(request, response);
        verify(clientside, never()).serveJson(request, response);
    }

    // The tests below stub and verify createFlowData on a mock, which
    // creates nothing to close, so the FlowData rule does not apply.

    /**
     * A request with no If-None-Match is left to be processed, and no
     * FlowData is created for it.
     */
    @Test
    @SuppressWarnings("PMD.FlowDataInTryWithResources")
    public void NotModified_NoIfNoneMatch_NothingCreated() throws IOException {
        FiftyOneJSServiceCore service =
            new FiftyOneJSServiceCore.Default(clientside, true);
        PipelineResultServiceCore results =
            mock(PipelineResultServiceCore.class);
        requestPath("/51Degrees.core.js");

        assertFalse(service.serveNotModified(request, response, results));

        verify(results, never()).createFlowData(request);
    }

    /**
     * A request for another path is left to be processed, whatever its
     * If-None-Match.
     */
    @Test
    @SuppressWarnings("PMD.FlowDataInTryWithResources")
    public void NotModified_OtherPath_NothingCreated() throws IOException {
        FiftyOneJSServiceCore service =
            new FiftyOneJSServiceCore.Default(clientside, true);
        PipelineResultServiceCore results =
            mock(PipelineResultServiceCore.class);
        when(request.getHeader("If-None-Match")).thenReturn("*");
        requestPath("/page");

        assertFalse(service.serveNotModified(request, response, results));

        verify(results, never()).createFlowData(request);
    }

    /**
     * With the client-side features off, nothing is answered early.
     */
    @Test
    @SuppressWarnings("PMD.FlowDataInTryWithResources")
    public void NotModified_ClientSideDisabled_NothingCreated()
        throws IOException {
        FiftyOneJSServiceCore service =
            new FiftyOneJSServiceCore.Default(clientside, false);
        PipelineResultServiceCore results =
            mock(PipelineResultServiceCore.class);
        when(request.getHeader("If-None-Match")).thenReturn("*");
        requestPath("/51Degrees.core.js");

        assertFalse(service.serveNotModified(request, response, results));

        verify(results, never()).createFlowData(request);
    }

    /**
     * A revalidation of the JavaScript or the JSON goes to its own endpoint
     * with a FlowData created for it, which is closed afterwards.
     */
    @Test
    @SuppressWarnings("PMD.FlowDataInTryWithResources")
    public void NotModified_JavaScriptAndJson_AskTheirEndpoint()
        throws Exception {
        FiftyOneJSServiceCore service =
            new FiftyOneJSServiceCore.Default(clientside, true);
        PipelineResultServiceCore results =
            mock(PipelineResultServiceCore.class);
        FlowData flowData = mock(FlowData.class);
        when(results.createFlowData(request)).thenReturn(flowData);
        when(request.getHeader("If-None-Match")).thenReturn("*");
        when(clientside.serveJavascriptNotModified(flowData, request, response))
            .thenReturn(true);
        when(clientside.serveJsonNotModified(flowData, request, response))
            .thenReturn(true);

        requestPath("/51Degrees.core.js");
        assertTrue(service.serveNotModified(request, response, results));
        requestPath("/51dpipeline/json");
        assertTrue(service.serveNotModified(request, response, results));

        verify(clientside, times(1))
            .serveJavascriptNotModified(flowData, request, response);
        verify(clientside, times(1))
            .serveJsonNotModified(flowData, request, response);
        verify(flowData, times(2)).close();
    }
}
