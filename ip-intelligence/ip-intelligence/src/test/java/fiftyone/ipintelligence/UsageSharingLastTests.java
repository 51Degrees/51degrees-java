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

package fiftyone.ipintelligence;

import fiftyone.ipintelligence.engine.onpremise.flowelements.IPIntelligenceOnPremiseEngine;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.fiftyone.flowelements.SequenceElementBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageElement;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * The on-premise builder adds usage sharing after the engine and after any
 * element the caller adds, as the last Flow Element of the Pipeline.
 * @see <a href="https://github.com/51Degrees/specifications/blob/52e9b047743db365592f9bd1c737fc7ddb6a2de4/pipeline-specification/pipeline-elements/usage-sharing-element.md#position-in-the-pipeline">Specification</a>
 */
public class UsageSharingLastTests {

    /**
     * The free data file, which every build has.
     */
    private static final String ASN_IPI_DATA_FILE_NAME =
        "51Degrees-IPIV4AsnIpiV41.ipi";

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    private static File dataFile() {
        try {
            return getFilePath(ASN_IPI_DATA_FILE_NAME);
        }
        catch (IllegalArgumentException e) {
            assumeTrue(e.getMessage(), false);
            return null;
        }
    }

    private IPIntelligenceOnPremisePipelineBuilder builder()
        throws Exception {
        return new IPIntelligencePipelineBuilder(loggerFactory)
            .useOnPremise(dataFile().getAbsolutePath(), false)
            .setAutoUpdate(false)
            .setDataFileSystemWatcher(false)
            .setShareUsage(true);
    }

    private static void assertLast(List<FlowElement<?, ?>> elements) {
        assertTrue(elements.toString(),
            elements.get(elements.size() - 1) instanceof ShareUsageElement);
    }

    @Test
    public void ShareUsageOn_UsageSharingIsLast() throws Exception {
        try (Pipeline pipeline = builder().build()) {
            List<FlowElement<?, ?>> elements = pipeline.getFlowElements();
            assertEquals(2, elements.size());
            assertTrue(elements.toString(),
                elements.get(0) instanceof IPIntelligenceOnPremiseEngine);
            assertLast(elements);
        }
    }

    /**
     * An element the caller adds after turning usage sharing on still goes
     * before usage sharing, which the builder adds when it builds.
     */
    @Test
    public void ElementAddedAfterShareUsage_UsageSharingIsLast()
        throws Exception {
        try (Pipeline pipeline = builder()
            .addFlowElement(new SequenceElementBuilder(loggerFactory).build())
            .build()) {
            List<FlowElement<?, ?>> elements = pipeline.getFlowElements();
            assertEquals(3, elements.size());
            assertLast(elements);
        }
    }
}
