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

package fiftyone.ipintelligence.examples.web;

import fiftyone.ipintelligence.examples.shared.DataFileHelper;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Checks the options file this web example runs with.
 */
public class OptionsFileTest {

    /**
     * With no data file set, the example falls back to the free Lite file,
     * which a fresh clone downloads, and not to the Enterprise file, which a
     * fresh clone never has.
     */
    @Test
    public void defaultDataFile_FreeLiteFile() throws IOException {
        String xml = new String(Files.readAllBytes(Paths.get(
                System.getProperty("basedir", "."), "src", "main", "webapp",
                "WEB-INF", "51Degrees-OnPrem.xml")), StandardCharsets.UTF_8);
        Matcher dataFile = Pattern.compile(
                "<DataFile>\\$\\{TestDataFile:-([^}]*)\\}</DataFile>")
                .matcher(xml);
        assertTrue("51Degrees-OnPrem.xml should give a default IP Intelligence " +
                "data file", dataFile.find());
        assertEquals("cxx/data/" + DataFileHelper.LITE_DATA_FILE_REL_PATH,
                dataFile.group(1));
    }
}
