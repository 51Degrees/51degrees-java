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
package fiftyone.ipintelligence.examples.console;

import fiftyone.pipeline.engines.fiftyone.data.FiftyOneAspectPropertyMetaData;
import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngineBuilder;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.fiftyone.data.CloseableIterable;
import fiftyone.pipeline.engines.fiftyone.data.ComponentMetaData;
import fiftyone.pipeline.engines.fiftyone.data.ProfileMetaData;
import fiftyone.pipeline.engines.fiftyone.data.ValueMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.LITE_DATA_FILE_REL_PATH;

/**
 * This example shows how to access metadata about the IP Intelligence properties that are available 
 * in the data file. This can be useful for understanding what information is available and how to access it.
 *
 * The example will output the available properties along with details about their data types and descriptions.
 * This helps you understand what IP Intelligence data you can access for your use case.
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/51degrees-java/blob/main/examples/ip-intelligence/console/src/main/java/fiftyone/ipintelligence/examples/console/MetadataOnPrem.java).
 *
 * Required Maven Dependencies:
 * - [com.51degrees:ip-intelligence](https://central.sonatype.com/artifact/com.51degrees/ip-intelligence)
 *
 * By default the example uses the free Lite IP Intelligence data file, which it downloads when
 * it is missing. To obtain an Enterprise data file, which has many more properties, please
 * [contact us](https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-metadataonprem.java&utm_term=header).
 */
public class MetadataOnPrem {
    private static final Logger logger = LoggerFactory.getLogger(GettingStartedOnPrem.class);

    /* In this example, by default, the free 51degrees Lite IP Intelligence data file is used from
    the data repository in cxx/data, and is downloaded if it is missing, or you may specify another
    file as a command line parameter.

    Contact us for a copy of the Enterprise data file: https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-metadataonprem.java&utm_term=main */

    /**
     * Where to ask for a copy of the Enterprise data file, tagged with this
     * example.
     */
    private static final String CONTACT_US = "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-metadataonprem.java&utm_term=enterprise-data-file";

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));
        String dataFile = args.length > 0 ? args[0] : LITE_DATA_FILE_REL_PATH;
        run(dataFile, System.out);
    }


    public static void run(String dataFile, OutputStream output) throws Exception {
        logger.info("Running MetadataOnPrem example");
        String dataFileLocation;
        try {
            dataFileLocation = DataFileHelper.getDataFileLocation(dataFile);
        } catch (Exception e) {
            logger.error("Failed to find IP Intelligence data file at '{}'. " +
                    "Please provide a valid path to an IP Intelligence data file (.ipi). " +
                    "An explicit path can be supplied via the {} environment variable.",
                    dataFile, DataFileHelper.IPI_PATH_VARIABLE);
            throw e;
        }
        // Build an on-premise IP Intelligence engine with the low memory
        // profile. There is no need to build a complete pipeline to read the
        // metadata. Code that already has a pipeline can get its engine with
        // pipeline.getElement(IPIntelligenceOnPremiseEngine.class).
        try (IPIntelligenceOnPremiseEngine engine =
                     new IPIntelligenceOnPremiseEngineBuilder(LoggerFactory.getILoggerFactory())
                // We use the low memory profile to minimize memory usage for this
                // metadata example, which is memory-intensive.
                .setPerformanceProfile(Constants.PerformanceProfiles.LowMemory)
                // inhibit auto-update of the data file for this test
                .setAutoUpdate(false)
                .setDataFileSystemWatcher(false)
                .setDataUpdateOnStartup(false)
                .build(dataFileLocation, false)){

            PrintWriter writer = new PrintWriter(output);
            logger.info("Listing Components");
            outputComponents(engine, writer);
            writer.println();
            writer.flush();

            logger.info("Listing Profile Details");
            outputProfileDetails(engine, writer);
            writer.println();
            writer.flush();

            logger.info("Listing Evidence Key Details");
            outputEvidenceKeyDetails(engine, writer);
            writer.println();
            writer.flush();

            DataFileHelper.logDataFileInfo(engine);
            if (DataFileHelper.isFreeDataFile(engine)) {
                DataFileHelper.writeEnterpriseOffer(writer, CONTACT_US);
            }
        }
    }

    private static void outputEvidenceKeyDetails(IPIntelligenceOnPremiseEngine engine,
                                              PrintWriter output){
        output.println();
        if (engine.getEvidenceKeyFilter() instanceof EvidenceKeyFilterWhitelist) {
            // If the evidence key filter extends EvidenceKeyFilterWhitelist then we can
            // display a list of accepted keys.
            EvidenceKeyFilterWhitelist filter = (EvidenceKeyFilterWhitelist) engine.getEvidenceKeyFilter();
            output.println("Accepted evidence keys:");
            for (Map.Entry<String, Integer> entry : filter.getWhitelist().entrySet()){
                output.println("\t" + entry.getKey());
            }
        }  else {
            output.format("The evidence key filter has type " +
                    "%s. As this does not extend " +
                    "EvidenceKeyFilterWhitelist, a list of accepted values cannot be " +
                    "displayed. As an alternative, you can pass evidence keys to " +
                    "filter.include(string) to see if a particular key will be included " +
                    "or not.\n", engine.getEvidenceKeyFilter().getClass().getName());
            output.println("For example, query.client-ip " +
                    (engine.getEvidenceKeyFilter().include("query.client-ip") ?
                            "is " : "is not ") + "accepted.");
        }
    }

    private static void outputProfileDetails(IPIntelligenceOnPremiseEngine engine,
                                            PrintWriter output) throws Exception {
        // Group the profiles by component and then output the number of profiles
        // for each component. Count with a reducing collector rather than
        // collecting every ProfileMetaData into a List: an on-premise IP
        // Intelligence data file has an enormous number of profiles, so
        // materializing them all just to call size() exhausts the heap
        // (OutOfMemoryError). counting() retains only the per-group total.
        //
        // A try-with-resource block MUST be used for the collections of
        // metadata returned by the engine. Each one holds a reference to the
        // engine's data set until it is closed, so if it is left open the
        // data set cannot be freed when the engine is closed.
        Map<String, Long> groups;
        try (CloseableIterable<ProfileMetaData> profiles = engine.getProfiles()) {
            groups = StreamSupport.stream(profiles.spliterator(), false)
                    .collect(Collectors.groupingBy(
                            p -> p.getComponent().getName(),
                            Collectors.counting()));
        }
        groups.forEach((k,v)->output.format("%s Profiles: %d\n", k , v));
    }

    // Output the component name as well as a list of all the associated properties.
    // If we're outputting to console then we also add some formatting to make it
    // more readable.
    private static void outputComponents(IPIntelligenceOnPremiseEngine engine, PrintWriter output)
            throws Exception {
        // See outputProfileDetails for why this is a try-with-resource block.
        try (CloseableIterable<ComponentMetaData> components = engine.getComponents()) {
            for (ComponentMetaData c : components) {
                output.println("Component - "+ c.getName());
                outputProperties(c, output);
            }
        }
    }

    private static void outputProperties(ComponentMetaData component, PrintWriter output)
            throws Exception {
        // The properties of a component, and the values of a property, are
        // collections that are closed in the same way.
        try (CloseableIterable<FiftyOneAspectPropertyMetaData> properties =
                     component.getProperties()) {
            if (properties.iterator().hasNext() == false) {
                output.println("    ... no properties");
                return;
            }
            for (FiftyOneAspectPropertyMetaData property : properties) {
                // Output some details about the property.
                // If we're outputting to console then we also add some formatting to make it
                // more readable.
                output.format("    Property - %s [Category: %s] (%s)\n        " +
                                "Description: %s\n",
                        property.getName(),
                        property.getCategory(),
                        property.getType().getName(),
                        property.getDescription());

                // Next, output a list of the possible values this property can have.
                // Most properties in the Metrics category do not have defined
                // values so exclude them.
                if (property.getCategory().equals("Metrics")==false) {
                    StringBuilder values = new StringBuilder("        Possible " +
                            "values: ");
                    try (CloseableIterable<ValueMetaData> propertyValues =
                                 property.getValues()) {
                        Spliterator<ValueMetaData> spliterator2 =
                                propertyValues.spliterator();
                        StreamSupport.stream(spliterator2, false)
                                .limit(20)
                                .forEach(value -> {
                                    // add value
                                    values.append(truncateToNl(value.getName()));
                                    // add description if exists
                                    String d = value.getDescription();
                                    if (Objects.nonNull(d) && d.isEmpty() == false) {
                                        values.append("(")
                                                .append(d)
                                                .append(")");
                                    }
                                    values.append(",");
                                });

                        if (spliterator2.estimateSize() > 20) {
                            values.append(" +  more ...");
                        }
                    }
                    output.println(values);
                }
            }
        }
    }


    // Truncate value if it contains newline (esp for the JavaScript property)
    private static String truncateToNl(String text) {
        String[] lines = text.split("\n");
        Optional<String> result = Arrays.stream(lines).filter(s -> !s.isEmpty()).findFirst();
        return result.orElse("[empty]") + (lines.length > 1 ? "..." : "");
    }
}
