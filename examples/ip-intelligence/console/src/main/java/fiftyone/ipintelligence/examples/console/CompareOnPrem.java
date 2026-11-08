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
 * @example console/CompareOnPrem.java
 *
 * This example takes in a CSV file containing 'known and true' IP-Location
 * records (IP addresses with associated latitude and longitudes).
 * In this context, 'known and true' refers to real-world data the user
 * has collected that is considered accurate and trustworthy.
 *
 * These records are then used for comparison against an IP Intelligence
 * service that can return latitude, longitude and area information
 * for a given IP address.
 * This can be useful for understanding how the results of an IP to
 * location service compare to real-world information, especially when
 * evaluating different solutions.
 *
 * The example will ingest the following fields from a CSV file.
 *
 * Date Time
 * IP address
 * Address Family (optional)
 * Latitude
 * Longitude
 * Continent (optional)
 * Country (optional)
 *
 * The IP Intelligence service is used to obtain the latitude and longitude
 * from the IP address. The distance in kilometers is then calculated along
 * with the confidence if available, the total geographic area covered by
 * the area returned, and an indicator as to if the provided latitude and
 * longitude is within the area returned.
 *
 * The output CSV file contains the input truth and the result fields for easy
 * evaluation.
 *
 * The performance profile can be given as the fourth argument, for example
 * MaxPerformance, which makes each lookup faster where there is memory to
 * hold the whole data file.
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/51degrees-java/blob/main/examples/ip-intelligence/console/src/main/java/fiftyone/ipintelligence/examples/console/CompareOnPrem.java).
 *
 * This example needs the location properties only the Enterprise IP Intelligence data file has.
 * With the free Lite data file, which it uses by default, it says which and stops. To obtain an
 * Enterprise data file, please [contact us](https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-compareonprem.java&utm_term=contact-us).
 *
 * Required Maven Dependencies:
 * - [com.51degrees:ip-intelligence](https://central.sonatype.com/artifact/com.51degrees/ip-intelligence)
 * - [org.locationtech.jts:jts-core](https://central.sonatype.com/artifact/org.locationtech.jts/jts-core)
 * - [org.locationtech.proj4j:proj4j](https://central.sonatype.com/artifact/org.locationtech.proj4j/proj4j)
 */

package fiftyone.ipintelligence.examples.console;

import fiftyone.ipintelligence.IPIntelligenceOnPremisePipelineBuilder;
import fiftyone.ipintelligence.IPIntelligencePipelineBuilder;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.examples.console.areas.Calculations;
import fiftyone.ipintelligence.examples.console.areas.Result;
import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.ipintelligence.examples.shared.DataFileHelper.LITE_DATA_FILE_REL_PATH;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.firstValue;
import static fiftyone.ipintelligence.examples.shared.PropertyHelper.tryGet;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * Compares 'known and true' IP-Location records from a CSV file with the
 * results of the on-premise IP Intelligence engine. For each unique IP
 * address the distance in kilometers between the true location and the
 * location returned, the size of the area returned, and whether the true
 * location is contained in the area returned are written to an output CSV.
 */
public class CompareOnPrem {
    private static final Logger logger = LoggerFactory.getLogger(CompareOnPrem.class);

    /**
     * Relative path to the CSV file of truth records distributed with the
     * data repository, in cxx/data/ip-intelligence.
     */
    public static final String GEOIP_COMPARISON_EVIDENCE_REL_PATH =
            "ip-intelligence/geoip_comparison_evidence.csv";

    /**
     * Default name of the output file written to the working directory.
     */
    public static final String DEFAULT_OUTPUT_FILE = "compare-output.csv";

    /**
     * The performance profile used unless one is given as the fourth
     * argument. This example needs the Enterprise data file, which is over
     * 6 GB, and LowMemory reads it from disk as it is needed, so the example
     * runs without memory to hold the whole file. MaxPerformance loads the
     * whole file into memory, which makes each lookup faster where there is
     * memory to hold it.
     */
    public static final Constants.PerformanceProfiles
            DEFAULT_PERFORMANCE_PROFILE =
            Constants.PerformanceProfiles.LowMemory;

    /**
     * The properties the comparison asks the engine for, which only the
     * Enterprise data file has.
     */
    public static final List<String> LOCATION_PROPERTIES = Arrays.asList(
            "Latitude",
            "Longitude",
            "LocationConfidence",
            "Areas");

    /**
     * Where to ask for a copy of the Enterprise data file, tagged with this
     * example.
     */
    private static final String CONTACT_US = "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-compareonprem.java&utm_term=enterprise-data-file";

    /**
     * Mean radius of the earth in kilometers, used in the haversine distance
     * calculation.
     */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * A record of latitude, longitude, IP address, and a date time that is
     * considered truthful for the purposes of comparing with an IP to
     * location solution result.
     */
    public static class Truth {
        public final String dateTimeUtc;
        public final double latitude;
        public final double longitude;
        public final String ip;
        public final String addressFamily;
        public final String continent;
        public final String country;

        public Truth(String dateTimeUtc, double latitude, double longitude,
                     String ip, String addressFamily, String continent,
                     String country) {
            this.dateTimeUtc = dateTimeUtc;
            this.latitude = latitude;
            this.longitude = longitude;
            this.ip = ip;
            this.addressFamily = addressFamily;
            this.continent = continent;
            this.country = country;
        }
    }

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));

        // Use the supplied path for the data file
        String dataFile = DataFileHelper.getDataFileLocation(
                args.length > 0 ? args[0] : LITE_DATA_FILE_REL_PATH);
        // Use the supplied path for the truth CSV file
        String csvTruthFile = args.length > 1 ? args[1] : GEOIP_COMPARISON_EVIDENCE_REL_PATH;
        // Get the location for the output file
        String outputFile = args.length > 2 ? args[2] : DEFAULT_OUTPUT_FILE;
        // Get the performance profile, such as MaxPerformance, or use the
        // default
        Constants.PerformanceProfiles profile = args.length > 3 ?
                Constants.PerformanceProfiles.valueOf(args[3]) :
                DEFAULT_PERFORMANCE_PROFILE;

        // The free data files have none of the location properties.
        if (DataFileHelper.lacksEnterpriseProperties(dataFile,
                "Comparing known locations with IP Intelligence",
                LOCATION_PROPERTIES,
                new PrintWriter(System.out),
                CONTACT_US)) {
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            run(dataFile, csvTruthFile, profile, writer);
        }
        logger.info("Output written to '{}'", outputFile);
    }

    /**
     * Run the example
     * @param dataFile an IP Intelligence data file
     * @param csvTruthFile a CSV file of truth records
     * @param output somewhere for the result CSV
     */
    public static void run(String dataFile,
                           String csvTruthFile,
                           PrintWriter output) throws Exception {
        run(dataFile, csvTruthFile, DEFAULT_PERFORMANCE_PROFILE, output);
    }

    /**
     * Run the example
     * @param dataFile an IP Intelligence data file
     * @param csvTruthFile a CSV file of truth records
     * @param profile the performance profile to build the engine with
     * @param output somewhere for the result CSV
     */
    public static void run(String dataFile,
                           String csvTruthFile,
                           Constants.PerformanceProfiles profile,
                           PrintWriter output) throws Exception {
        logger.info("Running CompareOnPrem example");

        String dataFileLocation;
        try {
            dataFileLocation = DataFileHelper.getDataFileLocation(dataFile);
        } catch (Exception e) {
            logger.error("Failed to find IP Intelligence data file at '{}'. " +
                    "Please provide a valid path to an IP Intelligence data file (.ipi).", dataFile);
            throw e;
        }

        File truthFile = DataFileHelper.findDataFile(csvTruthFile);

        int processors = Runtime.getRuntime().availableProcessors();

        // Build a new pipeline with an on-premise IP Intelligence engine using
        // the performance profile given and restricted to the properties
        // needed.
        IPIntelligenceOnPremisePipelineBuilder builder = new IPIntelligencePipelineBuilder()
                .useOnPremise(dataFileLocation, false)
                // The profile trades memory for speed, as
                // DEFAULT_PERFORMANCE_PROFILE explains. See the documentation
                // for more detail on this and other configuration options.
                // https://51degrees.com/documentation/_pipeline_api__features__automatic_datafile_updates.html?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-ipintelligence-examples-console-compareonprem.java&utm_term=automatic-datafile-updates
                .setPerformanceProfile(profile)
                // inhibit sharing usage for this example
                .setShareUsage(false)
                // inhibit auto-update of the data file for this example
                .setAutoUpdate(false)
                .setDataUpdateOnStartup(false)
                .setDataFileSystemWatcher(false)
                // Optimize for the expected parallel workload.
                .setConcurrency(processors);
        // Set to only return from processing the properties needed.
        for (String property : LOCATION_PROPERTIES) {
            builder.setProperty(property);
        }
        try (Pipeline pipeline = builder.build()) {

            // Read the truth records, skipping duplicate IP addresses.
            List<Truth> truths = readTruths(truthFile);
            logger.info("Read '{}' unique truth records from '{}'",
                    truths.size(), truthFile);

            // Write the output header combining the truth and result fields.
            output.println("DateTimeUtc,Latitude,Longitude,Ip,AddressFamily," +
                    "Continent,Country,LatitudeResult,LongitudeResult," +
                    "Confidence,DistanceKms,SquareKms,Geometries,Contains");

            // Process the truth records in parallel. The IP Intelligence
            // engine and the area calculations are thread safe.
            ExecutorService executor = Executors.newFixedThreadPool(processors);
            try {
                List<Future<String>> futures = new ArrayList<>();
                for (Truth truth : truths) {
                    futures.add(executor.submit(() -> processTruth(pipeline, truth)));
                }
                int processed = 0;
                for (Future<String> future : futures) {
                    String line = future.get();
                    if (line != null) {
                        output.println(line);
                    }
                    processed++;
                    if (processed % 1000 == 0) {
                        logger.info("Processed '{}' truth records", processed);
                    }
                }
                logger.info("Finished processing '{}' truth records", processed);
            } finally {
                executor.shutdown();
            }
            output.flush();

            // Finally log the data file used for consistency with the other
            // examples.
            IPIntelligenceOnPremiseEngine engine =
                    pipeline.getElement(IPIntelligenceOnPremiseEngine.class);
            DataFileHelper.logDataFileInfo(engine);
        }
    }

    /**
     * Read the truth records from the CSV file provided, skipping records
     * with IP addresses that have already been seen.
     */
    private static List<Truth> readTruths(File csvFile) throws Exception {
        List<Truth> truths = new ArrayList<>();
        Set<String> ips = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(
                csvFile.toPath(), StandardCharsets.UTF_8)) {
            // skip the header line
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = line.split(",", -1);
                if (fields.length < 4) {
                    continue;
                }
                // Fields are DateTimeUtc, Latitude, Longitude, Ip,
                // AddressFamily, Continent, Country
                String ip = fields[3].trim();
                if (ips.add(ip) == false) {
                    continue;
                }
                truths.add(new Truth(
                        fields[0].trim(),
                        Double.parseDouble(fields[1].trim()),
                        Double.parseDouble(fields[2].trim()),
                        ip,
                        fields.length > 4 ? fields[4].trim() : "",
                        fields.length > 5 ? fields[5].trim() : "",
                        fields.length > 6 ? fields[6].trim() : ""));
            }
        }
        return truths;
    }

    /**
     * Process the specific truth record returning the output CSV line, or
     * null where the IP address was not found in the data file.
     */
    private static String processTruth(Pipeline pipeline, Truth truth)
            throws Exception {
        // Get the data for the IP address.
        try (FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence("query.client-ip", truth.ip);
            flowData.process();
            IPIntelligenceData data = flowData.get(IPIntelligenceData.class);

            // Check if the required properties have values. If not, skip this
            // record as the IP address was not found in the database.
            Float latitude = firstValue(tryGet(data::getLatitude));
            Float longitude = firstValue(tryGet(data::getLongitude));
            Object areas = firstValue(tryGet(data::getAreas));
            if (latitude == null || longitude == null || areas == null) {
                return null;
            }

            // Determine the address family if the source truth does not
            // provide it.
            String addressFamily = truth.addressFamily;
            if (addressFamily == null || addressFamily.isEmpty()) {
                addressFamily = truth.ip.contains(":") ?
                        "InterNetworkV6" : "InterNetwork";
            }

            String confidence = firstValue(tryGet(data::getLocationConfidence));

            // Get the distance in kilometers between the result and the truth.
            double distanceKms = haversineKms(
                    truth.latitude, truth.longitude,
                    latitude, longitude);

            // Get the area result for the returned data and the true latitude
            // and longitude.
            Result area = Calculations.getAreas(
                    areas.toString(),
                    truth.latitude,
                    truth.longitude);

            return truth.dateTimeUtc + "," +
                    truth.latitude + "," +
                    truth.longitude + "," +
                    truth.ip + "," +
                    addressFamily + "," +
                    truth.continent + "," +
                    truth.country + "," +
                    latitude + "," +
                    longitude + "," +
                    (confidence != null ? confidence : "") + "," +
                    String.format("%.3f", distanceKms) + "," +
                    area.getSquareKms() + "," +
                    area.getGeometries() + "," +
                    area.getContains();
        }
    }

    /**
     * Returns the great circle distance in kilometers between two points
     * using the haversine formula.
     */
    private static double haversineKms(double lat1, double lon1,
                                       double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Used by the example test to run against a stream rather than a file.
     * @param dataFile an IP Intelligence data file
     * @param csvTruthFile a CSV file of truth records
     * @param outputStream somewhere for the result CSV
     */
    public static void run(String dataFile,
                           String csvTruthFile,
                           OutputStream outputStream) throws Exception {
        PrintWriter writer = new PrintWriter(outputStream);
        run(dataFile, csvTruthFile, writer);
        writer.flush();
    }
}
