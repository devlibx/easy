package io.github.devlibx.easy.messaging.kafka;


import ch.qos.logback.classic.Level;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class kafkaResilientExample {

    // Just a wrapper class to read YAML config for kafka
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KafkaMessagingTestConfig {
        private MessagingConfigs messaging;
    }

    public static void main(String[] args) throws Exception {
        // Disable logs for testing
        try {
            LoggerContext loggerFactory = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerFactory.getLogger("ROOT").setLevel(Level.ERROR);
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
        int messageCount = 10;
        CountDownLatch latch = new CountDownLatch(messageCount);
        messagingFactory.getConsumer("customer").ifPresent(consumer -> {
            consumer.start((message, metadata) -> {
                System.out.println(message);
                latch.countDown();
            });
        });

        // Close producer after 2 sec
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            messagingFactory.getProducer("customerNoErrorIfMessageSendFail").ifPresent(producer -> {
                producer.shutdown();
            });
        }).start();

        // Produce sample data
        messagingFactory.getProducer("customerNoErrorIfMessageSendFail").ifPresent(producer -> {
            for (int i = 0; i < 1000000; i++) {
                Object toSend = StringObjectMap.of("input", UUID.randomUUID().toString());
                producer.send(UUID.randomUUID().toString(), JsonUtils.asJson(toSend).getBytes());
            }
        });

        latch.await(5, TimeUnit.SECONDS);
        System.exit(0);
    }
}
