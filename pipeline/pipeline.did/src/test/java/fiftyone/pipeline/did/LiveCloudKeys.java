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

package fiftyone.pipeline.did;


/**
 * Where the tests that call the live 51Degrees cloud service read their
 * resource key and licence key from, and what they say when there is
 * none.
 * <p>
 * Every name is tried as an environment variable first and then as a
 * system property, so a developer can export one and a build can pass one
 * on the command line with {@code -D}. Each key is read under its one
 * _51DEGREES_ name and no other. A test with no key is skipped, naming
 * the variable to set, rather than passed having called nothing.
 */
final class LiveCloudKeys {

    /**
     * The name the resource key is read from. The workflow that runs these
     * tests once for every key the organisation holds sets this single
     * name to each key in turn.
     */
    static final String RESOURCE_KEY_NAME = "_51DEGREES_RESOURCE_KEY";

    /**
     * The name the license key is read from. A license key is optional,
     * because an account can hold its products on the resource key alone.
     * It is the 51Did license, which redeems a sealed verdict, and not
     * _51DEGREES_LICENSE_KEY, which holds the license for the data files.
     */
    static final String LICENCE_KEY_NAME = "_51DEGREES_LICENSE_KEY_51DID";

    private LiveCloudKeys() {
    }

    /**
     * The trimmed value of the environment variable of this name, or of
     * the system property where that is not set or is blank, or null when
     * neither holds a value.
     */
    static String find(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(name);
        }
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /**
     * Why the tests are skipped when no resource key is set, naming the
     * variable to set and what the key has to be entitled to.
     */
    static String noResourceKey() {
        return "These tests call the live 51Degrees cloud service, so they "
            + "need a resource key, and they are skipped because "
            + RESOURCE_KEY_NAME + " is not set as an "
            + "environment variable or as a system property. "
            + "The key has to carry the fodid product, because "
            + "these tests ask the cloud for FODiD.IdProbGlobal and "
            + "FODiD.IdProbLic. Create a key at "
            + "https://51degrees.com/configure.";
    }

    /**
     * What to fail with when the cloud answered but returned no 51Did,
     * which is the resource key not carrying the product rather than the
     * service being broken.
     */
    static String notEntitled(String detail) {
        return "The live 51Degrees cloud service answered, but returned no "
            + "51Did for " + detail + ". That is the resource key not "
            + "carrying the fodid product rather than a fault in the "
            + "service or in this package. Use a key entitled to fodid, "
            + "from https://51degrees.com/configure.";
    }
}
