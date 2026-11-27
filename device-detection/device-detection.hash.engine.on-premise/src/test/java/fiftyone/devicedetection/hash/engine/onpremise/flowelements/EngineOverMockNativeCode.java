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

package fiftyone.devicedetection.hash.engine.onpremise.flowelements;

import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.ComponentMetaDataCollectionSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.EngineHashSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.MetaDataSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.PropertyMetaDataCollectionSwig;
import fiftyone.devicedetection.hash.engine.onpremise.interop.swig.VectorStringSwig;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Makes a {@link DeviceDetectionHashEngine} whose native engine is a mock
 * with no properties and no components of its own, so the parts of the
 * engine written in Java can be tested without a data file. The vectors it
 * gives are real native vectors, as the vector proxies are final. The
 * engine's own methods run as written. Its constructor does not run, as it
 * loads a data file, so the fields it would set are set here.
 */
class EngineOverMockNativeCode {

    final DeviceDetectionHashEngine engine;

    final EngineHashSwig nativeEngine;

    EngineOverMockNativeCode() throws Exception {
        nativeEngine = mock(EngineHashSwig.class);
        MetaDataSwig metaData = mock(MetaDataSwig.class);
        PropertyMetaDataCollectionSwig properties =
            mock(PropertyMetaDataCollectionSwig.class);
        when(properties.getSize()).thenReturn(0L);
        ComponentMetaDataCollectionSwig components =
            mock(ComponentMetaDataCollectionSwig.class);
        when(components.getSize()).thenReturn(0L);
        when(metaData.getProperties()).thenReturn(properties);
        when(metaData.getComponents()).thenReturn(components);
        when(nativeEngine.getMetaData()).thenReturn(metaData);

        engine = mock(
            DeviceDetectionHashEngine.class,
            withSettings().defaultAnswer(CALLS_REAL_METHODS));
        set("engine", nativeEngine);
        set("properties", new ArrayList<>());
        set("logger", mock(Logger.class));
        // No data file, so a refresh from memory updates no file metadata.
        set("dataFiles", new ArrayList<>());
        // The property indexes are fixed, as they are after the first load.
        set("propertyIndexes", Collections.emptyMap());
        set("propertyIndexesFixed", true);
        setKeys();
    }

    /**
     * Set the property names the engine was created with, so each name's
     * position is its place in the list.
     * @param names the names
     */
    void setPropertyNames(String... names) throws Exception {
        Map<String, Integer> indexes =
            new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < names.length; i++) {
            indexes.putIfAbsent(names[i], i);
        }
        set("propertyIndexes", indexes);
    }

    /**
     * Set the evidence keys the native engine gives from now on.
     * @param keys the keys
     */
    void setKeys(String... keys) {
        // Each call gives a new native vector, as the native engine does.
        when(nativeEngine.getKeys())
            .thenAnswer(invocation -> new VectorStringSwig(keys));
    }

    private void set(String name, Object value) throws Exception {
        Class<?> type = DeviceDetectionHashEngine.class;
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(engine, value);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
