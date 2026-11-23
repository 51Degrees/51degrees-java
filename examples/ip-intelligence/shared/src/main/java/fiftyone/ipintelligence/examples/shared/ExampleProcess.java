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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs an example's main method in a new Java process with no key in its
 * environment, as someone runs it from a fresh clone, for the tests of
 * what an example does with no settings. A new process is needed because
 * a running Java process cannot remove a variable from its own
 * environment.
 */
public final class ExampleProcess {

    /**
     * How long an example may run before the test stops it.
     */
    private static final long TIMEOUT_SECONDS = 120;

    /**
     * What an example wrote and how it ended.
     */
    public static final class Result {
        /**
         * The process's exit code.
         */
        public final int exitCode;

        /**
         * Everything the process wrote to standard output and standard
         * error.
         */
        public final String output;

        Result(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    private ExampleProcess() {
    }

    /**
     * Runs the example's main method with no arguments and with every
     * environment variable whose name holds "KEY", in any case, left out.
     * The process takes the class path of the process that starts it, and
     * of its system properties only _51DEGREES_PROJECT_ROOT, which says where the
     * files the example reads are.
     * @param example the class whose main method runs
     * @return the exit code and the output
     * @throws Exception if the process cannot start, or does not end
     *                   within the timeout
     */
    public static Result runWithoutKeys(Class<?> example) throws Exception {
        return runWith(example, Collections.<String, String>emptyMap());
    }

    /**
     * Runs the example's main method as {@link #runWithoutKeys(Class)}
     * does, then with each of these environment variables set and with
     * these arguments, as a person who has a variable set and gives the
     * example an argument runs it.
     * @param example the class whose main method runs
     * @param environment the environment variables to set, by name
     * @param arguments the arguments the example is given
     * @return the exit code and the output
     * @throws Exception if the process cannot start, or does not end
     *                   within the timeout
     */
    public static Result runWith(Class<?> example,
                                 Map<String, String> environment,
                                 String... arguments) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(new File(new File(System.getProperty("java.home"),
            "bin"), "java").getPath());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        String projectRoot = System.getProperty("_51DEGREES_PROJECT_ROOT");
        if (projectRoot != null) {
            command.add("-D_51DEGREES_PROJECT_ROOT=" + projectRoot);
        }
        command.add(example.getName());
        command.addAll(Arrays.asList(arguments));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().keySet().removeIf(
            name -> name.toUpperCase(Locale.ROOT).contains("KEY"));
        builder.environment().putAll(environment);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thread reader = new Thread(() -> copy(process.getInputStream(),
            output));
        reader.start();
        if (process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS) == false) {
            process.destroyForcibly();
            reader.join();
            throw new AssertionError(example.getName() +
                " did not end within " + TIMEOUT_SECONDS + " seconds. " +
                "It wrote:\n" + output.toString("UTF-8"));
        }
        reader.join();
        return new Result(process.exitValue(), output.toString("UTF-8"));
    }

    private static void copy(InputStream input, ByteArrayOutputStream output) {
        byte[] buffer = new byte[8192];
        try {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
        } catch (IOException e) {
            // The process ended, so there is no more to read.
        }
    }
}
