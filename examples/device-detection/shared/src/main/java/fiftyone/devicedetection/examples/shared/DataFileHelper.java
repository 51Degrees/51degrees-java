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
import fiftyone.pipeline.engines.fiftyone.data.FiftyOneDataFile;
import fiftyone.pipeline.util.FileFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static fiftyone.pipeline.util.FileFinder.getFilePath;

public class DataFileHelper {
    static Logger logger = LoggerFactory.getLogger(DataFileHelper.class);

    /**
     * Sent with each download, so the Distributor can count the downloads
     * made by these examples.
     */
    private static final String DOWNLOAD_SOURCE = "examples-51degrees-java";

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
        if (dataTier.equals("Lite")) {
            logger.warn("This example is using the 'Lite ' data file. " +
                    "This is used for illustration, and has limited " +
                    "accuracy and capabilities. Find out about the " +
                    "Enterprise data file on our pricing page: " +
                    "https://51degrees.com/pricing?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-devicedetection-examples-shared-datafilehelper.java&utm_term=lite-data-file");
        }
        if (daysOld > 28) {
            logger.warn("This example is using a data file that is more " +
                    "than {} days old. A more recent data file " +
                    "may be needed to correctly detect the latest " +
                    "devices, browsers, etc.", daysOld);
            logger.info("Run get-data-files.ps1 -Update or " +
                    "get-data-files.sh -u in cxx/data, which is the data " +
                    "repository https://github.com/51Degrees/data, to get " +
                    "the latest 'Lite' data file. " +
                    "Find out about the Enterprise data file, which " +
                    "includes automatic daily updates, on our pricing " +
                    "page: https://51degrees.com/pricing?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-devicedetection-examples-shared-datafilehelper.java&utm_term=data-file-age-warning");
        }
    }

    public static void cantFindDataFile(String dataFile) {
        logger.error("Could not find the data file '{}' which must be " +
                "somewhere in the project space to be found.", dataFile);
        logger.info("The free 'Lite' data file and the evidence files " +
                "are downloaded by get-data-files.ps1 or get-data-files.sh " +
                "in cxx/data, which is the data repository " +
                "https://github.com/51Degrees/data. " +
                "Find out about the Enterprise data file, which includes " +
                "automatic daily updates, on our pricing " +
                "page: https://51degrees.com/pricing?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-devicedetection-examples-shared-datafilehelper.java&utm_term=data-file-not-found");
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
            dataFilename = FileUtils.LITE_HASH_DATA_FILE_NAME;
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
     * {@link FileFinder#getFilePath(String)} does. A free data file that is
     * missing has a {@code <file>.link} in the data repository, so the data
     * repository's script is run to download it, and the file is looked for
     * again.
     * @param file the name of the file, or the end of its path
     * @return the file
     * @throws IllegalArgumentException if the file was not found, naming the
     *         command that downloads it when there is one
     */
    public static File findDataFile(String file) {
        try {
            return getFilePath(file);
        } catch (IllegalArgumentException notFound) {
            File link;
            try {
                link = getFilePath(file + ".link");
            } catch (IllegalArgumentException noLink) {
                throw notFound;
            }
            String command = runDownload(link);
            try {
                return getFilePath(file);
            } catch (IllegalArgumentException stillNotFound) {
                String message = "Could not download '" + file +
                        "'. Run this command to download it: " + command;
                logger.error(message);
                throw new IllegalArgumentException(message, stillNotFound);
            }
        }
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
        List<String> command;
        if (System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT).startsWith("windows")) {
            command = Arrays.asList("powershell", "-NoProfile",
                    "-ExecutionPolicy", "Bypass", "-File",
                    new File(data, "get-data-files.ps1").getPath(), name,
                    "-Source", DOWNLOAD_SOURCE);
        } else {
            command = Arrays.asList("sh",
                    new File(data, "get-data-files.sh").getPath(),
                    "-s", DOWNLOAD_SOURCE, name);
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
            new ProcessBuilder(command).inheritIO().start().waitFor();
        } catch (IOException e) {
            logger.error("Could not run {}. {}", line, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return line;
    }
}
