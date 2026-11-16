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

import fiftyone.devicedetection.DeviceDetectionPipelineBuilder;
import fiftyone.devicedetection.examples.shared.DataFileHelper;
import fiftyone.devicedetection.examples.shared.FindOutMore;
import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.devicedetection.examples.shared.PropertyHelper.asString;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * Shows how to make the on-premise device detection engine load a new data
 * file when the application decides to, rather than through an automatic
 * update. The engine goes on detecting while the file loads, so the
 * application does not need to restart.
 * <p>
 * In practice a newer data file is copied over the one the engine was built
 * with, for example by a scheduled task, and then refreshData is called.
 * This example has no newer file, so it reloads the file it started with
 * and the results stay the same. refreshData can also be given the
 * contents of a data file held in memory.
 */
public class ManualDataUpdate {
    /**
     * This example's tag in the links of its "Find out more" section.
     */
    private static final String EXAMPLE_TAG =
            "console-src-main-java-fiftyone-devicedetection-examples-console-manualdataupdate.java";

    /**
     * Where to ask for a copy of the Enterprise data file, tagged with this
     * example.
     */
    private static final String CONTACT_US = "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-devicedetection-examples-console-manualdataupdate.java&utm_term=enterprise-data-file";

    private static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36";

    private static final String IPHONE_USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_2 like Mac OS X) " +
            "AppleWebKit/604.4.7 (KHTML, like Gecko) Mobile/15C114";

    private static final Logger logger =
            LoggerFactory.getLogger(ManualDataUpdate.class);

    public static void main(String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));
        // The free Lite data file is used unless another is given.
        String dataFile = args.length > 0 ?
                args[0] : GettingStartedOnPrem.LITE_V_4_1_HASH;
        run(dataFile, System.out);
        FindOutMore.write(System.out, EXAMPLE_TAG,
                FindOutMore.ON_PREMISE_REPOSITORIES);
    }

    /**
     * Run the example.
     * @param dataFile a device detection data file
     * @param outputStream somewhere for the results
     */
    public static void run(String dataFile, OutputStream outputStream)
            throws Exception {
        String dataFileLocation;
        try {
            dataFileLocation =
                    DataFileHelper.findDataFile(dataFile).getAbsolutePath();
        } catch (Exception e) {
            DataFileHelper.cantFindDataFile(dataFile);
            throw e;
        }
        PrintWriter writer = new PrintWriter(outputStream, true);
        logger.info("Constructing pipeline from file " + dataFileLocation);

        // The data file changes only when this code says so, because
        // automatic updates and the file system watcher are off.
        try (Pipeline pipeline = new DeviceDetectionPipelineBuilder()
                // The engine works from a copy of the data file, so a newer
                // file can be copied over the original while it runs.
                .useOnPremise(dataFileLocation, true)
                .setAutoUpdate(false)
                .setDataFileSystemWatcher(false)
                .setPerformanceProfile(Constants.PerformanceProfiles.LowMemory)
                // Usage sharing is off because this example's evidence is already
                // known to 51Degrees. In production, leave usage sharing on, which adds
                // a Usage Sharing Element to the Pipeline and helps improve the service.
                .setShareUsage(false)
                .build()) {
            DeviceDetectionHashEngine engine =
                    pipeline.getElement(DeviceDetectionHashEngine.class);

            boolean needsEnterprise = false;
            needsEnterprise |= checkIfMobile(pipeline, engine,
                    DESKTOP_USER_AGENT, writer);
            needsEnterprise |= checkIfMobile(pipeline, engine,
                    IPHONE_USER_AGENT, writer);

            writer.println("Reloading the data file published on " +
                    published(engine) + ".");
            // Load the data file again, from the place the engine was built
            // with. Call this once a newer data file has been copied there.
            engine.refreshData(engine.getDataFileMetaData().getIdentifier());
            writer.println("Reloaded the data file published on " +
                    published(engine) + ".");
            writer.println();

            needsEnterprise |= checkIfMobile(pipeline, engine,
                    DESKTOP_USER_AGENT, writer);
            needsEnterprise |= checkIfMobile(pipeline, engine,
                    IPHONE_USER_AGENT, writer);

            if (needsEnterprise) {
                writer.println("This example reloaded the free Lite data " +
                        "file. The Enterprise data file adds daily automatic " +
                        "updates.");
                DataFileHelper.writeEnterpriseNote(writer, CONTACT_US);
            }
        }
        writer.flush();
    }

    /**
     * Detect the device a User-Agent comes from and write whether it is a
     * mobile device, and its hardware vendor, which only the Enterprise data
     * file has.
     * @return true if the hardware vendor needs the Enterprise data file
     */
    private static boolean checkIfMobile(Pipeline pipeline,
                                         DeviceDetectionHashEngine engine,
                                         String userAgent,
                                         PrintWriter writer)
            throws Exception {
        // The flow data is closed straight away, which releases the results
        // of the detection. Results that are not released keep the data
        // they were read from in memory, even after the data is refreshed.
        try (FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", userAgent);
            data.process();
            DeviceData device = data.get(DeviceData.class);
            writer.println("Is User-Agent '" + userAgent +
                    "' a mobile device? " + asString(device.getIsMobile()));
            if (DataFileHelper.hasProperty(engine, "HardwareVendor")) {
                writer.println("\tHardware Vendor: " +
                        asString(device.getHardwareVendor()));
                writer.println();
                return false;
            }
            writer.println("\tHardware Vendor: " +
                    DataFileHelper.NEEDS_ENTERPRISE);
            writer.println();
            return true;
        }
    }

    /**
     * The date the engine's data file was published.
     */
    private static String published(DeviceDetectionHashEngine engine) {
        return new SimpleDateFormat("yyyy-MM-dd").format(
                engine.getDataFileMetaData().getDataPublishedDateTime());
    }
}
/*!
 * @example console/ManualDataUpdate.java
 * This example shows how to make the on-premise device detection engine
 * load a new data file when the application decides to, rather than through
 * an automatic update.
 * <p>
 * This example is available in full on [GitHub](https://github.com/51Degrees/51degrees-java/blob/main/examples/device-detection/console/src/main/java/fiftyone/devicedetection/examples/console/ManualDataUpdate.java).
 * @include{doc} example-require-datafile.txt
 */
