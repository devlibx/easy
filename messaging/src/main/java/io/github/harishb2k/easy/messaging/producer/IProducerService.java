package io.github.harishb2k.easy.messaging.producer;

import io.gitbub.harishb2k.easy.helper.map.StringObjectMap;

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
