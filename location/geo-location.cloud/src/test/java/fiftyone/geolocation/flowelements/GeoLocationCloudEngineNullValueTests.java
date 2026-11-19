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
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
import fiftyone.pipeline.engines.services.MissingPropertyServiceDefault;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A property the cloud response gives as null has no value, and its message
 * is the text under the property's name followed by nullreason, as the
 * Processing section of the cloud aspect engine specification says. A
 * property the response leaves out is missing, so reading it throws, as the
 * Missing Properties section of the properties specification says. The tests
 * read each property by name, from the map the JSON Builder reads, and from
 * the typed getter.
 */
public class GeoLocationCloudEngineNullValueTests {

    private static final String SUBURB_NULL_REASON =
        "No suburb was found at this location.";

    private static final String NULL_SUBURB_RESPONSE =
        "{ 'location': { " +
            "'suburb': null, " +
            "'suburbnullreason': '" + SUBURB_NULL_REASON + "', " +
            "'town': 'Reading' }, " +
        "'javascriptProperties': [] }";

    private static final String NULL_SUBURB_WITHOUT_REASON_RESPONSE =
        "{ 'location': { 'suburb': null, 'town': 'Reading' }, " +
        "'javascriptProperties': [] }";

    private static final String NO_SUBURB_RESPONSE =
        "{ 'location': { 'town': 'Reading' }, " +
        "'javascriptProperties': [] }";

    private final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    @Test
    public void NullValue_NoValueWithNullReason() throws Exception {
        try (GeoLocationCloudEngine engine = buildEngine()) {
            CloudGeoData data = process(engine, NULL_SUBURB_RESPONSE);

            assertNoValue(SUBURB_NULL_REASON, data.get("Suburb"));
            assertNoValue(SUBURB_NULL_REASON, data.asKeyMap().get("Suburb"));
            assertNoValue(SUBURB_NULL_REASON, data.getSuburb());
        }
    }

    @Test
    public void Value_HasValue() throws Exception {
        try (GeoLocationCloudEngine engine = buildEngine()) {
            CloudGeoData data = process(engine, NULL_SUBURB_RESPONSE);

            assertValue("Reading", data.get("Town"));
            assertValue("Reading", data.asKeyMap().get("Town"));
            assertValue("Reading", data.getTown());
        }
    }

    @Test
    public void NullValueWithoutReason_NoValueUnknown() throws Exception {
        try (GeoLocationCloudEngine engine = buildEngine()) {
            CloudGeoData data = process(
                engine,
                NULL_SUBURB_WITHOUT_REASON_RESPONSE);

            assertNoValue("Unknown", data.get("Suburb"));
            assertNoValue("Unknown", data.asKeyMap().get("Suburb"));
            assertNoValue("Unknown", data.getSuburb());
        }
    }

    @Test
    public void NoEntry_PropertyIsMissing() throws Exception {
        try (GeoLocationCloudEngine engine = buildEngine()) {
            CloudGeoData data = process(engine, NO_SUBURB_RESPONSE);

            assertFalse(data.asKeyMap().containsKey("Suburb"));
            assertMissing(() -> data.get("Suburb"));
            assertMissing(() -> data.getSuburb());
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void SetNoValueReasons_ChangesNothing() throws Exception {
        try (GeoLocationCloudEngine engine = buildEngine()) {
            CloudGeoData data = process(engine, NULL_SUBURB_RESPONSE);
            Map<String, String> reasons = new HashMap<>();
            reasons.put("suburb", "Another reason");
            data.setNoValueReasons(reasons);

            assertNoValue(SUBURB_NULL_REASON, data.get("Suburb"));
            assertNoValue(SUBURB_NULL_REASON, data.getSuburb());
        }
    }

    private static void assertMissing(Runnable read) {
        try {
            read.run();
            fail("Expected a PropertyMissingException");
        }
        catch (PropertyMissingException e) {
            // The property is not in the result, as expected.
        }
    }

    private static AspectPropertyValue<?> asAspectPropertyValue(Object value) {
        assertTrue(
            "Expected an AspectPropertyValue but found " +
                (value == null ? "null" : value.getClass().getName()),
            value instanceof AspectPropertyValue);
        return (AspectPropertyValue<?>) value;
    }

    private static void assertNoValue(String reason, Object value) {
        AspectPropertyValue<?> propertyValue = asAspectPropertyValue(value);
        assertFalse(
            "Expected no value but found " + propertyValue,
            propertyValue.hasValue());
        assertEquals(reason, propertyValue.getNoValueMessage());
    }

    private static void assertValue(Object expected, Object value)
        throws Exception {
        AspectPropertyValue<?> propertyValue = asAspectPropertyValue(value);
        assertTrue(
            "Expected a value but found none, because " +
                propertyValue.getNoValueMessage(),
            propertyValue.hasValue());
        assertEquals(expected, propertyValue.getValue());
    }

    /**
     * An engine whose Cloud Request Engine reports the Location product with
     * the Suburb and Town properties.
     */
    private GeoLocationCloudEngine buildEngine() throws Exception {
        GeoLocationCloudEngine engine =
            new GeoLocationCloudEngineBuilder(loggerFactory)
                .build(Enums.GeoLocationProvider.FiftyOneDegrees);

        ProductMetaData product = new ProductMetaData();
        product.dataTier = "test";
        product.properties = Arrays.asList(
            new PropertyMetaData("Suburb", "String", "Location", null),
            new PropertyMetaData("Town", "String", "Location", null));
        Map<String, ProductMetaData> publicProperties = new HashMap<>();
        publicProperties.put("location", product);

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
}
