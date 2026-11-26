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

import fiftyone.pipeline.core.data.ElementDataBase;
import fiftyone.pipeline.core.data.ElementPropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Members of a parallel group add their Element Data to the Flow Data
 * without a lock, so one member adding its own never waits for another
 * member that is still making its instance.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/thread-safety.md">Specification</a>
 */
public class ParallelElementDataAddTests {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(ParallelElementDataAddTests.class);

    private static final long WAIT_SECONDS = 5;

    public static class MemberData extends ElementDataBase {
        public MemberData(Logger logger, FlowData flowData) {
            super(logger, flowData);
        }
    }

    /**
     * Adds its own Element Data. The slow member's data factory waits until
     * the quick member has added its own, as making an instance can take a
     * while.
     */
    private static class Member
        extends FlowElementBase<MemberData, ElementPropertyMetaData> {

        @Override
        protected Class<MemberData> getDataType() {
            return MemberData.class;
        }
        private final String key;

        Member(String key, CountDownLatch beforeAdd, CountDownLatch waitFor,
               CountDownLatch added, AtomicBoolean quickAdded) {
            super(LOGGER, (flowData, element) -> {
                if (beforeAdd != null) {
                    beforeAdd.countDown();
                }
                if (waitFor != null) {
                    try {
                        quickAdded.set(
                            waitFor.await(WAIT_SECONDS, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return new MemberData(LOGGER, flowData);
            });
            this.key = key;
            this.beforeAdd = waitFor == null ? beforeAdd : null;
            this.added = added;
        }

        private final CountDownLatch beforeAdd;
        private final CountDownLatch added;

        @Override
        protected void processInternal(FlowData data) throws Exception {
            if (beforeAdd != null) {
                // The quick member starts once the slow one is making its
                // instance.
                assertTrue(beforeAdd.await(WAIT_SECONDS, TimeUnit.SECONDS));
            }
            data.getOrAdd(getTypedDataKey(), getDataFactory());
            if (added != null) {
                added.countDown();
            }
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

    @Test
    public void parallelMembers_AddWithoutWaitingForEachOther()
        throws Exception {
        CountDownLatch slowMaking = new CountDownLatch(1);
        CountDownLatch quickAddedLatch = new CountDownLatch(1);
        AtomicBoolean quickAdded = new AtomicBoolean(false);
        Member slow = new Member(
            "slow", slowMaking, quickAddedLatch, null, quickAdded);
        Member quick = new Member(
            "quick", slowMaking, null, quickAddedLatch, quickAdded);
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .addFlowElementsParallel(new FlowElement<?, ?>[]{slow, quick})
                .setAutoCloseElements(true)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.process();
            assertTrue(
                "The quick member's add waited for the slow member's",
                quickAdded.get());
            assertNotNull(data.get("slow"));
            assertNotNull(data.get("quick"));
        }
    }
}
