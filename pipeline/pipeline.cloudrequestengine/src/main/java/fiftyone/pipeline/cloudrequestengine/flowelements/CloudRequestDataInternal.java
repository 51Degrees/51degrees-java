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
import fiftyone.pipeline.core.data.LazyTask;
import fiftyone.pipeline.core.data.TryGetResult;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.flowelements.AspectEngine;
import org.slf4j.Logger;

import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The Cloud Request Engine's Element Data. json-response is a lazy property
 * whose task is the HTTP request the Engine starts while it processes. Like
 * all Element Data, it is not safe to use from several threads at once,
 * apart from json-response, which any number of readers wait on.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/pipeline-elements/cloud-request-engine.md#element-data">Specification</a>
 */
public class CloudRequestDataInternal
    extends AspectDataBase
    implements AutoCloseable {
    private static final String JSON_RESPONSE_KEY = "json-response";
    private static final String PROCESS_STARTED_KEY = "process-started";

    private volatile LazyTask<String> jsonResponseTask = null;

    private final AtomicBoolean failureRecorded = new AtomicBoolean(false);

    /**
     * Wait for the response, if the request is still running, and return
     * it. A failed request throws its error, to every reader.
     * @return the JSON response, or null if there is none
     */
    public String getJsonResponse() {
        LazyTask<String> task = jsonResponseTask;
        if (task != null) {
            return task.get();
        }
    	if(super.get(JSON_RESPONSE_KEY) != null)
            return super.get(JSON_RESPONSE_KEY).toString();
		return null;
    }

    /**
     * Read json-response from a Flow Element that is processing the Flow
     * Data. A failed request is recorded once in the Flow Data's errors, as
     * an error of the Cloud Request Engine and not of the reader, and null
     * is returned, as it is when there is no response.
     * @param processing the Flow Data being processed
     * @return the JSON response, or null if there is none
     */
    public String getJsonResponse(FlowData processing) {
        try {
            return getJsonResponse();
        } catch (RuntimeException e) {
            if (failureRecorded.compareAndSet(false, true)) {
                Throwable failure =
                    e instanceof CompletionException && e.getCause() != null ?
                        e.getCause() :
                        e;
                processing.addError(failure, getEngines().get(0));
            }
            return null;
        }
    }

    /**
     * Set the task that gives json-response. Used by the Cloud Request
     * Engine while it processes.
     * @param task the started request
     */
    public void setJsonResponseTask(LazyTask<String> task) {
        jsonResponseTask = task;
    }

    @Override
    protected <T> TryGetResult<T> tryGetValue(
        String key,
        Class<T> type,
        Class<?>... parameterisedTypes) {
        LazyTask<String> task = jsonResponseTask;
        if (task != null && JSON_RESPONSE_KEY.equalsIgnoreCase(key)) {
            TryGetResult<T> result = new TryGetResult<>();
            result.setValue(type.cast(task.get()));
            return result;
        }
        return super.tryGetValue(key, type, parameterisedTypes);
    }

    /**
     * Wait for a request that is still running, so that nothing it uses is
     * freed under it. A failure is left for the readers.
     */
    @Override
    public void close() {
        LazyTask<String> task = jsonResponseTask;
        if (task != null) {
            try {
                task.get();
            } catch (RuntimeException e) {
                // A reader gets the failure from json-response.
            }
        }
    }


    public CloudRequestDataInternal(
        Logger logger,
        FlowData flowData,
        AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
        super(logger, flowData, engine);
    }

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
    
    public void setProcessStarted(Boolean value) {
        super.put(PROCESS_STARTED_KEY, value);
    }

}
