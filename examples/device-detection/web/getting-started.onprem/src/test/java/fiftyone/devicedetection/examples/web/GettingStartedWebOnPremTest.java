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

package fiftyone.devicedetection.examples.web;

import fiftyone.devicedetection.examples.shared.DataFileHelper;
import fiftyone.devicedetection.examples.shared.FindOutMore;
import fiftyone.devicedetection.shared.testhelpers.FileUtils;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import static fiftyone.devicedetection.examples.web.GettingStartedWebOnPrem.resourceBase;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GettingStartedWebOnPremTest {
    private static Server SERVER;

    @BeforeClass
    public static void startJetty() throws Exception {
        // With no Enterprise file, the free Lite file is found, or
        // downloaded, as the example finds it, so a fresh clone runs this.
        System.setProperty("TestDataFile", FileUtils.getHashFile() == null
                ? DataFileHelper.findDataFile(
                        FileUtils.LITE_HASH_DATA_FILE_PATH).getAbsolutePath()
                : FileUtils.getHashFile().getAbsolutePath());
        SERVER = EmbedJetty.startWebApp(getFilePath(resourceBase).getAbsolutePath(), 8081);
    }

    @Test
    public void testWebOnPrem() throws Exception {

        HttpURLConnection connection =
                (HttpURLConnection) new URL("http://localhost:8081/").openConnection();

        InputStream response = connection.getInputStream();
        String responseBody;
        try (Scanner scanner = new Scanner(response)) {
            responseBody = scanner.useDelimiter("\\A").next();
            System.out.println(responseBody);
        }
        int code = connection.getResponseCode();
        connection.disconnect();
        assertEquals(200, code);
        assertTrue("The page should end with the '" + FindOutMore.HEADING +
                "' section", responseBody.contains(
                        "<h2>" + FindOutMore.HEADING + "</h2>"));
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        SERVER.stop();
    }
}