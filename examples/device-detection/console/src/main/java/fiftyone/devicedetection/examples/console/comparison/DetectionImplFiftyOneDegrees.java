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

package fiftyone.devicedetection.examples.console.comparison;

import fiftyone.devicedetection.DeviceDetectionOnPremisePipelineBuilder;
import fiftyone.devicedetection.DeviceDetectionPipelineBuilder;
import fiftyone.devicedetection.examples.shared.DataFileHelper;
import fiftyone.devicedetection.examples.shared.PropertyHelper;
import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectPropertyValue;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Implementation of {@link Detection} for 51Degrees. It uses the free Lite
 * data file unless it is given another, such as the Enterprise data file,
 * which has every property it reads.
 */
public class DetectionImplFiftyOneDegrees {
    public static final String FIFTY_ONE_DEGREES = "FiftyOneDegrees";

    /**
     * The free data file, downloaded if it is missing.
     */
    public static final String LITE_HASH_DATA_FILE_NAME =
            "device-detection/51Degrees-LiteV4.1.hash";

    /**
     * The data file with every property this solution reads.
     */
    public static final String ENTERPRISE_HASH_DATA_FILE_NAME = "Enterprise-HashV41.hash";

    /**
     * Where to ask for a copy of the Enterprise data file, tagged with this
     * file.
     */
    private static final String CONTACT_US = "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-devicedetection-examples-console-comparison-detectionimplfiftyonedegrees.java&utm_term=enterprise-data-file";

    /**
     * The properties this solution reads.
     */
    private static final String[] PROPERTIES = {
            "HardwareVendor", "HardwareModel", "BrowserVendor",
            "BrowserVersion", "IsMobile", "DeviceType"};

    public static class FiftyOneConfig {
        String dataFile;

        public FiftyOneConfig() {
            this(LITE_HASH_DATA_FILE_NAME);
        }

        /**
         * @param dataFile the data file to use, found as the other examples
         *                 find theirs
         */
        public FiftyOneConfig(String dataFile) {
            this.dataFile =
                    DataFileHelper.findDataFile(dataFile).getAbsolutePath();
        }
    }

    public static class FiftyOneProperties extends Detection.Properties.Base {

        /**
         * @param missing the properties the data file does not have, which
         *                read as needing the Enterprise data file
         */
        FiftyOneProperties(Detection.Request request, FlowData flowData,
                           Set<String> missing) {
            super(request);
            // transcribe properties so flowData can be freed
            DeviceData device = flowData.get(DeviceData.class);
            isMobile = valueOf(missing, "IsMobile", device::getIsMobile);
            hardwareVendor = valueOf(missing, "HardwareVendor",
                    device::getHardwareVendor);
            hardwareModel = valueOf(missing, "HardwareModel",
                    device::getHardwareModel);
            browserVendor = valueOf(missing, "BrowserVendor",
                    device::getBrowserVendor);
            browserVersion = valueOf(missing, "BrowserVersion",
                    device::getBrowserVersion);
            deviceType = valueOf(missing, "DeviceType", device::getDeviceType);
        }

        private static String valueOf(
                Set<String> missing,
                String name,
                Supplier<? extends AspectPropertyValue<?>> value) {
            return missing.contains(name) ?
                    DataFileHelper.NEEDS_ENTERPRISE :
                    PropertyHelper.asString(value.get());
        }

        @Override
        public String getVendorId() {
            return FIFTY_ONE_DEGREES;
        }
    }

    public static class FiftyOneSolution implements Detection.Solution {
        Pipeline pipeline;
        FiftyOneConfig config;
        private final String dataFile;
        private Set<String> missing = Collections.emptySet();
        private boolean lite;

        public FiftyOneSolution() {
            this(LITE_HASH_DATA_FILE_NAME);
        }

        /**
         * @param dataFile the data file to use
         */
        public FiftyOneSolution(String dataFile) {
            this.dataFile = dataFile;
        }

        @Override
        public void initialise(int numberOfThreads) throws Exception {
            this.config = new FiftyOneConfig(dataFile);

            // The Lite data file has only some of the properties, so it is
            // given none to restrict to, and the rest read as needing the
            // Enterprise data file.
            lite = "Lite".equals(DataFileHelper.getDatafileMetaData(
                    this.config.dataFile).getTier());
            DeviceDetectionOnPremisePipelineBuilder builder =
                    new DeviceDetectionPipelineBuilder()
                    .useOnPremise(this.config.dataFile, false)
                    .setPerformanceProfile(Constants.PerformanceProfiles.MaxPerformance)
                    .setUsePerformanceGraph(true)
                    .setUsePredictiveGraph(false)
                    .setShareUsage(false)
                    .setAutoUpdate(false)
                    .setConcurrency(numberOfThreads);
            if (lite == false) {
                for (String property : PROPERTIES) {
                    builder.setProperty(property);
                }
            }
            pipeline = builder.build();

            DeviceDetectionHashEngine engine =
                    pipeline.getElement(DeviceDetectionHashEngine.class);
            Set<String> notInFile = new HashSet<>();
            for (String property : PROPERTIES) {
                if (DataFileHelper.hasProperty(engine, property) == false) {
                    notInFile.add(property);
                }
            }
            missing = notInFile;
        }

        @Override
        public Detection.Properties detect(Detection.Request request) throws Exception {
            try (FlowData flowData = pipeline.createFlowData()){
                flowData.addEvidence(request.getEvidence()).process();
                return new FiftyOneProperties(request, flowData, missing);
            }
        }

        /**
         * On the free Lite data file, says what the Enterprise data file
         * adds and where to ask for a copy.
         */
        @Override
        public void writeNote(PrintWriter writer) {
            if (lite) {
                DataFileHelper.writeEnterpriseOffer(writer, CONTACT_US);
            }
        }

        @Override
        public String getVendorId() {
            return FIFTY_ONE_DEGREES;
        }

        @Override
        public void close() throws Exception {
            if (Objects.nonNull(pipeline)) {
                pipeline.close();
            }
        }
    }
}
