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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Element Data is written only by the Flow Element that provides it, or the
 * Flow Elements that share it, and only while they process. Once a Flow
 * Element that writes it has finished, a write through its write methods
 * fails with an error, whether the Flow Element finished normally or threw.
 * A Flow Element that shares the instance opens it for writing again while
 * it processes. Populating it from a map is a write like any other.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#element-data">Specification</a>
 */
public class FinishedElementDataTests {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(FinishedElementDataTests.class);

    /**
     * Writes a value under its own name into the Element Data with the key
     * given, which other Writers may share, and optionally throws.
     */
    private static class Writer
        extends FlowElementBase<TestElementData, ElementPropertyMetaData> {

        @Override
        protected Class<TestElementData> getDataType() {
            return TestElementData.class;
        }
        private final String key;
        private final String name;
        private final boolean fail;

        Writer(String key, String name, boolean fail) {
            super(LOGGER, (flowData, element) ->
                new TestElementData(LOGGER, flowData));
            this.key = key;
            this.name = name;
            this.fail = fail;
        }

        @Override
        protected void processInternal(FlowData data) {
            data.getOrAdd(getTypedDataKey(), getDataFactory())
                .put(name, "written");
            if (fail) {
                throw new IllegalStateException("thrown");
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

    private static void assertWriteFails(TestElementData data) {
        try {
            data.put("after", "value");
            fail("A write succeeded after the Flow Element had finished");
        } catch (IllegalStateException e) {
            // Expected.
        }
        try {
            data.setResult("value");
            fail("A setter succeeded after the Flow Element had finished");
        } catch (IllegalStateException e) {
            // Expected.
        }
        try {
            data.populateFromMap(Collections.singletonMap(
                "after",
                (Object) "value"));
            fail("Populating from a map succeeded after the Flow Element " +
                "had finished");
        } catch (IllegalStateException e) {
            // Expected.
        }
    }

    private static Pipeline build(Writer... writers) throws Exception {
        PipelineBuilder builder = new PipelineBuilder(
            LoggerFactory.getILoggerFactory())
            .setSuppressProcessException(true);
        for (Writer writer : writers) {
            builder.addFlowElement(writer);
        }
        return builder.build();
    }

    @Test
    public void finished_WriteFails() throws Exception {
        try (Pipeline pipeline = build(new Writer("own", "first", false));
             FlowData data = pipeline.createFlowData()) {
            data.process();

            TestElementData own = (TestElementData) data.get("own");
            assertEquals("written", own.get("first"));
            assertWriteFails(own);
        }
    }

    @Test
    public void finishedWithException_WriteFails() throws Exception {
        try (Pipeline pipeline = build(new Writer("own", "first", true));
             FlowData data = pipeline.createFlowData()) {
            data.process();

            assertEquals(1, data.getErrors().size());
            assertWriteFails((TestElementData) data.get("own"));
        }
    }

    @Test
    public void shared_EachWriterWritesWhileItProcesses() throws Exception {
        try (Pipeline pipeline = build(
                new Writer("shared", "first", false),
                new Writer("shared", "second", false));
             FlowData data = pipeline.createFlowData()) {
            data.process();

            assertEquals(String.valueOf(data.getErrors()),
                0, data.getErrors().size());
            TestElementData shared = (TestElementData) data.get("shared");
            assertEquals("written", shared.get("first"));
            assertEquals("written", shared.get("second"));
            assertWriteFails(shared);
        }
    }

    @Test
    public void beforeProcessing_WriteWorks() throws Exception {
        Writer writer = new Writer("own", "first", false);
        try (Pipeline pipeline = build(writer);
             FlowData data = pipeline.createFlowData()) {
            TestElementData own = data.getOrAdd(
                writer.getTypedDataKey(),
                writer.getDataFactory());
            own.put("before", "value");
            data.process();

            assertEquals("value", own.get("before"));
            assertEquals("written", own.get("first"));
        }
    }

    @Test
    public void finished_PopulateFromMapFails() throws Exception {
        try (Pipeline pipeline = build(new Writer("own", "first", false));
             FlowData data = pipeline.createFlowData()) {
            data.process();

            TestElementData own = (TestElementData) data.get("own");
            try {
                own.populateFromMap(Collections.singletonMap(
                    "after",
                    (Object) "value"));
                fail("Populating from a map succeeded after the Flow " +
                    "Element had finished");
            } catch (IllegalStateException e) {
                // Expected.
            }
            assertFalse(own.asKeyMap().containsKey("after"));
        }
    }
}
