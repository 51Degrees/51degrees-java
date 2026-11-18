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
import fiftyone.pipeline.core.testclasses.data.TestElementData;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * The check, for the Flow Elements in this module, that processes each one,
 * then writes to its Element Data and expects the error. A parallel group,
 * {@link ParallelElements}, has no Element Data of its own, so its members'
 * Element Data is written to once the group has finished.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Specification</a>
 */
public class WritesAfterProcessingTests {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(WritesAfterProcessingTests.class);

    /**
     * Writes a value into its own Element Data while it processes.
     */
    private static class Member
        extends FlowElementBase<TestElementData, ElementPropertyMetaData> {
        private final String key;

        Member(String key) {
            super(LOGGER, (flowData, element) ->
                new TestElementData(LOGGER, flowData));
            this.key = key;
        }

        @Override
        protected void processInternal(FlowData data) {
            data.getOrAdd(getTypedDataKey(), getDataFactory())
                .put("written", "during");
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

    private interface Write {
        void write();
    }

    private static void assertFails(String what, Write write) {
        try {
            write.write();
            fail(what + " succeeded after the Flow Element had finished");
        } catch (IllegalStateException e) {
            // Expected.
        }
    }

    /**
     * Every write method of Element Data fails once its Flow Element has
     * finished.
     * @param data the Element Data of a finished Flow Element
     */
    static void assertWritesFail(ElementData data) {
        assertFails("put", () -> data.put("after", "value"));
        assertFails("populateFromMap", () -> data.populateFromMap(
            Collections.<String, Object>singletonMap("after", "value")));
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void parallelElements_MembersElementDataRefusesWrites()
        throws Exception {
        Member first = new Member("first");
        Member second = new Member("second");
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .addFlowElementsParallel(new FlowElement[] { first, second })
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.process();

            assertEquals(String.valueOf(data.getErrors()),
                0, data.getErrors().size());
            for (Member member : new Member[] { first, second }) {
                ElementData own = data.getFromElement(member);
                assertEquals("during", own.get("written"));
                assertWritesFail(own);
            }
        }
    }
}
