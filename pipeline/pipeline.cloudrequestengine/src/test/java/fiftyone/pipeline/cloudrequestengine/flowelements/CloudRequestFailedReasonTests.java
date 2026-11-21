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

package fiftyone.pipeline.cloudrequestengine.flowelements;

import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElement;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import fiftyone.pipeline.engines.flowelements.AspectEngineBase;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A Cloud Aspect Engine's property is missing with the CloudRequestFailed
 * reason when the Cloud Request Engine's request for the Flow Data failed,
 * naming the first engine of the Element Data, and when the engine's
 * metadata is not loaded because the request for the property list failed.
 */
public class CloudRequestFailedReasonTests {

    private static final String CLOUD_REQUEST_FAILED =
        "This is because an upstream cloud request failed, so no data was " +
        "available to populate the property.";

    private final TestLoggerFactory loggerFactory =
        new TestLoggerFactory(null);

    public static class TestData extends AspectDataBase {
        public TestData(
            Logger logger,
            FlowData flowData,
            AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
            super(
                logger,
                flowData,
                engine,
                MissingPropertyServiceDefault.getInstance());
        }
    }

    private class TestAspectEngine extends CloudAspectEngineBase<TestData> {
        TestAspectEngine() {
            super(
                loggerFactory.getLogger("TestAspectEngine"),
                new ElementDataFactory<TestData>() {
                    @Override
                    public TestData create(
                        FlowData flowData,
                        FlowElement<TestData, ?> flowElement) {
                        return new TestData(
                            loggerFactory.getLogger("TestData"),
                            flowData,
                            (AspectEngine<TestData, ?>) flowElement);
                    }
                });
        }

        @Override
        public String getElementDataKey() {
            return "test";
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return new EvidenceKeyFilterWhitelist(new ArrayList<String>());
        }

        @Override
        protected void unmanagedResourcesCleanup() {
        }

        @Override
        protected void processCloudEngine(
            FlowData data,
            TestData aspectData,
            String json) {
        }
    }

    /**
     * A Cloud Request Engine whose request fails, or answers with no part
     * for the test engine, and which has the given accessible properties.
     */
    private class TestRequestEngine
        extends AspectEngineBase<CloudRequestData, AspectPropertyMetaData>
        implements CloudRequestEngine {
        private final boolean fail;
        private final Map<String, AccessiblePropertyMetaData.ProductMetaData>
            products;

        TestRequestEngine(
            boolean fail,
            Map<String, AccessiblePropertyMetaData.ProductMetaData> products) {
            super(
                loggerFactory.getLogger("TestRequestEngine"),
                new ElementDataFactory<CloudRequestData>() {
                    @Override
                    public CloudRequestData create(
                        FlowData flowData,
                        FlowElement<CloudRequestData, ?> flowElement) {
                        return new CloudRequestData(
                            loggerFactory.getLogger("CloudRequestData"),
                            flowData,
                            (AspectEngine<CloudRequestData, ?>) flowElement);
                    }
                });
            this.fail = fail;
            this.products = products;
        }

        @Override
        public Map<String, AccessiblePropertyMetaData.ProductMetaData>
        getPublicProperties() {
            return products;
        }

        @Override
        public String getDataSourceTier() {
            return "";
        }

        @Override
        public String getElementDataKey() {
            return "cloud";
        }

        @Override
        public EvidenceKeyFilter getEvidenceKeyFilter() {
            return new EvidenceKeyFilterWhitelist(new ArrayList<String>());
        }

        @Override
        public List<AspectPropertyMetaData> getProperties() {
            return Collections.emptyList();
        }

        @Override
        protected void unmanagedResourcesCleanup() {
        }

        @Override
        protected void processEngine(
            FlowData data,
            CloudRequestData aspectData) throws IOException {
            aspectData.setProcessStarted(true);
            if (fail) {
                throw new IOException("The cloud service could not be reached.");
            }
            aspectData.setJsonResponse("{ \"other\": {} }");
        }
    }

    @Test
    public void RequestFailed_CloudRequestFailed() throws Exception {
        PropertyMissingException e = readMissing(true, products());

        assertEquals("CloudRequestFailed", e.getReason().name(),
            e.getMessage());
        assertEquals(
            "Property 'ismobile' not found in data for element 'test'. " +
                CLOUD_REQUEST_FAILED,
            e.getMessage());
    }

    @Test
    public void RequestSucceeded_NotCloudRequestFailed() throws Exception {
        PropertyMissingException e = readMissing(false, products());

        assertEquals(
            "PropertyNotAccessibleWithResourceKey",
            e.getReason().name(),
            e.getMessage());
    }

    @Test
    public void PropertyListRequestFailed_CloudRequestFailed()
        throws Exception {
        PropertyMissingException e = readMissing(false, null);

        assertEquals("CloudRequestFailed", e.getReason().name(),
            e.getMessage());
        assertEquals(
            "Property 'ismobile' not found in data for element 'test'. " +
                CLOUD_REQUEST_FAILED,
            e.getMessage());
    }

    /**
     * Process one Flow Data, with process exceptions suppressed, and read
     * the property the response does not hold.
     */
    private PropertyMissingException readMissing(
        boolean fail,
        Map<String, AccessiblePropertyMetaData.ProductMetaData> products)
        throws Exception {
        TestAspectEngine engine = new TestAspectEngine();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(new TestRequestEngine(fail, products))
                .addFlowElement(engine)
                .build();
             FlowData flowData = pipeline.createFlowData()) {
            flowData.addEvidence("query.user-agent", "iPhone");
            flowData.process();
            TestData data = flowData.getFromElement(engine);
            return assertThrows(
                PropertyMissingException.class,
                () -> data.get("ismobile"));
        }
    }

    /**
     * Products holding one test property, so that the engine's metadata is
     * loaded and does not list the property read.
     */
    private static Map<String, AccessiblePropertyMetaData.ProductMetaData>
    products() {
        List<AccessiblePropertyMetaData.PropertyMetaData> properties =
            new ArrayList<>();
        properties.add(new AccessiblePropertyMetaData.PropertyMetaData(
            "hardwarevendor", "String", null, null));
        AccessiblePropertyMetaData.ProductMetaData product =
            new AccessiblePropertyMetaData.ProductMetaData();
        product.properties = properties;
        Map<String, AccessiblePropertyMetaData.ProductMetaData> products =
            new HashMap<>();
        products.put("test", product);
        return products;
    }
}
