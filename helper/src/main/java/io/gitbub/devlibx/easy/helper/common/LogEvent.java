package io.gitbub.devlibx.easy.helper.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.map.Maps;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Sample event Json
 * <pre>
 * {
 *    "__service__" : "testing",
 *    "__timestamp__" : 1641722003542,
 *    "__unique_id__" : "432f211b-ddad-44f2-bd51-78e1e793b7ad",
 *    "data" : {
 *       "key" : "value"
 *    },
 *    "dimensions" : {
 *       "key1" : "value1",
 *       "key2" : "value2",
 *       "key3" : "value3",
 *       "key4" : "value4"
 *    },
 *    "entity" : {
 *       "id" : "user_1",
 *       "type" : "user"
 *    },
 *    "event_sub_type" : "test_sub_type",
 *    "event_type" : "test"
 * }
 * </pre>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LogEvent {
    private static String SERVICE_NAME_FOR_APPLICATION;

    @JsonProperty("__service__")
    private String service;

    @JsonProperty("__timestamp__")
    private long timestamp;

    @JsonProperty("__unique_id__")
    private String uniqueId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("event_sub_type")
    private String eventSubType;

    @JsonProperty("dimensions")
    private Map<String, String> dimensions;

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

        public static Builder withEventType(String eventType) {
            if (Strings.isNullOrEmpty(SERVICE_NAME_FOR_APPLICATION)) {
                throw new RuntimeException("Please set global application name using LogEvent.setGlobalServiceName(XYZ) at boot-up");
            }

            Builder builder = new Builder();
            builder.logEvent.service = SERVICE_NAME_FOR_APPLICATION;
            builder.logEvent.eventType = eventType;
            return builder;
        }

        public static Builder withEventTypeAndEventSubType(String eventType, String eventSubType) {
            if (Strings.isNullOrEmpty(SERVICE_NAME_FOR_APPLICATION)) {
                throw new RuntimeException("Please set global application name using LogEvent.setGlobalServiceName(XYZ) at boot-up");
            }

            Builder builder = new Builder();
            builder.logEvent.service = SERVICE_NAME_FOR_APPLICATION;
            builder.logEvent.eventType = eventType;
            builder.logEvent.eventSubType = eventSubType;
            return builder;
        }

        public static Builder withEventTypeAndEntity(String eventType, String entityType, String entityId) {
            if (Strings.isNullOrEmpty(SERVICE_NAME_FOR_APPLICATION)) {
                throw new RuntimeException("Please set global application name using LogEvent.setGlobalServiceName(XYZ) at boot-up");
            }

            Builder builder = new Builder();
            builder.logEvent.service = SERVICE_NAME_FOR_APPLICATION;
            builder.logEvent.eventType = eventType;
            builder.logEvent.entity = new Entity();
            builder.logEvent.entity.type = entityType;
            builder.logEvent.entity.id = entityId;
            return builder;
        }

        public static Builder withEventTypeEventSubTypeAndEntity(String eventType, String eventSubType, String entityType, String entityId) {
            if (Strings.isNullOrEmpty(SERVICE_NAME_FOR_APPLICATION)) {
                throw new RuntimeException("Please set global application name using LogEvent.setGlobalServiceName(XYZ) at boot-up");
            }

            Builder builder = new Builder();
            builder.logEvent.service = SERVICE_NAME_FOR_APPLICATION;
            builder.logEvent.eventType = eventType;
            builder.logEvent.eventSubType = eventSubType;
            builder.logEvent.entity = new Entity();
            builder.logEvent.entity.type = entityType;
            builder.logEvent.entity.id = entityId;
            return builder;
        }

        public Builder data(StringObjectMap data) {
            logEvent.data = data;
            return this;
        }

        public Builder dimensions(Map<String, String> dimensions) {
            if (logEvent.dimensions == null) {
                logEvent.dimensions = dimensions;
            } else {
                logEvent.dimensions.putAll(dimensions);
            }
            return this;
        }

        public Builder dimensions(String key, String value) {
            Map<String, String> m = Maps.of(key, value);
            if (logEvent.dimensions == null) {
                logEvent.dimensions = m;
            } else {
                logEvent.dimensions.putAll(m);
            }
            return this;
        }

        public Builder dimensions(String key1, String value1, String key2, String value2) {
            Map<String, String> m = Maps.of(key1, value1, key2, value2);
            if (logEvent.dimensions == null) {
                logEvent.dimensions = m;
            } else {
                logEvent.dimensions.putAll(m);
            }
            return this;
        }

        public Builder dimensions(String key1, String value1, String key2, String value2, String key3, String value3) {
            Map<String, String> m = Maps.of(key1, value1, key2, value2, key3, value3);
            if (logEvent.dimensions == null) {
                logEvent.dimensions = m;
            } else {
                logEvent.dimensions.putAll(m);
            }
            return this;
        }

        public Builder dimensions(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4) {
            Map<String, String> m = Maps.of(key1, value1, key2, value2, key3, value3, key4, value4);
            if (logEvent.dimensions == null) {
                logEvent.dimensions = m;
            } else {
                logEvent.dimensions.putAll(m);
            }
            return this;
        }

        public Builder dimensions(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5) {
            Map<String, String> m = Maps.of(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5);
            if (logEvent.dimensions == null) {
                logEvent.dimensions = m;
            } else {
                logEvent.dimensions.putAll(m);
            }
            return this;
        }

        public Builder dimensions(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4, String key5, String value5, String key6, String value6) {
            Map<String, String> m = Maps.of(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5, key6, value6);
            if (logEvent.dimensions == null) {
                logEvent.dimensions = m;
            } else {
                logEvent.dimensions.putAll(m);
            }
            return this;
        }

        public Builder data(String key, Object value) {
            StringObjectMap m = StringObjectMap.of(key, value);
            if (logEvent.data == null) {
                logEvent.data = m;
            } else {
                logEvent.data.putAll(m);
            }
            return this;
        }

        public Builder data(String key1, Object value1, String key2, Object value2) {
            StringObjectMap m = StringObjectMap.of(key1, value1, key2, value2);
            if (logEvent.data == null) {
                logEvent.data = m;
            } else {
                logEvent.data.putAll(m);
            }
            return this;
        }

        public Builder data(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
            StringObjectMap m = StringObjectMap.of(key1, value1, key2, value2, key3, value3);
            if (logEvent.data == null) {
                logEvent.data = m;
            } else {
                logEvent.data.putAll(m);
            }
            return this;
        }


        public Builder data(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4) {
            StringObjectMap m = StringObjectMap.of(key1, value1, key2, value2, key3, value3, key4, value4);
            if (logEvent.data == null) {
                logEvent.data = m;
            } else {
                logEvent.data.putAll(m);
            }
            return this;
        }


        public Builder data(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5) {
            StringObjectMap m = StringObjectMap.of(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5);
            if (logEvent.data == null) {
                logEvent.data = m;
            } else {
                logEvent.data.putAll(m);
            }
            return this;
        }


        public Builder data(String key1, Object value1, String key2, Object value2, String key3, Object value3, String key4, Object value4, String key5, Object value5, String key6, Object value6) {
            StringObjectMap m = StringObjectMap.of(key1, value1, key2, value2, key3, value3, key4, value4, key5, value5, key6, value6);
            if (logEvent.data == null) {
                logEvent.data = m;
            } else {
                logEvent.data.putAll(m);
            }
            return this;
        }

        public LogEvent build() {
            logEvent.uniqueId = UUID.randomUUID().toString();
            logEvent.timestamp = System.currentTimeMillis();
            return logEvent;
        }
    }
}
