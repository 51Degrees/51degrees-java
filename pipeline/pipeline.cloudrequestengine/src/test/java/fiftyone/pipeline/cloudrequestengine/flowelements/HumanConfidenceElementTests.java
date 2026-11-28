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

import fiftyone.common.testhelpers.TestLogger;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.FlowError;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.data.AspectPropertyValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;

/**
 * The Cloud Request Engine and the {@link HumanConfidenceElement} redeem a
 * sealed Human Confidence result through the JSON call, so a consumer only
 * reads the HumanConfidence and HumanConfidenceSource Properties. These
 * are the tests the Human Confidence specification lists under "Required
 * tests", apart from the ones for the transaction the package issues.
 */
public class HumanConfidenceElementTests extends CloudRequestEngineTestsBase {

    private static final String SEALED = "SEALED-RESULT-VALUE";
    private static final String LICENSE = "LICENSE-KEY-VALUE-1234";

    /** The form of each JSON call, in order. */
    private final List<Map<String, String>> calls = new ArrayList<>();

    /** The answer to each JSON call, or null to fail the call. */
    private String answer;

    public HumanConfidenceElementTests() throws MalformedURLException {
        super();
    }

    private void answerJsonCalls() throws IOException {
        configureMockedClient();
        doAnswer(invocation -> {
            HttpURLConnection connection = invocation.getArgument(0);
            String path = connection.getURL().getPath();
            if (path.endsWith("properties")) {
                return accessiblePropertiesResponse;
            }
            if (path.endsWith("evidencekeys")) {
                return evidenceKeysResponse;
            }
            calls.add(form(new String(
                (byte[]) invocation.getArgument(2), StandardCharsets.UTF_8)));
            if (answer == null) {
                throw new IOException("The cloud could not be reached.");
            }
            return answer;
        }).when(httpClient).postData(any(), anyMap(), (byte[]) any());
    }

    private static Map<String, String> form(String body) throws Exception {
        Map<String, String> fields = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            int at = pair.indexOf('=');
            fields.put(
                URLDecoder.decode(pair.substring(0, at), "UTF-8"),
                URLDecoder.decode(pair.substring(at + 1), "UTF-8"));
        }
        return fields;
    }

    private CloudRequestEngineBuilder builder() {
        return new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey("resourcekey");
    }

    /** What one request gave, read after processing. */
    private static final class Read {
        String humanConfidence;
        String source;
        String humanConfidenceReason;
        final List<FlowError> errors = new ArrayList<>();
    }

    private Read process(
        CloudRequestEngine engine,
        Map<String, String> evidence) throws Exception {
        Read read = new Read();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(engine)
                .addFlowElement(new HumanConfidenceElementBuilder(loggerFactory)
                    .setRequestEngine(engine).build())
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence(evidence);
            data.process();
            read(data, read);
            data.getErrors().forEach(read.errors::add);
        }
        return read;
    }

    @SuppressWarnings("unchecked")
    private static void read(FlowData data, Read read) {
        AspectPropertyValue<String> value =
            (AspectPropertyValue<String>) data.get(
                HumanConfidenceElement.ELEMENT_DATA_KEY)
                .get(HumanConfidenceElement.HUMAN_CONFIDENCE);
        AspectPropertyValue<String> source =
            (AspectPropertyValue<String>) data.get(
                HumanConfidenceElement.ELEMENT_DATA_KEY)
                .get(HumanConfidenceElement.HUMAN_CONFIDENCE_SOURCE);
        read.humanConfidence = value.hasValue() ? value.getValue() : null;
        read.humanConfidenceReason =
            value.hasValue() ? null : value.getNoValueMessage();
        read.source = source.hasValue() ? source.getValue() : null;
    }

    private static Map<String, String> sealed() {
        Map<String, String> evidence = new LinkedHashMap<>();
        evidence.put("query.User-Agent", "iPhone");
        evidence.put("query.humanconfidence", SEALED);
        return evidence;
    }

    /**
     * A sealed result that redeems gives the Value in the response as
     * HumanConfidence, with HumanConfidenceSource Active, and the call
     * carries the sealed result exactly as it arrived and the License Key.
     */
    @ParameterizedTest
    @ValueSource(strings = { "High", "Medium", "Low" })
    public void redeemedValueIsGivenWithActive(String value) throws Exception {
        answer = "{'derived':{'humanconfidence':'" + value
            + "','humanconfidencesource':'Active'}}";
        answerJsonCalls();

        Read read = process(builder().setLicenseKey(LICENSE).build(), sealed());

        assertEquals(value, read.humanConfidence);
        assertEquals("Active", read.source);
        assertEquals(1, calls.size());
        assertEquals(SEALED, calls.get(0).get("humanconfidence"));
        assertEquals(LICENSE, calls.get(0).get("license"));
        assertTrue(read.errors.isEmpty(), read.errors.toString());
    }

    /**
     * Each answer that does not hold a redeemed Value gives Medium and
     * Active, and the read adds no error of its own.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "{'derived':{'humanconfidence':'Unknown'}}",
        "{'derived':{'humanconfidence':null,"
            + "'humanconfidencenullreason':'Not covered'},"
            + "'errors':['HumanConfidence is not covered']}",
        "{'derived':{'humanconfidence':'high'}}",
        "{'derived':{}}",
        "{'device':{'value':'1'}}",
        "{'derived':{'humanconfidence':'Passive-only'}}",
    })
    public void answerWithoutARedeemedValueGivesMediumAndActive(String body)
        throws Exception {
        answer = body;
        answerJsonCalls();

        Read read = process(builder().setLicenseKey(LICENSE).build(), sealed());

        assertEquals("Medium", read.humanConfidence);
        assertEquals("Active", read.source);
        for (FlowError error : read.errors) {
            assertFalse(error.getFlowElement()
                instanceof HumanConfidenceElement, error.toString());
        }
    }

    /**
     * Without a License Key the sealed result is not redeemed, whatever
     * the answer holds.
     */
    @Test
    public void withoutALicenseKeyTheValueIsMedium() throws Exception {
        answer = "{'derived':{'humanconfidence':'High'}}";
        answerJsonCalls();

        Read read = process(builder().build(), sealed());

        assertEquals("Medium", read.humanConfidence);
        assertEquals("Active", read.source);
    }

    /**
     * A failed request gives Medium and Active. The failure is recorded
     * once, as the Cloud Request Engine's error, and the element adds none.
     */
    @Test
    public void failedRequestGivesMediumAndOneEngineError() throws Exception {
        answer = null;
        answerJsonCalls();
        CloudRequestEngine engine = builder().setLicenseKey(LICENSE).build();

        Read read = process(engine, sealed());

        assertEquals("Medium", read.humanConfidence);
        assertEquals("Active", read.source);
        assertEquals(1, read.errors.size(), read.errors.toString());
        assertTrue(read.errors.get(0).getFlowElement() == engine);
    }

    /**
     * A Pipeline with no Cloud Request Engine gives Medium and Active.
     */
    @Test
    public void withoutACloudRequestEngineTheValueIsMedium() throws Exception {
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(new HumanConfidenceElementBuilder(loggerFactory)
                    .setRequestEngine(null).build())
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence(sealed());
            data.process();
            Read read = new Read();
            read(data, read);
            assertEquals("Medium", read.humanConfidence);
            assertEquals("Active", read.source);
        }
    }

    /**
     * Two reads of HumanConfidence for one request make one request to the
     * remote server.
     */
    @Test
    public void twoReadsMakeOneRequest() throws Exception {
        answer = "{'derived':{'humanconfidence':'High'}}";
        answerJsonCalls();
        CloudRequestEngine engine = builder().setLicenseKey(LICENSE).build();
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .addFlowElement(new HumanConfidenceElementBuilder(loggerFactory)
                    .setRequestEngine(engine).build())
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence(sealed());
            data.process();
            Read first = new Read();
            Read second = new Read();
            read(data, first);
            read(data, second);
            assertEquals("High", first.humanConfidence);
            assertEquals("High", second.humanConfidence);
        }
        assertEquals(1, calls.size());
    }

    /**
     * A request that carries a sealed result is neither stored in nor
     * served from the cache, so the same Evidence twice makes two calls.
     * A request without one is cached as before.
     */
    @Test
    public void sealedRequestIsNotCached() throws Exception {
        answer = "{'derived':{'humanconfidence':'High'}}";
        answerJsonCalls();
        CloudRequestEngine engine = builder()
            .setLicenseKey(LICENSE)
            .setCacheSize(10)
            .build();

        process(engine, sealed());
        process(engine, sealed());
        assertEquals(2, calls.size());

        Map<String, String> plain = new LinkedHashMap<>();
        plain.put("query.User-Agent", "iPhone");
        process(engine, plain);
        process(engine, plain);
        assertEquals(3, calls.size());
    }

    /**
     * Evidence under any prefix whose key strips to transaction or
     * challenge, in any letter case, is left out of a call that carries a
     * sealed result. A call without a sealed result sends it as before.
     */
    @Test
    public void sealedCallHoldsNoTransactionFromTheRequest() throws Exception {
        answer = "{'derived':{'humanconfidence':'High'}}";
        answerJsonCalls();
        CloudRequestEngine engine = builder().setLicenseKey(LICENSE).build();
        Map<String, String> evidence = sealed();
        evidence.put("query.transaction", "from-the-page");
        evidence.put("header.Transaction", "from-a-header");
        evidence.put("cookie.CHALLENGE", "from-a-cookie");

        process(engine, evidence);

        for (String name : calls.get(0).keySet()) {
            assertFalse(name.equalsIgnoreCase("transaction"), name);
            assertFalse(name.equalsIgnoreCase("challenge"), name);
        }

        evidence.remove("query.humanconfidence");
        process(engine, evidence);
        assertEquals("from-the-page", calls.get(1).get("transaction"));
    }

    /**
     * A call that carries a sealed result names derived.humanconfidence,
     * which is added where the configured list does not hold it, and the
     * report of Properties the answer left out does not name it.
     */
    @Test
    public void sealedCallNamesHumanConfidence() throws Exception {
        answer = "{'device':{'value':'1'},"
            + "'derived':{'humanconfidence':'Low'}}";
        answerJsonCalls();
        CloudRequestEngine engine = builder()
            .setLicenseKey(LICENSE)
            .setRequestedProperties("device.value")
            .build();

        Read read = process(engine, sealed());

        assertEquals("Low", read.humanConfidence);
        assertEquals("device.value,derived.humanconfidence",
            calls.get(0).get("values"));
        for (String warning : warnings()) {
            assertFalse(warning.toLowerCase().contains("humanconfidence"),
                warning);
        }
    }

    /**
     * No log entry and no error message holds the sealed result, the
     * License Key, or the transaction, here where two prefixes give the
     * same names and the engine logs the conflict.
     */
    @Test
    public void noLogEntryHoldsTheSealedResultOrTransaction() throws Exception {
        answer = null;
        answerJsonCalls();
        Map<String, String> evidence = sealed();
        evidence.put("cookie.humanconfidence", SEALED + "-COOKIE");
        evidence.put("header.humanconfidence", SEALED + "-HEADER");
        evidence.put("cookie.transaction", "TRANSACTION-VALUE");
        evidence.put("header.transaction", "TRANSACTION-VALUE");

        Read read = process(builder().setLicenseKey(LICENSE).build(), evidence);

        List<String> logged = warnings();
        for (TestLogger logger : loggerFactory.loggers) {
            logged.addAll(logger.errorsLogged);
        }
        for (FlowError error : read.errors) {
            logged.add(String.valueOf(error.getThrowable().getMessage()));
        }
        assertFalse(logged.isEmpty(), "the conflict was logged");
        for (String entry : logged) {
            assertFalse(entry.contains(SEALED), entry);
            assertFalse(entry.contains(LICENSE), entry);
            assertFalse(entry.contains("TRANSACTION-VALUE"), entry);
        }
    }

    /**
     * On a request without a sealed result the two Properties are the ones
     * in the response's derived member, and a Property the response gives
     * as null has no value and the cloud's reason.
     */
    @Test
    public void unsealedRequestReadsTheResponse() throws Exception {
        answer = "{'derived':{'humanconfidence':null,"
            + "'humanconfidencenullreason':'The key does not include it.',"
            + "'humanconfidencesource':'Passive'}}";
        answerJsonCalls();
        Map<String, String> plain = new LinkedHashMap<>();
        plain.put("query.User-Agent", "iPhone");

        Read read = process(builder().setLicenseKey(LICENSE).build(), plain);

        assertNull(read.humanConfidence);
        assertEquals("The key does not include it.",
            read.humanConfidenceReason);
        assertEquals("Passive", read.source);
        assertNull(calls.get(0).get("humanconfidence"));
    }

    private List<String> warnings() {
        List<String> warnings = new ArrayList<>();
        for (TestLogger logger : loggerFactory.loggers) {
            warnings.addAll(logger.warningsLogged);
        }
        return warnings;
    }
}
