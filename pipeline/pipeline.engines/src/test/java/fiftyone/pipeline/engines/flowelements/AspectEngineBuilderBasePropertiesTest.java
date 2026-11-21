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

import static org.junit.Assert.assertEquals;

/**
 * A list of properties given as one string, as a configuration file gives
 * it, is a comma-separated list.
 */
public class AspectEngineBuilderBasePropertiesTest {

    private static EmptyEngineBuilder builder() {
        return new EmptyEngineBuilder(LoggerFactory.getILoggerFactory());
    }

    @Test
    public void setProperties_CommasWithoutSpaces_EachProperty() {
        EmptyEngineBuilder builder = builder()
            .setProperties("IsMobile,BrowserName,PlatformName");
        assertEquals(
            Arrays.asList("IsMobile", "BrowserName", "PlatformName"),
            builder.getProperties());
    }

    @Test
    public void setProperties_SpacesAroundNames_Trimmed() {
        EmptyEngineBuilder builder = builder()
            .setProperties(" IsMobile, BrowserName ,PlatformName , DeviceType");
        assertEquals(
            Arrays.asList("IsMobile", "BrowserName", "PlatformName",
                "DeviceType"),
            builder.getProperties());
    }

    @Test
    public void setProperties_EmptyEntries_LeftOut() {
        EmptyEngineBuilder builder = builder()
            .setProperties("IsMobile,,BrowserName,");
        assertEquals(
            Arrays.asList("IsMobile", "BrowserName"),
            builder.getProperties());
    }

    @Test
    public void setProperties_OneName_OneProperty() {
        EmptyEngineBuilder builder = builder().setProperties("IsMobile");
        assertEquals(
            Collections.singletonList("IsMobile"),
            builder.getProperties());
    }

    @Test
    public void setProperties_Repeated_AddedOnce() {
        EmptyEngineBuilder builder = builder()
            .setProperties("IsMobile,IsMobile")
            .setProperties("BrowserName, IsMobile");
        assertEquals(
            Arrays.asList("IsMobile", "BrowserName"),
            builder.getProperties());
    }
}
