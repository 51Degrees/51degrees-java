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

package fiftyone.pipeline.javascriptbuilder;

import fiftyone.common.testhelpers.TestLogger;
import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Checks the callback URL the rendered script is given. These tests render
 * the script only and need no browser.
 */
public class JavaScriptUrlTests {
    private final TestLoggerFactory loggerFactory;

    public JavaScriptUrlTests() {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString()))
            .thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
    }

    private String render(String contextRoot) throws Exception {
        JavaScriptBuilderElementBuilder builder =
            new JavaScriptBuilderElementBuilder(loggerFactory)
                .setEndpoint("/json")
                .setHost("example.com")
                .setProtocol("https");
        if (contextRoot != null) {
            builder.setContextRoot(contextRoot);
        }
        JavaScriptBuilderElement jsElement = builder.build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(new SequenceElementBuilder(loggerFactory).build())
            .addFlowElement(new JsonBuilderElementBuilder(loggerFactory).build())
            .addFlowElement(jsElement)
            .build();
             FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence("query.user-agent", "iPhone");
            flowData.addEvidence("query.id.usage", "non-marketing");
            flowData.process();
            return flowData.getFromElement(jsElement).getJavaScript();
        }
    }

    /**
     * Every URL the script requests, taken from the fetch and the
     * XMLHttpRequest calls in the rendered script.
     */
    private static List<String> requestUrls(String javaScript) {
        List<String> urls = new ArrayList<>();
        Matcher matcher = Pattern
            .compile("(?:fetch\\(|createCORSRequest\\('POST', )'([^']*)'")
            .matcher(javaScript);
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        return urls;
    }

    /**
     * The script is given protocol, host and endpoint only, as the .NET
     * builder renders it. Query evidence reaches the script through the
     * parameters object and nowhere else, so every builder renders the same
     * script for the same request.
     */
    @Test
    public void Url_CarriesNoQueryEvidence() throws Exception {
        String javaScript = render(null);

        List<String> urls = requestUrls(javaScript);
        assertFalse(urls.isEmpty(), "No request URL found in the script.");
        for (String url : urls) {
            assertEquals("https://example.com/json", url,
                "The script's URL must carry no query string.");
        }
        // The line assigning the parameters is found without naming the
        // declaration, as JavaScriptBuilderTests does, because the template
        // has named it both "parameters" and "renderedParameters".
        String parameters = null;
        for (String line : javaScript.split("\\r?\\n")) {
            if (line.toLowerCase().contains("parameters =")
                    && line.contains("{")) {
                parameters = line;
                break;
            }
        }
        assertNotNull(parameters, "No parameters object in the script.");
        assertTrue(parameters.contains("\"user-agent\":\"iPhone\""),
            "The parameters object lost the query evidence: " + parameters);
        assertTrue(parameters.contains("\"id.usage\":\"non-marketing\""),
            "The parameters object lost the query evidence: " + parameters);
    }

    /**
     * A context root still sits between the host and the endpoint, and the
     * URL still carries no query string.
     */
    @Test
    public void Url_KeepsContextRoot() throws Exception {
        List<String> urls = requestUrls(render("/app/"));
        assertFalse(urls.isEmpty(), "No request URL found in the script.");
        for (String url : urls) {
            assertEquals("https://example.com/app/json", url);
        }
    }

    /**
     * Loggers for the tests that read what the element logged. No logger is
     * behind them, so each one keeps the name it was created with.
     */
    private final TestLoggerFactory hostLoggerFactory =
        new TestLoggerFactory(null);

    /**
     * Renders one script for each host given, all through the same element,
     * with the host taken from the request's evidence.
     */
    private List<String> renderWithHosts(String... hosts) throws Exception {
        TestLoggerFactory factory = hostLoggerFactory;
        JavaScriptBuilderElement jsElement =
            new JavaScriptBuilderElementBuilder(factory)
                .setEndpoint("/json")
                .build();
        List<String> scripts = new ArrayList<>();
        try (Pipeline pipeline = new PipelineBuilder(factory)
            .addFlowElement(new SequenceElementBuilder(factory).build())
            .addFlowElement(new JsonBuilderElementBuilder(factory).build())
            .addFlowElement(jsElement)
            .build()) {
            for (String host : hosts) {
                try (FlowData flowData = pipeline.createFlowData()) {
                    flowData.addEvidence(Constants.EVIDENCE_HOST_KEY, host);
                    flowData.process();
                    scripts.add(
                        flowData.getFromElement(jsElement).getJavaScript());
                }
            }
        }
        return scripts;
    }

    /**
     * The errors the JavaScript builder element logged.
     */
    private List<String> builderErrors() {
        List<String> errors = new ArrayList<>();
        for (TestLogger logger : hostLoggerFactory.loggers) {
            if (JavaScriptBuilderElement.class.getName().equals(
                    logger.getName())) {
                errors.addAll(logger.errorsLogged);
            }
        }
        return errors;
    }

    /**
     * A host the check refuses gives a script with no callback, and the
     * refusal is logged as an error the first time only, whatever the
     * requests in between.
     */
    @Test
    public void Url_RefusedHost_NoCallbackAndLoggedOnce() throws Exception {
        List<String> scripts = renderWithHosts(
            "exa mple.com", "exa mple.com", "example.com", "exa mple.com");

        for (int i = 0; i < scripts.size(); i++) {
            String script = scripts.get(i);
            assertTrue(script.contains("new fiftyoneDegreesManager()"),
                "The script was not rendered.");
            if (i == 2) {
                assertFalse(requestUrls(script).isEmpty(),
                    "The valid host lost its callback.");
            } else {
                assertTrue(requestUrls(script).isEmpty(),
                    "A refused URL reached the script.");
                assertFalse(script.contains("exa mple.com"),
                    "The refused host reached the script.");
            }
        }
        List<String> errors = builderErrors();
        assertEquals(1, errors.size(), String.join("\n", errors));
        assertTrue(errors.get(0).contains("\"https://exa mple.com/json\""),
            errors.get(0));
    }

    /**
     * The URL is quoted and escaped in the log, so a line break in it cannot
     * start a new log line.
     */
    @Test
    public void Url_RefusedHost_LoggedOnOneLine() throws Exception {
        renderWithHosts("a\nb.com\r\u2028");

        List<String> errors = builderErrors();
        assertEquals(1, errors.size(), String.join("\n", errors));
        String error = errors.get(0);
        for (char c : new char[] {'\n', '\r', '\u2028'}) {
            assertTrue(error.indexOf(c) < 0, error);
        }
        assertTrue(error.contains("a\\nb.com\\r\\u2028"), error);
    }

    /**
     * A host made only of whitespace is not set, so the script has no
     * callback and nothing is logged.
     */
    @Test
    public void Url_BlankHost_NoCallbackAndNothingLogged() throws Exception {
        String script = renderWithHosts("  ").get(0);

        assertTrue(requestUrls(script).isEmpty(),
            "A blank host gave a callback.");
        assertTrue(builderErrors().isEmpty(),
            String.join("\n", builderErrors()));
    }

    /**
     * A host from the request reaches the script as the check writes it,
     * in lower case and with anything that could end the string the script
     * holds it in percent-encoded.
     */
    @Test
    public void Url_HostFromEvidence_WrittenAsChecked() throws Exception {
        String script =
            renderWithHosts("EXAMPLE.com/');alert(1);//").get(0);

        List<String> urls = requestUrls(script);
        assertFalse(urls.isEmpty(), "No request URL found in the script.");
        for (String url : urls) {
            assertEquals("https://example.com/%27);alert(1);//json", url);
        }
        assertFalse(script.contains("');alert(1)"),
            "The host ended the string.");
    }
}
