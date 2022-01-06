package io.github.devlibx.easy.messaging.kafka.producer;

import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.Safe;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.string.StringHelper;
import io.github.devlibx.easy.messaging.producer.IProducer;
import io.github.devlibx.easy.messaging.producer.IProducerService;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class KafkaBasedProducerService implements IProducerService {
    private final Map<String, KafkaBasedProducer> kafkaBasedProducerMap;
    private final StringHelper stringHelper;
    private final IMetrics metrics;

    @Inject
    public KafkaBasedProducerService(StringHelper stringHelper, IMetrics metrics) {
        this.kafkaBasedProducerMap = new ConcurrentHashMap<>();
        this.stringHelper = stringHelper;
        this.metrics = metrics;
    }

    @Override
    public void initialize(StringObjectMap config) {

        // Make sure name is provided
        String name = config.getString("name");
        if (Strings.isNullOrEmpty(name)) {
            throw new RuntimeException("name is not provided in kafka producer config");
        }

        // Create a producer
        if (!kafkaBasedProducerMap.containsKey(name)) {
            KafkaBasedProducer producer;
            if (config.getBoolean("enableCircuitBreakerOnError", false)) {
                producer = new KafkaBasedResilientProducer(config, stringHelper, metrics);
            } else {
                producer = new KafkaBasedProducer(config, stringHelper, metrics);
            }
            producer.start();
            kafkaBasedProducerMap.put(name, producer);
        }
    }

    @Override
    public Optional<IProducer> getProducer(String name) {
        IProducer producer = kafkaBasedProducerMap.get(name);
        return Optional.ofNullable(producer);
    }

    @Override
    public void shutdown() {
        kafkaBasedProducerMap.forEach((name, kafkaBasedProducer) -> Safe.safe(kafkaBasedProducer::shutdown));
    }
}
