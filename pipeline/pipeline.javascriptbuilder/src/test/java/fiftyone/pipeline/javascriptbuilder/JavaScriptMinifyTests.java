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

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptMinifier;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The minification of the script, which the SetMinify option turns on and
 * off and which is on by default, as the configuration options in the
 * JavaScript Builder specification give. The performance of minification is
 * tested in {@link fiftyone.pipeline.javascriptbuilder.performance.MinifyOverheadTests}.
 */
public class JavaScriptMinifyTests {
    private final TestLoggerFactory loggerFactory;

    public JavaScriptMinifyTests() {
        ILoggerFactory internalLogger = mock(ILoggerFactory.class);
        when(internalLogger.getLogger(anyString()))
            .thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internalLogger);
    }

    /**
     * Render the script with the builder given.
     */
    static String render(
        ILoggerFactory loggerFactory,
        JavaScriptBuilderElementBuilder builder) throws Exception {
        JavaScriptBuilderElement jsElement = builder
            .setEndpoint("/json")
            .setHost("example.com")
            .setProtocol("https")
            .build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(new SequenceElementBuilder(loggerFactory).build())
            .addFlowElement(new JsonBuilderElementBuilder(loggerFactory).build())
            .addFlowElement(jsElement)
            .build();
             FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence("query.user-agent", "iPhone");
            flowData.process();
            return flowData.getFromElement(jsElement).getJavaScript();
        }
    }

    @Test
    public void Default_IsMinified() throws Exception {
        String full = render(loggerFactory,
            new JavaScriptBuilderElementBuilder(loggerFactory)
                .setMinify(false));
        String byDefault = render(loggerFactory,
            new JavaScriptBuilderElementBuilder(loggerFactory));

        assertTrue(full.contains("// Set to true when the `catchError` is called."),
            "The full script has lost its comments.");
        assertFalse(byDefault.contains("// Set to true when the `catchError` is called."),
            "The default script was not minified.");
        assertTrue(byDefault.length() < full.length() * 3 / 4,
            "Minified " + byDefault.length() + " characters from " +
            full.length() + ".");
    }

    @Test
    public void Minify_RemovesCommentsAndIndentationKeepsLineBreaks() {
        assertEquals("var a=1\nvar b=a + 1;",
            JavaScriptMinifier.minify(
                "  // first\n  var a = 1 /* one */\n\n\n" +
                "    var b = a + 1; // second\n"));
    }

    @Test
    public void Minify_KeepsStringsTemplatesAndRegularExpressions() {
        assertEquals("var u='http://x/*y*/'",
            JavaScriptMinifier.minify("var u = 'http://x/*y*/'"));
        assertEquals("var s=\"a  b // c\"",
            JavaScriptMinifier.minify("var s = \"a  b // c\""));
        assertEquals("var t=`one\n    two // three`",
            JavaScriptMinifier.minify("var t = `one\n    two // three`"));
        assertEquals("var r=/[/]*\\/ x/g.test(s)",
            JavaScriptMinifier.minify("var r = /[/]*\\/ x/g.test(s)"));
        assertEquals("return /a b/.test(s)",
            JavaScriptMinifier.minify("return /a b/.test(s)"));
        assertEquals("var e=new RegExp('a\\\\.b\\'c','g')",
            JavaScriptMinifier.minify("var e = new RegExp('a\\\\.b\\'c', 'g')"));
    }

    @Test
    public void Minify_KeepsTheMeaningOfOperators() {
        assertEquals("x=a / b / c",
            JavaScriptMinifier.minify("x = a / b / c"));
        assertEquals("x=a + +b - -c",
            JavaScriptMinifier.minify("x = a + +b - -c"));
        assertEquals("if(a < !b){return c}",
            JavaScriptMinifier.minify("if (a < !b) { return c }"));
        assertEquals("x=a-- > b",
            JavaScriptMinifier.minify("x = a-- > b"));
        assertEquals("typeof a",
            JavaScriptMinifier.minify("typeof   a"));
    }
}
