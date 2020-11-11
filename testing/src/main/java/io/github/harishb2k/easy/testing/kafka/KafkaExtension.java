package io.github.harishb2k.easy.testing.kafka;

import ch.qos.logback.classic.Level;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.Safe;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.shaded.com.github.dockerjava.core.command.AbstrDockerCmd;
import org.testcontainers.utility.DockerImageName;

import java.util.Properties;
import java.util.UUID;

@Slf4j
public class KafkaExtension implements ParameterResolver, BeforeAllCallback, AfterAllCallback, IKafkaExtensionControl {
    private KafkaContainer kafkaContainer;

    @Override
    public synchronized void beforeAll(ExtensionContext context) throws Exception {

        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(AbstrDockerCmd.class).setLevel(Level.OFF);

        // Only create in once for all test cases
        if (kafkaContainer != null && kafkaContainer.isRunning()) return;

        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"))
                .withReuse(true);
        try {
            kafkaContainer.start();
        } catch (Exception e) {
            log.error("failed to start kafka in docker: error={}", e.getMessage());
        }
    }

    @Override
    public synchronized void afterAll(ExtensionContext context) throws Exception {
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == KafkaConfig.class
                || parameterContext.getParameter().getType() == Producer.class
                || parameterContext.getParameter().getType() == Consumer.class
                || parameterContext.getParameter().getType() == IKafkaExtensionControl.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (parameterContext.getParameter().getType() == KafkaConfig.class) {
            KafkaConfig kafkaConfig = new KafkaConfig();
            kafkaConfig.setRunning(false);
            if (isKafkaRunning()) {
                kafkaConfig.setBrokers(kafkaContainer.getBootstrapServers());
                kafkaConfig.setRunning(kafkaContainer.isRunning());
            }
            return kafkaConfig;
        } else if (parameterContext.getParameter().getType() == Producer.class) {
            if (isKafkaRunning()) {
                return createProducer();
            } else {
                return null;
            }
        } else if (parameterContext.getParameter().getType() == Consumer.class) {
            if (isKafkaRunning()) {
                return createConsumer();
            } else {
                return null;
            }
        } else if (parameterContext.getParameter().getType() == IKafkaExtensionControl.class) {
            return this;
        }
        return null;
    }

    @Override
    public boolean isKafkaRunning() {
        return kafkaContainer != null && kafkaContainer.isRunning();
    }

    private Producer<String, String> createProducer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaContainer.getBootstrapServers());
        properties.put("retries", 1);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("acks", "all");
        return new KafkaProducer<>(properties);
    }

    private KafkaConsumer<String, String> createConsumer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", kafkaContainer.getBootstrapServers());
        properties.put("group.id", UUID.randomUUID().toString());
        properties.put("auto.offset.reset", "earliest");
        properties.put("enable.auto.commit", "true");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("max.poll.records", "10");
        return new KafkaConsumer<>(properties);
    }

    @Override
    public synchronized void stopIfRunning() {
        Safe.safe(() -> kafkaContainer.stop());
        kafkaContainer = null;
    }
}
