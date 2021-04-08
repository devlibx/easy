package io.github.devlibx.easy.messaging.consumer;

import io.gitbub.devlibx.easy.helper.map.StringObjectMap;

import java.util.Map;
import java.util.Optional;

public interface IConsumerService {

    /**
     * Initialize consumer service
     */
    void initialize(StringObjectMap config);

    /**
     * Get a consumer with given name
     */
    Optional<IConsumer> getConsumer(String name);

    /**
     * Shutdown
     */
    void shutdown();
}
