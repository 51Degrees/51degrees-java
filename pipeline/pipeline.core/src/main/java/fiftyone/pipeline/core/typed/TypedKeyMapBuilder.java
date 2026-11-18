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
import fiftyone.pipeline.core.exceptions.PipelineDataException;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Level;
import java.util.logging.Logger;

import static fiftyone.pipeline.util.StringManipulation.stringJoin;

/**
 * Public builder for instances of {@link TypedKeyMap}. This follows the fluent
 * builder pattern.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class TypedKeyMapBuilder {

    private final TypedKeyMap _map;

    /**
     * Construct a new instance.
     * @param isConcurrent true if several threads may add to the resulting
     *                     {@link TypedKeyMap} at the same time, as the
     *                     members of a parallel group do. Adds then take no
     *                     lock, as each is one compare and swap. Reads never
     *                     take a lock.
     */
    public TypedKeyMapBuilder(boolean isConcurrent) {
        if (isConcurrent) {
            _map = new TypedKeyMapConcurrent();
        } else {
            _map = new TypedKeyMapInternal();
        }
    }

    /**
     * Add an item to the map which will be built.
     * @param typedKey key to add the value with
     * @param value value to add
     * @param <T> type of value
     * @return this builder
     */
    public <T> TypedKeyMapBuilder put(TypedKey<T> typedKey, T value) {
        _map.put(typedKey, value);
        return this;
    }

    /**
     * Build the {@link TypedKeyMap} instance.
     * @return map instance
     */
    public TypedKeyMap build() {
        return this._map;
    }

    /**
     * Puts each new head of the list in place with one compare and swap, so
     * that several threads can add at the same time with no lock.
     */
    private static class TypedKeyMapConcurrent extends TypedKeyMapInternal {
        @Override
        boolean swapHead(Entry expected, Entry replacement) {
            return HEAD.compareAndSet(this, expected, replacement);
        }
    }

    /**
     * Implementation of {@link TypedKeyMap} in which the underlying map is
     * provided by a singly linked list, newest first. A node never changes
     * once it is in the list. A change makes a new head, copying the nodes
     * before the one it changes, and puts it in place with
     * {@link #swapHead}, so a reader follows the links with no lock and
     * always sees a whole list.
     */
    private static class TypedKeyMapInternal implements TypedKeyMap, Map<String, Object>, Closeable {

        static final AtomicReferenceFieldUpdater<TypedKeyMapInternal, Entry>
            HEAD = AtomicReferenceFieldUpdater.newUpdater(
                TypedKeyMapInternal.class,
                Entry.class,
                "_head");

        private volatile Entry _head = null;

        /**
         * Put the replacement in place as the head if the head is still the
         * one expected. A plain write here, as only one thread adds.
         * @return true if the replacement is now the head
         */
        boolean swapHead(Entry expected, Entry replacement) {
            _head = replacement;
            return true;
        }

        private static Entry find(Entry head, String typedKeyName) {
            Entry current = head;
            while (current != null) {
                if (current._key.getName().equalsIgnoreCase(typedKeyName)) {
                    return current;
                }
                current = current._next;
            }
            return null;
        }

        private Entry find(String typedKeyName) {
            return find(_head, typedKeyName);
        }

        /**
         * Make a new list from the head given, with the target replaced by
         * the replacement, which may be the target's next node to remove it.
         */
        private static Entry replace(Entry head, Entry target, Entry replacement) {
            List<Entry> before = new ArrayList<>();
            for (Entry current = head; current != target; current = current._next) {
                before.add(current);
            }
            Entry result = replacement;
            for (int i = before.size() - 1; i >= 0; i--) {
                Entry copy = before.get(i);
                result = new Entry(copy._key, copy._value, result);
            }
            return result;
        }

        private String getKeyMissingMessage(String key) {
            return "There is no data for '" + key + "' against this " +
                "instance of '" + getClass().getSimpleName() +
                "'. Available keys are: " +
                stringJoin(getKeys(), ", ");
        }

        @Override
        public <T> T get(TypedKey<T> typedKey) {
            Entry current = find(typedKey.getName());
            if (current != null) {
                // Note - can throw java equivalent of invalid cast exception.
                return typedKey.getType().cast(current._value);
            }
            throw new NoSuchElementException(getKeyMissingMessage(typedKey.getName()));
        }

        @Override
        public <T> T get(Class<T> type) {
            Entry head = _head;
            List<Entry> matches = new ArrayList<>();
            Entry current = head;
            while (current != null) {
                if (current._value.getClass().equals(type)) {
                    matches.add(current);
                }
                current = current._next;
            }

            if (matches.isEmpty()) {
                current = head;
                while (current != null) {
                    if (type.isAssignableFrom(current._value.getClass())) {
                        matches.add(current);
                    }
                    current = current._next;
                }
            }

            if (matches.size() == 1) {
                return type.cast(matches.get(0)._value);
            } else if (matches.size() == 0) {
                throw new PipelineDataException(
                    "This map contains no data matching type " +
                        "'" + type.getSimpleName() + "'");
            } else {
                throw new PipelineDataException("This map contains " +
                    "multiple data instances matching type '" +
                    type.getSimpleName() + "'");
            }
        }

        @Override
        public <T> TryGetResult<T> tryGet(TypedKey<T> key) {

            TryGetResult<T> result = new TryGetResult<>();

            try {
                Entry current = find(key.getName());
                if (current != null) {
                    T value = key.getType().cast(current._value);
                    result.setValue(value);
                }
            } catch (ClassCastException e) {
                // Don't do anything here.
            }

            return result;
        }

        @Override
        public <T> T removeIfExists(TypedKey<T> typedKey) {
            while (true) {
                Entry head = _head;
                Entry target = find(head, typedKey.getName());
                if (target == null) {
                    return null;
                }
                if (swapHead(head, replace(head, target, target._next))) {
                    T value = (T) target._value;
                    target.closeValue();
                    return value;
                }
            }
        }

        @Override
        public <T> void put(TypedKey<T> typedKey, T value) {
            while (true) {
                Entry head = _head;
                Entry current = find(head, typedKey.getName());
                Entry newHead = current == null ?
                    // The key does not exist so add it to the head of the
                    // linked list.
                    new Entry<>(typedKey, value, head) :
                    // A key already exists so replace its node with one that
                    // holds the new value.
                    replace(
                        head,
                        current,
                        new Entry(current._key, value, current._next));
                if (swapHead(head, newHead)) {
                    if (current != null && current._value != null) {
                        current.closeValue();
                    }
                    return;
                }
            }
        }

        /**
         * Add the value if the key is not there, as one step that other
         * threads adding at the same time cannot split.
         * @return the value already held for the key, or null if this value
         * was added
         */
        @Override
        public Object putIfAbsent(String key, Object value) {
            TypedKey<Object> typedKey = new TypedKeyDefault<>(key, Object.class);
            while (true) {
                Entry head = _head;
                Entry current = find(head, key);
                if (current != null) {
                    return current._value;
                }
                if (swapHead(head, new Entry<>(typedKey, value, head))) {
                    return null;
                }
            }
        }

        @Override
        public int size() {
            int count = 0;
            Entry current = _head;
            while (current != null) {
                count++;
                current = current._next;
            }
            return count;
        }

        @Override
        public boolean isEmpty() {
            return _head == null;
        }

        @Override
        public boolean containsKey(Object key) {
            return containsKey((String) key);
        }

        @Override
        public boolean containsValue(Object value) {
            return values().contains(value);
        }

        @Override
        public Object get(Object key) {
            return this.get(new TypedKeyDefault<>((String) key, Object.class));
        }

        @Override
        public Object put(String key, Object value) {
            put(new TypedKeyDefault<>(key, Object.class), value);
            return null;
        }

        @Override
        public Object remove(Object key) {
            removeIfExists(new TypedKeyDefault<>((String) key, Object.class));
            return null;
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            for (Map.Entry<? extends String, ?> entry : m.entrySet()) {
                this.put(new TypedKeyDefault<>(entry.getKey(), Object.class), entry.getValue());
            }
        }

        @Override
        public void clear() {
            for (String key : keySet()) {
                remove(key);
            }
        }

        @Override
        public Set<String> keySet() {
            Entry current = _head;
            Set<String> set = new HashSet<>();
            while (current != null) {
                set.add(current._key.getName());
                current = current._next;
            }
            return set;

        }

        @Override
        public Collection<Object> values() {
            Entry current = _head;
            Set<Object> set = new HashSet<>();
            while (current != null) {
                set.add(current._value);
                current = current._next;
            }
            return set;
        }

        @Override
        public Set<Map.Entry<String, Object>> entrySet() {
            Entry current = _head;
            Set<Map.Entry<String, Object>> set = new HashSet<>();
            while (current != null) {
                set.add(new AbstractMap.SimpleEntry<>(current._key.getName(), current._value));
                current = current._next;
            }
            return set;
        }

        @Override
        public List<String> getKeys() {
            Entry current = _head;
            List<String> keys = new ArrayList<>();
            while (current != null) {
                keys.add(current._key.getName());
                current = current._next;
            }
            return keys;
        }

        @Override
        public Map<String, Object> asStringKeyMap() {
            return this;
        }

        @Override
        public boolean containsKey(String key) {
            return find(key) != null;
        }

        @Override
        public <T> boolean containsKey(TypedKey<T> typedKey) {
            boolean result = false;
            Entry value = find(typedKey.getName());
            if (value != null &&
                typedKey.getType().isAssignableFrom(value._value.getClass())) {
                result = true;
            }
            return result;
        }

        @Override
        public void close() {
            Entry current = _head;
            while (current != null) {
                current.closeValue();
                current = current._next;
            }
        }

        static class Entry<T> {
            final TypedKey<T> _key;
            final T _value;
            final Entry _next;

            Entry(TypedKey<T> key, T value, Entry next) {
                _key = key;
                _value = value;
                _next = next;
            }

            public void closeValue() {
                try {
                    if (_value instanceof AutoCloseable) {
                        ((AutoCloseable) _value).close();
                    }
                } catch (Exception ex) {
                    Logger.getLogger(TypedKeyMapBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
