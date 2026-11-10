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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
    /**
     * The Evidence key of the whole web request, which the web integration
     * adds for usage sharing to read. It holds every header and cookie of
     * the request, so it is never sent to the cloud service. The usage
     * sharing package defines the same key, and this package does not
     * depend on that one.
     */
    private static final String HTTP_REQUEST_KEY = "server.http-request";

    private HttpClient httpClient;

    private String endPoint;
    private String resourceKey;
    private String licenseKey;
    private String propertiesEndpoint;
    private String evidenceKeysEndpoint;
    private String cloudRequestOrigin;
    private Integer timeoutMillis;

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

    private List<AspectPropertyMetaData> propertyMetaData;

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
     * Held while the discovery requests are made, so that concurrent first
     * uses make them once.
     */
    private final Object discoveryLock = new Object();

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

    @SuppressWarnings("this-escape")
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
        super(logger, aspectDataFactory);

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

        propertyMetaData = new ArrayList<>();
        propertyMetaData.add(new AspectPropertyMetaDataDefault(
                "json-response",
                this,
                "",
                String.class,
                new ArrayList<String>(),
                true));
        propertyMetaData.add(new AspectPropertyMetaDataDefault(
                "process-started",
                this,
                "",
                Boolean.class,
                new ArrayList<String>(),
                true));
    }

    @Override
    public List<AspectPropertyMetaData> getProperties() {
        return propertyMetaData;
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

    @Override
    protected void processEngine(FlowData data, CloudRequestData aspectData) throws IOException {
        ((CloudRequestDataInternal)aspectData).setProcessStarted(true);
        completeDiscovery();

        byte[] content = getContent(data);
        HttpURLConnection connection = httpClient.connect(toUrl(endPoint.trim()));
        setTimeouts(connection);

        Map<String, String> headers = new HashMap<>();
        setCommonHeaders(headers);
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Content-Length", Integer.toString(content.length));

        String response = httpClient.postData(connection, headers, content);

        ((CloudRequestDataInternal)aspectData).setJsonResponse(response);

        validateResponse(response, connection);
    }

    /**
     * Generate the Content to send in the POST request. The evidence keys
     * e.g. 'query.' and 'header.' have an order of precedence. These are
     * added to the evidence in reverse order, if there is conflict then
     * the queryData value is overwritten.
     * 'query.' evidence should take precedence over all other evidence.
     * If there are evidence keys other than 'query.' that conflict then
     * this is unexpected so a warning will be logged.
     * @param data the FlowData
     * @return form content for a POST request
     * @throws UnsupportedEncodingException
     */
    private byte[] getContent(FlowData data) throws UnsupportedEncodingException {

        Map<String, Object> formData = getFormData(data);
        List<String> formItems = new ArrayList<>();

        formItems.add("resource=" + resourceKey);
        if(licenseKey != null && licenseKey.isEmpty() == false){
            formItems.add("license=" + licenseKey);
        }

        List<String> formKeys = Arrays.asList(formData.keySet().toArray(new String[0]));
        Collections.sort(formKeys, Collections.reverseOrder());
        for (String key : formKeys) {
            formItems.add(key + "=" + URLEncoder.encode(formData.get(key).toString(), "UTF-8"));

        }

        String string = stringJoin(formItems, "&");

        return string.getBytes(StandardCharsets.UTF_8);
    }

    Map<String, Object> getFormData(FlowData flowData) {
        Map<String, Object> evidence = new HashMap<>(
            flowData.getEvidence().asKeyMap());
        evidence.keySet().removeIf(HTTP_REQUEST_KEY::equalsIgnoreCase);
        Map<String, Object> formData = new HashMap<>();

        // Add evidence in reverse alphabetical order, excluding special keys.
        addFormData(formData, evidence, getSelectedEvidence(evidence, "other"));
        // Add cookie evidence.
        addFormData(formData, evidence, getSelectedEvidence(evidence, "cookie"));
        // Add header evidence.
        addFormData(formData, evidence, getSelectedEvidence(evidence, "header"));
        // Add query evidence.
        addFormData(formData, evidence, getSelectedEvidence(evidence, "query"));

        return formData;
    }

    /**
     * Add form data to the evidence.
     * @param formData the destination map to add the data to
     * @param allEvidence all evidence in the FlowData. This is used to
     *                    report which evidence keys are conflicting
     * @param evidence evidence to add to the form data
     */
    private void addFormData(
        Map<String, Object> formData,
        Map<String, Object> allEvidence,
        Map<String, Object> evidence) {

        List<String> evidenceKeys = Arrays.asList(evidence.keySet().toArray(new String[0]));
        Collections.sort(evidenceKeys, Collections.reverseOrder());

        for (String evidenceKey : evidenceKeys) {
            // Split at the first separator only. Everything after the prefix
            // is the name the cloud service expects, and that name can hold
            // a dot itself, as id.usage does in query.id.usage.
            String[] evidenceKeyParts = evidenceKey.split(
                Pattern.quote(Constants.EVIDENCE_SEPERATOR), 2);
            String prefix = evidenceKeyParts[0];
            String suffix = evidenceKeyParts[1];

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
                if (prefix.equals("query") == false) {
                    Map<String, Object> conflicts = new HashMap<>();
                    for (String key : allEvidence.keySet()) {
                        if (key.equals(evidenceKey) == false && key.contains(suffix)) {
                            conflicts.put(key, allEvidence.get(key));
                        }
                    }

                    StringBuilder conflictStr = new StringBuilder();
                    for (Map.Entry<String, Object> conflict : conflicts.entrySet()) {
                        if (conflictStr.length() > 0) {
                            conflictStr.append(", ");
                        }
                        conflictStr.append(String.format("%s:%s", conflict.getKey(), conflict.getValue()));
                    }

                    String warningMessage = String.format(
                            fiftyone.pipeline.cloudrequestengine.Constants.Messages.EvidenceConflict,
                            evidenceKey,
                            evidence.get(evidenceKey),
                            conflictStr.toString());
                    logger.warn(warningMessage);
                }
                // Overwrite the existing queryParameter value.
                formData.put(suffix, evidence.get(evidenceKey));
            }
        }
    }

    /**
     * Get evidence with specified prefix.
     * @param evidence all evidence in the FlowData
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
     * Check that the key of a KeyValuePair has the given prefix.
     * @param itemKey key to check
     * @param prefix the prefix to check for
     * @return true if the key has the prefix
     */
    private boolean hasKeyPrefix(String itemKey, String prefix) {
        return itemKey.startsWith(prefix + ".");
    }

    @Override
    protected void unmanagedResourcesCleanup() {
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

    private void fetchPublicProperties() throws CloudRequestException, AggregateException, PropertyNotLoadedException {
        try {
            String jsonResult;

            Map<String, String> headers = new HashMap<>();
            setCommonHeaders(headers);

            HttpURLConnection connection = httpClient.connect(toUrl(propertiesEndpoint.trim() + (resourceKey != null ? "?Resource=" + resourceKey : "")));
            setTimeouts(connection);
            jsonResult = httpClient.getResponseString(connection, headers);
            validateResponse(jsonResult, connection);

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
