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
 * @example console/SuspiciousOnPrem.java
 *
 * This example shows how to combine 51Degrees On-premise IP Intelligence
 * properties to determine whether an IP is likely to be the source of
 * suspicious requests.
 *
 * You will learn:
 *
 * 1. How to get diversity properties from the IP Intelligence engine.
 * 2. How to combine diversity values with other network related values to
 * assess the likelihood of an IP address being something suspicious.
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/ip-intelligence-java-examples/blob/main/console/src/main/java/fiftyone/ipintelligence/examples/console/SuspiciousOnPrem.java).
 *
 * This example requires an enterprise IP Intelligence data file (.ipi).
 * To obtain an enterprise data file for testing, please [contact us](https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-suspiciousonprem.java&utm_term=contact-us).
 *
 * Required Maven Dependencies:
 * - [com.51degrees:ip-intelligence](https://central.sonatype.com/artifact/com.51degrees/ip-intelligence)
 */

package fiftyone.ipintelligence.examples.console;

import fiftyone.ipintelligence.IPIntelligencePipelineBuilder;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import fiftyone.ipintelligence.examples.shared.EvidenceHelper;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.ENTERPRISE_DATA_FILE_REL_PATH;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.firstValue;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.tryGet;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * This example shows how to combine 51Degrees On-premise IP Intelligence
 * properties to determine whether an IP is likely to be the source of
 * suspicious requests.
 * <p>
 * In this example the diversity properties are combined with other network
 * related values to calculate a simple "suspicious" score. Many other
 * properties can be used to draw conclusions about the likelihood of
 * suspicious activity, and this is a basic example that should not be used
 * in production without further testing and tuning.
 */
public class SuspiciousOnPrem {
    private static final Logger logger = LoggerFactory.getLogger(SuspiciousOnPrem.class);

    /* In this example, by default, the 51degrees IP Intelligence data file needs to be somewhere
    in the project space, or you may specify another file as a command line parameter.

    For testing, contact us to obtain an enterprise data file: https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-suspiciousonprem.java&utm_term=contact-us */

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));

        // Use the supplied path for the data file
        String dataFile = args.length > 0 ? args[0] : ENTERPRISE_DATA_FILE_REL_PATH;
        // prepare 'evidence' for use in pipeline (see below)
        List<Map<String, String>> evidence = EvidenceHelper.setUpEvidence();
        run(dataFile, evidence, System.out);
    }

    /**
     * Run the example
     * @param dataFile an IP Intelligence data file
     * @param evidenceList a List&lt;Map&lt;String, String>> representing evidence
     * @param outputStream somewhere for the results
     */
    public static void run(String dataFile,
                           List<Map<String, String>> evidenceList,
                           OutputStream outputStream) throws Exception {
        logger.info("Running SuspiciousOnPrem example");

        String dataFileLocation;
        try {
            dataFileLocation = DataFileHelper.getDataFileLocation(dataFile);
        } catch (Exception e) {
            logger.error("Failed to find IP Intelligence data file at '{}'. " +
                    "Please provide a valid path to an IP Intelligence data file (.ipi). " +
                    "For testing, you can obtain an enterprise data file by contacting us at " +
                    "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-suspiciousonprem.java&utm_term=contact-us", dataFile);
            throw e;
        }

        /* In this example, we use the IPIntelligencePipelineBuilder and configure it in code.

        For more information about builders in general see the documentation at
        https://51degrees.com/documentation/_concepts__configuration__builders__index.html?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-suspiciousonprem.java&utm_term=configuration-builders

        Note that we wrap the creation of a pipeline in a try/resources to control its lifecycle */
        try (Pipeline pipeline = new IPIntelligencePipelineBuilder()
                .useOnPremise(dataFileLocation, false)
                // We use the max performance profile for optimal lookup speed in this
                // example. See the documentation for more detail on this and other
                // configuration options.
                // https://51degrees.com/documentation/_features__automatic_datafile_updates.html?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-suspiciousonprem.java&utm_term=automatic-datafile-updates
                // https://51degrees.com/documentation/_features__usage_sharing.html?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-suspiciousonprem.java&utm_term=usage-sharing
                .setPerformanceProfile(Constants.PerformanceProfiles.MaxPerformance)
                // inhibit sharing usage for this example, usually this should be set to "true"
                .setShareUsage(false)
                // inhibit auto-update of the data file for this example
                .setAutoUpdate(false)
                .setDataUpdateOnStartup(false)
                .setDataFileSystemWatcher(false)
                .build()) {

            // carry out some sample detections
            for (Map<String, String> evidence : evidenceList) {
                analyzeEvidence(evidence, pipeline, outputStream);
            }

            IPIntelligenceOnPremiseEngine engine =
                    pipeline.getElement(IPIntelligenceOnPremiseEngine.class);
            DataFileHelper.logDataFileInfo(engine);

            logger.info("All done");
        }
    }

    /**
     * Taking a map of evidence as a parameter, process it in the pipeline
     * supplied and output an assessment of how suspicious the IP address is.
     * @param evidence a map representing evidence
     * @param pipeline a pipeline set up to process the evidence
     * @param out somewhere to send the results
     */
    private static void analyzeEvidence(Map<String, String> evidence,
                                        Pipeline pipeline,
                                        OutputStream out) throws Exception {
        PrintWriter writer = new PrintWriter(out);
        try (FlowData data = pipeline.createFlowData()) {

            // Add the evidence values to the flow data and process it
            data.addEvidence(evidence);
            data.process();
            IPIntelligenceData ipData = data.get(IPIntelligenceData.class);

            /* In this example we use the following properties to make some basic assumptions
            about the likelihood of an IP being a source of suspicious activity. */
            Boolean isCellularValue = firstValue(tryGet(ipData::getIsCellular));
            boolean isCellular = isCellularValue != null && isCellularValue;
            Integer hardwareDiversityValue = firstValue(tryGet(ipData::getHardwareDiversity));
            int hardwareDiversity = hardwareDiversityValue != null ? hardwareDiversityValue : 0;
            Integer browserDiversityValue = firstValue(tryGet(ipData::getBrowserDiversity));
            int browserDiversity = browserDiversityValue != null ? browserDiversityValue : 0;
            String locationConfidence = firstValue(tryGet(ipData::getLocationConfidence));
            Boolean hostedValue = firstValue(tryGet(ipData::getIsHosted));
            boolean hosted = hostedValue != null && hostedValue;
            String country = firstValue(tryGet(ipData::getCountryCode));
            String registeredCountry = firstValue(tryGet(ipData::getRegisteredCountry));
            Integer humanValue = firstValue(tryGet(ipData::getHumanProbability));
            int human = humanValue != null ? humanValue : 0;

            /* Calculating a simple "suspicious" score based on the properties above.

            Many other properties can be used to draw conclusions about the likelihood of
            suspicious activity, and this is a basic example that should not be used in
            production without further testing and tuning. */
            boolean isSuspicious =
                    // Here we can say that if there is a high diversity of hardware
                    // profiles in the IP, then it could be either VPN, cellular,
                    // proxy, or other hosting.
                    // We can then rule out cellular with the IsCellular property.
                    // A low location confidence is further evidence of VPN or
                    // proxy use, rather than other hosting, but this is not a
                    // strong determiner on its own.
                    (hardwareDiversity >= 7 &&
                    isCellular == false &&
                    "Low".equals(locationConfidence)) ||
                    // Then we can also consider the observed country, and the
                    // country the IP range is registered to. If these are not the
                    // same, then this can be an indication of VPN or proxy use.
                    (hosted == true &&
                    country != null &&
                    "Unknown".equals(country) == false &&
                    country.equals(registeredCountry) == false) ||
                    // If the browser versions are significantly more diverse than
                    // the hardware, this may indicate that some devices are using
                    // multiple browsers, which can be a sign of suspicious
                    // activity.
                    browserDiversity - hardwareDiversity > 2;

            writer.println("Input values:");
            for (Map.Entry<String, String> entry : evidence.entrySet()) {
                writer.format("\t%s: %s\n", entry.getKey(), entry.getValue());
            }

            writer.println("Results:");
            writer.println("\tIsCellular: " + isCellular);
            writer.println("\tHardwareDiversity: " + hardwareDiversity);
            writer.println("\tBrowserDiversity: " + browserDiversity);
            writer.println("\tLocationConfidence: " + locationConfidence);
            writer.println("\tIsHosted: " + hosted);
            writer.println("\tCountry: " + country);
            writer.println("\tRegisteredCountry: " + registeredCountry);
            writer.println("\tHumanProbability: " + human);
            writer.println("\tIsSuspicious: " + isSuspicious);
        }
        writer.println();
        writer.flush();
    }
}
