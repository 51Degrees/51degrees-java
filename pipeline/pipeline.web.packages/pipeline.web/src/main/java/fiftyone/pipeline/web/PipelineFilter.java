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

import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.DataUpdateService;
import fiftyone.pipeline.engines.services.DataUpdateServiceDefault;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import fiftyone.pipeline.web.services.*;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;

import static fiftyone.pipeline.web.Constants.DEFAULT_JSON_PATH;
import static fiftyone.pipeline.web.Constants.CORE_JS_NAME;
import static fiftyone.pipeline.web.Constants.DEFAULT_CLIENTSIDE_CACHE_MAX_AGE_SECONDS;
import static fiftyone.pipeline.web.Constants.DEFAULT_CLIENTSIDE_ENABLED;
import static fiftyone.pipeline.web.Constants.DEFAULT_CONFIG_FILE;
import static fiftyone.pipeline.web.Constants.DEFAULT_ELEMENT_ENABLED;
import static fiftyone.pipeline.web.Constants.INIT_PARAM_CLIENTSIDE_CACHE_MAX_AGE;
import static fiftyone.pipeline.web.Constants.INIT_PARAM_JAVASCRIPT_PATH;
import static fiftyone.pipeline.web.Constants.INIT_PARAM_JSON_PATH;
import static fiftyone.pipeline.web.Constants.INIT_PARAM_SEQUENCE_ELEMENT_ENABLED;
import static fiftyone.pipeline.web.Constants.INIT_PARAM_SET_HEADERS_ELEMENT_ENABLED;

/**
 * Servlet filter used to intercept HTTP requests and process them using the
 * 51Degrees Pipeline.
 * <p>
 * A Flow Data cannot be used by two threads at once. Application code reads
 * the values it needs from the Flow Data on the request's thread, and hands
 * those values, not the Flow Data, to any other thread or task. The Flow
 * Data is closed when the request ends.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/web-integration.md">Specification</a>
 */
public class PipelineFilter implements Filter {
    /** Create a new instance. */
    public PipelineFilter() {
    }

    private static final org.slf4j.Logger logger =
        LoggerFactory.getLogger(PipelineFilter.class);

    private Pipeline pipeline;

    private WebRequestEvidenceServiceCore evidenceService;

    private PipelineResultServiceCore resultService;

    private FlowDataProviderCore flowDataProviderCore;

    private ClientsidePropertyServiceCore clientsidePropertyServiceCore;

    private FiftyOneJSServiceCore jsService;

    private UACHServiceCore uachServiceCore;
    
    FilterConfig config;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
        String configFileName = filterConfig.getInitParameter("config-file");
        if (configFileName == null || configFileName.isEmpty()) {
            configFileName = DEFAULT_CONFIG_FILE;
        }
        boolean clientsideEnabled;
        String clientsideEnabledString = filterConfig.getInitParameter("clientside-properties-enabled");
        if (clientsideEnabledString == null) {
            clientsideEnabled = DEFAULT_CLIENTSIDE_ENABLED;
        }
        else {
            clientsideEnabled = Boolean.parseBoolean(clientsideEnabledString);
        }
        int cacheMaxAgeSeconds = getCacheMaxAgeSeconds(filterConfig);
        
        boolean sequenceElementEnabled = getBooleanInitParameter(
            filterConfig,
            INIT_PARAM_SEQUENCE_ELEMENT_ENABLED,
            DEFAULT_ELEMENT_ENABLED);
        boolean setHeadersElementEnabled = getBooleanInitParameter(
            filterConfig,
            INIT_PARAM_SET_HEADERS_ELEMENT_ENABLED,
            DEFAULT_ELEMENT_ENABLED);
        String javaScriptPath = getPathInitParameter(
            filterConfig, INIT_PARAM_JAVASCRIPT_PATH, CORE_JS_NAME);
        String jsonPath = getPathInitParameter(
            filterConfig, INIT_PARAM_JSON_PATH, DEFAULT_JSON_PATH);

        ServletContext context = config.getServletContext();

        File configFile = new File(context.getRealPath(configFileName));
        PipelineBuilder builder = new PipelineBuilder()
            .addService(new DataUpdateServiceDefault(
                LoggerFactory.getLogger(DataUpdateService.class.getSimpleName()),
                new HttpClientDefault()));

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PipelineOptions.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            // Bind the configuration to a pipeline options instance
            PipelineOptions options = (PipelineOptions) unmarshaller.unmarshal(configFile);

            pipeline = StartupHelpers.buildFromConfiguration(builder, options, 
                clientsideEnabled, context.getContextPath(), jsonPath,
                sequenceElementEnabled, setHeadersElementEnabled);
        } catch (Exception e) {
            throw new ServletException(e);
        }

        evidenceService = new WebRequestEvidenceServiceCore.Default();
        resultService = new PipelineResultServiceCore.Default(evidenceService, pipeline);
        flowDataProviderCore = new FlowDataProviderCore.Default();
        uachServiceCore = new UACHServiceCore.Default();
        ClientsidePropertyServiceCore.Default clientside =
            new ClientsidePropertyServiceCore.Default(
                flowDataProviderCore,
                pipeline);
        clientside.setCacheMaxAge(cacheMaxAgeSeconds);
        clientsidePropertyServiceCore = clientside;
        jsService = new FiftyOneJSServiceCore.Default(
            clientsidePropertyServiceCore,
            clientsideEnabled,
            javaScriptPath,
            jsonPath);
    }

    private static boolean getBooleanInitParameter(
        FilterConfig filterConfig,
        String name,
        boolean defaultValue) {
        String value = filterConfig.getInitParameter(name);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    private static String getPathInitParameter(
        FilterConfig filterConfig,
        String name,
        String defaultValue) {
        String value = filterConfig.getInitParameter(name);
        return value == null || value.trim().isEmpty() ?
            defaultValue : value.trim();
    }

    /**
     * The max-age of the client-side endpoints' Cache-Control header, in
     * seconds, from the filter's init parameter, or the default when the
     * parameter is not set.
     * @param filterConfig the filter's configuration
     * @return the max-age in seconds
     * @throws ServletException if the parameter is not a whole number of
     * seconds, 0 or more
     */
    static int getCacheMaxAgeSeconds(FilterConfig filterConfig)
        throws ServletException {
        String value = filterConfig.getInitParameter(
            INIT_PARAM_CLIENTSIDE_CACHE_MAX_AGE);
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_CLIENTSIDE_CACHE_MAX_AGE_SECONDS;
        }
        try {
            int seconds = Integer.parseInt(value.trim());
            if (seconds >= 0) {
                return seconds;
            }
        } catch (NumberFormatException e) {
            // Reported below, as a negative number is.
        }
        throw new ServletException("The '" +
            INIT_PARAM_CLIENTSIDE_CACHE_MAX_AGE + "' init parameter must " +
            "be a whole number of seconds, 0 or more, and was '" + value +
            "'.");
    }

    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        boolean suppress = isSuppressProcessExceptions();

        boolean served;
        try {
            served = processRequest(httpRequest, httpResponse);
        } catch (RuntimeException e) {
            // The Pipeline's setting decides whether an error in the web
            // integration stops the request. When exceptions are suppressed
            // it is logged and the request goes on without the results, so
            // the API does not take down the website. See
            // https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/exception-handling.md#L73
            closeFlowData(httpRequest, suppress, e);
            if (suppress == false) {
                throw e;
            }
            logger.error("The 51Degrees Pipeline could not process the " +
                "request, which goes on without its results.", e);
            served = false;
        } catch (IOException e) {
            // Writing the JavaScript or JSON failed, for example because the
            // client went away. The request ends here, so the FlowData is
            // closed before the failure is thrown.
            closeFlowData(httpRequest, suppress, e);
            throw e;
        }
        if (served) {
            closeFlowData(httpRequest, suppress, null);
            return;
        }

        try {
            chain.doFilter(request, response);//sends request to next resource
        } finally {
            // The rest of the filters in the chain have now been called, or
            // one of them failed. Either way it is time to dispose of the
            // FlowData instance.
            closeFlowData(httpRequest, suppress, null);
        }
    }

    /**
     * Whether the Pipeline suppresses process exceptions, which also
     * decides whether an error in this filter stops the request.
     * @return true if exceptions are suppressed and logged
     */
    private boolean isSuppressProcessExceptions() {
        return pipeline != null && pipeline.isSuppressProcessExceptions();
    }

    /**
     * Process the request and set the response headers, and serve the
     * 51Degrees JavaScript or JSON if that is what is asked for.
     * @param request the request
     * @param response the response
     * @return true if the response has been served, so the request does not
     * go on down the filter chain
     * @throws IOException if the JavaScript or JSON could not be written
     */
    private boolean processRequest(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException {
        // A request for the 51Degrees JavaScript or JSON whose If-None-Match
        // matches the ETag of its Evidence is answered without processing,
        // because that Evidence alone decides the content.
        if (jsService.serveNotModified(request, response, resultService)) {
            return true;
        }

        // Populate the request properties and store against the
        // HttpContext.
        resultService.process(request);

        // Get FlowData from the request.
        FlowData flowData = flowDataProviderCore.getFlowData(request);

        // Set UACH response headers.
        uachServiceCore.setResponseHeaders(flowData, response);

        // If 51Degrees JavaScript or JSON is being requested then serve it.
        // Otherwise the request continues down the filter chain.
        return jsService.serveJS(request, response) ||
            jsService.serveJson(request, response);
    }

    /**
     * Close the request's FlowData, if processing put one there.
     * @param request the request
     * @param suppress true if a failure to close is logged rather than
     *                 thrown
     * @param failure the failure already being handled, which a failure to
     *                close is added to, or null
     * @throws ServletException if the FlowData could not be closed and
     * exceptions are not suppressed
     */
    private void closeFlowData(
        HttpServletRequest request,
        boolean suppress,
        Exception failure) throws ServletException {
        FlowData flowData;
        try {
            flowData = flowDataProviderCore.getFlowData(request);
        } catch (RuntimeException e) {
            flowData = null;
        }
        if (flowData == null) {
            return;
        }
        try {
            flowData.close();
        } catch (Exception e) {
            if (failure != null) {
                failure.addSuppressed(e);
            } else if (suppress) {
                logger.error("FlowData could not be disposed of.", e);
            } else {
                throw new ServletException(
                    "FlowData could not be disposed of.", e);
            }
        }
    }

    @Override
    public void destroy() {
        try {
            pipeline.close();
        } catch (Exception e) {
            // Nothing to be done here as everything is closing anyway.
        }
    }
}
