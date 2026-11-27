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

import fiftyone.devicedetection.examples.shared.FindOutMore;
import fiftyone.devicedetection.examples.shared.KeyHelper;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.web.Constants;
import fiftyone.pipeline.web.services.FlowDataProviderCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.devicedetection.examples.shared.KeyHelper.getOrSetTestResourceKey;
import static fiftyone.devicedetection.examples.web.HtmlContentHelper.*;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * This is the getting started Web/Cloud example showing use of the 51Degrees
 * supplied filter which automatically creates and configures a device detection pipeline.
 * <p>
 * To use this example you must obtain a resource key. A resource key suitable for
 * use with the example can be obtained here:
 * <a href="https://51degrees.com/configure/jqz435Nc?utm_source=code&amp;utm_medium=example&amp;utm_campaign=51degrees-java&amp;utm_content=web-getting-started.cloud-src-main-java-fiftyone-devicedetection-examples-web-gettingstartedwebcloud.java&amp;utm_term=gettingstartedwebcloud">https://51degrees.com/configure/jqz435Nc?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=web-getting-started.cloud-src-main-java-fiftyone-devicedetection-examples-web-gettingstartedwebcloud.java&utm_term=gettingstartedwebcloud-2</a>.
 * <p>
 * The configuration file for the pipeline is at src/main/webapp/WEB-INF/51Degrees-Cloud.xml
 * <p>
 * The server listens on port 5151 and stops when Enter is pressed. If the
 * _51DEGREES_EXAMPLE_HTTP_PORTS environment variable is set, the server listens
 * on the first port it lists instead and runs until the process is stopped,
 * which is how automated tests start it.
 */
public class GettingStartedWebCloud extends HttpServlet {
    /**
     * This example's tag in the links it writes when it has no resource
     * key.
     */
    private static final String EXAMPLE_TAG =
        "web-getting-started.cloud-src-main-java-fiftyone-devicedetection-examples-web-gettingstartedwebcloud.java";

    private static final long serialVersionUID = 1734154705981153540L;
    /**
     * The port the example listens on when the
     * _51DEGREES_EXAMPLE_HTTP_PORTS environment variable does not choose
     * another.
     */
    public static final int DEFAULT_PORT = 5151;
    public static String resourceBase = "examples/device-detection/web/getting-started.cloud/src/main/webapp";
    public static Logger logger = LoggerFactory.getLogger(GettingStartedWebCloud.class);

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));
        String resourceKey = KeyHelper.findResourceKey(
                args.length > 0 ? args[0] : null,
                KeyHelper.RESOURCE_KEY_VARIABLE);
        if (resourceKey == null) {
            KeyHelper.writeNoResourceKey(System.out, EXAMPLE_TAG,
                    KeyHelper.RESOURCE_KEY_VARIABLE,
                    KeyHelper.FREE_PROPERTIES_CONFIGURATION);
            FindOutMore.write(System.out, EXAMPLE_TAG,
                    FindOutMore.CLOUD_REPOSITORIES);
            return;
        }
        logger.info("Running Example {}", GettingStartedWebCloud.class);
        // Set the key as the system property the options file reads.
        getOrSetTestResourceKey(resourceKey);

        Integer port = EmbedJetty.portFrom(
                System.getenv(EmbedJetty.HTTP_PORTS_ENV_VAR));
        if (port != null) {
            // An automated run, such as the shared browser tests, sets the
            // port and has no console input, so keep the server running by
            // joining it rather than waiting on Enter.
            EmbedJetty.startWebApp(getFilePath(resourceBase).getAbsolutePath(), port).join();
        } else {
            // start Jetty with this WebApp
            EmbedJetty.runWebApp(getFilePath(resourceBase).getAbsolutePath(), DEFAULT_PORT);
        }
    }

    // HttpServlet is Serializable, but this servlet is never serialized.
    transient FlowDataProviderCore flowDataProvider = new FlowDataProviderCore.Default();

    /**
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws Exception when things go wrong
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        // the detection has already been carried out by the Filter
        // which is responsible to the lifecycle of the flowData - do NOT dispose
        // A Flow Data cannot be used by two threads at once. Work handed to
        // another thread or task gets the values it needs, read here on the
        // request's thread, not the Flow Data.
        FlowData flowData = flowDataProvider.getFlowData(request);
        // retrieve the device data from the flowdata
        DeviceData device = flowData.get(DeviceData.class);

        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            doHtmlPreamble(out, "Web Integration Cloud Example");

            // request main 51Degrees Client Side Script - this is automatically
            // served by inclusion of the PipelineFilter which intercepts the request
            // and serves dynamically generated JavaScript
            out.println("<script src=\"" + Constants.CORE_JS_NAME+ "\"></script>");

            // include description of example
            doStaticText(out, resourceBase + "/WEB-INF/html/example-description.html");

            // include a script to display the results of the client side detection
            doStaticText(out, resourceBase + "/WEB-INF/html/client-side-js-include.html");
            doDeviceData(out, device, flowData, null);
            doStaticText(out, resourceBase + "/WEB-INF/html/apple-detection.html");
            doEvidence(out, request, flowData);
            doResponseHeaders(out, response);
            doHtmlPostamble(out, flowData);
        }
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
/*!
 * @example GettingStartedWebCloud.java
 *
 * @include{doc} example-getting-started-web.txt
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/51degrees-java/blob/main/examples/device-detection/web/getting-started.cloud/src/main/java/fiftyone/devicedetection/examples/web/GettingStartedWebCloud.java).
 *
 * @include{doc} example-require-resourcekey.txt
 *
 * ## Overview
 *
 * The `PipelineFilter` to intercept requests and perform device detection. The results will be
 * stored in the HttpServletRequest object.
 * The filter will also handle setting response headers (e.g. Accept-CH for User-Agent
 * Client Hints) and serving requests for client-side JavaScript and JSON resources.
 *
 * The results of detection can be accessed by using a FlowDataProvider which
 * is responsible for managing the lifecycle of the flowData - do NOT dispose
 * ```{java}
 * FlowData flowData = flowDataProvider.getFlowData(request);
 * DeviceData device = flowData.get(DeviceData.class);
 * ...
 * ```
 *
 * Results can also be accessed in client-side code by using the `fod` object.
 *
 * ```{java}
 * window.onload = function () {
 *     fod.complete(function(data) {
 *         var hardwareName = data.device.hardwarename;
 *         alert(hardwareName.join(", "));
 *     }
 * }
 * ```
 *
 */

