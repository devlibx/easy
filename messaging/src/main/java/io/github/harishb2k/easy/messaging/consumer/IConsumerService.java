package io.github.harishb2k.easy.messaging.consumer;

import java.util.Map;
import java.util.Optional;

public interface IConsumerService {

    /**
     * Initialize consumer service
     */
    void initialize(Map<String, Object> config);

    /**
     * Get a consumer with given name
     */
    Optional<IConsumer> getConsumer(String name);

    /**
     * Shutdown
     */
    void shutdown();
}
