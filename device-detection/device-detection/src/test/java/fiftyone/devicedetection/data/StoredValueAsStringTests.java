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

package fiftyone.devicedetection.data;

import fiftyone.devicedetection.shared.DeviceDataBaseOnPremise;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.ElementPropertyMetaDataDefault;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.engines.data.ProvidesValuesAsString;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.MissingPropertyReason;
import fiftyone.pipeline.engines.services.MissingPropertyResult;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * On-premise Device Data holds Property values as strings, and the string
 * stored for a Property can be read as well as the parsed value.
 * <p>
 * Specification: pipeline-specification/features/access-to-results.md lines
 * 65 to 67 at 51Degrees/specifications 706de1f0, "Where an Element Data
 * instance holds Property values as strings and parses them into the type
 * each Property declares, it SHOULD be possible to get the stored string for
 * a Property as well as the parsed value.", and value-types.md lines 39 to
 * 53 for what an implementation offering it must do.
 */
public class StoredValueAsStringTests {

    private static final String MISSING_DESCRIPTION = "Not in this file.";

    private TestData data;

    @Before
    @SuppressWarnings("unchecked")
    public void init() {
        AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData>
            engine = mock(AspectEngine.class);
        when(engine.getElementDataKey()).thenReturn("device");
        Map<String, ElementPropertyMetaData> properties =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        properties.put("ismobile", new ElementPropertyMetaDataDefault(
            "ismobile", engine, "category", Boolean.class, true));
        properties.put("screenpixelswidth", new ElementPropertyMetaDataDefault(
            "screenpixelswidth", engine, "category", Integer.class, true));
        Map<String, Map<String, ElementPropertyMetaData>> available =
            new HashMap<>();
        available.put("device", properties);
        Pipeline pipeline = mock(Pipeline.class);
        when(pipeline.getElementAvailableProperties()).thenReturn(available);
        FlowData flowData = mock(FlowData.class);
        when(flowData.getPipeline()).thenReturn(pipeline);
        MissingPropertyService missing = mock(MissingPropertyService.class);
        when(missing.getMissingPropertyReason(anyString(), any(List.class)))
            .thenReturn(new MissingPropertyResult(
                MissingPropertyReason.DataFileUpgradeRequired,
                MISSING_DESCRIPTION));

        Map<String, String> stored = new HashMap<>();
        stored.put("ismobile", "Unknown");
        stored.put("screenpixelswidth", "N/A");
        data = new TestData(flowData, engine, missing, stored);
    }

    @Test
    public void capability_CanBeTestedFor() {
        assertTrue(data instanceof ProvidesValuesAsString);
    }

    @Test
    public void storedString_IsReturnedAsStored() {
        ProvidesValuesAsString strings = data;

        assertEquals("Unknown",
            strings.getStoredValueAsString("ismobile").getValue());
        assertEquals("N/A",
            strings.getStoredValueAsString("ScreenPixelsWidth").getValue());
    }

    @Test
    public void typedAccessor_IsUnchanged() {
        data.getStoredValueAsString("ismobile");

        assertFalse(data.getIsMobile().getValue());
        assertEquals(0, (int) data.getScreenPixelsWidth().getValue());
    }

    @Test
    public void missingProperty_ReportedAsByTheTypedAccessor() {
        String typed = null;
        try {
            data.getIsTablet();
            fail("The typed accessor throws for a missing property");
        } catch (PropertyMissingException e) {
            typed = e.getMessage();
        }
        try {
            data.getStoredValueAsString("istablet");
            fail("The stored value throws for a missing property");
        } catch (PropertyMissingException e) {
            assertEquals(typed, e.getMessage());
            assertEquals(MissingPropertyReason.DataFileUpgradeRequired,
                e.getReason());
        }
    }

    /**
     * Device Data that holds its values as strings and parses them as the C
     * library does.
     */
    private static class TestData extends DeviceDataBaseOnPremise {
        private final Map<String, String> stored;

        TestData(
            FlowData flowData,
            AspectEngine<? extends AspectData,
                ? extends AspectPropertyMetaData> engine,
            MissingPropertyService missingPropertyService,
            Map<String, String> stored) {
            super(mock(Logger.class), flowData, engine,
                missingPropertyService);
            this.stored = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            this.stored.putAll(stored);
        }

        @Override
        protected boolean propertyIsAvailable(String propertyName) {
            return stored.containsKey(propertyName);
        }

        @Override
        public AspectPropertyValue<List<String>> getValues(
            String propertyName) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected AspectPropertyValue<String> getValueAsString(
            String propertyName) {
            return new AspectPropertyValueDefault<>(stored.get(propertyName));
        }

        @Override
        protected AspectPropertyValue<Integer> getValueAsInteger(
            String propertyName) {
            String value = stored.get(propertyName);
            int parsed = 0;
            try {
                parsed = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // As C's atoi, text that is not a number gives 0.
            }
            return new AspectPropertyValueDefault<>(parsed);
        }

        @Override
        protected AspectPropertyValue<Boolean> getValueAsBool(
            String propertyName) {
            return new AspectPropertyValueDefault<>(
                "True".equals(stored.get(propertyName)));
        }

        @Override
        protected AspectPropertyValue<Double> getValueAsDouble(
            String propertyName) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected AspectPropertyValue<JavaScript> getValueAsJavaScript(
            String propertyName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AspectPropertyValue<List<String>> getUserAgents() {
            throw new UnsupportedOperationException();
        }

        @Override
        public AspectPropertyValue<String> getDeviceId() {
            throw new UnsupportedOperationException();
        }
    }
}
