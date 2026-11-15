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

import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.HttpClientDefault;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Calls the live 51Degrees cloud service through the cloud request
 * engine, end to end.
 * <p>
 * Every other test in this module answers the engine from a mock, which
 * checks the request the engine builds and the way it reads a reply, and
 * proves nothing about the service on the other end. A field the service
 * stops writing, a header it starts refusing or an endpoint that moves
 * all leave those tests green. This one makes the three calls the engine
 * really makes, which are accessible properties and evidence keys when it
 * is built, and the JSON request when it processes, so a change in the
 * service is seen here.
 * <p>
 * {@code _51DEGREES_CLOUD_ENDPOINT} points the engine at another host,
 * which the builder reads for itself.
 */
public class CloudRequestEngineLiveTests {

    /**
     * A real browser User-Agent, so the service has something to detect
     * rather than a string it will report as unknown.
     */
    private static final String USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        + "(KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36";

    /** A property every device data file has. */
    private static final String IS_MOBILE = "device.ismobile";

    /** A name no product has, which the service leaves out of its answer. */
    private static final String NOT_A_PROPERTY = "device.notaproperty51d";

    @Test
    public void build_Process_ReadsDeviceDataFromTheLiveService()
            throws Exception {
        String resourceKey =
            LiveCloudKeys.find(LiveCloudKeys.RESOURCE_KEY_NAMES);
        if (resourceKey == null) {
            fail(LiveCloudKeys.noResourceKey());
        }

        CloudRequestEngineBuilder builder = new CloudRequestEngineBuilder(
            LoggerFactory.getILoggerFactory(), new HttpClientDefault())
            .setResourceKey(resourceKey);
        String licenceKey =
            LiveCloudKeys.find(LiveCloudKeys.LICENCE_KEY_NAMES);
        if (licenceKey != null) {
            builder.setLicenseKey(licenceKey);
        }

        // Building asks the service for its accessible properties and its
        // evidence keys, so a service that cannot be reached, or a key it
        // refuses, fails here.
        CloudRequestEngine engine = builder.build();

        Map<String, AccessiblePropertyMetaData.ProductMetaData> products =
            engine.getPublicProperties();
        assertNotNull(products,
            "The live cloud service returned no accessible properties at "
            + "all, which is the service or the key rather than this "
            + "package.");
        assertFalse(products.isEmpty(),
            "The live cloud service listed no products for this resource "
            + "key. A key carries the products its account holds, so this "
            + "is an entitlement problem and not a fault in the service. "
            + "Create a key at https://51degrees.com/configure.");
        assertTrue(products.containsKey("device"),
            "The live cloud service listed the products "
            + String.join(", ", products.keySet()) + " for this resource "
            + "key, and none of them is the device product this test asks "
            + "for. That is the key not carrying the device product rather "
            + "than a fault in the service.");

        try (Pipeline pipeline = new PipelineBuilder(
                LoggerFactory.getILoggerFactory())
                .addFlowElement(engine)
                .build()) {
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("header.user-agent", USER_AGENT).process();

                CloudRequestData requestData = data.getFromElement(engine);
                assertNotNull(requestData,
                    "The cloud request engine put no data in the flow, so "
                    + "the request to the live cloud service produced "
                    + "nothing to read.");
                String json = requestData.getJsonResponse();
                assertNotNull(json,
                    "The live cloud service returned no JSON body for a "
                    + "request carrying a User-Agent.");

                JSONObject answer = new JSONObject(json);
                assertTrue(answer.has("device"),
                    "The live cloud service answered with " + json
                    + ", which carries no device element. The resource key "
                    + "lists the device product, so this is the service "
                    + "rather than entitlement.");
                JSONObject device = answer.getJSONObject("device");
                assertTrue(device.length() > 0,
                    "The live cloud service returned an empty device "
                    + "element for a real browser User-Agent.");
            }
        }
    }

    /**
     * Authenticates on a license key alone, with no resource key, and
     * names the properties wanted. The service then answers only those
     * properties. One the license does not grant comes back as null with a
     * reason, and a name that is not a property is left out without the
     * service saying so, which the engine has to report itself, once.
     * <p>
     * Skipped when no license key is set, or the one set grants no cloud
     * product, because a resource key cannot stand in for one. No message
     * here holds the service's answer, only the names of its fields, so
     * that nothing the service says about the key reaches a build log.
     */
    @Test
    public void build_Process_LicenseKeyAlone_AnswersOnlyWhatWasAsked()
            throws Exception {
        String licenceKey =
            LiveCloudKeys.find(LiveCloudKeys.LICENCE_KEY_NAMES);
        assumeTrue(licenceKey != null,
            "None of " + String.join(", ", LiveCloudKeys.LICENCE_KEY_NAMES)
            + " is set, so there is no license key to authenticate with.");

        // The service answers a request that names no property the license
        // grants with an error, so one granted property is asked for too.
        // Discovery under the license key lists what the license grants.
        String granted;
        try (CloudRequestEngine discovery = new CloudRequestEngineBuilder(
                LoggerFactory.getILoggerFactory(), new HttpClientDefault())
                .setLicenseKey(licenceKey)
                .setRequestedProperties(Collections.singletonList(IS_MOBILE))
                .build()) {
            granted = firstProperty(discovery.getPublicProperties());
        }
        assumeTrue(granted != null,
            "The live cloud service lists no product for this license "
            + "key, so it is not a key for the cloud service.");

        Logger logger = mock(Logger.class);
        ILoggerFactory loggerFactory = mock(ILoggerFactory.class);
        when(loggerFactory.getLogger(anyString())).thenReturn(logger);
        List<String> asked =
            Arrays.asList(granted, IS_MOBILE, NOT_A_PROPERTY);
        CloudRequestEngine engine = new CloudRequestEngineBuilder(
            loggerFactory, new HttpClientDefault())
            .setLicenseKey(licenceKey)
            .setRequestedProperties(asked)
            .build();

        JSONObject answer = null;
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build()) {
            // Twice, because the engine reports a name left out once and
            // not on every request.
            for (int i = 0; i < 2; i++) {
                try (FlowData data = pipeline.createFlowData()) {
                    data.addEvidence("header.user-agent", USER_AGENT)
                        .process();
                    answer = new JSONObject(
                        data.getFromElement(engine).getJsonResponse());
                }
            }
        }

        assertFalse(answer.has("errors"),
            "The live cloud service answered a license key and a list of "
            + "properties with errors. Its answer has the fields "
            + answer.keySet() + ".");
        JSONObject device = answer.optJSONObject("device");
        assertTrue(device != null && device.has("ismobile"),
            "The live cloud service left " + IS_MOBILE + " out of its "
            + "answer to a license key that asked for it. Its answer has "
            + "the fields " + answer.keySet() + ".");
        assertTrue(
            device.isNull("ismobile") == false
                || device.has("ismobilenullreason"),
            "The live cloud service answered " + IS_MOBILE + " as null "
            + "with no reason beside it.");
        for (String element : answer.keySet()) {
            JSONObject values = answer.optJSONObject(element);
            if (values == null) {
                continue;
            }
            for (String property : values.keySet()) {
                assertTrue(
                    wasAsked(asked, element, property),
                    "The live cloud service answered a license key alone "
                    + "with " + element + "." + property + ", which was "
                    + "not asked for.");
            }
        }

        // The name that is not a property is reported once, and the
        // property answered as null is not reported.
        verify(logger, times(1)).warn(argThat(
            (String m) -> m.contains(NOT_A_PROPERTY)
                && m.contains(IS_MOBILE) == false));
    }

    /**
     * The fully qualified name of the first property of the first product
     * the service lists, in name order, or null when it lists none.
     */
    private static String firstProperty(
        Map<String, AccessiblePropertyMetaData.ProductMetaData> products) {
        if (products == null) {
            return null;
        }
        for (Map.Entry<String, AccessiblePropertyMetaData.ProductMetaData>
                product : new TreeMap<>(products).entrySet()) {
            if (product.getValue().properties != null
                && product.getValue().properties.isEmpty() == false) {
                return product.getKey() + "."
                    + product.getValue().properties.get(0).name;
            }
        }
        return null;
    }

    /**
     * True if the property of the element, or the property its null reason
     * belongs to, is one of the names asked for, in any letter case.
     */
    private static boolean wasAsked(
        List<String> asked, String element, String property) {
        String suffix = "nullreason";
        for (String name : asked) {
            if (name.equalsIgnoreCase(element + "." + property)) {
                return true;
            }
            if (property.endsWith(suffix) && name.equalsIgnoreCase(
                    element + "." + property.substring(
                        0, property.length() - suffix.length()))) {
                return true;
            }
        }
        return false;
    }
}
