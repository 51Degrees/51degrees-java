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
         * @param javaScriptPath the end of the request path that the
         *                       JavaScript is served for, or null or empty
         *                       for the default
         * @param jsonPath the end of the request path that the JSON is
         *                 served for, or null or empty for the default
         */
        public Default(
            ClientsidePropertyServiceCore clientsidePropertyServiceCore,
            boolean enabled,
            String javaScriptPath,
            String jsonPath) {
            this.clientsidePropertyServiceCore = clientsidePropertyServiceCore;
            this.enabled = enabled;
            setPaths(javaScriptPath, jsonPath);
        }

        /**
         * Set the paths the JavaScript and the JSON are served from.
         * @param javaScriptPath the end of the request path that the
         *                       JavaScript is served for, or null or empty
         *                       for the default
         * @param jsonPath the end of the request path that the JSON is
         *                 served for, or null or empty for the default
         */
        public void setPaths(String javaScriptPath, String jsonPath) {
            this.javaScriptPath = javaScriptPath == null ||
                javaScriptPath.isEmpty() ? CORE_JS_NAME : javaScriptPath;
            this.jsonPath = jsonPath == null || jsonPath.isEmpty() ?
                CORE_JSON_NAME : jsonPath;
        }

        @Override
        public boolean serveJS(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
            boolean result = false;
            if (request.getRequestURL().toString().toLowerCase()
                .endsWith(javaScriptPath.toLowerCase())) {
                serveCoreJS(request, response);
                result = true;
            }
            return result;
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
            if (request.getRequestURI().toString().toLowerCase()
                .endsWith(jsonPath.toLowerCase())) {
                serveCoreJson(request, response);
                result = true;
            }

            return result;
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
