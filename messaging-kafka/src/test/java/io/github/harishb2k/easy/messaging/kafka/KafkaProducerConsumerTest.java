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
import io.github.harishb2k.easy.testing.kafka.TestingKafkaConfig;
import io.github.harishb2k.easy.testing.kafka.KafkaExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.harishb2k.easy.testing.kafka.KafkaExtension.DISABLE_IF_KAFKA_NOT_RUNNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ExtendWith(KafkaExtension.class)
public class KafkaProducerConsumerTest {

    @BeforeClass
    public static void setup() {
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(KafkaBasedProducer.class).setLevel(Level.DEBUG);
        LoggingHelper.getLogger(org.apache.kafka.clients.NetworkClient.class).setLevel(Level.OFF);
    }

    @Test
    @DisplayName("Run kafka and verify that messages are being sent")
    @Tag(DISABLE_IF_KAFKA_NOT_RUNNING)
    public void verifyMessagesPostedToKafka(TestingKafkaConfig kafkaConfig) {

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

    @RepeatedTest(3)
    @DisplayName("Run kafka and verify that messages are being sent and consumer is able to get it")
    @Tag(DISABLE_IF_KAFKA_NOT_RUNNING)
    public void verifyMessagesPostedToKafkaAreConsumed(TestingKafkaConfig kafkaConfig) throws Exception {
        final String topic = "some_topic_" + UUID.randomUUID().toString();
        // final String topic = "some_topic_8";
        KafkaMessagingTestConfig config = YamlUtils.readYaml("kafka_test_config.yml", KafkaMessagingTestConfig.class);
        if (kafkaConfig.isRunning()) {
            config.getMessaging().getProducers().forEach((s, stringObjectMap) -> {
                stringObjectMap.put("brokers", kafkaConfig.getBrokers());
                stringObjectMap.put("topic", topic);
            });
            config.getMessaging().getConsumers().forEach((s, stringObjectMap) -> {
                stringObjectMap.put("brokers", kafkaConfig.getBrokers());
                stringObjectMap.put("group.id", UUID.randomUUID().toString());
                stringObjectMap.put("topic", topic);
            });
        }

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(MessagingConfigs.class).toInstance(config.getMessaging());
            }
        }, new MessagingKafkaModule(), new MessagingModule());

        final int messageCount = 50;
        final CountDownLatch messagesReceived = new CountDownLatch(messageCount);
        final AtomicInteger messagesReceivedErrorCount = new AtomicInteger();
        final AtomicInteger messagesReceivedCount = new AtomicInteger();
        IMessagingFactory messagingFactory = injector.getInstance(IMessagingFactory.class);

        {
            IProducer producer = messagingFactory.getProducer("sampleProducer").orElseThrow(() -> new RuntimeException("producer not found"));
            producer.send("dummy", "Some sample text");
        }

        // Consume messages
        messagingFactory.getConsumer("sampleConsumer").ifPresent(consumer -> {
            consumer.start((message, metadata) -> {
                try {
                    assertEquals("Some sample text", message);
                    messagesReceivedCount.incrementAndGet();
                } catch (Exception e) {
                    messagesReceivedErrorCount.incrementAndGet();
                } finally {
                    messagesReceived.countDown();
                }
            });
        });
        Thread.sleep(500);

        new Thread(() -> {
            IProducer producer = messagingFactory.getProducer("sampleProducer").orElseThrow(() -> new RuntimeException("producer not found"));
            for (int i = 0; i < messageCount; i++) {
                boolean result = producer.send(UUID.randomUUID().toString(), "Some sample text");
                assertTrue(result);
            }
        }).start();

        messagesReceived.await(10, TimeUnit.SECONDS);
        assertEquals(0, messagesReceivedErrorCount.get(), "Expected 0 errors in consumers message receive");
        assertTrue(messagesReceivedCount.get() >= messageCount, "Expected all messages to reach consumer");
    }
}
