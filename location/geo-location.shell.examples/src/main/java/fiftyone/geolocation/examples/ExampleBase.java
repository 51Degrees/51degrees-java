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

import java.io.PrintWriter;
import java.io.StringWriter;

/** Output helpers the examples share. */
public class ExampleBase {

    private final boolean printOutput;

    /**
     * Create a new instance.
     *
     * @param printOutput false to print nothing, for example in tests
     */
    public ExampleBase(boolean printOutput) {
        this.printOutput = printOutput;
    }

    private static void addToMessage(StringBuilder message, String textToAdd, int depth) {
        for (int i = 0; i < depth; i++) {
            message.append("   ");
        }
        message.append(textToAdd);
        message.append("\n");
    }

    /**
     * Print text when output is on.
     *
     * @param string the text
     */
    protected void print(String string) {
        if (printOutput) {
            System.out.print(string);
        }
    }

    /**
     * Print a line when output is on.
     *
     * @param string the line
     */
    protected void println(String string) {
        if (printOutput) {
            System.out.println(string);
        }
    }

    /**
     * Print formatted text when output is on.
     *
     * @param format the format string
     * @param args the values to format
     */
    protected void printf(String format, Object... args) {
        if (printOutput) {
            System.out.printf(format, args);
        }
    }

    /**
     * Print an exception and its causes when output is on.
     *
     * @param ex the exception
     * @param depth how far to indent the output
     */
    protected void outputException(Throwable ex, int depth) {
        StringBuilder message = new StringBuilder();
        addToMessage(message, ex.getClass().getSimpleName() + " - " + ex.getMessage(), depth);
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        addToMessage(message, writer.toString(), depth);
        println(message.toString());
        if (ex.getCause() != null) {
            outputException(ex.getCause(), depth++);
        }
    }
}
