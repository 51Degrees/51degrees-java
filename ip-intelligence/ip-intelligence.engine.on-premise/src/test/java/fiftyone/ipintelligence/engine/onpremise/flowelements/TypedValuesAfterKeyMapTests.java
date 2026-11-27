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
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.engines.Constants;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Once the results have been read as a map, through asKeyMap(), each typed
 * getter still gives a value of the type it declares, and the same value it
 * gave before.
 */
public class TypedValuesAfterKeyMapTests extends TestsBase {

    private static final String IP_ADDRESS = "116.154.188.222";

    @Before
    public void init() throws Exception {
        testInitialize(Constants.PerformanceProfiles.MaxPerformance);
    }

    @After
    public void cleanup() {
        testCleanup();
    }

    @Test
    public void TypedGetters_AfterAsKeyMap_GiveTheDeclaredTypes()
        throws Exception {
        try (FlowData flowData = getWrapper().getPipeline().createFlowData()) {
            flowData.addEvidence("query.client-ip", IP_ADDRESS);
            flowData.process();
            IPIntelligenceData data = flowData.get(IPIntelligenceData.class);

            Map<String, Object> before = readGetters(data);
            assertFalse("No typed getter gave a value.", before.isEmpty());

            data.asKeyMap();

            Map<String, Object> after = readGetters(data);
            List<String> wrong = new ArrayList<>();
            for (Method getter : getters()) {
                String name = getter.getName();
                if (after.containsKey(name) == false) {
                    continue;
                }
                Object value = after.get(name);
                Class<?> declared = declaredValueType(getter);
                if (value != null && declared.isInstance(value) == false) {
                    wrong.add(name + " gave " +
                        value.getClass().getSimpleName() + " not " +
                        declared.getSimpleName());
                }
            }
            assertTrue(String.join(", ", wrong), wrong.isEmpty());
            assertEquals(before, after);
        }
    }

    /**
     * The value of each typed getter that has one, by getter name.
     */
    private static Map<String, Object> readGetters(IPIntelligenceData data)
        throws IllegalAccessException {
        Map<String, Object> values = new TreeMap<>();
        for (Method getter : getters()) {
            AspectPropertyValue<?> value;
            try {
                value = (AspectPropertyValue<?>) getter.invoke(data);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof PropertyMissingException) {
                    continue;
                }
                throw new AssertionError(getter.getName() + " threw " +
                    e.getCause(), e.getCause());
            }
            if (value != null && value.hasValue()) {
                values.put(getter.getName(), value.getValue());
            }
        }
        return values;
    }

    private static List<Method> getters() {
        List<Method> getters = new ArrayList<>();
        for (Method method : IPIntelligenceData.class.getMethods()) {
            if (method.getParameterCount() == 0 &&
                method.getName().startsWith("get") &&
                AspectPropertyValue.class.equals(method.getReturnType())) {
                getters.add(method);
            }
        }
        return getters;
    }

    /**
     * The class of the value a getter declares, for example Float for
     * AspectPropertyValue&lt;Float&gt; and List for a list of values.
     */
    private static Class<?> declaredValueType(Method getter) {
        Type type = ((ParameterizedType) getter.getGenericReturnType())
            .getActualTypeArguments()[0];
        if (type instanceof ParameterizedType) {
            type = ((ParameterizedType) type).getRawType();
        }
        return (Class<?>) type;
    }
}
