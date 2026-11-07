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

package fiftyone.devicedetection.examples.shared;

import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngineBuilder;
import fiftyone.devicedetection.shared.testhelpers.FileUtils;
import fiftyone.pipeline.engines.data.AspectEngineDataFile;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneAspectPropertyMetaData;
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneDataFile;
import fiftyone.pipeline.util.FileFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
     * Where to ask about the Enterprise data file when the data file in use
     * is old or cannot be found.
     */
    private static final String CONTACT_US = "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-devicedetection-examples-shared-datafilehelper.java&utm_term=data-file";

    /**
     * Shown in place of a value for a property that the data file in use
     * does not have. Every property the examples ask for is in the
     * Enterprise data file.
     */
    public static final String NEEDS_ENTERPRISE = "needs the Enterprise data file";

public static class DatafileInfo {
        FiftyOneDataFile fileInfo;
        String tier;

        public DatafileInfo(FiftyOneDataFile fileInfo, String dataSourceTier) {
            this.fileInfo = fileInfo;
            this.tier = dataSourceTier;
        }

        public AspectEngineDataFile getFileInfo(){
            return fileInfo;
        }
        public String getTier() {
            return tier;
        }
    }
    public static DatafileInfo getDatafileMetaData(String dataFileLocation) throws Exception {
        try(DeviceDetectionHashEngine ddhe = new DeviceDetectionHashEngineBuilder()
                .setAutoUpdate(false)
                .build(dataFileLocation,false)) {
            return new DatafileInfo((FiftyOneDataFile) ddhe.getDataFileMetaData(), ddhe.getDataSourceTier());
        }
    }

    /**
     * Logs the data file in use, and a warning if it is old. An example run
     * on the free Lite data file says what the Enterprise data file adds in
     * its own output, with {@link #writeEnterpriseNote} or
     * {@link #writeEnterpriseOffer}.
     * @param engine the engine whose data file is logged
     */
    public static void logDataFileInfo(DeviceDetectionHashEngine engine) {
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
                    "may be needed to correctly detect the latest " +
                    "devices, browsers, etc.", daysOld);
            logger.info("Run get-data-files.ps1 -Update or " +
                    "get-data-files.sh -u in cxx/data, which is the data " +
                    "repository https://github.com/51Degrees/data, to get " +
                    "the latest 'Lite' data file. Contact us about the " +
                    "Enterprise data file, which includes automatic daily " +
                    "updates, at " + CONTACT_US);
        }
    }

    public static void cantFindDataFile(String dataFile) {
        logger.error("Could not find the data file '{}' which must be " +
                "somewhere in the project space to be found.", dataFile);
        logger.info("The free 'Lite' data file and the evidence files " +
                "are downloaded by get-data-files.ps1 or get-data-files.sh " +
                "in cxx/data, which is the data repository " +
                "https://github.com/51Degrees/data. Contact us about the " +
                "Enterprise data file, which includes automatic daily " +
                "updates, at " + CONTACT_US);
    }

    /**
     * True when the engine's data file has the property. The Lite data
     * file leaves out the properties only the Enterprise data file has,
     * such as HardwareVendor, HardwareName and DeviceType.
     * @param engine the engine to check
     * @param name the name of the property
     * @return true if the data file has the property
     */
    public static boolean hasProperty(DeviceDetectionHashEngine engine, String name) {
        for (FiftyOneAspectPropertyMetaData property : engine.getProperties()) {
            if (property.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the engine uses the free Lite data file.
     * @param engine the engine to check
     * @return true for the Lite data file
     */
    public static boolean isLiteDataFile(DeviceDetectionHashEngine engine) {
        return "Lite".equals(engine.getDataSourceTier());
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
     * Writes, after an example that shows no properties has run on the
     * free Lite data file, what the Enterprise data file adds and where to
     * ask for a copy.
     * @param output where to write
     * @param contactUs the link to the contact page, tagged with the
     *                  example's file so an enquiry can be traced to it
     */
    public static void writeEnterpriseOffer(PrintWriter output, String contactUs) {
        output.println();
        output.println("This example used the free Lite data file. The " +
                "Enterprise data file adds many more properties, such as " +
                "HardwareVendor, HardwareName and DeviceType. Contact us " +
                "for a copy at " + contactUs);
        output.flush();
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
            evidenceFilename = FileUtils.EVIDENCE_FILE_PATH;
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
     * Tries to find the passed file, or if null the first data file found,
     * or the free Lite file when there is none
     * @param dataFilename a filename to find
     * @return a full pathname
     * @throws Exception if the file was not found
     */
    @SuppressWarnings("RedundantThrows")
    public static String getDataFileLocation(String dataFilename) throws Exception {
        if (Objects.isNull(dataFilename)) {
            dataFilename = FileUtils.getHashFileName();
        }
        if (Objects.isNull(dataFilename)) {
            dataFilename = FileUtils.LITE_HASH_DATA_FILE_PATH;
        }
        String dataFileLocation;
        try {
            dataFileLocation = findDataFile(dataFilename).getAbsolutePath();
        } catch (Exception e) {
            DataFileHelper.cantFindDataFile(dataFilename);
            throw e;
        }
        return dataFileLocation;
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
