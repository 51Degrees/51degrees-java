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

package pipeline.developerexamples.fodid;

/**
 * Prints the resource key and the licence key the example read from the
 * environment of the process. {@link DeprecatedKeyNameTests} runs this in
 * a Java process of its own, because the example reads both keys once,
 * when its class loads, and a process cannot change its own environment
 * variables.
 */
public final class KeyProbe {

    static final String RESOURCE_PREFIX = "RESOURCE ";
    static final String LICENCE_PREFIX = "LICENCE ";

    private KeyProbe() {
    }

    public static void main(String[] args) {
        System.out.println(
            RESOURCE_PREFIX + CreatorContextDemoServer.RESOURCE);
        System.out.println(
            LICENCE_PREFIX + CreatorContextDemoServer.LICENCE);
        System.out.flush();
        System.exit(0);
    }
}
