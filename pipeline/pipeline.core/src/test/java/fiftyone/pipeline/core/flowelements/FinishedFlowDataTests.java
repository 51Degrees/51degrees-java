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
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.testclasses.data.TestElementData;
import fiftyone.pipeline.exceptions.AggregateException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Once processing has finished, a Flow Data is read only. Adding Evidence,
 * Element Data or errors fails with an error, whether processing ended
 * normally or with an exception, while reads, including asking for Element
 * Data that is already there, still work. Adds before and during
 * processing are unaffected.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md#flow-data">Specification</a>
 */
public class FinishedFlowDataTests {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(FinishedFlowDataTests.class);

    private interface Write {
        void write() throws Exception;
    }

    /**
     * Adds Evidence, its own Element Data and an error while it processes,
     * and optionally throws.
     */
    private static class Writer
        extends FlowElementBase<TestElementData, ElementPropertyMetaData> {

        @Override
        protected Class<TestElementData> getDataType() {
            return TestElementData.class;
        }
        private final boolean fail;

        Writer(boolean fail) {
            super(LOGGER, (flowData, element) ->
                new TestElementData(LOGGER, flowData));
            this.fail = fail;
        }

        @Override
        protected void processInternal(FlowData data) {
            data.addEvidence("query.added", "during");
            data.getOrAdd(getTypedDataKey(), getDataFactory());
            data.addError(new RuntimeException("recorded"), this);
            if (fail) {
                throw new IllegalStateException("thrown");
            }
        }

        @Override
        public String getElementDataKey() {
            return "writer";
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

    private static void assertFails(String what, Write write) {
        try {
            write.write();
            fail(what + " succeeded on a Flow Data that has finished");
        } catch (Exception e) {
            assertTrue(
                what + " failed with " + e,
                e instanceof IllegalStateException);
        }
    }

    private static void assertReadOnly(FlowData data, Writer writer) {
        assertFails("Adding Evidence",
            () -> data.addEvidence("query.after", "value"));
        assertFails("Adding a map of Evidence",
            () -> data.addEvidence(
                Collections.singletonMap("query.after", "value")));
        assertFails("Putting Evidence into the Evidence it gives",
            () -> data.getEvidence().put("query.after", "value"));
        assertFails("Populating the Evidence it gives from a map",
            () -> data.getEvidence().populateFromMap(
                Collections.<String, Object>singletonMap(
                    "query.after",
                    "value")));
        assertFails("Adding Element Data by name",
            () -> data.getOrAdd("other", flowData ->
                new TestElementData(LOGGER, flowData)));
        assertFails("Adding Element Data by typed key",
            () -> data.getOrAdd(
                new fiftyone.pipeline.core.typed.TypedKeyDefault<>(
                    "other",
                    TestElementData.class),
                flowData -> new TestElementData(LOGGER, flowData)));
        assertFails("Adding Element Data through the map",
            () -> data.elementDataAsMap().put(
                "other",
                new TestElementData(LOGGER, data)));
        assertFails("Adding an error",
            () -> data.addError(new FlowError.Default(
                new RuntimeException("after"),
                writer)));
        assertFails("Adding an exception",
            () -> data.addError(new RuntimeException("after"), writer));

        // Reads still work, including asking for Element Data that is
        // already there.
        assertSame(
            data.get("writer"),
            data.getOrAdd(writer.getTypedDataKey(), writer.getDataFactory()));
        assertEquals("during", data.getEvidence().get("query.added"));
        assertNull(data.getEvidence().get("query.after"));
    }

    @Test
    public void finished_WritesFail_ReadsWork() throws Exception {
        Writer writer = new Writer(false);
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .setSuppressProcessException(true)
                .addFlowElement(writer)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("query.before", "value");
            data.process();

            assertEquals(1, data.getErrors().size());
            assertReadOnly(data, writer);
            assertEquals(1, data.getErrors().size());
        }
    }

    /**
     * The Element Data map taken before processing refuses writes once
     * processing has finished, as one taken afterwards does.
     */
    @Test
    public void mapTakenBeforeProcessing_WritesFailOnceFinished()
        throws Exception {
        Writer writer = new Writer(false);
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .setSuppressProcessException(true)
                .addFlowElement(writer)
                .build();
             FlowData data = pipeline.createFlowData()) {
            Map<String, Object> map = data.elementDataAsMap();
            data.process();

            assertFails("Putting Element Data into the map",
                () -> map.put("other", new TestElementData(LOGGER, data)));
            assertFails("Putting a map of Element Data into the map",
                () -> map.putAll(Collections.<String, Object>singletonMap(
                    "other",
                    new TestElementData(LOGGER, data))));
            assertFails("Removing Element Data from the map",
                () -> map.remove("writer"));
            assertFails("Clearing the map", map::clear);

            // Reads through the map still work, and see what processing
            // added.
            assertSame(data.get("writer"), map.get("writer"));
            assertFalse(map.containsKey("other"));
            assertFalse(data.elementDataAsMap().containsKey("other"));
        }
    }

    @Test
    public void finishedWithException_WritesFail() throws Exception {
        Writer writer = new Writer(true);
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .addFlowElement(writer)
                .build();
             FlowData data = pipeline.createFlowData()) {
            try {
                data.process();
                fail("Processing should have thrown");
            } catch (AggregateException e) {
                // The Flow Element threw, and processing has still finished.
            }
            assertReadOnly(data, writer);
        }
    }
}
