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

package pipeline.developerexamples.onpremiseengine;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import pipeline.developerexamples.onpremiseengine.flowelements.SimpleOnPremiseEngine;
import pipeline.developerexamples.onpremiseengine.flowelements.SimpleOnPremiseEngineBuilder;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Main {
    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    public static class Example {
        public void run() throws Exception {
//! [usage]
            SimpleOnPremiseEngine starSignEngine =
                new SimpleOnPremiseEngineBuilder(loggerFactory)
                    .setAutoUpdate(false)
                    .build(
                        Main.class.getClassLoader().getResource("starsigns.csv").getPath(),
                        false);

            // The pipeline closes the engine, and with it the data file,
            // when it is closed.
            try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(starSignEngine)
                .setAutoCloseElements(true)
                .build()) {
                Calendar dob = Calendar.getInstance();
                dob.set(1992, Calendar.DECEMBER, 18);

                try (FlowData flowData = pipeline.createFlowData()) {
                    flowData
                        .addEvidence("date-of-birth", dob.getTime())
                        .process();

                    System.out.println("With a date of birth of " +
                        new SimpleDateFormat("yyyy/MM/dd").format(dob.getTime()) +
                        ", your star sign is " +
                        flowData.getFromElement(starSignEngine).getStarSign() +
                        ".");
                }
            }
            //! [usage]
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
        output.println("\tThis example: https://51degrees.com/documentation/_pipeline_api__examples__custom_element__on_premise.html?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=pipeline.developer-examples.onpremise-engine-main.java&utm_term=find-out-more");
        output.println("\tCustom flow elements: https://51degrees.com/documentation/_pipeline_api__features__custom_elements.html?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=pipeline.developer-examples.onpremise-engine-main.java&utm_term=find-out-more");
        output.println("\thttps://github.com/51Degrees/51degrees-java");
        output.println("\thttps://github.com/51Degrees/specifications");
        output.println(RULE);
        output.flush();
    }

    public static void main(String[] args) throws Exception {
        new Example().run();
        writeFindOutMore(System.out);
    }
}
