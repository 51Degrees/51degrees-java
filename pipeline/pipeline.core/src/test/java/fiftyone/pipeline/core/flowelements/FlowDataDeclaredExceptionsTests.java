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

package fiftyone.pipeline.core.flowelements;

import fiftyone.pipeline.core.data.ElementData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.core.exceptions.PipelineDataException;
import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByFiveElement;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * The FlowData interface declares, in the throws clause that IDEs and
 * reflection read, the exceptions its methods throw for a mistake by the
 * caller, and the implementation throws one of those. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/exception-handling.md#L17">exception-handling.md line 17</a>:
 * "In all cases, standard language features SHOULD be used to let IDEs and
 * users know what exceptions/errors a particular function can throw."
 */
public class FlowDataDeclaredExceptionsTests {

    private final ILoggerFactory loggerFactory = name -> mock(Logger.class);

    private interface Call {
        void run(FlowData flowData) throws Exception;
    }

    private static List<Class<?>> declared(String name, Class<?>... types)
        throws Exception {
        Method method = FlowData.class.getMethod(name, types);
        return Arrays.asList(method.getExceptionTypes());
    }

    private static void assertDeclares(
        List<Class<?>> declared, Class<?> type, String method) {
        assertTrue(method + " does not declare " + type.getSimpleName() +
                ", only " + declared + ".",
            declared.contains(type));
    }

    /**
     * Call the method on Flow Data that has not been processed, and check
     * the exception is one the interface declares.
     */
    private void assertThrowsDeclared(
        List<Class<?>> declared, String method, boolean process, Call call)
        throws Exception {
        MultiplyByFiveElement element =
            new MultiplyByFiveElement(mock(Logger.class));
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(element)
                .build();
             FlowData flowData = pipeline.createFlowData()) {
            if (process) {
                flowData.addEvidence(element.evidenceKeys.get(0), 2);
                flowData.process();
            }
            try {
                call.run(flowData);
                fail(method + " did not throw.");
            } catch (Exception e) {
                boolean isDeclared = false;
                for (Class<?> type : declared) {
                    isDeclared |= type.isInstance(e);
                }
                assertTrue(method + " threw " + e.getClass().getName() +
                    ", which it does not declare, only " + declared + ".",
                    isDeclared);
            }
        }
    }

    @Test
    public void process_DeclaresAndThrowsIllegalState() throws Exception {
        List<Class<?>> declared = declared("process");
        assertDeclares(declared, IllegalStateException.class, "process");
        assertThrowsDeclared(declared, "process", true, FlowData::process);
    }

    @Test
    public void getByTypedKey_DeclaresAndThrowsIllegalState()
        throws Exception {
        List<Class<?>> declared = declared("get", TypedKey.class);
        assertDeclares(declared, IllegalStateException.class, "get(TypedKey)");
        assertDeclares(declared, IllegalArgumentException.class,
            "get(TypedKey)");
        TypedKey<ElementData> key =
            new TypedKeyDefault<>("test", ElementData.class);
        assertThrowsDeclared(declared, "get(TypedKey)", false,
            d -> d.get(key));
    }

    @Test
    public void getByName_DeclaresAndThrowsIllegalState() throws Exception {
        List<Class<?>> declared = declared("get", String.class);
        assertDeclares(declared, IllegalStateException.class, "get(String)");
        assertThrowsDeclared(declared, "get(String)", false,
            d -> d.get("muliplyByFive"));
    }

    @Test
    public void getFromElement_DeclaresAndThrowsIllegalState()
        throws Exception {
        List<Class<?>> declared =
            declared("getFromElement", FlowElement.class);
        assertDeclares(declared, IllegalStateException.class,
            "getFromElement");
        assertDeclares(declared, IllegalArgumentException.class,
            "getFromElement");
        assertThrowsDeclared(declared, "getFromElement", true,
            d -> d.getFromElement(null));
    }

    @Test
    public void getAs_DeclaresAndThrowsPipelineDataException()
        throws Exception {
        List<Class<?>> declared = declared("getAs", String.class, Class.class);
        assertDeclares(declared, PipelineDataException.class, "getAs");
        assertDeclares(declared, ClassCastException.class, "getAs");
        assertThrowsDeclared(declared, "getAs", false,
            d -> d.getAs("result", Integer.class));
        for (String name : new String[] {
            "getAsString", "getAsBool", "getAsInt", "getAsLong",
            "getAsFloat", "getAsDouble" }) {
            assertDeclares(declared(name, String.class),
                PipelineDataException.class, name);
        }
    }

    /**
     * Keeps the ElementData import, which documents what get returns.
     */
    private static ElementData unused;
}
