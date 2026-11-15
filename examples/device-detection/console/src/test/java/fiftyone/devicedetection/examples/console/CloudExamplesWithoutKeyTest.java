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

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

/**
 * Runs each cloud console example with no resource key
 * (device-detection-specification/required-examples.md, "A cloud example run
 * with no Resource Key MUST end without an error, name the environment
 * variable to set, give a link to where a Resource Key is created, such as
 * the configurator, and exit successfully", with the link tagged with
 * utm_source=code, utm_medium=example, the repository's name as
 * utm_campaign and the example's file as utm_content).
 *
 * The key is given as an empty argument, which is not a key, so that a key
 * set in the environment, as CI sets one for the other tests, is not read.
 */
public class CloudExamplesWithoutKeyTest {

    private interface Main {
        void main(String[] args) throws Exception;
    }

    /**
     * Runs the example's main with an empty key, which must return without
     * an exception, and gives what it wrote to standard output, where the
     * examples' log goes too.
     */
    private static String runWithoutKey(Main main) throws Exception {
        PrintStream out = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, "UTF-8"));
        try {
            main.main(new String[]{""});
        } finally {
            System.setOut(out);
        }
        String output = buffer.toString("UTF-8");
        out.println(output);
        return output;
    }

    /**
     * The tag the repository gives this example's file in its links.
     */
    private static String tag(Class<?> example) {
        return "console-src-main-java-fiftyone-devicedetection-examples-console-" +
            example.getSimpleName().toLowerCase() + ".java";
    }

    private static void assertSaysHowToGetAKey(String output,
                                               Class<?> example,
                                               String variable) {
        assertTrue("The output should name the environment variable '" +
            variable + "'", output.contains(variable));
        assertTrue("The output should link to the configurator",
            output.contains("https://51degrees.com/configure"));
        String link = "utm_source=code&utm_medium=example" +
            "&utm_campaign=51degrees-java&utm_content=" + tag(example) + "&";
        assertTrue("The link should be tagged with the example's file, '" +
            link + "'", output.contains(link));
    }

    @Test
    public void gettingStartedCloud() throws Exception {
        assertSaysHowToGetAKey(runWithoutKey(GettingStartedCloud::main),
            GettingStartedCloud.class, "TestResourceKey");
    }

    @Test
    public void metadataCloud() throws Exception {
        assertSaysHowToGetAKey(runWithoutKey(MetadataCloud::main),
            MetadataCloud.class, "TestResourceKey");
    }

    @Test
    public void tacCloud() throws Exception {
        assertSaysHowToGetAKey(runWithoutKey(TacCloud::main),
            TacCloud.class, TacCloud.TAC_EXAMPLE_RESOURCE_KEY_NAME);
    }

    @Test
    public void nativeModelCloud() throws Exception {
        assertSaysHowToGetAKey(runWithoutKey(NativeModelCloud::main),
            NativeModelCloud.class,
            NativeModelCloud.NATIVE_MODEL_EXAMPLE_RESOURCE_KEY_NAME);
    }

    @Test
    public void minimalExample() throws Exception {
        assertSaysHowToGetAKey(runWithoutKey(MinimalExample::main),
            MinimalExample.class, "TestResourceKey");
    }
}
