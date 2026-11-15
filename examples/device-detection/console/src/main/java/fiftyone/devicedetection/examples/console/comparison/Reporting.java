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

package fiftyone.devicedetection.examples.console.comparison;

import fiftyone.devicedetection.examples.shared.DataFileHelper;

import java.io.PrintWriter;
import java.util.List;

/**
 * Interface proves ability to compare outcomes of detection
 */
public interface Reporting {
    /**
     * Report on the results of benchmarks
     * @param executions a list of executions carried out
     */
    void report(List<Comparer.ExecutionResult> executions, PrintWriter writer);

    /**
     * report on overall execution time and that of the threads
     */

    class Minimal implements Reporting {
        /**
         * Where to ask for a copy of the Enterprise data file, tagged with
         * this example's file so that an enquiry can be traced to it.
         */
        static final String CONTACT_US = "https://51degrees.com/contact-us?utm_source=code&utm_medium=example&utm_campaign=51degrees-java&utm_content=console-src-main-java-fiftyone-devicedetection-examples-console-comparison-comparer.java&utm_term=enterprise-data-file";

         public void report(List<Comparer.ExecutionResult> executions, PrintWriter writer){
            boolean needsEnterprise = false;
            for (Comparer.ExecutionResult execution: executions) {
                writer.format("Vendor: %s, %,d millis\n", execution.solutionId, execution.elapsedMillis);
                for (Detection.BenchmarkResult benchmark: execution.benchmarkResults) {
                    writer.format("Thread: %,d millis. Count: %,d\n",
                            benchmark.elapsedMillis, benchmark.count);
                }
                needsEnterprise |= writeFirstDetection(execution, writer);
            }
            // The free Lite data file has fewer properties than the
            // Enterprise data file, so say where to ask for a copy, once
            if (needsEnterprise) {
                DataFileHelper.writeEnterpriseNote(writer, CONTACT_US);
            }
            writer.flush();
        }

        /**
         * Writes the properties of the first detection a solution made, so
         * that the solutions can be compared by what they found as well as
         * by time.
         * @return true if a property needs the Enterprise data file
         */
        private static boolean writeFirstDetection(
                Comparer.ExecutionResult execution, PrintWriter writer) {
            for (Detection.BenchmarkResult benchmark : execution.benchmarkResults) {
                if (benchmark.properties.isEmpty() == false) {
                    Detection.Properties first = benchmark.properties.get(0);
                    writer.println("First detection:");
                    String[][] values = {
                            {"Is Mobile", first.isMobile()},
                            {"Hardware Vendor", first.getHardwareVendor()},
                            {"Hardware Model", first.getHardwareModel()},
                            {"Device Type", first.getDeviceType()},
                            {"Browser Vendor", first.getBrowserVendor()},
                            {"Browser Version", first.getBrowserVersion()}};
                    boolean needsEnterprise = false;
                    for (String[] value : values) {
                        writer.println("\t" + value[0] + ": " + value[1]);
                        needsEnterprise |= DataFileHelper.NEEDS_ENTERPRISE.equals(value[1]);
                    }
                    return needsEnterprise;
                }
            }
            return false;
        }
    }
}
