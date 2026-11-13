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
package fiftyone.devicedetection.examples.console;

import fiftyone.pipeline.engines.fiftyone.data.FiftyOneAspectPropertyMetaData;
import fiftyone.devicedetection.examples.shared.DataFileHelper;
import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngineBuilder;
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


public class MetadataOnPrem {
    private static final Logger logger = LoggerFactory.getLogger(GettingStartedOnPrem.class);

    /* In this example, by default, the 51degrees "Lite" file is used from the data repository in
    cxx/data, and is downloaded if it is missing, or you may specify another file as a command line
    parameter.

    Note that the Lite data file is only used for illustration, and has a small number of
    Properties. Contact us for a copy of the Enterprise data file: https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-devicedetection-examples-console-metadataonprem.java&utm_term=lite_v_4_1_hash */
    public static String LITE_V_4_1_HASH = "device-detection/51Degrees-LiteV4.1.hash";

    /**
     * Where to ask for a copy of the Enterprise data file, tagged with this
     * example.
     */
    private static final String CONTACT_US = "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-devicedetection-examples-console-metadataonprem.java&utm_term=enterprise-data-file";

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));
        String dataFile = args.length > 0 ? args[0] : LITE_V_4_1_HASH;
        run(dataFile, System.out);
    }


    public static void run(String dataFile, OutputStream output) throws Exception {
        logger.info("Running MetadataOnPrem example");
        String dataFileLocation;
        try {
            dataFileLocation =
                    DataFileHelper.findDataFile(dataFile).getAbsolutePath();
        } catch (Exception e) {
            DataFileHelper.cantFindDataFile(dataFile);
            throw e;
        }
        // Build a new on-premise Hash engine with the low memory performance profile.
        // Note that there is no need to construct a complete pipeline in order to access
        // the meta-data.
        // If you already have a pipeline and just want to get a reference to the engine 
        // then you can use `var engine = pipeline.GetElement<DeviceDetectionHashEngine>();`
        try (DeviceDetectionHashEngine ddEngine =
                     new DeviceDetectionHashEngineBuilder(LoggerFactory.getILoggerFactory())
                // We use the low memory profile as its performance is sufficient for this
                // example. See the documentation for more detail on this and other
                // configuration options:
                // https://51degrees.com/documentation/_device_detection__features__performance_options.html?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-devicedetection-examples-console-metadataonprem.java&utm_term=run
                // https://51degrees.com/documentation/_pipeline_api__features__automatic_datafile_updates.html?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-devicedetection-examples-console-metadataonprem.java&utm_term=run
                .setPerformanceProfile(Constants.PerformanceProfiles.LowMemory)
                // inhibit auto-update of the data file for this test
                .setAutoUpdate(false)
                .setDataFileSystemWatcher(false)
                .setDataUpdateOnStartup(false)
                .build(dataFileLocation, false)){

            PrintWriter writer = new PrintWriter(output);
            logger.info("Listing Components");
            outputComponents(ddEngine, writer);
            writer.println();
            writer.flush();

            logger.info("Listing Profile Details");
            outputProfileDetails(ddEngine, writer);
            writer.println();
            writer.flush();

            logger.info("Listing Evidence Key Details");
            outputEvidenceKeyDetails(ddEngine, writer);
            writer.println();
            writer.flush();

            DataFileHelper.logDataFileInfo(ddEngine);
            if (DataFileHelper.isLiteDataFile(ddEngine)) {
                DataFileHelper.writeEnterpriseOffer(writer, CONTACT_US);
            }
        }
    }

    private static void outputEvidenceKeyDetails(DeviceDetectionHashEngine ddEngine,
                                              PrintWriter output){
        output.println();
        if (ddEngine.getEvidenceKeyFilter() instanceof EvidenceKeyFilterWhitelist) {
            // If the evidence key filter extends EvidenceKeyFilterWhitelist then we can
            // display a list of accepted keys.
            EvidenceKeyFilterWhitelist filter = (EvidenceKeyFilterWhitelist) ddEngine.getEvidenceKeyFilter();
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
                    "or not.\n", ddEngine.getEvidenceKeyFilter().getClass().getName());
            output.println("For example, header.user-agent " +
                    (ddEngine.getEvidenceKeyFilter().include("header.user-agent") ?
                            "is " : "is not ") + "accepted.");
        }
    }

    private static void outputProfileDetails(DeviceDetectionHashEngine ddEngine,
                                            PrintWriter output) throws Exception {
        // Group the profiles by component and then output the number of profiles
        // for each component.
        //
        // A try-with-resource block MUST be used for the collections of
        // metadata returned by the engine. Each one holds a reference to the
        // engine's data set until it is closed, so if it is left open the
        // data set cannot be freed when the engine is closed.
        Map<String, Long> groups;
        try (CloseableIterable<ProfileMetaData> profiles = ddEngine.getProfiles()) {
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
    private static void outputComponents(DeviceDetectionHashEngine ddEngine, PrintWriter output)
            throws Exception {
        // See outputProfileDetails for why this is a try-with-resource block.
        try (CloseableIterable<ComponentMetaData> components = ddEngine.getComponents()) {
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
                // Most properties in the Device Metrics category do not have defined
                // values so exclude them.
                if (property.getCategory().equals("Device Metrics")==false) {
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


/*!
 * @example MetadataOnPrem.java
 *
 * The device detection data file contains meta data that can provide additional information
 * about the various records in the data model.
 * This example shows how to access this data and display the values available.
 *
 * To help navigate the data, it's useful to have an understanding of the types of records that
 * are present:
 * - Component - A record relating to a major aspect of the entity making a web request. There are currently 4 components: Hardware, Software Platform (OS), Browser and Crawler.
 * - Profile - A record containing the details for a specific instance of a component. An example of a hardware profile would be the profile for the iPhone 13. An example of a platform profile would be Android 12.1.0.
 * - Property - Each property will have a specific value (or values) for each profile. An example of a hardware property is 'IsMobile'. An example of a browser property is 'BrowserName'.
 *
 * The example will output each component in turn, with a list of the properties associated with
 * each component. Some of the possible values for each property are also displayed.
 * There are too many profiles to display, so we just list the number of profiles for each
 * component.
 *
 * Finally, the evidence keys that are accepted by device detection are listed. These are the
 * keys that, when added to the evidence collection in flow data, could have some impact on the
 * result returned by device detection.
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/51degrees-java/blob/main/examples/device-detection/console/src/main/java/fiftyone/devicedetection/examples/console/MetadataOnPrem.java).
 *
 * @include{doc} example-require-datafile.txt
 */
