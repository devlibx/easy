package io.github.devlibx.easy.messaging.kafka.module;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import io.github.devlibx.easy.messaging.consumer.IConsumerService;
import io.github.devlibx.easy.messaging.kafka.consumer.KafkaBasedConsumerService;
import io.github.devlibx.easy.messaging.kafka.producer.KafkaBasedProducerService;
import io.github.devlibx.easy.messaging.producer.IProducerService;

public class MessagingKafkaModule extends AbstractModule {
    @Override
    protected void configure() {
        super.configure();

        // Provide kafka service provider
        MapBinder<String, IProducerService> lockBuilderMapBinder = MapBinder.newMapBinder(binder(), String.class, IProducerService.class);
        lockBuilderMapBinder.permitDuplicates();
        lockBuilderMapBinder.addBinding("KAFKA").to(KafkaBasedProducerService.class);

        // Provide kafka service provider
        MapBinder<String, IConsumerService> consumerServiceMapBinder = MapBinder.newMapBinder(binder(), String.class, IConsumerService.class);
        consumerServiceMapBinder.permitDuplicates();
        consumerServiceMapBinder.addBinding("KAFKA").to(KafkaBasedConsumerService.class);
    }
}
