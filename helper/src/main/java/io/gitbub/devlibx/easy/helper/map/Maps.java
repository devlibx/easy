package io.gitbub.devlibx.easy.helper.map;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class Maps {

    public static <K, V> Map<K, V> of(K key1, V value1) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        return map;
    }

    public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }

    public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2, K key3, V value3) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        return map;
    }

    public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        return map;
    }

    public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        return map;
    }

    public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5, K key6, V value6) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        map.put(key6, value6);
        return map;
    }

    public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5, K key6, V value6, K key7, V value7) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        map.put(key6, value6);
        map.put(key7, value7);
        return map;
    }

    public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5, K key6, V value6, K key7, V value7, K key8, V value8) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        map.put(key6, value6);
        map.put(key7, value7);
        map.put(key8, value8);
        return map;
    }

    public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5, K key6, V value6, K key7, V value7, K key8, V value8, K key9, V value9) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        map.put(key6, value6);
        map.put(key7, value7);
        map.put(key8, value8);
        map.put(key9, value9);
        return map;
    }

    public static <K, V> Map<K, V> of(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5, K key6, V value6, K key7, V value7, K key8, V value8, K key9, V value9, K key10, V value10) {
        Map<K, V> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        map.put(key6, value6);
        map.put(key7, value7);
        map.put(key8, value8);
        map.put(key9, value9);
        map.put(key10, value10);
        return map;
    }

    public static class SortedMaps {

        public static SortedMap<String, String> of(String key1, String value1) {
            SortedMap<String, String> map = new TreeMap<>();
            map.put(key1, value1);
            return map;
        }

        public static SortedMap<String, String> of(String key1, String value1, String key2, String value2) {
            SortedMap<String, String> map = new TreeMap<>();
            map.put(key1, value1);
            map.put(key2, value2);
            return map;
        }

        public static SortedMap<String, String> of(String key1, String value1, String key2, String value2, String key3, String value3) {
            SortedMap<String, String> map = new TreeMap<>();
            map.put(key1, value1);
            map.put(key2, value2);
            map.put(key3, value3);
            return map;
        }

        public static SortedMap<String, String> of(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4) {
            SortedMap<String, String> map = new TreeMap<>();
            map.put(key1, value1);
            map.put(key2, value2);
            map.put(key3, value3);
            map.put(key4, value4);
            return map;
        }

        public static SortedMap<String, String> of(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5) {
            SortedMap<String, String> map = new TreeMap<>();
            map.put(key1, value1);
            map.put(key2, value2);
            map.put(key3, value3);
            map.put(key4, value4);
            map.put(key5, value5);
            return map;
        }

        public static SortedMap<String, String> of(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5, String key6, String value6) {
            SortedMap<String, String> map = new TreeMap<>();
            map.put(key1, value1);
            map.put(key2, value2);
            map.put(key3, value3);
            map.put(key4, value4);
            map.put(key5, value5);
            map.put(key6, value6);
            return map;
        }

        public static SortedMap<String, String> of(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5, String key6, String value6, String key7, String value7) {
            SortedMap<String, String> map = new TreeMap<>();
            map.put(key1, value1);
            map.put(key2, value2);
            map.put(key3, value3);
            map.put(key4, value4);
            map.put(key5, value5);
            map.put(key6, value6);
            map.put(key7, value7);
            return map;
        }

        public static SortedMap<String, String> of(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5, String key6, String value6, String key7, String value7, String key8, String value8) {
            SortedMap<String, String> map = new TreeMap<>();
            map.put(key1, value1);
            map.put(key2, value2);
            map.put(key3, value3);
            map.put(key4, value4);
            map.put(key5, value5);
            map.put(key6, value6);
            map.put(key7, value7);
            map.put(key8, value8);
            return map;
        }

        public static SortedMap<String, String> of(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5, String key6, String value6, String key7, String value7, String key8, String value8, String key9, String value9) {
            SortedMap<String, String> map = new TreeMap<>();
            map.put(key1, value1);
            map.put(key2, value2);
            map.put(key3, value3);
            map.put(key4, value4);
            map.put(key5, value5);
            map.put(key6, value6);
            map.put(key7, value7);
            map.put(key8, value8);
            map.put(key9, value9);
            return map;
        }

        public static SortedMap<String, String> of(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5, String key6, String value6, String key7, String value7, String key8, String value8, String key9, String value9, String key10, String value10) {
            SortedMap<String, String> map = new TreeMap<>();
            map.put(key1, value1);
            map.put(key2, value2);
            map.put(key3, value3);
            map.put(key4, value4);
            map.put(key5, value5);
            map.put(key6, value6);
            map.put(key7, value7);
            map.put(key8, value8);
            map.put(key9, value9);
            map.put(key10, value10);
            return map;
        }
    }
}
