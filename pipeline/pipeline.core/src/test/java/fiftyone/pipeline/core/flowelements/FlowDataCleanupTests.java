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
import fiftyone.pipeline.core.testclasses.flowelements.MultiplyByFiveElement;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.withSettings;

/**
 * Closing Flow Data closes the Element Data that needs cleanup and does
 * nothing at all to the rest, which a cache may still hold. See
 * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/resource-cleanup.md#L84">resource-cleanup.md line 84</a>:
 * "Element Data instances that do not require cleanup MUST be left alone
 * in order for result caching to work correctly."
 */
public class FlowDataCleanupTests {

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    @Test
    public void close_LeavesElementDataWithoutCleanupAlone() throws Exception {
        ElementData plain = mock(ElementData.class);
        ElementData closeable = mock(ElementData.class,
            withSettings().extraInterfaces(AutoCloseable.class));

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(new MultiplyByFiveElement(mock(Logger.class)))
                .build();
             FlowData flowData = pipeline.createFlowData()) {
            flowData.getOrAdd("plain", data -> plain);
            flowData.getOrAdd("closeable", data -> closeable);
        }

        verifyNoInteractions(plain);
        verify((AutoCloseable) closeable, times(1)).close();
    }
}
