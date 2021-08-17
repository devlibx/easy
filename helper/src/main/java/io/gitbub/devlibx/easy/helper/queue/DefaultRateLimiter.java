package io.gitbub.devlibx.easy.helper.queue;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import java.time.Duration;

public class DefaultRateLimiter implements IRateLimiter {
    private final RateLimiter rateLimiter;

    public DefaultRateLimiter(Config c) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(c.waitTimeToAcquirePermissionToExecuteInMs <= 0 ? Integer.MAX_VALUE : c.waitTimeToAcquirePermissionToExecuteInMs))
                .limitRefreshPeriod(Duration.ofMillis(c.timeToResetPermissionBackToInitialValueInMs <= 0 ? 1000 : c.timeToResetPermissionBackToInitialValueInMs))
                .limitForPeriod(c.limit <= 0 ? 100 : c.limit)
                .build();
        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        rateLimiter = registry.rateLimiter("default");
    }

    @Override
    public void execute(Runnable runnable) {
        rateLimiter.executeRunnable(runnable);
    }
}
