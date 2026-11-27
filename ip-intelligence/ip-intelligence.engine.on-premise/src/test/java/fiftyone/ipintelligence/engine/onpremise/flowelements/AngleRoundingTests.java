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

package fiftyone.ipintelligence.engine.onpremise.flowelements;

import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.ipintelligence.shared.IPIntelligenceDataBaseOnPremise;
import fiftyone.ipintelligence.shared.testhelpers.FileUtils;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * Latitude and Longitude are stored as Declination and Azimuth, so every
 * form they are given in is rounded to 3 decimal places. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/value-types.md#L99">value-types.md line 99</a>:
 * "A value converted from Azimuth or Declination MUST be given to the
 * caller rounded to 3 decimal places, whether it is given as a number, as a
 * string or as a point in a WKT shape."
 * <p>
 * The free Lite file has no Latitude or Longitude, so these tests need a
 * paid data file and are skipped without one.
 */
public class AngleRoundingTests {

    private static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    private static final List<String> ANGLES =
        Arrays.asList("Latitude", "Longitude");

    /**
     * Addresses in different parts of the world, so that the values are
     * not all whole numbers.
     */
    private static final List<String> ADDRESSES = Arrays.asList(
        "185.28.167.77",
        "116.154.188.222",
        "45.236.48.61",
        "2001:4860:4860::8888");

    private interface Check {
        void accept(IPIntelligenceData data) throws Exception;
    }

    private static void forEachAddress(Check check) throws Exception {
        File file = FileUtils.getHashFile();
        assumeTrue("No IP Intelligence data file was found.", file != null);
        try (IPIntelligenceOnPremiseEngine engine =
                 new IPIntelligenceOnPremiseEngineBuilder(loggerFactory, null)
                     .setPerformanceProfile(
                         Constants.PerformanceProfiles.MaxPerformance)
                     .setAutoUpdate(false)
                     .setDataFileSystemWatcher(false)
                     .build(file.toString(), false);
             Pipeline pipeline = new PipelineBuilder(loggerFactory)
                 .addFlowElement(engine)
                 .build()) {
            for (String angle : ANGLES) {
                assumeTrue(
                    "The data file has no " + angle + ".",
                    engine.getProperties().stream().anyMatch(
                        p -> p.getName().equalsIgnoreCase(angle)));
            }
            for (String address : ADDRESSES) {
                try (FlowData flowData = pipeline.createFlowData()) {
                    flowData.addEvidence("query.client-ip", address);
                    flowData.process();
                    check.accept(flowData.get(IPIntelligenceData.class));
                }
            }
        }
    }

    private static void assertRounded(String angle, double value) {
        double rounded = Math.round(value * 1000d) / 1000d;
        assertEquals(
            angle + " " + value + " is not rounded to 3 decimal places.",
            rounded,
            value,
            0d);
    }

    private static void assertRounded(String angle, float value) {
        float rounded = (float) (Math.round(value * 1000d) / 1000d);
        assertEquals(
            angle + " " + value + " is not rounded to 3 decimal places.",
            rounded,
            value,
            0f);
    }

    /**
     * A weighted list of doubles gives each value rounded.
     */
    @Test
    public void weightedDoubles_AreRounded() throws Exception {
        forEachAddress(data -> {
            for (String angle : ANGLES) {
                for (Object value : weighted(data, angle, Double.class)) {
                    assertRounded(angle, (double) (Double) value);
                }
            }
        });
    }

    /**
     * The values of a weighted list, read through the public getValues.
     */
    private static List<Object> weighted(
        IPIntelligenceData data, String angle, Class<?> type) {
        AspectPropertyValue<List<?>> values =
            ((IPIntelligenceDataBaseOnPremise) data).getValues(
                angle,
                new Class<?>[] { List.class, IWeightedValue.class, type });
        List<Object> result = new java.util.ArrayList<>();
        if (values != null && values.hasValue()) {
            for (Object value : values.getValue()) {
                result.add(((IWeightedValue<?>) value).getValue());
            }
        }
        return result;
    }

    /**
     * A weighted list of floats gives each value rounded.
     */
    @Test
    public void weightedFloats_AreRounded() throws Exception {
        forEachAddress(data -> {
            for (String angle : ANGLES) {
                for (Object value : weighted(data, angle, Float.class)) {
                    assertRounded(angle, (float) (Float) value);
                }
            }
        });
    }

    /**
     * The single number is rounded. The text the native results give is
     * not reachable through a public accessor, so it is not checked here.
     */
    @Test
    public void number_IsRounded() throws Exception {
        forEachAddress(data -> {
            AspectPropertyValue<Float> latitude = data.getLatitude();
            if (latitude.hasValue()) {
                assertRounded("Latitude", latitude.getValue());
            }
            AspectPropertyValue<Float> longitude = data.getLongitude();
            if (longitude.hasValue()) {
                assertRounded("Longitude", longitude.getValue());
            }
        });
    }
}
