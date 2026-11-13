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

package fiftyone.pipeline.web.mvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fiftyone.pipeline.core.data.FlowData;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElement;
import fiftyone.pipeline.engines.fiftyone.flowelements.SetHeadersElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.web.mvc.components.FiftyOneInterceptor;
import fiftyone.pipeline.web.mvc.components.FlowDataProvider;
import fiftyone.pipeline.web.mvc.configuration.FiftyOneInterceptorConfig;
import fiftyone.pipeline.web.mvc.services.ClientsidePropertyService;
import fiftyone.pipeline.web.mvc.services.FiftyOneJSService;
import fiftyone.pipeline.web.mvc.services.PipelineResultService;
import fiftyone.pipeline.web.mvc.services.UACHService;

public class FiftyOneInterceptorTests {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Object handler;
    
    private FiftyOneInterceptor interceptor;
    private PipelineResultService resultService;
    private FlowData flowData;
    
    @Before
    public void init() throws IOException {
        FiftyOneInterceptorConfig config = 
            mock(FiftyOneInterceptorConfig.class);
        when(config.getDataFilePath())
            .thenReturn("src/test/resources/Test.xml");
        
        FlowDataProvider flowDataProvider = mock(FlowDataProvider.class);
        ClientsidePropertyService clientsidePropertyService = 
            mock(ClientsidePropertyService.class);
        
        FiftyOneJSService fiftyOneJsService = mock(FiftyOneJSService.class);
        when(fiftyOneJsService.serveJS(
                any(HttpServletRequest.class), 
                any(HttpServletResponse.class)))
            .thenReturn(true);
        when(fiftyOneJsService.serveJson(
                any(HttpServletRequest.class), 
                any(HttpServletResponse.class)))
            .thenReturn(true);
        
        UACHService uachService = mock(UACHService.class);
        
        resultService = mock(PipelineResultService.class);
        flowData = mock(FlowData.class);
        
        // Mock request, response and handler object
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        handler = mock(Object.class);
        
        when(flowDataProvider.getFlowData(any(HttpServletRequest.class)))
            .thenReturn(flowData);
        
        // Create the interceptor object for testing
        interceptor = new FiftyOneInterceptor(
                config, 
                resultService, 
                flowDataProvider, 
                clientsidePropertyService, 
                fiftyOneJsService,
                uachService);
    }
    
    /**
     * Check that 'resultService.process()' is called during
     * interceptor's preHandle process
     * @throws Exception
     */
    @Test
    public void FiftyOneInterceptor_PreHandle() throws Exception {
        interceptor.preHandle(request, response, handler);
        verify(resultService, times(1))
            .process(any(HttpServletRequest.class));
    }
    
    /**
     * Check that 'close()' is called on the FlowData instance in the 
     * interceptor's afterCompletion processed
     * @throws Exception
     */
    @Test
    public void FiftyOneInterceptor_AfterCompletion() throws Exception {
        interceptor.afterCompletion(
                request, 
                response, 
                handler, 
                new Exception());
        verify(flowData, times(1))
            .close();
    }

    /**
     * Check that the FlowData is closed when preHandle serves the JavaScript
     * itself, as Spring does not call afterCompletion when preHandle returns
     * false.
     * @throws Exception
     */
    @Test
    public void FiftyOneInterceptor_PreHandle_ServesJavaScript_ClosesFlowData()
        throws Exception {
        assertFalse(interceptor.preHandle(request, response, handler));
        verify(flowData, times(1))
            .close();
    }

    private FiftyOneInterceptor newInterceptor(
        FiftyOneInterceptorConfig config,
        FiftyOneJSService jsService) throws IOException {
        return new FiftyOneInterceptor(
            config,
            mock(PipelineResultService.class),
            mock(FlowDataProvider.class),
            mock(ClientsidePropertyService.class),
            jsService,
            mock(UACHService.class));
    }

    private FiftyOneInterceptorConfig newConfig(
        boolean sequence,
        boolean setHeaders,
        String jsonPath,
        String javaScriptPath) {
        FiftyOneInterceptorConfig config = mock(FiftyOneInterceptorConfig.class);
        when(config.getDataFilePath())
            .thenReturn("src/test/resources/NoSequence.xml");
        when(config.getClientsidePropertiesEnabled()).thenReturn(true);
        when(config.getSequenceElementEnabled()).thenReturn(sequence);
        when(config.getSetHeadersElementEnabled()).thenReturn(setHeaders);
        when(config.getJsonPath()).thenReturn(jsonPath);
        when(config.getJavaScriptPath()).thenReturn(javaScriptPath);
        return config;
    }

    private static boolean hasElement(
        FiftyOneInterceptor interceptor,
        Class<?> type) {
        for (FlowElement<?, ?> element : interceptor.pipeline.getFlowElements()) {
            if (type.isInstance(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check that both elements are added when the configuration allows it.
     */
    @Test
    public void FiftyOneInterceptor_ElementsEnabled_Added() throws Exception {
        FiftyOneInterceptor created = newInterceptor(
            newConfig(true, true, null, null), mock(FiftyOneJSService.class));

        assertTrue(hasElement(created, SequenceElement.class));
        assertTrue(hasElement(created, SetHeadersElement.class));
    }

    /**
     * Check that the switches stop each element being added.
     */
    @Test
    public void FiftyOneInterceptor_ElementsDisabled_NotAdded()
        throws Exception {
        FiftyOneInterceptorConfig config = newConfig(false, false, null, null);
        when(config.getClientsidePropertiesEnabled()).thenReturn(false);
        when(config.getDataFilePath())
            .thenReturn("src/test/resources/NoJson.xml");
        FiftyOneInterceptor created = newInterceptor(
            config, mock(FiftyOneJSService.class));

        assertFalse(hasElement(created, SequenceElement.class));
        assertFalse(hasElement(created, SetHeadersElement.class));
    }

    /**
     * Check that the build is refused when the JSON Builder Element would
     * have no Sequence Element before it.
     */
    @Test
    public void FiftyOneInterceptor_NoSequenceElementWithJson_Refused() {
        RuntimeException e = assertThrows(RuntimeException.class,
            () -> newInterceptor(
                newConfig(false, true, null, null),
                mock(FiftyOneJSService.class)));

        assertTrue(e.getCause().getMessage().contains("Sequence Element"));
    }

    /**
     * Check that the JSON path is the JavaScript Builder Element's callback
     * endpoint and, with the JavaScript path, goes to the service that serves
     * them.
     */
    @Test
    public void FiftyOneInterceptor_Paths_UsedByBuilderAndService()
        throws Exception {
        FiftyOneJSService jsService = mock(FiftyOneJSService.class);
        FiftyOneInterceptor created = newInterceptor(
            newConfig(true, true, "/51dpipeline/json", "/scripts/51d.js"),
            jsService);

        verify(jsService).setPaths("/scripts/51d.js", "/51dpipeline/json");
        for (FlowElement<?, ?> element : created.pipeline.getFlowElements()) {
            if (element instanceof JavaScriptBuilderElement) {
                Field field =
                    JavaScriptBuilderElement.class.getDeclaredField("endpoint");
                field.setAccessible(true);
                assertEquals("/51dpipeline/json", field.get(element));
            }
        }
    }

    /**
     * Check that no paths in the configuration mean the default paths.
     */
    @Test
    public void FiftyOneInterceptor_NoPaths_DefaultPaths() throws Exception {
        FiftyOneJSService jsService = mock(FiftyOneJSService.class);
        newInterceptor(newConfig(true, true, null, null), jsService);

        verify(jsService).setPaths(null, "/51dpipeline/json");
    }

    /**
     * Check that the paths the application can call are mapped, the old JSON
     * path included.
     */
    @Test
    public void FiftyOneInterceptor_EnableClientsideProperties_MapsBothJsonPaths() {
        ViewControllerRegistry registry = mock(ViewControllerRegistry.class);

        FiftyOneInterceptor.enableClientsideProperties(registry);

        verify(registry).addViewController("/51Degrees.core.js");
        verify(registry).addViewController("/51dpipeline/json");
        verify(registry).addViewController("/51Degrees.core.json");
    }
}
