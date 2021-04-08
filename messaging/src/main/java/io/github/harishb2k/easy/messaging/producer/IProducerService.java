package io.github.devlibx.easy.messaging.producer;

import io.gitbub.devlibx.easy.helper.map.StringObjectMap;

import java.util.Optional;

public interface IProducerService {

    /**
     * Shutdown
     */
    void initialize(StringObjectMap config);

    /**
     * Get a producer with given name
     */
    Optional<IProducer> getProducer(String name);

    /**
     * Shutdown
     */
    void shutdown();
}
