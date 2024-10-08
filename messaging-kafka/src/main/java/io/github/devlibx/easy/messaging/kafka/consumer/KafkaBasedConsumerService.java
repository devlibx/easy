package io.github.devlibx.easy.messaging.kafka.consumer;

import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.Safe;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.github.devlibx.easy.messaging.consumer.IConsumer;
import io.github.devlibx.easy.messaging.consumer.IConsumerService;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class KafkaBasedConsumerService implements IConsumerService {
    private final Map<String, KafkaBasedConsumer> kafkaBasedConsumerMap;
    private final IMetrics metrics;

    @Inject
    public KafkaBasedConsumerService(IMetrics metrics) {
        this.kafkaBasedConsumerMap = new ConcurrentHashMap<>();
        this.metrics = metrics;
    }

    @Override
    public void initialize(StringObjectMap config) {
        // Make sure name is provided
        String name = config.getString("name");
        if (Strings.isNullOrEmpty(name)) {
            throw new RuntimeException("name is not provided in kafka consumer config");
        }

        // Create a producer
        if (!kafkaBasedConsumerMap.containsKey(name)) {
            KafkaBasedConsumer consumer;
            if (config.containsKey("parallelThreadCount") && config.getInt("parallelThreadCount", 0) > 0) {
                consumer = new KafkaBasedParallelConsumer(config, metrics);
            } else {
                consumer = new KafkaBasedConsumer(config, metrics);
            }
            kafkaBasedConsumerMap.put(name, consumer);
        }
    }

    @Override
    public Optional<IConsumer> getConsumer(String name) {
        IConsumer consumer = kafkaBasedConsumerMap.get(name);
        return Optional.ofNullable(consumer);
    }

    @Override
    public void shutdown() {
        kafkaBasedConsumerMap.forEach((name, kafkaBasedConsumer) -> {
            Safe.safe(kafkaBasedConsumer::shutdown);
        });
    }
}
