package io.github.harishb2k.easy.messaging.kafka.consumer;

import io.github.harishb2k.easy.messaging.consumer.IConsumer;
import io.github.harishb2k.easy.messaging.consumer.IConsumerService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class KafkaBasedConsumerService implements IConsumerService {

    @Override
    public void initialize(Map<String, Object> config) {
    }

    @Override
    public Optional<IConsumer> getConsumer(String name) {
        return Optional.empty();
    }

    @Override
    public void shutdown() {
    }
}
