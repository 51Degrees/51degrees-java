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
 * @example GettingStartedWebOnPrem.java
 *
 * This example shows how to use 51Degrees On-premise IP Intelligence in a web application to determine location and network details from IP addresses.
 *
 * You will learn:
 * 
 * 1. How to configure a Pipeline in a web application that uses 51Degrees On-premise IP Intelligence
 * 2. How evidence from the web request is automatically passed to the Pipeline
 * 3. How to retrieve the results in your web application
 * 
 * This example is available in full on [GitHub](https://github.com/51Degrees/51degrees-java/blob/main/examples/ip-intelligence/web/getting-started.onprem/src/main/java/fiftyone/ipintelligence/examples/web/GettingStartedWebOnPrem.java).
 *
 * By default the example uses the free Lite IP Intelligence data file, which it downloads when
 * it is missing, and shows each property only the Enterprise data file has as needing it. To
 * obtain an Enterprise data file, please [contact us](https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=web-getting-started.onprem-src-main-java-fiftyone-ipintelligence-examples-web-gettingstartedwebonprem.java&utm_term=header).
 *
 * Required Maven Dependencies:
 * - [com.51degrees:ip-intelligence](https://central.sonatype.com/artifact/com.51degrees/ip-intelligence)
 * - [com.51degrees:pipeline-web](https://central.sonatype.com/artifact/com.51degrees/pipeline-web)
 *
 * ## Overview
 *
 * The `PipelineFilter` is used to intercept requests and perform IP Intelligence. The results
 * will be stored in the HttpServletRequest object.
 *
 * The results of the lookup can be accessed by using a FlowDataProvider. The
 * PipelineFilter closes the FlowData at the end of the request, so it is not
 * closed by the example.
 * ```{java}
 * FlowData flowData = flowDataProvider.getFlowData(request);
 * IPIntelligenceData ipData = flowData.get(IPIntelligenceData.class);
 * ...
 * ```
 *
 * IP Intelligence operates entirely server-side, so all results are available immediately
 * after processing through the Pipeline API shown above.
 */

package fiftyone.ipintelligence.examples.web;

import fiftyone.ipintelligence.examples.shared.FindOutMore;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.web.services.FlowDataProviderCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Supplier;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.LITE_DATA_FILE_REL_PATH;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.IPI_PATH_VARIABLE;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.asString;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * This example shows how to use 51Degrees On-premise IP Intelligence to determine location and network details from IP addresses in a web application.
 * 
 * You will learn:
 * 
 * 1. How to configure a Pipeline that uses 51Degrees On-premise IP Intelligence in a web application
 * 2. How the PipelineFilter automatically processes requests and makes results available
 * 3. How to retrieve the results in your web application
 * 
 * This is the getting started Web/On-Prem example showing use of the 51Degrees
 * supplied filter which automatically creates and configures an IP Intelligence pipeline.
 * 
 * The configuration file for the pipeline is at src/main/webapp/WEB-INF/51Degrees-OnPrem.xml
 * 
 * By default the example uses the free Lite IP Intelligence data file, which it downloads when
 * it is missing, and shows each property only the Enterprise data file has as needing it. To
 * obtain an Enterprise data file, please [contact us](https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=web-getting-started.onprem-src-main-java-fiftyone-ipintelligence-examples-web-gettingstartedwebonprem.java&utm_term=gettingstartedwebonprem).
 */
public class GettingStartedWebOnPrem extends HttpServlet {
    /**
     * This example's tag in the links of its "Find out more" section.
     */
    private static final String EXAMPLE_TAG =
            "web-getting-started.onprem-src-main-java-fiftyone-ipintelligence-examples-web-gettingstartedwebonprem.java";

    private static final long serialVersionUID = 1734154705981153540L;
    public static Logger logger = LoggerFactory.getLogger(GettingStartedWebOnPrem.class);

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));
        logger.info("Running Example {}", GettingStartedWebOnPrem.class);

        // Use the supplied path for the data file
        String dataFile = args.length > 0 ? args[0] : LITE_DATA_FILE_REL_PATH;

        // Set data file location using DataFileHelper
        try {
            String dataFilePath = DataFileHelper.getDataFileLocation(dataFile);
            System.setProperty("TestDataFile", dataFilePath);
            logger.info("Using data file: {}", dataFilePath);
        } catch (Exception e) {
            logger.warn("Data file not found at expected location: {}. " +
                    "An explicit path can be supplied via the {} environment variable.",
                    dataFile, IPI_PATH_VARIABLE);
            logger.warn("Will attempt to use default path from XML configuration");
            logger.debug("Error finding data file", e);
        }

        // start Jetty with this WebApp
        EmbedJetty.runWebApp(getResourceBase(), 8082);
    }

    public static String getResourceBase() {
        // Use the unique pipeline config file to locate this example's webapp directory.
        // Cannot use "WEB-INF/web.xml" because FileFinder may find another example's web.xml.
        java.io.File configXml = getFilePath("examples/ip-intelligence/web/getting-started.onprem/src/main/webapp/WEB-INF/51Degrees-OnPrem.xml");
        // 51Degrees-OnPrem.xml is in WEB-INF/, so parent is webapp directory
        return configXml.getParentFile().getParent();
    }

    FlowDataProviderCore flowDataProvider = new FlowDataProviderCore.Default();

    /**
     * Process the HTTP request and generate the IP Intelligence results page
     * @param request  servlet request
     * @param response servlet response
     * @throws Exception when things go wrong
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        
        // Get the IP address parameter from the request for custom lookup
        String inputIpAddress = request.getParameter("client-ip");
        
        // The lookup has already been carried out by the PipelineFilter
        // which closes the FlowData at the end of the request, so it is not
        // closed here.
        FlowData flowData = flowDataProvider.getFlowData(request);
        
        // Determine target IP for display.
        // When behind a reverse proxy (e.g. ngrok), the real client IP
        // is in the X-Forwarded-For header, not request.getRemoteAddr().
        String targetIp;
        if (inputIpAddress != null && !inputIpAddress.trim().isEmpty()) {
            targetIp = inputIpAddress.trim();
        } else {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                targetIp = forwarded.split(",")[0].trim();
            } else {
                targetIp = request.getRemoteAddr();
            }
        }
        
        // Get IP Intelligence data
        IPIntelligenceData ipiData = flowData.get(IPIntelligenceData.class);
        
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            // Load and process the HTML template
            String resourceBase = getResourceBase();
            String htmlTemplate = loadTemplate(resourceBase + "/WEB-INF/html/index.html");
            String processedHtml = substituteTemplateValues(htmlTemplate, ipiData, targetIp, flowData);
            out.println(processedHtml);
        }
    }
    
    /**
     * Load HTML template from file system
     */
    private String loadTemplate(String templatePath) throws IOException {
        // FileFinder always finds the file when running from examples root
        Path path = getFilePath(templatePath).toPath();
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
    
    /**
     * Replace template variables with IP Intelligence data
     */
    private String substituteTemplateValues(String template, IPIntelligenceData ipiData, String inputIp, FlowData flowData) {
        // The engine says which properties its data file has.
        IPIntelligenceOnPremiseEngine ipiEngine =
            flowData.getPipeline().getElement(IPIntelligenceOnPremiseEngine.class);
        return template
            .replace("${DATA_FILE_WARNING}", HtmlContentHelper.getDataFileAgeWarning(flowData))
            .replace("${INPUT_IP_ADDRESS}", inputIp != null ? inputIp : "")
            .replace("${REGISTERED_NAME}", value(ipiEngine, "RegisteredName", ipiData::getRegisteredName))
            .replace("${REGISTERED_OWNER}", value(ipiEngine, "RegisteredOwner", ipiData::getRegisteredOwner))
            .replace("${REGISTERED_COUNTRY}", value(ipiEngine, "RegisteredCountry", ipiData::getRegisteredCountry))
            .replace("${IP_RANGE_START}", value(ipiEngine, "IpRangeStart", ipiData::getIpRangeStart))
            .replace("${IP_RANGE_END}", value(ipiEngine, "IpRangeEnd", ipiData::getIpRangeEnd))
            .replace("${COUNTRY}", value(ipiEngine, "Country", ipiData::getCountry))
            .replace("${COUNTRY_CODE}", value(ipiEngine, "CountryCode", ipiData::getCountryCode))
            .replace("${COUNTRY_CODE3}", value(ipiEngine, "CountryCode3", ipiData::getCountryCode3))
            .replace("${REGION}", value(ipiEngine, "Region", ipiData::getRegion))
            .replace("${STATE}", value(ipiEngine, "State", ipiData::getState))
            .replace("${TOWN}", value(ipiEngine, "Town", ipiData::getTown))
            .replace("${LATITUDE}", value(ipiEngine, "Latitude", ipiData::getLatitude))
            .replace("${LONGITUDE}", value(ipiEngine, "Longitude", ipiData::getLongitude))
            .replace("${AREAS}", value(ipiEngine, "Areas", ipiData::getAreas))
            // The map shows nothing when the data file has no areas.
            .replace("${AREAS_JS}", DataFileHelper.hasProperty(ipiEngine, "Areas") ?
                escapeForJs(asString(ipiData.getAreas())) : "")
            .replace("${ACCURACY_RADIUS}", value(ipiEngine, "AccuracyRadiusMin", ipiData::getAccuracyRadiusMin))
            .replace("${TIME_ZONE_OFFSET}", value(ipiEngine, "TimeZoneOffset", ipiData::getTimeZoneOffset))
            .replace("${FIND_OUT_MORE}", FindOutMore.html(EXAMPLE_TAG,
                    FindOutMore.ON_PREMISE_REPOSITORIES))
            .replace("${EVIDENCE_ROWS}", buildEvidenceRows(flowData))
            // On-premise engine: only invite the user to contact us about the
            // Enterprise data file when running on a free data file.
            .replace("${CONTACT_MESSAGE}", HtmlContentHelper.getContactMessage(
                HtmlContentHelper.ContactMessageVariant.ON_PREMISE,
                DataFileHelper.isFreeDataFile(ipiEngine)));
    }

    /**
     * The value of a property for the page, or that it needs the Enterprise
     * data file when the data file in use does not have it. The value is
     * only read when the data file has the property.
     */
    private static String value(IPIntelligenceOnPremiseEngine engine,
                                String property,
                                Supplier<AspectPropertyValue<?>> value) {
        return DataFileHelper.hasProperty(engine, property) ?
                asString(value.get()) :
                DataFileHelper.NEEDS_ENTERPRISE;
    }
    
    /**
     * Build evidence table rows showing all evidence, with distinction between used vs present
     */
    private String buildEvidenceRows(FlowData flowData) {
        StringBuilder evidenceRows = new StringBuilder();
        
        // Get the IP Intelligence engine to check which evidence was actually used
        FlowElement<?, ?> engine = getIPIntelligenceEngine(flowData);
        
        // Get all evidence from FlowData
        for (Map.Entry<String, Object> evidenceEntry : flowData.getEvidence().asKeyMap().entrySet()) {
            String key = evidenceEntry.getKey();
            Object value = evidenceEntry.getValue();
            String valueStr = value != null ? value.toString() : "null";
            
            // Check if this evidence was actually used by the engine
            boolean wasUsed = engine != null && engine.getEvidenceKeyFilter().include(key);
            String rowClass = wasUsed ? "c-eg-table__row--used" : "c-eg-table__row--present";

            evidenceRows.append(String.format(
                "<tr class=\"c-eg-table__row %s\">" +
                    "<td class=\"c-eg-table__cell c-eg-table__cell--key\">%s</td>" +
                    "<td class=\"c-eg-table__cell\">%s</td></tr>",
                rowClass, key, valueStr));
        }
        
        return evidenceRows.toString();
    }
    
    /**
     * Get the IP Intelligence on-premise engine from the pipeline
     */
    private FlowElement<?, ?> getIPIntelligenceEngine(FlowData flowData) {
        // Get IP Intelligence on-premise engine
        return flowData.getPipeline().getElement(
            fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine.class);
    }
    
    /**
     * Escape string for JavaScript
     */
    private String escapeForJs(String value) {
        return value != null ? value.replace("'", "\\'").replace("\"", "\\\"") : "";
    }
    

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        // any failure causes 500 error
        try {
            processRequest(request, response);
            response.setStatus(200);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }
}

