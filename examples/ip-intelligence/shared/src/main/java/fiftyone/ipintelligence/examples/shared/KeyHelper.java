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

package fiftyone.ipintelligence.examples.shared;

import fiftyone.ipintelligence.shared.testhelpers.KeyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class KeyHelper {
    /**
     * Aligned name of the environment variable or system property which may
     * hold the resource key. This name is checked first, before the legacy
     * {@link #TEST_RESOURCE_KEY} name.
     */
    public static final String RESOURCE_KEY_VARIABLE = "_51DEGREES_RESOURCE_KEY";

    /**
     * The old name of {@link #RESOURCE_KEY_VARIABLE}, read only when that
     * one is not set.
     * @deprecated use {@link #RESOURCE_KEY_VARIABLE}, and set the variable
     * it names in place of this one.
     */
    @Deprecated
    public static final String RESOURCE_KEY_ENV_VAR = "51DEGREES_RESOURCE_KEY";

    // Set once the use of the deprecated name has been logged.
    private static final AtomicBoolean OLD_NAME_LOGGED =
        new AtomicBoolean(false);
    public static final String TEST_RESOURCE_KEY = "TestResourceKey";
    static Logger logger = LoggerFactory.getLogger(KeyHelper.class);

    /**
     * The names every cloud test reads a resource key from first, before
     * the names its own example reads.
     */
    public static final List<String> SHARED_RESOURCE_KEY_NAMES =
        Collections.unmodifiableList(Arrays.asList(
            "_51DEGREES_RESOURCE_KEY",
            "_51DEGREES_RESOURCE_KEY_BESPOKE"));

    static final String CREATE_KEY_URL =
        "https://configure.51degrees.com/?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-ipintelligence-examples-shared-keyhelper.java&utm_term=no-resource-key";

    /**
     * The resource key for a test that calls the live cloud service, from
     * {@link #SHARED_RESOURCE_KEY_NAMES} and then the names
     * {@link #getOrSetTestResourceKey()} reads. Fails the test when there
     * is none, because a cloud test that passes without a key has called
     * nothing.
     * @param asksFor what the test asks the service for
     * @param products the products the key has to carry
     * @return the resource key
     */
    public static String requireTestResourceKey(String asksFor,
                                                String... products) {
        return requireResourceKey(KeyHelper::readKey, Arrays.asList(
            RESOURCE_KEY_VARIABLE, TEST_RESOURCE_KEY), asksFor, products);
    }

    static String requireResourceKey(Function<String, String> read,
                                     List<String> exampleNames,
                                     String asksFor,
                                     String... products) {
        List<String> names = new ArrayList<>(SHARED_RESOURCE_KEY_NAMES);
        names.addAll(exampleNames);
        for (String name : names) {
            String value = read.apply(name);
            if (KeyUtils.isInvalidKey(value)
                && RESOURCE_KEY_VARIABLE.equals(name)) {
                value = oldNameResourceKey(read);
            }
            if (KeyUtils.isInvalidKey(value) == false) {
                return value.trim();
            }
        }
        throw new AssertionError(noResourceKey(names, asksFor, products));
    }

    /**
     * The key held under the deprecated {@link #RESOURCE_KEY_ENV_VAR}, or
     * null when there is none. It is read only when
     * {@link #RESOURCE_KEY_VARIABLE} holds no key, and its first use is
     * logged as a warning.
     */
    private static String oldNameResourceKey(Function<String, String> read) {
        String value = read.apply(RESOURCE_KEY_ENV_VAR);
        if (KeyUtils.isInvalidKey(value)) {
            return null;
        }
        if (OLD_NAME_LOGGED.compareAndSet(false, true)) {
            logger.warn(
                "The environment variable " + RESOURCE_KEY_ENV_VAR
                + " is deprecated and " + RESOURCE_KEY_VARIABLE
                + " replaces it. Set " + RESOURCE_KEY_VARIABLE
                + " to the same value, as a later release may no longer "
                + "read " + RESOURCE_KEY_ENV_VAR + ".");
        }
        return value;
    }

    static String noResourceKey(List<String> names, String asksFor,
                                String... products) {
        return "This test calls the live 51Degrees cloud service, so it " +
            "needs a resource key and there is nothing to fall back on. " +
            "None of " + String.join(", ", names) + " is set, as an " +
            "environment variable or as a system property, tried in that " +
            "order. The key has to carry the " + productList(products) +
            ", because the test asks the service for " + asksFor + ". To " +
            "see the products a key carries, open " +
            "https://cloud.51degrees.com/api/v4/accessibleproperties?resource=<key>. " +
            "Create a key at " + CREATE_KEY_URL + ".";
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
     * A key read as an environment variable, as its upper case name, and
     * then as a system property, skipping any that is not a key, such as
     * the empty string the example poms pass for a key that is not set.
     */
    private static String readKey(String name) {
        for (String value : new String[]{
            System.getenv(name),
            System.getenv(name.toUpperCase(Locale.ROOT)),
            System.getProperty(name)}) {
            if (KeyUtils.isInvalidKey(value) == false) {
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
            "A free resource key may be obtained from " +
                "https://configure.51degrees.com/Wkqxf3Bs?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-ipintelligence-examples-shared-keyhelper.java&utm_term=resource-key-required. A free key " +
                "populates the free tier properties only. See " +
                "https://51degrees.com/pricing?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-ipintelligence-examples-shared-keyhelper.java&utm_term=resource-key-required to get a paid subscription " +
                "with more properties.",
            shouldThrow);
    }
    public static String getOrSetTestResourceKey(String value) {
        return getOrSetTestResourceKey(value, true);
    }
    /**
     * Obtain a resource key from the passed argument,
     * from environment variable or from a property. The aligned
     * {@link #RESOURCE_KEY_VARIABLE} name is checked first, then the
     * deprecated {@link #RESOURCE_KEY_ENV_VAR}, then the passed variable
     * name. Store as System Property under the passed variable name.
     */
    public static String getOrSetResourceKey(String value, String variableName,
                                             String errorMessage,
                                             boolean shouldThrow) {
        if (Objects.isNull(value)) {
            // check the aligned name first, then the legacy name
            value = KeyUtils.getNamedKey(RESOURCE_KEY_VARIABLE);
            if (Objects.isNull(value) || value.isEmpty()) {
                String oldNameKey = oldNameResourceKey(KeyUtils::getNamedKey);
                if (Objects.nonNull(oldNameKey)) {
                    value = oldNameKey;
                }
            }
            if (Objects.isNull(value)) {
                value = KeyUtils.getNamedKey(variableName);
            }
        }
        if (KeyUtils.isInvalidKey(value)) {
            logger.error("\nTo access Cloud Services you must supply a " +
                    "\"ResourceKey\" in one of the following ways: \n - in the " +
                    "configuration file of an example,\n - as a command line parameter of a " +
                    "runnable example,\n - as an Environment Variable named \"\u001B[36m{}\u001B[0m\"," +
                    "\n - as a System Property named \"\u001B[36m{}\u001B[0m\" " +
                    "(the legacy name \"\u001B[36m{}\u001B[0m\" is also still accepted).",
                    RESOURCE_KEY_VARIABLE, RESOURCE_KEY_VARIABLE, variableName);
            logger.error(errorMessage);
            if (shouldThrow) {
                throw new IllegalStateException("\"" + value + "\" is not a valid resource key");
            }
        }

        // Keep a valid key for next time. System.setProperty throws on null,
        // and an invalid key is not worth keeping.
        if (KeyUtils.isInvalidKey(value) == false) {
            System.setProperty(variableName, value);
        }
        return value;
    }
    public static String getOrSetSuperResourceKey(String value, String variablename) {
        return getOrSetSuperResourceKey(value, variablename, true);
    }
    public static String getOrSetSuperResourceKey(String value, String variablename, boolean shouldThrow) {
        return getOrSetResourceKey(value, variablename, "TAC lookup and native model are not " +
                "available with a free resource key. " +
                "See https://51degrees.com/pricing?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-ipintelligence-examples-shared-keyhelper.java&utm_term=super-resource-key-required to get a paid subscription " +
                "with more properties. " +
                "Once subscribed, a resource key with the properties required " +
                "by this example can be created at " +
                "https://configure.51degrees.com/hYzn3TV3?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=shared-src-main-java-fiftyone-ipintelligence-examples-shared-keyhelper.java&utm_term=super-resource-key-required.",
            shouldThrow);
    }
}
