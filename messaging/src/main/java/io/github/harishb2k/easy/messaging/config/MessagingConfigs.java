package io.github.devlibx.easy.messaging.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * <pre>
 * A sample configuration:
 * ======================
 * messaging:
 *   producers:
 *     sampleProducer:
 *       topic: some_topic
 *       brokers: localhost:9092
 *       sync: false
 *       retries: 1
 *       acks: 1
 *   consumers:
 *     sampleConsumer:
 *       topic: some_topic
 *       broker: localhost:9092
 *       sync: true
 *
 * </pre>
 * <pre>
 *     Kafka Specific configurations:
 *
 *     # Producer will send a message and return once it is sent. If false, producer will not wait for result.
 *     # However, the actual delivery is based on setting given in following link
 *     # https://docs.confluent.io/current/installation/configuration/producer-configs.html#acks
 *     # Applicable for producer
 *     sync: true
 *
 *     # acks = 0    -> not wait for any acknowledgment from the server at all (message loss may happen)
 *     # acks = 1    -> respond without awaiting full acknowledgement from all kafka servers (message loss may happen)
 *     # acks = all  -> strongest available guarantee
 *     # Applicable for producer
 *     acks: 1
 *
 *     # Retry if failed to send to server
 *     # Full details - https://docs.confluent.io/current/installation/configuration/producer-configs.html#retries
 *     # Applicable for producer
 *     retries: 1
 *
 *     # Topic where message will be posted
 *     topic: some_topic
 *
 *     # Kafka brokers
 *     broker: localhost:9092
 *
 *     # Type of message producer/consumer (default=KAFKA)
 *     type: KAFKA
 *
 * </pre>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessagingConfigs {
    private Map<String, StringObjectMap> producers = new ConcurrentHashMap<>();
    private Map<String, StringObjectMap> consumers = new ConcurrentHashMap<>();
}
