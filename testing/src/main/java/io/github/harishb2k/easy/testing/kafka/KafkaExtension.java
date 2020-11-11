package io.github.harishb2k.easy.testing.kafka;

import ch.qos.logback.classic.Level;
import io.gitbub.harishb2k.easy.helper.LoggingHelper;
import io.gitbub.harishb2k.easy.helper.Safe;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.shaded.com.github.dockerjava.core.command.AbstrDockerCmd;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Slf4j
public class KafkaExtension implements ParameterResolver, BeforeAllCallback, AfterAllCallback, IKafkaExtensionControl {
    private KafkaContainer kafkaContainer;
    private AdminClient client;

    @Override
    public synchronized void beforeAll(ExtensionContext context) throws Exception {
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(AbstrDockerCmd.class).setLevel(Level.OFF);

        // Check global store to see if we already have kafka client
        Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);

        LocalKafkaClientHolder kafkaHolder = store.getOrComputeIfAbsent(LocalKafkaClientHolder.class);
        if (kafkaHolder.isRunning()) {
            client = kafkaHolder.localKafkaClient;
        } else {
            DockerContainerKafkaHolder kafkaContainerHolder = store.getOrComputeIfAbsent(DockerContainerKafkaHolder.class);
            if (kafkaContainerHolder.isRunning()) {
                kafkaContainer = kafkaContainerHolder.kafkaContainer;
            } else {
                kafkaContainerHolder.start();
                if (kafkaContainerHolder.isRunning()) {
                    kafkaContainer = kafkaContainerHolder.kafkaContainer;
                }
            }
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
            if (kafkaContainer != null) {
                log.info("Harish : {}", kafkaContainer.isRunning());
            }
            if (isKafkaRunning()) {
                kafkaConfig.setBrokers(getKafkaUrl());
                kafkaConfig.setRunning(true);
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

    private Producer<String, String> createProducer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", getKafkaUrl());
        properties.put("retries", 1);
        properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("acks", "all");
        return new KafkaProducer<>(properties);
    }

    private KafkaConsumer<String, String> createConsumer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", getKafkaUrl());
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
        if (kafkaContainer != null) {
            Safe.safe(() -> kafkaContainer.stop());
            kafkaContainer = null;
        }
        if (client != null) {
            Safe.safe(() -> client.close());
            client = null;
        }
    }

    @Override
    public synchronized boolean isKafkaRunning() {
        if (kafkaContainer != null) {
            return kafkaContainer.isRunning();
        } else if (client != null) {
            try {
                client.listTopics();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private String getKafkaUrl() {
        if (kafkaContainer != null) {
            return kafkaContainer.getBootstrapServers();
        } else if (client != null) {
            try {
                return "localhost:9092";
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * This class creates and holds reference to docker kafka client
     */
    public static class DockerContainerKafkaHolder {
        private KafkaContainer kafkaContainer;

        public DockerContainerKafkaHolder() {
            start();
        }

        public void start() {

            // Stop any running container
            if (kafkaContainer != null) {
                Safe.safe(() -> kafkaContainer.stop());
            }

            try {
                log.info("Try to create a client for docker kafka - to see if we can use local kafka");
                // Local kafka is not running we will try docker
                kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:5.4.3"))
                        .withReuse(true);
                kafkaContainer.start();
                log.info("docker kafka available");
            } catch (Exception e) {
                log.error("failed to start kafka in docker: error={}", e.getMessage());
                kafkaContainer = null;
            }
        }

        /**
         * @return true if local kafka is running
         */
        public boolean isRunning() {
            try {
                if (kafkaContainer != null) {
                    log.info("Docker kafka running = {}", kafkaContainer.isRunning());
                }
                return kafkaContainer != null && kafkaContainer.isRunning();
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * This class creates and holds reference to local kafka client
     */
    public static class LocalKafkaClientHolder {
        private AdminClient localKafkaClient;

        public LocalKafkaClientHolder() {
            try {
                log.info("Try to create a client for local kafka - to see if we can use local kafka");
                Map<String, Object> conf = new HashMap<>();
                conf.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
                conf.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
                localKafkaClient = AdminClient.create(conf);
                DescribeClusterResult result = localKafkaClient.describeCluster();
                localKafkaClient.listTopics();
                log.info("local kafka available: {} {}", result, result.nodes().get().size());
            } catch (Exception e) {
                log.error("Kafka not running in localhost: we will try to use kafka on docker: e={}", e.getMessage());
                if (localKafkaClient != null) {
                    Safe.safe(() -> localKafkaClient.close());
                }
                localKafkaClient = null;
            }
        }

        /**
         * @return true if local kafka is running
         */
        public boolean isRunning() {
            if (localKafkaClient != null) {
                try {
                    localKafkaClient.listTopics();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }
    }
}
