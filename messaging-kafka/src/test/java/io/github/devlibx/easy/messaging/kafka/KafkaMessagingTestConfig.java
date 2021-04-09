package io.github.devlibx.easy.messaging.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.devlibx.easy.messaging.config.MessagingConfigs;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaMessagingTestConfig {
    private MessagingConfigs messaging;
}
