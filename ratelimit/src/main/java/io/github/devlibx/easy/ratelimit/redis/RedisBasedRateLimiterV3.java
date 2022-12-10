package io.github.devlibx.easy.ratelimit.redis;

import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.Safe;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.github.devlibx.easy.ratelimit.IRateLimiter;
import io.github.devlibx.easy.ratelimit.RateLimiterConfig;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;

import javax.inject.Inject;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class RedisBasedRateLimiterV3 implements IRateLimiter {
    private final RateLimiterConfig rateLimiterConfig;
    private final IMetrics metrics;
    private RedissonClient redissonClient;

    @Getter
    private RRateLimiter limiter;
    private final Lock limiterLock;
    private final CircuitBreaker circuitBreaker;

    @Inject
    public RedisBasedRateLimiterV3(RateLimiterConfig rateLimiterConfig, IMetrics metrics) {
        this.rateLimiterConfig = rateLimiterConfig;
        this.metrics = metrics;
        this.limiterLock = new ReentrantLock();

        // Set up a circuit breaker if redis is down
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(rateLimiterConfig.getProperties().getInt("circuit-breaker-config-failureRateThreshold", 50))
                .minimumNumberOfCalls(rateLimiterConfig.getProperties().getInt("circuit-breaker-config-minimumNumberOfCalls", 10))
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .build();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        circuitBreaker = circuitBreakerRegistry.circuitBreaker(rateLimiterConfig.getName());
    }

    @Override
    public void start() {
        if (rateLimiterConfig.getRedis() != null) {

            // Get revision client provider
            IRedissonProvider redissonProvider;
            try {
                redissonProvider = ApplicationContext.getInstance(IRedissonProvider.class);
            } catch (Exception e) {
                redissonProvider = new IRedissonProvider.DefaultRedissonProvider();
            }

            // Create limiter
            redissonClient = redissonProvider.get(rateLimiterConfig.getRedis());

            // Delete a existing rate limiter if exists
            limiter = redissonClient.getRateLimiter(rateLimiterConfig.getName());
            applyRate(limiter);

            // Start rate limit job
            rateLimiterConfig.getRateLimitJob().ifPresent(rateLimitJob -> {
                rateLimitJob.startRateLimitJob(rateLimiterConfig);
            });

        } else {
            throw new RuntimeException("redis property must be set to use redis based rate limiter");
        }
    }

    // Apply rate to limiter
    private boolean applyRate(RRateLimiter limiter) {
        if (limiter == null) return false;
        return true;
    }

    @Override
    public boolean trySetRate(long rate) {
        rateLimiterConfig.setRate((int) rate);
        return applyRate(limiter);
    }

    @Override
    public void acquire() {
        acquire(1);
    }

    @Override
    public void acquire(long permits) {
        int retry = 3;
        while (retry-- >= 0) {
            try {
                if (limiter != null) {
                    Runnable runnable = CircuitBreaker.decorateRunnable(circuitBreaker, () -> {
                        RedissonRateLimiterExt redissonRateLimiterExt = (RedissonRateLimiterExt) limiter;
                        redissonRateLimiterExt.setRateLimiterConfig(rateLimiterConfig);
                        redissonRateLimiterExt.acquireExtV3(permits);
                        metrics.inc("rate_limiter", (int) permits, "name", rateLimiterConfig.getName(), "status", "ok");
                    });
                    runnable.run();
                    retry = -1;
                } else {
                    metrics.inc("rate_limiter", (int) permits, "name", rateLimiterConfig.getName(), "status", "error", "error", "linter_null");
                    sleep(10);
                }
            } catch (CallNotPermittedException e) {
                log.error("circuit open in taking lock. Lock is taken: name={}, retryCount={}, error={}", rateLimiterConfig.getName(), retry, e.getMessage());
                retry = -1;
                metrics.inc("rate_limiter", (int) permits, "name", rateLimiterConfig.getName(), "status", "error", "error", "circuit_open");
            } catch (Exception e) {
                log.error("error in acquiring lock: name={}, retryCount={}", rateLimiterConfig.getName(), retry, e);
                metrics.inc("rate_limiter", (int) permits, "name", rateLimiterConfig.getName(), "status", "error", "error", "unknown");
                sleep(50);
            }
        }
    }

    @Override
    public void stop() {
        limiterLock.lock();
        try {
            if (redissonClient != null) {
                Safe.safe(() -> {
                    redissonClient.shutdown();
                    redissonClient = null;
                    limiter = null;
                }, "failed to stop redisson client: " + rateLimiterConfig);
            }
        } finally {
            limiterLock.unlock();
        }
    }

    @Override
    public StringObjectMap debug() {
        return StringObjectMap.of(
                "rateType", rateLimiterConfig.getRateType(),
                "rate", rateLimiterConfig.getRate(),
                "rateInterval", rateLimiterConfig.getRateInterval(),
                "rateIntervalUnit", rateLimiterConfig.getRateIntervalUnit()
        );
    }

    private void sleep(long l) {
        try {
            Thread.sleep(l);
        } catch (Exception ignored) {
        }
    }
}
