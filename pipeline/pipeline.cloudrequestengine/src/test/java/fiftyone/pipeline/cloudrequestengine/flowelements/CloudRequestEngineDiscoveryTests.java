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
import fiftyone.common.testhelpers.TestLoggerFactory;
import fiftyone.pipeline.cloudrequestengine.CloudDiscoveryState;
import fiftyone.pipeline.cloudrequestengine.CloudRequestException;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.flowelements.Pipeline;
import fiftyone.pipeline.core.flowelements.PipelineBuilder;
import fiftyone.pipeline.engines.services.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Cloud Request Engine fetches its accessible properties and accepted
 * Evidence keys when it is built. A 4xx answer to either fails the build.
 * Any other failure is logged as one warning, the build completes, and
 * processing fetches what is missing before it sends a data request.
 */
public class CloudRequestEngineDiscoveryTests {

    private static final String PROPERTIES = "accessibleproperties";
    private static final String EVIDENCE_KEYS = "evidencekeys";
    private static final String DATA = "json";

    private static final String PROPERTIES_BODY =
        "{\"Products\": {\"device\": {\"DataTier\": \"tier\", " +
            "\"Properties\": [{\"Name\": \"IsMobile\", \"Type\": \"Boolean\"," +
            " \"Category\": \"Device\"}]}}}";
    private static final String EVIDENCE_KEYS_BODY =
        "[\"query.User-Agent\", \"header.User-Agent\"]";
    private static final String DATA_BODY = "{\"device\": {\"ismobile\": true}}";

    /**
     * A failure that is not an HTTP status, such as a refused connection.
     */
    private static final int UNREACHABLE = -1;

    /**
     * A failure where the request hangs until the timeout.
     */
    private static final int TIMES_OUT = -2;

    /**
     * A 200 answer whose body cannot be read.
     */
    private static final int UNREADABLE = -3;

    private static final int TIMEOUT_SECONDS = 3;

    /**
     * How long a discovery request waits for the other one to start.
     */
    private static final int WAIT_SECONDS = 5;

    /**
     * The timeout the page's Configuration options table and .NET give.
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 2;

    private TestLoggerFactory loggerFactory;
    private HttpClient httpClient;
    private final Map<String, Integer> requests = new ConcurrentHashMap<>();
    private final Map<String, Integer> answers = new HashMap<>();
    private final List<HttpURLConnection> connections =
        Collections.synchronizedList(new ArrayList<HttpURLConnection>());

    /**
     * When set, each discovery request waits until the other has started,
     * so the two only complete if they are made at once.
     */
    private volatile CountDownLatch bothDiscoveryRequests = null;

    @BeforeEach
    public void init() throws IOException {
        ILoggerFactory internal = mock(ILoggerFactory.class);
        when(internal.getLogger(anyString())).thenReturn(mock(Logger.class));
        loggerFactory = new TestLoggerFactory(internal);
        answers.put(PROPERTIES, 200);
        answers.put(EVIDENCE_KEYS, 200);
        answers.put(DATA, 200);

        httpClient = mock(HttpClient.class);
        when(httpClient.connect(any(URL.class))).thenAnswer(invocation -> {
            URL url = invocation.getArgument(0);
            String name = endpoint(url);
            requests.merge(name, 1, Integer::sum);
            CountDownLatch latch = bothDiscoveryRequests;
            if (latch != null && name.equals(DATA) == false) {
                latch.countDown();
                if (latch.await(WAIT_SECONDS, TimeUnit.SECONDS) == false) {
                    throw new SocketTimeoutException(
                        "The other discovery request was not made");
                }
            }
            int answer = answers.get(name);
            if (answer == UNREACHABLE) {
                throw new ConnectException("Connection refused");
            }
            HttpURLConnection connection = mock(HttpURLConnection.class);
            when(connection.getURL()).thenReturn(url);
            when(connection.getResponseCode())
                .thenReturn(answer < 0 ? 200 : answer);
            when(connection.getHeaderFields())
                .thenReturn(new HashMap<String, List<String>>());
            connections.add(connection);
            return connection;
        });
        when(httpClient.getResponseString(
            any(HttpURLConnection.class), anyMap()))
            .thenAnswer(invocation -> body(invocation.getArgument(0)));
        when(httpClient.getResponseString(any(HttpURLConnection.class)))
            .thenAnswer(invocation -> body(invocation.getArgument(0)));
        when(httpClient.postData(
            any(HttpURLConnection.class), anyMap(), any(byte[].class)))
            .thenAnswer(invocation -> body(invocation.getArgument(0)));
    }

    /**
     * In the normal case the built engine is ready. Both lists are fetched
     * once, at build, and processing sends only the data request.
     */
    @Test
    public void Build_FetchesBothLists() throws Exception {
        CloudRequestEngine engine = build();

        assertEquals(1, requests(PROPERTIES), "accessibleproperties");
        assertEquals(1, requests(EVIDENCE_KEYS), "evidencekeys");
        assertEquals(0, requests(DATA), "json");
        assertNotNull(engine.getPublicProperties());
        assertTrue(engine.getPublicProperties().containsKey("device"));
        assertTrue(engine.getEvidenceKeyFilter().include("header.user-agent"));
        assertEquals(0, warnings().size(), warnings().toString());

        process(engine, false);

        assertEquals(1, requests(PROPERTIES), "accessibleproperties");
        assertEquals(1, requests(EVIDENCE_KEYS), "evidencekeys");
        assertEquals(1, requests(DATA), "json");
    }

    private static Stream<Arguments> definitiveFailures() {
        List<Arguments> arguments = new ArrayList<>();
        for (int status : new int[] { 400, 401, 403, 404 }) {
            arguments.add(Arguments.of(PROPERTIES, status));
            arguments.add(Arguments.of(EVIDENCE_KEYS, status));
        }
        return arguments.stream();
    }

    /**
     * A 4xx answer to either request, other than 408 or 429, is a
     * definitive configuration error, which fails the build once both
     * requests have been made.
     */
    @ParameterizedTest(name = "{1} from {0}")
    @MethodSource("definitiveFailures")
    public void Build_4xx_FailsTheBuild(String endpoint, int status) {
        answers.put(endpoint, status);

        CloudRequestException error =
            assertThrows(CloudRequestException.class, this::build);

        assertEquals(status, error.getHttpStatusCode());
        assertEquals(1, requests(PROPERTIES), "accessibleproperties");
        assertEquals(1, requests(EVIDENCE_KEYS), "evidencekeys");
    }

    /**
     * A 4xx answer fails the build even when the other request failed in a
     * way that can pass.
     */
    @Test
    public void Build_4xxAndTransient_FailsTheBuild() {
        answers.put(PROPERTIES, 503);
        answers.put(EVIDENCE_KEYS, 403);

        CloudRequestException error =
            assertThrows(CloudRequestException.class, this::build);

        assertEquals(403, error.getHttpStatusCode());
    }

    private static Stream<Arguments> transientFailures() {
        return Stream.of(
            Arguments.of("500 from accessibleproperties", 500, 200),
            Arguments.of("503 from evidencekeys", 200, 503),
            Arguments.of("502 from both", 502, 502),
            Arguments.of("408 from accessibleproperties", 408, 200),
            Arguments.of("408 from evidencekeys", 200, 408),
            Arguments.of("429 from accessibleproperties", 429, 200),
            Arguments.of("429 from evidencekeys", 200, 429),
            Arguments.of("unreadable accessibleproperties", UNREADABLE, 200),
            Arguments.of("unreadable evidencekeys", 200, UNREADABLE),
            Arguments.of("connection refused to both", UNREACHABLE, UNREACHABLE),
            Arguments.of("timeout on evidencekeys", 200, TIMES_OUT));
    }

    /**
     * Any other failure, the service unreachable, a timeout, a 5xx, 408 or
     * 429 answer or a body that cannot be read, is logged as one warning and
     * the build completes.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("transientFailures")
    public void Build_TransientFailure_CompletesWithOneWarning(
        String name,
        int propertiesAnswer,
        int evidenceKeysAnswer) throws Exception {
        answers.put(PROPERTIES, propertiesAnswer);
        answers.put(EVIDENCE_KEYS, evidenceKeysAnswer);

        CloudRequestEngine engine = build();

        assertNotNull(engine);
        assertEquals(1, requests(PROPERTIES), "accessibleproperties");
        assertEquals(1, requests(EVIDENCE_KEYS), "evidencekeys");
        assertEquals(1, warnings().size(), warnings().toString());
    }

    /**
     * A list that arrived at build is kept, and the other is empty until it
     * is fetched. Reading them makes no request.
     */
    @Test
    public void Build_PartialFailure_KeepsWhatArrived() throws Exception {
        answers.put(EVIDENCE_KEYS, 503);

        CloudRequestEngine engine = build();

        assertNotNull(engine.getPublicProperties());
        assertTrue(engine.getPublicProperties().containsKey("device"));
        assertFalse(engine.getEvidenceKeyFilter().include("header.user-agent"));
        assertEquals(1, requests(PROPERTIES), "accessibleproperties");
        assertEquals(1, requests(EVIDENCE_KEYS), "evidencekeys");
    }

    /**
     * Processing fetches only what the build could not, then sends the data
     * request. The Pipeline's Evidence filter, built before the retry, sees
     * the keys the retry fetched.
     */
    @Test
    public void Process_FetchesOnlyWhatIsMissing() throws Exception {
        answers.put(EVIDENCE_KEYS, 503);
        CloudRequestEngine engine = build();
        answers.put(EVIDENCE_KEYS, 200);

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build()) {
            assertFalse(pipeline.getEvidenceKeyFilter()
                .include("header.user-agent"));
            try (FlowData data = pipeline.createFlowData()) {
                data.addEvidence("header.user-agent", "iPhone");
                data.process();
            }
            assertTrue(pipeline.getEvidenceKeyFilter()
                .include("header.user-agent"));
        }

        assertEquals(1, requests(PROPERTIES), "accessibleproperties");
        assertEquals(2, requests(EVIDENCE_KEYS), "evidencekeys");
        assertEquals(1, requests(DATA), "json");
        assertTrue(engine.getEvidenceKeyFilter().include("header.user-agent"));
    }

    /**
     * While discovery still fails no data request is sent, and the failure
     * is recorded as SuppressProcessExceptions says.
     */
    @Test
    public void Process_DiscoveryStillFailing_Suppressed() throws Exception {
        answers.put(PROPERTIES, 503);
        CloudRequestEngine engine = build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(true)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", "iPhone");
            data.process();

            assertFalse(data.getErrors().isEmpty());
        }

        assertEquals(2, requests(PROPERTIES), "accessibleproperties");
        assertEquals(0, requests(DATA), "json");
    }

    @Test
    public void Process_DiscoveryStillFailing_Throws() throws Exception {
        answers.put(PROPERTIES, UNREACHABLE);
        answers.put(EVIDENCE_KEYS, UNREACHABLE);
        CloudRequestEngine engine = build();

        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", "iPhone");
            assertThrows(RuntimeException.class, data::process);
        }

        assertEquals(0, requests(DATA), "json");
    }

    /**
     * The configured timeout applies to the discovery requests, so a
     * service that does not answer delays the build by the timeout alone.
     */
    @Test
    public void Build_AppliesTheTimeoutToDiscovery() throws Exception {
        build();

        assertEquals(2, connections.size());
        for (HttpURLConnection connection : connections) {
            verify(connection).setConnectTimeout(TIMEOUT_SECONDS * 1000);
            verify(connection).setReadTimeout(TIMEOUT_SECONDS * 1000);
        }
    }

    /**
     * A copy of what discovery fetched is read from the builder after a
     * build, kept as JSON, and given to a later build, which makes no
     * discovery request and is ready to process.
     */
    @Test
    public void DiscoveryState_KeptAndUsedWithNoRequest() throws Exception {
        CloudRequestEngineBuilder first = builder();
        first.build();
        CloudDiscoveryState state = first.getDiscoveryState();
        assertNotNull(state);
        String kept = state.toJson();

        answers.put(PROPERTIES, UNREACHABLE);
        answers.put(EVIDENCE_KEYS, UNREACHABLE);
        requests.clear();
        CloudRequestEngine engine = builder()
            .setDiscoveryState(CloudDiscoveryState.fromJson(kept))
            .build();

        assertEquals(0, requests(PROPERTIES), "accessibleproperties");
        assertEquals(0, requests(EVIDENCE_KEYS), "evidencekeys");
        assertTrue(engine.getPublicProperties().containsKey("device"));
        assertTrue(engine.getEvidenceKeyFilter().include("header.user-agent"));
        assertEquals(0, warnings().size(), warnings().toString());

        process(engine, false);

        assertEquals(0, requests(PROPERTIES), "accessibleproperties");
        assertEquals(0, requests(EVIDENCE_KEYS), "evidencekeys");
        assertEquals(1, requests(DATA), "json");
    }

    /**
     * A build that does not know both lists gives no copy.
     */
    @Test
    public void DiscoveryState_NoneWithoutBothLists() throws Exception {
        answers.put(EVIDENCE_KEYS, 503);
        CloudRequestEngineBuilder builder = builder();
        builder.build();

        assertNull(builder.getDiscoveryState());
    }

    private CloudRequestEngineBuilder builder() {
        return new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey("resource_key")
            .setTimeOutSeconds(TIMEOUT_SECONDS);
    }

    /**
     * The two discovery requests are made at once, as the page allows, so
     * a service that does not answer delays the build by one timeout. Each
     * request here waits for the other to start, so made one after the
     * other they would both fail.
     */
    @Test
    public void Build_MakesBothRequestsAtOnce() throws Exception {
        bothDiscoveryRequests = new CountDownLatch(2);

        CloudRequestEngine engine = build();

        assertEquals(0, warnings().size(), warnings().toString());
        assertNotNull(engine.getPublicProperties());
        assertTrue(engine.getEvidenceKeyFilter().include("header.user-agent"));
    }

    /**
     * With no timeout set, the requests time out after 2 seconds.
     */
    @Test
    public void Build_DefaultTimeoutIsTwoSeconds() throws Exception {
        new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey("resource_key")
            .build();

        assertEquals(2, connections.size());
        for (HttpURLConnection connection : connections) {
            verify(connection).setConnectTimeout(
                DEFAULT_TIMEOUT_SECONDS * 1000);
            verify(connection).setReadTimeout(DEFAULT_TIMEOUT_SECONDS * 1000);
        }
    }

    private CloudRequestEngine build() throws Exception {
        return new CloudRequestEngineBuilder(loggerFactory, httpClient)
            .setResourceKey("resource_key")
            .setTimeOutSeconds(TIMEOUT_SECONDS)
            .build();
    }

    private void process(CloudRequestEngine engine, boolean suppress)
        throws Exception {
        try (Pipeline pipeline = new PipelineBuilder(loggerFactory)
                .setSuppressProcessException(suppress)
                .addFlowElement(engine)
                .build();
             FlowData data = pipeline.createFlowData()) {
            data.addEvidence("header.user-agent", "iPhone");
            data.process();
        }
    }

    private int requests(String endpoint) {
        return requests.getOrDefault(endpoint, 0);
    }

    private List<String> warnings() {
        List<String> warnings = new ArrayList<>();
        for (TestLogger logger : loggerFactory.loggers) {
            warnings.addAll(logger.warningsLogged);
        }
        return warnings;
    }

    private static String endpoint(URL url) {
        String path = url.getPath().toLowerCase();
        if (path.endsWith(PROPERTIES)) {
            return PROPERTIES;
        }
        if (path.endsWith(EVIDENCE_KEYS)) {
            return EVIDENCE_KEYS;
        }
        return DATA;
    }

    private String body(HttpURLConnection connection) throws IOException {
        String name = endpoint(connection.getURL());
        int answer = answers.get(name);
        if (answer == TIMES_OUT) {
            throw new SocketTimeoutException("Read timed out");
        }
        if (answer == UNREADABLE) {
            return "<html>proxy error</html>";
        }
        if (answer != 200) {
            return "{\"errors\": [\"" + name + " answered " + answer + "\"]}";
        }
        switch (name) {
            case PROPERTIES:
                return PROPERTIES_BODY;
            case EVIDENCE_KEYS:
                return EVIDENCE_KEYS_BODY;
            default:
                return DATA_BODY;
        }
    }
}
