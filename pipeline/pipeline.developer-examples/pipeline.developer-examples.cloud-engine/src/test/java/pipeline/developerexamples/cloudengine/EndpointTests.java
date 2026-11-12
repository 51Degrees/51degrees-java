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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests which name the example takes its endpoint from. The example reads
 * each name as an environment variable and then as a system property, so
 * these tests set system properties, and are skipped where the environment
 * already sets either name, because the environment would answer first.
 */
public class EndpointTests {

    private static final String NEW_NAME = "_51DEGREES_CLOUD_ENDPOINT";
    private static final String OLD_NAME = "FOD_CLOUD_API_URL";

    @BeforeEach
    public void setUp() {
        assumeTrue(System.getenv(NEW_NAME) == null);
        assumeTrue(System.getenv(OLD_NAME) == null);
        clear();
    }

    @AfterEach
    public void clear() {
        System.clearProperty(NEW_NAME);
        System.clearProperty(OLD_NAME);
    }

    @Test
    public void Endpoint_Old_Name_Alone() {
        System.setProperty(OLD_NAME, "https://old.example/api/v4");

        assertEquals("https://old.example/api/v4/", Main.endpoint());
    }

    @Test
    public void Endpoint_New_Name_Wins() {
        System.setProperty(NEW_NAME, "https://new.example/api/v4/");
        System.setProperty(OLD_NAME, "https://old.example/api/v4/");

        assertEquals("https://new.example/api/v4/", Main.endpoint());
    }

    @Test
    public void Endpoint_Neither_Name() {
        String expected = Main.configuredResourceKey() == null
            ? "https://51degrees.com/starsign/api/"
            : "https://cloud.51degrees.com/api/v4/";

        assertEquals(expected, Main.endpoint());
    }
}
