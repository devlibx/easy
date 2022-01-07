package io.gitbub.devlibx.easy.helper.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * <pre>
 * {
 *    "__service__" : "testing",
 *    "__timestamp__" : 1641583496507,
 *    "__unique_id__" : "d45bee09-b8a6-4188-ad16-a1cab3496c42",
 *    "data" : {
 *       "key" : "value"
 *    },
 *    "entity" : {
 *       "id" : "user_1",
 *       "type" : "user"
 *    },
 *    "event_name" : "test"
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class LogEvent {
    private static String SERVICE_NAME_FOR_APPLICATION;

    @JsonProperty("__service__")
    private String service;

    @JsonProperty("__timestamp__")
    private long timestamp;

    @JsonProperty("__unique_id__")
    private String uniqueId;

    @JsonProperty("event_name")
    private String eventName;

    @JsonProperty("entity")
    private Entity entity;

    @JsonProperty("data")
    private StringObjectMap data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Entity {
        @JsonProperty("type")
        private String type;

        @JsonProperty("id")
        private String id;
    }

    public static void setGlobalServiceName(String serviceName) {
        SERVICE_NAME_FOR_APPLICATION = serviceName;
    }

    public static class Builder {
        private final LogEvent logEvent = new LogEvent();

        public static Builder withServiceAndEventName(String service, String event) {
            Builder builder = new Builder();
            builder.logEvent.service = service;
            builder.logEvent.eventName = event;
            return builder;
        }

        public static Builder withEventName(String event) {
            if (Strings.isNullOrEmpty(SERVICE_NAME_FOR_APPLICATION)) {
                throw new RuntimeException("Please set global application name using LogEvent.setGlobalServiceName(XYZ) at boot-up");
            }

            Builder builder = new Builder();
            builder.logEvent.service = SERVICE_NAME_FOR_APPLICATION;
            builder.logEvent.eventName = event;
            return builder;
        }

        public static Builder withEventNameAndEntity(String event, String entityType, String entityId) {
            if (Strings.isNullOrEmpty(SERVICE_NAME_FOR_APPLICATION)) {
                throw new RuntimeException("Please set global application name using LogEvent.setGlobalServiceName(XYZ) at boot-up");
            }

            Builder builder = new Builder();
            builder.logEvent.service = SERVICE_NAME_FOR_APPLICATION;
            builder.logEvent.eventName = event;
            builder.logEvent.entity = new Entity();
            builder.logEvent.entity.type = entityType;
            builder.logEvent.entity.id = entityId;
            return builder;
        }

        public Builder data(StringObjectMap data) {
            logEvent.data = data;
            return this;
        }

        public Builder data(String key, Object value) {
            logEvent.data = StringObjectMap.of(key, value);
            return this;
        }

        public Builder data(String key1, Object value1, String key2, Object value2) {
            logEvent.data = StringObjectMap.of(key1, value1, key2, value2);
            return this;
        }

        public Builder data(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
            logEvent.data = StringObjectMap.of(key1, value1, key2, value2, key3, value3);
            return this;
        }


        public Builder data(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4) {
            logEvent.data = StringObjectMap.of(key1, value1, key2, value2, key3, value3, key4, value4);
            return this;
        }


        public Builder data(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5) {
            logEvent.data = StringObjectMap.of(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5);
            return this;
        }


        public Builder data(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5, String key6, Object value6) {
            logEvent.data = StringObjectMap.of(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5, key6, value6);
            return this;
        }

        public LogEvent build() {
            logEvent.uniqueId = UUID.randomUUID().toString();
            logEvent.timestamp = System.currentTimeMillis();
            return logEvent;
        }
    }
}
