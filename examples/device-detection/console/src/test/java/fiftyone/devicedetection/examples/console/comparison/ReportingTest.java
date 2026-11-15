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

import fiftyone.devicedetection.examples.shared.DataFileHelper;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Checks that the comparison shows what only the Enterprise data file can
 * do when it ran on the free Lite data file: each property the data file
 * has no value for reads as needing the Enterprise data file, and the
 * output gives the contact page link once, tagged with the example's file
 * (device-detection-specification/required-examples.md, "Examples MUST also
 * show what only the Enterprise Data File can do").
 */
public class ReportingTest {

    private static final String CONTACT_US = "https://51degrees.com/contact-us?";

    private static final String EXAMPLE_FILE =
            "utm_content=console-src-main-java-fiftyone-devicedetection-examples-console-comparison-comparer.java";

    /**
     * A detection as the 51Degrees solution makes it with the Lite data
     * file, which has IsMobile and the browser but not the hardware.
     */
    private static Detection.Properties liteDetection() {
        Detection.Properties.Base properties =
                new Detection.Properties.Base(new Detection.Request(new HashMap<>())) {
                    @Override
                    public String getVendorId() {
                        return DetectionImplFiftyOneDegrees.FIFTY_ONE_DEGREES;
                    }
                };
        properties.isMobile = "False";
        properties.browserVendor = "Google";
        properties.browserVersion = "98";
        properties.hardwareVendor = DataFileHelper.NEEDS_ENTERPRISE;
        properties.hardwareModel = DataFileHelper.NEEDS_ENTERPRISE;
        properties.deviceType = DataFileHelper.NEEDS_ENTERPRISE;
        return properties;
    }

    private static String report(Detection.Properties properties) {
        Detection.BenchmarkResult benchmark = new Detection.BenchmarkResult(1);
        benchmark.properties.add(properties);
        benchmark.count = 1;
        Comparer.ExecutionResult execution = new Comparer.ExecutionResult(
                properties.getVendorId(),
                Collections.singletonList(benchmark), 1);
        StringWriter output = new StringWriter();
        new Reporting.Minimal().report(Collections.singletonList(execution),
                new PrintWriter(output, true));
        return output.toString();
    }

    @Test
    public void saysWhatNeedsTheEnterpriseDataFile() {
        String output = report(liteDetection());
        assertTrue("The report should show each property the data file " +
                "has no value for as needing the Enterprise data file. " +
                output, output.contains("Hardware Vendor: " +
                DataFileHelper.NEEDS_ENTERPRISE));
        assertTrue("The report should show the properties the Lite data " +
                "file has. " + output, output.contains("Browser Vendor: Google"));
        assertTrue("The report should give the link to the contact page " +
                "for a copy of the Enterprise data file. " + output,
                output.contains(CONTACT_US));
        assertTrue("The link should carry the example's file as " +
                "utm_content. " + output, output.contains(EXAMPLE_FILE));
        assertEquals("The link should be given once. " + output,
                output.indexOf(CONTACT_US), output.lastIndexOf(CONTACT_US));
    }
}
