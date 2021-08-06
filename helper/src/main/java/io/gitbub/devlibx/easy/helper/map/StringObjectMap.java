package io.gitbub.devlibx.easy.helper.map;

import io.gitbub.devlibx.easy.helper.json.JsonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class StringObjectMap extends HashMap<String, Object> {

    /**
     * Convenience method to get StringObjectMap from normal map
     */
    public static StringObjectMap from(Map<String, Object> map) {
        StringObjectMap extendedMap = new StringObjectMap();
        if (map != null) {
            extendedMap.putAll(map);
        }
        return extendedMap;
    }

    /**
     * Get value as given class
     */
    public <T> T get(String key, Class<T> cls) {
        return (T) get(key);
    }

    /**
     * @return get value as int - it will be type casted if required
     */
    public Integer getInt(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new RuntimeException("Value with key " + key + " is not a integer");
    }

    /**
     * @return get value as int - it will be type casted if required
     */
    public Integer getInt(String key, int defaultValue) {
        Integer value = getInt(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * @return get value as long - it will be type casted if required
     */
    public Long getLong(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        } else if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        throw new RuntimeException("Value with key " + key + " is not a long");
    }

    /**
     * @return get value as float - it will be type casted if required
     */
    public Float getFloat(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        } else if (value instanceof Float) {
            return (Float) value;
        } else if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof String) {
            return Float.parseFloat((String) value);
        }
        throw new RuntimeException("Value with key " + key + " is not a float");
    }

    /**
     * @return get value as double - it will be type casted if required
     */
    public Double getDouble(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        } else if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        throw new RuntimeException("Value with key " + key + " is not a double");
    }

    /**
     * @return get value as boolean - it will be type casted if required
     */
    public Boolean getBoolean(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        throw new RuntimeException("Value with key " + key + " is not a boolean");
    }

    /**
     * @return get value as boolean - it will be type casted if required
     */
    public Boolean getBoolean(String key, Boolean defaultValue) {
        Boolean value = getBoolean(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * @return get value as string - it will be type casted if required
     */
    public String getString(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else {
            return value.toString();
        }
    }

    /**
     * @return get value as string - it will be type casted if required
     */
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * @return get value as uuid - it will be type casted if required
     */
    public UUID getUUID(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        } else if (value instanceof UUID) {
            return (UUID) value;
        } else if (value instanceof String) {
            return UUID.fromString((String) value);
        }
        throw new RuntimeException("Value with key " + key + " is not a uuid");
    }

    /**
     * @return get value as list if type T
     */
    public <T> List<T> getList(String key, Class<T> cls) {
        Object value = get(key);
        if (value == null) {
            return null;
        } else if (value instanceof List) {
            return (List<T>) value;
        } else if (value instanceof String) {
            return JsonUtils.readList((String) value, cls);
        }
        throw new RuntimeException("Value with key " + key + " is not a uuid");
    }


    /**
     * Convenience method to add more than one key value pair
     */
    public void put(String key, Object value, String key1, Object value1) {
        put(key, value);
        put(key1, value1);
    }

    /**
     * Convenience method to add more than one key value pair
     */
    public void put(String key, Object value, String key1, Object value1, String key2, Object value2) {
        put(key, value);
        put(key1, value1);
        put(key2, value2);
    }

    /**
     * Convenience method to add more than one key value pair
     */
    public void put(String key, Object value, String key1, Object value1, String key2, Object value2,
                    String key3, Object value3) {
        put(key, value);
        put(key1, value1);
        put(key2, value2);
        put(key3, value3);
    }

    /**
     * Convenience method to add more than one key value pair
     */
    public void put(String key, Object value, String key1, Object value1, String key2, Object value2,
                    String key3, Object value3, String key4, Object value4) {
        put(key, value);
        put(key1, value1);
        put(key2, value2);
        put(key3, value3);
        put(key3, value4);
    }

    /**
     * Convenience method to get values from keys (recursively)
     */
    public <T> T get(String key1, String key2, Class<T> cls) {
        StringObjectMap subMap = getStringObjectMap(key1);
        if (subMap == null) return null;
        return subMap.get(key2, cls);
    }

    /**
     * Convenience method to get values from keys (recursively)
     */
    public <T> T get(String key1, String key2, String key3, Class<T> cls) {
        StringObjectMap subMap = getStringObjectMap(key1);
        if (subMap == null) return null;
        StringObjectMap subMap1 = subMap.getStringObjectMap(key2);
        return subMap1 == null ? null : subMap1.get(key3, cls);
    }

    /**
     * Convenience method to get values from keys (recursively)
     */
    public <T> T get(String key1, String key2, String key3, String key4, Class<T> cls) {
        StringObjectMap subMap = getStringObjectMap(key1);
        if (subMap == null) return null;
        StringObjectMap subMap1 = subMap.getStringObjectMap(key2);
        if (subMap1 == null) return null;
        StringObjectMap subMap2 = subMap1.getStringObjectMap(key3);
        return subMap2 == null ? null : subMap2.get(key4, cls);
    }

    /**
     * Convenience method to get get value as StringObjectMap
     */
    public StringObjectMap getStringObjectMap(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        } else if (value instanceof StringObjectMap) {
            return (StringObjectMap) value;
        } else if (value instanceof Map) {
            StringObjectMap map = new StringObjectMap();
            map.putAll((Map<? extends String, ?>) value);
            return map;
        } else {
            return null;
        }
    }


    public static StringObjectMap of(String key, Object value) {
        StringObjectMap map = new StringObjectMap();
        map.put(key, value);
        return map;
    }

    public static StringObjectMap of(String key1, Object value1, String key2, Object value2) {
        StringObjectMap map = new StringObjectMap();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }

    public static StringObjectMap of(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        StringObjectMap map = new StringObjectMap();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        return map;
    }

    public static StringObjectMap of(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4) {
        StringObjectMap map = new StringObjectMap();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        return map;
    }


    public static StringObjectMap of(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5) {
        StringObjectMap map = new StringObjectMap();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        return map;
    }

    public static StringObjectMap of(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5,
                                     String key6, Object value6) {
        StringObjectMap map = new StringObjectMap();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        map.put(key6, value6);
        return map;
    }

    public static StringObjectMap of(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5,
                                     String key6, Object value6, String key7, Object value7) {
        StringObjectMap map = new StringObjectMap();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        map.put(key6, value6);
        map.put(key7, value7);
        return map;
    }


    public static StringObjectMap of(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5,
                                     String key6, Object value6, String key7, Object value7, String key8, Object value8) {
        StringObjectMap map = new StringObjectMap();
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

    public static StringObjectMap of(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5,
                                     String key6, Object value6, String key7, Object value7, String key8, Object value8, String key9, Object value9) {
        StringObjectMap map = new StringObjectMap();
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

    public static StringObjectMap of(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5,
                                     String key6, Object value6, String key7, Object value7, String key8, Object value8, String key9, Object value9, String key10, Object value10) {
        StringObjectMap map = new StringObjectMap();
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

    /**
     * @param separator what is path separator e.g. "."
     * @param key       path separated key "data.entity.id
     * @return result or null if not found
     */
    public <T> T path(String separator, String key, Class<T> cls) {
        if (separator.equals(".")) {
            separator = "\\.";
        }
        StringObjectMap map = this;
        String[] tokens = key.split(separator);
        int len = tokens.length;
        for (int i = 0; i < len - 1; i++) {
            map = map.getStringObjectMap(tokens[i]);
            if (map == null) {
                return null;
            }
        }
        return map.get(tokens[len - 1], cls);
    }

    /**
     * @param key "." separated key "data.entity.id
     * @return result or null if not found
     */
    public <T> T path(String key, Class<T> cls) {
        return path(".", key, cls);
    }

    public boolean isPathValueEqual(String key, Object value) {
        return Objects.equals(path(".", key, Object.class), value);
    }

    public boolean isPathValueTrue(String key) {
        return Objects.equals(path(".", key, Boolean.class), true);
    }

    public boolean isPathValueFalse(String key) {
        return Objects.equals(path(".", key, Boolean.class), false);
    }
}

