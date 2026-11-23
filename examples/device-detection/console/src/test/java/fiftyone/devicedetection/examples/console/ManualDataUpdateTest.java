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

package fiftyone.devicedetection.examples.console;

import fiftyone.devicedetection.examples.shared.DataFileHelper;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Runs the example that reloads the data file when its code says so,
 * rather than through an automatic update, and checks that it detects
 * the same User-Agents before and after the reload.
 */
public class ManualDataUpdateTest {

    private static final String CHECK = "a mobile device?";

    @Test
    public void manualDataUpdate_ReloadsTheDataFile() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ManualDataUpdate.run(
            DataFileHelper.getDataFileLocation(null), buffer);
        String output = buffer.toString("UTF-8");
        System.out.println(output);

        int reloading = output.indexOf("Reloading the data file");
        int reloaded = output.indexOf("Reloaded the data file");
        assertTrue("The example should say it reloads the data file",
                reloading >= 0);
        assertTrue("The example should say it reloaded the data file",
                reloaded > reloading);
        assertEquals(2, count(output.substring(0, reloading), CHECK));
        assertEquals(2, count(output.substring(reloaded), CHECK));
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int i = text.indexOf(part); i >= 0;
             i = text.indexOf(part, i + part.length())) {
            count++;
        }
        return count;
    }
}
