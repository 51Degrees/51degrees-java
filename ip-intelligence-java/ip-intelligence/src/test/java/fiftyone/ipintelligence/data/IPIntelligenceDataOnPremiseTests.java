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

package fiftyone.ipintelligence.data;

import fiftyone.ipintelligence.shared.IPIntelligenceDataBaseOnPremise;
import fiftyone.pipeline.core.data.*;
import fiftyone.pipeline.core.data.types.JavaScript;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.data.AspectPropertyValueDefault;
import fiftyone.pipeline.engines.exceptions.NoValueException;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IPIntelligenceDataOnPremiseTests {

    private static String testPropertyName = "testproperty";
    private Logger logger;
    private FlowData flowData;
    private AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine;
    private Pipeline pipeline;
    private MissingPropertyService missingPropertyService;

    private void setupElementProperties(Class<?> type) {
        Map<String, ElementPropertyMetaData> properties = new HashMap<>();
        ElementPropertyMetaData property = new ElementPropertyMetaDataDefault(
            testPropertyName,
            engine,
            "category",
            type,
            true);
        properties.put(testPropertyName, property);
        Map<String, Map<String, ElementPropertyMetaData>> elementProperties =
            new HashMap<>();
        elementProperties.put(engine.getElementDataKey(), properties);
        when(flowData.getPipeline()).thenReturn(pipeline);
        when(pipeline.getElementAvailableProperties())
            .thenReturn(elementProperties);
    }

    @SuppressWarnings("unchecked")
    @Before
    public void init() {
        logger = mock(Logger.class);
        missingPropertyService = mock(MissingPropertyService.class);
        engine = mock(AspectEngine.class);
        when(engine.getElementDataKey()).thenReturn("test");
        flowData = mock(FlowData.class);
        pipeline = mock(Pipeline.class);
    }

    private static <T> void assertValueInside(T expected, AspectPropertyValue<?> value) {
        assertTrue(value.hasValue());
        assertTrue(List.class.isAssignableFrom(value.getValue().getClass()));
        final List<?> values = (List<?>)value.getValue();
        assertEquals(1, values.size());
        final Object firstValue = values.get(0);
        assertTrue(IWeightedValue.class.isAssignableFrom(firstValue.getClass()));
        final IWeightedValue<?> weightedValue = (IWeightedValue<?>)firstValue;
        assertEquals(65535, weightedValue.getRawWeighting());
        assertEquals(expected, weightedValue.getValue());
    }

    @Test
    public void getList() throws NoValueException {
        setupElementProperties(List.class);
        List<String> expected = new ArrayList<>();
        TestResults<List<String>> results =
                new TestResults<>(
                        logger,
                        flowData,
                        engine,
                        missingPropertyService,
                        expected);

        Object value = results.get(testPropertyName);
        assertTrue(AspectPropertyValue.class.isAssignableFrom(value.getClass()));
        assertValueInside(expected, (AspectPropertyValue<?>)value);
        Map<String, Object> map = results.asKeyMap();
        assertTrue(map.containsKey(testPropertyName));
        Object mapValue = map.get(testPropertyName);
        assertTrue(AspectPropertyValue.class.isAssignableFrom(mapValue.getClass()));
        assertValueInside(expected, (AspectPropertyValue<?>)mapValue);
    }

    @Test
    public void getString() throws NoValueException {
        setupElementProperties(String.class);
        String expected = "string";
        TestResults<String> results =
                new TestResults<>(
                        logger,
                        flowData,
                        engine,
                        missingPropertyService,
                        expected);

        Object value = results.get(testPropertyName);
        assertTrue(AspectPropertyValue.class.isAssignableFrom(value.getClass()));
        assertValueInside(expected, (AspectPropertyValue<?>)value);
        Map<String, Object> map = results.asKeyMap();
        assertTrue(map.containsKey(testPropertyName));
        Object mapValue = map.get(testPropertyName);
        assertTrue(AspectPropertyValue.class.isAssignableFrom(mapValue.getClass()));
        assertValueInside(expected, (AspectPropertyValue<?>)mapValue);
    }

    @Test
    public void getBool() throws NoValueException {
        setupElementProperties(Boolean.class);
        Boolean expected = true;
        TestResults<Boolean> results =
                new TestResults<>(
                        logger,
                        flowData,
                        engine,
                        missingPropertyService,
                        expected);

        Object value = results.get(testPropertyName);
        assertTrue(AspectPropertyValue.class.isAssignableFrom(value.getClass()));
        assertValueInside(expected, (AspectPropertyValue<?>)value);
        Map<String, Object> map = results.asKeyMap();
        assertTrue(map.containsKey(testPropertyName));
        Object mapValue = map.get(testPropertyName);
        assertTrue(AspectPropertyValue.class.isAssignableFrom(mapValue.getClass()));
        assertValueInside(expected, (AspectPropertyValue<?>)mapValue);
    }

    @Test
    public void getInt() throws NoValueException {
        setupElementProperties(Integer.class);
        int expected = 1;
        TestResults<Integer> results =
                new TestResults<Integer>(
                        logger,
                        flowData,
                        engine,
                        missingPropertyService,
                        expected);

        Object value = results.get(testPropertyName);
        assertTrue(AspectPropertyValue.class.isAssignableFrom(value.getClass()));
        assertValueInside(expected, (AspectPropertyValue<?>)value);
        Map<String, Object> map = results.asKeyMap();
        assertTrue(map.containsKey(testPropertyName));
        Object mapValue = map.get(testPropertyName);
        assertTrue(AspectPropertyValue.class.isAssignableFrom(mapValue.getClass()));
        assertValueInside(expected, (AspectPropertyValue<?>)mapValue);
    }

    @Test
    public void getDouble() throws NoValueException {
        setupElementProperties(Double.class);
        double expected = 1;
        TestResults<Double> results =
                new TestResults<Double>(
                        logger,
                        flowData,
                        engine,
                        missingPropertyService,
                        expected);

        Object value = results.get(testPropertyName);
        assertTrue(AspectPropertyValue.class.isAssignableFrom(value.getClass()));
        assertValueInside(expected, (AspectPropertyValue<?>)value);
        Map<String, Object> map = results.asKeyMap();
        assertTrue(map.containsKey(testPropertyName));
        Object mapValue = map.get(testPropertyName);
        assertTrue(AspectPropertyValue.class.isAssignableFrom(mapValue.getClass()));
        assertValueInside(expected, (AspectPropertyValue<?>)mapValue);
    }

    @Test
    public void getJavaScript() throws NoValueException {
        setupElementProperties(JavaScript.class);
        String expectedString = "javascript";
        JavaScript expected = new JavaScript(expectedString);
        TestResults<JavaScript> results =
                new TestResults<JavaScript>(
                        logger,
                        flowData,
                        engine,
                        missingPropertyService,
                        expected);

        Object value = results.get(testPropertyName);
        assertTrue(AspectPropertyValue.class.isAssignableFrom(value.getClass()));
        assertValueInside(expected, (AspectPropertyValue<?>)value);
        Map<String, Object> map = results.asKeyMap();
        assertTrue(map.containsKey(testPropertyName));
        Object mapValue = map.get(testPropertyName);
        assertTrue(AspectPropertyValue.class.isAssignableFrom(mapValue.getClass()));
        assertValueInside(expected, (AspectPropertyValue<?>)mapValue);
    }

    private static class TestResults<T> extends IPIntelligenceDataBaseOnPremise {
        private final Object value;

        TestResults(
            Logger logger,
            FlowData flowData,
            AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine,
            MissingPropertyService missingPropertyService,
            Object value) {
            super(logger, flowData, engine, missingPropertyService);
            final List<IWeightedValue<?>> valueList = new ArrayList<>();
            final IWeightedValue<Object> weightedValue = new WeightedValue<>(65535, value);
            valueList.add(weightedValue);
            this.value = valueList;
        }


        @Override
        protected AspectPropertyValue<Boolean> getValueAsBool(String propertyName) {
            if (propertyName.equals(testPropertyName)) {
                return new AspectPropertyValueDefault<>((Boolean)value);
            } else {
                throw new PropertyMissingException();
            }
        }

        @Override
        protected AspectPropertyValue<Double> getValueAsDouble(String propertyName) {
            if (propertyName.equals(testPropertyName)) {
                return new AspectPropertyValueDefault<>((Double)value);
            } else {
                throw new PropertyMissingException();
            }
        }

        @Override
        protected AspectPropertyValue<Integer> getValueAsInteger(String propertyName) {
            if (propertyName.equals(testPropertyName)) {
                return new AspectPropertyValueDefault<>((Integer)value);
            } else {
                throw new PropertyMissingException();
            }
        }

        @Override
        protected AspectPropertyValue<String> getValueAsString(String propertyName) {
            if (propertyName.equals(testPropertyName)) {
                return new AspectPropertyValueDefault<>((String)value);
            } else {
                throw new PropertyMissingException();
            }
        }

        @Override
        protected AspectPropertyValue<JavaScript> getValueAsJavaScript(String propertyName) {
            if (propertyName.equals(testPropertyName)) {
                return new AspectPropertyValueDefault<>((JavaScript) value);
            } else {
                throw new PropertyMissingException();
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public AspectPropertyValue<List<?>> getValues(String propertyName,
                                                      Class<?>[] parametrizedTypes) {
            if (propertyName.equals(testPropertyName)) {
                return new AspectPropertyValueDefault<>((List<?>)value);
            } else {
                throw new PropertyMissingException();
            }
        }

        @Override
        protected boolean propertyIsAvailable(String propertyName) {
            return propertyName.equals(testPropertyName);
        }
    }
}
