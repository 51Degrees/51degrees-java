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

import fiftyone.pipeline.core.configuration.ElementOptions;
import fiftyone.pipeline.core.configuration.PipelineOptions;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.testhelpers.flowelements.ConfiguredEngineBuilder;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Configuration files hold a list of strings as a comma-separated string and
 * an enumeration value as the name of the value, the same in every language.
 * <p>
 * Specification: pipeline-specification/features/pipeline-configuration.md
 * line 50 at 51Degrees/specifications 706de1f0, "Configuration files MUST be
 * human-readable and consistent between languages.", and lines 60 and 61,
 * "list of strings (represented as a comma-separated string)" and
 * "enumeration value (represented as the string name of the value)".
 */
public class ConfigurationValueTypesTests {

    private static void build(String name, String value) throws Exception {
        ElementOptions element = new ElementOptions();
        element.builderName = "ConfiguredEngine";
        element.buildParameters.put(name, value);
        PipelineOptions options = new PipelineOptions();
        options.elements.add(element);
        // Building is enough.
        new PipelineBuilder(LoggerFactory.getILoggerFactory())
            .buildFromConfiguration(options)
            .close();
    }

    @Test
    public void properties_CommaSeparated_AreEachProperty() throws Exception {
        build("Properties", "IsMobile,BrowserName");

        assertEquals(
            Arrays.asList("IsMobile", "BrowserName"),
            ConfiguredEngineBuilder.lastProperties);
    }

    @Test
    public void properties_CommaAndSpaceSeparated_AreEachProperty()
        throws Exception {
        build("Properties", "IsMobile, BrowserName ,PlatformName");

        assertEquals(
            Arrays.asList("IsMobile", "BrowserName", "PlatformName"),
            ConfiguredEngineBuilder.lastProperties);
    }

    @Test
    public void enumeration_ByName_IsSet() throws Exception {
        build("Speed", "FAST");

        assertEquals(
            ConfiguredEngineBuilder.Speed.FAST,
            ConfiguredEngineBuilder.lastSpeed);
    }
}
