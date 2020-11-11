package io.github.harishb2k.easy.testing.kafka;

import lombok.Data;

@Data
public class KafkaConfig {
    private String brokers;
    private boolean running;
}
