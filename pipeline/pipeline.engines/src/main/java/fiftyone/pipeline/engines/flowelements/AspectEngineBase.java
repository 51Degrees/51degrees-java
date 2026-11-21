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

package fiftyone.pipeline.engines.flowelements;

import fiftyone.pipeline.core.data.FlowData;
import fiftyone.pipeline.core.data.factories.ElementDataFactory;
import fiftyone.pipeline.core.flowelements.FlowElementBase;
import fiftyone.pipeline.core.typed.TypedKey;
import fiftyone.pipeline.core.typed.TypedKeyDefault;
import fiftyone.pipeline.engines.caching.FlowCache;
import fiftyone.pipeline.engines.configuration.LazyLoadingConfiguration;
import fiftyone.pipeline.engines.data.AspectData;
import fiftyone.pipeline.engines.data.AspectDataBase;
import fiftyone.pipeline.engines.data.AspectPropertyMetaData;
import fiftyone.pipeline.engines.services.MissingPropertyService;
import fiftyone.pipeline.util.Types;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Base class for {@link AspectEngine}s to extend. This implements the data
 * update service and missing property service along with everything
 * already implemented by {@link FlowElementBase}.
 * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/conceptual-overview.md#aspect-engine">Specification</a>
 */
public abstract class AspectEngineBase<
    TData extends AspectData,
    TProperty extends AspectPropertyMetaData>
    extends FlowElementBase<TData, TProperty>
    implements AspectEngine<TData, TProperty> {

    private LazyLoadingConfiguration lazyLoadingConfiguration = null;

    protected MissingPropertyService missingPropertyService;

    protected FlowCache cache = null;

    /**
     * Construct a new instance of the {@link AspectEngine}.
     * @param logger logger instance to use for logging
     * @param aspectDataFactory the factory to use when creating a TData
     *                          instance
     */
    public AspectEngineBase(
        Logger logger,
        ElementDataFactory<TData> aspectDataFactory) {
        super(logger, aspectDataFactory);
    }

    @Override
    public TypedKey<TData> getTypedDataKey() {
        if (typedKey == null) {
            typedKey = new TypedKeyDefault<>(
                getElementDataKey(),
                Types.findSubClassParameterType(this, AspectEngineBase.class, 0));
        }
        return typedKey;

    }

    @Override
    public abstract List<TProperty> getProperties();

    @Override
    public abstract String getDataSourceTier();

    @Override
    public void setCache(FlowCache cache) {
        this.cache = cache;
        cache.setFlowElement(this);
    }

    /**
     * Deprecated, and has no effect. The configuration is kept so that it can
     * be read back, but the Engine always processes on the thread that
     * processes the Flow Data, and no executor is created. Long-running work
     * is a lazy property of the Engine's own Element Data instead.
     * @param configuration ignored apart from being kept
     * @see <a href="https://github.com/51Degrees/specifications/blob/main/pipeline-specification/advanced-features/lazy-loading.md">Specification</a>
     */
    @Override
    public void setLazyLoading(LazyLoadingConfiguration configuration) {
        this.lazyLoadingConfiguration = configuration;
    }

    @Override
    public LazyLoadingConfiguration getLazyLoadingConfiguration() {
        return lazyLoadingConfiguration;
    }

    /**
     * Deprecated. Lazy loading has no effect, so no executor is created.
     * @return always null
     */
    @Override
    public ExecutorService getExecutor() {
        return null;
    }

    /**
     * Extending classes must implement this method. It should perform the
     * required processing and update the specified aspect data instance.
     * @param flowData the {@link FlowData} instance that provides the evidence
     * @param aspectData the {@link AspectData} instance to populate with the
     *                   results of processing
     * @throws Exception if there was an exception during processing
     */
    protected abstract void processEngine(FlowData flowData, TData aspectData) throws Exception;

    /**
     * Implementation of method from the base class {@link FlowElementBase}.
     * This exists to centralise the results caching logic.
     * @param flowData the {@link FlowData} instance that provides the evidence
     *                 and holds the result.
     * @throws Exception if there was an exception during processing
     */
    @Override
    protected final void processInternal(FlowData flowData) throws Exception {
        processWithCache(flowData);
    }

    /**
     * Whether a processed result may be stored in the cache. A result that
     * holds a value created for the one request that asked for it, such as a
     * 51Did, must be neither stored nor served. See the
     * <a href="https://github.com/51Degrees/specifications/blob/d79bbe3b2aef86273570742f6b660f29bfe72816/pipeline-specification/features/caching.md#L151-L156">caching specification</a>:
     * "A cache MUST NOT store a result that holds such a value, and MUST NOT
     * serve one that it finds."
     * <p>
     * It is asked about a processed result before it is stored.
     * {@link #isServable} is asked about a result found in the cache before
     * it is served.
     * @param data the result to check
     * @return true, unless an engine overrides this to keep a result out of
     * the cache
     */
    protected boolean isCacheable(TData data) {
        return true;
    }

    /**
     * Whether a result found in the cache may be served. By default the
     * answer is {@link #isCacheable}'s. An engine whose result is still being
     * worked out by a lazy task when it is stored overrides this, so that a
     * result is served only once its task has finished and the finished
     * result may be served.
     * @param data the result found in the cache
     * @return true if the result may be served
     */
    protected boolean isServable(TData data) {
        return isCacheable(data);
    }

    /**
     * Private method that checks if the result is already in the cache or not.
     * If it is then the result is added to 'data', if not then
     * {@link #processEngine(FlowData, AspectData)}is called to do so.
     * @param flowData the {@link FlowData} instance that provides the evidence
     *                 and holds the result.
     * @throws Exception if there was an exception during processing
     */
    @SuppressWarnings("unchecked")
    private void processWithCache(final FlowData flowData) throws Exception {
        TData cacheResult = null;

        if (cache != null) {
            Object cacheResultsObject = cache.get(flowData);
            if (cacheResultsObject != null &&
                getTypedDataKey().getType().isAssignableFrom(
                    cacheResultsObject.getClass()) &&
                isServable((TData)cacheResultsObject)) {
                // This is checked.
                //noinspection unchecked
                cacheResult = (TData)cacheResultsObject;
            }
        }
        // If we don't have a result from the cache then
        // run through the normal processing.
        if (cacheResult == null) {
            // If the flow data already contains an entry for this
            // element's key then use it. Otherwise, create a new
            // aspect data instance and add it to the flow data.
            final TData aspectData =
                flowData.getOrAdd(getTypedDataKey(), getDataFactory());
            if (aspectData.getEngines().contains(this) == false) {
                ((AspectDataBase) aspectData).addEngine(this);
            }

            // Process on this thread, whether or not the deprecated lazy
            // loading option is set, then cache the result if it may be.
            processEngine(flowData, aspectData);
            putInCache(flowData, aspectData);
        } else {
            // We have a result from the cache so add it
            // into the flow data.
            flowData.getOrAdd(getTypedDataKey(), new DataFactorySimple<>(cacheResult));
        }
    }

    /**
     * Add a processed result to the cache, if there is one and the result
     * may be cached.
     * @param flowData the {@link FlowData} whose Evidence makes the key
     * @param aspectData the processed result
     */
    private void putInCache(FlowData flowData, TData aspectData) {
        if (cache != null && isCacheable(aspectData)) {
            cache.put(flowData, aspectData);
        }
    }

    @Override
    protected void managedResourcesCleanup() {
        if (cache != null) {
            try {
                cache.close();
            } catch (IOException e) {
                logger.warn("Exception closing the cache", e);
            }
        }
    }
}