package io.gitbub.harishb2k.easy.helper.json;

import com.fasterxml.jackson.databind.JavaType;

import java.util.Map;

public class JsonUtils {
    private static final JsonUtil jsonUtil = new JsonUtil();

    /**
     * Read a object from string
     */
    public static <T> T readObject(String str, Class<T> cls) {
        return jsonUtil.readObject(str, cls);
    }

    /**
     * Read a object from string
     */
    public static <T> T readObject(String str, JavaType cls) {
        return jsonUtil.readObject(str, cls);
    }

    /**
     * Convert string to Map
     */
    public static Map<String, Object> convertAsMap(String str) {
        return jsonUtil.convertAsMap(str);
    }
}
