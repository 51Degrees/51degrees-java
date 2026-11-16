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

import fiftyone.ipintelligence.engine.onpremise.TestsBase;
import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.ipintelligence.shared.IPIntelligenceDataBaseOnPremise;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * A value the data file stores as an angle, Latitude and Longitude, is
 * given rounded to 3 decimal places, read as a list of weighted values or
 * by its typed accessor. A float is the float nearest the rounded decimal.
 * Only a data file that has these properties, the Enterprise file, can show
 * it, so the test is skipped with the free Lite file.
 */
public class AngleRoundingTests extends TestsBase {

    private static final String[] ANGLES = {"Latitude", "Longitude"};

    private static final String[] ADDRESSES = {
        "185.28.167.77",
        "116.154.188.222",
        "45.236.48.61",
        "2001:0db8:085a:0000:0000:8a2e:0370:7334"
    };

    /**
     * The number of decimal places an angle is rounded to.
     */
    private static final double PLACES_SCALE = 1000.0;

    @Before
    public void init() throws Exception {
        testInitialize(Constants.PerformanceProfiles.LowMemory);
    }

    @After
    public void cleanup() {
        testCleanup();
    }

    @Test
    public void Angles_RoundedTo3Places() throws Exception {
        IPIntelligenceOnPremiseEngine engine = getWrapper().getEngine();
        assumeTrue("Needs a data file with Latitude and Longitude, being " +
            "the Enterprise file", hasAngles(engine));
        List<String> unrounded = new ArrayList<>();
        for (String address : ADDRESSES) {
            try (FlowData data =
                     getWrapper().getPipeline().createFlowData()) {
                data.addEvidence("query.client-ip", address);
                data.process();
                IPIntelligenceData ipi = data.get(IPIntelligenceData.class);
                for (String angle : ANGLES) {
                    checkWeighted(address, angle,
                        readWeighted(ipi, angle), unrounded);
                }
                checkValue(address, "getLatitude",
                    ipi.getLatitude(), unrounded);
                checkValue(address, "getLongitude",
                    ipi.getLongitude(), unrounded);
            }
        }
        assertTrue("These angles are not rounded to 3 decimal places: " +
            unrounded, unrounded.isEmpty());
    }

    /**
     * Read by name, with no type given, Latitude and Longitude are weighted
     * values of Float, the type the IP Intelligence data model gives them
     * and the type their typed accessors return, so a caller reading them
     * either way gets the same number.
     */
    @Test
    public void Angles_ByName_AreWeightedFloats() throws Exception {
        IPIntelligenceOnPremiseEngine engine = getWrapper().getEngine();
        assumeTrue("Needs a data file with Latitude and Longitude, being " +
            "the Enterprise file", hasAngles(engine));
        List<String> wrong = new ArrayList<>();
        for (String address : ADDRESSES) {
            try (FlowData data =
                     getWrapper().getPipeline().createFlowData()) {
                data.addEvidence("query.client-ip", address);
                data.process();
                IPIntelligenceData ipi = data.get(IPIntelligenceData.class);
                for (String angle : ANGLES) {
                    AspectPropertyValue<?> apv =
                        (AspectPropertyValue<?>) ipi.get(angle.toLowerCase());
                    if (apv.hasValue() == false) {
                        continue;
                    }
                    for (Object item : (List<?>) apv.getValue()) {
                        Object value = ((IWeightedValue<?>) item).getValue();
                        if ((value instanceof Float) == false) {
                            wrong.add(address + " " + angle + " " +
                                (value == null ?
                                    "null" :
                                    value.getClass().getSimpleName()));
                        }
                    }
                }
            }
        }
        assertTrue("These weighted values are not Float: " + wrong,
            wrong.isEmpty());
    }

    private static boolean hasAngles(IPIntelligenceOnPremiseEngine engine) {
        int found = 0;
        for (AspectPropertyMetaData property : engine.getProperties()) {
            for (String angle : ANGLES) {
                if (property.getName().equalsIgnoreCase(angle) &&
                    property.isAvailable()) {
                    found++;
                }
            }
        }
        return found == ANGLES.length;
    }

    /**
     * Read a property as a list of weighted values of Float, the type the
     * IP Intelligence data model gives Latitude and Longitude and the type
     * their typed accessors return. This is the call the element data makes
     * for a weighted property, and for Float the engine reads the native
     * weighted double list, which is where the rounding is done.
     */
    private static Object readWeighted(IPIntelligenceData ipi, String angle) {
        return ((IPIntelligenceDataBaseOnPremise) ipi).getValues(
            angle,
            new Class<?>[] {List.class, IWeightedValue.class, Float.class});
    }

    private static void checkWeighted(String address,
                                      String angle,
                                      Object value,
                                      List<String> unrounded) {
        AspectPropertyValue<?> apv = (AspectPropertyValue<?>) value;
        if (apv.hasValue() == false) {
            return;
        }
        List<Number> numbers = new ArrayList<>();
        if (apv.getValue() instanceof List) {
            for (Object item : (List<?>) apv.getValue()) {
                numbers.add((Number) ((IWeightedValue<?>) item).getValue());
            }
        } else {
            numbers.add((Number) apv.getValue());
        }
        for (Number number : numbers) {
            if (isRounded(number) == false) {
                unrounded.add(address + " " + angle + " " + number);
            }
        }
    }

    private static void checkValue(String address,
                                   String accessor,
                                   AspectPropertyValue<? extends Number> value,
                                   List<String> unrounded) {
        if (value.hasValue() && isRounded(value.getValue()) == false) {
            unrounded.add(address + " " + accessor + " " + value.getValue());
        }
    }

    /**
     * Whether a number is the value of its type nearest a decimal with at
     * most 3 places.
     */
    private static boolean isRounded(Number number) {
        double rounded =
            Math.round(number.doubleValue() * PLACES_SCALE) / PLACES_SCALE;
        if (number instanceof Float) {
            return number.floatValue() == (float) rounded;
        }
        return number.doubleValue() == rounded;
    }
}
