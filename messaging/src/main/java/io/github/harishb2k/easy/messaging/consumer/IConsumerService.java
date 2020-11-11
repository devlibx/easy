package io.github.harishb2k.easy.messaging.consumer;

import io.gitbub.harishb2k.easy.helper.map.StringObjectMap;

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
