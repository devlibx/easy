package io.github.devlibx.easy.messaging.kafka.producer;

import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.string.StringHelper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class KafkaBasedResilientProducer extends KafkaBasedProducer {
    private final CircuitBreaker circuitBreaker;

    public KafkaBasedResilientProducer(StringObjectMap config, StringHelper stringHelper, IMetrics metrics) {
        super(config, stringHelper, metrics);

        // Setup a circuit breaker with default settings
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .waitDurationInOpenState(Duration.ofMillis(100))
                .build();
        circuitBreaker = CircuitBreaker.of(config.getString("name") + "-circuit-breaker", circuitBreakerConfig);
    }

    @Override
    public boolean send(String key, Object value) {
        long start = System.currentTimeMillis();
        try {
            circuitBreaker.decorateCallable(() -> KafkaBasedResilientProducer.super.send(key, value)).call();
        } catch (CallNotPermittedException e) {
            metrics.observe(metricsPrefix + "_failure_time_taken", (System.currentTimeMillis() - start));
            metrics.inc(metricsPrefix + "_failure");
            metrics.inc(metricsPrefix + "_failure_circuit_open");
            log.error("failed to send kafka message: topic={}, error={}", config.getString("name"), e.getMessage());
        } catch (Exception e) {
            metrics.observe(metricsPrefix + "_failure_time_taken", (System.currentTimeMillis() - start));
            metrics.inc(metricsPrefix + "_failure");
            log.error("circuit-open to send kafka message: topic={}, error={}", config.getString("name"), e.getMessage());
        }
        return false;
    }
}
