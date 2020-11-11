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
import io.github.harishb2k.easy.testing.kafka.KafkaConfig;
import io.github.harishb2k.easy.testing.kafka.KafkaExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static io.github.harishb2k.easy.testing.kafka.KafkaExtension.DISABLE_IF_KAFKA_NOT_RUNNING;

@Slf4j
@ExtendWith(KafkaExtension.class)
public class KafkaProducerTest {

    public static void main(String[] args) {
        KafkaProducerTest kafkaDemoApp = new KafkaProducerTest();
        kafkaDemoApp.verifyMessagesPostedToKafka(null);
    }

    @Test
    @DisplayName("Run kafka and verify that messages are being setn")
    @Tag(DISABLE_IF_KAFKA_NOT_RUNNING)
    public void verifyMessagesPostedToKafka(KafkaConfig kafkaConfig) {
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(KafkaBasedProducer.class).setLevel(Level.DEBUG);
        LoggingHelper.getLogger(org.apache.kafka.clients.NetworkClient.class).setLevel(Level.OFF);

        KafkaMessagingTestConfig config = YamlUtils.readYaml("kafka_test_config.yml", KafkaMessagingTestConfig.class);
        if (kafkaConfig.isRunning()) {
            config.getMessaging().getProducers().forEach((s, stringObjectMap) -> {
                stringObjectMap.put("brokers", kafkaConfig.getBrokers());
            });
        }

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(MessagingConfigs.class).toInstance(config.getMessaging());
            }
        }, new MessagingKafkaModule(), new MessagingModule());

        IMessagingFactory messagingFactory = injector.getInstance(IMessagingFactory.class);
        IProducer producer = messagingFactory.getProducer("sampleProducer").orElseThrow(() -> new RuntimeException("producer not found"));
        for (int i = 0; i < 10; i++) {
            boolean result = producer.send(UUID.randomUUID().toString(), "Some sample text");
            log.info("Message sent = {}", result);
            Assertions.assertTrue(result);
        }
    }
}
