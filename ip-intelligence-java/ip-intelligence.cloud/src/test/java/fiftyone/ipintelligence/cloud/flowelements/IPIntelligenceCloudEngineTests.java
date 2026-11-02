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

package fiftyone.ipintelligence.cloud.flowelements;

import fiftyone.ipintelligence.cloud.data.IPIntelligenceDataCloud;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.cloudrequestengine.flowelements.CloudRequestEngine;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.ProductMetaData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData.PropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.IWeightedValue;
import fiftyone.pipeline.core.data.WktString;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Offline unit tests for the IP Intelligence specific value handling in
 * {@link IPIntelligenceCloudEngine}: weighted-list parsing, IP address parsing
 * and the {@code "ip"} element data key. The cloud response is mocked so the
 * tests need neither a resource key nor network access.
 */
public class IPIntelligenceCloudEngineTests {

    protected static final ILoggerFactory loggerFactory =
        LoggerFactory.getILoggerFactory();

    @Test
    public void getElementDataKey_isIp() throws Exception {
        try (IPIntelligenceCloudEngine engine =
                 new IPIntelligenceCloudEngineBuilder(loggerFactory).build()) {
            assertEquals("ip", engine.getElementDataKey());
        }
    }

    /**
     * A property the cloud reports with a {@code Weighted...} type is parsed
     * into a list of {@link IWeightedValue}, preserving each value and its
     * raw weighting.
     */
    @Test
    public void weightedProperty_parsedAsWeightedValueList() throws Exception {
        try (IPIntelligenceCloudEngine engine = buildEngine(
                new PropertyMetaData(
                    "countrycodespopulation", "WeightedString", "cat", null))) {

            IPIntelligenceDataCloud data = process(engine,
                "{ 'ip': { 'countrycodespopulation': [" +
                    "{ 'rawweighting': 59439, 'value': 'GB' }," +
                    "{ 'rawweighting': 6097, 'value': 'US' } ] } }");

            AspectPropertyValue<List<IWeightedValue<String>>> value =
                data.getCountryCodesPopulation();
            assertTrue(value.hasValue());
            List<IWeightedValue<String>> weighted = value.getValue();
            assertEquals(2, weighted.size());
            assertEquals("GB", weighted.get(0).getValue());
            assertEquals(59439, weighted.get(0).getRawWeighting());
            assertEquals("US", weighted.get(1).getValue());
            assertEquals(6097, weighted.get(1).getRawWeighting());
        }
    }

    /**
     * A weighted property reported as null carries through the cloud's
     * null reason rather than producing an empty list.
     */
    @Test
    public void weightedProperty_nullValue_usesNullReason() throws Exception {
        try (IPIntelligenceCloudEngine engine = buildEngine(
                new PropertyMetaData(
                    "countrycodespopulation", "WeightedString", "cat", null))) {

            IPIntelligenceDataCloud data = process(engine,
                "{ 'ip': { 'countrycodespopulation': null," +
                    " 'countrycodespopulationnullreason': 'no value available' } }");

            AspectPropertyValue<List<IWeightedValue<String>>> value =
                data.getCountryCodesPopulation();
            assertFalse(value.hasValue());
            assertEquals("no value available", value.getNoValueMessage());
        }
    }

    /**
     * A property the cloud reports with the {@code IPAddress} type is parsed
     * into an {@link InetAddress}, so the typed getter returns the declared
     * type rather than a raw string.
     */
    @Test
    public void ipAddressProperty_parsedAsInetAddress() throws Exception {
        try (IPIntelligenceCloudEngine engine = buildEngine(
                new PropertyMetaData("ip", "IPAddress", "cat", null))) {

            IPIntelligenceDataCloud data = process(engine,
                "{ 'ip': { 'ip': '1.2.3.4' } }");

            AspectPropertyValue<InetAddress> value = data.getIp();
            assertTrue(value.hasValue());
            assertEquals(InetAddress.getByName("1.2.3.4"), value.getValue());
        }
    }

    /**
     * A property the cloud reports with the {@code WktString} type (e.g.
     * {@code Areas}) is wrapped in a {@link WktString}. The cloud serialises it
     * as an object that holds the text under a {@code "value"} key, so this also
     * exercises that the text is extracted rather than the object stringified.
     */
    @Test
    public void wktStringProperty_parsedAsWktString() throws Exception {
        try (IPIntelligenceCloudEngine engine = buildEngine(
                new PropertyMetaData("areas", "WktString", "cat", null))) {

            IPIntelligenceDataCloud data = process(engine,
                "{ 'ip': { 'areas': { 'value': 'POLYGON((1 2,3 4,5 6,1 2))' } } }");

            AspectPropertyValue<WktString> value = data.getAreas();
            assertTrue(value.hasValue());
            assertEquals("POLYGON((1 2,3 4,5 6,1 2))", value.getValue().getValue());
        }
    }

    /**
     * A property the cloud reports with the {@code Single} type (the .NET
     * 32-bit float used for e.g. Latitude/Longitude) is parsed into a
     * {@link Float}, so the typed getter returns the declared type rather than
     * throwing on an unknown type.
     */
    @Test
    public void singleProperty_parsedAsFloat() throws Exception {
        try (IPIntelligenceCloudEngine engine = buildEngine(
                new PropertyMetaData("latitude", "Single", "cat", null))) {

            IPIntelligenceDataCloud data = process(engine,
                "{ 'ip': { 'latitude': 42.447 } }");

            AspectPropertyValue<Float> value = data.getLatitude();
            assertTrue(value.hasValue());
            assertEquals(42.447f, value.getValue(), 0.0001f);
        }
    }

    /**
     * Build an engine whose properties are loaded from a mocked cloud
     * {@code getPublicProperties()} response under the {@code "ip"} product,
     * exercising the real {@code loadAspectProperties}/{@code resolvePropertyType}
     * path.
     */
    private IPIntelligenceCloudEngine buildEngine(PropertyMetaData... properties)
        throws Exception {
        IPIntelligenceCloudEngine engine =
            new IPIntelligenceCloudEngineBuilder(loggerFactory).build();

        ProductMetaData product = new ProductMetaData();
        product.dataTier = "test";
        product.properties = Arrays.asList(properties);
        Map<String, ProductMetaData> publicProperties = new HashMap<>();
        publicProperties.put("ip", product);

        CloudRequestEngine requestEngine = mock(CloudRequestEngine.class);
        when(requestEngine.getPublicProperties()).thenReturn(publicProperties);

        Pipeline pipeline = mock(Pipeline.class);
        when(pipeline.getElement(CloudRequestEngine.class)).thenReturn(requestEngine);

        engine.addPipeline(pipeline);
        return engine;
    }

    /**
     * Run {@code processEngine} against a mocked cloud JSON response and return
     * the populated data instance.
     */
    private IPIntelligenceDataCloud process(
        IPIntelligenceCloudEngine engine,
        String json) {
        CloudRequestData cloudData = mock(CloudRequestData.class);
        when(cloudData.getJsonResponse()).thenReturn(json);

        FlowData flowData = mock(FlowData.class);
        when(flowData.getFromElement(any(CloudRequestEngine.class)))
            .thenReturn(cloudData);

        IPIntelligenceDataCloud data = new IPIntelligenceDataCloudInternal(
            loggerFactory.getLogger(
                IPIntelligenceDataCloud.class.getSimpleName()),
            flowData,
            engine,
            mock(MissingPropertyService.class));

        engine.processEngine(flowData, data);
        return data;
    }
}
