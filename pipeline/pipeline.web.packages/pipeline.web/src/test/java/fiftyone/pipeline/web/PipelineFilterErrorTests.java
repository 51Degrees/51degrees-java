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
import fiftyone.pipeline.web.services.FiftyOneJSServiceCore;
import fiftyone.pipeline.web.services.FlowDataProviderCore;
import fiftyone.pipeline.web.services.PipelineResultServiceCore;
import fiftyone.pipeline.web.services.UACHServiceCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import static fiftyone.pipeline.web.Constants.HTTPCONTEXT_FLOWDATA;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * An error in the web integration does not stop the customer's request
 * when the Pipeline suppresses process exceptions: it is logged and the
 * request goes on down the filter chain. When the Pipeline does not
 * suppress them, the error is thrown as before. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/exception-handling.md#L73">exception-handling.md line 73</a>:
 * "The implementor MUST ensure that exceptions/errors that happen in these
 * areas are handled correctly in order to avoid the API taking down the
 * customer's production website." and line 42, which gives
 * IsSuppressProcessExceptions to wrappers "to make a decision on whether the
 * exceptions should be suppressed and logged during processing."
 */
public class PipelineFilterErrorTests {

    private PipelineFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private Object storedFlowData;

    @Before
    public void init() throws Exception {
        FilterConfig config = mock(FilterConfig.class);
        when(config.getInitParameter(anyString())).thenAnswer(
            (Answer<String>) invocation ->
                "config-file".equals(invocation.getArgument(0)) ?
                    "filename" : null);
        ServletContext context = mock(ServletContext.class);
        File file = new File(getClass().getClassLoader()
            .getResource("config.xml").getFile());
        when(context.getRealPath(anyString()))
            .thenReturn(file.getAbsolutePath());
        when(config.getServletContext()).thenReturn(context);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        when(request.getScheme()).thenReturn("");
        when(request.getLocalAddr()).thenReturn("");
        when(request.getRequestURL()).thenReturn(new StringBuffer(""));
        when(request.getRequestURI()).thenReturn("");
        when(request.getAttribute(eq(HTTPCONTEXT_FLOWDATA)))
            .thenAnswer(invocation -> storedFlowData);
        org.mockito.Mockito.doAnswer(invocation -> {
            storedFlowData = invocation.getArgument(1);
            return null;
        }).when(request).setAttribute(eq(HTTPCONTEXT_FLOWDATA), any());

        filter = new PipelineFilter();
        filter.init(config);
    }

    @After
    public void cleanUp() {
        if (filter != null) {
            filter.destroy();
        }
    }

    private void set(String name, Object value) throws Exception {
        Field field = PipelineFilter.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(filter, value);
    }

    private Object get(String name) throws Exception {
        Field field = PipelineFilter.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(filter);
    }

    /**
     * Give the filter a Pipeline that reports the setting, keeping the
     * real one to close.
     */
    private void suppress(boolean value) throws Exception {
        Pipeline real = (Pipeline) get("pipeline");
        Pipeline reporting = mock(Pipeline.class,
            org.mockito.AdditionalAnswers.delegatesTo(real));
        when(reporting.isSuppressProcessExceptions()).thenReturn(value);
        set("pipeline", reporting);
    }

    private void failProcessing(RuntimeException failure) throws Exception {
        PipelineResultServiceCore failing =
            mock(PipelineResultServiceCore.class);
        doThrow(failure).when(failing).process(any(HttpServletRequest.class));
        set("resultService", failing);
    }

    @Test
    public void processingFails_Suppressed_RequestGoesOn() throws Exception {
        suppress(true);
        failProcessing(new IllegalStateException("processing failed"));

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(
            any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    public void headersFail_Suppressed_RequestGoesOn() throws Exception {
        suppress(true);
        UACHServiceCore failing = mock(UACHServiceCore.class);
        doThrow(new IllegalStateException("headers failed")).when(failing)
            .setResponseHeaders(any(FlowData.class),
                any(HttpServletResponse.class));
        set("uachServiceCore", failing);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(
            any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    public void processingFails_NotSuppressed_IsThrown() throws Exception {
        suppress(false);
        IllegalStateException failure =
            new IllegalStateException("processing failed");
        failProcessing(failure);

        try {
            filter.doFilter(request, response, chain);
            fail("The failure was not thrown.");
        } catch (IllegalStateException e) {
            assertSame(failure, e);
        } catch (ServletException e) {
            fail("The failure was wrapped: " + e);
        }
    }

    /**
     * Serving the 51Degrees JavaScript fails with an IOException, for
     * example when the client has gone away. The failure is thrown, and the
     * Flow Data is still closed, so its Element Data and native results are
     * released.
     */
    @Test
    public void servingJavaScriptFails_IOException_FlowDataClosed()
        throws Exception {
        suppress(true);
        set("resultService", mock(PipelineResultServiceCore.class));
        set("uachServiceCore", mock(UACHServiceCore.class));
        FlowData flowData = mock(FlowData.class);
        FlowDataProviderCore provider = mock(FlowDataProviderCore.class);
        when(provider.getFlowData(any(HttpServletRequest.class)))
            .thenReturn(flowData);
        set("flowDataProviderCore", provider);
        FiftyOneJSServiceCore js = mock(FiftyOneJSServiceCore.class);
        IOException failure = new IOException("client went away");
        when(js.serveJS(any(HttpServletRequest.class),
            any(HttpServletResponse.class))).thenThrow(failure);
        set("jsService", js);

        try {
            filter.doFilter(request, response, chain);
            fail("The failure was not thrown.");
        } catch (IOException e) {
            assertSame(failure, e);
        }

        verify(flowData, times(1)).close();
        verify(chain, never()).doFilter(
            any(ServletRequest.class), any(ServletResponse.class));
    }
}
