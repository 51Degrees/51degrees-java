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
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Members of a parallel group add the errors they record under a lock while
 * the group runs, so they appear in the order the members add them. Every
 * other add, and every read once processing has finished, takes no lock.
 * The lock is the Flow Data's errors collection, which these tests hold to
 * see whether an add waits for it.
 * @see <a href="https://github.com/51Degrees/specifications/blob/52e9b047/pipeline-specification/features/thread-safety.md#flow-data">Specification</a>
 */
public class ParallelErrorsLockTests {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(ParallelErrorsLockTests.class);

    private static final long WAIT_SECONDS = 5;

    /**
     * How long an add that waits for the lock is given to finish anyway.
     */
    private static final long HELD_MILLISECONDS = 300;

    /**
     * Adds one error of its own while it processes, saying just before and
     * just after. Members given a barrier wait for each other first, so
     * they add at the same time.
     */
    private static class ErrorMember
        extends FlowElementBase<ElementData, ElementPropertyMetaData> {
        private final String key;
        private final CountDownLatch adding = new CountDownLatch(1);
        private final CountDownLatch added = new CountDownLatch(1);
        private final CyclicBarrier together;

        ErrorMember(String key, CyclicBarrier together) {
            super(LOGGER, (flowData, element) -> null);
            this.key = key;
            this.together = together;
        }

        @Override
        protected void processInternal(FlowData data) throws Exception {
            if (together != null) {
                together.await(WAIT_SECONDS, TimeUnit.SECONDS);
            }
            adding.countDown();
            data.addError(new Exception(key), this);
            added.countDown();
        }

        @Override
        public String getElementDataKey() {
            return key;
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return new EvidenceKeyFilterWhitelist(Collections.emptyList());
        }

        @Override
        public List<ElementPropertyMetaData> getProperties() {
            return Collections.emptyList();
        }

        @Override
        protected void managedResourcesCleanup() {
        }

        @Override
        protected void unmanagedResourcesCleanup() {
        }
    }

    private static Object errorsLock(FlowData data) throws Exception {
        Field field = FlowDataDefault.class.getDeclaredField("errors");
        field.setAccessible(true);
        return field.get(data);
    }

    private static List<String> messages(Collection<FlowError> errors) {
        List<String> messages = new ArrayList<>();
        for (FlowError error : errors) {
            messages.add(error.getThrowable().getMessage());
        }
        Collections.sort(messages);
        return messages;
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void memberError_AddedUnderTheLockWhileTheGroupRuns()
        throws Exception {
        ErrorMember member = new ErrorMember("member", null);
        ErrorMember other = new ErrorMember("other", null);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .setSuppressProcessException(true)
                .addFlowElementsParallel(new FlowElement[]{member, other})
                .setAutoCloseElements(true)
                .build();
             FlowData data = pipeline.createFlowData()) {
            Future<?> processed;
            synchronized (errorsLock(data)) {
                processed = caller.submit(() -> {
                    data.process();
                    return null;
                });
                assertTrue(
                    member.adding.await(WAIT_SECONDS, TimeUnit.SECONDS));
                assertFalse(
                    "A member added its error without the errors lock",
                    member.added.await(
                        HELD_MILLISECONDS,
                        TimeUnit.MILLISECONDS));
            }
            processed.get(WAIT_SECONDS, TimeUnit.SECONDS);
            assertTrue(member.added.await(WAIT_SECONDS, TimeUnit.SECONDS));
            assertEquals(
                Arrays.asList("member", "other"),
                messages(data.getErrors()));
        } finally {
            caller.shutdownNow();
        }
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void errorOutsideTheGroup_AddedWithNoLock() throws Exception {
        ErrorMember before = new ErrorMember("before", null);
        ErrorMember first = new ErrorMember("first", null);
        ErrorMember second = new ErrorMember("second", null);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .setSuppressProcessException(true)
                .addFlowElement(before)
                .addFlowElementsParallel(new FlowElement[]{first, second})
                .setAutoCloseElements(true)
                .build();
             FlowData data = pipeline.createFlowData()) {
            Object lock = errorsLock(data);
            Future<?> processed;
            synchronized (lock) {
                processed = caller.submit(() -> {
                    data.process();
                    return null;
                });
                assertTrue(
                    "An error added outside the group waited for the lock",
                    before.added.await(WAIT_SECONDS, TimeUnit.SECONDS));
            }
            processed.get(WAIT_SECONDS, TimeUnit.SECONDS);
            synchronized (lock) {
                Future<Collection<FlowError>> read =
                    caller.submit(data::getErrors);
                assertEquals(
                    "Reading the errors once processing has finished " +
                        "waited for the lock",
                    3,
                    read.get(WAIT_SECONDS, TimeUnit.SECONDS).size());
            }
        } finally {
            caller.shutdownNow();
        }
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void membersAddErrorsAtOnce_AllPresent() throws Exception {
        CyclicBarrier together = new CyclicBarrier(2);
        ErrorMember first = new ErrorMember("first", together);
        ErrorMember second = new ErrorMember("second", together);
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .setSuppressProcessException(true)
                .addFlowElementsParallel(new FlowElement[]{first, second})
                .setAutoCloseElements(true)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.process();
            assertEquals(
                Arrays.asList("first", "second"),
                messages(data.getErrors()));
        }
    }
}
