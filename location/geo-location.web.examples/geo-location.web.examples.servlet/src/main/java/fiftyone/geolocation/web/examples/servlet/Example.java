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

package fiftyone.geolocation.web.examples.servlet;

import fiftyone.geolocation.core.data.GeoData;
import fiftyone.geolocation.web.examples.shared.EmbedTomcat;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.NoValueException;
import fiftyone.pipeline.web.services.FlowDataProviderCore;
import org.apache.catalina.LifecycleException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @example servlet/Example.java
 * Servlet geo location example
 *
 * This example shows how to:
 *
 * 1. Set up configuration options to add elements to the 51Degrees Pipeline.
 *
 * @include geo-location.web.examples.servlet/src/main/webapp/WEB-INF/51Degrees-Cloud.xml
 *
 * 2. Configure the filter and map it to be run for all URLs.
 * ```{xml}
 * <web-app version="3.1" xmlns="http://xmlns.jcp.org/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd">
 *     ...
 *     <filter>
 *         <filter-name>Pipeline</filter-name>
 *         <filter-class>fiftyone.pipeline.web.PipelineFilter</filter-class>
 *         <init-param>
 *             <param-name>clientside-properties-enabled</param-name>
 *             <param-value>true</param-value>
 *         </init-param>
 *     </filter>
 *     <filter-mapping>
 *         <filter-name>Pipeline</filter-name>
 *         <url-pattern>/*</url-pattern>
 *     </filter-mapping>
 *     ...
 * ```
 *
 * 3. Add the `FlowDataProvider` to the servlet.
 *
 * 4. User the results contained in the flow data to display something on a page, and
 * optionally add the client-side code to improve detection accuracy on devices like iPhones.
 *
 * ## Servlet
 */
public class Example extends HttpServlet {
    public static void main(String[] args) throws LifecycleException {
        EmbedTomcat.runWebApp("geo-location.web.examples/geo-location.web.examples.servlet/src/main/webapp",
            "geo-location.web.examples/geo-location.web.examples.servlet/target",
            8080);
    }
    /**
     * Serializable class version number, which is used during deserialization.
     */
    private static final long serialVersionUID = -7420567024186859548L;
    FlowDataProviderCore flowDataProvider = new FlowDataProviderCore.Default();

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, NoValueException {
        FlowData data = flowDataProvider.getFlowData(request);
        GeoData geo = data.get(GeoData.class);
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet Example</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<script src=\"/geo-location.web.examples.servlet/51Degrees.core.js\"></script>");

            AspectPropertyValue<String> town = geo.getTown();
            AspectPropertyValue<String> county = geo.getCounty();
            AspectPropertyValue<String> state = geo.getState();
            AspectPropertyValue<String> country = geo.getCountry();

            out.println("<h2>Example</h2>\n" +
                "\n" +
                "<div id=\"content\">\n" +
                "    <p>\n" +
                "        The following values are determined sever-side on the first request.\n" +
                "        As the server has no location information to work from, these \n" +
                "        values will all be unknown:\n" +
                "    </p>\n" +
                "    <dl>\n" +
                "        <dt><strong>Country</strong></dt>\n" +
                "        <dv>" + (county.hasValue() ? county.getValue() : "Unknown: " + county.getNoValueMessage()) + "</dv>\n" +
                "        <dt><strong>State</strong></dt>\n" +
                "        <dv>" + (state.hasValue() ? state.getValue() : "Unknown: " +  state.getNoValueMessage()) + "</dv>\n" +
                "        <dt><strong>County</strong></dt>\n" +
                "        <dv>" + (country.hasValue() ? country.getValue() : "Unknown: " +  country.getNoValueMessage()) + "</dv>\n" +
                "        <dt><strong>Town/City</strong></dt>\n" +
                "        <dv>" + (town.hasValue() ? town.getValue() : "Unknown: " +  town.getNoValueMessage()) + "</dv>\n" +
                "    </dl>   \n" +
                "    <p>\n" +
                "        When the button below is clicked, JavaScript running on the client-side will be used to obtain additional evidence (i.e. the location information from the device). If no additional information appears then it may indicate an external problem such as JavaScript being disabled in your browser.\n" +
                "    </p>\n" +
                "    <p>\n" +
                "        Note that the accuracy of the information is dependent on the accuracy of the location data returned by your device. Any device that lacks GPS is likely to return a highly inaccurate result. Among devices with GPS, some have a significantly lower margin of error than others.\n" +
                "    </p>\n" +
                "    <button type=\"button\" onclick=\"buttonClicked()\">Use my location</button>\n" +
                "</div>\n" +
                "\n" +
                "<script>\n" +
                "    buttonClicked = function () {\n" +
                "        // This function will fire when the JSON data object is updated\n" +
                "        // with information from the server.\n" +
                "        // The sequence is:\n" +
                "        // 1. Response contains JavaScript property 'JavaScript'. This is not executed immediately on the client as it will prompt the user to allow access to location.\n" +
                "        // 2. When the button is clicked, the fod.complete function is called, passing 'location' as the second parameter. This lets the code know that we want to execute\n" +
                "        // any JavaScript needed to obtain the location data that is needed to determine the user's postal address details.\n" +
                "        // 3. The execution of the location JavaScript triggers a background callback to the webserver that includes the new evidence (i.e. lat/lon).\n" +
                "        // 4. The web server responds with new JSON data that contains the updated property values based on the new evidence.\n" +
                "        // 5. The JavaScript integrates the new JSON data and fires the 'complete' callback function below, which then displays the results.\n" +
                "        fod.complete(function (data) {\n" +
                "            let fieldValues = [];\n" +
                "            if (data.location) {\n" +
                "                fieldValues.push([\"Country\", data.location.country]);\n" +
                "                fieldValues.push([\"State\", data.location.state]);\n" +
                "                fieldValues.push([\"County\", data.location.country]);\n" +
                "                fieldValues.push([\"Town/City\", data.location.town]);\n" +
                "            }\n" +
                "            else {\n" +
                "                fieldValue.push([\"Location data is empty. This probably means that something has gone wrong with the JavaScript evaluation.\", \"\"])\n" +
                "            }\n" +
                "            displayValues(fieldValues);\n" +
                "        }, 'location');\n" +
                "    }\n" +
                "\n" +
                "    function displayValues(fieldValues) {\n" +
                "        var list = document.createElement('dl');\n" +
                "        fieldValues.forEach(function (entry) {\n" +
                "            var name = document.createElement('dt');\n" +
                "            var value = document.createElement('dv');\n" +
                "            var bold = document.createElement('strong');\n" +
                "            var fieldname = document.createTextNode(entry[0]);\n" +
                "            var fieldvalue = document.createTextNode(entry[1]);\n" +
                "            bold.appendChild(fieldname);\n" +
                "            name.appendChild(bold);\n" +
                "            value.appendChild(fieldvalue);\n" +
                "            list.appendChild(name);\n" +
                "            list.appendChild(value);\n" +
                "        });\n" +
                "\n" +
                "        var element = document.getElementById('content');\n" +
                "        element.appendChild(list);\n" +
                "    }\n" +
                "</script>");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (NoValueException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}