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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.derived.data.IDerivedPropertyData;
import fiftyone.pipeline.derived.flowelements.DerivedPropertyElement;
import fiftyone.pipeline.derived.flowelements.DerivedPropertyElementBuilder;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import pipeline.developerexamples.derivedproperty.flowelements.SensorElementBuilder;

import java.io.PrintStream;

/**
 * Shows the Derived Property Element, which computes a property from
 * properties other elements in the pipeline have already produced,
 * following rules written in a script. The element needs no resource key,
 * no data file and no network connection, so this example runs anywhere.
 * <p>
 * The properties the scripts read come from {@code SensorElement}, which
 * stands in for an engine and supplies a temperature and a humidity under
 * the element data key {@code sensor}. The derived values appear under the
 * element data key {@code derived}.
 */
public class Main {
    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    /**
     * A script held in the example and passed to the builder as text. It
     * produces Comfort from the Temperature the sensor element supplies.
     * The rules are read in order and the first that matches gives the
     * value. The last rule is always an Else.
     */
    static final String COMFORT_SCRIPT =
        "Format: 1\n" +
        "Name: Comfort\n" +
        "Version: \"1.0.0\"\n" +
        "Output:\n" +
        "  Name: Comfort\n" +
        "  Description: How the temperature feels.\n" +
        "  ValueType: string\n" +
        "  IsList: false\n" +
        "  Values:\n" +
        "    - Name: Cold\n" +
        "    - Name: Comfortable\n" +
        "    - Name: Warm\n" +
        "Rules:\n" +
        "  - When: { Property: sensor.Temperature, Lt: 18 }\n" +
        "    Then: Cold\n" +
        "  - When: { Property: sensor.Temperature, Le: 24 }\n" +
        "    Then: Comfortable\n" +
        "  - Else: Warm\n";

    public static class Example {
        public void run(PrintStream output) throws Exception {
            output.println("1. A script in the code, passed to the " +
                "builder as text");
            DerivedPropertyElement fromCode =
                new DerivedPropertyElementBuilder(loggerFactory)
                    .addScript("Comfort", COMFORT_SCRIPT)
                    .build();
            // The pipeline closes the elements when it is closed. The
            // element that supplies the source properties comes first.
            try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(new SensorElementBuilder(loggerFactory).build())
                .addFlowElement(fromCode)
                .setAutoCloseElements(true)
                .build()) {
                show(pipeline, output, "21", "40", "Comfort");
            }
        }
    }

    /**
     * Processes a reading and writes the derived properties named, each as
     * its value or, where it has no value, the message saying why.
     */
    static void show(Pipeline pipeline, PrintStream output,
                     String temperature, String humidity,
                     String... properties) throws Exception {
        try (FlowData flowData = pipeline.createFlowData()) {
            if (temperature != null) {
                flowData.addEvidence("query.temperature", temperature);
            }
            if (humidity != null) {
                flowData.addEvidence("query.humidity", humidity);
            }
            flowData.process();
            output.println("\tTemperature " +
                (temperature == null ? "not given" : temperature) +
                ", humidity " + (humidity == null ? "not given" : humidity));
            IDerivedPropertyData derived =
                flowData.get(IDerivedPropertyData.class);
            for (String property : properties) {
                AspectPropertyValue<Object> value =
                    derived.getValue(property, Object.class);
                output.println("\tderived." + property + " = " +
                    (value.hasValue() ? value.getValue() :
                        "no value. " + value.getNoValueMessage()));
            }
        }
    }

    /**
     * The rule above and below the "Find out more" section.
     */
    private static final String RULE =
        "----------------------------------------------------------------";

    /**
     * Ends the output with the pages and repositories that say more about
     * what this example shows.
     * @param output where to write
     */
    static void writeFindOutMore(PrintStream output) {
        output.println();
        output.println(RULE);
        output.println("Find out more");
        output.println("\tCustom flow elements: https://51degrees.com/documentation/_pipeline_api__features__custom_elements.html?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=pipeline.developer-examples.derived-property-main.java&utm_term=find-out-more");
        output.println("\thttps://github.com/51Degrees/derived-properties");
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
