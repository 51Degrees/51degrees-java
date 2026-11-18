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
import fiftyone.pipeline.core.testclasses.data.TestElementData;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * A Flow Data keeps its Element Data in a list that the members of a
 * parallel group add to at the same time. Every member's Element Data is
 * present afterwards, found by its key, and listed in Pipeline order.
 * Members add errors at the same time with none lost, and the exceptions
 * members throw are added by the group once every member has stopped, in
 * the order the members were added.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#flow-data">Specification</a>
 */
public class ElementDataListTests {

    private static final int MEMBERS = 16;

    private static final int ERRORS_EACH = 50;

    private static final long WAIT_SECONDS = 10;

    private static final Logger LOGGER =
        LoggerFactory.getLogger(ElementDataListTests.class);

    /**
     * Adds its own Element Data, and any errors it is told to, once every
     * member of its group has reached the same point, so that the adds
     * happen at the same time.
     */
    private static class Member
        extends FlowElementBase<TestElementData, ElementPropertyMetaData> {
        private final String key;
        private final CyclicBarrier barrier;
        private final int errors;
        private final boolean fail;

        Member(String key, CyclicBarrier barrier, int errors, boolean fail) {
            super(LOGGER, (flowData, element) ->
                new TestElementData(LOGGER, flowData));
            this.key = key;
            this.barrier = barrier;
            this.errors = errors;
            this.fail = fail;
        }

        @Override
        protected void processInternal(FlowData data) throws Exception {
            if (barrier != null) {
                barrier.await(WAIT_SECONDS, TimeUnit.SECONDS);
            }
            data.getOrAdd(getTypedDataKey(), getDataFactory());
            for (int i = 0; i < errors; i++) {
                data.addError(new RuntimeException(key + "-" + i), this);
            }
            if (fail) {
                throw new IllegalStateException(key);
            }
        }

        @Override
        public String getElementDataKey() {
            return key;
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return new EvidenceKeyFilterWhitelist(
                Collections.<String>emptyList());
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

    private static List<String> memberKeys() {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < MEMBERS; i++) {
            keys.add(String.format("member%02d", i));
        }
        return keys;
    }

    @SuppressWarnings("rawtypes")
    private static Pipeline build(
        List<String> keys,
        int errors,
        boolean fail) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(keys.size());
        FlowElement[] group = new FlowElement[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            group[i] = new Member(keys.get(i), barrier, errors, fail);
        }
        return new PipelineBuilder(LoggerFactory.getILoggerFactory())
            .setSuppressProcessException(true)
            .setAutoCloseElements(true)
            .addFlowElement(new Member("before", null, 0, false))
            .addFlowElementsParallel(group)
            .addFlowElement(new Member("after", null, 0, false))
            .build();
    }

    @Test
    public void elementData_MembersAddAtOnce_AllPresentInPipelineOrder()
        throws Exception {
        List<String> expected = new ArrayList<>();
        expected.add("before");
        expected.addAll(memberKeys());
        expected.add("after");

        try (Pipeline pipeline = build(memberKeys(), 0, false);
             FlowData data = pipeline.createFlowData()) {
            data.process();

            assertEquals(String.valueOf(data.getErrors()),
                0, data.getErrors().size());
            for (String key : expected) {
                ElementData found = data.get(key.toUpperCase());
                assertNotNull(key, found);
                assertSame(found, data.get(key));
            }
            assertEquals(expected, data.getDataKeys());
            List<ElementData> listed = new ArrayList<>();
            for (ElementData elementData : data.elementDataAsIterable()) {
                listed.add(elementData);
            }
            assertEquals(expected.size(), listed.size());
            for (int i = 0; i < expected.size(); i++) {
                assertSame(expected.get(i), data.get(expected.get(i)),
                    listed.get(i));
            }
        }
    }

    @Test
    public void errors_MembersAddAtOnce_NoneLost() throws Exception {
        try (Pipeline pipeline = build(memberKeys(), ERRORS_EACH, false);
             FlowData data = pipeline.createFlowData()) {
            data.process();

            assertEquals(MEMBERS * ERRORS_EACH, data.getErrors().size());
        }
    }

    @Test
    public void errors_MembersThrow_AddedByTheGroupInMemberOrder()
        throws Exception {
        List<String> keys = memberKeys();
        try (Pipeline pipeline = build(keys, 0, true);
             FlowData data = pipeline.createFlowData()) {
            data.process();

            List<String> thrown = new ArrayList<>();
            for (FlowError error : data.getErrors()) {
                thrown.add(error.getThrowable().getMessage());
            }
            assertEquals(keys, thrown);
        }
    }
}
