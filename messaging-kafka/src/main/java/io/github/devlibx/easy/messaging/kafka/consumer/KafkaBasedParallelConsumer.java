package io.github.devlibx.easy.messaging.kafka.consumer;

import io.gitbub.devlibx.easy.helper.Safe;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KafkaBasedParallelConsumer extends KafkaBasedConsumer {
    private final ExecutorService parallelMessageExecutorService;

    public KafkaBasedParallelConsumer(StringObjectMap config, IMetrics metrics) {
        super(config, metrics);
        String topic = config.getString("topic");
        int parallelThread = config.getInt("parallelThreadCount");
        log.info("KafkaConsumer - consumer will run {} threads to process messages from topic={}", topic, parallelThread);
        parallelMessageExecutorService = Executors.newScheduledThreadPool(parallelThread);
    }

    protected Runnable consumerRunnable(Consumer<String, Object> consumer, IMessageConsumer messageConsumer) {
        final int pollTime;
        // parallelMsgProcessTimeout is in seconds and default is 5 sec
        final long parallelMsgProcessTimeout = config.getInt("parallelMsgProcessTimeout", 5);
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
                    int recordCount = records.count();
                    if (recordCount == 0) {
                        continue;
                    }

                    // Process each record
                    CountDownLatch processingLatch = new CountDownLatch(recordCount);
                    for (ConsumerRecord<String, Object> record : records) {
                        parallelMessageExecutorService.submit(() -> {
                            try {
                                processSingleRecord(messageConsumer, topic, record);
                            } finally {
                                processingLatch.countDown();
                            }
                        });
                    }

                    // Wait for all events to get process (for faery we will wait for max of 5 sec for each message)
                    boolean result = processingLatch.await(recordCount * parallelMsgProcessTimeout, TimeUnit.SECONDS);
                    if (!result) {
                        log.error("Processing messages in parallel but messages did not finish on time : topic={}, noOfMessagesToProcess={}", topic, recordCount);
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

    private void processSingleRecord(IMessageConsumer messageConsumer, String topic, ConsumerRecord<String, Object> record) {
        String key = null;
        long start = System.currentTimeMillis();

        try {
            key = record.key();

            // Pass message the client message processor
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

    @Override
    public void shutdown() {
        super.shutdown();
        Safe.safe(parallelMessageExecutorService::shutdownNow);
    }
}
