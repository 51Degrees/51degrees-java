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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.pipeline.engines.testhelpers.flowelements.EmptyEngineBuilder;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * "Configuration files MUST be human-readable and consistent between
 * languages." (pipeline-specification/features/pipeline-configuration.md
 * line 50) A list of strings is a comma-separated string in a configuration
 * file (line 60), so the list of properties is read the same whatever white
 * space there is around each comma.
 */
public class AspectEngineBuilderPropertiesTests {

    private static List<String> read(String properties) {
        return new EmptyEngineBuilder(LoggerFactory.getILoggerFactory())
            .setProperties(properties)
            .getProperties();
    }

    private static final List<String> EXPECTED =
        Arrays.asList("IsMobile", "BrowserName");

    @Test
    public void setProperties_CommaWithNoSpaces() {
        assertEquals(EXPECTED, read("IsMobile,BrowserName"));
    }

    @Test
    public void setProperties_CommaThenSpace() {
        assertEquals(EXPECTED, read("IsMobile, BrowserName"));
    }

    @Test
    public void setProperties_SpaceOnEachSide() {
        assertEquals(EXPECTED, read("IsMobile , BrowserName"));
    }

    @Test
    public void setProperties_SurroundingWhiteSpace() {
        assertEquals(EXPECTED, read("  IsMobile\t,   BrowserName  "));
    }

    @Test
    public void setProperties_OneProperty() {
        assertEquals(Collections.singletonList("IsMobile"),
            read("IsMobile"));
    }
}
