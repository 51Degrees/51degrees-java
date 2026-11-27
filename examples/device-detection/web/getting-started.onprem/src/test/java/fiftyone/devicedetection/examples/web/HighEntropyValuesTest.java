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

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The web example illustrates the ways it obtains high entropy values
 * (device-detection-specification/required-examples.md, "The example SHOULD
 * illustrate various techniques for obtaining "high entropy values" for
 * presentation to the origin server as well as to the 51degrees cloud
 * service"): the Accept-CH response header, which asks the browser to send
 * them with its next request, and the client-side JavaScript, which reads
 * them with getHighEntropyValues and sends them back to the server. The
 * page says so, and the options file adds the elements the JavaScript needs.
 */
public class HighEntropyValuesTest {

    private static String read(String file) throws Exception {
        File found = getFilePath(GettingStartedWebOnPrem.resourceBase + file);
        return new String(Files.readAllBytes(found.toPath()),
            StandardCharsets.UTF_8);
    }

    @Test
    public void pageNamesEachTechnique() throws Exception {
        String page = read("/WEB-INF/html/example-description.html");
        assertTrue("The page should name the Accept-CH header",
            page.contains("Accept-CH"));
        assertTrue("The page should name getHighEntropyValues, which the " +
            "client-side JavaScript calls", page.contains("getHighEntropyValues"));
    }

    @Test
    public void optionsFileAddsTheJavaScriptAndItsEndpoint() throws Exception {
        String options = read("/WEB-INF/51Degrees-OnPrem.xml");
        assertTrue(options.contains("<BuilderName>JavaScriptBuilderElement</BuilderName>"));
        assertTrue(options.contains("<BuilderName>JsonBuilderElement</BuilderName>"));
        assertFalse("getHighEntropyValues needs a secure origin; the comment " +
                "should not say it fails in one",
            options.replaceAll("\\s+", " ").contains("in secure origins won't"));
    }
}
