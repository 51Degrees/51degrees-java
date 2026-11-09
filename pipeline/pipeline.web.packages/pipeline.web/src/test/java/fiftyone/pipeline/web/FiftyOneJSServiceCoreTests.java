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

import fiftyone.pipeline.web.services.ClientsidePropertyServiceCore;
import fiftyone.pipeline.web.services.FiftyOneJSServiceCore;
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
        when(request.getRequestURL())
            .thenReturn(new StringBuffer("http://example.com" + path));
        when(request.getRequestURI()).thenReturn(path);
    }

    @Test
    public void DefaultPaths_JavaScriptAndJsonServed() throws IOException {
        FiftyOneJSServiceCore service =
            new FiftyOneJSServiceCore.Default(clientside, true);

        requestPath("/51Degrees.core.js");
        assertTrue(service.serveJS(request, response));
        requestPath("/51Degrees.core.json");
        assertTrue(service.serveJson(request, response));

        verify(clientside, times(1)).serveJavascript(request, response);
        verify(clientside, times(1)).serveJson(request, response);
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
        requestPath("/51Degrees.core.json");
        assertFalse(service.serveJson(request, response));

        verify(clientside, times(1)).serveJavascript(request, response);
        verify(clientside, times(1)).serveJson(request, response);
    }

    @Test
    public void EmptyPaths_UseTheDefaults() throws IOException {
        FiftyOneJSServiceCore service = new FiftyOneJSServiceCore.Default(
            clientside, true, "", null);

        requestPath("/51Degrees.core.js");
        assertTrue(service.serveJS(request, response));
        requestPath("/51Degrees.core.json");
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
}
