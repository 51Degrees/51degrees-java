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

package pipeline.developerexamples.cloudengine;

/**
 * The resource key the tests run the example with. The tests read the key
 * with every property from _51DEGREES_RESOURCE_KEY_BESPOKE, and give it to
 * the example as the system property of the name the example reads.
 */
final class ExampleKey {

    /**
     * The name the tests read their resource key from.
     */
    static final String NAME = "_51DEGREES_RESOURCE_KEY_BESPOKE";

    /**
     * What runs with the key set.
     */
    interface Action {
        void run() throws Exception;
    }

    private ExampleKey() {
    }

    /**
     * The key, read as an environment variable and then as a system
     * property, or null when there is none.
     */
    static String find() {
        return Main.env(NAME);
    }

    /**
     * Runs the action with the key set as the system property the example
     * reads, and puts the property back afterwards. When the action fails,
     * every cause is printed before the failure is passed on, as the test
     * report shortens the chain.
     */
    static void runWith(String key, Action action) throws Exception {
        String saved = System.getProperty(Main.RESOURCE_KEY_NAME);
        System.setProperty(Main.RESOURCE_KEY_NAME, key);
        try {
            action.run();
        } catch (Exception e) {
            printCauses(e);
            throw e;
        } finally {
            if (saved == null) {
                System.clearProperty(Main.RESOURCE_KEY_NAME);
            } else {
                System.setProperty(Main.RESOURCE_KEY_NAME, saved);
            }
        }
    }

    /**
     * Prints the failure and each cause under it, one to a line.
     */
    private static void printCauses(Throwable failure) {
        String indent = "";
        for (Throwable cause = failure; cause != null;
            cause = cause.getCause()) {
            System.err.println(indent + cause);
            indent = "  caused by ";
            if (cause.getCause() == cause) {
                break;
            }
        }
    }
}
