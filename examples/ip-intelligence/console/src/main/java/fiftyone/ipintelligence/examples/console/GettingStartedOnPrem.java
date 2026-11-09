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
 * @example console/GettingStartedOnPrem.java
 * 
 * This example shows how to use 51Degrees On-premise IP Intelligence to determine location and network details from IP addresses.
 * 
 * You will learn:
 * 
 * 1. How to create a Pipeline that uses 51Degrees On-premise IP Intelligence
 * 2. How to pass input data (evidence) to the Pipeline
 * 3. How to retrieve the results
 * 
 * This example is available in full on [GitHub](https://github.com/51Degrees/51degrees-java/blob/main/examples/ip-intelligence/console/src/main/java/fiftyone/ipintelligence/examples/console/GettingStartedOnPrem.java).
 * 
 * By default the example uses the free Lite IP Intelligence data file, which it downloads when
 * it is missing. The Lite data file has the registration properties, and the example shows each
 * property only the Enterprise data file has as needing it. To obtain an Enterprise data file,
 * please [contact us](https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-gettingstartedonprem.java&utm_term=header).
 * 
 * Required Maven Dependencies:
 * - [com.51degrees:ip-intelligence](https://central.sonatype.com/artifact/com.51degrees/ip-intelligence)
 */

package fiftyone.ipintelligence.examples.console;

import fiftyone.ipintelligence.engine.onpremise.interop.swig.WeightedStringListValueSwig;
import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import fiftyone.ipintelligence.examples.shared.EvidenceHelper;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.configuration.PipelineOptionsFactory;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.fiftyone.flowelements.FiftyOnePipelineBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.asString;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.LITE_DATA_FILE_REL_PATH;

/**
 * Provides an illustration of the fundamental elements of carrying out IP Intelligence using
 * an "on premise" engine, meaning the IP Intelligence data is stored on your server
 * and the lookup software executes exclusively on your server.
 * <p>
 * This example shows how to use pipeline configuration file, as opposed to the fluent builder.
 * The configuration file is <code>src/main/resources/gettingStartedOnPrem.xml</code>.
 * <p>
 * The concepts of "pipeline", "flow data", "evidence" and "results" are illustrated.
 */
public class GettingStartedOnPrem {
    private static final Logger logger = LoggerFactory.getLogger(GettingStartedOnPrem.class);

    /* In this example, by default, the free 51degrees Lite IP Intelligence data file is used from
    the data repository in cxx/data, and is downloaded if it is missing, or you may specify another
    file as a command line parameter.

    Contact us for a copy of the Enterprise data file: https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-gettingstartedonprem.java&utm_term=main */

    /**
     * Where to ask for a copy of the Enterprise data file, tagged with this
     * example.
     */
    private static final String CONTACT_US = "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-gettingstartedonprem.java&utm_term=enterprise-data-file";

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));

        // Use the supplied path for the data file
        String dataFile = args.length > 0 ? args[0] : LITE_DATA_FILE_REL_PATH;
        // prepare 'evidence' for use in pipeline (see below)
        List<Map<String, String>> evidence = EvidenceHelper.setUpEvidence();
        run(dataFile, evidence, System.out);
    }

    /**
     * Run the example
     * @param dataFile a IP Intelligence data file
     * @param evidenceList a List&lt;Map&lt;String, String>> representing evidence
     * @param outputStream somewhere for the results
     */
    public static void run(String dataFile,
                           List<Map<String, String>> evidenceList,
                           OutputStream outputStream) throws Exception {
        logger.info("Running GettingStarted example");
        
        String dataFileLocation;
        try {
            dataFileLocation = DataFileHelper.getDataFileLocation(dataFile);
            // the location of the test data file is interpolated in the pipeline
            // configuration file
            System.setProperty("TestDataFile", dataFileLocation);
        } catch (Exception e) {
            logger.error("Failed to find IP Intelligence data file at '{}'. " +
                    "Please provide a valid path to an IP Intelligence data file (.ipi). " +
                    "An explicit path can be supplied via the {} environment variable. " +
                    "For testing, you can obtain an enterprise data file by contacting us at " +
                    "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-gettingstartedonprem.java&utm_term=data-file-not-found",
                    dataFile, DataFileHelper.IPI_PATH_ENV_VAR);
            throw e;
        }

        /* In this example, we use the IPIntelligencePipelineBuilder and configure it from
        options contained in the file "gettingStartedOnPrem.xml".

        For more information about pipelines in general see the documentation at
        https://51degrees.com/documentation/_pipeline_api__concepts__configuration__builders__index.html?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-gettingstartedonprem.java&utm_term=run

        Note that we wrap the creation of a pipeline in a try/resources to control its lifecycle */
        // the configuration file is in the resources directory
        File optionsFile = getFilePath("examples/ip-intelligence/console/src/main/resources/gettingStartedOnPrem.xml");
        // load the options and if no resource key has been set in the file
        // use the one supplied to this method
        PipelineOptions pipelineOptions = PipelineOptionsFactory.getOptionsFromFile(optionsFile);
        // Build a new Pipeline from the configuration.
        try (Pipeline pipeline = new FiftyOnePipelineBuilder()
                .buildFromConfiguration(pipelineOptions)) {


            /* Get the 'engine' element within the pipeline that performs IP Intelligence. We
            can use this to get details about the data file as well as meta-data describing
            things such as the available properties. */
            IPIntelligenceOnPremiseEngine engine = pipeline.getElement(IPIntelligenceOnPremiseEngine.class);

            // carry out some sample detections
            boolean needsEnterprise = false;
            for (Map<String, String> evidence : evidenceList) {
                needsEnterprise |= analyzeEvidence(evidence, pipeline, engine, outputStream);
            }
            if (needsEnterprise) {
                DataFileHelper.writeEnterpriseNote(new PrintWriter(outputStream), CONTACT_US);
            }

            DataFileHelper.logDataFileInfo(engine);

            logger.info("All done");
        }
    }

    /**
     * Taking a map of evidence as a parameter, process it in the pipeline
     * supplied and output the IP Intelligence results to the output stream.
     * @param evidence a map representing HTTP headers
     * @param pipeline a pipeline set up to process the evidence
     * @param engine the engine, which says which properties its data file has
     * @param out somewhere to send the detection results
     * @return true if a property needed the Enterprise data file
     */
    private static boolean analyzeEvidence(Map<String, String> evidence,
                                           Pipeline pipeline,
                                           IPIntelligenceOnPremiseEngine engine,
                                           OutputStream out) throws Exception {
        boolean needsEnterprise = false;
        PrintWriter writer = new PrintWriter(out);
        /* FlowData is a data structure that is used to convey information required for a lookup
        and the results of the lookup through the pipeline. Information required for
        a lookup is called "evidence", for IP Intelligence this is an IP address, in this
        case represented by a Map<String, String> of evidence key/value entries.

        FlowData is wrapped in a try/resources block in order to ensure that the unmanaged
        resources allocated by the native IP Intelligence library are freed */
        try (FlowData data = pipeline.createFlowData()) {

            // list the evidence
            writer.println("Input values:");
            for (Map.Entry<String, String> entry : evidence.entrySet()) {
                writer.format("\t%s: %s\n", entry.getKey(), entry.getValue());
            }

            // Add the evidence values to the flow data
            data.addEvidence(evidence);

            // Process the flow data.
            data.process();

            writer.println("Results:");
            /* Now that it has been processed, the flow data will have been populated with the result.

            In this case, we want information about the IP address, which we can get by asking for a
            result matching the "IPIntelligenceData" interface. */
            IPIntelligenceData ipData = data.get(IPIntelligenceData.class);

            /* Display the results of the lookup, which are called IP Intelligence properties. See the
            property dictionary at https://51degrees.com/developers/property-dictionary?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-gettingstartedonprem.java&utm_term=analyzeevidence for
            details of all available properties. */
            
            // The free Lite data file has the registration properties, and
            // only the Enterprise data file has the others.
            needsEnterprise |= writeValue(writer, engine, "RegisteredName", ipData::getRegisteredName);
            needsEnterprise |= writeValue(writer, engine, "RegisteredOwner", ipData::getRegisteredOwner);
            needsEnterprise |= writeValue(writer, engine, "RegisteredCountry", ipData::getRegisteredCountry);
            needsEnterprise |= writeValue(writer, engine, "IpRangeStart", ipData::getIpRangeStart);
            needsEnterprise |= writeValue(writer, engine, "IpRangeEnd", ipData::getIpRangeEnd);
            needsEnterprise |= writeValue(writer, engine, "Country", ipData::getCountry);
            needsEnterprise |= writeValue(writer, engine, "CountryCode", ipData::getCountryCode);
            needsEnterprise |= writeValue(writer, engine, "CountryCode3", ipData::getCountryCode3);
            needsEnterprise |= writeValue(writer, engine, "Region", ipData::getRegion);
            needsEnterprise |= writeValue(writer, engine, "State", ipData::getState);
            needsEnterprise |= writeValue(writer, engine, "Town", ipData::getTown);
            needsEnterprise |= writeValue(writer, engine, "Latitude", ipData::getLatitude);
            needsEnterprise |= writeValue(writer, engine, "Longitude", ipData::getLongitude);
            needsEnterprise |= writeValue(writer, engine, "Areas", ipData::getAreas);
            needsEnterprise |= writeValue(writer, engine, "AccuracyRadiusMin", ipData::getAccuracyRadiusMin);
            needsEnterprise |= writeValue(writer, engine, "TimeZoneOffset", ipData::getTimeZoneOffset);
        }
        writer.println();
        writer.flush();
        return needsEnterprise;
    }

    /**
     * Writes a property, or that it needs the Enterprise data file when the
     * data file in use does not have it. The value is only read when the
     * data file has the property.
     * @return true if the property needs the Enterprise data file
     */
    private static boolean writeValue(PrintWriter writer,
                                      IPIntelligenceOnPremiseEngine engine,
                                      String property,
                                      Supplier<AspectPropertyValue<?>> value) {
        if (DataFileHelper.hasProperty(engine, property)) {
            writer.println("\t" + property + ": " + asString(value.get()));
            return false;
        }
        writer.println("\t" + property + ": " + DataFileHelper.NEEDS_ENTERPRISE);
        return true;
    }
}
