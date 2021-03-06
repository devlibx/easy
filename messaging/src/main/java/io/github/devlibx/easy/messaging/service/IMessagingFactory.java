package io.github.devlibx.easy.messaging.service;

import com.google.inject.ImplementedBy;
import io.github.devlibx.easy.messaging.consumer.IConsumer;
import io.github.devlibx.easy.messaging.producer.IProducer;

import java.util.Optional;

@ImplementedBy(MessageFactory.class)
public interface IMessagingFactory {

    /**
     * Initialized messaging
     */
    void initialize();

    /**
     * Get a producer
     */
    Optional<IProducer> getProducer(String name);

    /**
     * Get a consumer
     */
    Optional<IConsumer> getConsumer(String name);

    /**
     * Shutdown
     */
    void shutdown();
}
