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

package fiftyone.pipeline.cloudrequestengine.flowelements;

/**
 * The root address a test should expect when it gives the engine no
 * address in code or configuration. That is the
 * {@link Constants#CLOUD_ENDPOINT_VARIABLE} environment variable when it
 * is set and not empty, otherwise {@link Constants#END_POINT_DEFAULT},
 * ending in "/" as the builder makes it.
 */
final class ExpectedEndpoint {

    private ExpectedEndpoint() {
    }

    /**
     * The root address for the environment this test runs in.
     * @return the address, ending in "/"
     */
    static String root() {
        return root(System.getenv(Constants.CLOUD_ENDPOINT_VARIABLE));
    }

    /**
     * The root address for a given value of the environment variable.
     * @param variable the value of the variable, or null when it is unset
     * @return the address, ending in "/"
     */
    static String root(String variable) {
        String root = variable == null || variable.isEmpty()
            ? Constants.END_POINT_DEFAULT
            : variable;
        return root.endsWith("/") ? root : root + "/";
    }
}
