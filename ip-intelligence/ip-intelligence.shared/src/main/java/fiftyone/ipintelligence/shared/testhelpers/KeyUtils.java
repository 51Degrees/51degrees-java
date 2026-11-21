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

package fiftyone.ipintelligence.shared.testhelpers;

import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

/**
 * Helpers to obtain keys from the environment
 */
public class KeyUtils {
    /**
     * Name of the environment variable or system property which supplies
     * the resource key. It is the only name read.
     */
    public static final String RESOURCE_KEY_VARIABLE = "_51DEGREES_RESOURCE_KEY";

    /**
     * The old name of {@link #RESOURCE_KEY_VARIABLE}. It is no longer read.
     * @deprecated use {@link #RESOURCE_KEY_VARIABLE}, and set the variable
     * it names in place of this one.
     */
    @Deprecated
    public static final String RESOURCE_KEY_ENV_VAR = "51DEGREES_RESOURCE_KEY";

    /**
     * Obtain the resource key from the environment variable or system
     * property {@link #RESOURCE_KEY_VARIABLE}, looked up using
     * {@link #getNamedKey(String)}.
     * @return the resource key, or null if it is not set
     */
    public static String getResourceKey() {
        return getNamedKey(RESOURCE_KEY_VARIABLE);
    }

    /**
     * Obtain the resource key from {@link #RESOURCE_KEY_VARIABLE}. The name
     * passed is no longer read.
     * @param legacyKeyName an old name, which is not read
     * @return the resource key, or null if it is not set
     * @deprecated use {@link #getResourceKey()}
     */
    @Deprecated
    public static String getResourceKey(String legacyKeyName) {
        return getResourceKey();
    }

    /**
     * Obtain a key either from environment variable or from a property.
     * <p>
     * Try resource key as env var, then as upper case env var, the system property
     */
    public static String getNamedKey(String keyName) {
        String resourceKey = System.getenv(keyName);
        if (Objects.isNull(resourceKey)) {
            resourceKey = System.getenv(keyName.toUpperCase(Locale.ROOT));
            if (Objects.isNull(resourceKey)) {
                resourceKey = System.getProperty(keyName);
            }
        }
        return resourceKey;
    }

    /**
     * Evaluate whether a key might be valid
     * @param keyValue value to test
     * @return boolean
     */
    public static boolean isInvalidKey(String keyValue){
        Base64.Decoder decoder = Base64.getUrlDecoder();
        try {
            return Objects.isNull(keyValue) ||
                    keyValue.trim().length() < 19 ||
                    decoder.decode(keyValue).length < 14;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
}
