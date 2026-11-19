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

package pipeline.developerexamples.derivedproperty;

import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.configuration.PipelineOptionsFactory;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.derivedproperty.data.IDerivedPropertyData;
import fiftyone.pipeline.derivedproperty.flowelements.DerivedOutputMetaData;
import fiftyone.pipeline.derivedproperty.flowelements.DerivedPropertyElement;
import fiftyone.pipeline.derivedproperty.flowelements.DerivedPropertyElementBuilder;
import fiftyone.pipeline.derivedproperty.flowelements.DerivedPropertyMetaData;
import fiftyone.pipeline.derivedproperty.flowelements.DerivedValueMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import pipeline.developerexamples.derivedproperty.flowelements.SessionElement;
import pipeline.developerexamples.derivedproperty.flowelements.SessionElementBuilder;

import java.io.File;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * @example derivedproperty/Main.java
 *
 * The Derived Property Element computes new properties from properties
 * that other elements in the Pipeline have already produced, following a
 * script written in YAML or in JSON. The element holds no data file, sends
 * no request over the network and needs no resource key, so this example
 * runs offline.
 *
 * The example shows four things.
 *
 * 1. A script held in the example as text, passed to the builder.
 * 2. A script read from a file beside the example,
 *    src/main/resources/ReaderEngagement.yaml.
 * 3. The same Pipeline built from a configuration file,
 *    src/main/resources/derived-property.xml, naming the builder and
 *    passing the script through the ScriptFiles build parameter.
 * 4. What a caller sees when a source property a script names is not
 *    available, which is a property with no value and a message naming
 *    what was missing.
 *
 * The source properties come from a small element written in this example,
 * SessionElement, so that nothing outside is needed. A real Pipeline puts
 * device detection or IP intelligence in that position instead.
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/51degrees-java/blob/main/pipeline/pipeline.developer-examples/pipeline.developer-examples.derivedproperty/src/main/java/pipeline/developerexamples/derivedproperty/Main.java).
 *
 * The script file the example reads is
 * @include pipeline.developer-examples.derivedproperty/src/main/resources/ReaderEngagement.yaml
 *
 * and the configuration file is
 * @include pipeline.developer-examples.derivedproperty/src/main/resources/derived-property.xml
 */
public class Main {

    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    /**
     * The script file beside the example, which both halves of the example
     * use, one by naming it on the builder and one by naming it in the
     * configuration file.
     */
    static final String SCRIPT_FILE = "ReaderEngagement.yaml";

    /**
     * The configuration file beside the example.
     */
    static final String CONFIGURATION_FILE = "derived-property.xml";

    /**
     * The property the script in {@link #SCRIPT_FILE} produces.
     */
    static final String ENGAGEMENT = "ReaderEngagement";

    /**
     * The property the script held below produces.
     */
    static final String EVERY_TEST = "ReaderEngagementIsHigh";

    /**
     * A second script, held as text rather than in a file, to show that a
     * script can come straight from your own code. The text is YAML here,
     * and JSON is read just as well. It reads the same three source
     * properties and says whether every one of the tests passed, so one
     * element runs more than one script over one set of source properties.
     */
    static final String EVERY_TEST_SCRIPT = String.join("\n",
        "Format: 1",
        "Name: ReaderEngagementIsHigh",
        "Version: \"1.0.0\"",
        "Output:",
        "  Name: ReaderEngagementIsHigh",
        "  Description: Whether every one of the ReaderEngagement tests " +
            "passed on this request.",
        "  ValueType: bool",
        "  IsList: false",
        "  Category: Example",
        "Checks:",
        "  SeveralPages: { Property: session.PagesViewed, Ge: 3 }",
        "  LongEnough:   { Property: session.SecondsSincePageLoad, Ge: 30 }",
        "  PointerMoved: { Property: session.PointerMoved, Eq: true }",
        "Rules:",
        "  - When: { Failed: Checks, Eq: 0 }",
        "    Then: true",
        "  - Else: false",
        "");

    /**
     * The rule above and below each part of the output.
     */
    private static final String RULE =
        "----------------------------------------------------------------";

    public static class Example {

        /**
         * Run the example.
         * @param output where to write what the example shows
         * @throws Exception where the example cannot run
         */
        public void run(PrintStream output) throws Exception {
            runFromCode(output);
            runFromConfiguration(output);
        }

        /**
         * The first way to configure the element, being the builder in
         * code. One element runs two scripts, one held as text and one read
         * from a file.
         */
        private void runFromCode(PrintStream output) throws Exception {
            output.println(RULE);
            output.println("1. Configured with the builder in code");
            output.println();
//! [usage]
            SessionElement session =
                new SessionElementBuilder(loggerFactory).build();
            DerivedPropertyElement derived =
                new DerivedPropertyElementBuilder(loggerFactory)
                    // A script from your own environment. A path may hold a
                    // wildcard, so "scripts/*.yaml" adds a whole folder.
                    .addScriptFile(resource(SCRIPT_FILE).getPath())
                    // A script held as text in your own code. The name
                    // given here must equal the Name the script carries.
                    .addScript(EVERY_TEST, EVERY_TEST_SCRIPT)
                    // addScript(BuiltInScript.HumanConfidence) adds a
                    // script that ships inside the package. Those scripts
                    // come from the derived-properties repository, and
                    // this example uses none of them so that it stands on
                    // its own.
                    .build();

            // The Derived Property Element goes after every element that
            // supplies a source property. The Pipeline closes both elements
            // when it is closed.
            try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(session)
                .addFlowElement(derived)
                .setAutoCloseElements(true)
                .build()) {
                List<String> properties = Arrays.asList(ENGAGEMENT, EVERY_TEST);
                process(output, pipeline,
                    "Request 1, every source property present.",
                    5, 120, true, properties);
                process(output, pipeline,
                    "Request 2, the pointer has not been seen to move.",
                    5, 120, null, properties);
            }
//! [usage]
            output.println("One element runs both scripts, so both " +
                "properties are written under the");
            output.println("key derived, and both say the same thing about " +
                "the second request because");
            output.println("they read the same source properties. The " +
                "second request has no value at");
            output.println("all, because a property the scripts name was " +
                "not there, and the message");
            output.println("says which property was missing and what the " +
                "element that supplies it said.");
            output.println();
        }

        /**
         * The second way to configure the element, being a configuration
         * file. The file names the builders and the script file, so the
         * Pipeline is described entirely outside the code.
         */
        private void runFromConfiguration(PrintStream output)
            throws Exception {
            output.println(RULE);
            output.println("2. The same Pipeline built from " +
                CONFIGURATION_FILE);
            output.println();
            PipelineOptions options = PipelineOptionsFactory
                .getOptionsFromFile(resource(CONFIGURATION_FILE));
            // The Pipeline builder makes each element with the builder the
            // file names. A script text held in code cannot be given in a
            // configuration file, so this Pipeline runs the file's script.
            try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .buildFromConfiguration(options)) {
                List<String> properties = Arrays.asList(ENGAGEMENT);
                process(output, pipeline,
                    "Request 1, every source property present.",
                    5, 120, true, properties);
                process(output, pipeline,
                    "Request 2, the pointer has not been seen to move.",
                    5, 120, null, properties);
                printDefinitions(output,
                    pipeline.getElement(DerivedPropertyElement.class));
            }
        }
    }

    /**
     * A file beside the example, which the build copies onto the class
     * path.
     */
    static File resource(String name) throws URISyntaxException {
        URL url = Main.class.getClassLoader().getResource(name);
        if (url == null) {
            throw new IllegalStateException(
                "The file " + name + " is not on the class path.");
        }
        return new File(url.toURI());
    }

    /**
     * Process one request and print the source properties and the derived
     * properties it produced.
     * @param pointerMoved evidence for session.PointerMoved, or null to
     *                     leave the property with no value on this request
     */
    private static void process(
        PrintStream output,
        Pipeline pipeline,
        String title,
        int pagesViewed,
        int seconds,
        Boolean pointerMoved,
        List<String> derivedProperties) throws Exception {
        output.println(title);
        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence(SessionElement.PAGES_VIEWED_EVIDENCE, pagesViewed);
            data.addEvidence(SessionElement.SECONDS_EVIDENCE, seconds);
            if (pointerMoved != null) {
                data.addEvidence(
                    SessionElement.POINTER_MOVED_EVIDENCE, pointerMoved);
            }
            data.process();

            // The source properties, read the way any element's
            // properties are read.
            ElementData session = data.get(SessionElement.ELEMENT_DATA_KEY);
            for (String name : new String[]{SessionElement.PAGES_VIEWED,
                SessionElement.SECONDS_SINCE_PAGE_LOAD,
                SessionElement.POINTER_MOVED}) {
                print(output, "session." + name,
                    (AspectPropertyValue<?>) session.get(name));
            }

            // The derived properties, read from the Element Data the
            // Derived Property Element writes under the key derived.
            IDerivedPropertyData derived = data.get(IDerivedPropertyData.class);
            for (String name : derivedProperties) {
                print(output, "derived." + name,
                    (AspectPropertyValue<?>) derived.get(name));
            }
        }
        output.println();
    }

    /**
     * Print a value, or, where it has no value, the reason there is none.
     */
    private static void print(
        PrintStream output,
        String name,
        AspectPropertyValue<?> value) {
        output.println(String.format("  %-33s %s", name, value.hasValue() ?
            value.getValue() :
            "no value, because " + value.getNoValueMessage()));
    }

    /**
     * Print the full definition each script carries, read from the element,
     * so that everything a script says about its property is available to
     * a caller.
     */
    private static void printDefinitions(
        PrintStream output,
        DerivedPropertyElement element) {
        for (DerivedPropertyMetaData script : element.getScripts()) {
            DerivedOutputMetaData definition = script.getOutput();
            output.println("The definition the script '" + script.getName() +
                "' version " + script.getVersion() + " carries.");
            output.println(String.format("  %-13s %s", "Name",
                definition.getName()));
            output.println(String.format("  %-13s %s", "Description",
                definition.getDescription()));
            output.println(String.format("  %-13s %s", "ValueType",
                definition.getValueType()));
            output.println(String.format("  %-13s %s", "DefaultValue",
                definition.getDefaultValue()));
            output.println(String.format("  %-13s %s", "Category",
                definition.getCategory()));
            output.println(String.format("  %-13s %s", "Dependencies",
                definition.getDependencies()));
            if (definition.getValues() != null) {
                output.println("  Values");
                for (DerivedValueMetaData value : definition.getValues()) {
                    output.println(String.format("    %-8s %s",
                        value.getName(), value.getDescription()));
                }
            }
            output.println();
        }
    }

    /**
     * End the output with the pages and repositories that say more about
     * what this example shows.
     * @param output where to write
     */
    static void writeFindOutMore(PrintStream output) {
        output.println();
        output.println(RULE);
        output.println("Find out more");
        output.println("\tThe script format, the shared scripts and how to " +
            "test a script: https://github.com/51Degrees/derived-properties");
        output.println("\tA page that runs a script in your browser: " +
            "https://51degrees.github.io/derived-properties/");
        output.println("\tThe Pipeline API: https://51degrees.com/documentation/_pipeline_api__index.html?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=pipeline.developer-examples.derivedproperty-main.java&utm_term=find-out-more");
        output.println("\tCustom flow elements, for work a script cannot do: https://51degrees.com/documentation/_pipeline_api__features__custom_elements.html?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=pipeline.developer-examples.derivedproperty-main.java&utm_term=find-out-more");
        output.println("\thttps://github.com/51Degrees/51degrees-java");
        output.println("\thttps://github.com/51Degrees/specifications");
        output.println(RULE);
        output.flush();
    }

    public static void main(String[] args) throws Exception {
        new Example().run(System.out);
        writeFindOutMore(System.out);
    }
}
