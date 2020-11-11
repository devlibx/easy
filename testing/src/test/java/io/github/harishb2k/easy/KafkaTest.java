package io.github.harishb2k.easy;

import ch.qos.logback.classic.Level;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.github.harishb2k.easy.testing.kafka.IKafkaExtensionControl;
import io.github.harishb2k.easy.testing.kafka.KafkaConfig;
import io.github.harishb2k.easy.testing.kafka.KafkaExtension;
import org.apache.kafka.clients.consumer.internals.AbstractCoordinator;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;
import java.util.concurrent.Future;

@ExtendWith(KafkaExtension.class)
public class KafkaTest {

    @BeforeClass
    public static void setup() {
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(AbstractCoordinator.class).setLevel(Level.OFF);
        LoggingHelper.getLogger("org.apache.kafka.clients.consumer.internals").setLevel(Level.OFF);
        LoggingHelper.getLogger(org.apache.kafka.clients.NetworkClient.class).setLevel(Level.OFF);
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
