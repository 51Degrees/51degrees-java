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

/*!
 * @example console/OfflineProcessing.java
 *
 * Provides an example of processing a YAML file containing evidence for IP Intelligence.
 * There are 20,000 examples in the supplied file of evidence representing IP addresses.
 *
 * We create an IP Intelligence pipeline to read the data and find out about the associated network information,
 * we write this data to a YAML formatted output stream.
 *
 * This example demonstrates the integration in a simplified form. It intentionally outputs only a
 * small subset of the available properties (the registered network details) to keep the illustration
 * clear. Many more IP Intelligence properties can be enabled - refer to the
 * [Property Dictionary](https://51degrees.com/developers/property-dictionary?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-offlineprocessing.java&utm_term=header)
 * for the complete list. The commented-out `getPopulatedProperties` call further down shows how to
 * output every populated property instead of the fixed subset.
 *
 * As well as explaining the basic operation of offline processing using the defaults, for
 * advanced operation this example can be used to experiment with tuning IP Intelligence for
 * performance using Performance Profile settings.
 *
 * Evidence files can be obtained from the [ip-intelligence-data repository](https://github.com/51Degrees/ip-intelligence-data).
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/ip-intelligence-java-examples/blob/main/console/src/main/java/fiftyone/ipintelligence/examples/console/OfflineProcessing.java).
 *
 * This example requires an enterprise IP Intelligence data file (.ipi).
 * To obtain an enterprise data file for testing, please [contact us](https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-offlineprocessing.java&utm_term=header).
 *
 * Required Maven Dependencies:
 * - [com.51degrees:ip-intelligence](https://central.sonatype.com/artifact/com.51degrees/ip-intelligence)
 */

package fiftyone.ipintelligence.examples.console;

import fiftyone.ipintelligence.IPIntelligencePipelineBuilder;
import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.asStringProperty;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.ENTERPRISE_DATA_FILE_REL_PATH;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * Provides an example of processing a YAML file containing evidence for IP Intelligence. There are
 * 20,000 examples in the supplied file of evidence representing IP addresses. For example:
 *
 * <code><pre>
 *   server.client-ip: '96.235.219.216'
 * </pre></code>
 * <p>
 * We create a IP Intelligence pipeline to read the data and find out about the associated IP addresses,
 * we write this data to a YAML formatted output stream.
 * <p>
 * As well as explaining the basic operation of offline processing using the defaults, for advanced
 * operation this example can be used to experiment with tuning IP Intelligence for performance
 * using Performance Profile settings.
 */
public class OfflineProcessing {
    static final Logger logger = LoggerFactory.getLogger(OfflineProcessing.class);
    // This 51degrees IP Intelligence data file (distributed with the source) needs to
    // be somewhere in the project space
    //
    // For testing, contact us to obtain an enterprise data file: https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-offlineprocessing.java&utm_term=datadir
    private static final String dataDir = "ip-intelligence-data";
    // This 51degrees file of 20,000 examples (distributed with the source)
    // needs to be somewhere in the project space. Additional evidence files
    // can be obtained from the [ip-intelligence-data repository](https://github.com/51Degrees/ip-intelligence-data)
    public static final String HEADER_EVIDENCE_YML =
            dataDir + "/evidence.yml";

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));

        // Use provided data file argument, or fall back to default data file path
        String dataFile = (args.length > 0) ? args[0] : ENTERPRISE_DATA_FILE_REL_PATH;
        
        File evidenceFile = getFilePath(HEADER_EVIDENCE_YML);
        run(dataFile, Files.newInputStream(evidenceFile.toPath()), System.out);
    }

    /**
     * Process a YAML representation of evidence - and create a YAML output
     * containing the processed evidence
     *
     * @param dataFile the 51Degrees on premise data file containing
     *                 information about IP addresses
     * @param is       an InputStream containing YAML documents - one per IP address
     * @param os       an OutputStream for the processed data
     */
    @SuppressWarnings("unchecked")
    public static void run(String dataFile, InputStream is, OutputStream os) throws Exception {

        String dataFileLocation;
        try {
            dataFileLocation = DataFileHelper.getDataFileLocation(dataFile);
        } catch (Exception e) {
            logger.error("Failed to find IP Intelligence data file at '{}'. " +
                    "Please provide a valid path to an IP Intelligence data file (.ipi). " +
                    "An explicit path can be supplied via the {} environment variable.",
                    dataFile, DataFileHelper.IPI_PATH_ENV_VAR);
            throw e;
        }

        /*
          ---- configure YAML for getting evidence and saving the results ----
         */

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        dumperOptions.setSplitLines(false);

        // get a YAML loader to iterate over the IP evidence
        Yaml yaml = new Yaml(dumperOptions);
        Iterator<Object> evidenceIterator = yaml.loadAll(is).iterator();

        /*
          ---- Build a pipeline ----
         */

        logger.info("Constructing pipeline with on-premise IP Intelligence " +
                "engine from file " + dataFile);
        // Build a new on-premise IP Intelligence engine in a try/resources so
        // that the pipeline is disposed when done
        try (Pipeline pipeline = new IPIntelligencePipelineBuilder()
                .useOnPremise(dataFileLocation, false)
                // inhibit sharing usage for this test, usually
                // this should be set "true"
                .setShareUsage(false)
                // inhibit auto-update of the data file for this test
                .setAutoUpdate(false)
                // -- Setting the Profile
                // For information on profiles see
                // Performance options for IP Intelligence
                //.setPerformanceProfile(Constants.PerformanceProfiles.MaxPerformance)
                //.setPerformanceProfile(Constants.PerformanceProfiles.HighPerformance)
                // Low memory profile has the data streamed from disk on
                // demand and is conservative in its use of memory, but
                // slower because of disk access
                .setPerformanceProfile(Constants.PerformanceProfiles.MaxPerformance)
                //.setPerformanceProfile(Constants.PerformanceProfiles.Balanced)
                .build()) {

            // get the details of the IP Intelligence engine from the pipeline,
            // to find out what data file we are using
            IPIntelligenceOnPremiseEngine engine = pipeline.getElement(IPIntelligenceOnPremiseEngine.class);
            logger.info("IP Intelligence data file was created {}", engine.getDataFilePublishedDate());

            /*
              ---- Iterate over the evidence ----
             */

            // open a writer to collect the results
            try (Writer writer = new OutputStreamWriter(os)) {
                // read a batch of IP evidence from the stream
                int count = 0;
                while (evidenceIterator.hasNext() && count < 20) {
                    // Flow data is the container for inputs and outputs that
                    // flow through the pipeline a flowdata instance is
                    // created by the pipeline factory method it's important
                    // to dispose flowdata - so wrap in a try/resources
                    try (FlowData flowData = pipeline.createFlowData()) {
                        // the evidence values in the test YAML data are read
                        // as a Map<String, String> - add the evidence to the
                        // flowData
                        flowData.addEvidence(
                                filterEvidence((Map<String, String>) evidenceIterator.next(),
                                        "server."));

                        /*
                          ---- Do the processing ----
                         */

                        // carry out ip-intelligence (and other
                        // pipeline actions) on the evidence
                        flowData.process();
                        // extract IP Intelligence data from the flowData
                        IPIntelligenceData ipData = flowData.get(IPIntelligenceData.class);

                        /*
                          ---- use the IP Intelligence data - output to YAML in this case
                         */

                        // This sample is intentionally minimal: for illustration it
                        // outputs just a small subset of the available properties (the
                        // registered network details below). Many more IP Intelligence
                        // properties can be enabled - for the complete list, see the
                        // property dictionary at
                        // https://51degrees.com/developers/property-dictionary?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-offlineprocessing.java&utm_term=run
                        Map<String, ? super Object> resultMap = new HashMap<>();
                        resultMap.put("ip.RegisteredName", asStringProperty(ipData.getRegisteredName()));
                        resultMap.put("ip.RegisteredOwner", asStringProperty(ipData.getRegisteredOwner()));
                        resultMap.put("ip.RegisteredCountry", asStringProperty(ipData.getRegisteredCountry()));

                        // To output all populated IP Intelligence properties instead of
                        // the fixed subset above, replace the three puts with the helper:
                        // resultMap.putAll(getPopulatedProperties(ipData, "ip."));

                        // write document to output stream as a YAML document
                        writer.write("---\n");
                        yaml.dump(flowData.getEvidence().asKeyMap(), writer);
                        yaml.dump(resultMap, writer);
                        writer.flush();
                    }
                    count++;
                }
                // finish the last YAML document
                writer.write("...\n");
                writer.flush();
                logger.info("Finished processing {} records", count);

                if (engine.getDataSourceTier().equals("Lite")) {
                    logger.warn("You have used a Lite data file which has " +
                            "limited properties and is of limited accuracy");
                    logger.info("The example requires an Enterprise data file " +
                            "to work fully. Find out about the Enterprise " +
                            "data file here: https://51degrees.com/pricing?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-offlineprocessing.java&utm_term=enterprise-data-file");
                }
            }
        }
    }
	
    /**
     * Filter entries that are not keyed on the required prefix
     *
     * @param prefix a prefix for the evidence to filter - e.g. "header."
     * @param evidence a Map<String, String> of evidence entries
     * @return a filtered Map
     */
    @SuppressWarnings("SameParameterValue")
    private static Map<String, String> filterEvidence(Map<String, String> evidence, String prefix) {

        return evidence.entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Get a map of IP Intelligence properties that are populated
     * @param data IPIntelligenceData
     * @param prefix the prefix we want for the property name e.g "ip."
     * @return a filtered map
     */
    @SuppressWarnings({"SameParameterValue", "unused"})
    private static Map<String, Object> getPopulatedProperties(IPIntelligenceData data, String prefix) {

        return data.asKeyMap().entrySet()
                .stream()
                .filter(e -> ((AspectPropertyValue<?>) e.getValue()).hasValue())
                .collect(Collectors.toMap(e -> prefix + e.getKey(),
                        e -> ((AspectPropertyValue<?>)e.getValue()).getValue()));
    }
}
