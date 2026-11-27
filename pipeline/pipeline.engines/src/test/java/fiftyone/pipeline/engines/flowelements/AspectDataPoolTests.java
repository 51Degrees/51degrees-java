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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.data.ProcessCallable;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.services.MissingPropertyReason;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * Aspect Data that opts in to pooling starts each request with nothing of
 * the request it served before. Besides its values, that is the engines that
 * shared it, the process tasks added to it and a failed cloud request.
 * @see <a href="https://github.com/51Degrees/specifications/blob/52e9b047/pipeline-specification/features/resource-cleanup.md#reusing-element-data">Specification</a>
 */
public class AspectDataPoolTests {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(AspectDataPoolTests.class);

    private static final String FAIL_EVIDENCE = "query.fail";

    public static class PooledAspectData extends AspectDataBase {
        public PooledAspectData(
            Logger logger,
            FlowData flowData,
            AspectEngine<? extends AspectData,
                ? extends AspectPropertyMetaData> engine) {
            super(
                logger,
                flowData,
                engine,
                MissingPropertyServiceDefault.getInstance());
        }

        @Override
        protected boolean isPoolable() {
            return true;
        }
    }

    /**
     * Writes valueone. Given the fail Evidence, it instead leaves everything
     * a request can leave in Aspect Data besides values: a second engine
     * sharing it, a process task that failed and a failed cloud request.
     */
    private static class PoolEngine
        extends AspectEngineBase<PooledAspectData, AspectPropertyMetaData> {

        @Override
        protected Class<PooledAspectData> getDataType() {
            return PooledAspectData.class;
        }
        private final List<AspectPropertyMetaData> properties;
        private final PoolEngine partner;

        PoolEngine(PoolEngine partner) {
            super(LOGGER, (flowData, element) -> new PooledAspectData(
                LOGGER,
                flowData,
                (AspectEngine<PooledAspectData, ?>) element));
            this.partner = partner;
            properties = Arrays.asList(
                (AspectPropertyMetaData) new AspectPropertyMetaDataDefault(
                    "valueone", this, "", int.class,
                    new ArrayList<String>(), true),
                (AspectPropertyMetaData) new AspectPropertyMetaDataDefault(
                    "valuetwo", this, "", int.class,
                    new ArrayList<String>(), true));
        }

        @Override
        protected void processEngine(
            FlowData data,
            PooledAspectData aspectData) {
            if (data.getEvidence().get(FAIL_EVIDENCE) != null) {
                aspectData.addEngine(partner);
                aspectData.addProcessCallable(new ProcessCallable(this) {
                    @Override
                    public Void call() throws Exception {
                        throw new Exception("The task failed.");
                    }
                });
                aspectData.setCloudRequestFailed();
            } else {
                aspectData.put("valueone", 1);
            }
        }

        @Override
        public List<AspectPropertyMetaData> getProperties() {
            return properties;
        }

        @Override
        public String getDataSourceTier() {
            return "test";
        }

        @Override
        public String getElementDataKey() {
            return "pooled-aspect";
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return new EvidenceKeyFilterWhitelist(
                Collections.singletonList(FAIL_EVIDENCE));
        }

        @Override
        protected void unmanagedResourcesCleanup() {
        }
    }

    private static PooledAspectData process(
        Pipeline pipeline,
        PoolEngine engine,
        boolean fail) throws Exception {
        try (FlowData data = pipeline.createFlowData()) {
            if (fail) {
                data.addEvidence(FAIL_EVIDENCE, "yes");
            }
            data.process();
            return data.getFromElement(engine);
        }
    }

    @Test
    public void reusedAspectData_KeepsNothingOfTheRequestBefore()
        throws Exception {
        PoolEngine partner = new PoolEngine(null);
        PoolEngine engine = new PoolEngine(partner);
        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .addFlowElement(engine)
                .setAutoCloseElements(true)
                .build()) {
            PooledAspectData failed = process(pipeline, engine, true);

            try (FlowData data = pipeline.createFlowData()) {
                data.process();
                PooledAspectData reused = data.getFromElement(engine);
                assertSame(
                    "The instance was not reused, so this proves nothing",
                    failed,
                    reused);
                assertEquals(
                    "It kept the engines of the request before",
                    Collections.singletonList(engine),
                    reused.getEngines());
                assertEquals(
                    "It kept the failed task of the request before",
                    1,
                    reused.get("valueone"));
                try {
                    reused.get("valuetwo");
                    fail("valuetwo was never written");
                } catch (PropertyMissingException e) {
                    assertNotEquals(
                        "It kept the failed cloud request of the request " +
                            "before",
                        MissingPropertyReason.CloudRequestFailed,
                        e.getReason());
                }
            }
        } finally {
            partner.close();
        }
    }
}
