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

package fiftyone.devicedetection.hash.engine.onpremise.interop;

import fiftyone.devicedetection.hash.engine.onpremise.data.ComponentMetaDataHash;
import fiftyone.devicedetection.hash.engine.onpremise.flowelements.DeviceDetectionHashEngine;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.ComponentMetaDataCollectionSwig;
import fiftyone.pipeline.engines.fiftyone.data.CollectionIterableBase;
import fiftyone.pipeline.engines.fiftyone.data.ComponentMetaData;

/**
 * Class which adds the {@link Iterable} and {@link AutoCloseable} interfaces to
 * a native collection representing {@link ComponentMetaData}s.
 */
public class ComponentIterable
    extends CollectionIterableBase<ComponentMetaData> {

    private final ComponentMetaDataCollectionSwig collection;

    private final DeviceDetectionHashEngine engine;

    private final long nativeSize;

    private final ComponentMetaData[] added;

    /**
     * Create a new instance.
     * @param engine the engine which the meta data relates to
     * @param collection the native collection of components
     */
    public ComponentIterable(
        DeviceDetectionHashEngine engine,
        ComponentMetaDataCollectionSwig collection) {
        this(engine, collection, collection.getSize());
    }

    /**
     * Create a new instance which gives the native components and then the
     * components given, such as the Metrics component, which the data file
     * does not have.
     * @param engine the engine which the meta data relates to
     * @param collection the native collection of components
     * @param added components to give after the native ones
     */
    public ComponentIterable(
        DeviceDetectionHashEngine engine,
        ComponentMetaDataCollectionSwig collection,
        ComponentMetaData... added) {
        this(engine, collection, collection.getSize(), added);
    }

    private ComponentIterable(
        DeviceDetectionHashEngine engine,
        ComponentMetaDataCollectionSwig collection,
        long nativeSize,
        ComponentMetaData... added) {
        super(nativeSize + added.length);
        this.engine = engine;
        this.collection = collection;
        this.nativeSize = nativeSize;
        this.added = added;
    }

    @Override
    protected ComponentMetaData get(long index) {
        return index < nativeSize ?
            new ComponentMetaDataHash(engine, collection.getByIndex(index)) :
            added[(int) (index - nativeSize)];
    }

    @Override
    public void close() {
        collection.delete();
    }
}
