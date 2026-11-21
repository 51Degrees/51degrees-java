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

package fiftyone.pipeline.web;

import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import fiftyone.pipeline.web.services.ClientsidePropertyServiceCore;
import fiftyone.pipeline.web.services.FlowDataProviderCore;
import fiftyone.pipeline.web.services.WebRequestEvidenceServiceCore;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The JSON endpoint answers with JSON in the same form as the JSON the
 * JavaScript holds, as the SetEndpoint row of the JavaScript Builder's
 * configuration options asks.
 * @see <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/pipeline-elements/javascript-builder.md#L348">Specification</a>
 */
public class ClientsideJsonEndpointTests {

    private static final ILoggerFactory LOGGERS =
        LoggerFactory.getILoggerFactory();

    /**
     * The text that the template puts before the JSON in the script, with
     * or without the white space that minifying takes out.
     */
    private static final Pattern JSON_IN_SCRIPT =
        Pattern.compile("var json\\s*=\\s*");

    /**
     * The JSON served by the JSON endpoint and the JSON the script served by
     * the JavaScript endpoint holds, for one request, are the same, with a
     * value, a Property assigned no value and its nullreason, and the
     * JavaScript Property and session values the builders add.
     */
    @Test
    public void JsonEndpoint_SameFormAsJsonInJavaScript() throws Exception {
        try (Pipeline pipeline = new PipelineBuilder(LOGGERS)
                .addFlowElement(new SequenceElementBuilder(LOGGERS).build())
                .addFlowElement(new ValuesElement())
                .addFlowElement(new JsonBuilderElementBuilder(LOGGERS).build())
                .addFlowElement(new JavaScriptBuilderElementBuilder(LOGGERS)
                    .setEndpoint("/51dpipeline/json")
                    .build())
                .setAutoCloseElements(true)
                .build();
             FlowData data = pipeline.createFlowData()) {
            HttpServletRequest request = request();
            new WebRequestEvidenceServiceCore.Default()
                .addEvidenceFromRequest(data, request);
            data.process();
            assertTrue("Processing recorded errors: " + data.getErrors(),
                data.getErrors().isEmpty());

            FlowDataProviderCore provider = mock(FlowDataProviderCore.class);
            when(provider.getFlowData(request)).thenReturn(data);
            ClientsidePropertyServiceCore service =
                new ClientsidePropertyServiceCore.Default(provider, pipeline);

            StringWriter json = new StringWriter();
            service.serveJson(request, response(json));
            StringWriter javaScript = new StringWriter();
            service.serveJavascript(request, response(javaScript));

            JSONObject served = new JSONObject(json.toString());
            JSONObject embedded = jsonInScript(javaScript.toString());
            assertTrue("The JSON endpoint answered\n" + served.toString(2) +
                    "\nbut the script holds\n" + embedded.toString(2),
                served.similar(embedded));
            JSONObject values = served.getJSONObject("values");
            assertEquals("a value", values.getString("value"));
            assertTrue(values.isNull("novalue"));
            assertEquals("No value for the test.",
                values.getString("novaluenullreason"));
        }
    }

    /**
     * The JSON object the script assigns to its json variable.
     */
    private static JSONObject jsonInScript(String javaScript) {
        Matcher matcher = JSON_IN_SCRIPT.matcher(javaScript);
        assertTrue("The script holds no JSON: " + javaScript, matcher.find());
        return (JSONObject) new JSONTokener(
            javaScript.substring(matcher.end()))
            .nextValue();
    }

    private static HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/51dpipeline/json");
        when(request.getHeaderNames()).thenReturn(
            Collections.enumeration(Arrays.asList("Host")));
        when(request.getHeader("Host")).thenReturn("example.com");
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(request.getScheme()).thenReturn("https");
        return request;
    }

    private static HttpServletResponse response(StringWriter body)
        throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(body, true));
        return response;
    }

    /**
     * An element that adds a value, a Property assigned no value and a
     * JavaScript Property, in place of an engine.
     */
    private static class ValuesElement
        extends FlowElementBase<ElementDataBase, ElementPropertyMetaData> {

        ValuesElement() {
            super(LoggerFactory.getLogger(ValuesElement.class), null);
        }

        @Override
        protected void processInternal(FlowData data) {
            ElementDataBase values =
                new ElementDataBase(logger, data) { };
            values.put("value", "a value");
            AspectPropertyValueDefault<String> noValue =
                new AspectPropertyValueDefault<>();
            noValue.setNoValueMessage("No value for the test.");
            values.put("novalue", noValue);
            values.put("javascript", "var a = 1;");
            data.getOrAdd(getElementDataKey(), d -> values);
        }

        @Override
        public String getElementDataKey() {
            return "values";
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return new EvidenceKeyFilterWhitelist(
                Collections.<String>emptyList());
        }

        @Override
        public List<ElementPropertyMetaData> getProperties() {
            return Arrays.<ElementPropertyMetaData>asList(
                new ElementPropertyMetaDataDefault(
                    "value", this, "test", String.class, true),
                new ElementPropertyMetaDataDefault(
                    "novalue", this, "test", String.class, true),
                new ElementPropertyMetaDataDefault(
                    "javascript", this, "test", String.class, true));
        }

        @Override
        protected void managedResourcesCleanup() {
        }

        @Override
        protected void unmanagedResourcesCleanup() {
        }
    }
}
