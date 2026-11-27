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

import java.util.*;

import static org.junit.Assert.*;

public class TypedKeyMapTestBase {

    TypedKeyMap map;


    protected void TypedMap_AddRetrieve() {
        String dataToStore = "testdata";
        TypedKey<String> key = new TypedKeyDefault<>("datakey", String.class);
        map.put(key, dataToStore);

        String result = map.get(key);

        assertEquals(dataToStore, result);
    }

    protected void TypedMap_ComplexValueType() {
        ComplexValue dataToStore =
            new ComplexValue(1, "testdata");
        TypedKey<ComplexValue> key =
            new TypedKeyDefault<>("datakey", ComplexValue.class);
        map.put(key, dataToStore);

        ComplexValue result = map.get(key);

        assertTrue(result.getKey().equals(dataToStore.getKey()) &&
            result.getValue().equals(dataToStore.getValue()));
    }

    protected void TypedMap_ComplexReferenceType() {
        StringList dataToStore = new StringList("a", "b", "c");
        TypedKey<StringList> key = new TypedKeyDefault<>("datakey", StringList.class);
        map.put(key, dataToStore);

        StringList result = map.get(key);

        assertEquals(3, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    protected void TypedMap_MultipleDataObjects() {
        ComplexValue dataToStore1 =
            new ComplexValue(1, "testdata");
        TypedKey<ComplexValue> key1 = new TypedKeyDefault<>("datakey1", ComplexValue.class);
        map.put(key1, dataToStore1);
        String dataToStore2 = "testdata";
        TypedKey<String> key2 = new TypedKeyDefault<>("datakey2", String.class);
        map.put(key2, dataToStore2);

        ComplexValue result1 = map.get(key1);
        String result2 = map.get(key2);

        assertTrue(result1.getKey().equals(dataToStore1.getKey()) &&
            result1.getValue().equals(dataToStore1.getValue()));
        assertEquals(dataToStore2, result2);
    }

    protected void TypedMap_Overwrite() {
        ComplexValue dataToStore1 =
            new ComplexValue(1, "testdata");
        TypedKey<ComplexValue> key1 = new TypedKeyDefault<>("datakey", ComplexValue.class);
        map.put(key1, dataToStore1);
        String dataToStore2 = "testdata";
        TypedKey<String> key2 = new TypedKeyDefault<>("datakey", String.class);
        map.put(key2, dataToStore2);

        String result = map.get(key2);

        assertEquals(dataToStore2, result);
    }

    protected void TypedMap_NoData() {
        TypedKey<String> key = new TypedKeyDefault<>("datakey", String.class);

        String result = map.get(key);
    }


    protected void TypedMap_WrongKeyType() {
        ComplexValue dataToStore1 =
            new ComplexValue(1, "testdata");
        TypedKey<ComplexValue> key1 = new TypedKeyDefault<>("datakey", ComplexValue.class);
        map.put(key1, dataToStore1);
        TypedKey<String> key2 = new TypedKeyDefault<>("datakey", String.class);

        String result = map.get(key2);
    }

    protected void TypedMap_NullValue() {
        StringList dataToStore = null;
        TypedKey<StringList> key = new TypedKeyDefault<>("datakey", StringList.class);
        map.put(key, dataToStore);

        StringList result = map.get(key);

        assertNull(result);
    }

    protected void TypedMap_GetByType() {
        String dataToStore = "TEST";
        TypedKey<String> key = new TypedKeyDefault<>("datakey", String.class);
        map.put(key, dataToStore);

        String result = map.get(String.class);

        assertEquals("TEST", result);
    }

    protected void TypedMap_GetByTypeNoMatch() {
        String dataToStore = "TEST";
        TypedKey<String> key = new TypedKeyDefault<>("datakey", String.class);
        map.put(key, dataToStore);

        int result = map.get(int.class);
    }


    protected void TypedMap_GetByTypeMultiMatch() {
        String dataToStore = "TEST";
        TypedKey<String> key1 = new TypedKeyDefault<>("datakey1", String.class);
        TypedKey<String> key2 = new TypedKeyDefault<>("datakey2", String.class);
        map.put(key1, dataToStore);
        map.put(key2, dataToStore);

        String result = map.get(String.class);
    }

    protected void TypedMap_GetByTypeInterface() {
        StringBuilder dataToStore = new StringBuilder("TEST");
        TypedKey<StringBuilder> key =
            new TypedKeyDefault<>("datakey", StringBuilder.class);
        map.put(key, dataToStore);

        CharSequence result = map.get(CharSequence.class);

        assertEquals(4, result.length());
    }


    protected void TypedMap_TryGetValue_GoodKey_String() {
        String dataToStore = "TEST";
        TypedKey<String> key1 = new TypedKeyDefault<>("datakey1", String.class);
        map.put(key1, dataToStore);

        TryGetResult<String> result = map.tryGet(key1);

        assertTrue(result.hasValue());
        assertEquals(dataToStore, result.getValue());
    }

    protected void TypedMap_TryGetValue_BadKeyName_String() {
        String dataToStore = "TEST";
        TypedKey<String> key1 = new TypedKeyDefault<>("datakey1", String.class);
        TypedKey<String> key2 = new TypedKeyDefault<>("datakey2", String.class);
        map.put(key1, dataToStore);

        TryGetResult<String> result = map.tryGet(key2);

        assertFalse(result.hasValue());
        assertNull(result.getValue());
    }

    protected void TypedMap_TryGetValue_BadKeyType_String() {
        String dataToStore = "TEST";
        TypedKey<String> key1 = new TypedKeyDefault<>("datakey1", String.class);
        TypedKey<Integer> key2 = new TypedKeyDefault<>("datakey1", Integer.class);
        map.put(key1, dataToStore);

        TryGetResult<Integer> result = map.tryGet(key2);

        assertFalse(result.hasValue());
        assertNull(result.getValue());
    }


    protected void TypedMap_TryGetValue_GoodKey_ComplexType() {
        StringList dataToStore = new StringList("TEST");
        TypedKey<StringList> key1 = new TypedKeyDefault<>("datakey1", StringList.class);
        map.put(key1, dataToStore);

        TryGetResult<StringList> result = map.tryGet(key1);

        assertTrue(result.hasValue());
        assertEquals(dataToStore.get(0), result.getValue().get(0));
    }

    protected void TypedMap_TryGetValue_GoodKeyInterface_ComplexType() {
        StringList dataToStore = new StringList("TEST");
        TypedKey<StringList> key1 = new TypedKeyDefault<>("datakey1", StringList.class);
        TypedKey<Strings> key2 = new TypedKeyDefault<>("datakey1", Strings.class);
        map.put(key1, dataToStore);

        TryGetResult<Strings> result = map.tryGet(key2);

        assertTrue(result.hasValue());
        assertEquals(dataToStore.get(0), result.getValue().get(0));
    }

    protected void TypedMap_TryGetValue_BadKeyName_ComplexType() {
        StringList dataToStore = new StringList("TEST");
        TypedKey<StringList> key1 = new TypedKeyDefault<>("datakey1", StringList.class);
        TypedKey<StringList> key2 = new TypedKeyDefault<>("datakey2", StringList.class);
        map.put(key1, dataToStore);

        TryGetResult<StringList> result = map.tryGet(key2);

        assertFalse(result.hasValue());
        assertNull(result.getValue());
    }

    protected void TypedMap_TryGetValue_BadKeyType_ComplexType() {
        StringList dataToStore = new StringList("TEST");
        TypedKey<StringList> key1 = new TypedKeyDefault<>("datakey1", StringList.class);
        TypedKey<ComplexValue> key2 = new TypedKeyDefault<>("datakey1", ComplexValue.class);
        map.put(key1, dataToStore);

        TryGetResult<ComplexValue> result = map.tryGet(key2);

        assertFalse(result.hasValue());
        assertNull(result.getValue());
    }

    /**
     * A value of a type with no type parameters, as a typed key can only
     * name such a type.
     */
    protected static class ComplexValue {
        private final Integer key;
        private final String value;

        ComplexValue(Integer key, String value) {
            this.key = key;
            this.value = value;
        }

        Integer getKey() {
            return key;
        }

        String getValue() {
            return value;
        }
    }

    /**
     * A list of strings, as an interface with no type parameters.
     */
    protected interface Strings extends List<String> {
    }

    /**
     * A list of strings with no type parameters.
     */
    protected static class StringList extends ArrayList<String>
        implements Strings {
        private static final long serialVersionUID = 1L;

        StringList(String... values) {
            super(Arrays.asList(values));
        }
    }
}
