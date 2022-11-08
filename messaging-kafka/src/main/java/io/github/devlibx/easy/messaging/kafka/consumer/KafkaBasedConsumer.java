package io.github.devlibx.easy.messaging.kafka.consumer;

import io.gitbub.devlibx.easy.helper.Safe;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.github.devlibx.easy.messaging.consumer.IConsumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class KafkaBasedConsumer implements IConsumer {
    protected final StringObjectMap config;
    protected final IMetrics metrics;
    protected final String metricsPrefix;
    protected final boolean metricsEnabled;
    private final ExecutorService executorService;
    private final List<Consumer<String, Object>> consumers;
    protected final AtomicBoolean stop;
    protected final CountDownLatch stopLatch;
    private final int threadCount;

    public KafkaBasedConsumer(StringObjectMap config, IMetrics metrics) {
        this.config = config;
        this.metrics = metrics;
        this.metricsPrefix = config.getString("name", UUID.randomUUID().toString());
        this.stop = new AtomicBoolean(false);
        this.consumers = new ArrayList<>();

        if (config.containsKey("thread.count")) {
            threadCount = config.getInt("thread.count", 2);
        } else if (config.containsKey("thread-count")) {
            threadCount = config.getInt("thread-count", 2);
        } else {
            threadCount = 2;
        }

        if (config.containsKey("metrics.enabled")) {
            this.metricsEnabled = config.getBoolean("metrics.enabled", Boolean.TRUE);
        } else if (config.containsKey("metrics-enabled")) {
            this.metricsEnabled = config.getBoolean("metrics-enabled", Boolean.TRUE);
        } else {
            this.metricsEnabled = Boolean.TRUE;
        }

        this.executorService = Executors.newScheduledThreadPool(threadCount);
        this.stopLatch = new CountDownLatch(threadCount);
    }

    @Override
    public void start(IMessageConsumer messageConsumer) {
        for (int i = 0; i < threadCount; i++) {
            Consumer<String, Object> consumer = createConsumer();
            consumers.add(consumer);
            executorService.submit(consumerRunnable(consumer, messageConsumer));
        }
    }

    protected Runnable consumerRunnable(Consumer<String, Object> consumer, IMessageConsumer messageConsumer) {
        final int pollTime;
        if (config.containsKey("poll.time")) {
            pollTime = config.getInt("poll.time", 100);
        } else if (config.containsKey("poll-time")) {
            pollTime = config.getInt("poll-time", 100);
        } else {
            pollTime = 100;
        }

        return () -> {
            final String topic = config.getString("topic");

            while (!stop.get()) {
                try {

                    // Fetch records
                    ConsumerRecords<String, Object> records = consumer.poll(pollTime);

                    // Process each record
                    for (ConsumerRecord<String, Object> record : records) {

                        String key = null;
                        long start = System.currentTimeMillis();

                        try {
                            key = record.key();

                            // Pass message the the client message processor
                            messageConsumer.process(record.value(), record);

                            if (metricsEnabled) {
                                metrics.inc(metricsPrefix + "_message_consume_success");
                                metrics.observe(metricsPrefix + "_message_consume_success_time_taken", (System.currentTimeMillis() - start));
                            }
                        } catch (Exception e) {
                            log.error("Got some in processing kafka message: topic={} key={}, error={}", topic, key, e.getMessage());
                            if (metricsEnabled) {
                                metrics.inc(metricsPrefix + "_message_consume_error");
                                metrics.observe(metricsPrefix + "_message_consume_error_time_taken", (System.currentTimeMillis() - start));
                            }
                        }
                    }

                } catch (Exception e) {
                    if (metricsEnabled) {
                        metrics.inc(metricsPrefix + "_message_consume_unknown_error");
                    }
                    log.error("Got some error in kafka consumer: topic={}, error={}", topic, e.getMessage());
                }
            }

            // Count down stopping latch
            stopLatch.countDown();
        };
    }

    private Consumer<String, Object> createConsumer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", config.getString("brokers", "localhost:9092"));

        if (config.containsKey("group.id")) {
            properties.put("group.id", config.getString("group.id", UUID.randomUUID().toString()));
        } else if (config.containsKey("group-id")) {
            properties.put("group.id", config.getString("group-id", UUID.randomUUID().toString()));
        } else {
            properties.put("group.id", UUID.randomUUID().toString());
        }

        if (config.containsKey("auto.offset.reset")) {
            properties.put("auto.offset.reset", config.getString("auto.offset.reset", "latest"));
        } else if (config.containsKey("auto-offset-reset")) {
            properties.put("auto.offset.reset", config.getString("auto-offset-reset", "latest"));
        } else {
            properties.put("auto.offset.reset", "latest");
        }

        if (config.containsKey("enable.auto.commit")) {
            properties.put("enable.auto.commit", config.getBoolean("enable.auto.commit", Boolean.TRUE));
        } else if (config.containsKey("enable-auto-commit")) {
            properties.put("enable.auto.commit", config.getBoolean("enable-auto-commit", Boolean.TRUE));
        } else {
            properties.put("enable.auto.commit", Boolean.TRUE);
        }

        if (config.containsKey("key.deserializer")) {
            properties.put("key.deserializer", config.getString("key.serializer", "org.apache.kafka.common.serialization.StringDeserializer"));
        } else if (config.containsKey("key-deserializer")) {
            properties.put("key.deserializer", config.getString("key-serializer", "org.apache.kafka.common.serialization.StringDeserializer"));
        } else {
            properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        }

        if (config.containsKey("value.deserializer")) {
            properties.put("value.deserializer", config.getString("value.serializer", "org.apache.kafka.common.serialization.StringDeserializer"));
        } else if (config.containsKey("value-deserializer")) {
            properties.put("value.deserializer", config.getString("value-serializer", "org.apache.kafka.common.serialization.StringDeserializer"));
        } else {
            properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        }

        if (config.containsKey("max.poll.records")) {
            properties.put("max.poll.records", config.getInt("max.poll.records", 10));
        } else if (config.containsKey("max-poll-records")) {
            properties.put("max.poll.records", config.getInt("max-poll-records", 10));
        } else {
            properties.put("max.poll.records", 10);
        }

        String topic = config.getString("topic");

        Consumer<String, Object> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Arrays.asList(topic.split(",")), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                log.info("onPartitionsRevoked for topic {}", topic);
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {
                log.info("onPartitionsAssigned for topic {}", topic);
            }
        });

        return consumer;
    }

    @Override
    public void shutdown() {

        // This will make the threads stop
        stop.set(true);

        // Wait for some time - just in-case we take time to shut down
        try {
            stopLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }

        // Waited long enough - stop threads
        Safe.safe(executorService::shutdownNow);

        // Finally stop all consumers
        consumers.forEach(consumer -> {
            Safe.safe(consumer::close);
        });
    }
}
