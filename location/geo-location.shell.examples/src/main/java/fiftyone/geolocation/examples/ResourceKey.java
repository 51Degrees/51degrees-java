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

package fiftyone.geolocation.examples;

/**
 * Finds the resource key a Location example runs with.
 */
public final class ResourceKey {

    /**
     * The variable the examples read their resource key from.
     */
    public static final String VARIABLE = "_51DEGREES_RESOURCE_KEY";

    private ResourceKey() {
    }

    /**
     * The key given as the first argument, or else the key in
     * {@link #VARIABLE}, read as an environment variable and then as a
     * system property.
     * @param args the example's arguments
     * @return the key, or null when there is none
     */
    public static String find(String[] args) {
        for (String value : new String[]{
            args.length > 0 ? args[0] : null,
            System.getenv(VARIABLE),
            System.getProperty(VARIABLE)}) {
            if (value != null && value.trim().isEmpty() == false) {
                return value.trim();
            }
        }
        return null;
    }
}
