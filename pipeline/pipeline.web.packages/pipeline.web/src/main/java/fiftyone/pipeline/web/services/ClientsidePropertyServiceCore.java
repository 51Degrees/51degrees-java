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

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.fiftyone.data.ShareUsageEvidence;
import fiftyone.pipeline.javascriptbuilder.flowelements.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static fiftyone.pipeline.core.Constants.EVIDENCE_HTTPHEADER_PREFIX;
import static fiftyone.pipeline.core.Constants.EVIDENCE_SEPERATOR;
import static fiftyone.pipeline.web.Constants.DEFAULT_CLIENTSIDE_CACHE_MAX_AGE_SECONDS;
import fiftyone.pipeline.javascriptbuilder.data.JavaScriptBuilderData;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElement;

import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Class that provides functionality for the 'Client side overrides' feature.
 * Client side overrides allow JavaScript running on the client device to
 * provide additional evidence in the form of cookies or query string parameters
 * to the pipeline on subsequent requests. This enables more detailed
 * information to be supplied to the application. (e.g. iPhone model for device
 * detection).
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/web-integration.md#client-side-features">Specification</a>
 */
public interface ClientsidePropertyServiceCore {

    /**
     * Add the JavaScript from the {@link FlowData} object to the
     * {@link HttpServletResponse}
     * @param request the {@link HttpServletRequest} containing the
     * {@link FlowData}
     * @param response the {@link HttpServletResponse} to add the JavaScript to
     * @throws IOException if there was a failure reading or writing to the
     * request or response
     */
    void serveJavascript(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException;

    /**
     * Add the JSON from the {@link FlowData} object to the
     * {@link HttpServletResponse}
     * @param request the {@link HttpServletRequest} containing the
     * {@link FlowData}
     * @param response the {@link HttpServletResponse} to add the JSON to
     * @throws IOException if there was a failure reading or writing to the
     * request or response
     */
    void serveJson(
        HttpServletRequest request,
        HttpServletResponse response) throws IOException;

    /**
     * Default implementation of the {@link ClientsidePropertyServiceCore}
     * interface.
     */
    class Default implements ClientsidePropertyServiceCore {

        private enum ContentTypes {
            JavaScript,
            Json
        }

        /**
         * Character used by profile override logic to separate profile IDs.
         */
        protected static final char PROFILE_OVERRIDES_SPLITTER = '|';

        /**
         * Provider to get the {@link FlowData} from.
         */
        private final FlowDataProviderCore flowDataProviderCore;

        /**
         * The Pipeline in the server instance.
         */
        protected Pipeline pipeline;

        /**
         * A list of all the HTTP headers that are requested evidence for
         * elements that populate JavaScript properties.
         */
        private List<String> headersAffectingJavaScript;

        /**
         * The cache control value for a good response, which only the
         * browser may keep, for the configured max-age.
         */
        private volatile String cacheControl =
            cacheControl(DEFAULT_CLIENTSIDE_CACHE_MAX_AGE_SECONDS);

        /**
         * The cache control value for a response built while processing
         * recorded errors. A browser may keep it but must ask before using
         * it again, and with no ETag the next request gets a full response.
         */
        private static final String FAILED_CACHE_CONTROL = "private, no-cache";

        /**
         * The digest the ETag holds. Every Java platform provides it.
         */
        private static final String ETAG_DIGEST = "SHA-256";

        private static final char[] HEX = "0123456789abcdef".toCharArray();

        /**
         * Create a new instance.
         * @param flowDataProviderCore the provider to the {@link FlowData} for
         *                             a request from
         * @param pipeline the Pipeline in the server instance
         */
        public Default(
            FlowDataProviderCore flowDataProviderCore,
            Pipeline pipeline) {
            this.flowDataProviderCore = flowDataProviderCore;
            this.pipeline = pipeline;
            if (pipeline != null) {
                init();
            }
        }

        /**
         * Initialise the service.
         */
        @SuppressWarnings("rawtypes")
        protected final void init() {
            headersAffectingJavaScript = new ArrayList<>();
            // Get evidence filters for all elements.
            List<EvidenceKeyFilter> filters = new ArrayList<>();
            for (FlowElement element : pipeline.getFlowElements()) {
                filters.add(element.getEvidenceKeyFilter());
            }

            for (EvidenceKeyFilter filter : filters) {
                // If the filter is a white list or derived type, such as
                // the filter of a parallel group, then get all HTTP header
                // evidence keys from white list and add them to the
                // headers that could affect the generated JavaScript.
                if (filter instanceof EvidenceKeyFilterWhitelist) {
                    EvidenceKeyFilterWhitelist whitelist = (EvidenceKeyFilterWhitelist) filter;
                    for (String key : whitelist.getWhitelist().keySet()) {
                        if (key.startsWith(EVIDENCE_HTTPHEADER_PREFIX + EVIDENCE_SEPERATOR) &&
                            hasControlChar(key) == false) {
                            String header = key.substring(key.indexOf(EVIDENCE_SEPERATOR) + 1);
                            if(headersAffectingJavaScript.contains(header) == false){
                                headersAffectingJavaScript.add(header);
                            }
                        }
                    }
                }
            }
        }

        /**
         * Set how many seconds a browser may keep a good response before it
         * asks again, the max-age of the Cache-Control header. The default
         * is {@link fiftyone.pipeline.web.Constants#DEFAULT_CLIENTSIDE_CACHE_MAX_AGE_SECONDS}.
         * A shorter time suits JavaScript that gathers values which change,
         * such as a location.
         * @param seconds the max-age, 0 or more
         * @throws IllegalArgumentException if seconds is negative
         */
        public void setCacheMaxAge(int seconds) {
            if (seconds < 0) {
                throw new IllegalArgumentException(
                    "The max-age of the client-side responses cannot be " +
                        "negative, and was " + seconds + ".");
            }
            cacheControl = cacheControl(seconds);
        }

        private static String cacheControl(int maxAgeSeconds) {
            return "private,max-age=" + maxAgeSeconds;
        }

        private boolean hasControlChar(String str) {
            for (char chr : str.toCharArray()) {
                if(chr <= 31) return true;
            }
            return false;
        }

        @Override
        public void serveJavascript(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
            serveContent(request, response, ContentTypes.JavaScript);
        }

        @Override
        public void serveJson(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
            serveContent(request, response, ContentTypes.Json);
        }

        public void serveContent(
            HttpServletRequest request,
            HttpServletResponse response,
            ContentTypes contentType) throws IOException {
            FlowData flowData = flowDataProviderCore.getFlowData(request);
            String etag = getETag(flowData);

            // A response built while processing recorded errors may lack the
            // data of the element that failed, so it must not be revalidated
            // into a 304 once processing works again.
            Collection<FlowError> errors = flowData.getErrors();
            boolean failed = errors != null && errors.isEmpty() == false;

            // A Pipeline without the builder for this endpoint is a
            // configuration error, so it is reported before a matching
            // If-None-Match could answer 304.
            JavaScriptBuilderElement jsElement = null;
            JsonBuilderElement jsonElement = null;
            switch (contentType) {
                case JavaScript:
                    jsElement = flowData.getPipeline().getElement(JavaScriptBuilderElement.class);
                    if (jsElement == null) {
                        throw new PipelineConfigurationException(
                            "Client-side JavaScript has been requested from " +
                                "the Pipeline. However, the JavaScriptBuilderElement " +
                                "is not present. To resolve this error, " +
                                "either disable client-side evidence or ensure " +
                                "the JavaScriptBuilderElement is added to " +
                                "your Pipeline.");
                    }
                    break;
                case Json:
                    jsonElement = flowData.getPipeline().getElement(JsonBuilderElement.class);
                    if (jsonElement == null) {
                        throw new PipelineConfigurationException(
                            "JSON data has been requested from the Pipeline. " +
                                "However, the JsonBuilderElement is not " +
                                "present. To resolve this error, either " +
                                "disable client-side evidence or ensure " +
                                "the JsonBuilderElement is added to your " +
                                "Pipeline."
                        );
                    }
                    break;
                default:
                    break;
            }

            if (ifNoneMatchMatches(
                request.getHeaders("If-None-Match"),
                etag)) {
                // The content has not changed. RFC 9110 section 13.1.2 has a
                // GET or HEAD answered with 304, carrying the caching headers
                // of the 200 it stands for, and any other method with 412.
                String method = request.getMethod();
                if ("GET".equals(method) || "HEAD".equals(method)) {
                    response.setStatus(304);
                    setCachingHeaders(response, etag, failed);
                } else {
                    response.setStatus(412);
                    response.setHeader("Access-Control-Allow-Origin", "*");
                }
            } else {
                // Otherwise, return the requested content to the client.
                String content = null;
                switch (contentType) {
                    case JavaScript:
                        JavaScriptBuilderData jsData = flowData.getFromElement(jsElement);
                        content = jsData == null ? null : jsData.getJavaScript();
                        break;
                    case Json:
                        JsonBuilderData jsonData = flowData.getFromElement(jsonElement);
                        content = jsonData == null ? null : jsonData.getJson();
                        break;
                    default:
                        break;
                }

                setHeaders(
                    response,
                    etag,
                    failed,
                    content,
                    contentType == ContentTypes.JavaScript ? "x-javascript" : "json");

                if (content != null) {
                    response.getWriter().write(content);
                }
            }

        }

        /**
         * The ETag of the Evidence the request supplied, a weak entity tag
         * holding the SHA-256 digest of the supplied values whose keys are
         * in the Pipeline's Evidence key filter.
         * <p>
         * The values are read from the {@link WebRequestEvidence}, which
         * holds them as the request supplied them, because Flow Elements add
         * Evidence while they process, such as the new session id the
         * Sequence Element adds to a request with none. A FlowData whose
         * Evidence was added without the web integration has no such
         * record, so its Evidence is used instead.
         * <p>
         * Keys are lower cased, as Evidence keys are case insensitive, and
         * sorted. Each key and value is written as its length in four bytes,
         * then its UTF-8 bytes, so the same values give the same ETag in
         * every process. The tag is weak because the content can change for
         * the same Evidence, for example when a data file is updated.
         * @param flowData the {@link FlowData} of the request
         * @return the ETag, such as W/"9bee...dbd3"
         */
        private String getETag(FlowData flowData) {
            Map<String, Object> evidence = flowData.getEvidence().asKeyMap();
            Object supplied = evidence.get(ShareUsageEvidence.HTTP_REQUEST_KEY);
            if (supplied instanceof WebRequestEvidence) {
                evidence = (WebRequestEvidence) supplied;
            }

            EvidenceKeyFilter filter = pipeline.getEvidenceKeyFilter();
            Map<String, String> values = new TreeMap<>();
            for (Map.Entry<String, Object> entry : evidence.entrySet()) {
                if (entry.getValue() != null &&
                    filter.include(entry.getKey())) {
                    values.put(
                        entry.getKey().toLowerCase(Locale.ROOT),
                        String.valueOf(entry.getValue()));
                }
            }

            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance(ETAG_DIGEST);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
            for (Map.Entry<String, String> entry : values.entrySet()) {
                addToDigest(digest, entry.getKey());
                addToDigest(digest, entry.getValue());
            }

            byte[] hash = digest.digest();
            StringBuilder etag = new StringBuilder(hash.length * 2 + 4);
            etag.append("W/\"");
            for (byte b : hash) {
                etag.append(HEX[(b >> 4) & 0xF]).append(HEX[b & 0xF]);
            }
            return etag.append('"').toString();
        }

        /**
         * Add the text to the digest as its length in four bytes, big
         * endian, then its UTF-8 bytes, so that no two lists of texts give
         * the same bytes.
         */
        private static void addToDigest(MessageDigest digest, String text) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            digest.update(new byte[] {
                (byte) (bytes.length >>> 24),
                (byte) (bytes.length >>> 16),
                (byte) (bytes.length >>> 8),
                (byte) bytes.length });
            digest.update(bytes);
        }

        /**
         * Whether the If-None-Match field matches the ETag, evaluated as RFC
         * 9110 section 13.1.2 says. The field lines are one comma separated
         * list (section 5.2). "*" matches. A list of entity tags matches when
         * one of them has the same quoted part as the ETag, with or without
         * W/ on either, which is the weak comparison of section 8.8.3.2.
         * Empty list elements and spaces or tabs around commas are allowed
         * (sections 5.6.1.2 and 5.6.3). Any other field, such as a tag then
         * something that is not a tag, or two tags with no comma between
         * them, does not match.
         * @param lines the If-None-Match field lines, or null
         * @param etag the ETag of the response
         * @return true if the method must not be performed
         */
        private static boolean ifNoneMatchMatches(
            Enumeration<String> lines,
            String etag) {
            if (lines == null || lines.hasMoreElements() == false) {
                return false;
            }
            StringBuilder field = new StringBuilder();
            boolean first = true;
            while (lines.hasMoreElements()) {
                String line = lines.nextElement();
                if (first == false) {
                    field.append(", ");
                }
                field.append(line == null ? "" : line);
                first = false;
            }
            String value = field.toString();
            if ("*".equals(trimWhitespace(value))) {
                return true;
            }

            String opaque = etag.substring(etag.indexOf('"'));
            boolean matched = false;
            boolean afterComma = true;
            int index = 0;
            while (index < value.length()) {
                char c = value.charAt(index);
                if (c == ' ' || c == '\t') {
                    index++;
                } else if (c == ',') {
                    afterComma = true;
                    index++;
                } else if (afterComma) {
                    int start = value.startsWith("W/", index) ?
                        index + 2 :
                        index;
                    int end = endOfOpaqueTag(value, start);
                    if (end < 0) {
                        return false;
                    }
                    if (opaque.equals(value.substring(start, end))) {
                        matched = true;
                    }
                    afterComma = false;
                    index = end;
                } else {
                    return false;
                }
            }
            return matched;
        }

        /**
         * The index after the opaque tag starting at start, a double quote,
         * any characters an entity tag allows, and a double quote, or -1 if
         * there is none (RFC 9110 section 8.8.3).
         */
        private static int endOfOpaqueTag(String value, int start) {
            if (start >= value.length() || value.charAt(start) != '"') {
                return -1;
            }
            for (int index = start + 1; index < value.length(); index++) {
                char c = value.charAt(index);
                if (c == '"') {
                    return index + 1;
                }
                // etagc is %x21 / %x23-7E / obs-text (%x80-FF).
                if ((c == 0x21 ||
                    (c >= 0x23 && c <= 0x7E) ||
                    (c >= 0x80 && c <= 0xFF)) == false) {
                    return -1;
                }
            }
            return -1;
        }

        /**
         * The text without leading and trailing spaces and tabs.
         */
        private static String trimWhitespace(String text) {
            int start = 0;
            int end = text.length();
            while (start < end &&
                (text.charAt(start) == ' ' || text.charAt(start) == '\t')) {
                start++;
            }
            while (end > start &&
                (text.charAt(end - 1) == ' ' || text.charAt(end - 1) == '\t')) {
                end--;
            }
            return text.substring(start, end);
        }

        /**
         * Set various HTTP headers on the JavaScript response.
         * @param response the {@link HttpServletResponse} to add the response
         *                 headers to
         * @param etag the ETag
         * @param failed true when processing recorded errors, which sends
         *               no-cache in place of max-age, and no ETag
         * @param content the content, or null if there is none. Its length
         *                in bytes, in the character encoding the response is
         *                written with, is the 'Content-Length' header
         * @param contentType the type of content
         */
        private void setHeaders(
            HttpServletResponse response,
            String etag,
            boolean failed,
            String content,
            String contentType) {
            response.setContentType("application/" + contentType);
            response.setContentLength(content == null ?
                0 :
                content.getBytes(characterEncoding(response)).length);
            response.setStatus(200);
            setCachingHeaders(response, etag, failed);
        }

        /**
         * The character encoding the response's writer uses, which is
         * ISO-8859-1 when none has been set, as the servlet specification
         * says.
         */
        private static Charset characterEncoding(
            HttpServletResponse response) {
            String name = response.getCharacterEncoding();
            return name == null ?
                StandardCharsets.ISO_8859_1 :
                Charset.forName(name);
        }

        /**
         * Set the headers a 200 and the 304 that stands for it both carry,
         * which are Vary, Cache-Control, the ETag unless processing recorded
         * errors, and the CORS header.
         * @param response the {@link HttpServletResponse} to add the response
         *                 headers to
         * @param etag the ETag
         * @param failed true when processing recorded errors, which sends
         *               no-cache in place of max-age, and no ETag
         */
        private void setCachingHeaders(
            HttpServletResponse response,
            String etag,
            boolean failed) {
            // Where no header affects the response there is no Vary header,
            // rather than an empty one.
            if (headersAffectingJavaScript.isEmpty() == false) {
                response.setHeader("Vary", stringJoin(headersAffectingJavaScript, ","));
            }
            if (failed) {
                response.setHeader("Cache-Control", FAILED_CACHE_CONTROL);
            } else {
                response.setHeader("Cache-Control", cacheControl);
                response.setHeader("ETag", etag);
            }
            response.setHeader("Access-Control-Allow-Origin", "*");
        }
    }
}