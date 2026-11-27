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

import fiftyone.pipeline.core.exceptions.PipelineConfigurationException;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageBuilder;
import fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageElement;
import fiftyone.pipeline.engines.services.DataUpdateServiceDefault;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import static fiftyone.pipeline.util.FileFinder.getFilePath;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * When the on-premise builder's Pipeline is refused, the builder closes the
 * Flow Elements it made itself, because no caller holds them to close, and
 * leaves the caller's own Flow Elements open.
 * @see <a href="https://github.com/51Degrees/specifications/blob/52e9b047743db365592f9bd1c737fc7ddb6a2de4/pipeline-specification/features/resource-cleanup.md">Specification</a>
 */
public class RefusedBuildTests {

    /**
     * The free data file, which every build has.
     */
    private static final String ASN_IPI_DATA_FILE_NAME =
        "51Degrees-IPIV4AsnIpiV41.ipi";

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    /**
     * Records every Flow Element the builder adds to its own list, which are
     * the ones it made.
     */
    private static class RecordingBuilder
        extends IPIntelligenceOnPremisePipelineBuilder {
        private final List<FlowElement<?, ?>> made = new ArrayList<>();

        RecordingBuilder(ILoggerFactory loggerFactory) {
            super(
                loggerFactory,
                new DataUpdateServiceDefault(
                    loggerFactory.getLogger(
                        DataUpdateServiceDefault.class.getName()),
                    new HttpClientDefault()));
        }

        List<FlowElement<?, ?>> elements() {
            return super.getFlowElements();
        }

        @Override
        protected List<FlowElement<?, ?>> getFlowElements() {
            final List<FlowElement<?, ?>> elements = super.getFlowElements();
            return new AbstractList<FlowElement<?, ?>>() {
                @Override
                public FlowElement<?, ?> get(int index) {
                    return elements.get(index);
                }

                @Override
                public int size() {
                    return elements.size();
                }

                @Override
                public void add(int index, FlowElement<?, ?> element) {
                    made.add(element);
                    elements.add(index, element);
                }

                @Override
                public FlowElement<?, ?> set(int index, FlowElement<?, ?> element) {
                    return elements.set(index, element);
                }

                @Override
                public FlowElement<?, ?> remove(int index) {
                    return elements.remove(index);
                }
            };
        }
    }

    private static File dataFile() {
        try {
            return getFilePath(ASN_IPI_DATA_FILE_NAME);
        }
        catch (IllegalArgumentException e) {
            assumeTrue(e.getMessage(), false);
            return null;
        }
    }

    /**
     * Usage sharing the caller adds goes before the engine and the usage
     * sharing element the builder adds, so the Pipeline is refused.
     */
    @Test
    public void Refused_ElementsTheBuilderMadeAreClosed() throws Exception {
        RecordingBuilder builder = new RecordingBuilder(loggerFactory);
        builder.setFilename(dataFile().getAbsolutePath(), false)
            .setAutoUpdate(false)
            .setDataFileSystemWatcher(false);
        ShareUsageElement callers =
            new ShareUsageBuilder(loggerFactory).build();
        try {
            builder.addFlowElement(callers);
            try {
                builder.build().close();
                fail("A Pipeline with usage sharing first was built");
            } catch (PipelineConfigurationException e) {
                assertTrue(e.getMessage(),
                    e.getMessage().contains("must be the last Flow Element"));
            }

            assertFalse("The builder made no Flow Elements",
                builder.made.isEmpty());
            for (FlowElement<?, ?> element : builder.made) {
                assertTrue(element + " that the builder made is open",
                    element.isClosed());
                assertFalse(element + " is still in the builder's list",
                    builder.elements().contains(element));
            }
            assertFalse("The caller's own Flow Element was closed",
                callers.isClosed());
        } finally {
            callers.close();
        }
    }
}
