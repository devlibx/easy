package io.github.devlibx.easy.ratelimit.impl;

import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.Safe;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.github.devlibx.easy.ratelimit.IRateLimiter;
import io.github.devlibx.easy.ratelimit.IRateLimiterFactory;
import io.github.devlibx.easy.ratelimit.RateLimiterFactoryConfig;
import io.github.devlibx.easy.ratelimit.redis.RedisBasedRateLimiter;
import io.github.devlibx.easy.ratelimit.redis.RedisBasedRateLimiterV2;
import io.github.devlibx.easy.ratelimit.redis.RedisBasedRateLimiterV3;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class RateLimiterFactory implements IRateLimiterFactory {
    private final RateLimiterFactoryConfig rateLimiterFactoryConfig;
    private final IMetrics metrics;
    private final Map<String, IRateLimiter> rateLimiterMap;

    @Inject
    public RateLimiterFactory(RateLimiterFactoryConfig rateLimiterFactoryConfig, IMetrics metrics) {
        this.rateLimiterFactoryConfig = rateLimiterFactoryConfig;
        this.metrics = metrics;
        this.rateLimiterMap = new HashMap<>();
    }

    @Override
    public void start() {
        if (rateLimiterFactoryConfig.isEnabled()) {
            rateLimiterFactoryConfig.getRateLimiters().forEach((name, rateLimiterConfig) -> {
                rateLimiterConfig.setName(name);
                if (Strings.isNullOrEmpty(rateLimiterConfig.getPrefix())) {
                    rateLimiterConfig.setPrefix(rateLimiterFactoryConfig.getPrefix());
                }
                if (rateLimiterConfig.getRedis() != null) {

                    // Create a rate limiter
                    IRateLimiter rateLimiter;
                    if (Objects.equals(rateLimiterConfig.getRedis().getVersion(), "v2")) {
                        rateLimiter = new RedisBasedRateLimiterV2(rateLimiterConfig, metrics);
                    } else if (Objects.equals(rateLimiterConfig.getRedis().getVersion(), "v3")) {
                        rateLimiter = new RedisBasedRateLimiterV3(rateLimiterConfig, metrics);
                    } else {
                        rateLimiter = new RedisBasedRateLimiter(rateLimiterConfig, metrics);
                    }
                    rateLimiter.start();

                    // Set the rate limit (try for 3 times)
                    boolean result = false;
                    int count = 3;
                    while (count-- >= 0 && !result) {
                        result = rateLimiter.trySetRate(rateLimiterConfig.getRate());
                    }

                    rateLimiterMap.put(name, rateLimiter);
                } else {
                    throw new RuntimeException("Only redis type is support for rate limit");
                }
            });
        }
    }

    @Override
    public void stop() {
        Safe.safe(() -> rateLimiterMap.forEach((name, rateLimiter) -> {
            Safe.safe(rateLimiter::stop, "failed to stop rate limiter: " + name);
        }));
    }

    @Override
    public Optional<IRateLimiter> get(String name) {
        if (rateLimiterMap.containsKey(name)) {
            return Optional.ofNullable(rateLimiterMap.get(name));
        }
        return Optional.empty();
    }
}
