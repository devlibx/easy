package io.github.devlibx.easy.messaging.kafka.producer;

import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.string.StringHelper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KafkaBasedResilientProducer extends KafkaBasedProducer {
    private final CircuitBreaker circuitBreaker;

    public KafkaBasedResilientProducer(StringObjectMap config, StringHelper stringHelper, IMetrics metrics) {
        super(config, stringHelper, metrics);

        // How long to remain in open state when circuit opens (default = 10sec)
        int waitDurationInOpenState;
        if (config.containsKey("circuit-breaker.stay_in_open_state_on_error.ms")) {
            waitDurationInOpenState = config.getInt("circuit-breaker.stay_in_open_state_on_error.ms", 10 * 1000);
        } else if (config.containsKey("circuit-breaker-stay_in_open_state_on_error-ms")) {
            waitDurationInOpenState = config.getInt("circuit-breaker-stay_in_open_state_on_error-ms", 10 * 1000);
        } else {
            waitDurationInOpenState = 10 * 1000;
        }

        // Setup a circuit breaker with default settings
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .waitDurationInOpenState(Duration.ofMillis(waitDurationInOpenState))
                .build();
        circuitBreaker = CircuitBreaker.of(config.getString("name") + "-circuit-breaker", circuitBreakerConfig);
    }

    @Override
    public boolean send(String key, Object value) {
        long start = System.currentTimeMillis();
        try {
            try {
                return circuitBreaker.decorateCallable(() -> KafkaBasedResilientProducer.super.send(key, value)).call();
            } catch (InternalErrorWrapper wrapperError) {
                throw wrapperError.e;
            }
        } catch (CallNotPermittedException e) {
            metrics.observe(metricsPrefix + "_failure_time_taken", (System.currentTimeMillis() - start));
            metrics.inc(metricsPrefix + "_failure");
            metrics.inc(metricsPrefix + "_failure_circuit_open");
            log.error("failed to send kafka message (CallNotPermittedException): topic={}, error={}", config.getString("name"), e.getMessage());
        } catch (Exception e) {
            metrics.observe(metricsPrefix + "_failure_time_taken", (System.currentTimeMillis() - start));
            metrics.inc(metricsPrefix + "_failure");
            log.error("circuit-open to send kafka message: topic={}, error={}", config.getString("name"), e.getMessage());
        }
        return false;
    }

    @Override
    protected void processError(Exception e) {
        throw new InternalErrorWrapper(e);
    }

    @Override
    protected void processErrorOnAsyncResponse(long duration, TimeUnit durationUnit, Throwable throwable) {
        try {
            circuitBreaker.onError(duration, durationUnit, throwable);
        } catch (Exception ignored) {
        }
    }

    private static class InternalErrorWrapper extends RuntimeException {
        private final Exception e;

        private InternalErrorWrapper(Exception e) {
            this.e = e;
        }
    }
}
