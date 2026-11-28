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

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.exceptions.PipelineDataException;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds the response from the cloud service and whether the request has
 * started.
 */
public class CloudRequestDataInternal extends AspectDataBase
    implements AutoCloseable {
    private static final String JSON_RESPONSE_KEY = "json-response";
    private static final String PROCESS_STARTED_KEY = "process-started";

    /**
     * The cloud request, started while the engine processes. The JSON
     * response is a lazy property whose value is this task's result, so the
     * task never writes this Element Data.
     */
    private volatile CompletableFuture<String> jsonResponseTask = null;

    /**
     * Set by the first reader to meet the task's failure, so the failure is
     * recorded in the Flow Data's errors once.
     */
    private final AtomicBoolean failureMet = new AtomicBoolean(false);

    /**
     * Whether the cloud request redeems a sealed Human Confidence result,
     * with the License Key and the transaction kept for the page.
     */
    private volatile boolean redeeming = false;

    /**
     * @return true if the cloud request redeems a sealed Human Confidence
     * result
     */
    boolean isRedeeming() {
        return redeeming;
    }

    /**
     * Record that the cloud request redeems a sealed Human Confidence
     * result.
     */
    void setRedeeming() {
        redeeming = true;
    }

    /**
     * The JSON response from the cloud. Where the engine started a request,
     * this waits for it to finish, and every caller gets the same response
     * or the same failure.
     * @return the JSON response, or null if there is none
     * @throws PipelineDataException if the cloud request failed, being the
     * failure itself where it is unchecked, or wrapping it otherwise
     */
    public String getJsonResponse() {
        CompletableFuture<String> task = jsonResponseTask;
        if (task != null) {
            try {
                return task.join();
            } catch (CompletionException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new PipelineDataException(cause.getMessage(), cause);
            }
        }
    	if(super.get(JSON_RESPONSE_KEY) != null)
            return super.get(JSON_RESPONSE_KEY).toString();
		return null;
    }

    /**
     * The JSON response asked for by name waits for the request too.
     */
    @Override
    public Object get(String propertyName) {
        if (jsonResponseTask != null &&
            JSON_RESPONSE_KEY.equalsIgnoreCase(propertyName)) {
            return getJsonResponse();
        }
        return super.get(propertyName);
    }

    /**
     * Add the started cloud request as the task for the JSON response.
     * Called by the engine while it processes.
     * @param task the started request
     */
    void setJsonResponseTask(CompletableFuture<String> task) {
        jsonResponseTask = task;
    }

    /**
     * The failure of the cloud request as it was thrown, waiting for the
     * request to finish.
     * @return the failure, or null if there is no request or it succeeded
     */
    Throwable getRequestFailure() {
        CompletableFuture<String> task = jsonResponseTask;
        if (task == null) {
            return null;
        }
        try {
            task.join();
            return null;
        } catch (CompletionException e) {
            return e.getCause() == null ? e : e.getCause();
        }
    }

    /**
     * @return true for the first caller only, which records the failure
     */
    boolean firstToMeetFailure() {
        return failureMet.compareAndSet(false, true);
    }

    /**
     * Wait for a cloud request still running, so closing the Flow Data does
     * not leave it behind. Readers get any failure, so this ignores it.
     */
    @Override
    protected void onReuse() {
        super.onReuse();
        redeeming = false;
    }

    @Override
    public void close() {
        CompletableFuture<String> task = jsonResponseTask;
        if (task != null) {
            try {
                task.join();
            } catch (CompletionException e) {
                // Left to the readers.
            }
        }
    }

    /**
     * The JSON response as it is now, without waiting for the cloud request,
     * so the engine can check it before the result is cached or served from
     * the cache.
     * @return the JSON response, or null if there is none, or the cloud
     * request is still running or failed
     */
    String getJsonResponseWithoutWaiting() {
        CompletableFuture<String> task = jsonResponseTask;
        if (task != null) {
            return task.isDone() && task.isCompletedExceptionally() == false ?
                task.join() : null;
        }
        Object response = getWithoutWaiting(JSON_RESPONSE_KEY);
        return response == null ? null : response.toString();
    }

    /**
     * Whether the cloud request the engine started is still running.
     * @return true if a request was started and has not finished
     */
    boolean isRequestRunning() {
        CompletableFuture<String> task = jsonResponseTask;
        return task != null && task.isDone() == false;
    }


    /**
     * Create a new instance.
     *
     * @param logger used for logging
     * @param flowData the Flow Data this element data belongs to
     * @param engine the engine which created the instance
     */
    public CloudRequestDataInternal(
        Logger logger,
        FlowData flowData,
        AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
        super(logger, flowData, engine);
    }

    /**
     * Set the JSON response from the cloud service.
     *
     * @param value the response
     */
    public void setJsonResponse(String value) {
        super.put(JSON_RESPONSE_KEY, value);
    }


    /**
     * Flag to confirm that the CloudRequestEngine has started processing.
     * @return true if the engine has started processing
     */
    public Boolean getProcessStarted() {
    	if(super.get(PROCESS_STARTED_KEY) != null)
    		return Boolean.valueOf(super.get(PROCESS_STARTED_KEY).toString());
		return false;
    }
    
    /**
     * Set whether the Cloud Request Engine has started processing.
     *
     * @param value true once it has started
     */
    public void setProcessStarted(Boolean value) {
        super.put(PROCESS_STARTED_KEY, value);
    }

}
