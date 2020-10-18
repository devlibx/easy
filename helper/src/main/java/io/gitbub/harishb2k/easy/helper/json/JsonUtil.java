package io.gitbub.harishb2k.easy.helper.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.Getter;

import java.io.IOException;

public class JsonUtil {
    @Getter
    private final ObjectMapper objectMapper;

    public JsonUtil() {
        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    /**
     * Make custom JsonUtil with provided ObjectMapper
     */
    public JsonUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Read a object from string
     */
    public <T> T readObject(String str, Class<T> cls) {
        try {
            return objectMapper.readValue(str, cls);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read a object from string
     */
    public <T> T readObject(String str, JavaType cls) {
        try {
            return objectMapper.readValue(str, cls);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}