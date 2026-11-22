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
 * The tests run once for each resource key in {@link #PASSES}. Every name
 * is read as an environment variable first and then as a system property
 * of the same name. Each key is read under its one _51DEGREES_ name and no
 * other. A pass with no resource key is skipped, naming the variable to
 * set, rather than passed having called nothing.
 */
final class LiveCloudKeys {

    /**
     * The resource key and the licence key of each pass of the tests. A
     * sealed verdict is redeemed only with the licence key of the account
     * the resource key belongs to, so the 51Did resource key runs with the
     * 51Did licence key, and the Bespoke resource key runs with none. The
     * licence key is not _51DEGREES_LICENSE_KEY, which holds the licence
     * for the data files.
     */
    static final String[][] PASSES = {
        {"_51DEGREES_RESOURCE_KEY_51DID", "_51DEGREES_LICENSE_KEY_51DID"},
        {"_51DEGREES_RESOURCE_KEY_BESPOKE", null},
    };

    private LiveCloudKeys() {
    }

    /**
     * The trimmed value of the environment variable of this name, or of
     * the system property where that is not set or is blank, or null when
     * neither holds a value or there is no name.
     */
    static String read(String name) {
        if (name == null) {
            return null;
        }
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(name);
        }
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /**
     * Why a pass is skipped when its resource key is not set, naming the
     * variable to set and what the key has to be entitled to.
     */
    static String noResourceKey(String name) {
        return "These tests call the live 51Degrees cloud service, so they "
            + "need a resource key, and they are skipped because "
            + name + " is not set as an "
            + "environment variable or as a system property. "
            + "The key has to carry the fodid product, because "
            + "these tests ask the cloud for FODiD.IdProbGlobal and "
            + "FODiD.IdProbLic. Create a key at "
            + "https://51degrees.com/configure.";
    }

    /**
     * What to fail with when a licence key is set and the resource key of
     * its account is not. The tests that redeem with that licence key
     * would otherwise never run, and nothing would say so.
     */
    static String missingResourceKey(String name, String licenceName) {
        return name + " is not set, but " + licenceName + ", the license "
            + "key of the same account, is. Set " + name + " to a resource "
            + "key of that account, so the tests that redeem a sealed "
            + "result run.";
    }

    /**
     * Why a test that redeems a sealed result is skipped when the pass has
     * no licence key.
     * @param licenceName the name the licence key of the pass is read
     *                    from, or null when the pass runs with none
     */
    static String noLicenceKey(String licenceName) {
        return licenceName == null
            ? "This resource key runs with no license key, so there is no "
                + "sealed result it can redeem."
            : "Set " + licenceName + " to redeem a sealed result.";
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
