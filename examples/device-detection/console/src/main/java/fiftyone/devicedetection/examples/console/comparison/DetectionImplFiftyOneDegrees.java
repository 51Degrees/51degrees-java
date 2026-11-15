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

import fiftyone.devicedetection.DeviceDetectionPipelineBuilder;
import fiftyone.devicedetection.examples.shared.DataFileHelper;
import fiftyone.devicedetection.examples.shared.PropertyHelper;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.devicedetection.shared.testhelpers.FileUtils;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.util.FileFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;

/**
 * Implementation of {@link Detection} for 51Degrees.
 */
public class DetectionImplFiftyOneDegrees {
    public static final String FIFTY_ONE_DEGREES = "FiftyOneDegrees";
    public static final String ENTERPRISE_HASH_DATA_FILE_NAME = "Enterprise-HashV41.hash";
    public static class FiftyOneConfig {
        String dataFile;

        /**
         * Uses the Enterprise data file when there is one, and otherwise
         * the free Lite data file, which is downloaded if it is missing,
         * so that the comparison runs from a fresh clone.
         * @throws IllegalArgumentException if neither file can be found
         */
        public FiftyOneConfig() throws IllegalArgumentException {
            File enterprise;
            try {
                enterprise = FileFinder.getFilePath(ENTERPRISE_HASH_DATA_FILE_NAME);
            } catch (IllegalArgumentException notFound) {
                enterprise = null;
            }
            dataFile = (enterprise != null ? enterprise :
                    DataFileHelper.findDataFile(FileUtils.LITE_HASH_DATA_FILE_PATH))
                    .getAbsolutePath();
        }
    }

    public static class FiftyOneProperties extends Detection.Properties.Base {

        FiftyOneProperties(Detection.Request request, FlowData flowData) {
            super(request);
            // transcribe properties so flowData can be freed
            DeviceData device = flowData.get(DeviceData.class);
            // The free Lite data file does not have every property, so a
            // property it lacks is read without stopping the comparison
            isMobile = PropertyHelper.asString(PropertyHelper.tryGet(device::getIsMobile));
            hardwareVendor = PropertyHelper.asString(PropertyHelper.tryGet(device::getHardwareVendor));
            hardwareModel = PropertyHelper.asString(PropertyHelper.tryGet(device::getHardwareModel));
            browserVendor = PropertyHelper.asString(PropertyHelper.tryGet(device::getBrowserVendor));
            browserVersion = PropertyHelper.asString(PropertyHelper.tryGet(device::getBrowserVersion));
            deviceType = PropertyHelper.asString(PropertyHelper.tryGet(device::getDeviceType));
        }

        @Override
        public String getVendorId() {
            return FIFTY_ONE_DEGREES;
        }
    }

    public static class FiftyOneSolution implements Detection.Solution {
        Pipeline pipeline;
        FiftyOneConfig config;


        @Override
        public void initialise(int numberOfThreads) throws Exception {
            try {
                this.config = new FiftyOneConfig();
            } catch (IllegalArgumentException e) {
                FileNotFoundException notFound = new FileNotFoundException(
                        "Neither the Enterprise data file nor the free Lite " +
                        "data file was found");
                notFound.initCause(e);
                throw notFound;
            }

            pipeline = new DeviceDetectionPipelineBuilder()
                    .useOnPremise(this.config.dataFile, false)
                    .setPerformanceProfile(Constants.PerformanceProfiles.MaxPerformance)
                    .setUsePerformanceGraph(true)
                    .setUsePredictiveGraph(false)
                    .setShareUsage(false)
                    .setAutoUpdate(false)
                    .setConcurrency(numberOfThreads)
                    .setProperty("hardwareVendor")
                    .setProperty("hardwareModel")
                    .setProperty("browserVendor")
                    .setProperty("browserVersion")
                    .setProperty("isMobile")
                    .setProperty("deviceType")
                    .build();
        }

        @Override
        public Detection.Properties detect(Detection.Request request) throws Exception {
            try (FlowData flowData = pipeline.createFlowData()){
                flowData.addEvidence(request.getEvidence()).process();
                return new FiftyOneProperties(request, flowData);
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
