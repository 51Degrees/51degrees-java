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

package fiftyone.pipeline.web.services;

import fiftyone.pipeline.core.data.FlowData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static fiftyone.pipeline.web.Constants.CORE_JS_NAME;
import static fiftyone.pipeline.web.Constants.CORE_JSON_NAME;
import static fiftyone.pipeline.web.Constants.DEFAULT_JSON_PATH;

/**
 * Service that provides the 51Degrees javascript when requested.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/web-integration.md#client-side-features">Specification</a>
 */
public interface FiftyOneJSServiceCore {

    /**
     * Check if the 51Degrees JavaScript is being requested and write it to the
     * response if it is.
     * @param request the {@link HttpServletRequest} to get the {@link FlowData}
     *                from
     * @param response the {@link HttpServletResponse} to write the JavaScript
     *                 to
     * @return true if JavaScript was written to the response, false otherwise
     * @throws IOException if there was a failure reading or writing to the
     * request or response
     */
    boolean serveJS(HttpServletRequest request, HttpServletResponse response)
        throws IOException;

    /**
     * Check if the 51Degrees JSON is being requested and
     * write it to the response if it is
     * @param request the {@link HttpServletRequest} to get the {@link FlowData}
     *                from
     * @param response the {@link HttpServletResponse} to write the JSON
     *                 to
     * @return true if JSON was written to the response, false otherwise
     * @throws IOException if there was a failure reading or writing to the
     * request or response
     */
    boolean serveJson(HttpServletRequest request, HttpServletResponse response)
        throws IOException;

    /**
     * Answer a request for the 51Degrees JavaScript or JSON whose
     * If-None-Match matches the ETag of the Evidence it supplies, without
     * processing it, because that Evidence alone decides the content. A GET
     * or HEAD gets a 304 and any other method a 412. Call it before the
     * request is processed.
     * @param request the {@link HttpServletRequest} to answer
     * @param response the {@link HttpServletResponse} to write the answer to
     * @param resultService the service that creates the request's
     *                      {@link FlowData}, with the Evidence processing
     *                      would use
     * @return true if the request was answered and is not to be processed,
     * false otherwise
     * @throws IOException if there was a failure reading or writing to the
     * request or response
     */
    default boolean serveNotModified(
        HttpServletRequest request,
        HttpServletResponse response,
        PipelineResultServiceCore resultService) throws IOException {
        return false;
    }

    /**
     * Default implementation of the {@link FiftyOneJSServiceCore} service.
     */
    class Default implements FiftyOneJSServiceCore {
        protected final ClientsidePropertyServiceCore clientsidePropertyServiceCore;
        protected boolean enabled;
        protected String javaScriptPath;
        protected String jsonPath;

        /**
         * Construct a new instance that serves the JavaScript and the JSON
         * from the default paths.
         * @param clientsidePropertyServiceCore used to get the JavaScript to
         *                                      add to the returned file
         * @param enabled true if the service should be enabled
         */
        public Default(
            ClientsidePropertyServiceCore clientsidePropertyServiceCore,
            boolean enabled) {
            this(clientsidePropertyServiceCore, enabled, null, null);
        }

        /**
         * Construct a new instance.
         * @param clientsidePropertyServiceCore used to get the JavaScript to
         *                                      add to the returned file
         * @param enabled true if the service should be enabled
         * @param javaScriptPath the path inside the application that the
         *                       JavaScript is served for, or null or empty
         *                       for the default
         * @param jsonPath the path inside the application that the JSON
         *                 is served for, or null or empty for the default
         */
        public Default(
            ClientsidePropertyServiceCore clientsidePropertyServiceCore,
            boolean enabled,
            String javaScriptPath,
            String jsonPath) {
            this.clientsidePropertyServiceCore = clientsidePropertyServiceCore;
            this.enabled = enabled;
            this.javaScriptPath = orDefault(javaScriptPath, CORE_JS_NAME);
            this.jsonPath = orDefault(jsonPath, DEFAULT_JSON_PATH);
        }

        private static String orDefault(String path, String defaultPath) {
            return path == null || path.isEmpty() ? defaultPath : path;
        }

        /**
         * Set the paths the JavaScript and the JSON are served from.
         * @param javaScriptPath the path inside the application that the
         *                       JavaScript is served for, or null or empty
         *                       for the default
         * @param jsonPath the path inside the application that the JSON
         *                 is served for, or null or empty for the default
         */
        public void setPaths(String javaScriptPath, String jsonPath) {
            this.javaScriptPath = orDefault(javaScriptPath, CORE_JS_NAME);
            this.jsonPath = orDefault(jsonPath, DEFAULT_JSON_PATH);
        }

        @Override
        public boolean serveJS(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
            boolean result = false;
            if (matches(request, javaScriptPath)) {
                serveCoreJS(request, response);
                result = true;
            }
            return result;
        }

        /**
         * Whether the request is for the path. The path is compared with
         * the whole path of the request inside the application, after the
         * context path, ignoring case and with or without a leading slash.
         */
        private static boolean matches(
            HttpServletRequest request,
            String path) {
            String requested = request.getRequestURI();
            if (requested == null) {
                return false;
            }
            String contextPath = request.getContextPath();
            if (contextPath != null && requested.startsWith(contextPath)) {
                requested = requested.substring(contextPath.length());
            }
            return withSlash(requested).equalsIgnoreCase(withSlash(path));
        }

        private static String withSlash(String path) {
            return path.startsWith("/") ? path : "/" + path;
        }

        private void serveCoreJS(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
            if (enabled) {
                clientsidePropertyServiceCore.serveJavascript(request, response);
            }
        }

        @Override
        public boolean serveJson(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
            if (request == null) {
                throw new IllegalArgumentException("request");
            }
            if (response == null) {
                throw new IllegalArgumentException("response");
            }


            boolean result = false;
            if (matches(request, jsonPath) ||
                matches(request, CORE_JSON_NAME)) {
                serveCoreJson(request, response);
                result = true;
            }

            return result;
        }

        @Override
        public boolean serveNotModified(
            HttpServletRequest request,
            HttpServletResponse response,
            PipelineResultServiceCore resultService) throws IOException {
            // Most requests carry no If-None-Match, and need nothing more.
            if (enabled == false ||
                request.getHeader("If-None-Match") == null) {
                return false;
            }
            boolean javaScript = matches(request, javaScriptPath);
            if (javaScript == false &&
                matches(request, jsonPath) == false &&
                matches(request, CORE_JSON_NAME) == false) {
                return false;
            }
            try (FlowData flowData = resultService.createFlowData(request)) {
                if (flowData == null) {
                    return false;
                }
                return javaScript ?
                    clientsidePropertyServiceCore.serveJavascriptNotModified(
                        flowData, request, response) :
                    clientsidePropertyServiceCore.serveJsonNotModified(
                        flowData, request, response);
            } catch (IOException | RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("FlowData could not be disposed of.", e);
            }
        }

        private void serveCoreJson(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
            if (enabled) {
                clientsidePropertyServiceCore.serveJson(request, response);
            }
        }
    }
}
