package io.gitbub.harishb2k.easy.helper;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.io.IOException;

public class JsonUtil {
    private final ObjectMapper objectMapper;

    public JsonUtil() {
        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public JsonUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Method to convert object to JSON string.
     *
     * @param value - value to convert
     * @return JSON string for the object
     * @throws RuntimeException error on some issue
     */
    public String writeString(Object value) throws RuntimeException {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
