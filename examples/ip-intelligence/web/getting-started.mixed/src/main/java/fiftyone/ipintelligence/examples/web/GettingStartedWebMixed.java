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
 * @example GettingStartedWebMixed.java
 *
 * This example shows how to use both 51Degrees Device Detection and IP Intelligence
 * engines in a single pipeline within a web application.
 *
 * You will learn:
 *
 * 1. How to configure a Pipeline with both Device Detection and IP Intelligence engines
 * 2. How evidence from the web request is automatically passed to both engines
 * 3. How to retrieve results from both engines in your web application
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/ip-intelligence-java-examples/blob/main/web/getting-started.mixed/src/main/java/fiftyone/ipintelligence/examples/web/GettingStartedWebMixed.java).
 *
 * This example requires:
 * - A Device Detection data file (.hash format)
 * - An IP Intelligence data file (.ipi format)
 *
 * Required Maven Dependencies:
 * - [com.51degrees:ip-intelligence](https://central.sonatype.com/artifact/com.51degrees/ip-intelligence)
 * - [com.51degrees:device-detection](https://central.sonatype.com/artifact/com.51degrees/device-detection)
 * - [com.51degrees:pipeline-web](https://central.sonatype.com/artifact/com.51degrees/pipeline-web)
 */

package fiftyone.ipintelligence.examples.web;

import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElement;
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
import java.nio.file.Paths;
import java.util.Map;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.ENTERPRISE_DATA_FILE_REL_PATH;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.IPI_PATH_ENV_VAR;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.*;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * Demonstrates using both Device Detection and IP Intelligence engines in a web application.
 * The PipelineFilter automatically processes requests through both engines, making results
 * available for device properties and IP-based location data simultaneously.
 * <p>
 * The configuration file for the pipeline is at src/main/webapp/WEB-INF/51Degrees-Mixed.xml
 * <p>
 * This example requires:
 * - A Device Detection data file (.hash format)
 * - An IP Intelligence data file (.ipi format)
 */
public class GettingStartedWebMixed extends HttpServlet {
    private static final long serialVersionUID = 1734154705981153541L;
    public static Logger logger = LoggerFactory.getLogger(GettingStartedWebMixed.class);

    public static final String DD_DATA_FILE_DEFAULT = "device-detection-data/51Degrees-LiteV4.1.hash";

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));
        logger.info("Running Example {}", GettingStartedWebMixed.class);

        // Accept two command line arguments: device detection data file and IP intelligence data file
        String ddDataFile = args.length > 0 ? args[0] : DD_DATA_FILE_DEFAULT;
        String ipiDataFile = args.length > 1 ? args[1] : ENTERPRISE_DATA_FILE_REL_PATH;

        // Set Device Detection data file location
        try {
            String ddPath = resolveDataFile(ddDataFile);
            System.setProperty("TestDeviceDetectionDataFile", ddPath);
            logger.info("Using Device Detection data file: {}", ddPath);
        } catch (Exception e) {
            logger.warn("Device Detection data file not found at: {}", ddDataFile);
            logger.warn("Will attempt to use default path from XML configuration");
            logger.debug("Error finding Device Detection data file", e);
        }

        // Set IP Intelligence data file location
        try {
            String ipiPath = fiftyone.ipintelligence.examples.shared.DataFileHelper.getDataFileLocation(ipiDataFile);
            System.setProperty("TestDataFile", ipiPath);
            logger.info("Using IP Intelligence data file: {}", ipiPath);
        } catch (Exception e) {
            logger.warn("IP Intelligence data file not found at: {}. " +
                    "An explicit path can be supplied via the {} environment variable.",
                    ipiDataFile, IPI_PATH_ENV_VAR);
            logger.warn("Will attempt to use default path from XML configuration");
            logger.debug("Error finding IP Intelligence data file", e);
        }

        // Start Jetty with this WebApp
        EmbedJetty.runWebApp(getResourceBase(), 8081);
    }

    public static String getResourceBase() {
        // Use the unique pipeline config file to locate this example's webapp directory.
        // Cannot use "WEB-INF/web.xml" because FileFinder may find another example's web.xml.
        java.io.File configXml = getFilePath("WEB-INF/51Degrees-Mixed.xml");
        return configXml.getParentFile().getParent();
    }

    FlowDataProviderCore flowDataProvider = new FlowDataProviderCore.Default();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        // Get the IP address parameter from the request for custom lookup
        String inputIpAddress = request.getParameter("client-ip");

        // The detection has already been carried out by the PipelineFilter
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
                // X-Forwarded-For may contain multiple IPs; the first is the client
                targetIp = forwarded.split(",")[0].trim();
            } else {
                targetIp = request.getRemoteAddr();
            }
        }

        // Get Device Detection data
        DeviceData deviceData = flowData.get(DeviceData.class);

        // Get IP Intelligence data
        IPIntelligenceData ipiData = flowData.get(IPIntelligenceData.class);

        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            String resourceBase = getResourceBase();
            String htmlTemplate = loadTemplate(resourceBase + "/WEB-INF/html/index.html");
            String processedHtml = substituteTemplateValues(htmlTemplate, deviceData, ipiData, targetIp, flowData);
            out.println(processedHtml);
        } finally {
            if (flowData != null) {
                try {
                    flowData.close();
                } catch (Exception e) {
                    logger.warn("Error closing flow data", e);
                }
            }
        }
    }

    private String loadTemplate(String templatePath) throws IOException {
        Path path = getFilePath(templatePath).toPath();
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private String substituteTemplateValues(String template, DeviceData deviceData,
                                            IPIntelligenceData ipiData, String inputIp,
                                            FlowData flowData) {
        return template
            .replace("${DATA_FILE_WARNING}", HtmlContentHelper.getDataFileAgeWarning(flowData))
            .replace("${INPUT_IP_ADDRESS}", inputIp != null ? inputIp : "")
            // Device Detection properties
            .replace("${HARDWARE_VENDOR}", asString(tryGet(deviceData::getHardwareVendor)))
            .replace("${HARDWARE_NAME}", asString(tryGet(deviceData::getHardwareName)))
            .replace("${DEVICE_TYPE}", asString(tryGet(deviceData::getDeviceType)))
            .replace("${PLATFORM_NAME}", asString(tryGet(deviceData::getPlatformName)))
            .replace("${PLATFORM_VERSION}", asString(tryGet(deviceData::getPlatformVersion)))
            .replace("${BROWSER_NAME}", asString(tryGet(deviceData::getBrowserName)))
            .replace("${BROWSER_VERSION}", asString(tryGet(deviceData::getBrowserVersion)))
            .replace("${SCREEN_WIDTH}", asString(tryGet(deviceData::getScreenPixelsWidth)))
            .replace("${SCREEN_HEIGHT}", asString(tryGet(deviceData::getScreenPixelsHeight)))
            .replace("${DEVICE_ID}", asString(tryGet(deviceData::getDeviceId)))
            // IP Intelligence properties
            .replace("${REGISTERED_NAME}", asStringProperty(tryGet(ipiData::getRegisteredName)))
            .replace("${REGISTERED_OWNER}", asStringProperty(tryGet(ipiData::getRegisteredOwner)))
            .replace("${REGISTERED_COUNTRY}", asStringProperty(tryGet(ipiData::getRegisteredCountry)))
            .replace("${IP_RANGE_START}", asIPAddressProperty(tryGet(ipiData::getIpRangeStart)))
            .replace("${IP_RANGE_END}", asIPAddressProperty(tryGet(ipiData::getIpRangeEnd)))
            .replace("${COUNTRY}", asStringProperty(tryGet(ipiData::getCountry)))
            .replace("${COUNTRY_CODE}", asStringProperty(tryGet(ipiData::getCountryCode)))
            .replace("${COUNTRY_CODE3}", asStringProperty(tryGet(ipiData::getCountryCode3)))
            .replace("${REGION}", asStringProperty(tryGet(ipiData::getRegion)))
            .replace("${STATE}", asStringProperty(tryGet(ipiData::getState)))
            .replace("${TOWN}", asStringProperty(tryGet(ipiData::getTown)))
            .replace("${LATITUDE}", asFloatProperty(tryGet(ipiData::getLatitude)))
            .replace("${LONGITUDE}", asFloatProperty(tryGet(ipiData::getLongitude)))
            .replace("${AREAS}", asWktStringProperty(tryGet(ipiData::getAreas)))
            .replace("${AREAS_JS}", escapeForJs(asWktStringProperty(tryGet(ipiData::getAreas))))
            .replace("${ACCURACY_RADIUS}", asIntegerProperty(tryGet(ipiData::getAccuracyRadiusMin)))
            .replace("${TIME_ZONE_OFFSET}", asIntegerProperty(tryGet(ipiData::getTimeZoneOffset)))
            .replace("${EVIDENCE_ROWS}", buildEvidenceRows(flowData))
            //.replace("${HumanProbability}",asStringProperty(tryGet(ipiData::getHumanProbability)))
             .replace("${LocationConfidence}",asStringProperty(tryGet(ipiData::getLocationConfidence)))
            .replace("${ConnectionType}",asStringProperty(tryGet(ipiData::getConnectionType)))
                .replace("${HumanProbability}",asIntegerProperty(tryGet(ipiData::getHumanProbability)))
                .replace("${HardwareDiversity}",asIntegerProperty(tryGet(ipiData::getHardwareDiversity)))
        .replace("${BrowserDiversity}",asIntegerProperty(tryGet(ipiData::getBrowserDiversity)))
                .replace("${PlatformDiversity}",asIntegerProperty(tryGet(ipiData::getPlatformDiversity)))
            // On-premise engines: only invite the user to contact us about more
            // properties and features when running against the free Lite tier.
            .replace("${CONTACT_MESSAGE}", HtmlContentHelper.getContactMessage(
                HtmlContentHelper.ContactMessageVariant.ON_PREMISE, isLiteDataFile(flowData)));

    }

    /**
     * Determine whether the on-premise IP Intelligence engine is using the free Lite tier data file.
     */
    private boolean isLiteDataFile(FlowData flowData) {
        fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine engine =
            flowData.getPipeline().getElement(
                fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine.class);
        return engine != null && engine.getDataSourceTier().equals("Lite");
    }

    private String buildEvidenceRows(FlowData flowData) {
        StringBuilder evidenceRows = new StringBuilder();

        for (Map.Entry<String, Object> evidenceEntry : flowData.getEvidence().asKeyMap().entrySet()) {
            String key = evidenceEntry.getKey();
            Object value = evidenceEntry.getValue();
            String valueStr = value != null ? value.toString() : "null";

            // Check if this evidence was used by either engine
            boolean wasUsed = false;
            try {
                FlowElement<?, ?> ddEngine = flowData.getPipeline().getElement(DeviceDetectionHashEngine.class);
                if (ddEngine != null && ddEngine.getEvidenceKeyFilter().include(key)) {
                    wasUsed = true;
                }
            } catch (Exception e) { /* ignore */ }
            try {
                FlowElement<?, ?> ipiEngine = flowData.getPipeline().getElement(
                    fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine.class);
                if (ipiEngine != null && ipiEngine.getEvidenceKeyFilter().include(key)) {
                    wasUsed = true;
                }
            } catch (Exception e) { /* ignore */ }

            String rowClass = wasUsed ? "c-eg-table__row--used" : "c-eg-table__row--present";

            evidenceRows.append(String.format(
                "<tr class=\"c-eg-table__row %s\">" +
                    "<td class=\"c-eg-table__cell c-eg-table__cell--key\">%s</td>" +
                    "<td class=\"c-eg-table__cell\">%s</td></tr>",
                rowClass, key, valueStr));
        }

        return evidenceRows.toString();
    }

    private String escapeForJs(String value) {
        return value != null ? value.replace("'", "\\'").replace("\"", "\\\"") : "";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            processRequest(request, response);
            response.setStatus(200);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
        }
    }

    /**
     * Resolve a data file path that may be absolute, relative, or within the project.
     */
    private static String resolveDataFile(String dataFile) throws IOException {
        Path dataPath = Paths.get(dataFile);
        if (dataPath.isAbsolute() && Files.exists(dataPath)) {
            return dataPath.toString();
        }
        Path relativePath = Paths.get(System.getProperty("user.dir"), dataFile);
        if (Files.exists(relativePath)) {
            return relativePath.toAbsolutePath().toString();
        }
        try {
            return getFilePath(dataFile).getAbsolutePath();
        } catch (Exception e) {
            throw new IOException("Data file not found: " + dataFile);
        }
    }
}
