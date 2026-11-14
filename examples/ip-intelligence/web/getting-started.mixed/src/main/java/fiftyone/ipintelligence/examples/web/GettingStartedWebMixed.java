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
 * This example is available in full on [GitHub](https://github.com/51Degrees/51degrees-java/blob/main/examples/ip-intelligence/web/getting-started.mixed/src/main/java/fiftyone/ipintelligence/examples/web/GettingStartedWebMixed.java).
 *
 * By default the example uses the free Lite Device Detection and IP Intelligence data files,
 * which it downloads when they are missing, and shows each property only the Enterprise data
 * files have as needing them. To obtain the Enterprise data files, please
 * [contact us](https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=web-getting-started.mixed-src-main-java-fiftyone-ipintelligence-examples-web-gettingstartedwebmixed.java&utm_term=header).
 *
 * Required Maven Dependencies:
 * - [com.51degrees:ip-intelligence](https://central.sonatype.com/artifact/com.51degrees/ip-intelligence)
 * - [com.51degrees:device-detection](https://central.sonatype.com/artifact/com.51degrees/device-detection)
 * - [com.51degrees:pipeline-web](https://central.sonatype.com/artifact/com.51degrees/pipeline-web)
 */

package fiftyone.ipintelligence.examples.web;

import fiftyone.ipintelligence.examples.shared.FindOutMore;
import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneAspectPropertyMetaData;
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
import java.util.function.Supplier;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.LITE_DATA_FILE_REL_PATH;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.IPI_PATH_VARIABLE;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.*;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * Demonstrates using both Device Detection and IP Intelligence engines in a web application.
 * The PipelineFilter automatically processes requests through both engines, making results
 * available for device properties and IP-based location data simultaneously.
 * <p>
 * The configuration file for the pipeline is at src/main/webapp/WEB-INF/51Degrees-Mixed.xml
 * <p>
 * By default the free Lite data files are used.
 */
public class GettingStartedWebMixed extends HttpServlet {
    /**
     * This example's tag in the links of its "Find out more" section.
     */
    private static final String EXAMPLE_TAG =
            "web-getting-started.mixed-src-main-java-fiftyone-ipintelligence-examples-web-gettingstartedwebmixed.java";

    private static final long serialVersionUID = 1734154705981153541L;
    public static Logger logger = LoggerFactory.getLogger(GettingStartedWebMixed.class);

    public static final String DD_DATA_FILE_DEFAULT = "device-detection/51Degrees-LiteV4.1.hash";

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));
        logger.info("Running Example {}", GettingStartedWebMixed.class);

        // Accept two command line arguments: device detection data file and IP intelligence data file
        String ddDataFile = args.length > 0 ? args[0] : DD_DATA_FILE_DEFAULT;
        String ipiDataFile = args.length > 1 ? args[1] : LITE_DATA_FILE_REL_PATH;

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
            String ipiPath = DataFileHelper.getDataFileLocation(ipiDataFile);
            System.setProperty("TestDataFile", ipiPath);
            logger.info("Using IP Intelligence data file: {}", ipiPath);
        } catch (Exception e) {
            logger.warn("IP Intelligence data file not found at: {}. " +
                    "An explicit path can be supplied via the {} environment variable.",
                    ipiDataFile, IPI_PATH_VARIABLE);
            logger.warn("Will attempt to use default path from XML configuration");
            logger.debug("Error finding IP Intelligence data file", e);
        }

        // Start Jetty with this WebApp
        EmbedJetty.runWebApp(getResourceBase(), 8081);
    }

    public static String getResourceBase() {
        // Use the unique pipeline config file to locate this example's webapp directory.
        // Cannot use "WEB-INF/web.xml" because FileFinder may find another example's web.xml.
        java.io.File configXml = getFilePath("examples/ip-intelligence/web/getting-started.mixed/src/main/webapp/WEB-INF/51Degrees-Mixed.xml");
        return configXml.getParentFile().getParent();
    }

    FlowDataProviderCore flowDataProvider = new FlowDataProviderCore.Default();

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        // Get the IP address parameter from the request for custom lookup
        String inputIpAddress = request.getParameter("client-ip");

        // The detection has already been carried out by the PipelineFilter,
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
        }
    }

    private String loadTemplate(String templatePath) throws IOException {
        Path path = getFilePath(templatePath).toPath();
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private String substituteTemplateValues(String template, DeviceData deviceData,
                                            IPIntelligenceData ipiData, String inputIp,
                                            FlowData flowData) {
        // The engines say which properties their data files have.
        DeviceDetectionHashEngine ddEngine =
            flowData.getPipeline().getElement(DeviceDetectionHashEngine.class);
        IPIntelligenceOnPremiseEngine ipiEngine =
            flowData.getPipeline().getElement(IPIntelligenceOnPremiseEngine.class);
        return template
            .replace("${DATA_FILE_WARNING}", HtmlContentHelper.getDataFileAgeWarning(flowData))
            .replace("${INPUT_IP_ADDRESS}", inputIp != null ? inputIp : "")
            // Device Detection properties
            .replace("${HARDWARE_VENDOR}", value(ddEngine, "HardwareVendor", deviceData::getHardwareVendor))
            .replace("${HARDWARE_NAME}", value(ddEngine, "HardwareName", deviceData::getHardwareName))
            .replace("${DEVICE_TYPE}", value(ddEngine, "DeviceType", deviceData::getDeviceType))
            .replace("${PLATFORM_NAME}", value(ddEngine, "PlatformName", deviceData::getPlatformName))
            .replace("${PLATFORM_VERSION}", value(ddEngine, "PlatformVersion", deviceData::getPlatformVersion))
            .replace("${BROWSER_NAME}", value(ddEngine, "BrowserName", deviceData::getBrowserName))
            .replace("${BROWSER_VERSION}", value(ddEngine, "BrowserVersion", deviceData::getBrowserVersion))
            .replace("${SCREEN_WIDTH}", value(ddEngine, "ScreenPixelsWidth", deviceData::getScreenPixelsWidth))
            .replace("${SCREEN_HEIGHT}", value(ddEngine, "ScreenPixelsHeight", deviceData::getScreenPixelsHeight))
            // DeviceId is a match metric, which every data file gives.
            .replace("${DEVICE_ID}", asString(tryGet(deviceData::getDeviceId)))
            // IP Intelligence properties
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
            .replace("${LocationConfidence}", value(ipiEngine, "LocationConfidence", ipiData::getLocationConfidence))
            .replace("${ConnectionType}", value(ipiEngine, "ConnectionType", ipiData::getConnectionType))
            .replace("${HumanProbability}", value(ipiEngine, "HumanProbability", ipiData::getHumanProbability))
            .replace("${HardwareDiversity}", value(ipiEngine, "HardwareDiversity", ipiData::getHardwareDiversity))
            .replace("${BrowserDiversity}", value(ipiEngine, "BrowserDiversity", ipiData::getBrowserDiversity))
            .replace("${PlatformDiversity}", value(ipiEngine, "PlatformDiversity", ipiData::getPlatformDiversity))
            // On-premise engines: only invite the user to contact us about the
            // Enterprise data files when running on a free data file.
            .replace("${CONTACT_MESSAGE}", HtmlContentHelper.getContactMessage(
                HtmlContentHelper.ContactMessageVariant.ON_PREMISE,
                "Lite".equals(ddEngine.getDataSourceTier()) ||
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
     * As {@link #value(IPIntelligenceOnPremiseEngine, String, Supplier)} for
     * a Device Detection property.
     */
    private static String value(DeviceDetectionHashEngine engine,
                                String property,
                                Supplier<AspectPropertyValue<?>> value) {
        return hasProperty(engine, property) ?
                asString(value.get()) :
                DataFileHelper.NEEDS_ENTERPRISE;
    }

    /**
     * True when the Device Detection engine's data file has the property.
     * The Lite data file leaves out the properties only the Enterprise data
     * file has, such as HardwareVendor, HardwareName and DeviceType.
     */
    private static boolean hasProperty(DeviceDetectionHashEngine engine, String name) {
        for (FiftyOneAspectPropertyMetaData property : engine.getProperties()) {
            if (property.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
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
        // Search within the project directory, downloading the free Lite
        // file if it is missing
        try {
            return fiftyone.ipintelligence.examples.shared.DataFileHelper
                    .findDataFile(dataFile).getAbsolutePath();
        } catch (Exception e) {
            throw new IOException("Data file not found: " + dataFile, e);
        }
    }
}
