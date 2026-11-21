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

import fiftyone.devicedetection.examples.shared.ExampleProcess;
import fiftyone.devicedetection.examples.shared.FindOutMore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Runs each cloud example as it runs from a fresh clone, with no resource
 * key anywhere, and checks that it ends without an error, names the
 * variable to set and links to where a key is created, then ends with its
 * Find out more section.
 */
public class CloudExamplesWithoutKeyTest {

    private static final String CONFIGURE = "https://51degrees.com/configure/";

    private static final String TAGS =
        "?utm_source=code&utm_medium=example&utm_campaign=51degrees-java" +
        "&utm_content=";

    private static final String CONTACT_US =
        "https://51degrees.com/contact-us" + TAGS;

    private static String runWithoutKey(Class<?> example, String variable)
        throws Exception {
        ExampleProcess.Result result = ExampleProcess.runWithoutKeys(example);
        System.out.println(result.output);
        String output = result.output;
        assertEquals(example.getSimpleName() + " should end with exit code 0",
            0, result.exitCode);
        assertTrue("The output should name " + variable,
            output.contains("Set the " + variable + " environment variable"));
        int link = output.indexOf(CONFIGURE);
        assertTrue("The output should link to where a key is created",
            link >= 0 && output.startsWith(TAGS,
                output.indexOf('?', link)));
        assertTrue("The output should end with the '" + FindOutMore.HEADING +
                "' section after the link",
            output.lastIndexOf(FindOutMore.HEADING) > link);
        return output;
    }

    @Test
    public void gettingStartedCloud() throws Exception {
        runWithoutKey(GettingStartedCloud.class, "_51DEGREES_RESOURCE_KEY");
    }

    @Test
    public void metadataCloud() throws Exception {
        runWithoutKey(MetadataCloud.class, "_51DEGREES_RESOURCE_KEY");
    }

    @Test
    public void minimalExample() throws Exception {
        runWithoutKey(MinimalExample.class, "_51DEGREES_RESOURCE_KEY");
    }

    @Test
    public void tacCloud() throws Exception {
        String output = runWithoutKey(TacCloud.class, "SuperResourceKey");
        assertTrue("The output should link to the contact page",
            output.contains(CONTACT_US));
    }

    @Test
    public void nativeModelCloud() throws Exception {
        String output = runWithoutKey(NativeModelCloud.class,
            "SuperResourceKey");
        assertTrue("The output should link to the contact page",
            output.contains(CONTACT_US));
    }
}
