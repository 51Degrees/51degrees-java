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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * Demonstrates how to use the ShareUsageElement directly to send evidence data
 * to 51Degrees for usage sharing. This example shows how to:
 * <ul>
 *   <li>Build a pipeline with only a ShareUsageElement</li>
 *   <li>Configure batch size (minimum entries per message)</li>
 *   <li>Configure sampling rate (share percentage)</li>
 *   <li>Process evidence from a YAML file</li>
 *   <li>Add custom identifiers to track the data source</li>
 * </ul>
 *
 * This is useful for scenarios where you want to share usage data without
 * performing device detection, or when you want fine-grained control over
 * the share usage settings.
 *
 * <p><b>Note on Client IP Address:</b> The client IP address is included in the shared
 * evidence solely for deduplication purposes. This allows 51Degrees' machine learning
 * algorithms to properly weight evidence coming from different sources versus repeated
 * evidence from the same source. Without this, the training data could be skewed by
 * over-representing certain device configurations.</p>
 *
 * <p><b>Identifying the Data Source (usage-from):</b> To help 51Degrees identify which
 * customer or partner is sending usage data, you can add a custom "usage-from" header
 * to the evidence. This is done by adding evidence with key {@code header.usage-from}
 * and your company/application name as the value. In the XML packet sent to 51Degrees,
 * this appears as: {@code <header Name="usage-from">YourCompanyName</header>}.
 * Replace "YourCompanyName" in the {@link #USAGE_FROM_VALUE} constant with your actual
 * identifier before running this example.</p>
 */
public class ShareUsageExample {
    static final Logger logger = LoggerFactory.getLogger(ShareUsageExample.class);

    // Evidence file with 20,000 examples
    public static final String HEADER_EVIDENCE_YML =
            "device-detection-data/20000 Evidence Records.yml";

    // Configurable settings
    private static final int MINIMUM_ENTRIES_PER_MESSAGE = 10;  // Send after 10 entries (default is 50)
    private static final double SHARE_PERCENTAGE = 1.0;          // Share 100% of data (1.0 = 100%)
    private static final int RECORDS_TO_PROCESS = 100;           // Number of records to process

    // Evidence key for client IP address - used for evidence deduplication (see class note above)
    private static final String EVIDENCE_CLIENTIP_KEY = "server.client-ip";

    // Evidence key for identifying the source of share usage data.
    // This adds a <header Name="usage-from">YourCompanyName</header> element to the XML packet,
    // allowing 51Degrees to identify which customer/partner is sending the data.
    // Replace "YourCompanyName" with your actual company or application identifier.
    private static final String EVIDENCE_USAGE_FROM_KEY = "header.usage-from";
    private static final String USAGE_FROM_VALUE = "YourCompanyName";

    // Random generator for fake IPs in this demo
    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));

        File evidenceFile = getFilePath(HEADER_EVIDENCE_YML);
        run(Files.newInputStream(evidenceFile.toPath()));
    }

    /**
     * Process evidence from a YAML input stream and share it via the ShareUsageElement.
     *
     * @param is an InputStream containing YAML documents with evidence
     */
    @SuppressWarnings("unchecked")
    public static void run(InputStream is) throws Exception {
        logger.info("Starting ShareUsage example");
        logger.info("Settings: minEntriesPerMessage={}, sharePercentage={}%, recordsToProcess={}",
                MINIMUM_ENTRIES_PER_MESSAGE,
                (int)(SHARE_PERCENTAGE * 100),
                RECORDS_TO_PROCESS);

        // Build the ShareUsageElement with custom settings
        ShareUsageElement shareUsageElement = new ShareUsageBuilder(LoggerFactory.getILoggerFactory())
                // Set the minimum number of entries before sending a batch
                .setMinimumEntriesPerMessage(MINIMUM_ENTRIES_PER_MESSAGE)
                // Set the maximum queue size (must be > minEntriesPerMessage)
                .setMaximumQueueSize(MINIMUM_ENTRIES_PER_MESSAGE * 10)
                // Set the percentage of requests to share (1.0 = 100%)
                .setSharePercentage(SHARE_PERCENTAGE)
                // Disable repeat evidence filtering for this demo (share all evidence)
                .setRepeatEvidenceIntervalMinutes(0)
                // Optional: set a custom URL (default is 51Degrees endpoint)
                // .setShareUsageUrl("https://your-custom-endpoint.com/usage")
                // Optional: block specific headers from being shared
                // .setBlockedHttpHeader("Authorization")
                // Optional: include specific query string parameters
                // .setIncludedQueryStringParameter("campaign")
                .build();

        // Build a pipeline with only the ShareUsageElement
        try (Pipeline pipeline = new PipelineBuilder()
                .addFlowElement(shareUsageElement)
                .build()) {

            logger.info("Pipeline built successfully with ShareUsageElement");

            // Load YAML evidence iterator
            Yaml yaml = new Yaml();
            Iterator<Object> evidenceIterator = yaml.loadAll(is).iterator();

            // Process evidence records
            int count = 0;
            while (evidenceIterator.hasNext() && count < RECORDS_TO_PROCESS) {
                try (FlowData flowData = pipeline.createFlowData()) {
                    // Get evidence from YAML and filter to only header.* entries
                    Map<String, String> evidence = filterEvidence(
                            (Map<String, String>) evidenceIterator.next(),
                            "header.");

                    // Add a client IP address to the evidence for deduplication purposes
                    // (see class-level note). In a real web application, this would come
                    // from the request (e.g., request.getRemoteAddr() or X-Forwarded-For header)
                    String clientIp = generateRandomIp();
                    evidence = new HashMap<>(evidence);  // Make mutable copy
                    evidence.put(EVIDENCE_CLIENTIP_KEY, clientIp);

                    // Add the usage-from identifier so 51Degrees knows the source of this data.
                    // This appears as <header Name="usage-from">YourCompanyName</header> in the XML.
                    evidence.put(EVIDENCE_USAGE_FROM_KEY, USAGE_FROM_VALUE);

                    // Add evidence to flow data
                    flowData.addEvidence(evidence);

                    // Process - this will queue the evidence for sharing
                    flowData.process();

                    count++;
                    if (count % 10 == 0) {
                        logger.info("Processed {} records", count);
                    }
                }
            }

            logger.info("Finished processing {} records", count);
            logger.info("Closing pipeline - this will flush any remaining queued data...");
        }

        // Give the background thread time to send any remaining data
        logger.info("Waiting for share usage to complete sending...");
        Thread.sleep(5000);

        logger.info("Done!");
    }

    /**
     * Filter evidence entries to only include those with the specified prefix.
     *
     * @param evidence the original evidence map
     * @param prefix the prefix to filter on (e.g., "header.")
     * @return filtered evidence map
     */
    private static Map<String, String> filterEvidence(Map<String, String> evidence, String prefix) {
        return evidence.entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Generate a random IP address for demonstration purposes.
     * In a real application, this would come from the HTTP request.
     *
     * @return a random IPv4 address string
     */
    private static String generateRandomIp() {
        return String.format("%d.%d.%d.%d",
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256));
    }
}
