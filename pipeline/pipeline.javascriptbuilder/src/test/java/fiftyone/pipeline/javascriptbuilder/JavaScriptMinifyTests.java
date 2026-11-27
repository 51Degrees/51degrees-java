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
import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElement;
import fiftyone.pipeline.javascriptbuilder.flowelements.JavaScriptBuilderElementBuilder;
import fiftyone.pipeline.jsonbuilder.flowelements.JsonBuilderElementBuilder;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The script is minified, or not, as configured, and minification is on
 * unless it is turned off. See the Process section and the SetMinify
 * configuration option of the JavaScript Builder specification.
 * @see <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/pipeline-elements/javascript-builder.md#L105-L107">Specification</a>
 */
public class JavaScriptMinifyTests {

    /**
     * A comment in the template, which a minified script does not carry.
     */
    private static final String TEMPLATE_COMMENT =
        "// Log any errors returned in the JSON object.";

    /**
     * A value that looks like code a minifier would change, and holds
     * quotes and a line break, so it shows whether minification has changed
     * the data the script carries.
     */
    private static final String VALUE =
        "a  /* not a comment */  b // nor this\n\t'c' \"d\" {{_objName}}";

    private static final String BROWSER =
        "var window = globalThis;\n" +
        "var console = { log: function() {}, warn: function() {},\n" +
        "    error: function() {} };\n" +
        "var document = { cookie: '' };\n" +
        "var sessionStorage = (function() {\n" +
        "    var data = {};\n" +
        "    return {\n" +
        "        get length() { return Object.keys(data).length; },\n" +
        "        key: function(i) {\n" +
        "            var k = Object.keys(data)[i];\n" +
        "            return k === undefined ? null : k; },\n" +
        "        getItem: function(k) {\n" +
        "            return k in data ? data[k] : null; },\n" +
        "        setItem: function(k, v) { data[k] = String(v); },\n" +
        "        removeItem: function(k) { delete data[k]; }\n" +
        "    };\n" +
        "})();\n";

    private final TestLoggerFactory loggerFactory = new TestLoggerFactory(null);

    /**
     * Renders the script for one request.
     * @param minify null to leave the builder's default, otherwise the
     *               value given to setMinify
     * @param endpoint the endpoint, or an empty string for no callback
     * @param cookies the value given to setEnableCookies
     */
    private String render(Boolean minify, String endpoint, boolean cookies)
        throws Exception {
        return render(minify, endpoint, cookies, false, false);
    }

    /**
     * Renders the script for one request, choosing every setting the
     * minified forms are held by.
     * @param minify null to leave the builder's default, otherwise the
     *               value given to setMinify
     * @param endpoint the endpoint, or an empty string for no callback
     * @param cookies the value given to setEnableCookies
     * @param promises true if the request's browser supports promises
     * @param delayed true if the JSON holds a delayed execution property
     */
    private String render(
        Boolean minify,
        String endpoint,
        boolean cookies,
        boolean promises,
        boolean delayed) throws Exception {
        JavaScriptBuilderElementBuilder builder =
            new JavaScriptBuilderElementBuilder(loggerFactory)
                .setEndpoint(endpoint)
                .setHost("example.com")
                .setProtocol("https")
                .setEnableCookies(cookies);
        if (minify != null) {
            builder.setMinify(minify);
        }
        JavaScriptBuilderElement jsElement = builder.build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
            .addFlowElement(new SequenceElementBuilder(loggerFactory).build())
            .addFlowElement(new ValuesElement(promises, delayed))
            .addFlowElement(new JsonBuilderElementBuilder(loggerFactory).build())
            .addFlowElement(jsElement)
            .build();
             FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence("query.some-value", "x y");
            // The same session for every render, so two renders differ only
            // in whether they are minified.
            flowData.addEvidence("query.session-id", "abc-123");
            flowData.addEvidence("query.sequence", "1");
            flowData.process();
            return flowData.getFromElement(jsElement).getJavaScript();
        }
    }

    /**
     * With no setting the script is minified, so it carries none of the
     * template's comments and is much shorter than the script made with
     * minification turned off.
     */
    @Test
    public void Minify_Default_ScriptIsMinified() throws Exception {
        String minified = render(null, "/json", true);
        String asWritten = render(false, "/json", true);

        assertFalse(minified.contains(TEMPLATE_COMMENT),
            "The default script still carries the template's comments.");
        assertTrue(minified.length() < asWritten.length() * 3 / 4,
            "The default script is " + minified.length() +
            " characters and the script with minification off is " +
            asWritten.length() + ".");
    }

    /**
     * With minification turned on the script is minified as it is with no
     * setting.
     */
    @Test
    public void Minify_On_ScriptIsMinified() throws Exception {
        assertEquals(render(null, "/json", true), render(true, "/json", true));
    }

    /**
     * With minification turned off the script is the template as written.
     */
    @Test
    public void Minify_Off_ScriptIsNotMinified() throws Exception {
        assertTrue(render(false, "/json", true).contains(TEMPLATE_COMMENT));
    }

    /**
     * Minification changes the code and not what the script carries: the
     * JSON, the callback URL, the parameters, the session id, the sequence
     * and the object name are the same as in the script made with
     * minification turned off.
     */
    @Test
    public void Minify_On_ValuesUnchanged() throws Exception {
        String minified = render(true, "/json", true);
        String asWritten = render(false, "/json", true);

        assertTrue(json(asWritten).similar(json(minified)),
            "The JSON differs.\n" + json(asWritten).toString(2) + "\n" +
            json(minified).toString(2));
        assertEquals(VALUE, json(minified).getJSONObject("values")
            .getString("text"));
        assertTrue(minified.contains("'https://example.com/json'"),
            "The callback URL is missing.");
        assertEquals(
            parameters(asWritten).toString(),
            parameters(minified).toString());
        assertTrue(minified.contains("\"" + sessionId(asWritten) + "\""),
            "The session id is missing.");
        assertTrue(Pattern.compile("var fod\\s*=\\s*new fiftyoneDegreesManager")
            .matcher(minified).find(), "The object is not created.");
    }

    /**
     * Every choice of the four settings the minified forms are held by,
     * being whether there is a callback, whether cookies are enabled,
     * whether the browser supports promises and whether the JSON holds a
     * delayed execution property.
     */
    static Stream<Arguments> everyForm() {
        List<Arguments> forms = new ArrayList<>();
        for (String endpoint : new String[] {"/json", ""}) {
            for (boolean cookies : new boolean[] {true, false}) {
                for (boolean promises : new boolean[] {true, false}) {
                    for (boolean delayed : new boolean[] {true, false}) {
                        forms.add(Arguments.of(
                            endpoint, cookies, promises, delayed));
                    }
                }
            }
        }
        return forms.stream();
    }

    /**
     * Each of the four settings changes the script as written, so the
     * sixteen choices are sixteen different scripts and the forms tested
     * below are every form the element holds.
     */
    @Test
    public void Minify_EveryForm_IsADifferentScript() throws Exception {
        Set<String> scripts = new HashSet<>();
        Iterable<Arguments> forms = everyForm()::iterator;
        for (Arguments form : forms) {
            Object[] a = form.get();
            scripts.add(render(false, (String) a[0], (Boolean) a[1],
                (Boolean) a[2], (Boolean) a[3]));
        }
        assertEquals(16, scripts.size(),
            "The four settings do not give sixteen different scripts.");
    }

    /**
     * Every form of the minified script parses.
     */
    @ParameterizedTest
    @MethodSource("everyForm")
    public void Minify_On_EveryFormParses(
        String endpoint,
        boolean cookies,
        boolean promises,
        boolean delayed) throws Exception {
        String script = render(true, endpoint, cookies, promises, delayed);
        try (Context context = context()) {
            try {
                context.parse(Source.create("js", script));
            } catch (PolyglotException e) {
                fail("The minified script does not parse: " + e.getMessage());
            }
        }
    }

    /**
     * The minified script runs and creates the object with the values from
     * the JSON.
     */
    @Test
    public void Minify_On_ScriptRuns() throws Exception {
        String script = render(true, "", true);
        try (Context context = context()) {
            context.eval("js", BROWSER);
            context.eval(Source.create("js", script));
            Value fod = context.eval("js", "window").getMember("fod");
            assertFalse(fod == null || fod.isNull(), "fod was not created.");
            assertTrue(fod.getMember("complete").canExecute());
            assertEquals(VALUE,
                fod.getMember("values").getMember("text").asString());
        }
    }

    private static Context context() {
        return Context.newBuilder("js")
            .option("engine.WarnInterpreterOnly", "false")
            .build();
    }

    /**
     * The JSON the script assigns to its json variable.
     */
    private static JSONObject json(String script) {
        Matcher matcher = Pattern.compile("var json\\s*=\\s*").matcher(script);
        assertTrue(matcher.find(), "The script holds no JSON.");
        return (JSONObject) new JSONTokener(script.substring(matcher.end()))
            .nextValue();
    }

    /**
     * The parameters object the script's request parameters function
     * returns.
     */
    private static JSONObject parameters(String script) {
        Matcher matcher = Pattern.compile(
            "renderedParameters\\s*=\\s*function\\(\\)\\s*\\{\\s*return\\s*")
            .matcher(script);
        assertTrue(matcher.find(), "The script holds no parameters.");
        return (JSONObject) new JSONTokener(script.substring(matcher.end()))
            .nextValue();
    }

    private static String sessionId(String script) {
        Matcher matcher = Pattern.compile("var sessionId\\s*=\\s*\"([^\"]*)\"")
            .matcher(script);
        assertTrue(matcher.find(), "The script holds no session id.");
        return matcher.group(1);
    }

    /**
     * An element that adds a value which looks like code, in place of an
     * engine.
     */
    private static class ValuesElement
        extends FlowElementBase<ElementDataBase, ElementPropertyMetaData> {

        @Override
        protected Class<ElementDataBase> getDataType() {
            return ElementDataBase.class;
        }

        private final boolean promises;

        private final boolean delayed;

        /**
         * @param promises true to give Promise as "Full", which the
         *                 JavaScript Builder reads as the browser
         *                 supporting promises
         * @param delayed true to add a delayed execution property
         */
        ValuesElement(boolean promises, boolean delayed) {
            super(new TestLoggerFactory(null).getLogger("values"), null);
            this.promises = promises;
            this.delayed = delayed;
        }

        @Override
        protected void processInternal(FlowData data) {
            ElementDataBase values = new ElementDataBase(logger, data) { };
            values.put("text", VALUE);
            if (promises) {
                values.put("Promise",
                    new AspectPropertyValueDefault<>("Full"));
            }
            if (delayed) {
                values.put("delayed", new JavaScript("var delayed = 1;"));
            }
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
            List<ElementPropertyMetaData> properties = new ArrayList<>();
            properties.add(new ElementPropertyMetaDataDefault(
                "text", this, "test", String.class, true));
            if (promises) {
                properties.add(new ElementPropertyMetaDataDefault(
                    "Promise", this, "test", String.class, true));
            }
            if (delayed) {
                properties.add(new ElementPropertyMetaDataDefault(
                    "delayed", this, "test", JavaScript.class, true, null,
                    true, Collections.<String>emptyList()));
            }
            return properties;
        }

        @Override
        protected void managedResourcesCleanup() {
        }

        @Override
        protected void unmanagedResourcesCleanup() {
        }
    }
}
