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

package fiftyone.pipeline.engines.fiftyone.flowelements;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Sequence and Set Headers Element Data hold only their values, so they
 * are pooled. Requests made one after another reuse one instance of each.
 * @see <a href="https://github.com/51Degrees/specifications/blob/52e9b047/pipeline-specification/features/resource-cleanup.md#reusing-element-data">Specification</a>
 */
public class PooledElementDataTests {

    private static final int REQUESTS = 100;

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    @Test
    public void oneThread_OneInstanceOfEach() throws Exception {
        Set<Object> sequence =
            Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> headers =
            Collections.newSetFromMap(new IdentityHashMap<>());
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(
                    new SequenceElementBuilder(loggerFactory).build())
                .addFlowElement(
                    new SetHeadersElementBuilder(loggerFactory).build())
                .setAutoCloseElements(true)
                .build()) {
            for (int i = 0; i < REQUESTS; i++) {
                try (FlowData data = pipeline.createFlowData()) {
                    data.process();
                    sequence.add(data.get("sequence"));
                    headers.add(data.get("set-headers"));
                }
            }
        }
        assertEquals(1, sequence.size(), "Sequence Element Data allocated");
        assertEquals(1, headers.size(), "Set Headers Element Data allocated");
    }
}
