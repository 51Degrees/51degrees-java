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

package fiftyone.ipintelligence.cloud;

import fiftyone.ipintelligence.shared.IPIntelligenceData;
import fiftyone.ipintelligence.shared.testhelpers.Constants;
import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static fiftyone.pipeline.util.StringManipulation.stringJoin;
import static org.junit.Assert.*;

public class ValueTests {

    public static void registeredName(WrapperCloud wrapper) throws Exception {
        try (FlowData data = wrapper.getPipeline().createFlowData()) {
            data.addEvidence("server.client-ip", Constants.IPV4_ADDRESS)
                .process();
            ElementData elementData = data.get(wrapper.getEngine().getElementDataKey());
            IPIntelligenceData ipiData = (IPIntelligenceData) elementData;
            assertNotNull("The registered name should not be null.",
                ipiData.getRegisteredName().getValue());
            assertFalse("The registered name should not be empty.",
                ipiData.getRegisteredName().getValue().isEmpty());
        }
    }

    @SuppressWarnings("unchecked")
    public static void valueTypes(WrapperCloud wrapper) throws Exception {
        try (FlowData data = wrapper.getPipeline().createFlowData()) {
            data.addEvidence("server.client-ip", Constants.IPV6_ADDRESS)
                .process();
            ElementData elementData = data.get(wrapper.getEngine().getElementDataKey());
            for (AspectPropertyMetaData property :
                (List<AspectPropertyMetaData>) wrapper.getEngine().getProperties()) {
                if (property.isAvailable()) {
                    Class<?> expectedType;
                    Object value = elementData.get(property.getName());

                    expectedType = getPropertyType(property.getType());

                    assertNotNull("Value of " + property.getName() + " is null. ", value);
                    assertTrue(AspectPropertyValue.class.isAssignableFrom(value.getClass()));
                    assertTrue("Value of '" + property.getName() +
                            "' was of type " + ((AspectPropertyValue<?>) value).getValue().getClass().getSimpleName() +
                            " but should have been " + expectedType.getSimpleName() +
                            ".",
                        expectedType.isAssignableFrom(
                            ((AspectPropertyValue<?>) value).getValue().getClass()));
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void availableProperties(WrapperCloud wrapper) throws Exception {
        try (FlowData data = wrapper.getPipeline().createFlowData()) {
            data.addEvidence("server.client-ip", Constants.IPV4_ADDRESS)
                .process();
            ElementData elementData = data.get(wrapper.getEngine().getElementDataKey());
            for (AspectPropertyMetaData property :
                (List<AspectPropertyMetaData>) wrapper.getEngine().getProperties()) {
                Map<String, Object> map = elementData.asKeyMap();

                assertEquals("Property '" + property.getName() + "' " +
                        (property.isAvailable() ? "should" : "should not") +
                        " be in the results.",
                    property.isAvailable(), map.containsKey(property.getName()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void typedGetters(WrapperCloud   wrapper) throws Exception {
        try (FlowData data = wrapper.getPipeline().createFlowData()) {
            data.addEvidence("server.client-ip", Constants.IPV6_ADDRESS)
                .process();
            ElementData elementData = data.get(wrapper.getEngine().getElementDataKey());
            List<String> missingGetters = new ArrayList<>();
            for (AspectPropertyMetaData property :
                (List<AspectPropertyMetaData>) wrapper.getEngine().getProperties()) {

                if (Arrays.asList(Constants.ExcludedProperties)
                    .contains(property.getName()) == false) {
                    String cleanPropertyName = property.getName()
                        .replace("/", "")
                        .replace("-", "");
                    try {
                        Method classProperty = elementData.getClass()
                            .getMethod("get" + cleanPropertyName);
                        if (classProperty != null) {
                            if (property.isAvailable() == true) {
                                Object value = null;
                                try {
                                    value = classProperty.invoke(elementData);
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                                assertNotNull(
                                    "The typed getter for '" +
                                        property.getName() + "' should " +
                                        "not have returned a null value.",
                                    value);
                            } else {
                                try {
                                    classProperty.invoke(elementData);
                                    fail("The property getter for '" +
                                        property.getName() + "' " +
                                        "should have thrown a " +
                                        "PropertyMissingException.");
                                } catch (Exception e) {
                                    assertTrue(
                                        "The property getter for '" +
                                            property.getName() + "' " +
                                            "should have thrown a " +
                                            "PropertyMissingException, but the exception " +
                                            "was of type '" +
                                            e.getCause().getClass().getSimpleName() +
                                            "'.",
                                        e.getCause() instanceof PropertyMissingException);
                                }
                            }
                        }
                    } catch (NoSuchMethodException e) {
                        missingGetters.add(property.getName());
                    }
                }
            }
            if (missingGetters.size() > 0) {
                if (missingGetters.size() == 1) {
                    fail("The property '" + missingGetters.get(0) + "' " +
                        "is missing a getter in the IPIntelligenceData class. This is not " +
                        "a serious issue, and the property can still be used " +
                        "through the asMap method, but it is an indication " +
                        "that the API should be updated in order to enable the " +
                        "the strongly typed getter for this property.");
                } else {
                    fail("The properties " +
                        stringJoin(missingGetters, ", ") +
                        "are missing getters in the IPIntelligenceData class. This is not " +
                        "a serious issue, and the properties can still be used " +
                        "through the asMap method, but it is an indication " +
                        "that the API should be updated in order to enable the " +
                        "the strongly typed getter for these properties.");
                }
            }
        }
    }
    
    public static Class<?> getPropertyType(Class<?> clazz)
    {
        if (!clazz.isPrimitive()) 
            return clazz;

        if (clazz == Integer.TYPE)
            return Integer.class;
        if (clazz == Boolean.TYPE)
            return Boolean.class;
        if (clazz == Double.TYPE)
            return Double.class;       
        return clazz;
    }
}