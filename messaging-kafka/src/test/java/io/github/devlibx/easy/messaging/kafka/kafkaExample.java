package io.github.devlibx.easy.messaging.kafka;


import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.gitbub.devlibx.easy.helper.yaml.YamlUtils;
import io.github.devlibx.easy.messaging.config.MessagingConfigs;
import io.github.devlibx.easy.messaging.kafka.module.MessagingKafkaModule;
import io.github.devlibx.easy.messaging.module.MessagingModule;
import io.github.devlibx.easy.messaging.service.IMessagingFactory;
import lombok.Data;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class kafkaExample {

    // Just a wrapper class to read YAML config for kafka
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KafkaMessagingTestConfig {
        private MessagingConfigs messaging;
    }

    public static void main(String[] args) throws Exception {
        // Disable logs for testing - You can ignore it
        try {
            LoggerContext loggerFactory = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerFactory.getLogger("ROOT").setLevel(ch.qos.logback.classic.Level.OFF);
        } catch (Exception ignored) {
        }

        // Setup messaging factory - recommended using this code, as it will setup defaults.
        // You can create objects by yourself if you want
        KafkaMessagingTestConfig kafkaConfig = YamlUtils.readYaml("kafka_test_config.yml", KafkaMessagingTestConfig.class);
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(MessagingConfigs.class).toInstance(kafkaConfig.messaging);
            }
        }, new MessagingKafkaModule(), new MessagingModule());
        IMessagingFactory messagingFactory = injector.getInstance(IMessagingFactory.class);
        messagingFactory.initialize();

        // Get and start consuming data
        messagingFactory.getConsumer("customer").ifPresent(consumer -> {
            consumer.start((message, metadata) -> {
                // Consumer event
                System.out.println(message);
            });
        });

        // Produce sample data
        messagingFactory.getProducer("customer").ifPresent(producer -> {
            for (int i = 0; i < 100; i++) {

                // Produce event to Kafka
                Object toSend = StringObjectMap.of("input", UUID.randomUUID().toString());
                producer.send(UUID.randomUUID().toString(), JsonUtils.asJson(toSend).getBytes());

                // Sleep for Demo
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            }

        });

        messagingFactory.shutdown();
        Thread.sleep(1000);
        System.exit(0);
    }
}
