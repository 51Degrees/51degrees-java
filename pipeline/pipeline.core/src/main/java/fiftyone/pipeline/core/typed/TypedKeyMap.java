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

package fiftyone.pipeline.core.typed;

import fiftyone.pipeline.core.data.TryGetResult;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * Provides access to a type safe collection of data.
 *
 * The collection is not necessarily stored in a type safe way, in other words,
 * irrespective that item a and b have same name and different type are not
 * equal, they may overwrite each other in the map.
 *
 * In any case, when accessed via the {@link #asStringKeyMap()} method, they will
 * arbitrarily overwrite each other.
 */
public interface TypedKeyMap extends Closeable {
    /**
     * Get a value from a typed key
     *
     * @param typedKey the value to get
     * @param <T>      type of the value
     * @return a value or null if not present
     */
    <T> T get(TypedKey<T> typedKey);


    <T> T get(Class<T> type);

    <T> TryGetResult<T> tryGet(TypedKey<T> key);

    /**
     * Put a key and value. May overwrite a typedKey of the same name.
     *
     * @param typedKey the key to add
     * @param value    the value
     * @param <T>      the type of the value
     */
    <T> void put(TypedKey<T> typedKey, T value);

    /**
     * Add a value unless one with the same name is already there, and return
     * whichever value the map then holds for the name. Implementations that
     * several threads add to do it without a lock.
     *
     * @param typedKey the key to add
     * @param value    the value to add if the name is not there yet
     * @param <T>      the type of the value
     * @return the value now held for the name
     */
    default <T> T putIfAbsent(TypedKey<T> typedKey, T value) {
        if (containsKey(typedKey.getName()) == false) {
            put(typedKey, value);
        }
        return get(typedKey);
    }

    <T> T removeIfExists(TypedKey<T> typedKey);

    boolean containsKey(String key);

    <T> boolean containsKey(TypedKey<T> typedKey);

    Map<String, Object> asStringKeyMap();

    @Override
    void close();

    List<String> getKeys();

    int size();
}
