package io.github.harishb2k.easy.messaging.kafka;

import ch.qos.logback.classic.Level;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.yaml.YamlUtils;
import io.github.harishb2k.easy.messaging.config.MessagingConfigs;
import io.github.harishb2k.easy.messaging.kafka.module.MessagingKafkaModule;
import io.github.harishb2k.easy.messaging.kafka.producer.KafkaBasedProducer;
import io.github.harishb2k.easy.messaging.module.MessagingModule;
import io.github.harishb2k.easy.messaging.producer.IProducer;
import io.github.harishb2k.easy.messaging.service.IMessagingFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class KafkaDemoApp {

    public static void main(String[] args) {
        KafkaDemoApp kafkaDemoApp = new KafkaDemoApp();
        kafkaDemoApp.runKafka();
    }

    public void runKafka() {
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(KafkaBasedProducer.class).setLevel(Level.DEBUG);

        KafkaMessagingTestConfig config = YamlUtils.readYaml("kafka_test_config.yml", KafkaMessagingTestConfig.class);
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(MessagingConfigs.class).toInstance(config.getMessaging());
            }
        }, new MessagingKafkaModule(), new MessagingModule());

        IMessagingFactory messagingFactory = injector.getInstance(IMessagingFactory.class);
        IProducer producer = messagingFactory.getProducer("sampleProducer").orElseThrow(() -> new RuntimeException("producer not found"));
        for (int i = 0; i < 1000; i++) {
            boolean result = producer.send(UUID.randomUUID().toString(), "Some sample text");
            log.info("Message sent = {}", result);
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
