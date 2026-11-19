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

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The page's map draws OpenStreetMap's standard tiles, so it credits
 * OpenStreetMap as the other IP Intelligence web examples do, and says
 * nothing of a tile provider it no longer uses.
 */
public class MapAttributionTest {
    private static final String PAGE =
        "src/main/webapp/WEB-INF/html/index.html";
    private static final String TILES =
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
    private static final String CREDIT =
        "&copy; <a href=\"https://www.openstreetmap.org/copyright\">" +
        "OpenStreetMap</a> contributors";

    private static String page() throws Exception {
        return new String(
            Files.readAllBytes(Paths.get(PAGE)),
            StandardCharsets.UTF_8);
    }

    private static int count(String text, String part) {
        int found = 0;
        for (int at = text.indexOf(part); at >= 0;
             at = text.indexOf(part, at + part.length())) {
            found++;
        }
        return found;
    }

    @Test
    public void everyMapCreditsOpenStreetMapForItsTiles() throws Exception {
        String page = page();
        int maps = count(page, TILES);
        assertTrue("the page draws OpenStreetMap tiles", maps > 0);
        assertEquals(
            "each OpenStreetMap tile layer credits OpenStreetMap",
            maps,
            count(page, "attribution: '" + CREDIT + "'"));
    }

    @Test
    public void thePageNamesNoTileProviderItDoesNotUse() throws Exception {
        String page = page();
        assertFalse("no CARTO credit", page.contains("CARTO"));
        assertFalse("no CartoDB comment", page.contains("CartoDB"));
        assertFalse("no CARTO tiles", page.contains("cartocdn.com"));
    }
}
