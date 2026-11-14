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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helpers to obtain keys from the environment
 */
public class KeyUtils {
    /**
     * Name of the aligned environment variable or system property which
     * supplies the resource key. This name is checked before any legacy name.
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

    // Set once the use of the deprecated name has been reported.
    private static final AtomicBoolean OLD_NAME_REPORTED =
        new AtomicBoolean(false);

    /**
     * Obtain a resource key from an environment variable or a system property.
     * <p>
     * The aligned name {@link #RESOURCE_KEY_VARIABLE} is checked first. When
     * it is not set or is empty the deprecated {@link #RESOURCE_KEY_ENV_VAR}
     * is checked, and its first use is reported on standard error. The
     * supplied legacy name is checked last. Each name is looked up using
     * {@link #getNamedKey(String)}.
     * @param legacyKeyName legacy name to try when the aligned name is not set
     * @return the resource key, or null if no name is set
     */
    public static String getResourceKey(String legacyKeyName) {
        String resourceKey = getNamedKey(RESOURCE_KEY_VARIABLE);
        if (Objects.isNull(resourceKey) || resourceKey.isEmpty()) {
            String oldNameKey = getNamedKey(RESOURCE_KEY_ENV_VAR);
            if (Objects.nonNull(oldNameKey) && oldNameKey.isEmpty() == false) {
                if (OLD_NAME_REPORTED.compareAndSet(false, true)) {
                    System.err.println(
                        "The environment variable " + RESOURCE_KEY_ENV_VAR
                        + " is deprecated and " + RESOURCE_KEY_VARIABLE
                        + " replaces it. Set " + RESOURCE_KEY_VARIABLE
                        + " to the same value, as a later release may no longer "
                        + "read " + RESOURCE_KEY_ENV_VAR + ".");
                }
                return oldNameKey;
            }
        }
        if (Objects.isNull(resourceKey)) {
            resourceKey = getNamedKey(legacyKeyName);
        }
        return resourceKey;
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
