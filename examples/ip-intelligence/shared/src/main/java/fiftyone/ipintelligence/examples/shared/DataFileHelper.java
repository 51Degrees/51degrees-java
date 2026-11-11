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

package fiftyone.ipintelligence.examples.shared;

import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngineBuilder;
import fiftyone.ipintelligence.shared.testhelpers.FileUtils;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneAspectPropertyMetaData;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneDataFile;
import fiftyone.pipeline.util.FileFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static fiftyone.pipeline.util.FileFinder.getFilePath;

public class DataFileHelper {
    static Logger logger = LoggerFactory.getLogger(DataFileHelper.class);

    /**
     * Sent with each download, so the Distributor can count the downloads
     * made by these examples. It is left out inside GitHub Actions, where
     * the data repository's script names the build instead, so that test
     * runs are not counted as people using the examples.
     */
    private static final String DOWNLOAD_SOURCE = "examples-51degrees-java";

    private static final boolean IN_GITHUB_ACTIONS =
            "true".equals(System.getenv("GITHUB_ACTIONS"));

    private static final String WINDOWS_POWERSHELL = "powershell";

    /**
     * Shown in place of a value for a property that the data file in use
     * does not have. Every property the examples ask for is in the
     * Enterprise data file.
     */
    public static final String NEEDS_ENTERPRISE = "needs the Enterprise data file";

    /**
     * Folder and name of the enterprise IP Intelligence data file, which
     * belongs in cxx/data/ip-intelligence, a folder of the data repository.
     * This path is used by both console and web examples for testing.
     * The data file is linked to this location by CI scripts.
     */
    public static final String ENTERPRISE_DATA_FILE_REL_PATH = "ip-intelligence/51Degrees-EnterpriseIpiV41.ipi";

    /**
     * Folder and name of the free Lite IP Intelligence data file, which the
     * data repository's get-data-files script downloads to that folder.
     * It contains the RegisteredCountry, RegisteredName and RegisteredOwner properties.
     */
    public static final String LITE_DATA_FILE_REL_PATH = "ip-intelligence/51Degrees-LiteV41.ipi";

    /**
     * Folder and name of the free ASN IP Intelligence data file, which the
     * same script downloads. It contains the Asn and AsnName properties.
     */
    public static final String ASN_DATA_FILE_REL_PATH = "ip-intelligence/51Degrees-IPIV4AsnIpiV41.ipi";

    /**
     * Find the best IP Intelligence data file available, preferring the
     * enterprise file, then the free ASN file, then the free Lite file,
     * and downloading a free file that is missing. Suitable for examples
     * that work with whatever properties the data file contains, where any
     * data file allows the example to run.
     * @return a resolved data file path, or null when none is available
     */
    public static String findAvailableDataFile() {
        return findAvailableDataFile(
                ENTERPRISE_DATA_FILE_REL_PATH,
                ASN_DATA_FILE_REL_PATH,
                LITE_DATA_FILE_REL_PATH);
    }

    /**
     * Find the first of the passed candidate data files that can be
     * resolved. Examples that need particular properties can pass only
     * the files containing them, for example the ASN file does not
     * contain the RegisteredName property.
     * @param candidates relative paths to try in order of preference
     * @return a resolved data file path, or null when none is available
     */
    public static String findAvailableDataFile(String... candidates) {
        for (String candidate : candidates) {
            try {
                return getDataFileLocation(candidate);
            } catch (Exception e) {
                logger.debug("Data file '{}' not available", candidate);
            }
        }
        return null;
    }

    /**
     * Aligned name of the environment variable or system property which may
     * hold an explicit path to the IP Intelligence data file. This name is
     * checked first, before any search of the project space.
     */
    public static final String IPI_PATH_ENV_VAR = "_51DEGREES_IPI_PATH";

public static class DatafileInfo {
        FiftyOneDataFile fileInfo;
        String tier;
        boolean free;

        public DatafileInfo(FiftyOneDataFile fileInfo, String dataSourceTier, boolean free) {
            this.fileInfo = fileInfo;
            this.tier = dataSourceTier;
            this.free = free;
        }

        public AspectEngineDataFile getFileInfo(){
            return fileInfo;
        }
        public String getTier() {
            return tier;
        }

        /**
         * @return true for the free Lite and Asn data files
         */
        public boolean isFree() {
            return free;
        }
    }

    /**
     * Reads the details of a data file, such as its tier and publish date.
     * The data file is opened with the low memory profile, which reads
     * little of it.
     * @param dataFileLocation the data file to read
     * @return the data file's details
     * @throws Exception if the data file cannot be opened
     */
    public static DatafileInfo getDatafileMetaData(String dataFileLocation) throws Exception {
        try(IPIntelligenceOnPremiseEngine ddhe = new IPIntelligenceOnPremiseEngineBuilder()
                .setPerformanceProfile(Constants.PerformanceProfiles.LowMemory)
                .setAutoUpdate(false)
                .build(dataFileLocation,false)) {
            return new DatafileInfo((FiftyOneDataFile) ddhe.getDataFileMetaData(),
                    ddhe.getDataSourceTier(), isFreeDataFile(ddhe));
        }
    }

    /**
     * Logs the data file in use, and a warning if it is old. An example run
     * on a free data file says what the Enterprise data file adds in its
     * own output, with {@link #writeEnterpriseNote},
     * {@link #writeEnterpriseOffer} or {@link #lacksEnterpriseProperties}.
     * @param engine the engine whose data file is logged
     */
    public static void logDataFileInfo(IPIntelligenceOnPremiseEngine engine) {
        // Lite or Enterprise
        String dataTier = engine.getDataSourceTier();
        // date of creation
        Date fileDate = engine.getDataFilePublishedDate();
        String dataFileLocation = engine.getDataFileMetaData().getDataFilePath();

        long daysOld = ChronoUnit.DAYS.between(fileDate.toInstant(), Instant.now());
        String displayDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(fileDate);
        logger.info("Used a '{}' data file, created {}, {} days ago, from location '{}'",
                dataTier, displayDate, daysOld, dataFileLocation);
        if (daysOld > 28) {
            logger.warn("This example is using a data file that is more " +
                    "than {} days old. A more recent data file " +
                    "may be needed for correct results", daysOld);
        }
    }

    /**
     * True when the engine's data file has the property. The free Lite data
     * file has only RegisteredCountry, RegisteredName and RegisteredOwner,
     * and the free Asn data file the Asn properties, such as AsnName.
     * @param engine the engine to check
     * @param name the name of the property
     * @return true if the data file has the property
     */
    public static boolean hasProperty(IPIntelligenceOnPremiseEngine engine, String name) {
        for (FiftyOneAspectPropertyMetaData property : engine.getProperties()) {
            if (property.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the engine uses one of the free data files, Lite or Asn.
     * Only the Enterprise data file has the range properties, so a check
     * for IpRangeStart tells them apart.
     * @param engine the engine to check
     * @return true for a free data file
     */
    public static boolean isFreeDataFile(IPIntelligenceOnPremiseEngine engine) {
        return hasProperty(engine, "IpRangeStart") == false;
    }

    /**
     * For an example that needs properties only the Enterprise data file
     * has, reports when the data file has none of them, and then says so
     * and where to ask for a copy of the Enterprise data file. The data
     * file is opened with the low memory profile, which reads little of it.
     * @param dataFile the data file the example would use
     * @param purpose what the example does, as the start of a sentence, for
     *                example "Comparing known locations with IP Intelligence"
     * @param properties the properties the example needs
     * @param output where to write
     * @param contactUs the link to the contact page, tagged with the
     *                  example's file so an enquiry can be traced to it
     * @return true if the data file has none of the properties
     * @throws Exception if the data file cannot be opened
     */
    public static boolean lacksEnterpriseProperties(String dataFile,
                                                    String purpose,
                                                    List<String> properties,
                                                    PrintWriter output,
                                                    String contactUs) throws Exception {
        try (IPIntelligenceOnPremiseEngine engine = new IPIntelligenceOnPremiseEngineBuilder()
                .setPerformanceProfile(Constants.PerformanceProfiles.LowMemory)
                .setAutoUpdate(false)
                .build(dataFile, false)) {
            return lacksEnterpriseProperties(
                    engine, purpose, properties, output, contactUs);
        }
    }

    /**
     * As {@link #lacksEnterpriseProperties(String, String, List, PrintWriter, String)}
     * for an engine the example has already built.
     * @param engine the engine the example uses
     * @param purpose what the example does, as the start of a sentence
     * @param properties the properties the example needs
     * @param output where to write
     * @param contactUs the link to the contact page, tagged with the example
     * @return true if the data file has none of the properties
     */
    public static boolean lacksEnterpriseProperties(IPIntelligenceOnPremiseEngine engine,
                                                    String purpose,
                                                    List<String> properties,
                                                    PrintWriter output,
                                                    String contactUs) {
        for (String property : properties) {
            if (hasProperty(engine, property)) {
                return false;
            }
        }
        output.println(purpose + " needs the " + joinNames(properties) +
                " properties, which only the Enterprise data file has. The " +
                "data file in use has none of them, so the example stops " +
                "here. Contact us for a copy of the Enterprise data file at " +
                contactUs);
        output.flush();
        return true;
    }

    /**
     * Writes, after an example has shown properties that the data file in
     * use does not have, where to ask for a copy of the Enterprise data
     * file.
     * @param output where to write
     * @param contactUs the link to the contact page, tagged with the
     *                  example's file so an enquiry can be traced to it
     */
    public static void writeEnterpriseNote(PrintWriter output, String contactUs) {
        output.println();
        output.println("Some of these properties need the Enterprise data " +
                "file. Contact us for a copy at " + contactUs);
        output.flush();
    }

    /**
     * Writes, after an example that shows no location properties has run on
     * a free data file, what the Enterprise data file adds and where to ask
     * for a copy.
     * @param output where to write
     * @param contactUs the link to the contact page, tagged with the
     *                  example's file so an enquiry can be traced to it
     */
    public static void writeEnterpriseOffer(PrintWriter output, String contactUs) {
        output.println();
        output.println("This example used a free data file. The Enterprise " +
                "data file adds the location, range and connection " +
                "properties. Contact us for a copy at " + contactUs);
        output.flush();
    }

    /**
     * Finds the performance profile an example is given by name, ignoring
     * case. When no profile has the name, writes the names accepted.
     * @param name the name given, such as MaxPerformance
     * @param output where to write when no profile has the name
     * @return the profile, or null when no profile has the name
     */
    public static Constants.PerformanceProfiles findPerformanceProfile(
            String name,
            PrintWriter output) {
        List<String> names = new ArrayList<>();
        for (Constants.PerformanceProfiles profile :
                Constants.PerformanceProfiles.values()) {
            if (profile.name().equalsIgnoreCase(name)) {
                return profile;
            }
            names.add(profile.name());
        }
        output.println("'" + name + "' is not a performance profile. The " +
                "names accepted, in upper or lower case, are " +
                joinNames(names) + ".");
        output.flush();
        return null;
    }

    /**
     * Joins names as "A, B and C".
     */
    private static String joinNames(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) +
                " and " + names.get(names.size() - 1);
    }

    public static void cantFindDataFile(String dataFile) {
        logger.error("Could not find the data file '{}' which must be " +
                "somewhere in the project space to be found. An explicit " +
                "path to the data file can be supplied via the {} " +
                "environment variable or system property.",
                dataFile, IPI_PATH_ENV_VAR);
    }

    /**
     * Tries to find the passed file, or if null a default file
     * @param evidenceFilename a filename to find
     * @return a File object
     * @throws Exception if the file was not found
     */

    @SuppressWarnings("RedundantThrows")
    public static File getEvidenceFile(String evidenceFilename) throws Exception {
        if (Objects.isNull(evidenceFilename)) {
            evidenceFilename = FileUtils.EVIDENCE_FILE_NAME;
        }

        File evidenceFile;
        try {
            evidenceFile = findDataFile(evidenceFilename);
        } catch (Exception e) {
            logger.error("Could not find evidence file {}", evidenceFilename);
            throw e;
        }
        return evidenceFile;
    }

    /**
     * Tries to find the passed file, or if null a default file. Handles both absolute and relative paths.
     * An absolute path supplied by the caller is used as is. Otherwise an
     * explicit path supplied via the {@link #IPI_PATH_ENV_VAR} environment
     * variable or system property is checked before the folder hierarchy is
     * searched for the passed filename.
     * @param dataFilename a filename to find (can be absolute or relative path)
     * @return a full pathname
     * @throws Exception if the file was not found
     */
    @SuppressWarnings("RedundantThrows")
    public static String getDataFileLocation(String dataFilename) throws Exception {
        // an explicit path supplied via the aligned environment variable or
        // system property is checked before any search for the filename
        if (Objects.isNull(dataFilename) ||
                Paths.get(dataFilename).isAbsolute() == false) {
            String envDataFile = System.getenv(IPI_PATH_ENV_VAR);
            if (Objects.isNull(envDataFile)) {
                envDataFile = System.getProperty(IPI_PATH_ENV_VAR);
            }
            if (Objects.nonNull(envDataFile)) {
                if (Files.exists(Paths.get(envDataFile))) {
                    return envDataFile;
                }
                logger.warn("Ignoring {} value '{}' as no file exists there",
                        IPI_PATH_ENV_VAR, envDataFile);
            }
        }

        if (Objects.isNull(dataFilename)) {
            dataFilename = LITE_DATA_FILE_REL_PATH;
        }

        // Check if it's an absolute path
        Path dataPath = Paths.get(dataFilename);
        if (dataPath.isAbsolute()) {
            // It's an absolute path, check if file exists
            if (Files.exists(dataPath)) {
                return dataPath.toString();
            } else {
                throw new IOException("Data file not found at absolute path: " + dataFilename);
            }
        } else {
            // It's a relative path, try FileFinder first (searches project directories)
            try {
                return getFilePath(dataFilename).getAbsolutePath();
            } catch (Exception e) {
                // If FileFinder fails, try as relative to current working directory
                Path relativePath = Paths.get(System.getProperty("user.dir"), dataFilename);
                if (Files.exists(relativePath)) {
                    return relativePath.toString();
                } else {
                    // Try just the relative path as-is
                    if (Files.exists(dataPath)) {
                        return dataPath.toAbsolutePath().toString();
                    }
                    // Download it, if it is a free data file
                    try {
                        return findDataFile(dataFilename).getAbsolutePath();
                    } catch (IllegalArgumentException notFound) {
                        DataFileHelper.cantFindDataFile(dataFilename);
                        throw new IOException("Data file not found at relative path: " + dataFilename +
                                            ". Searched in project directories and current working directory." +
                                            " An explicit path can be supplied via the " + IPI_PATH_ENV_VAR +
                                            " environment variable.", notFound);
                    }
                }
            }
        }
    }

    /**
     * Finds a file below the project root, as
     * {@link FileFinder#getFilePath(String)} does, and then in the data
     * repository at the root of this repository, which is above the folder
     * of the examples that their READMEs run them from. A free data file
     * that is missing has a {@code <file>.link} in the data repository, so
     * the data repository's script is run to download it, and the file is
     * looked for again.
     * @param file the name of the file, or the end of its path
     * @return the file
     * @throws IllegalArgumentException if the file was not found, naming the
     *         command that downloads it when there is one
     */
    public static File findDataFile(String file) {
        File repository = findDataRepository();
        try {
            return find(file, repository);
        } catch (IllegalArgumentException notFound) {
            File link;
            try {
                link = find(file + ".link", repository);
            } catch (IllegalArgumentException noLink) {
                throw notFound;
            }
            String command = runDownload(link);
            try {
                return find(file, repository);
            } catch (IllegalArgumentException stillNotFound) {
                String message = "Could not download '" + file +
                        "'. Run this command to download it: " + command;
                logger.error(message);
                throw new IllegalArgumentException(message, stillNotFound);
            }
        }
    }

    /**
     * Finds a file below the project root, then below the data repository.
     */
    private static File find(String file, File repository) {
        try {
            return getFilePath(file);
        } catch (IllegalArgumentException notFound) {
            if (repository == null) {
                throw notFound;
            }
            return getFilePath(file, repository.getPath());
        }
    }

    /**
     * The data repository, cxx/data, in the nearest folder at or above the
     * working directory that has one.
     * @return the folder, or null if there is none
     */
    private static File findDataRepository() {
        File folder = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (folder != null) {
            File data = new File(new File(folder, "cxx"), "data");
            if (new File(data, "get-data-files.sh").isFile()) {
                return data;
            }
            folder = folder.getParentFile();
        }
        return null;
    }

    /**
     * Runs the data repository's script for the file that a link is for,
     * and waits for it to finish. The data repository is the folder above
     * the one the link is in.
     * @param link the {@code <file>.link} file
     * @return the command, as it would be typed
     */
    private static String runDownload(File link) {
        String name = link.getName().substring(
                0, link.getName().length() - ".link".length());
        File data = link.getAbsoluteFile().getParentFile().getParentFile();
        List<String> command = new ArrayList<>();
        String powerShell = null;
        if (System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT).startsWith("windows")) {
            powerShell = powerShell();
            command.add(powerShell);
            command.add("-NoProfile");
            command.add("-ExecutionPolicy");
            command.add("Bypass");
            command.add("-File");
            command.add(new File(data, "get-data-files.ps1").getPath());
            command.add(name);
            if (IN_GITHUB_ACTIONS == false) {
                command.add("-Source");
                command.add(DOWNLOAD_SOURCE);
            }
        } else {
            command.add("sh");
            command.add(new File(data, "get-data-files.sh").getPath());
            if (IN_GITHUB_ACTIONS == false) {
                command.add("-s");
                command.add(DOWNLOAD_SOURCE);
            }
            command.add(name);
        }
        List<String> typed = new ArrayList<>();
        for (String argument : command) {
            typed.add(argument.contains(" ") ?
                    "\"" + argument + "\"" : argument);
        }
        String line = String.join(" ", typed);
        logger.info("'{}' is missing, so it is being downloaded by running {}",
                name, line);
        try {
            ProcessBuilder builder = new ProcessBuilder(command).inheritIO();
            if (WINDOWS_POWERSHELL.equals(powerShell)) {
                // Started from PowerShell 7, Windows PowerShell inherits its
                // module path and then cannot load its own commands.
                builder.environment().remove("PSModulePath");
            }
            builder.start().waitFor();
        } catch (IOException e) {
            logger.error("Could not run {}. {}", line, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return line;
    }

    /**
     * PowerShell 7 when it is on the path, otherwise Windows PowerShell.
     */
    private static String powerShell() {
        String path = System.getenv("PATH");
        if (path != null) {
            for (String folder : path.split(File.pathSeparator)) {
                if (new File(folder.replace("\"", ""), "pwsh.exe").isFile()) {
                    return "pwsh";
                }
            }
        }
        return WINDOWS_POWERSHELL;
    }
}
