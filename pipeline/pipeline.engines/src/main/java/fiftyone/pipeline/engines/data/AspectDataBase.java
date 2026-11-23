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

package fiftyone.pipeline.engines.data;

    import fiftyone.pipeline.core.data.ElementDataBase;
    import fiftyone.pipeline.core.data.FlowData;
    import fiftyone.pipeline.core.data.FlowError;
    import fiftyone.pipeline.core.data.TryGetResult;
    import fiftyone.pipeline.engines.exceptions.PropertyMissingException;
    import fiftyone.pipeline.engines.flowelements.AspectEngine;
    import fiftyone.pipeline.engines.services.MissingPropertyResult;
    import fiftyone.pipeline.engines.services.MissingPropertyService;
    import fiftyone.pipeline.exceptions.AggregateException;
    import org.slf4j.Logger;

    import java.util.*;
    import java.util.concurrent.*;
    import java.util.stream.Collectors;

    import static fiftyone.pipeline.util.Check.getNotNull;
    import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Abstract base class for {@link AspectData} which overrides the
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#aspect-engine">Specification</a>
 */
public abstract class AspectDataBase extends ElementDataBase implements AspectData {

    private final MissingPropertyService missingPropertyService;

    private final List<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>> engines;

    private final Map<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>, Future<?>> processFutures;

    /**
     * Whether the cloud request for the Flow Data failed.
     */
    private volatile boolean cloudRequestFailed = false;

    /**
     * Constructs a new instance with a non-thread-safe, case-insensitive
     * {@link Map} as the underlying storage.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     * @param engine the engine which created the instance
     */
    public AspectDataBase(
        Logger logger,
        FlowData flowData,
        AspectEngine<? extends AspectData, ? extends AspectPropertyMetaData> engine) {
        this(logger, flowData, engine, null);
    }

    /**
     * Constructs a new instance with a non-thread-safe, case-insensitive
     * {@link Map} as the underlying storage.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     * @param engine the engine which created the instance
     * @param missingPropertyService service used to determine the reason for
     *                               a property value being missing
     */
    public AspectDataBase(
        Logger logger,
        FlowData flowData,
        AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> engine,
        MissingPropertyService missingPropertyService) {
        super(logger, flowData);
        this.engines = new ArrayList<>();
        this.engines.add(getNotNull(engine, "Engine must not be null"));
        this.processFutures = new HashMap<>();
        this.missingPropertyService = missingPropertyService;
    }

    /**
     * Constructs a new instance with a custom {@link Map} as the underlying
     * storage.
     * @param logger used for logging
     * @param flowData the {@link FlowData} instance this element data will be
     *                 associated with
     * @param engine the engine which created the instance
     * @param missingPropertyService service used to determine the reason for
     *                               a property value being missing
     * @param map the custom {@link Map} implementation to use as the underlying
     *            storage
     */
    public AspectDataBase(
        Logger logger,
        FlowData flowData,
        AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> engine,
        MissingPropertyService missingPropertyService,
        Map<String, Object> map) {
        super(logger, flowData, map);
        this.engines = new ArrayList<>();
        this.engines.add(getNotNull(engine, "Engine must not be null"));
        this.processFutures = new HashMap<>();
        this.missingPropertyService = missingPropertyService;
    }

    @Override
    public List<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>> getEngines() {
        return Collections.unmodifiableList(engines);
    }

    @Override
    public Future<?> getProcessFuture() {
        return new Future<Object>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean result = true;
                for (Future<?> future : processFutures.values()) {
                    result = future.cancel(mayInterruptIfRunning) && result;
                }
                return result;
            }

            @Override
            public boolean isCancelled() {
                for (Future<?> future : processFutures.values()) {
                    if (future.isCancelled()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean isDone() {
                for (Future<?> future : processFutures.values()) {
                    if (future.isDone() == false) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public Object get() throws InterruptedException, ExecutionException {
                for (Future<?> future : processFutures.values()) {
                    future.get();
                }
                return null;
            }

            @Override
            public Object get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
                for (Future<?> future : processFutures.values()) {
                    future.get(timeout, unit);
                }
                return null;
            }
        };
    }

    /**
     * Record that the cloud request for the Flow Data failed, so that a
     * property this instance does not hold is missing with the
     * CloudRequestFailed reason.
     * @throws IllegalStateException if the Flow Element has finished
     */
    public void setCloudRequestFailed() {
        checkWriting();
        cloudRequestFailed = true;
    }

    /**
     * Add an engine to the list of engines which have generated the data within
     * this instance.
     * @param engine engine adding data
     * @throws IllegalStateException if the Flow Element has finished
     */
    public void addEngine(AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> engine) {
        checkWriting();
        engines.add(engine);
    }

    /**
     * Keep only the engine that made this instance, and forget the process
     * tasks and failed cloud request of the request it served before.
     */
    @Override
    protected void onReuse() {
        super.onReuse();
        engines.subList(1, engines.size()).clear();
        processFutures.clear();
        cloudRequestFailed = false;
    }

    /**
     * Deprecated. Runs the callable on the calling thread, as lazy loading
     * has no effect. An exception it throws is given to each later read of a
     * property value, as before.
     * @param runnable processing runnable
     * @throws IllegalStateException if the Flow Element has finished
     */
    public void addProcessCallable(ProcessCallable runnable) {
        checkWriting();
        FutureTask<Void> task = new FutureTask<>(runnable);
        task.run();
        processFutures.put(runnable.engine, task);
    }

    @Override
    public Map<String, Object> asKeyMap() {
        waitOnAllProcessFutures();
        return super.asKeyMap();
    }

    /**
     * Get the value stored for the key as it is now, without waiting on the
     * callables added through {@link #addProcessCallable} and without the
     * checks against the {@link MissingPropertyService}. The value is null
     * when nothing has been stored for the key.
     * @param key the key to read
     * @return the value stored for the key, or null
     */
    protected Object getWithoutWaiting(String key) {
        return super.asKeyMap().get(key);
    }

    /**
     * Gets the value stored using the specified key with full checks
     * against the {@link MissingPropertyService}.
     * @param propertyName to get the value for
     * @return value of the property
     * @throws PropertyMissingException the property was not found
     */
    @Override
    public Object get(String propertyName) throws PropertyMissingException {
        return getAs(propertyName, Object.class);
    }

    @Override
    protected <T> T getAs(String key, Class<T> type, Class<?>... parameterisedTypes) {
        getNotNull(key, "Key cannot be null.");
        if (logger.isDebugEnabled()) {
            logger.debug("AspectData '" + getClass().getSimpleName() +
                "'-'" + hashCode() + "' property value requested for key '" +
                key + "'.");
        }
        TryGetResult<T> result;
        List<FlowError> errors;
        if (processFutures.isEmpty() ||
            (errors = waitOnAllProcessFutures()).size() == 0) {
            result = tryGetValue(key, type, parameterisedTypes);
            if (result.hasValue() == false &&
                missingPropertyService != null) {
                // If there was no entry for the key then use the missing
                // property service to find out why.
                MissingPropertyResult missingReason = cloudRequestFailed ?
                    missingPropertyService.getCloudRequestFailedReason(
                        key,
                        engines) :
                    missingPropertyService.getMissingPropertyReason(
                        key,
                        engines);
                logger.debug("Property '" + key + "' missing from " +
                    "aspect data '" + getClass().getName() + "'-'" +
                    hashCode() + "'. " + missingReason.getReason());
                throw new PropertyMissingException(
                    missingReason.getReason(),
                    key,
                    missingReason.getDescription());
            }
        } else {
            Throwable e;
            if (errors.size() == 1) {
                // A callable added through addProcessCallable threw.
                e = errors.get(0).getThrowable();
                throw new RuntimeException(
                    "Failed to retrieve property '" + key + "' " +
                        "because processing threw an exception in engine(s) " +
                        stringJoin(getDistinctEngineNames(), ", ") + ".",
                    e);
            }
            else {
                // More than one callable added through addProcessCallable
                // threw.
                throw new AggregateException(
                    "Failed to retrieve property '" + key + "' " +
                        "because processing threw multiple exceptions in engine(s) " +
                        stringJoin(getDistinctEngineNames(), ", ") + ".",
                    errors.stream()
                    .map(FlowError::getThrowable)
                    .collect(Collectors.toList()));
            }
        }
        return result.getValue();

    }

    /**
     * Get the value associated with the specified key. Inheriting classes can
     * override this method where they access data in different ways.
     * @param key the string key to retrieve the value for
     * @param type will be populated with the value for the specified key
     * @param parameterisedTypes any parameterised types the value has
     * @param <T> the type of the value to be returned
     * @return a 'true' {@link TryGetResult} if the key is present in the data
     * store, a 'false' {@link TryGetResult} if not
     */
    protected <T> TryGetResult<T> tryGetValue(
        String key,
        Class<T> type,
        Class<?>... parameterisedTypes) {
        TryGetResult<T> result = new TryGetResult<>();
        Map<String, Object> map = asKeyMap();
        if (map.containsKey(key)) {
            Object obj = asKeyMap().get(key);

            try {
                T value = type.cast(obj);
                result.setValue(value);
            } catch (ClassCastException e) {
                throw new ClassCastException("Expected property '" + key +
                    "' to be of type '" + type.getSimpleName() +
                    "' but it is '" + obj.getClass().getSimpleName() + "'");
            }
        }
        return result;
    }

    /**
     * Returns a list containing any errors thrown by the callables in
     * {@link #processFutures}, each of which has already run.
     * @return list of errors
     */
    private List<FlowError> waitOnAllProcessFutures() {
        List<FlowError> errors = new ArrayList<>();
        for (Map.Entry<AspectEngine<? extends AspectData,? extends AspectPropertyMetaData>, Future<?>> entry :
            processFutures.entrySet()) {
            try {
                entry.getValue().get();
            } catch (Exception e) {
                errors.add(new FlowError.Default(e, entry.getKey()));
            }
        }
        return errors;
    }

    /**
     * Get a list of all the unique class names of the engines contained in the
     * {@link #processFutures} map.
     * @return list of distinct engine classes
     */
    private List<String> getDistinctEngineNames() {
        List<String> strings = new ArrayList<>();
        for (AspectEngine<? extends AspectData,? extends AspectPropertyMetaData> engine : processFutures.keySet()) {
            if (strings.contains(engine.getClass().getName()) == false) {
                strings.add(engine.getClass().getName());
            }
        }
        return strings;
    }
}
