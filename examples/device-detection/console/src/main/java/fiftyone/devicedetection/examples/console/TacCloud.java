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

import fiftyone.devicedetection.examples.shared.FindOutMore;
import fiftyone.devicedetection.examples.shared.KeyHelper;
import fiftyone.devicedetection.cloud.data.MultiDeviceDataCloud;
import fiftyone.devicedetection.cloud.flowelements.HardwareProfileCloudEngine;
import fiftyone.devicedetection.shared.Constants;
import fiftyone.devicedetection.shared.DeviceData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.configuration.PipelineOptionsFactory;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.fiftyone.flowelements.FiftyOnePipelineBuilder;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import static fiftyone.common.testhelpers.LogbackHelper.configureLogback;
import static fiftyone.devicedetection.examples.shared.PropertyHelper.asString;
import static fiftyone.devicedetection.examples.shared.KeyHelper.*;
import static fiftyone.pipeline.util.FileFinder.getFilePath;

/**
 * This example demonstrates looking up device details using a TAC code.
 * <p>
 * Unlike other examples, use of this example requires a license key which can be purchased from our
 * <a href="https://51degrees.com/pricing?utm_source=code&amp;utm_medium=example&amp;utm_campaign=51degrees-java&amp;utm_content=console-src-main-java-fiftyone-devicedetection-examples-console-taccloud.java&amp;utm_term=taccloud">pricing page</a>. Once this is done, a resource key with the
 * properties required by this example can be created
 * <a href="https://51degrees.com/configure/QKyYH5XT?utm_source=code&amp;utm_medium=example&amp;utm_campaign=51degrees-java&amp;utm_content=console-src-main-java-fiftyone-devicedetection-examples-console-taccloud.java&amp;utm_term=taccloud">here</a>.
 * <p>
 * This resource key can be supplied as a command line argument to this program, as an environment
 * variable "_51DEGREES_RESOURCE_KEY", as a System Property "_51DEGREES_RESOURCE_KEY" or by editing the config
 * file tacCloud.xml.
 */
public class TacCloud {
    /**
     * This example's tag in the links of its "Find out more" section.
     */
    private static final String EXAMPLE_TAG =
            "console-src-main-java-fiftyone-devicedetection-examples-console-taccloud.java";

    /**
     * The hardware engine's property that holds the matching devices.
     */
    private static final String PROFILES = "Profiles";

    /**
     * The tag of the links given when the resource key lacks the lookup.
     */
    private static final String NO_HARDWARE_LOOKUP = "no-hardware-lookup";

    // Example values to use when looking up device details from TACs.
    static final String TAC_1 = "35925406";
    static final String TAC_2 = "86386802";
    public static final String TAC_EXAMPLE_RESOURCE_KEY_NAME = KeyHelper.RESOURCE_KEY_VARIABLE;

    public static void main (String[] args) throws Exception {
        configureLogback(getFilePath("logback.xml"));
        String resourceKey = KeyHelper.findResourceKey(
                args.length > 0 ? args[0] : null,
                TAC_EXAMPLE_RESOURCE_KEY_NAME);
        // A key typed into the options file is used as it is.
        String fileResourceKey = PipelineOptionsFactory.getOptionsFromFile(
                getFilePath("tacCloud.xml"))
                .find("CloudRequestEngine", "ResourceKey");
        if (resourceKey == null && isInvalidKey(fileResourceKey)) {
            KeyHelper.writeNoResourceKey(System.out, EXAMPLE_TAG,
                    TAC_EXAMPLE_RESOURCE_KEY_NAME,
                    KeyHelper.PAID_PROPERTIES_CONFIGURATION);
            FindOutMore.write(System.out, EXAMPLE_TAG,
                    FindOutMore.CLOUD_REPOSITORIES);
            return;
        }
        run(resourceKey, System.out);
    }

    public static void run(String resourceKey, OutputStream os) throws Exception {
        // In this example, we use the FiftyOnePipelineBuilder and configure it from a file.
        // For a demonstration of how to do this in code instead, see the
        // NativeModelCloud example.
        // For more information about builders in general see the documentation at
        // https://51degrees.com/documentation/_pipeline_api__concepts__configuration__builders__index.html?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-devicedetection-examples-console-taccloud.java&utm_term=run

        // the configuration file is in the resources directory
        File optionsFile = getFilePath("tacCloud.xml");
        // load the options and if no resource key has been set in the file
        // use the one supplied to this method
        PipelineOptions pipelineOptions = PipelineOptionsFactory.getOptionsFromFile(optionsFile);
        String fileResourceKey = pipelineOptions.find("CloudRequestEngine", "ResourceKey");
        if (isInvalidKey(fileResourceKey)) {
            getOrSetPaidResourceKey(resourceKey, TAC_EXAMPLE_RESOURCE_KEY_NAME);
        }
        try (PrintWriter writer = new PrintWriter(os)) {
            writer.println("This example shows the details of devices " +
                    "associated with a given 'Type Allocation Code' or 'TAC'.");
            writer.println("More background information on TACs can be " +
                    "found through various online sources such as Wikipedia: " +
                    "https://en.wikipedia.org/wiki/Type_Allocation_Code");
            writer.println("----------------------------------------");



            // Create the pipeline using the service provider and the configured options.
            try (Pipeline pipeline = new FiftyOnePipelineBuilder()
                    .buildFromConfiguration(pipelineOptions)) {
                // The pipeline asked the cloud for the key's accessible
                // properties as it was built, so unless that request failed,
                // a key without the lookup is found here, before any lookup.
                if (lacksHardwareLookup(pipeline)) {
                    writeNoHardwareLookup(writer);
                } else if (analyseTac(TAC_1, pipeline, writer)) {
                    // Pass the second TAC into the pipeline and list the
                    // matching devices.
                    analyseTac(TAC_2, pipeline, writer);
                }
            }
            // The writer closes the stream it was given, so the section is
            // written before it.
            FindOutMore.write(writer, EXAMPLE_TAG,
                    FindOutMore.CLOUD_REPOSITORIES);
        }
    }

    /**
     * Lists the devices associated with the TAC, or says that the resource
     * key does not include the hardware lookup.
     * @return false if the key does not include the lookup
     */
    static boolean analyseTac(String tac, Pipeline pipeline, PrintWriter output) throws Exception {
        // Create the FlowData instance.
        try (FlowData data = pipeline.createFlowData()) {
            // Add the TAC as evidence.
            data.addEvidence(Constants.EVIDENCE_QUERY_TAC_KEY, tac);
            // Process the supplied evidence. If the accessible properties
            // could not be fetched as the pipeline was built, processing
            // fetches them first, and throws the cloud service's error if
            // that fails again.
            data.process();
            // So they are known now, and are checked before the result is
            // used.
            if (lacksHardwareLookup(pipeline)) {
                writeNoHardwareLookup(output);
                return false;
            }
            // Get result data from the flow data.
            MultiDeviceDataCloud result = data.get(MultiDeviceDataCloud.class);
            // The 'MultiDeviceDataCloud' object contains one or more instances
            // implementing 'DeviceData' which is the same interface used for standard device
            // detection, so we have access to all the same properties.
            output.format("Which devices are associated with the TAC '%s'?%n", tac);
            if (result.getProfiles().isEmpty()) {
                output.println("\tNo devices are associated with this TAC.");
            }
            for (DeviceData device : result.getProfiles()) {
                AspectPropertyValue<String> vendor = device.getHardwareVendor();
                AspectPropertyValue<List<String>> name = device.getHardwareName();
                AspectPropertyValue<String> model = device.getHardwareModel();

                // output the result, taking care to deal with "no value" cases
                output.format("\t%s %s (%s)%n", asString(vendor), asString(name), asString(model));
            }
            return true;
        }
    }

    /**
     * Says that the resource key does not include the hardware lookup, with
     * where to create a key that does.
     */
    static void writeNoHardwareLookup(PrintWriter output) {
        output.println("The resource key does not include " +
                "HardwarePlatform.Profiles, the hardware lookup this " +
                "example needs, so no devices are listed. Create a " +
                "resource key that includes it at " +
                FindOutMore.link(KeyHelper.PAID_PROPERTIES_CONFIGURATION,
                        EXAMPLE_TAG, NO_HARDWARE_LOOKUP) +
                ", or contact us about it at " +
                FindOutMore.link("contact-us", EXAMPLE_TAG,
                        NO_HARDWARE_LOOKUP) + ".");
    }

    /**
     * Whether the resource key is known to lack the hardware lookup. The
     * hardware engine's properties come from the cloud's accessible
     * properties for the key, and hold Profiles only when the key includes
     * the lookup. Until the accessible properties have been fetched nothing
     * is known to be missing.
     */
    static boolean lacksHardwareLookup(Pipeline pipeline) {
        if (pipeline.getElement(CloudRequestEngine.class)
                .getPublicProperties() == null) {
            return false;
        }
        for (AspectPropertyMetaData property : pipeline
                .getElement(HardwareProfileCloudEngine.class)
                .getProperties()) {
            if (property.getName().equalsIgnoreCase(PROFILES)) {
                return false;
            }
        }
        return true;
    }
}
/*!
 * @example console/TacCloud.java
 *
 * This example shows how to use the 51Degrees Cloud service to look up the details of a device
 * based on a given 'TAC'. More background information on TACs can be found through various online
 * sources such as <a href="https://en.wikipedia.org/wiki/Type_Allocation_Code">Wikipedia</a>.
 *
 * Unlike other examples, use of this example requires a license key which can be purchased from our
 * [pricing page](https://51degrees.com/pricing?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-devicedetection-examples-console-taccloud.java&utm_term=top). Once this is done, a resource key with the
 * properties required by this example can be created [here](https://51degrees.com/configure/QKyYH5XT?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-devicedetection-examples-console-taccloud.java&utm_term=top).
 *
 * This example is available in full on [GitHub](https://github.com/51Degrees/51degrees-java/blob/main/examples/device-detection/console/src/main/java/fiftyone/devicedetection/examples/console/TacCloud.java).
 *
 * @include{doc} example-require-resourcekey.txt
 */