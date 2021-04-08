package io.github.devlibx.easy.http.helper;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public class CommonHttpHelper {

    public static MultivaluedMap<String, Object> multivaluedMap(
            String key, Object value
    ) {
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(key, value);
        return map;
    }

    public static MultivaluedMap<String, Object> multivaluedMap(
            String key, Object value,
            String key1, Object value1
    ) {
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(key, value);
        map.add(key1, value1);
        return map;
    }

    public static MultivaluedMap<String, Object> multivaluedMap(
            String key, Object value,
            String key1, Object value1,
            String key2, Object value2
    ) {
        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(key, value);
        map.add(key1, value1);
        map.add(key2, value2);
        return map;
    }
}
