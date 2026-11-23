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
import fiftyone.pipeline.core.exceptions.PipelineDataException;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.exceptions.AggregateException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Each exception a FlowData method throws is declared by that method of the
 * FlowData interface, by its own type or a type it extends that is more
 * specific than RuntimeException, so that IDEs and callers can see it.
 */
public class FlowDataThrowsTests {

    private PipelineInternal pipeline;

    private FlowDataDefault flowData;

    /**
     * Code that is expected to throw.
     */
    private interface Call {
        void call() throws Exception;
    }

    @Before
    public void init() {
        pipeline = mock(PipelineInternal.class);
        flowData = new FlowDataDefault(
            mock(Logger.class),
            pipeline,
            new EvidenceDefault(mock(Logger.class)));
    }

    @After
    public void cleanUp() throws Exception {
        flowData.close();
    }

    @Test
    public void process_Twice() throws Exception {
        flowData.process();
        assertDeclared(method("process"), () -> flowData.process());
    }

    @Test
    public void process_PipelineClosed() throws Exception {
        when(pipeline.isClosed()).thenReturn(true);
        assertDeclared(method("process"), () -> flowData.process());
    }

    @Test
    public void process_ElementsFailed() throws Exception {
        doThrow(new AggregateException(
            "Exception(s) occurred processing evidence.",
            Collections.<Throwable>singletonList(new Exception("failed"))))
            .when(pipeline).process(flowData);
        assertDeclared(method("process"), () -> flowData.process());
    }

    @Test
    public void getByTypedKey_NotProcessed() throws Exception {
        TypedKey<ElementData> key =
            new TypedKeyDefault<>("test", ElementData.class);
        assertDeclared(
            method("get", TypedKey.class),
            () -> flowData.get(key));
    }

    @Test
    public void getByDataKey_NotProcessed() throws Exception {
        assertDeclared(
            method("get", String.class),
            () -> flowData.get("test"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getFromElement_NotProcessed() throws Exception {
        FlowElement<ElementData, ?> element = mock(FlowElement.class);
        when(element.getTypedDataKey())
            .thenReturn(new TypedKeyDefault<>("test", ElementData.class));
        assertDeclared(
            method("getFromElement", FlowElement.class),
            () -> flowData.getFromElement(element));
    }

    @Test
    public void getAs_NotProcessed() throws Exception {
        assertDeclared(
            method("getAs", String.class, Class.class),
            () -> flowData.getAs("test", String.class));
    }

    @Test
    public void getAs_PropertyNotKnown() throws Exception {
        flowData.process();
        when(pipeline.getMetaDataForProperty("test"))
            .thenThrow(new PipelineDataException("Property not found"));
        assertDeclared(
            method("getAs", String.class, Class.class),
            () -> flowData.getAs("test", String.class));
    }

    @Test
    public void typedGetters_NotProcessed() throws Exception {
        assertDeclared(method("getAsString", String.class),
            () -> flowData.getAsString("test"));
        assertDeclared(method("getAsBool", String.class),
            () -> flowData.getAsBool("test"));
        assertDeclared(method("getAsInt", String.class),
            () -> flowData.getAsInt("test"));
        assertDeclared(method("getAsLong", String.class),
            () -> flowData.getAsLong("test"));
        assertDeclared(method("getAsFloat", String.class),
            () -> flowData.getAsFloat("test"));
        assertDeclared(method("getAsDouble", String.class),
            () -> flowData.getAsDouble("test"));
    }

    private static Method method(String name, Class<?>... parameters)
        throws NoSuchMethodException {
        return FlowData.class.getMethod(name, parameters);
    }

    /**
     * Run the call, which must throw, and check that the interface method
     * declares what it threw.
     */
    private static void assertDeclared(Method method, Call call) {
        Throwable thrown = null;
        try {
            call.call();
        } catch (Throwable e) {
            thrown = e;
        }
        if (thrown == null) {
            fail(method.getName() + " should have thrown");
        }
        for (Class<?> declared : method.getExceptionTypes()) {
            if (declared != RuntimeException.class &&
                declared != Exception.class &&
                declared != Throwable.class &&
                declared.isInstance(thrown)) {
                return;
            }
        }
        fail("FlowData." + method.getName() + " throws " +
            thrown.getClass().getName() + " but declares " +
            Arrays.toString(method.getExceptionTypes()));
    }
}
