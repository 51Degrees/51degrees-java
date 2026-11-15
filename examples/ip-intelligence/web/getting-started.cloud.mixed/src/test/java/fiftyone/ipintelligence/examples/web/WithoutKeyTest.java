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

package fiftyone.ipintelligence.examples.web;

import fiftyone.ipintelligence.examples.shared.KeyHelper;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

/**
 * Runs the mixed cloud web example with no resource key
 * (device-detection-specification/required-examples.md, "A cloud example run
 * with no Resource Key MUST end without an error, name the environment
 * variable to set, give a link to where a Resource Key is created, such as
 * the configurator, and exit successfully", which "applies to every example
 * that uses the cloud service, whatever its engines", with the link tagged
 * with the example's file as utm_content). The key is an empty argument, so
 * a key in the environment is not read.
 */
public class WithoutKeyTest {

    private static final String TAG =
        "web-getting-started.cloud.mixed-src-main-java-fiftyone-ipintelligence-examples-web-gettingstartedwebcloudmixed.java";

    @Test
    public void endsAndSaysHowToGetAKey() throws Exception {
        PrintStream out = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, "UTF-8"));
        try {
            GettingStartedWebCloudMixed.main(new String[]{""});
        } finally {
            System.setOut(out);
        }
        String output = buffer.toString("UTF-8");
        out.println(output);
        assertTrue(output.contains(KeyHelper.RESOURCE_KEY_VARIABLE));
        String link = "https://51degrees.com/configure[^\\s\"]*\\?" +
            "utm_source=code&utm_medium=example" +
            "&utm_campaign=51degrees-java&utm_content=" +
            java.util.regex.Pattern.quote(TAG) + "&";
        assertTrue("The output should link to the configurator, tagged " +
                "with the example's file, as '" + link + "'",
            java.util.regex.Pattern.compile(link).matcher(output).find());
    }
}
