package io.github.devlibx.easy.testing.kafka;

import lombok.Data;

@Data
public class TestingKafkaConfig {
    private String brokers;
    private boolean running;
}
