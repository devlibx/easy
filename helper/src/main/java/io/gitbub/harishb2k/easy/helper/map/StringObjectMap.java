package io.gitbub.harishb2k.easy.helper.map;

import java.util.HashMap;
import java.util.Map;
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

    public void put(String key, Object value, String key1, Object value1) {
        put(key, value);
        put(key1, value1);
    }

    public void put(String key, Object value, String key1, Object value1, String key2, Object value2) {
        put(key, value);
        put(key1, value1);
        put(key2, value2);
    }

    public void put(String key, Object value, String key1, Object value1, String key2, Object value2,
                    String key3, Object value3) {
        put(key, value);
        put(key1, value1);
        put(key2, value2);
        put(key3, value3);
    }

    public void put(String key, Object value, String key1, Object value1, String key2, Object value2,
                    String key3, Object value3, String key4, Object value4) {
        put(key, value);
        put(key1, value1);
        put(key2, value2);
        put(key3, value3);
        put(key3, value4);
    }

    public <T> T get(String key1, String key2, String key3, Class<T> cls) {
        StringObjectMap subMap = getStringObjectMap(key1);
        if (subMap == null) return null;
        StringObjectMap subMap1 = subMap.getStringObjectMap(key2);
        return subMap1 == null ? null : subMap1.get(key3, cls);
    }

    public <T> T get(String key1, String key2, String key3, String key4, Class<T> cls) {
        StringObjectMap subMap = getStringObjectMap(key1);
        if (subMap == null) return null;
        StringObjectMap subMap1 = subMap.getStringObjectMap(key2);
        if (subMap1 == null) return null;
        StringObjectMap subMap2 = subMap1.getStringObjectMap(key3);
        return subMap2 == null ? null : subMap2.get(key4, cls);
    }

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

}
