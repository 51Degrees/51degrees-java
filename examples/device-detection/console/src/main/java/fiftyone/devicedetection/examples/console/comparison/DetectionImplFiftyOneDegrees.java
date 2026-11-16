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
import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.util.FileFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

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
                    DataFileHelper.findDataFile(DataFileHelper.LITE_HASH_DATA_FILE_PATH))
                    .getAbsolutePath();
        }
    }

    public static class FiftyOneProperties extends Detection.Properties.Base {

        FiftyOneProperties(Detection.Request request, FlowData flowData,
                           Set<String> lacking) {
            super(request);
            // transcribe properties so flowData can be freed
            DeviceData device = flowData.get(DeviceData.class);
            // The free Lite data file does not have every property. A
            // property it lacks reads as needing the Enterprise data file,
            // rather than stopping the comparison or showing nothing.
            isMobile = enterprise(lacking, "IsMobile", device::getIsMobile);
            hardwareVendor = enterprise(lacking, "HardwareVendor", device::getHardwareVendor);
            hardwareModel = enterprise(lacking, "HardwareModel", device::getHardwareModel);
            browserVendor = enterprise(lacking, "BrowserVendor", device::getBrowserVendor);
            browserVersion = enterprise(lacking, "BrowserVersion", device::getBrowserVersion);
            deviceType = enterprise(lacking, "DeviceType", device::getDeviceType);
        }

        /**
         * The value of a property as a string, or
         * {@link DataFileHelper#NEEDS_ENTERPRISE} when the data file in use
         * does not have the property.
         */
        private static <T> String enterprise(Set<String> lacking, String name,
                                             Supplier<AspectPropertyValue<T>> getter) {
            if (lacking.contains(name)) {
                return DataFileHelper.NEEDS_ENTERPRISE;
            }
            try {
                return PropertyHelper.asString(getter.get());
            } catch (PropertyMissingException missing) {
                return DataFileHelper.NEEDS_ENTERPRISE;
            }
        }

        @Override
        public String getVendorId() {
            return FIFTY_ONE_DEGREES;
        }
    }

    public static class FiftyOneSolution implements Detection.Solution {
        private static final String[] PROPERTIES = {
                "IsMobile", "HardwareVendor", "HardwareModel",
                "BrowserVendor", "BrowserVersion", "DeviceType"};
        Pipeline pipeline;
        FiftyOneConfig config;
        Set<String> lacking = new HashSet<>();


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

            // The properties the data file in use does not have, which only
            // the Enterprise data file has
            DeviceDetectionHashEngine engine =
                    pipeline.getElement(DeviceDetectionHashEngine.class);
            lacking = new HashSet<>();
            for (String name : PROPERTIES) {
                if (DataFileHelper.hasProperty(engine, name) == false) {
                    lacking.add(name);
                }
            }
        }

        @Override
        public Detection.Properties detect(Detection.Request request) throws Exception {
            try (FlowData flowData = pipeline.createFlowData()){
                flowData.addEvidence(request.getEvidence()).process();
                return new FiftyOneProperties(request, flowData, lacking);
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
