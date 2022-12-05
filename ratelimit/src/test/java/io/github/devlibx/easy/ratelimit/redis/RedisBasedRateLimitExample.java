package io.github.devlibx.easy.ratelimit.redis;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.LoggingHelper;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.yaml.YamlUtils;
import io.github.devlibx.easy.ratelimit.IRateLimiterFactory;
import io.github.devlibx.easy.ratelimit.RateLimiterFactoryConfig;
import io.github.devlibx.easy.ratelimit.impl.RateLimiterFactory;
import io.github.devlibx.easy.ratelimit.job.ddb.DynamoDbWriteRateLimitJob;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.Charset;

public class RedisBasedRateLimitExample {

    public static void main(String[] args) throws Exception {
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(DynamoDbWriteRateLimitJob.class).setLevel(Level.DEBUG);

        //  Setup 1 - read config from your yaml file
        String rateLimiterName = "example-config-normal";
        String testFilePath = new File(".").getAbsoluteFile().getAbsolutePath() + "/ratelimit/src/test/resources/example.yaml";
        String content = FileUtils.readFileToString(new File(testFilePath), Charset.defaultCharset());
        RateLimiterFactoryConfig rateLimiterFactoryConfig = YamlUtils.readYamlFromString(content, Config.class).config;


        // Setup 2 - Start the rate limiter
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IRateLimiterFactory.class).to(RateLimiterFactory.class).in(Scopes.SINGLETON);
                bind(IMetrics.class).to(IMetrics.NoOpMetrics.class);
                bind(RateLimiterFactoryConfig.class).toInstance(rateLimiterFactoryConfig);
            }
        });
        ApplicationContext.setInjector(injector);

        // ***** MANDATORY STEP *****
        IRateLimiterFactory rateLimiterFactory = injector.getInstance(IRateLimiterFactory.class);
        rateLimiterFactory.start();

        // This will update the rate limit every 1 sec
        rateLimiterFactory.get(rateLimiterName).ifPresent(rateLimiter -> {
            new Thread(() -> {
                int value = 1;
                while (true) {
                    sleep(1000);
                    boolean result = rateLimiter.trySetRate(value);
                    System.out.println("Result of setting the new rate: " + result + ", new rate=" + value);
                    value++;
                }
            }).start();
        });

        // Try to take the lock
        // If rate limiter is disabled then this will be a NO-OP (easy to disabel rate limit)
        rateLimiterFactory.get(rateLimiterName).ifPresent(rateLimiter -> {
            for (int i = 0; i < 30; i++) {
                rateLimiter.acquire();
                System.out.println("Work with new rate limit: item=" + i);
            }
        });
    }

    @NoArgsConstructor
    private static class Config {
        @JsonProperty("rate_limit_factory")
        private RateLimiterFactoryConfig config;
    }

    private static void sleep(long l) {
        try {
            Thread.sleep(l);
        } catch (Exception ignored) {
        }
    }

}
