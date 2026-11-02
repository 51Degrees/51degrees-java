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
 * @example console/MetricsOnPrem.java
 *
 * This example takes all public IP addresses and passes them to the IP
 * Intelligence service, recording the average geographic area in square
 * kilometers, the number of polygons that form the area, and the equivalent
 * radius if the area can be represented as a circle.
 *
 * Depending on the available processor cores the example can take a long time
 * to complete.
 *
 * The sample of IP addresses used in the metrics can be adjusted as a
 * parameter.
 *
 * This example is primarily designed for those who are interested in
 * verifying the published metrics associated with 51Degrees'
 * IP intelligence service.
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/ip-intelligence-java-examples/blob/main/console/src/main/java/fiftyone/ipintelligence/examples/console/MetricsOnPrem.java).
 *
 * This example requires an enterprise IP Intelligence data file (.ipi).
 * To obtain an enterprise data file for testing, please [contact us](https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-metricsonprem.java&utm_term=contact-us).
 *
 * Required Maven Dependencies:
 * - [com.51degrees:ip-intelligence](https://central.sonatype.com/artifact/com.51degrees/ip-intelligence)
 * - [org.locationtech.jts:jts-core](https://central.sonatype.com/artifact/org.locationtech.jts/jts-core)
 * - [org.locationtech.proj4j:proj4j](https://central.sonatype.com/artifact/org.locationtech.proj4j/proj4j)
 */

package fiftyone.ipintelligence.examples.console;

import fiftyone.ipintelligence.IPIntelligencePipelineBuilder;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.examples.console.areas.Calculations;
import fiftyone.ipintelligence.examples.console.areas.Result;
import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.fiftyone.data.CloseableIterable;
import fiftyone.pipeline.engines.fiftyone.data.ComponentMetaData;
import fiftyone.pipeline.engines.fiftyone.data.ProfileMetaData;
import fiftyone.pipeline.engines.fiftyone.data.ValueMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.ENTERPRISE_DATA_FILE_REL_PATH;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.firstValue;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.tryGet;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * Iterates over the valid IP ranges contained in the data file, samples IP
 * addresses from each range, and records metrics grouped by continent,
 * country, location confidence, connection type and network flags. For each
 * group the number of IP addresses, the average geographic area in square
 * kilometers, the equivalent circle radius and the average number of polygons
 * are output in CSV format.
 */
public class MetricsOnPrem {
    private static final Logger logger = LoggerFactory.getLogger(MetricsOnPrem.class);

    /**
     * The percentage of registered IP addresses to randomly include in the
     * metrics sample. 1 is 100%. Higher values will result in longer elapsed
     * processing.
     */
    public static final double DEFAULT_SAMPLE_PERCENTAGE = 0.1;

    /**
     * Default name of the output file written to the working directory.
     */
    public static final String DEFAULT_OUTPUT_FILE = "metrics-output.csv";

    /**
     * Properties needed by the metrics. The first eight form the metric key.
     */
    private static final String[] PROPERTIES = {
            "ContinentName",
            "Country",
            "LocationConfidence",
            "ConnectionType",
            "IsVPN",
            "IsProxy",
            "IsTor",
            "IsPublicRouter",
            "Areas"};

    /**
     * Key used for each metric.
     */
    public static class Key implements Comparable<Key> {
        final String continentName;
        final String country;
        final String locationConfidence;
        final String connectionType;
        final String isVPN;
        final String isProxy;
        final String isTor;
        final String isPublicRouter;
        private final int hashCode;

        public Key(String continentName, String country,
                   String locationConfidence, String connectionType,
                   String isVPN, String isProxy, String isTor,
                   String isPublicRouter) {
            this.continentName = continentName;
            this.country = country;
            this.locationConfidence = locationConfidence;
            this.connectionType = connectionType;
            this.isVPN = isVPN;
            this.isProxy = isProxy;
            this.isTor = isTor;
            this.isPublicRouter = isPublicRouter;
            this.hashCode = Objects.hash(continentName, country,
                    locationConfidence, connectionType, isVPN, isProxy,
                    isTor, isPublicRouter);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Key == false) {
                return false;
            }
            Key other = (Key) obj;
            return continentName.equals(other.continentName) &&
                    country.equals(other.country) &&
                    locationConfidence.equals(other.locationConfidence) &&
                    connectionType.equals(other.connectionType) &&
                    isVPN.equals(other.isVPN) &&
                    isProxy.equals(other.isProxy) &&
                    isTor.equals(other.isTor) &&
                    isPublicRouter.equals(other.isPublicRouter);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public int compareTo(Key other) {
            int difference = continentName.compareTo(other.continentName);
            if (difference != 0) return difference;
            difference = country.compareTo(other.country);
            if (difference != 0) return difference;
            difference = locationConfidence.compareTo(other.locationConfidence);
            if (difference != 0) return difference;
            difference = connectionType.compareTo(other.connectionType);
            if (difference != 0) return difference;
            difference = isVPN.compareTo(other.isVPN);
            if (difference != 0) return difference;
            difference = isProxy.compareTo(other.isProxy);
            if (difference != 0) return difference;
            difference = isTor.compareTo(other.isTor);
            if (difference != 0) return difference;
            return isPublicRouter.compareTo(other.isPublicRouter);
        }
    }

    /**
     * Metric values gathered for a {@link Key}.
     */
    public static class Metric {
        final Key key;

        /**
         * Number of IP addresses that relate to this metric.
         */
        int ipCount = 0;

        /**
         * The total area in km squared of all IPs.
         */
        long totalAreaKm = 0;

        /**
         * Number of areas included.
         */
        int areaCount = 0;

        /**
         * Key is the number of polygons, and the value the number of IPs
         * that contain that number of polygons.
         */
        final Map<Integer, Integer> polygons = new HashMap<>();

        public Metric(Key key) {
            this.key = key;
        }

        /**
         * Average area in km squared for the IPs, or 0 if there is no area
         * available.
         */
        long getAverageAreaKm() {
            return areaCount > 0 ? totalAreaKm / areaCount : 0;
        }

        /**
         * The radius that a circle would need so that it covered the same
         * area as the average area.
         */
        int getEquivalentRadiusKm() {
            return (int) Math.sqrt(getAverageAreaKm() / Math.PI);
        }

        /**
         * Average number of polygons for the metric.
         */
        double getAveragePolygons() {
            if (polygons.isEmpty()) {
                return 0;
            }
            long weightedSum = 0;
            long total = 0;
            for (Map.Entry<Integer, Integer> entry : polygons.entrySet()) {
                weightedSum += (long) entry.getKey() * entry.getValue();
                total += entry.getValue();
            }
            return (double) weightedSum / total;
        }

        /**
         * Increase the count of IP addresses with the number of polygons
         * provided.
         */
        void incrementPolygons(int count) {
            polygons.merge(count, 1, Integer::sum);
        }

        /**
         * Merge the other metric instance with this one.
         */
        void merge(Metric other) {
            ipCount += other.ipCount;
            areaCount += other.areaCount;
            totalAreaKm += other.totalAreaKm;
            for (Map.Entry<Integer, Integer> entry : other.polygons.entrySet()) {
                polygons.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));

        // Use the supplied path for the data file
        String dataFile = args.length > 0 ? args[0] : ENTERPRISE_DATA_FILE_REL_PATH;
        // Get the location for the output file
        String outputFile = args.length > 1 ? args[1] : DEFAULT_OUTPUT_FILE;
        // Get the sample percentage or use the default
        double samplePercentage = args.length > 2 ?
                Double.parseDouble(args[2]) : DEFAULT_SAMPLE_PERCENTAGE;

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // Only include IP addresses with periods in them, i.e. IPv4.
            // There are too many IPv6 addresses for the metrics example to
            // complete in a short time frame. Set to null to include all.
            Predicate<String[]> condition = range -> range[0].contains(".");
            run(dataFile, samplePercentage, 0, condition, writer);
        }
        logger.info("Output written to '{}'", outputFile);
    }

    /**
     * Run the example
     * @param dataFile an IP Intelligence data file
     * @param samplePercentage percentage of possible IP addresses to include
     *                         in the metrics where 1 is 100%
     * @param maxRanges maximum number of IP ranges to process, or 0 for all
     * @param condition optional function used to determine if an IP address
     *                  range should be included, or null for all ranges
     * @param output somewhere for the metrics CSV
     */
    public static void run(String dataFile,
                           double samplePercentage,
                           int maxRanges,
                           Predicate<String[]> condition,
                           PrintWriter output) throws Exception {
        logger.info("Running MetricsOnPrem example");

        String dataFileLocation;
        try {
            dataFileLocation = DataFileHelper.getDataFileLocation(dataFile);
        } catch (Exception e) {
            logger.error("Failed to find IP Intelligence data file at '{}'. " +
                    "Please provide a valid path to an IP Intelligence data file (.ipi).", dataFile);
            throw e;
        }

        int processors = Runtime.getRuntime().availableProcessors();

        // Build the pipeline with an on-premise IP Intelligence engine using
        // the max performance profile.
        IPIntelligencePipelineBuilder builder = new IPIntelligencePipelineBuilder();
        fiftyone.ipintelligence.IPIntelligenceOnPremisePipelineBuilder onPremiseBuilder =
                builder.useOnPremise(dataFileLocation, false)
                        // We use the max performance profile for optimal
                        // detection speed in this example. See the
                        // documentation for more detail on this and other
                        // configuration options.
                        // https://51degrees.com/documentation/_features__automatic_datafile_updates.html?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-metricsonprem.java&utm_term=automatic-datafile-updates
                        .setPerformanceProfile(Constants.PerformanceProfiles.MaxPerformance)
                        // inhibit sharing usage for this example
                        .setShareUsage(false)
                        // inhibit auto-update of the data file for this example
                        .setAutoUpdate(false)
                        .setDataUpdateOnStartup(false)
                        .setDataFileSystemWatcher(false)
                        // Optimize for the expected parallel workload.
                        .setConcurrency(processors);
        // Set to only return from processing the properties needed.
        for (String property : PROPERTIES) {
            onPremiseBuilder.setProperty(property);
        }

        try (Pipeline pipeline = onPremiseBuilder.build()) {

            IPIntelligenceOnPremiseEngine engine =
                    pipeline.getElement(IPIntelligenceOnPremiseEngine.class);
            DataFileHelper.logDataFileInfo(engine);

            // Cache that takes a WKT value and returns the geographic area in
            // square kms and the number of polygons that form the area. The
            // cache is populated as new WKT values are seen which avoids the
            // long process of mapping every area in the data file up front.
            Map<String, Result> wktAreas = new ConcurrentHashMap<>();

            // Queue of ranges to ensure that there are always items available
            // for the consumers.
            BlockingQueue<String[]> ranges =
                    new LinkedBlockingQueue<>(processors * 4);

            // Create and start the consumers which will be waiting on the
            // producer to start.
            ExecutorService executor = Executors.newFixedThreadPool(processors);
            List<Future<Map<Key, Metric>>> consumers = new ArrayList<>();
            for (int i = 0; i < processors; i++) {
                consumers.add(executor.submit(() -> processRanges(
                        pipeline, wktAreas, ranges, samplePercentage)));
            }
            logger.info("Created '{}' consumer processors sampling '{}%' of IPs",
                    consumers.size(), samplePercentage * 100);

            // Use the main thread as the producer adding ranges for the
            // consumers to process.
            int added = addRanges(engine, ranges, condition, maxRanges);
            logger.info("Finished adding '{}' ranges", added);

            // Add a poison pill for each consumer to signal completion.
            for (int i = 0; i < processors; i++) {
                ranges.put(POISON);
            }

            // Combine all the consumer groups into the main groups.
            Map<Key, Metric> groups = new TreeMap<>();
            for (Future<Map<Key, Metric>> consumer : consumers) {
                for (Map.Entry<Key, Metric> group : consumer.get().entrySet()) {
                    Metric metric = groups.get(group.getKey());
                    if (metric != null) {
                        metric.merge(group.getValue());
                    } else {
                        groups.put(group.getKey(), group.getValue());
                    }
                }
            }
            executor.shutdown();

            writeCsv(output, groups);
            logger.info("All done");
        }
    }

    /**
     * Marker used to signal to consumers that no more ranges will be added.
     */
    private static final String[] POISON = new String[0];

    /**
     * Adds the valid ranges from the engine's profile metadata to the queue.
     * @return the number of ranges added
     */
    private static int addRanges(IPIntelligenceOnPremiseEngine engine,
                                 BlockingQueue<String[]> ranges,
                                 Predicate<String[]> condition,
                                 int maxRanges) throws Exception {
        int added = 0;
        // Find the Network component which contains the range properties.
        //
        // A try-with-resource block MUST be used for the collections of
        // metadata returned by the engine. Each one holds a reference to the
        // engine's data set until it is closed. If it is left open, closing
        // the pipeline cannot free the data set, and with the max performance
        // profile that is the whole data file held in memory until the
        // garbage collector happens to finalize the collection.
        ComponentMetaData network = null;
        try (CloseableIterable<ComponentMetaData> components =
                     engine.getComponents()) {
            for (ComponentMetaData component : components) {
                if (network == null &&
                        "Network".equalsIgnoreCase(component.getName())) {
                    network = component;
                } else {
                    component.close();
                }
            }
        }
        if (network == null) {
            logger.warn("No 'Network' component found in the data file. " +
                    "An enterprise data file is needed for this example.");
            return 0;
        }
        // The Network component is kept to compare each profile against, so
        // it is closed with the collection of profiles once that is done.
        try (ComponentMetaData networkComponent = network;
             CloseableIterable<ProfileMetaData> profiles =
                     engine.getProfiles()) {
            for (ProfileMetaData profile : profiles) {
                try (ComponentMetaData component = profile.getComponent()) {
                    if (networkComponent.equals(component) &&
                            isRegisteredCountryValid(profile)) {
                        String start = getValue(profile, "IpRangeStart");
                        String end = getValue(profile, "IpRangeEnd");
                        if (start != null && end != null) {
                            String[] range = new String[]{start, end};
                            if (condition == null || condition.test(range)) {
                                ranges.put(range);
                                added++;
                                if (maxRanges > 0 && added >= maxRanges) {
                                    break;
                                }
                            }
                        }
                    }
                } finally {
                    profile.close();
                }
            }
        }
        return added;
    }

    /**
     * Returns true unless the profile has the value 'Unknown' for the
     * RegisteredCountry property.
     */
    private static boolean isRegisteredCountryValid(ProfileMetaData profile)
            throws Exception {
        ValueMetaData value = profile.getValue("RegisteredCountry", "Unknown");
        if (value == null) {
            return true;
        }
        value.close();
        return false;
    }

    /**
     * Returns the name of the first value for the property, or null if there
     * are none.
     */
    private static String getValue(ProfileMetaData profile, String name)
            throws Exception {
        String result = null;
        // Every value is closed, not only the first, as each one wraps a
        // native object of its own.
        for (ValueMetaData value : profile.getValues(name)) {
            if (result == null) {
                result = value.getName();
            }
            value.close();
        }
        return result;
    }

    /**
     * Consumer task that takes ranges from the queue and processes the IP
     * addresses they contain until the poison marker is received.
     */
    private static Map<Key, Metric> processRanges(
            Pipeline pipeline,
            Map<String, Result> wktAreas,
            BlockingQueue<String[]> ranges,
            double samplePercentage) throws Exception {
        Random random = new Random();
        Map<Key, Metric> groups = new HashMap<>();
        while (true) {
            String[] range = ranges.take();
            if (range == POISON) {
                break;
            }
            byte[] current = InetAddress.getByName(range[0]).getAddress();
            byte[] end = InetAddress.getByName(range[1]).getAddress();
            if (current.length != end.length) {
                continue;
            }
            while (ipEquals(current, end) == false) {
                if (random.nextDouble() <= samplePercentage) {
                    // Only allocate the InetAddress object when we actually
                    // need to process it.
                    InetAddress ip = InetAddress.getByAddress(current);
                    processIp(pipeline, wktAreas, groups, ip);
                }
                // Increment to next IP address.
                if (tryGetNextAddress(current) == false) {
                    // Overflow, reached maximum IP.
                    break;
                }
            }
        }
        return groups;
    }

    /**
     * Processes a single IP address adding the result to the groups.
     */
    private static void processIp(Pipeline pipeline,
                                  Map<String, Result> wktAreas,
                                  Map<Key, Metric> groups,
                                  InetAddress ipAddress) throws Exception {
        // Get the data for the IP address.
        try (FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence("query.client-ip", ipAddress.getHostAddress());
            flowData.process();
            IPIntelligenceData data = flowData.get(IPIntelligenceData.class);

            // Get the metric instance for the group key.
            Key key = createKey(data);
            Metric metric = groups.get(key);
            if (metric == null) {
                metric = new Metric(key);
                groups.put(key, metric);
            }

            // Increase the number of IP addresses that relate to this key.
            metric.ipCount++;

            // Increase the total area and number of areas for the metric only
            // where a non zero area is available.
            Object areas = firstValue(tryGet(data::getAreas));
            if (areas != null) {
                Result result = wktAreas.computeIfAbsent(
                        areas.toString(),
                        wkt -> {
                            try {
                                return Calculations.getAreas(wkt, 0, 0);
                            } catch (Exception e) {
                                logger.warn("Failed to calculate area", e);
                                return null;
                            }
                        });
                if (result != null) {
                    metric.totalAreaKm += result.getSquareKms();
                    metric.incrementPolygons(result.getGeometries());
                    metric.areaCount++;
                }
            }
        }
    }

    /**
     * Returns the key for the data instance provided.
     */
    private static Key createKey(IPIntelligenceData data) {
        return new Key(
                stringValue(firstValue(tryGet(data::getContinentName))),
                stringValue(firstValue(tryGet(data::getCountry))),
                stringValue(firstValue(tryGet(data::getLocationConfidence))),
                stringValue(firstValue(tryGet(data::getConnectionType))),
                stringValue(firstValue(tryGet(data::getIsVPN))),
                stringValue(firstValue(tryGet(data::getIsProxy))),
                stringValue(firstValue(tryGet(data::getIsTor))),
                stringValue(firstValue(tryGet(data::getIsPublicRouter))));
    }

    /**
     * Returns the string form of the value or 'Unknown' when there is none.
     */
    private static String stringValue(Object value) {
        return value != null ? value.toString() : "Unknown";
    }

    /**
     * Increments the IP address in the buffer by 1.
     * @param buffer the IP address bytes to increment
     * @return true if successful, false if overflow (address was max value)
     */
    static boolean tryGetNextAddress(byte[] buffer) {
        for (int i = buffer.length - 1; i >= 0; i--) {
            if (buffer[i] != (byte) 0xFF) {
                buffer[i]++;
                return true;
            }
            buffer[i] = 0;
        }
        return false; // Overflow
    }

    /**
     * Compares two IP addresses represented as byte arrays.
     */
    static boolean ipEquals(byte[] a, byte[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Write the metrics to the provided output in CSV format.
     */
    private static void writeCsv(PrintWriter output, Map<Key, Metric> groups) {
        output.println("ContinentName,Country,LocationConfidence," +
                "ConnectionType,IsVPN,IsProxy,IsTor,IsPublicRouter,IpCount," +
                "AreaCount,AverageAreaKm,EquivalentRadiusKm,AveragePolygons");
        for (Metric metric : groups.values()) {
            output.println(
                    csvEscape(metric.key.continentName) + "," +
                    csvEscape(metric.key.country) + "," +
                    csvEscape(metric.key.locationConfidence) + "," +
                    csvEscape(metric.key.connectionType) + "," +
                    metric.key.isVPN + "," +
                    metric.key.isProxy + "," +
                    metric.key.isTor + "," +
                    metric.key.isPublicRouter + "," +
                    metric.ipCount + "," +
                    metric.areaCount + "," +
                    metric.getAverageAreaKm() + "," +
                    metric.getEquivalentRadiusKm() + "," +
                    String.format("%.2f", metric.getAveragePolygons()));
        }
        output.flush();
    }

    /**
     * Quote a CSV field when it contains a comma or quote.
     */
    private static String csvEscape(String field) {
        if (field.contains(",") || field.contains("\"")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /**
     * Used by the example test to run against a stream rather than a file.
     */
    public static void run(String dataFile,
                           double samplePercentage,
                           int maxRanges,
                           Predicate<String[]> condition,
                           OutputStream outputStream) throws Exception {
        PrintWriter writer = new PrintWriter(outputStream);
        run(dataFile, samplePercentage, maxRanges, condition, writer);
        writer.flush();
    }
}
