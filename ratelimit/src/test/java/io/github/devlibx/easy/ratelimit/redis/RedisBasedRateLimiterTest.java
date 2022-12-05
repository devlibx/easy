package io.github.devlibx.easy.ratelimit.redis;


import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.LoggingHelper;
import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.yaml.YamlUtils;
import io.github.devlibx.easy.ratelimit.IRateLimitJob;
import io.github.devlibx.easy.ratelimit.IRateLimiterFactory;
import io.github.devlibx.easy.ratelimit.RateLimiterConfig;
import io.github.devlibx.easy.ratelimit.RateLimiterFactoryConfig;
import io.github.devlibx.easy.ratelimit.impl.RateLimiterFactory;
import io.github.devlibx.easy.ratelimit.job.ddb.DynamoDbWriteRateLimitJob;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
                .rateLimitJobConfig(StringObjectMap.of(
                        "enabled", false,
                        "rate-limit-class", "io.github.devlibx.easy.ratelimit.job.ddb.DynamoDbWriteRateLimitJob",
                        "refresh-time", 10,
                        "region", "ap-south-1",
                        "table", "test",
                        "rate-limit-by-write", true,
                        "rate-limit-factor", 0.3
                ))
                .build();
        RateLimiterFactoryConfig rateLimiterFactoryConfig = RateLimiterFactoryConfig.builder()
                .build();
        rateLimiterFactoryConfig.getRateLimiters().put(rateLimiterName, rateLimiterConfig);
        System.out.println(JsonUtils.asJson(rateLimiterFactoryConfig));

        // Setup 2 - Start the rate limiter
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                super.configure();
                bind(IRateLimiterFactory.class).to(RateLimiterFactory.class).in(Scopes.SINGLETON);
                bind(IMetrics.class).to(IMetrics.NoOpMetrics.class);
                bind(RateLimiterFactoryConfig.class).toInstance(rateLimiterFactoryConfig);

                bind(IRateLimitJob.class).annotatedWith(Names.named("ddb")).to(DynamoDbWriteRateLimitJob.class);
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

    @Test
    public void testSimpleWithDDB() throws IOException, InterruptedException {
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(DynamoDbWriteRateLimitJob.class).setLevel(Level.DEBUG);

        String rateLimiterName = "test-7";
        String testFilePath = new File(".").getAbsoluteFile().getAbsolutePath() + "/src/test/resources/test-config.yaml";
        String content = FileUtils.readFileToString(new File(testFilePath), Charset.defaultCharset());
        Config config = YamlUtils.readYamlFromString(content, Config.class);
        RateLimiterFactoryConfig rateLimiterFactoryConfig = config.config;

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
        ApplicationContext.setInjector(injector);
        IRateLimiterFactory rateLimiterFactory = injector.getInstance(IRateLimiterFactory.class);
        rateLimiterFactory.start();

        Thread.sleep(100 * 1000);
    }

    @NoArgsConstructor
    private static class Config {
        @JsonProperty("rate_limit_factory")
        private RateLimiterFactoryConfig config;
    }
}