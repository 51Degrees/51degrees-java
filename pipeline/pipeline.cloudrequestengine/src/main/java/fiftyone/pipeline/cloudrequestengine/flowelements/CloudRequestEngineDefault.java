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

import fiftyone.pipeline.cloudrequestengine.CloudDiscoveryState;
import fiftyone.pipeline.cloudrequestengine.CloudRequestException;
import fiftyone.pipeline.cloudrequestengine.data.CloudRequestData;
import fiftyone.pipeline.core.Constants;
import fiftyone.pipeline.core.data.AccessiblePropertyMetaData;
import fiftyone.pipeline.core.data.EvidenceKeyFilter;
import fiftyone.pipeline.core.data.EvidenceKeyFilterWhitelist;
import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.exceptions.PropertyNotLoadedException;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.data.AspectPropertyMetaDataDefault;
import fiftyone.pipeline.engines.flowelements.AspectEngineBase;
import fiftyone.pipeline.engines.services.HttpClient;
import fiftyone.pipeline.exceptions.AggregateException;
import fiftyone.pipeline.util.LicenseKeyMask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fiftyone.pipeline.cloudrequestengine.Constants.Messages.*;
import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Engine that makes requests to the 51Degrees cloud service.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/cloud-request-engine.md">Specification</a>
 */
public class CloudRequestEngineDefault
    extends AspectEngineBase<CloudRequestData, AspectPropertyMetaData>
    implements CloudRequestEngine {

    @Override
    protected Class<CloudRequestData> getDataType() {
        return CloudRequestData.class;
    }

    /**
     * The Evidence key of the whole web request, which the web integration
     * adds for usage sharing to read. It holds every header and cookie of
     * the request, so it is never sent to the cloud service. The usage
     * sharing package defines the same key, and this package does not
     * depend on that one.
     */
    private static final String HTTP_REQUEST_KEY = "server.http-request";

    /**
     * Form fields the request reserves for the credentials and the list of
     * Properties, with licence, which the cloud service reads as the
     * license key where license is absent. Evidence whose key strips to one
     * of them is never sent, as the Processing section of the
     * specification requires.
     */
    private static final Set<String> ENGINE_FIELDS =
        Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("resource", "license", "licence", "values")));

    /**
     * The Evidence that carries a sealed Human Confidence result. See the
     * Human Confidence specification, "When HumanConfidence arrives
     * sealed".
     */
    public static final String SEALED_HUMAN_CONFIDENCE_EVIDENCE =
        "query.humanconfidence";

    /**
     * The Evidence that carries the id of a {@link HumanConfidencePage},
     * being the {@code humanconfidencepage} parameter the page sends. It
     * is never sent to the cloud service.
     */
    public static final String HUMAN_CONFIDENCE_PAGE_EVIDENCE =
        "query.humanconfidencepage";

    /** The form field the kept transaction is sent as. */
    private static final String TRANSACTION_FIELD = "transaction";

    /** What replaces a sealed result, transaction or key in a message. */
    private static final String HIDDEN = "[hidden]";

    /** The transactions given to pages and not yet used. */
    private final HumanConfidencePages humanConfidencePages =
        new HumanConfidencePages();

    /**
     * The Property the request names on a request that carries a sealed
     * result, so the answer holds the redeemed value.
     */
    static final String HUMAN_CONFIDENCE_PROPERTY = "derived.humanconfidence";

    /**
     * Names whose Evidence is left out of a request that carries a sealed
     * result, because the only transaction in that call is the one the
     * package kept. See the Human Confidence specification, "Redeeming
     * through the JSON call".
     */
    private static final Set<String> TRANSACTION_FIELDS =
        Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("transaction", "challenge")));

    /**
     * Names whose values are never written to a log entry, being the
     * sealed result and the transaction.
     */
    private static final Set<String> HIDDEN_FIELDS =
        Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("humanconfidence", "transaction", "challenge")));

    private HttpClient httpClient;

    private String endPoint;
    private String resourceKey;
    private String licenseKey;
    private String propertiesEndpoint;
    private String evidenceKeysEndpoint;
    private String cloudRequestOrigin;
    private Integer timeoutMillis;
    private final List<String> requestedProperties;
    private final AtomicBoolean requestedPropertiesChecked =
        new AtomicBoolean(false);

    /**
     * The lowest HTTP status of a client error. A discovery request answered
     * with a status from this up to {@link #HTTP_SERVER_ERROR_MIN} is a
     * definitive configuration error.
     */
    private static final int HTTP_CLIENT_ERROR_MIN = 400;

    /**
     * The lowest HTTP status of a server error.
     */
    private static final int HTTP_SERVER_ERROR_MIN = 500;

    private volatile List<AspectPropertyMetaData> propertyMetaData;

    /**
     * The products the resource key gives access to, from the
     * accessibleproperties request, or null until that request succeeds.
     */
    private volatile Map<String, AccessiblePropertyMetaData.ProductMetaData> publicProperties;

    /**
     * The answer to the accessibleproperties request that gave
     * {@link #publicProperties}, kept for {@link #getDiscoveryState()}.
     */
    private volatile String accessibleProperties;

    /**
     * The accepted Evidence, from the evidencekeys request. It is the same
     * instance before and after the request succeeds.
     */
    private final CloudEvidenceKeyFilter evidenceKeyFilter =
        new CloudEvidenceKeyFilter();

    /**
     * The prefixes after query, header and cookie, highest precedence
     * first, as evidence.md lists them.
     */
    private static final String[] OTHER_PREFIX_PRECEDENCE =
        { "server", "fiftyone", "location" };

    /**
     * Held while the discovery requests are made, so that concurrent first
     * uses make them once.
     */
    private final Object discoveryLock = new Object();

    /**
     * Runs the cloud requests started while processing, on daemon threads
     * so an engine not closed does not keep the process alive.
     */
    private final ExecutorService requests =
        Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "cloud-request");
            thread.setDaemon(true);
            return thread;
        });

    /**
     * Create a new instance with a resource key.
     *
     * @param logger used for logging
     * @param aspectDataFactory creates the element data this engine returns
     * @param httpClient used to make requests to the cloud service
     * @param endPoint the cloud service endpoint
     * @param resourceKey the resource key, or null
     * @param propertiesEndpoint the properties endpoint
     * @param evidenceKeysEndpoint the evidence keys endpoint
     * @param timeoutMillis the request timeout in milliseconds
     */
    public CloudRequestEngineDefault(
        Logger logger,
        ElementDataFactory<CloudRequestData> aspectDataFactory,
        HttpClient httpClient,
        String endPoint,
        String resourceKey,
        String propertiesEndpoint,
        String evidenceKeysEndpoint,
        int timeoutMillis) {
        this(
            logger,
            aspectDataFactory,
            httpClient,
            endPoint,
            resourceKey,
            null,
            propertiesEndpoint,
            evidenceKeysEndpoint,
            timeoutMillis,
            null);
    }
    /**
     * Create a new instance with a resource key and Origin header.
     *
     * @param logger used for logging
     * @param aspectDataFactory creates the element data this engine returns
     * @param httpClient used to make requests to the cloud service
     * @param endPoint the cloud service endpoint
     * @param resourceKey the resource key, or null
     * @param propertiesEndpoint the properties endpoint
     * @param evidenceKeysEndpoint the evidence keys endpoint
     * @param timeoutMillis the request timeout in milliseconds
     * @param cloudRequestOrigin the value of the Origin header, or null for
     *    none
     */
    public CloudRequestEngineDefault(
        Logger logger,
        ElementDataFactory<CloudRequestData> aspectDataFactory,
        HttpClient httpClient,
        String endPoint,
        String resourceKey,
        String propertiesEndpoint,
        String evidenceKeysEndpoint,
        int timeoutMillis,
        String cloudRequestOrigin) {
        this(
            logger,
            aspectDataFactory,
            httpClient,
            endPoint,
            resourceKey,
            null,
            propertiesEndpoint,
            evidenceKeysEndpoint,
            timeoutMillis,
            cloudRequestOrigin);
    }

    /**
     * Create a new instance with a resource key or license key.
     *
     * @param logger used for logging
     * @param aspectDataFactory creates the element data this engine returns
     * @param httpClient used to make requests to the cloud service
     * @param endPoint the cloud service endpoint
     * @param resourceKey the resource key, or null
     * @param licenseKey the license key, or null
     * @param propertiesEndpoint the properties endpoint
     * @param evidenceKeysEndpoint the evidence keys endpoint
     * @param timeoutMillis the request timeout in milliseconds
     * @param cloudRequestOrigin the value of the Origin header, or null for
     *    none
     */
    public CloudRequestEngineDefault(
        Logger logger,
        ElementDataFactory<CloudRequestData> aspectDataFactory,
        HttpClient httpClient,
        String endPoint,
        String resourceKey,
        String licenseKey,
        String propertiesEndpoint,
        String evidenceKeysEndpoint,
        int timeoutMillis,
        String cloudRequestOrigin) {
        this(
            logger,
            aspectDataFactory,
            httpClient,
            endPoint,
            resourceKey,
            licenseKey,
            propertiesEndpoint,
            evidenceKeysEndpoint,
            timeoutMillis,
            cloudRequestOrigin,
            Collections.<String>emptyList());
    }

    /**
     * Constructor.
     * @param logger used for logging
     * @param aspectDataFactory creates the element data this engine returns
     * @param httpClient used to make requests to the cloud service
     * @param endPoint the cloud service endpoint
     * @param resourceKey the resource key, or null
     * @param licenseKey the license key, or null
     * @param propertiesEndpoint the properties endpoint
     * @param evidenceKeysEndpoint the evidence keys endpoint
     * @param timeoutMillis the request timeout in milliseconds
     * @param cloudRequestOrigin the value of the Origin header, or null for
     *                           none
     * @param requestedProperties the fully qualified names of the
     *                            properties to ask for, sent as {@code values}
     *                            when the list is not empty
     */
    public CloudRequestEngineDefault(
        Logger logger,
        ElementDataFactory<CloudRequestData> aspectDataFactory,
        HttpClient httpClient,
        String endPoint,
        String resourceKey,
        String licenseKey,
        String propertiesEndpoint,
        String evidenceKeysEndpoint,
        int timeoutMillis,
        String cloudRequestOrigin,
        List<String> requestedProperties) {
        super(logger, aspectDataFactory);
        this.requestedProperties = Collections.unmodifiableList(
            new ArrayList<>(requestedProperties));

        this.endPoint = endPoint;
        this.resourceKey = resourceKey;
        this.licenseKey = licenseKey;
        this.propertiesEndpoint = propertiesEndpoint;
        this.evidenceKeysEndpoint = evidenceKeysEndpoint;
        this.httpClient = httpClient;
        this.cloudRequestOrigin = cloudRequestOrigin;

        if (timeoutMillis > 0) {
            this.timeoutMillis = timeoutMillis;
        } else {
            this.timeoutMillis = null;
        }
    }

    /**
     * The property metadata of this engine, created on first use as it
     * refers to the engine.
     * @return the property metadata
     */
    @Override
    public List<AspectPropertyMetaData> getProperties() {
        List<AspectPropertyMetaData> localRef = propertyMetaData;
        if (localRef == null) {
            synchronized (this) {
                localRef = propertyMetaData;
                if (localRef == null) {
                    localRef = new ArrayList<>();
                    localRef.add(new AspectPropertyMetaDataDefault(
                        "json-response",
                        this,
                        "",
                        String.class,
                        new ArrayList<String>(),
                        true));
                    localRef.add(new AspectPropertyMetaDataDefault(
                        "process-started",
                        this,
                        "",
                        Boolean.class,
                        new ArrayList<String>(),
                        true));
                    propertyMetaData = localRef;
                }
            }
        }
        return localRef;
    }

    @Override
    public String getDataSourceTier() {
        return "cloud";
    }

    @Override
    public String getElementDataKey() {
        return "cloud-response";
    }

    /**
     * The accepted Evidence keys from the evidencekeys request. Until that
     * request succeeds the filter includes nothing, and it makes no request
     * itself. A filter that holds this one, such as the Pipeline's, sees
     * the keys as soon as a retry fetches them.
     * @return the accepted Evidence
     */
    @Override
    public EvidenceKeyFilter getEvidenceKeyFilter() {
        return evidenceKeyFilter;
    }

    /**
     * The products the resource key gives access to, from the
     * accessibleproperties request, which the builder makes when it builds
     * the engine. After a failure at build that can pass this is null until
     * processing fetches them. It makes no request itself.
     * @return the products, or null if they are not known yet
     */
    @Override
    public Map<String, AccessiblePropertyMetaData.ProductMetaData>
    getPublicProperties() {
        return publicProperties;
    }

    /**
     * Fetch the accepted Evidence keys and the accessible properties that
     * are not known yet. The builder calls this when it builds the engine,
     * so in the normal case a built engine is ready to process. Both
     * requests are made. A 4xx answer to either is a definitive
     * configuration error, and its {@link CloudRequestException} is thrown,
     * because retrying with the same configuration can never succeed. Any
     * other failure, such as the service being unreachable, a timeout, a
     * 5xx answer or a body that cannot be read, is logged as one warning
     * and the list that did arrive is kept. Processing fetches what is
     * still missing.
     * @throws CloudRequestException for a 4xx answer to either request
     */
    public void discover() {
        List<RuntimeException> failures = new ArrayList<>();
        synchronized (discoveryLock) {
            // The two requests are made at once, so a service that does not
            // answer delays the build by one timeout rather than two.
            CompletableFuture<Void> evidenceKeys =
                evidenceKeyFilter.isKnown() ?
                    CompletableFuture.<Void>completedFuture(null) :
                    CompletableFuture.runAsync(this::fetchEvidenceKeys);
            if (publicProperties == null) {
                try {
                    fetchPublicProperties();
                } catch (RuntimeException e) {
                    failures.add(e);
                }
            }
            try {
                evidenceKeys.join();
            } catch (CompletionException e) {
                failures.add(e.getCause() instanceof RuntimeException ?
                    (RuntimeException) e.getCause() :
                    e);
            }
        }
        if (failures.isEmpty()) {
            return;
        }
        for (RuntimeException failure : failures) {
            CloudRequestException definitive = definitiveError(failure);
            if (definitive != null) {
                throw definitive;
            }
        }
        RuntimeException first = failures.get(0);
        for (int i = 1; i < failures.size(); i++) {
            first.addSuppressed(failures.get(i));
        }
        logger.warn(MessageDiscoveryDeferred, first);
    }

    /**
     * Whether discovery has fetched both the accepted evidence keys and the
     * accessible properties, so that what reads them, such as the
     * Pipeline's map of available properties, can keep what it reads.
     * @return true once both are known
     */
    @Override
    public boolean hasLoadedProperties() {
        return evidenceKeyFilter.isKnown() && publicProperties != null;
    }

    /**
     * A copy of what discovery fetched, which a later builder can use in
     * place of the discovery requests.
     * @return the copy, or null unless both the accepted evidence keys and
     * the accessible properties are known
     */
    public CloudDiscoveryState getDiscoveryState() {
        synchronized (discoveryLock) {
            List<String> keys = evidenceKeyFilter.getKeys();
            return keys == null || accessibleProperties == null ?
                null :
                new CloudDiscoveryState(keys, accessibleProperties);
        }
    }

    /**
     * Use a copy of what an earlier discovery fetched, in place of the
     * discovery requests.
     * @param state the copy
     */
    public void useDiscoveryState(CloudDiscoveryState state) {
        synchronized (discoveryLock) {
            evidenceKeyFilter.setKeys(state.getEvidenceKeys());
            setPublicProperties(state.getAccessibleProperties());
        }
    }

    /**
     * The failure of a discovery request that the cloud service received
     * and rejected with a 4xx status, so that retrying with the same
     * configuration can never succeed.
     * @param failure the failure of a discovery request
     * @return the exception with the 4xx status, or null if there is none
     */
    private static CloudRequestException definitiveError(Throwable failure) {
        if (failure instanceof CloudRequestException) {
            int status = ((CloudRequestException) failure).getHttpStatusCode();
            return status >= HTTP_CLIENT_ERROR_MIN &&
                status < HTTP_SERVER_ERROR_MIN ?
                (CloudRequestException) failure :
                null;
        }
        if (failure instanceof AggregateException) {
            for (Throwable inner : failure.getSuppressed()) {
                CloudRequestException definitive = definitiveError(inner);
                if (definitive != null) {
                    return definitive;
                }
            }
        }
        return null;
    }

    /**
     * Fetch what discovery could not when the engine was built, evidence
     * keys first, stopping at the first failure, which is a process-time
     * failure. No data request is sent until both are known.
     * @throws IOException if a discovery request could not be made
     */
    private void completeDiscovery() throws IOException {
        if (evidenceKeyFilter.isKnown() && publicProperties != null) {
            return;
        }
        synchronized (discoveryLock) {
            try {
                if (evidenceKeyFilter.isKnown() == false) {
                    fetchEvidenceKeys();
                }
                if (publicProperties == null) {
                    fetchPublicProperties();
                }
            } catch (PropertyNotLoadedException e) {
                // A request that could not be made is reported as the
                // IOException it was, as for the data request.
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                }
                throw e;
            }
        }
    }

    /**
     * Make what a new page needs so that its sealed Human Confidence result
     * can be redeemed. Call it once for each page served, and never share
     * the result between pages. The engine keeps the page's transaction for
     * a limited time and uses it once.
     * @return the page's id and script address
     */
    public HumanConfidencePage newHumanConfidencePage() {
        return humanConfidencePages.issue();
    }

    /**
     * Whether the Flow Data's Evidence holds a sealed Human Confidence
     * result, being {@link #SEALED_HUMAN_CONFIDENCE_EVIDENCE} with a value
     * that is not empty.
     * @param data the Flow Data
     * @return true if the request carries a sealed result
     */
    public static boolean carriesSealedResult(FlowData data) {
        for (Map.Entry<String, Object> entry :
            data.getEvidence().asKeyMap().entrySet()) {
            if (SEALED_HUMAN_CONFIDENCE_EVIDENCE.equalsIgnoreCase(
                    entry.getKey()) &&
                entry.getValue() != null &&
                entry.getValue().toString().isEmpty() == false) {
                return true;
            }
        }
        return false;
    }

    /**
     * A request that carries a sealed Human Confidence result is neither
     * served from nor stored in the cache, because a sealed result redeems
     * once. See the caching specification, "Values created for one
     * request".
     * @param flowData the Flow Data being processed
     * @return false where the request carries a sealed result
     */
    @Override
    protected boolean isCacheUsed(FlowData flowData) {
        return carriesSealedResult(flowData) == false;
    }

    /**
     * A response whose JSON has a top-level member named fodid, in any case,
     * holds a 51Did created for the one request, so it is never served from
     * the cache, and the request goes to the cloud service instead. See the
     * <a href="https://github.com/51Degrees/specifications/blob/706de1f0cb3edd560eb7fd7ef3bb19b93a4883dc/pipeline-specification/features/caching.md#L144">caching specification</a>:
     * "A cache MUST NOT store a result that holds such a value, and MUST NOT
     * serve one that it finds." A response that cannot be read as a JSON
     * object is not cached either, as it cannot be checked.
     * <p>
     * The response is a lazy task that is usually still running when the
     * result is offered to the cache, and this check never waits for it. A
     * result whose request is still running is stored, because what its
     * response holds is not known yet, and {@link #isServable} keeps it from
     * being served unless its request finished with a response that may be
     * served. A result whose request has finished is stored only if that
     * response may be served.
     * @param data the result to check
     * @return false if the request has finished and its response may not be
     * cached
     */
    @Override
    protected boolean isCacheable(CloudRequestData data) {
        if (((CloudRequestDataInternal)data).isRequestRunning()) {
            return true;
        }
        return isServable(data);
    }

    /**
     * A result found in the cache is served only once its cloud request has
     * finished, without failing, with a response that has no top-level fodid
     * member. Otherwise the request goes to the cloud service, so a request
     * still running for another Flow Data, and one that failed, are never
     * shared through the cache.
     * @param data the result found in the cache
     * @return true if the result may be served
     */
    @Override
    protected boolean isServable(CloudRequestData data) {
        String response =
            ((CloudRequestDataInternal)data).getJsonResponseWithoutWaiting();
        if (response == null) {
            return false;
        }
        try {
            for (String key : new JSONObject(response).keySet()) {
                if (FODID.equalsIgnoreCase(key)) {
                    return false;
                }
            }
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * The top-level member of a cloud response that holds a 51Did.
     */
    private static final String FODID = "fodid";

    @Override
    protected void processEngine(FlowData data, CloudRequestData aspectData) throws IOException {
        CloudRequestDataInternal request = aspectData;
        request.setProcessStarted(true);
        completeDiscovery();

        // A sealed Human Confidence result is redeemed only with the License
        // Key and the transaction kept for the page it came from. Otherwise
        // neither the sealed result nor any transaction is sent, and the
        // Human Confidence element gives Medium.
        String transaction = null;
        List<String> hidden = null;
        if (carriesSealedResult(data)) {
            if (hasLicenseKey()) {
                transaction = humanConfidencePages.take(
                    evidenceValue(data, HUMAN_CONFIDENCE_PAGE_EVIDENCE));
            }
            if (transaction != null) {
                request.setRedeeming();
            }
            hidden = new ArrayList<>(getLicenseKeys());
            hidden.add(evidenceValue(data, SEALED_HUMAN_CONFIDENCE_EVIDENCE));
            hidden.add(transaction);
        }

        byte[] content = getContent(data, transaction, hidden);
        HttpURLConnection connection = httpClient.connect(toUrl(endPoint.trim()));
        setTimeouts(connection);

        Map<String, String> headers = new HashMap<>();
        setCommonHeaders(headers);
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Content-Length", Integer.toString(content.length));

        // The request is built from the Flow Data above. The task holds only
        // the request, and readers of the JSON response wait for it.
        final List<String> hide = hidden;
        request.setJsonResponseTask(
            CompletableFuture.supplyAsync(
                () -> send(connection, headers, content, hide), requests));
    }

    /**
     * The value of an Evidence key, found without regard to case.
     * @return the value, or null where there is none or it is empty
     */
    private static String evidenceValue(FlowData data, String key) {
        for (Map.Entry<String, Object> entry :
            data.getEvidence().asKeyMap().entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey()) &&
                entry.getValue() != null) {
                String value = entry.getValue().toString();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    /**
     * The message with each value replaced, as written and as encoded in a
     * URL. On a request that carries a sealed result no message holds the
     * sealed result, the transaction or the License Key, not even in part.
     * See the Human Confidence specification, "What a package never
     * exposes".
     */
    private static String hide(String message, List<String> values) {
        if (message == null || values == null) {
            return message;
        }
        String result = message;
        List<String> longestFirst = new ArrayList<>();
        for (String value : values) {
            if (value != null && value.isEmpty() == false) {
                longestFirst.add(value);
            }
        }
        longestFirst.sort((a, b) -> b.length() - a.length());
        for (String value : longestFirst) {
            result = result.replace(value, HIDDEN);
            try {
                result = result.replace(
                    URLEncoder.encode(value, "UTF-8"), HIDDEN);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
        return result;
    }

    /**
     * Send a built request and check its response, as the task behind the
     * JSON response. Never reads the Flow Data.
     */
    private String send(
        HttpURLConnection connection,
        Map<String, String> headers,
        byte[] content,
        List<String> hidden) {
        try {
            String response = httpClient.postData(connection, headers, content);
            validateResponse(response, connection, true, hidden);
            reportPropertiesNotCovered(response);
            return response;
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    /**
     * Compare the properties asked for with the ones the answer carries, and
     * warn once about any left out. The service leaves out a requested
     * property it does not answer without naming it, and lower-cases the
     * names it answers with. A property that is present but null counts as
     * answered, as it carries a null reason beside it. The request is never
     * failed, because the properties that did come back are usable. With no
     * license key, a property the resource key does not carry is answered
     * as null, so a name left out is not a property, and the message says
     * so.
     */
    private void reportPropertiesNotCovered(String response) {
        if (requestedProperties.isEmpty() ||
            requestedPropertiesChecked.compareAndSet(false, true) == false) {
            return;
        }
        JSONObject answer;
        try {
            answer = new JSONObject(response);
        } catch (JSONException e) {
            return;
        }
        List<String> notCovered = new ArrayList<>();
        for (String name : requestedProperties) {
            String[] parts = name.split(
                Pattern.quote(Constants.EVIDENCE_SEPERATOR), 2);
            if (parts.length < 2 || hasKeyIgnoringCase(
                    answer.optJSONObject(keyIgnoringCase(answer, parts[0])),
                    parts[1]) == false) {
                notCovered.add(name);
            }
        }
        if (notCovered.isEmpty() == false) {
            logger.warn(String.format(
                hasLicenseKey() ? PropertiesNotCovered :
                    RequestedNamesNotProperties,
                stringJoin(notCovered, ", ")));
        }
    }

    private static String keyIgnoringCase(JSONObject object, String name) {
        for (String key : object.keySet()) {
            if (key.equalsIgnoreCase(name)) {
                return key;
            }
        }
        return name;
    }

    private static boolean hasKeyIgnoringCase(JSONObject object, String name) {
        return object != null &&
            object.has(keyIgnoringCase(object, name));
    }

    /**
     * Generate the Content to send in the POST request. The evidence keys
     * e.g. 'query.' and 'header.' have an order of precedence. These are
     * added to the evidence in reverse order, if there is conflict then
     * the queryData value is overwritten.
     * 'query.' evidence should take precedence over all other evidence.
     * If there are evidence keys other than 'query.' that conflict then
     * this is unexpected so a warning will be logged.
     * <p>
     * A request that carries a sealed Human Confidence result sends it only
     * with the transaction kept for its page. Otherwise it sends neither.
     * @param data the FlowData
     * @param transaction the transaction kept for the page, or null
     * @param hidden the values no message may hold, or null
     * @return form content for a POST request
     * @throws UnsupportedEncodingException
     */
    private byte[] getContent(
        FlowData data,
        String transaction,
        List<String> hidden) throws UnsupportedEncodingException {

        Map<String, Object> formData = getFormData(data, hidden);
        if (carriesSealedResult(data)) {
            if (transaction == null) {
                formData.remove(keySuffix(SEALED_HUMAN_CONFIDENCE_EVIDENCE));
            } else {
                formData.put(TRANSACTION_FIELD, transaction);
            }
        }
        List<String> formItems = new ArrayList<>();

        formItems.addAll(credentialItems());
        if (requestedProperties.isEmpty() == false) {
            formItems.add("values=" + URLEncoder.encode(
                stringJoin(valuesFor(data), ","), "UTF-8"));
        }

        List<String> formKeys = Arrays.asList(formData.keySet().toArray(new String[0]));
        Collections.sort(formKeys, Collections.reverseOrder());
        for (String key : formKeys) {
            formItems.add(key + "=" + URLEncoder.encode(formData.get(key).toString(), "UTF-8"));

        }

        String string = stringJoin(formItems, "&");

        return string.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * The Properties the request names. On a request that carries a sealed
     * result, {@link #HUMAN_CONFIDENCE_PROPERTY} is added where the
     * configured list does not hold it. The report of Properties the answer
     * left out reads the configured list alone.
     */
    private List<String> valuesFor(FlowData data) {
        if (carriesSealedResult(data) == false) {
            return requestedProperties;
        }
        for (String name : requestedProperties) {
            if (HUMAN_CONFIDENCE_PROPERTY.equalsIgnoreCase(name)) {
                return requestedProperties;
            }
        }
        List<String> values = new ArrayList<>(requestedProperties);
        values.add(HUMAN_CONFIDENCE_PROPERTY);
        return values;
    }

    private boolean hasResourceKey() {
        return resourceKey != null && resourceKey.isEmpty() == false;
    }

    /**
     * @return true where the engine was given a License Key
     */
    boolean hasLicenseKey() {
        return licenseKey != null && licenseKey.isEmpty() == false;
    }

    /** The resource and license keys that are set, as form items. */
    private List<String> credentialItems() throws UnsupportedEncodingException {
        List<String> items = new ArrayList<>();
        if (hasResourceKey()) {
            items.add("resource=" + URLEncoder.encode(resourceKey, "UTF-8"));
        }
        if (hasLicenseKey()) {
            items.add("license=" + URLEncoder.encode(licenseKey, "UTF-8"));
        }
        return items;
    }

    /**
     * The Evidence to send, keyed by the name the cloud service expects.
     * Only Evidence that {@link #isSent(String)} allows is included. Keys
     * ignore letter case, so one name under two prefixes is one field,
     * sent with the name and value of the key that wins by the precedence
     * in evidence.md, being query, header, cookie, server, fiftyone and
     * location, then any other prefix alphabetically.
     * Evidence with no prefix ranks with the other prefixes, unless the
     * whole key is query, header or cookie, as {@link #keyPrefix(String)}
     * gives the whole key as its prefix.
     * @param flowData the FlowData
     * @return the form fields to send
     */
    Map<String, Object> getFormData(FlowData flowData) {
        return getFormData(flowData, null);
    }

    /**
     * As {@link #getFormData(FlowData)}, with a conflict warning that holds
     * none of the values given.
     * @param flowData the FlowData
     * @param hidden the values no message may hold, or null
     * @return the form fields to send
     */
    private Map<String, Object> getFormData(
        FlowData flowData,
        List<String> hidden) {
        // On a request that carries a sealed result, Evidence named
        // transaction or challenge is left out, so the call holds no
        // transaction taken from the request.
        boolean sealed = carriesSealedResult(flowData);
        Map<String, Object> evidence = new HashMap<>();
        for (Map.Entry<String, Object> entry :
            flowData.getEvidence().asKeyMap().entrySet()) {
            if (isSent(entry.getKey()) &&
                (sealed == false || TRANSACTION_FIELDS.contains(
                    keySuffix(entry.getKey()).toLowerCase(Locale.ROOT))
                    == false)) {
                evidence.put(entry.getKey(), entry.getValue());
            }
        }
        Map<String, Object> formData =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        // Add the other evidence lowest precedence first, being other
        // prefixes in reverse alphabetical order, then location, fiftyone
        // and server.
        addFormData(formData, evidence,
            getSelectedEvidence(evidence, "other"), hidden);
        // Add cookie evidence.
        addFormData(formData, evidence,
            getSelectedEvidence(evidence, "cookie"), hidden);
        // Add header evidence.
        addFormData(formData, evidence,
            getSelectedEvidence(evidence, "header"), hidden);
        // Add query evidence.
        addFormData(formData, evidence,
            getSelectedEvidence(evidence, "query"), hidden);

        return formData;
    }

    /**
     * Whether Evidence is sent to the cloud service. Every key is sent,
     * including keys the service does not list as accepted and keys with no
     * prefix, apart from the web request and a key whose
     * {@link #keySuffix(String)} is in {@link #ENGINE_FIELDS}, compared
     * without regard to case.
     * @param key the Evidence key
     * @return true if the Evidence is sent
     */
    private boolean isSent(String key) {
        return HTTP_REQUEST_KEY.equalsIgnoreCase(key) == false &&
            HUMAN_CONFIDENCE_PAGE_EVIDENCE.equalsIgnoreCase(key) == false &&
            ENGINE_FIELDS.contains(
                keySuffix(key).toLowerCase(Locale.ROOT)) == false;
    }

    /**
     * The prefix of an Evidence key, being the part before its first
     * separator, or the whole key where it has none.
     * @param key the Evidence key
     * @return the prefix
     */
    private static String keyPrefix(String key) {
        int separator = key.indexOf(Constants.EVIDENCE_SEPERATOR);
        return separator >= 0 ? key.substring(0, separator) : key;
    }

    /**
     * The name an Evidence key is sent under, being the part after its
     * first separator, or the whole key where it has none. The name can
     * hold a dot itself, as id.usage does in query.id.usage.
     * @param key the Evidence key
     * @return the name to send
     */
    private static String keySuffix(String key) {
        int separator = key.indexOf(Constants.EVIDENCE_SEPERATOR);
        return separator >= 0 ?
            key.substring(separator + Constants.EVIDENCE_SEPERATOR.length()) :
            key;
    }

    /**
     * Add form data to the evidence.
     * @param formData the destination map to add the data to
     * @param allEvidence all the Evidence being sent. This is used to
     *                    report which evidence keys are conflicting
     * @param evidence evidence to add to the form data
     * @param hidden the values the conflict warning may not hold, or null
     */
    private void addFormData(
        Map<String, Object> formData,
        Map<String, Object> allEvidence,
        Map<String, Object> evidence,
        List<String> hidden) {

        // Lowest precedence first, so a later key replaces an earlier one.
        // Other prefixes are compared as prefixes, in lower case, then the
        // whole key, so that the order is the same whatever the map gives.
        List<String> evidenceKeys = new ArrayList<>(evidence.keySet());
        evidenceKeys.sort(Comparator
            .comparingInt((String key) -> otherPrefixRank(keyPrefix(key)))
            .thenComparing((String key) ->
                keyPrefix(key).toLowerCase(Locale.ROOT))
            .thenComparing((String key) -> key.toLowerCase(Locale.ROOT))
            .reversed());

        for (String evidenceKey : evidenceKeys) {
            String prefix = keyPrefix(evidenceKey);
            String suffix = keySuffix(evidenceKey);

            // Check and add the evidence to the query parameters.
            if (formData.containsKey(suffix) == false) {
                formData.put(suffix, evidence.get(evidenceKey));
            }
            else {
                // If the queryParameter exists already.
                // Get the conflicting pieces of evidence and then log a
                // warning, if the evidence prefix is not query. Otherwise a
                // warning is not needed as query evidence is expected
                // to overwrite any existing evidence with the same suffix.
                if (prefix.equalsIgnoreCase("query") == false) {
                    Map<String, Object> conflicts = new HashMap<>();
                    for (String key : allEvidence.keySet()) {
                        if (key.equals(evidenceKey) == false &&
                            key.toLowerCase(Locale.ROOT).contains(
                                suffix.toLowerCase(Locale.ROOT))) {
                            conflicts.put(key, allEvidence.get(key));
                        }
                    }

                    StringBuilder conflictStr = new StringBuilder();
                    for (Map.Entry<String, Object> conflict : conflicts.entrySet()) {
                        if (conflictStr.length() > 0) {
                            conflictStr.append(", ");
                        }
                        conflictStr.append(String.format("%s:%s",
                            conflict.getKey(),
                            shown(conflict.getKey(), conflict.getValue())));
                    }

                    String warningMessage = String.format(
                            fiftyone.pipeline.cloudrequestengine.Constants.Messages.EvidenceConflict,
                            evidenceKey,
                            shown(evidenceKey, evidence.get(evidenceKey)),
                            conflictStr.toString());
                    logger.warn(hide(warningMessage, hidden));
                }
                // Replace the existing field, name and value, so the name
                // sent is the winning key's.
                formData.remove(suffix);
                formData.put(suffix, evidence.get(evidenceKey));
            }
        }
    }

    /**
     * The value of an Evidence key as a log entry shows it. The sealed
     * result and the transaction are never shown.
     */
    private static Object shown(String key, Object value) {
        return HIDDEN_FIELDS.contains(keySuffix(key).toLowerCase(Locale.ROOT))
            ? "(not shown)"
            : value;
    }

    /**
     * The precedence of a prefix that is not query, header or cookie,
     * lower first, being server, fiftyone and location as evidence.md
     * lists them, then any other prefix.
     * @param prefix the prefix
     * @return the rank, where a lower rank wins
     */
    private static int otherPrefixRank(String prefix) {
        for (int i = 0; i < OTHER_PREFIX_PRECEDENCE.length; i++) {
            if (OTHER_PREFIX_PRECEDENCE[i].equalsIgnoreCase(prefix)) {
                return i;
            }
        }
        return OTHER_PREFIX_PRECEDENCE.length;
    }

    /**
     * Get evidence with specified prefix.
     * @param evidence the Evidence being sent
     * @param type required evidence key prefix
     * @return evidence with the required key prefix
     */
    Map<String, Object> getSelectedEvidence(Map<String, Object> evidence, String type) {
        Map<String, Object> selectedEvidence = new HashMap<>();

        if (type.equals("other")) {
            for (Map.Entry<String, Object> entry : evidence.entrySet()) {
                if (hasKeyPrefix(entry.getKey(), "query") == false &&
                    hasKeyPrefix(entry.getKey(), "header") == false &&
                    hasKeyPrefix(entry.getKey(), "cookie") == false ) {
                    selectedEvidence.put(entry.getKey(), entry.getValue());
                }
            }
        }
        else {
            for (Map.Entry<String, Object> entry : evidence.entrySet()) {
                if (hasKeyPrefix(entry.getKey(), type)) {
                    selectedEvidence.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return selectedEvidence;
    }

    /**
     * Check that an Evidence key has the given prefix, being the part
     * before its first separator, or the whole key where it has none,
     * compared without regard to case.
     * @param itemKey key to check
     * @param prefix the prefix to check for
     * @return true if the key has the prefix
     */
    private boolean hasKeyPrefix(String itemKey, String prefix) {
        return keyPrefix(itemKey).equalsIgnoreCase(prefix);
    }

    @Override
    protected void unmanagedResourcesCleanup() {
        // Wait for the requests already started, which the connection
        // timeouts bound, so none outlives the engine and every reader
        // still gets its response.
        requests.shutdown();
        try {
            requests.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void setCommonHeaders(Map<String, String> headers) {
        if(cloudRequestOrigin != null &&
            cloudRequestOrigin.length() > 0) {
            headers.put(fiftyone.pipeline.cloudrequestengine.Constants.OriginHeaderName, cloudRequestOrigin);
        }
    }

    /**
     * Convert a string into a {@link URL} instance.
     * <p>
     * Replaces the deprecated {@code new URL(String)} constructor with
     * {@link URI#toURL()}, translating the checked {@link URISyntaxException}
     * into a {@link MalformedURLException} so that callers continue to see the
     * same {@link java.io.IOException} as they did previously.
     * @param spec the string to parse as a URL
     * @return the parsed {@link URL}
     * @throws MalformedURLException if the string is not a valid URL
     */
    private static URL toUrl(String spec) throws MalformedURLException {
        try {
            return new URI(spec).toURL();
        } catch (URISyntaxException e) {
            MalformedURLException ex = new MalformedURLException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    /**
     * Apply the configured timeout to a request.
     */
    private void setTimeouts(HttpURLConnection connection) {
        if (timeoutMillis != null) {
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
        }
    }

    /**
     * The license key sent with each request, and each key in it when it
     * holds several separated by '|', to mask in messages.
     */
    private List<String> getLicenseKeys() {
        List<String> keys = new ArrayList<>();
        if (licenseKey != null && licenseKey.isEmpty() == false) {
            keys.add(licenseKey);
            keys.addAll(Arrays.asList(licenseKey.split(Pattern.quote("|"))));
        }
        return keys;
    }

    private void fetchPublicProperties() throws CloudRequestException, AggregateException, PropertyNotLoadedException {
        try {
            String jsonResult;

            Map<String, String> headers = new HashMap<>();
            setCommonHeaders(headers);

            if (hasLicenseKey()) {
                // The credentials go in the body, not the URL, so a license
                // key stays out of request logs.
                byte[] content = stringJoin(credentialItems(), "&")
                    .getBytes(StandardCharsets.UTF_8);
                headers.put(
                    "Content-Type", "application/x-www-form-urlencoded");
                headers.put("Content-Length", Integer.toString(content.length));
                HttpURLConnection connection =
                    httpClient.connect(toUrl(propertiesEndpoint.trim()));
                setTimeouts(connection);
                jsonResult = httpClient.postData(connection, headers, content);
                validateResponse(jsonResult, connection);
            } else {
                HttpURLConnection connection = httpClient.connect(toUrl(
                    propertiesEndpoint.trim() + "?Resource=" + resourceKey));
                setTimeouts(connection);
                jsonResult = httpClient.getResponseString(connection, headers);
                validateResponse(jsonResult, connection);
            }

            if (jsonResult.isEmpty() == false) {
                setPublicProperties(jsonResult);
            } else {
                throw new RuntimeException("Failed to retrieve available properties " +
                        "from cloud service at " + propertiesEndpoint + ".");
            }
        } catch (IOException e) {
            throw new PropertyNotLoadedException(e);
        }
    }

    /**
     * Read the answer to the accessibleproperties request.
     * @param json the answer
     */
    private void setPublicProperties(String json) {
        AccessiblePropertyMetaData.LicencedProducts accessiblePropertyData =
                new AccessiblePropertyMetaData.LicencedProducts(
                        new JSONObject(json).getJSONObject("Products"));
        accessibleProperties = json;
        publicProperties = accessiblePropertyData.products;
    }

    private void fetchEvidenceKeys() throws CloudRequestException, AggregateException, PropertyNotLoadedException {
        try {
            String jsonResult;

            Map<String, String> headers = new HashMap<>();
            setCommonHeaders(headers);

            HttpURLConnection connection = httpClient.connect(toUrl(evidenceKeysEndpoint.trim()));
            setTimeouts(connection);
            jsonResult = httpClient.getResponseString(connection, headers);
            validateResponse(jsonResult, connection, false);

            if (jsonResult != null && jsonResult.isEmpty() == false) {
                JSONArray jsonArray = new JSONArray(jsonResult);
                List<String> keys = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    keys.add(jsonArray.get(i).toString());
                }
                evidenceKeyFilter.setKeys(keys);
            }
        } catch (IOException e) {
            throw new PropertyNotLoadedException(e);
        }
    }

    /**
     * Validate the JSON response from the cloud service.
     * @param jsonResult the JSON content that is returned from the cloud
     * @param connection a HttpURLConnection
     */
    private void validateResponse(String jsonResult, HttpURLConnection connection)
        throws IOException, CloudRequestException, AggregateException {
        validateResponse(jsonResult, connection, true);
    }
    
    /**
     * Validate the JSON response from the cloud service.
     * @param jsonResult the JSON content that is returned from the cloud
     * @param connection connection used when making the request
     * @param checkForErrorMessages Set to false if the response will 
     * never contain error message text.
     */
    private void validateResponse(String jsonResult, 
        HttpURLConnection connection,
        boolean checkForErrorMessages) 
        throws IOException, CloudRequestException, AggregateException {
        validateResponse(jsonResult, connection, checkForErrorMessages, null);
    }

    /**
     * Validate the JSON response from the cloud service.
     * @param jsonResult the JSON content that is returned from the cloud
     * @param connection connection used when making the request
     * @param checkForErrorMessages Set to false if the response will
     * never contain error message text.
     * @param hidden the values no message may hold, or null
     */
    private void validateResponse(String jsonResult,
        HttpURLConnection connection,
        boolean checkForErrorMessages,
        List<String> hidden)
        throws IOException, CloudRequestException, AggregateException {

        int code = connection.getResponseCode();
        boolean hasData = jsonResult != null && jsonResult.isEmpty() == false;
        List<String> messages = new ArrayList<>();

        if (hasData && checkForErrorMessages) {
            JSONObject jObj;
            try {
                jObj = new JSONObject(jsonResult);
            } catch (JSONException e) {
                // The status says whether the failure is definitive, so it
                // is kept when the body cannot be read.
                CloudRequestException error = new CloudRequestException(
                    String.format(ExceptionResponseNotJson, code),
                    code,
                    connection.getHeaderFields());
                error.initCause(e);
                throw error;
            }
            boolean hasErrors = jObj.keySet().contains("errors");
            hasData = hasErrors ?
                jObj.keySet().size() > 1 :
                jObj.keySet().size() > 0;

            if (hasErrors) {
                JSONArray errors = jObj.getJSONArray("errors");
                messages.addAll(
                    errors.toList()
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.toList()));
            }

            // Each warning is logged, whether or not the response also
            // holds errors. The cloud's warnings can name the license key
            // it was sent.
            JSONArray warnings = jObj.optJSONArray("warnings");
            if (warnings != null) {
                for (Object warning : warnings.toList()) {
                    logger.warn(LicenseKeyMask.maskIn(
                        hide(String.valueOf(warning), hidden),
                        getLicenseKeys()));
                }
            }
        }

        // If there were no errors but there was also no other data
        // in the response then add an explanation to the list of
        // messages.
        if (messages.size() == 0 && hasData == false) {
            String message = String.format(
                MessageNoDataInResponse,
                this.endPoint);
            messages.add(message);
        }
        // If there is no detailed error message, but we got a
        // non-success status code, then add a message to the list
        else if (messages.size() == 0 && code != 200)
        {
            String message = String.format(
                MessageErrorCodeReturned,
                this.endPoint,
                code,
                jsonResult);
            messages.add(message);
        }

        // The cloud's errors can name the license key it was sent.
        messages.replaceAll(m -> LicenseKeyMask.maskIn(
            hide(m, hidden), getLicenseKeys()));

        Map<String, List<String>> headers = null;
        if (messages.size() > 0){
            headers = connection.getHeaderFields();
        }
        final Map<String, List<String>> finalHeaders = headers;

        // If there are any errors returned from the cloud service
        // then throw an exception
        if (messages.size() > 1) {
            throw new AggregateException(
                ExceptionCloudErrorsMultiple,
                messages.stream()
                    .map(m -> new CloudRequestException(m, code, finalHeaders))
                    .collect(Collectors.toList()));
        }
        else if (messages.size() == 1) {
            String message = String.format(
                ExceptionCloudError,
                messages.get(0));
            throw new CloudRequestException(message, code, finalHeaders);
        }
    }
}
