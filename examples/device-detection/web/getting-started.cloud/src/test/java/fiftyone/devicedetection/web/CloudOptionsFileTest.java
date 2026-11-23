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

package fiftyone.devicedetection.web;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * Checks the options file the cloud web example runs with.
 */
public class CloudOptionsFileTest {

    /**
     * With no endpoint set, the example reaches the cloud service over
     * HTTPS.
     */
    @Test
    public void defaultEndPoint_Https() throws IOException {
        String xml = new String(Files.readAllBytes(Paths.get(
                System.getProperty("basedir", "."), "src", "main", "webapp",
                "WEB-INF", "51Degrees-Cloud.xml")), StandardCharsets.UTF_8);
        Matcher endPoint = Pattern.compile(
                "<EndPoint>\\$\\{[^:}]*:-([^}]*)\\}</EndPoint>").matcher(xml);
        assertTrue("51Degrees-Cloud.xml should give a default endpoint",
                endPoint.find());
        assertTrue(endPoint.group(1), endPoint.group(1).startsWith("https://"));
    }
}
