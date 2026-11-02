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

/*!
 * @example web/GettingStartedApi.java
 *
 * This example shows how to host a local HTTP API providing IP Intelligence results,
 * mirroring the endpoints of the 51Degrees Cloud service. It is the Java equivalent
 * of the GettingStarted-API example in ip-intelligence-dotnet-examples.
 *
 * The following endpoints are provided
 *
 * - `/json` processes the evidence supplied in the request (query parameters, headers
 *   and cookies) and returns the result as JSON
 * - `/accessibleproperties` returns metadata about the properties available from the engine
 * - `/evidencekeys` returns the evidence keys accepted by the pipeline
 * - `/download-ipi-gz` returns the gzipped data file with a Content-MD5 header
 *
 * A cloud example, such as GettingStartedCloud, can be pointed at this server as a
 * custom endpoint.
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/ip-intelligence-java-examples/blob/main/web/getting-started.api/src/main/java/fiftyone/ipintelligence/examples/web/GettingStartedApi.java).
 *
 * Required Maven Dependencies:
 * - [com.51degrees:ip-intelligence](https://central.sonatype.com/artifact/com.51degrees/ip-intelligence)
 * - [com.51degrees:pipeline.jsonbuilder](https://central.sonatype.com/artifact/com.51degrees/pipeline.jsonbuilder)
 */

package fiftyone.ipintelligence.examples.web;

import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngineBuilder;
import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.jsonbuilder.data.JsonBuilderData;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.ENTERPRISE_DATA_FILE_REL_PATH;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * Provides a local HTTP API for IP Intelligence lookups, mirroring the endpoints of the
 * 51Degrees Cloud service. The pipeline contains the on-premise IP Intelligence engine
 * and a JSON builder element which serialises the results.
 * <p>
 * The data file can be supplied as a command line argument. When no argument is given
 * the enterprise data file is looked for in the ip-intelligence-data directory.
 * <p>
 * Note that the .NET version of this example also includes a device detection engine
 * in the same pipeline. This Java version is IP Intelligence only.
 */
public class GettingStartedApi extends HttpServlet {
    private static final long serialVersionUID = 1734154705981153543L;
    public static final int DEFAULT_PORT = 5225;
    public static final String DATA_FILE_INIT_PARAM = "dataFile";
    public static Logger logger = LoggerFactory.getLogger(GettingStartedApi.class);

    private Pipeline pipeline;
    private IPIntelligenceOnPremiseEngine engine;

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));
        String dataFile = args.length > 0 ? args[0] : ENTERPRISE_DATA_FILE_REL_PATH;
        String dataFileLocation = DataFileHelper.getDataFileLocation(dataFile);
        logger.info("Running GettingStartedApi server using data file '{}'", dataFileLocation);

        Map<String, String> initParams = new HashMap<>();
        initParams.put(DATA_FILE_INIT_PARAM, dataFileLocation);
        EmbedJetty.runServlet("/*", DEFAULT_PORT, GettingStartedApi.class, initParams);
    }

    @Override
    public void init() throws ServletException {
        String dataFileLocation = getInitParameter(DATA_FILE_INIT_PARAM);
        try {
            engine = new IPIntelligenceOnPremiseEngineBuilder(LoggerFactory.getILoggerFactory())
                    .setAutoUpdate(false)
                    .build(dataFileLocation, false);
            // The SequenceElement provides the sequence evidence required by
            // the JsonBuilderElement.
            pipeline = new PipelineBuilder(LoggerFactory.getILoggerFactory())
                    .addFlowElement(new SequenceElementBuilder(
                            LoggerFactory.getILoggerFactory()).build())
                    .addFlowElement(engine)
                    .addFlowElement(new JsonBuilderElementBuilder(
                            LoggerFactory.getILoggerFactory()).build())
                    .build();
            DataFileHelper.logDataFileInfo(engine);
        } catch (Exception e) {
            throw new ServletException("Failed to create pipeline from data file '" +
                    dataFileLocation + "'", e);
        }
    }

    @Override
    public void destroy() {
        try {
            if (pipeline != null) {
                pipeline.close();
            }
        } catch (Exception e) {
            logger.error("Error closing pipeline", e);
        }
        super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handle(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handle(request, response);
    }

    private void handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        try {
            if (path.equals("/json") || path.endsWith(".json")) {
                processEvidence(request, response);
            } else if (path.startsWith("/accessibleproperties")) {
                accessibleProperties(response);
            } else if (path.startsWith("/evidencekeys")) {
                evidenceKeys(response);
            } else if (path.equals("/download-ipi-gz")) {
                downloadDataFile(response);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Not found. Available endpoints " +
                        "are /json, /accessibleproperties, /evidencekeys " +
                        "and /download-ipi-gz\"}");
            }
        } catch (Exception e) {
            logger.error("Error handling request for {}", path, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": " + JSONObject.quote(e.getMessage()) + "}");
        }
    }

    /**
     * Gather evidence from the request (query parameters, headers and cookies),
     * process it in the pipeline and respond with the JSON built by the
     * JsonBuilderElement.
     */
    private void processEvidence(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        Map<String, Object> evidence = new HashMap<>();
        for (Enumeration<String> names = request.getParameterNames(); names.hasMoreElements(); ) {
            String name = names.nextElement();
            evidence.put("query." + name, request.getParameter(name));
        }
        for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements(); ) {
            String name = names.nextElement();
            evidence.put("header." + name, request.getHeader(name));
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                evidence.put("cookie." + cookie.getName(), cookie.getValue());
            }
        }

        try (FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence(evidence);
            flowData.process();

            JsonBuilderData jsonData = flowData.get(JsonBuilderData.class);
            if (jsonData == null || jsonData.getJson() == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"No results.\"}");
                return;
            }
            response.setContentType("application/json");
            response.getWriter().write(jsonData.getJson());
        }
    }

    /**
     * Respond with metadata describing the properties available from each
     * engine in the pipeline.
     */
    private void accessibleProperties(HttpServletResponse response) throws IOException {
        JSONObject products = new JSONObject();
        for (FlowElement<?, ?> element : pipeline.getFlowElements()) {
            if (element instanceof AspectEngine) {
                AspectEngine<?, ?> aspectEngine = (AspectEngine<?, ?>) element;
                JSONArray properties = new JSONArray();
                for (AspectPropertyMetaData property : aspectEngine.getProperties()) {
                    JSONObject p = new JSONObject();
                    p.put("Name", property.getName());
                    p.put("Type", property.getType().getSimpleName());
                    p.put("Category", property.getCategory());
                    properties.put(p);
                }
                JSONObject product = new JSONObject();
                product.put("Properties", properties);
                products.put(aspectEngine.getElementDataKey(), product);
            }
        }
        JSONObject result = new JSONObject();
        result.put("Products", products);
        response.setContentType("application/json");
        response.getWriter().write(result.toString());
    }

    /**
     * Respond with the distinct evidence keys accepted by the elements of the pipeline.
     */
    private void evidenceKeys(HttpServletResponse response) throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        for (FlowElement<?, ?> element : pipeline.getFlowElements()) {
            if (element.getEvidenceKeyFilter() instanceof EvidenceKeyFilterWhitelist) {
                EvidenceKeyFilterWhitelist filter =
                        (EvidenceKeyFilterWhitelist) element.getEvidenceKeyFilter();
                keys.addAll(filter.getWhitelist().keySet());
            }
        }
        response.setContentType("application/json");
        response.getWriter().write(new JSONArray(keys).toString());
    }

    /**
     * Respond with the gzipped data file. The MD5 of the compressed content is
     * supplied in the Content-MD5 header so that the receiver can verify the download.
     */
    private void downloadDataFile(HttpServletResponse response) throws Exception {
        String sourcePath = engine.getDataFileMetaData().getDataFilePath();

        logger.info("Compressing data file '{}'", sourcePath);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream source = Files.newInputStream(Paths.get(sourcePath));
             GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
            byte[] chunk = new byte[64 * 1024];
            int read;
            while ((read = source.read(chunk)) > 0) {
                gzip.write(chunk, 0, read);
            }
        }
        byte[] compressed = buffer.toByteArray();

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        StringBuilder hex = new StringBuilder();
        for (byte b : md5.digest(compressed)) {
            hex.append(String.format("%02x", b));
        }
        logger.info("MD5 of compressed data file is {}", hex);

        response.setContentType("application/gzip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                Paths.get(sourcePath).getFileName() + ".gz\"");
        response.setHeader("Content-MD5", hex.toString());
        response.setContentLength(compressed.length);
        try (OutputStream out = response.getOutputStream()) {
            out.write(compressed);
        }
    }
}
