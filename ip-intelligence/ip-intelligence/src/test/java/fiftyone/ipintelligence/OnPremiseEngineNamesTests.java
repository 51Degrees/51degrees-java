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

package fiftyone.ipintelligence;

import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.RequiredPropertiesConfigSwig;
import fiftyone.ipintelligence.engine.onpremise.interop.swig.VectorStringSwig;
import fiftyone.pipeline.core.flowelements.Pipeline;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fiftyone.ipintelligence.shared.testhelpers.FileUtils.LITE_IPI_DATA_FILE_NAME;
import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * The names the on-premise pipeline builder has its engine create the native
 * engine with, read from the configuration the engine passes to it.
 */
public class OnPremiseEngineNamesTests {

    @Test
    public void NoNames_NativeEngineCreatedWithNoNames() throws Exception {
        assertEquals(
            "A pipeline configured with no names should have its " +
                "engine create the native engine with no names.",
            Collections.emptyList(),
            namesCreatedWith(Collections.<String>emptyList()));
    }

    @Test
    public void Names_NativeEngineCreatedWithOnlyThoseNames()
        throws Exception {
        List<String> names = Collections.singletonList("RegisteredName");
        assertEquals(
            "A pipeline configured with names should have its engine " +
                "create the native engine with those names and no others.",
            names,
            namesCreatedWith(names));
    }

    /**
     * Build an on-premise pipeline with the names given, and give the names
     * its engine creates the native engine with.
     */
    private static List<String> namesCreatedWith(List<String> names)
        throws Exception {
        File file;
        try {
            file = getFilePath(LITE_IPI_DATA_FILE_NAME);
        }
        catch (IllegalArgumentException e) {
            assumeTrue(e.getMessage(), false);
            return null;
        }
        IPIntelligenceOnPremisePipelineBuilder builder =
            new IPIntelligencePipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .useOnPremise(file.getAbsolutePath(), false)
                .setShareUsage(false)
                .setAutoUpdate(false)
                .setDataFileSystemWatcher(false);
        for (String name : names) {
            builder.setProperty(name);
        }
        try (Pipeline pipeline = builder.build()) {
            return namesCreatedWith(
                pipeline.getElement(IPIntelligenceOnPremiseEngine.class));
        }
    }

    private static List<String> namesCreatedWith(
        IPIntelligenceOnPremiseEngine engine) throws Exception {
        Field field = IPIntelligenceOnPremiseEngine.class
            .getDeclaredField("propertiesConfigSwig");
        field.setAccessible(true);
        RequiredPropertiesConfigSwig config =
            (RequiredPropertiesConfigSwig) field.get(engine);
        try (VectorStringSwig names = config.getProperties()) {
            return new ArrayList<>(names);
        }
    }
}
