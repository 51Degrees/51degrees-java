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

package fiftyone.devicedetection.examples.web;

import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static fiftyone.devicedetection.examples.shared.PropertyHelper.asString;
import static fiftyone.devicedetection.examples.shared.PropertyHelper.tryGet;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

public class HtmlContentHelper {
    /**
     * Helper to output HTML headers
     * @param out the PrintWriter to write to
     * @param title title of the page
     */
    public static void doHtmlPreamble(PrintWriter out, String title) {
        // language=html
        out.append("<!DOCTYPE html>" +
                "<html lang='en'>\n" +
                "<head>\n"+
                "<meta charset=\"UTF-8\">\n" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "<title>" + title + "</title>\n" +
                "<link rel=\"stylesheet\" href=\"/css/examples-main.min.css\">\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"c-eg-page\">\n" +
                "<h1 class=\"c-eg-page__title\">51Degrees Device Detection Example ("+ title +")</h1>\n" +
                "\n");
    }


    public static void doResponseHeaders(PrintWriter out, HttpServletResponse response) {
        // language=html
        out.append("<div id=\"response\">\n" +
                "<h2 class=\"c-eg-page__heading\">Response Headers</h2>\n" +
                "<p class=\"c-eg-page__lead\">The following response headers were set:</p>\n" +
                "<table class=\"c-eg-table\">\n" +
                "<thead class=\"c-eg-table__head\"><tr class=\"c-eg-table__row\">" +
                "<th class=\"c-eg-table__cell\">Key</th>" +
                "<th class=\"c-eg-table__cell\">Value</th></tr></thead>\n" +
                "<tbody>");
        for (String headerName: response.getHeaderNames()) {
            out.append("<tr class=\"c-eg-table__row c-eg-table__row--present\">" +
                            "<td class=\"c-eg-table__cell c-eg-table__cell--key\">")
                    .append(headerName)
                    .append("</td><td class=\"c-eg-table__cell\">")
                    .append(response.getHeader(headerName))
                    .append("</td></tr>\n");
        }
        out.append("</tbody></table></div>");

        if (response.getHeaderNames().contains("Accept-CH") == false) {
            out.append(
         "<div class=\"c-eg-alert\">WARNING: There is no Accept-CH header in the response. " +
                 "This may indicate that your browser does not support User-Agent Client Hints. " +
                 "This is not necessarily a problem, but if you are wanting to try out detection " +
                 "using User-Agent Client Hints, then make sure that your browser "+
                 "<a href=\"https://developer.mozilla" +
                 ".org/en-US/docs/Web/API/User-Agent_Client_Hints_API#browser_compatibility" +
                 "\">supports them</a>.</div>");
        }

    }

    public static void doEvidence(PrintWriter out, HttpServletRequest request, FlowData flowData) {
        FlowElement<?, ?> engine = getFlowElement(flowData);
        // set up the table
        out.println(
                "  <div id=\"evidence\">\n" +
                        "  <h2 class=\"c-eg-page__heading\">Evidence Used</h2>\n");

        doUachInfo(out);

        out.println("<p class=\"c-eg-legend\">Evidence was " +
                "<span class=\"c-eg-legend__swatch c-eg-legend__swatch--used\">used</span> / " +
                "<span class=\"c-eg-legend__swatch c-eg-legend__swatch--present\">present</span> " +
                "for detection</p>");
        out.println("<table class=\"c-eg-table\">\n" +
                "<thead class=\"c-eg-table__head\"><tr class=\"c-eg-table__row\">" +
                "<th class=\"c-eg-table__cell\">Key</th>" +
                "<th class=\"c-eg-table__cell\">Value</th></tr></thead>\n" +
                "<tbody>");
        // list by other evidence entries
        for (Map.Entry<String, Object> evidence : flowData.getEvidence().asKeyMap().entrySet()) {
            if (engine.getEvidenceKeyFilter().include(evidence.getKey())) {
                out.println("<tr class=\"c-eg-table__row c-eg-table__row--present\">");
                out.println("<td class=\"c-eg-table__cell c-eg-table__cell--key\">" + evidence.getKey() +
                        "</td><td class=\"c-eg-table__cell\">" + evidence.getValue() + "</td></tr>");
            }
        }
        out.println("</tbody></table>\n" +
                "</div>\n");
    }

    private static FlowElement<?, ?> getFlowElement(FlowData flowData) {
        // we assume a CloudRequest or a HashEngine ... but not both
        FlowElement<?,?> engine = flowData.getPipeline().getElement(CloudRequestEngine.class);
        if (Objects.isNull(engine)) {
            engine = flowData.getPipeline().getElement(DeviceDetectionHashEngine.class);
            if (Objects.isNull(engine)) {
                throw new IllegalStateException("No device detection engine found");
            }
        }
        return engine;
    }

    /**
     * Helper to output a warning, intended to appear at the top of the page, when the
     * on-premise data file is more than 28 days old, suggesting a more recent data file
     * may be needed. For the cloud engine (which has no local data file) this is a no-op.
     * @param out the PrintWriter to write to
     * @param flowData the flowdata used to determine the engine and the data file age
     */
    public static void doDataFileAgeWarning(PrintWriter out, FlowData flowData) {
        FlowElement<?, ?> engine = getFlowElement(flowData);
        if (engine instanceof CloudRequestEngine) {
            // Cloud engine has no local data file - nothing to warn about.
            return;
        }
        // date of creation
        Date fileDate = ((DeviceDetectionHashEngine) engine).getDataFilePublishedDate();
        long daysOld = ChronoUnit.DAYS.between(fileDate.toInstant(), Instant.now());
        if (daysOld > 28) {
            // language=html
            out.append(String.format(
                    "<div class=\"c-eg-alert\">The data file is more than %d days old. " +
                            "A more recent data file " +
                            " may be needed to correctly detect the latest devices, " +
                            "browsers, etc.</div>\n", daysOld));
        }
    }

    public static void doDeviceData(PrintWriter out, DeviceData device, FlowData flowData,
                                    String dataFileLocation) {
        FlowElement<?, ?> engine = getFlowElement(flowData);
        String content = "";
        if (engine instanceof CloudRequestEngine) {
            content = "<p>The following values were detected using the <strong>cloud " +
                    "device detection engine</strong>.</p>";
        } else {
            // Lite or Enterprise
            String dataTier = ((DeviceDetectionHashEngine)engine).getDataSourceTier();
            // date of creation
            Date fileDate = ((DeviceDetectionHashEngine)engine).getDataFilePublishedDate();

            long daysOld = ChronoUnit.DAYS.between(fileDate.toInstant(), Instant.now());
            String displayDate = new SimpleDateFormat("yyyy-MM-dd").format(fileDate);

            content = String.format("<p>The following values detected using the " +
                    "<strong>on-premise device detection engine</strong> using a '%s' data file, " +
                    "created %s, %d days ago, from location '%s'.</p>",
                    dataTier, displayDate, daysOld, dataFileLocation);
        }
        // language=html
        out.append(
                "<h2 class=\"c-eg-page__heading\">Device Data</h2>\n" +
                        "<div id=\"content\">\n" +
                        content +
                        "    <table class=\"c-eg-table\">\n" +
                        "        <thead class=\"c-eg-table__head\"><tr class=\"c-eg-table__row\">" +
                        "<th class=\"c-eg-table__cell\">Key</th>" +
                        "<th class=\"c-eg-table__cell\">Value</th></tr></thead>\n" +
                        "        <tbody>\n" +
                        "        <tr class=\"c-eg-table__row c-eg-table__row--alt\"><td class=\"c-eg-table__cell c-eg-table__cell--key\">Hardware Vendor</td><td class=\"c-eg-table__cell\">" + asString(tryGet(device::getHardwareVendor)) + "</td></tr>\n" +
                        "        <tr class=\"c-eg-table__row\"><td class=\"c-eg-table__cell c-eg-table__cell--key\">Hardware Name</td><td class=\"c-eg-table__cell\">" + asString(tryGet(device::getHardwareName)) + "</td></tr>\n" +
                        "        <tr class=\"c-eg-table__row c-eg-table__row--alt\"><td class=\"c-eg-table__cell c-eg-table__cell--key\">Device Type</td><td class=\"c-eg-table__cell\">" + asString(tryGet(device::getDeviceType)) + "</td></tr>\n" +
                        "        <tr class=\"c-eg-table__row\"><td class=\"c-eg-table__cell c-eg-table__cell--key\">Platform Vendor</td><td class=\"c-eg-table__cell\">" + asString(tryGet(device::getPlatformVendor)) + "</td></tr>\n" +
                        "        <tr class=\"c-eg-table__row c-eg-table__row--alt\"><td class=\"c-eg-table__cell c-eg-table__cell--key\">Platform Name</td><td class=\"c-eg-table__cell\">" + asString(tryGet(device::getPlatformName)) + "</td></tr>\n" +
                        "        <tr class=\"c-eg-table__row\"><td class=\"c-eg-table__cell c-eg-table__cell--key\">Platform Version</td><td class=\"c-eg-table__cell\">" + asString(tryGet(device::getPlatformVersion)) + "</td></tr>\n" +
                        "        <tr class=\"c-eg-table__row c-eg-table__row--alt\"><td class=\"c-eg-table__cell c-eg-table__cell--key\">Browser Vendor</td><td class=\"c-eg-table__cell\">" + asString(tryGet(device::getBrowserVendor)) + "</td></tr>\n" +
                        "        <tr class=\"c-eg-table__row\"><td class=\"c-eg-table__cell c-eg-table__cell--key\">Browser Name</td><td class=\"c-eg-table__cell\">" + asString(tryGet((device::getBrowserName))) + "</td></tr>\n" +
                        "        <tr class=\"c-eg-table__row c-eg-table__row--alt\"><td class=\"c-eg-table__cell c-eg-table__cell--key\">Browser Version</td><td class=\"c-eg-table__cell\">" + asString(tryGet(device::getBrowserVersion)) + "</td></tr>\n" +
                        "        </tbody>\n" +
                        "    </table>\n" +
                        "</div>\n");
    }

    public static void doUachInfo(PrintWriter out) {
        //language=html
        out.append(

                "    <p>" +
                        "    A browser that supports client hints sends <code>Sec-CH-UA</code>, <code>Sec-CH-UA-Platform</code>"+
                        "    and <code>Sec-CH-UA-Mobile</code> HTTP headers along with the <code>User-Agent</code> header." +
                        "    <p>\n" +
                        "    If the server determines that the browser supports client hints, then" +
                        "    it may request additional client hints headers by setting the" +
                        "    <code>Accept-CH</code> header in the response." +
                        "    <p>\n" +
                        "    Refresh the page to send another request to the server. This time, some" +
                        "    additional client hints headers that have been requested by the server" +
                        "    may be included. The browser remembers the server's request to " +
                        "    add those headers and adds those additional headers on each subsequent request" +
                        "    (which is why this example works best in a \"private\" window if you launch the test" +
                        "    more than once).<p>" +
                        "    \n");
    }

    /**
     * Helper to output a contact-us message banner as the final element inside the
     * page container. The variant is chosen from the engine in use:
     * <ul>
     *   <li>Cloud engine: the cloud variant is shown unconditionally, as these cloud
     *   getting-started examples are free by design.</li>
     *   <li>On-premise engine: the on-premise variant is shown only when the engine
     *   reports the free 'Lite' data tier.</li>
     * </ul>
     * @param out the PrintWriter to write to
     * @param flowData the flowdata used to determine the engine and, for on-premise, the tier
     */
    public static void doContactUsMessage(PrintWriter out, FlowData flowData) {
        FlowElement<?, ?> engine = getFlowElement(flowData);
        if (engine instanceof CloudRequestEngine) {
            // Cloud getting-started example - free by design, always show.
            doContactUsMessage(out, true);
        } else {
            // On-premise engine - only show on the free Lite data tier.
            boolean isLite = ((DeviceDetectionHashEngine) engine)
                    .getDataSourceTier().equals("Lite");
            if (isLite) {
                doContactUsMessage(out, false);
            }
        }
    }

    /**
     * Outputs the contact-us message banner markup.
     * @param out the PrintWriter to write to
     * @param cloud true for the cloud variant, false for the on-premise variant
     */
    private static void doContactUsMessage(PrintWriter out, boolean cloud) {
        // language=html
        if (cloud) {
            out.append("<div class=\"c-eg-message\">\n" +
                    "  <p class=\"c-eg-message__text\">Want to try on-premise? <a href=\"https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=device-detection-java-examples&utm_content=web-shared-src-main-java-fiftyone-devicedetection-examples-web-htmlcontenthelper.java&utm_term=contact-us\">Contact us</a> to discuss requirements.</p>\n" +
                    "  <a class=\"b-btn c-eg-message__cta\" href=\"https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=device-detection-java-examples&utm_content=web-shared-src-main-java-fiftyone-devicedetection-examples-web-htmlcontenthelper.java&utm_term=contact-us\">Contact us</a>\n" +
                    "</div>\n");
        } else {
            out.append("<div class=\"c-eg-message\">\n" +
                    "  <p class=\"c-eg-message__text\">The paid data file adds daily automatic updates, non-human identification and IP intelligence. <a href=\"https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=device-detection-java-examples&utm_content=web-shared-src-main-java-fiftyone-devicedetection-examples-web-htmlcontenthelper.java&utm_term=contact-us\">Contact us</a> to explore the options.</p>\n" +
                    "  <a class=\"b-btn c-eg-message__cta\" href=\"https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=device-detection-java-examples&utm_content=web-shared-src-main-java-fiftyone-devicedetection-examples-web-htmlcontenthelper.java&utm_term=contact-us\">Contact us</a>\n" +
                    "</div>\n");
        }
    }

    public static void doHtmlPostamble(PrintWriter out, FlowData flowData) {
        // language=html
        doContactUsMessage(out, flowData);
        out.append("\n</div>\n</body></html>\n");
    }


    static void doStaticText(PrintWriter out, String location) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(
                getFilePath(location)))) {
            br.lines().forEach(out::println);
        }
    }
}
