package io.gitbub.harishb2k.easy.helper.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.gitbub.harishb2k.easy.helper.map.StringObjectMap;
import io.gitbub.harishb2k.easy.helper.string.StringHelper;

import java.util.List;
import java.util.Map;

public class JsonUtils {
    private static final JsonUtil jsonUtil = new JsonUtil();

    private static final JsonUtil camelCaseJsonUtil;
    private static final StringHelper stringHelper;

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        camelCaseJsonUtil = new JsonUtil(objectMapper);

        stringHelper = new StringHelper(jsonUtil);
    }

    public static JsonUtil getCamelCase() {
        return camelCaseJsonUtil;
    }

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

    /**
     * Convert string-object to Map
     */
    public static StringObjectMap convertAsStringObjectMap(String str) {
        return jsonUtil.convertAsStringObjectMap(str);
    }

    /**
     * Convert string to Map
     */
    public static StringObjectMap convertAsStringObjectMap(byte[] bytes) {
        return jsonUtil.convertAsStringObjectMap(bytes);
    }

    /**
     * Convert object to json string
     *
     * @param object object to convert
     * @return json string
     */
    public static String asJson(Object object) {
        return stringHelper.stringify(object);
    }

    /**
     * Convert string to List
     */
    public static <T> List<T> readList(String str, Class<T> cls) {
        return jsonUtil.readList(str, cls);
    }
}
