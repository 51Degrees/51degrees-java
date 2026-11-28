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

import fiftyone.pipeline.core.exceptions.CloseException;
import fiftyone.pipeline.core.services.PipelineService;
import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByFiveElement;
import org.junit.After;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

/**
 * Closing a Pipeline closes every element and then every service, even when
 * some of them throw. It then throws a CloseException with the first failure
 * as the cause and the others suppressed, and keeps an interrupt.
 */
public class PipelineCloseTests {

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    /**
     * An element whose close() throws once it has been asked to close.
     */
    private static class FailingElement extends MultiplyByFiveElement {
        private final RuntimeException failure;
        private boolean closeCalled = false;

        FailingElement(RuntimeException failure) {
            super(mock(Logger.class));
            this.failure = failure;
        }

        @Override
        public void close() {
            closeCalled = true;
            throw failure;
        }
    }

    /**
     * A service that is AutoCloseable and whose close() throws the exception
     * given, or nothing when it is null.
     */
    private static PipelineService service(Exception failure)
        throws Exception {
        PipelineService service = mock(PipelineService.class,
            withSettings().extraInterfaces(AutoCloseable.class));
        if (failure != null) {
            doThrow(failure).when((AutoCloseable) service).close();
        }
        return service;
    }

    /**
     * A service that is Closeable, whose close() throws IOException.
     */
    private static PipelineService closeableService() throws Exception {
        PipelineService service = mock(PipelineService.class,
            withSettings().extraInterfaces(Closeable.class));
        doThrow(new IOException("closeable service"))
            .when((Closeable) service).close();
        return service;
    }

    @After
    public void clearInterrupt() {
        // A test that fails part way must not leave the interrupt for the
        // next test on this thread.
        Thread.interrupted();
    }

    private Pipeline build(
        FlowElement<?, ?>[] elements,
        PipelineService... services) throws Exception {
        PipelineBuilder builder = new PipelineBuilder(loggerFactory)
            .setAutoCloseElements(true);
        for (FlowElement<?, ?> element : elements) {
            builder.addFlowElement(element);
        }
        for (PipelineService service : services) {
            builder.addService(service);
        }
        return builder.build();
    }

    @Test
    public void close_EveryMemberFails_AllClosedFirstIsCause()
        throws Exception {
        RuntimeException elementFailure =
            new IllegalStateException("element");
        FailingElement failing = new FailingElement(elementFailure);
        MultiplyByFiveElement good =
            new MultiplyByFiveElement(mock(Logger.class));
        InterruptedException interrupted =
            new InterruptedException("service");
        PipelineService interruptedService = service(interrupted);
        PipelineService closeable = closeableService();
        PipelineService goodService = service(null);
        Pipeline pipeline = build(
            new FlowElement<?, ?>[] { failing, good },
            interruptedService, closeable, goodService);

        CloseException thrown = null;
        try {
            assertFalse(good.isClosed());
        } finally {
            try {
                pipeline.close();
            } catch (CloseException e) {
                thrown = e;
            }
        }

        assertNotNull("Closing the Pipeline did not throw.", thrown);
        assertSame(elementFailure, thrown.getCause());
        Throwable[] suppressed = thrown.getSuppressed();
        assertEquals(2, suppressed.length);
        assertSame(interrupted, suppressed[0]);
        assertTrue(suppressed[1] instanceof IOException);
        assertTrue("The interrupt was lost.", Thread.interrupted());
        assertTrue(failing.closeCalled);
        assertTrue(good.isClosed());
        verify((AutoCloseable) interruptedService).close();
        verify((Closeable) closeable).close();
        verify((AutoCloseable) goodService).close();
    }

    @Test
    public void close_OnlyServiceInterrupted_CauseAndInterruptKept()
        throws Exception {
        MultiplyByFiveElement good =
            new MultiplyByFiveElement(mock(Logger.class));
        InterruptedException interrupted =
            new InterruptedException("service");
        PipelineService service = service(interrupted);
        Pipeline pipeline = build(new FlowElement<?, ?>[] { good }, service);

        CloseException thrown = null;
        try {
            assertFalse(good.isClosed());
        } finally {
            try {
                pipeline.close();
            } catch (CloseException e) {
                thrown = e;
            }
        }

        assertNotNull("Closing the Pipeline did not throw.", thrown);
        assertSame(interrupted, thrown.getCause());
        assertEquals(0, thrown.getSuppressed().length);
        assertTrue("The interrupt was lost.", Thread.interrupted());
        assertTrue(good.isClosed());
    }

    @Test
    public void close_NothingFails_NoExceptionNoInterrupt()
        throws Exception {
        MultiplyByFiveElement good =
            new MultiplyByFiveElement(mock(Logger.class));
        PipelineService service = service(null);
        Pipeline pipeline = build(new FlowElement<?, ?>[] { good }, service);

        CloseException thrown = null;
        try {
            assertFalse(good.isClosed());
        } finally {
            try {
                pipeline.close();
            } catch (CloseException e) {
                thrown = e;
            }
        }
        assertNull(thrown);
        // Closing again does nothing.
        pipeline.close();

        assertFalse(Thread.interrupted());
        assertTrue(good.isClosed());
        verify((AutoCloseable) service, times(1)).close();
    }
}
