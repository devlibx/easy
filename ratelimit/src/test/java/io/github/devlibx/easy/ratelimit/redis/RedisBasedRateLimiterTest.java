package io.github.devlibx.easy.ratelimit.redis;


import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.gitbub.devlibx.easy.helper.LoggingHelper;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.github.devlibx.easy.ratelimit.IRateLimiterFactory;
import io.github.devlibx.easy.ratelimit.RateLimiterConfig;
import io.github.devlibx.easy.ratelimit.RateLimiterFactoryConfig;
import io.github.devlibx.easy.ratelimit.impl.RateLimiterFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class RedisBasedRateLimiterTest {

    @Test
    public void testSimple() {
        LoggingHelper.setupLogging();

        // Setup 1 - Create rate limit config
        String rateLimiterName = "test-7";
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.builder()
                .redis(RateLimiterConfig.Redis.builder()
                        .host("localhost")
                        .port(6379)
                        .build())
                .rate(1)
                .build();
        RateLimiterFactoryConfig rateLimiterFactoryConfig = RateLimiterFactoryConfig.builder()
                .build();
        rateLimiterFactoryConfig.getRateLimiters().put(rateLimiterName, rateLimiterConfig);

        // Setup 2 - Start the rate limiter
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                super.configure();
                bind(IRateLimiterFactory.class).to(RateLimiterFactory.class).in(Scopes.SINGLETON);
                bind(IMetrics.class).to(IMetrics.NoOpMetrics.class);
                bind(RateLimiterFactoryConfig.class).toInstance(rateLimiterFactoryConfig);
            }
        });
        IRateLimiterFactory rateLimiterFactory = injector.getInstance(IRateLimiterFactory.class);
        rateLimiterFactory.start();

        // This will update the rate limit every 1 sec
        rateLimiterFactory.get(rateLimiterName).ifPresent(iRateLimiter -> {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int value = 1;
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        boolean result = iRateLimiter.trySetRate(value);
                        System.out.println("Result of setting the new rate: " + result + ", new rate=" + value);
                        value++;
                    }
                }
            }).start();
        });

        // Try to take the lock
        AtomicBoolean acquired = new AtomicBoolean();
        rateLimiterFactory.get(rateLimiterName).ifPresent(rateLimiter -> {
            for (int i = 0; i < 30; i++) {
                rateLimiter.acquire();
                System.out.println("-->> lock index: " + i);
            }
            acquired.set(true);
        });
        Assert.assertTrue(acquired.get());
    }
}