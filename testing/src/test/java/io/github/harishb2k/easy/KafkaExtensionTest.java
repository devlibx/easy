package io.github.harishb2k.easy;

import ch.qos.logback.classic.Level;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.Safe;
import io.github.harishb2k.easy.testing.kafka.IKafkaExtensionControl;
import io.github.harishb2k.easy.testing.kafka.KafkaConfig;
import io.github.harishb2k.easy.testing.kafka.KafkaExtension;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.internals.AbstractCoordinator;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(KafkaExtension.class)
public class KafkaExtensionTest {

    @BeforeClass
    public static void setup() {
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(AbstractCoordinator.class).setLevel(Level.OFF);
        LoggingHelper.getLogger("org.apache.kafka.clients.consumer.internals").setLevel(Level.OFF);
        LoggingHelper.getLogger(org.apache.kafka.clients.NetworkClient.class).setLevel(Level.OFF);
    }

    @Test
    @DisplayName("Test to verify that KafkaExtension is able to start a new Kafka in docker or use existing running kafka")
    public void verifyDockerKafkaStartedAndMessageProduceAndConsumeIsWorking(
            KafkaConfig kafkaConfig,
            Producer<String, String> producer,
            Consumer<String, String> consumer,
            IKafkaExtensionControl kafkaExtensionControl
    ) throws Exception {

        // Run this test only if kafka is running
        Assumptions.assumeTrue(kafkaConfig.isRunning(), "Kafka must be running for this test case to run: kafka status = not running");

        final String topic = "some_topic_" + UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();

        // Listen to message on topic
        consumer.subscribe(Collections.singletonList(topic));

        final int messageCount = 10;
        CountDownLatch consumerStarted = new CountDownLatch(1);
        CountDownLatch gotLatch = new CountDownLatch(messageCount);
        AtomicLong count = new AtomicLong();
        new Thread(() -> {
            try {
                consumerStarted.countDown();
                while (count.get() < messageCount) {
                    ConsumerRecords<String, String> records = consumer.poll(10);
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            count.incrementAndGet();
                            assertEquals(message, record.value());
                        } finally {
                            gotLatch.countDown();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }).start();

        new Thread(() -> {
            try {
                consumerStarted.await(10, TimeUnit.SECONDS);
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            for (int i = 0; i < messageCount; i++) {
                Future<RecordMetadata> ret = producer.send(new ProducerRecord<>(topic, UUID.randomUUID().toString(), message));
                try {
                    ret.get();
                } catch (Exception ignored) {
                }
            }
        }).start();

        gotLatch.await(10, TimeUnit.SECONDS);
        assertEquals(messageCount, count.get());
        Safe.safe(consumer::close);
        Safe.safe(producer::close);

        kafkaExtensionControl.stopIfRunning();
    }

    @Test
    public void verifyNewKafkaIsNotLaunched(
            KafkaConfig kafkaConfig,
            Producer<String, String> producer,
            IKafkaExtensionControl kafkaExtensionControl
    ) throws Exception {

        // Run this test only if kafka is running
        Assumptions.assumeTrue(kafkaConfig.isRunning(), "Kafka must be running for this test case to run: kafka status = not running");

        final String topic = "some_topic_" + UUID.randomUUID().toString();
        final String message = UUID.randomUUID().toString();

        Future<RecordMetadata> ret = producer.send(new ProducerRecord<>(topic, UUID.randomUUID().toString(), message));
        try {
            ret.get();
        } catch (Exception ignored) {
            Assertions.fail("Message sent fail");
        }
    }
}
