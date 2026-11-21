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

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByFiveElement;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The Pipeline's Evidence key filter depends only on the Flow Elements it
 * holds, which never change after it is built. So the Pipeline builds the
 * filter once, when it is built, and every request then reads it with no
 * lock, as usage sharing and the web integration do. A final field set in
 * the constructor is seen whole by every thread.
 * @see <a href="https://github.com/51Degrees/specifications/blob/52e9b047/pipeline-specification/features/thread-safety.md#flow-elements">Specification</a>
 */
public class PipelineEvidenceKeyFilterTests {

    private static final int THREADS = 8;

    @Test
    public void evidenceKeyFilter_HeldInAFinalField() throws Exception {
        Field field =
            PipelineDefault.class.getDeclaredField("evidenceKeyFilter");
        assertTrue(
            "The filter is built with the Pipeline, so its field is final",
            Modifier.isFinal(field.getModifiers()));
    }

    @Test
    public void evidenceKeyFilter_BuiltWithThePipeline() throws Exception {
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .addFlowElement(new MultiplyByFiveElement(
                    LoggerFactory.getLogger(MultiplyByFiveElement.class)))
                .build()) {
            Field field =
                PipelineDefault.class.getDeclaredField("evidenceKeyFilter");
            field.setAccessible(true);
            // Read the field before anything asks for the filter.
            Object built = field.get(pipeline);
            assertNotNull(
                "The filter exists as soon as the Pipeline is built",
                built);
            assertTrue(
                "The filter accepts the Flow Element's Evidence",
                ((EvidenceKeyFilter) built).include("value"));
            assertSame(built, pipeline.getEvidenceKeyFilter());
        }
    }

    @Test
    public void evidenceKeyFilter_SameFilterForEveryThread() throws Exception {
        ExecutorService threads = Executors.newFixedThreadPool(THREADS);
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .build()) {
            List<Callable<EvidenceKeyFilter>> calls = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                calls.add(pipeline::getEvidenceKeyFilter);
            }
            List<Future<EvidenceKeyFilter>> filters = threads.invokeAll(calls);
            EvidenceKeyFilter first = filters.get(0).get();
            for (Future<EvidenceKeyFilter> filter : filters) {
                assertSame(first, filter.get());
            }
        } finally {
            threads.shutdownNow();
        }
    }
}
