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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.*;
import java.util.function.Function;

public class KeyHelper {
    /**
     * The variable the cloud examples and their tests read a resource key
     * from.
     */
    public static final String TEST_RESOURCE_KEY = "_51DEGREES_RESOURCE_KEY";

    /**
     * The variable the tests of the TAC and native model examples read the
     * resource key with every property from. Each test gives the key to
     * its example in {@link #RESOURCE_KEY_VARIABLE}.
     */
    public static final String BESPOKE_RESOURCE_KEY_VARIABLE =
        "_51DEGREES_RESOURCE_KEY_BESPOKE";
    static Logger logger = LoggerFactory.getLogger(KeyHelper.class);

    /**
     * The shortest a key can be, as text and once decoded.
     */
    private static final int MIN_KEY_LENGTH = 19;

    private static final int MIN_DECODED_KEY_LENGTH = 14;

    /**
     * Read a key as an environment variable, then as a system property of
     * the same name.
     * @param keyName the name of the key
     * @return the key, or null when none is set
     */
    public static String getNamedKey(String keyName) {
        return getNamedKey(keyName, System::getenv);
    }

    static String getNamedKey(String keyName,
                              Function<String, String> environment) {
        String key = environment.apply(keyName);
        if (Objects.isNull(key)) {
            key = System.getProperty(keyName);
        }
        return key;
    }

    /**
     * Whether a value cannot be a key, being missing, too short, or not URL
     * safe base 64 of a long enough key.
     * @param keyValue the value to check
     * @return true if the value cannot be a key
     */
    public static boolean isInvalidKey(String keyValue) {
        try {
            return Objects.isNull(keyValue) ||
                keyValue.trim().length() < MIN_KEY_LENGTH ||
                Base64.getUrlDecoder().decode(keyValue).length <
                    MIN_DECODED_KEY_LENGTH;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    static final String CREATE_KEY_URL =
        "https://51degrees.com/configure/?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-devicedetection-examples-shared-keyhelper.java&utm_term=no-resource-key";

    /**
     * The variable a cloud example reads its resource key from.
     */
    public static final String RESOURCE_KEY_VARIABLE =
        "_51DEGREES_RESOURCE_KEY";

    /**
     * The configuration page that creates a key with the free properties
     * the cloud examples use.
     */
    public static final String FREE_PROPERTIES_CONFIGURATION =
        "configure/jqz435Nc";

    /**
     * The configuration page that creates a key with the properties the
     * TAC and native model examples use, which need a paid licence.
     */
    public static final String PAID_PROPERTIES_CONFIGURATION =
        "configure/QKyYH5XT";

    /**
     * The resource key a cloud example runs with, being the key given as
     * its argument, or else the key under the name given, read as an
     * environment variable and then as a system property.
     * @param argument the key given on the command line, or null
     * @param name the name to read
     * @return the key, or null when there is none
     */
    public static String findResourceKey(String argument, String name) {
        if (isInvalidKey(argument) == false) {
            return argument.trim();
        }
        String value = readKey(name);
        return value == null ? null : value.trim();
    }

    /**
     * Writes what a cloud example needs when it has no resource key, being
     * the variable to set and the page where a key is created, so that the
     * example can end without an error.
     * @param output where to write
     * @param example the example's tag, as its Find out more section has it
     * @param variable the variable the example reads its key from
     * @param configuration the configuration page for the key, such as
     *                      {@link #FREE_PROPERTIES_CONFIGURATION}
     */
    public static void writeNoResourceKey(PrintStream output, String example,
                                          String variable,
                                          String configuration) {
        output.println();
        output.println("This example calls the 51Degrees cloud service, " +
            "which needs a resource key, and none is set.");
        output.println("Set the " + variable + " environment variable to a " +
            "resource key, or give the key as the first argument, and run " +
            "the example again.");
        output.println("Create a resource key with the properties this " +
            "example uses at " +
            FindOutMore.link(configuration, example, "no-resource-key"));
        if (PAID_PROPERTIES_CONFIGURATION.equals(configuration)) {
            output.println("These properties need a paid licence. Contact " +
                "us about one at " +
                FindOutMore.link("contact-us", example, "no-resource-key"));
        }
        output.flush();
    }

    /**
     * The resource key for a test that calls the live cloud service, from
     * {@link #TEST_RESOURCE_KEY}. Skips the test, naming the variable, when
     * there is none, because a cloud test that passes without a key has
     * called nothing.
     * @param asksFor what the test asks the service for
     * @param products the products the key has to carry
     * @return the resource key
     */
    public static String requireTestResourceKey(String asksFor,
                                                String... products) {
        return requireResourceKey(TEST_RESOURCE_KEY, asksFor, products);
    }

    /**
     * The resource key for a test that calls the live cloud service, from
     * the given name only. Skips the test, naming it, when there is none.
     * @param exampleName the name the example reads
     * @param asksFor what the test asks the service for
     * @param products the products the key has to carry
     * @return the resource key
     */
    public static String requireResourceKey(String exampleName,
                                            String asksFor,
                                            String... products) {
        return requireResourceKey(KeyHelper::readKey, exampleName, asksFor,
            products);
    }

    static String requireResourceKey(Function<String, String> read,
                                     String exampleName,
                                     String asksFor,
                                     String... products) {
        String value = read.apply(exampleName);
        if (isInvalidKey(value) == false) {
            return value.trim();
        }
        throw skipTest(noResourceKey(exampleName, asksFor, products));
    }

    /**
     * The exception that skips a test, being JUnit's
     * AssumptionViolatedException. It is made by name, because the
     * examples use KeyHelper too, and run with no test library on their
     * class path. Where JUnit cannot be found, the exception is an
     * IllegalStateException with the same message.
     * @param message why the test is skipped
     * @return the exception to throw
     */
    static RuntimeException skipTest(String message) {
        try {
            return (RuntimeException) Class.forName(
                    "org.junit.AssumptionViolatedException")
                .getConstructor(String.class)
                .newInstance(message);
        } catch (ReflectiveOperationException | LinkageError |
                 ClassCastException e) {
            return new IllegalStateException(message);
        }
    }

    static String noResourceKey(String name, String asksFor,
                                String... products) {
        return "This test calls the live 51Degrees cloud service, so it " +
            "needs a resource key, and it is skipped because " +
            name + " is not set as an environment " +
            "variable or as a system property. " +
            "The key has to carry the " + productList(products) +
            ", because the test asks the service for " + asksFor + ". To " +
            "see the products a key carries, open " +
            "https://cloud.51degrees.com/api/v4/accessibleproperties?resource=<key>. " +
            "Create a key at " + CREATE_KEY_URL + ".";
    }

    /**
     * The message for a value the cloud answered without.
     * @param thing the property or element that has no value
     * @param reason the reason the cloud gave, or null
     * @param products the products a key needs for the value
     * @return the message
     */
    public static String answeredWithout(String thing, String reason,
                                         String... products) {
        return "The cloud answered without " + thing + " (" +
            (reason == null || reason.trim().isEmpty() ?
                "no reason given" : reason.trim()) +
            "), which is what a key that does not carry the " +
            productList(products) + " gets.";
    }

    private static String productList(String... products) {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < products.length; i++) {
            if (i > 0) {
                list.append(i == products.length - 1 ? " and " : ", ");
            }
            list.append(products[i]);
        }
        return list + (products.length == 1 ? " product" : " products");
    }

    /**
     * A key read as an environment variable, and then as a system property
     * of the same name, skipping any that is not a key.
     */
    private static String readKey(String name) {
        return readKey(name, System::getenv);
    }

    static String readKey(String name,
                          Function<String, String> environment) {
        for (String value : new String[]{
            environment.apply(name),
            System.getProperty(name)}) {
            if (isInvalidKey(value) == false) {
                return value;
            }
        }
        return null;
    }

    /**
     * Obtain a resource key either from environment variable or from a property.
     */
    public static String getOrSetTestResourceKey() {
        return getOrSetTestResourceKey(true);
    }

    public static String getOrSetTestResourceKey(boolean shouldThrow) {
        return getOrSetTestResourceKey(null, shouldThrow);
    }

    public static String getOrSetTestResourceKey(String value, boolean shouldThrow) {
        return getOrSetResourceKey(value, TEST_RESOURCE_KEY,
            "A free resource key configured with the " +
                "properties required by this example may be obtained from " +
                "https://51degrees.com/configure/jqz435Nc?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-devicedetection-examples-shared-keyhelper.java&utm_term=resource-key-required ",
            shouldThrow);
    }
    public static String getOrSetTestResourceKey(String value) {
        return getOrSetTestResourceKey(value, true);
    }
    /**
     * Obtain a resource key from the passed argument,
     * from environment variable or from a property. Store
     * as System Property TEST_RESOURCE_KEY
     */
    public static String getOrSetResourceKey(String value, String variableName,
                                             String errorMessage,
                                             boolean shouldThrow) {
        if (Objects.isNull(value)) {
            value = getNamedKey(variableName);

        }
        if (isInvalidKey(value)) {
            logger.error("\nTo access Cloud Services you must supply a " +
                    "\"ResourceKey\" in one of the following ways: \n - in the " +
                    "configuration file of an example,\n - as a command line parameter of a " +
                    "runnable example,\n - as an Environment Variable named \"\u001B[36m{}\u001B[0m\"," +
                    "\n - as a System Property named \"\u001B[36m{}\u001B[0m\").", variableName,
                    variableName);
            logger.error(errorMessage);
            if (shouldThrow) {
                throw new IllegalStateException("\"" + value + "\" is not a valid resource key");
            }
        }

        // Keep a valid key for next time. System.setProperty throws on null,
        // and an invalid key is not worth keeping.
        if (isInvalidKey(value) == false) {
            System.setProperty(variableName, value);
        }
        return value;
    }
    public static String getOrSetSuperResourceKey(String value, String variablename) {
        return getOrSetSuperResourceKey(value, variablename, true);
    }
    public static String getOrSetSuperResourceKey(String value, String variablename, boolean shouldThrow) {
        return getOrSetResourceKey(value, variablename, "TAC lookup and Native Model are not " +
                "available as a free service.\nThis means " +
                "that you will first need a license key, which can be purchased " +
                "from our pricing page: https://51degrees.com/pricing?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-devicedetection-examples-shared-keyhelper.java&utm_term=license-key-required. \nOnce this is " +
                "done, a resource key with the properties required by this example " +
                "can be created at https://51degrees.com/configure/QKyYH5XT?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-devicedetection-examples-shared-keyhelper.java&utm_term=license-key-required. ",
            shouldThrow);
    }
}
