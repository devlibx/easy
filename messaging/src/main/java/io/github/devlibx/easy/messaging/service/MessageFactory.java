package io.github.devlibx.easy.messaging.service;

import io.gitbub.devlibx.easy.helper.Safe;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.github.devlibx.easy.messaging.config.MessagingConfigs;
import io.github.devlibx.easy.messaging.consumer.IConsumer;
import io.github.devlibx.easy.messaging.consumer.IConsumerService;
import io.github.devlibx.easy.messaging.producer.IProducer;
import io.github.devlibx.easy.messaging.producer.IProducerService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class MessageFactory implements IMessagingFactory {
    private final Map<String, IProducerService> producerServiceMap;
    private final Map<String, IConsumerService> consumerServiceMap;
    private final MessagingConfigs messagingConfigs;
    private boolean initialized = false;

    @Inject
    public MessageFactory(Map<String, IProducerService> producerServiceMap, Map<String, IConsumerService> consumerServiceMap, MessagingConfigs messagingConfigs) {
        this.producerServiceMap = producerServiceMap;
        this.consumerServiceMap = consumerServiceMap;
        this.messagingConfigs = messagingConfigs;

        // Make sure we have valid config
        if (messagingConfigs == null || (messagingConfigs.getConsumers().isEmpty() && messagingConfigs.getProducers().isEmpty())) {
            log.error("You must bind MessagingConfigs in injector using [ bind(MessagingConfigs.class).toInstance(<config object>) ]");
            throw new RuntimeException("some misconfiguration found in messaging - it seems that MessagingConfigs is not proper. " +
                    "It has both producers and consumers missing. May be you did not bind MessagingConfigs in injector");
        }
    }


    @Override
    public synchronized void initialize() {
        // No-OP if already initialized
        if (initialized) return;

        messagingConfigs.getProducers().forEach((name, config) -> {

            // Do not setup if it is not enabled
            if (!config.getBoolean("enabled", Boolean.TRUE)) {
                log.info("messaging producer [{}] is not enabled", name);
                return;
            }

            // Get the type and produce service which will support his type
            String type = config.getString("type", "KAFKA");
            IProducerService producerService = producerServiceMap.get(type);
            if (producerService == null) {
                throw new RuntimeException("producer type " + type + " is not supported");
            }

            log.info("initialize producer [{}]", name);
            config.put("name", name);
            producerService.initialize(config);

        });

        messagingConfigs.getConsumers().forEach((name, config) -> {

            // Do not setup if it is not enabled
            if (!config.getBoolean("enabled", Boolean.TRUE)) {
                log.info("messaging consumer [{}] is not enabled", name);
                return;
            }

            // Get the type and consumer service which will support his type
            String type = config.getString("type", "KAFKA");
            IConsumerService consumerService = consumerServiceMap.get(type);
            if (consumerService == null) {
                throw new RuntimeException("consumer type " + type + " is not supported");
            }

            log.info("initialize consumer [{}]", name);
            config.put("name", name);
            consumerService.initialize(config);
        });

        // Ok we are initialized
        initialized = true;
    }

    @Override
    public Optional<IProducer> getProducer(String name) {

        // Initialized if not done already
        if (!initialized) {
            initialize();
        }

        StringObjectMap config = messagingConfigs.getProducers().get(name);
        if (config == null) return Optional.empty();

        IProducerService producerService = producerServiceMap.get(config.getString("type", "KAFKA"));
        return producerService.getProducer(name);
    }

    @Override
    public Optional<IConsumer> getConsumer(String name) {

        // Initialized if not done already
        if (!initialized) {
            initialize();
        }

        StringObjectMap config = messagingConfigs.getConsumers().get(name);
        if (config == null) return Optional.empty();

        IConsumerService consumerService = consumerServiceMap.get(config.getString("type", "KAFKA"));
        return consumerService.getConsumer(name);
    }

    @Override
    public void shutdown() {
        producerServiceMap.forEach((type, producerService) -> {
            Safe.safe(producerService::shutdown);
        });
        consumerServiceMap.forEach((type, consumerService) -> {
            Safe.safe(consumerService::shutdown);
        });
    }
}
