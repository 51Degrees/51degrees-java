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

package fiftyone.geolocation.flowelements;

import fiftyone.geolocation.core.Enums;
import fiftyone.geolocation.data.CloudGeoData;
import fiftyone.geolocation.data.CloudGeoDataDefault;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.ProductMetaData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.PropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.services.MissingPropertyReason;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A resource key can be valid and include no product that holds the
 * Location properties. The engine then builds with empty property
 * metadata, logs the missing product once, and logs or throws nothing on
 * each request.
 */
public class GeoLocationCloudEngineTests {

    private Logger logger;
    private ILoggerFactory loggerFactory;

    @Before
    public void init() {
        logger = mock(Logger.class);
        loggerFactory = mock(ILoggerFactory.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);
    }

    @Test
    public void ProductMissing_BuildsWithEmptyMetadata() throws Exception {
        try (GeoLocationCloudEngine engine = buildEngine()) {
            assertNotNull(engine.getProperties());
            assertEquals(0, engine.getProperties().size());
            List<String> logged = warningsAndErrors();
            assertEquals(logged.toString(), 1, logged.size());
            assertTrue(logged.get(0), logged.get(0).startsWith("warn "));
            assertTrue(logged.get(0), logged.get(0).contains("'location'"));
        }
    }

    @Test
    public void ProductMissing_ProcessLogsAndThrowsNothing() throws Exception {
        try (GeoLocationCloudEngine engine = buildEngine()) {
            for (int i = 0; i < 3; i++) {
                process(engine);
            }
            List<String> logged = warningsAndErrors();
            assertEquals(logged.toString(), 1, logged.size());
        }
    }

    /**
     * With the Location product included, a response that holds no
     * part for it leaves the element data without values and throws
     * nothing, as the Processing section of the cloud aspect engine
     * specification says.
     */
    @Test
    public void ProductPresent_ResponseWithoutPart_NoValues()
        throws Exception {
        try (GeoLocationCloudEngine engine = buildEngine("location", "town")) {
            CloudGeoData data = process(
                engine,
                "{ 'ip': { 'registeredname': 'Example' }, " +
                    "'javascriptProperties': [] }");
            assertFalse(data.getTown().hasValue());
        }
    }

    /**
     * Reading a property behaves as for any property the response does not
     * carry.
     */
    @Test
    public void ProductMissing_PropertyIsMissing() throws Exception {
        try (GeoLocationCloudEngine engine = buildEngine()) {
            CloudGeoData data = process(engine);
            try {
                data.getTown();
                fail("A PropertyMissingException should have been thrown");
            } catch (PropertyMissingException e) {
                assertEquals(
                    MissingPropertyReason.ProductNotAccessibleWithResourceKey,
                    e.getReason());
            }
        }
    }

    /**
     * After a failure at build that can pass, the Cloud Request Engine does
     * not know the accessible products yet. The engine builds without
     * logging anything, and reads its metadata once the products are known
     * rather than keeping none.
     */
    @Test
    public void DiscoveryNotComplete_LoadsTheMetadataLater() throws Exception {
        ProductMetaData product = new ProductMetaData();
        product.dataTier = "test";
        product.properties = Arrays.asList(
            new PropertyMetaData("town", "String", "cat", null));
        Map<String, ProductMetaData> products = new HashMap<>();
        products.put("location", product);
        CloudRequestEngine requestEngine = mock(CloudRequestEngine.class);
        when(requestEngine.getPublicProperties())
            .thenReturn(null)
            .thenReturn(products);
        Pipeline pipeline = mock(Pipeline.class);
        when(pipeline.getElement(CloudRequestEngine.class))
            .thenReturn(requestEngine);

        try (GeoLocationCloudEngine engine =
            new GeoLocationCloudEngineBuilder(loggerFactory)
                .build(Enums.GeoLocationProvider.FiftyOneDegrees)) {
            engine.addPipeline(pipeline);
            List<String> logged = warningsAndErrors();
            assertEquals(logged.toString(), 0, logged.size());

            assertEquals(1, engine.getProperties().size());
            assertEquals("town", engine.getProperties().get(0).getName());
        }
    }

    /**
     * An engine whose Cloud Request Engine reports one product, holding
     * one string property.
     */
    // thenReturn has a varargs overload, so a generic Map is an unchecked array.
    @SuppressWarnings("unchecked")
    private GeoLocationCloudEngine buildEngine(
        String productKey,
        String property) throws Exception {
        GeoLocationCloudEngine engine =
            new GeoLocationCloudEngineBuilder(loggerFactory)
                .build(Enums.GeoLocationProvider.FiftyOneDegrees);

        ProductMetaData product = new ProductMetaData();
        product.dataTier = "test";
        product.properties = Arrays.asList(
            new PropertyMetaData(property, "String", "cat", null));
        Map<String, ProductMetaData> publicProperties = new HashMap<>();
        publicProperties.put(productKey, product);

        CloudRequestEngine requestEngine = mock(CloudRequestEngine.class);
        when(requestEngine.getPublicProperties()).thenReturn(publicProperties);

        Pipeline pipeline = mock(Pipeline.class);
        when(pipeline.getElement(CloudRequestEngine.class))
            .thenReturn(requestEngine);

        engine.addPipeline(pipeline);
        return engine;
    }

    /**
     * Process the given cloud response.
     */
    private CloudGeoData process(
        GeoLocationCloudEngine engine,
        String response) {
        CloudRequestData cloudData = mock(CloudRequestData.class);
        when(cloudData.getJsonResponse()).thenReturn(response);

        FlowData flowData = mock(FlowData.class);
        when(flowData.getFromElement(any(CloudRequestEngine.class)))
            .thenReturn(cloudData);

        CloudGeoData data = new CloudGeoDataDefault(
            loggerFactory.getLogger(CloudGeoData.class.getSimpleName()),
            flowData,
            engine,
            MissingPropertyServiceDefault.getInstance());
        engine.processEngine(flowData, data);
        return data;
    }

    /**
     * An engine on a resource key that includes IP Intelligence alone.
     */
    private GeoLocationCloudEngine buildEngine() throws Exception {
        return buildEngine("ip", "registeredname");
    }

    /**
     * Process a cloud response that holds nothing for the Location
     * product.
     */
    private CloudGeoData process(GeoLocationCloudEngine engine) {
        return process(
            engine,
            "{ 'ip': { 'registeredname': 'Example' }, " +
                "'javascriptProperties': [] }");
    }

    /**
     * Every warning and error logged, at any overload.
     */
    private List<String> warningsAndErrors() {
        List<String> logged = new ArrayList<>();
        for (Invocation invocation :
            Mockito.mockingDetails(logger).getInvocations()) {
            String level = invocation.getMethod().getName();
            if (level.equals("warn") || level.equals("error")) {
                logged.add(level + " " +
                    Arrays.toString(invocation.getArguments()));
            }
        }
        return Collections.unmodifiableList(logged);
    }
}
