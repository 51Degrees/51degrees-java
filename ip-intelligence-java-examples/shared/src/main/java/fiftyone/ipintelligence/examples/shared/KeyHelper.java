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

public class KeyHelper {
    /**
     * Aligned name of the environment variable or system property which may
     * hold the resource key. This name is checked first, before the legacy
     * {@link #TEST_RESOURCE_KEY} name.
     */
    public static final String RESOURCE_KEY_ENV_VAR = "51DEGREES_RESOURCE_KEY";
    public static final String TEST_RESOURCE_KEY = "TestResourceKey";
    static Logger logger = LoggerFactory.getLogger(KeyHelper.class);

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
                "https://configure.51degrees.com/Wkqxf3Bs?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=shared-src-main-java-fiftyone-ipintelligence-examples-shared-keyhelper.java&utm_term=resource-key-required. A free key " +
                "populates the free tier properties only. See " +
                "https://51degrees.com/pricing?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=shared-src-main-java-fiftyone-ipintelligence-examples-shared-keyhelper.java&utm_term=resource-key-required to get a paid subscription " +
                "with more properties.",
            shouldThrow);
    }
    public static String getOrSetTestResourceKey(String value) {
        return getOrSetTestResourceKey(value, true);
    }
    /**
     * Obtain a resource key from the passed argument,
     * from environment variable or from a property. The aligned
     * {@link #RESOURCE_KEY_ENV_VAR} name is checked first, then the
     * passed variable name. Store as System Property under the
     * passed variable name.
     */
    public static String getOrSetResourceKey(String value, String variableName,
                                             String errorMessage,
                                             boolean shouldThrow) {
        if (Objects.isNull(value)) {
            // check the aligned name first, then the legacy name
            value = KeyUtils.getNamedKey(RESOURCE_KEY_ENV_VAR);
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
                    RESOURCE_KEY_ENV_VAR, RESOURCE_KEY_ENV_VAR, variableName);
            logger.error(errorMessage);
            if (shouldThrow) {
                throw new IllegalStateException("\"" + value + "\" is not a valid resource key");
            }
        }

        // capture the passed parameter for next time called
        System.setProperty(variableName, value);
        return value;
    }
    public static String getOrSetSuperResourceKey(String value, String variablename) {
        return getOrSetSuperResourceKey(value, variablename, true);
    }
    public static String getOrSetSuperResourceKey(String value, String variablename, boolean shouldThrow) {
        return getOrSetResourceKey(value, variablename, "TAC lookup and native model are not " +
                "available with a free resource key. " +
                "See https://51degrees.com/pricing?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=shared-src-main-java-fiftyone-ipintelligence-examples-shared-keyhelper.java&utm_term=super-resource-key-required to get a paid subscription " +
                "with more properties. " +
                "Once subscribed, a resource key with the properties required " +
                "by this example can be created at " +
                "https://configure.51degrees.com/hYzn3TV3?utm_source=code&utm_medium=example&utm_campaign=ip-intelligence-java-examples&utm_content=shared-src-main-java-fiftyone-ipintelligence-examples-shared-keyhelper.java&utm_term=super-resource-key-required.",
            shouldThrow);
    }
}
