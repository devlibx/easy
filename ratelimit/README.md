This is a simple redis based distributed rate limiter. It can wokr with any custom logic which changes the limit at
runtime.

One of the way is to rate limit by the DynamoDB table write unit to avoid throllting (example shared).

## How to use example

Get the rate limiter by name - defined in your config and acquire permits to continue (full
code ```https://github.com/devlibx/easy/tree/master/ratelimit/src/test/java/io/github/devlibx/easy/ratelimit/redis/RedisBasedRateLimitExample.java```)

```java
rateLimiterFactory.get(rateLimiterName).ifPresent(rateLimiter->{
        rateLimiter.acquire();
        // DO YOUR WORK HERE
        });
```

```yaml
rate_limit_factory:
  enabled: true
  rate_limiters:
    example-config-normal:
      enabled: true
      redis:
        host: localhost
        port: 6379      
      rate: 1                         <<< How many requests are permitted
      rate_interval: 1                <<< After how long rate limit will reset (value)
      rate_interval_unit: SECONDS     <<< After how long rate limit will reset (unit)
```

```java

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

        RateLimiterFactoryConfig rateLimiterFactoryConfig = /** Read config from some place */;

        // Setup 1 - Start the rate limiter
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
            // Update the rate with some logic
            boolean result = rateLimiter.trySetRate(value);
        });

        // Try to take the lock
        // If rate limiter is disabled then this will be a NO-OP (easy to disable rate limit)
        rateLimiterFactory.get(rateLimiterName).ifPresent(rateLimiter -> {
            rateLimiter.acquire();
            // DO YOUR WORK HERE
        });
    }
}
```

## How to ratelimit with DynamoDB write unit

full
code ```https://github.com/devlibx/easy/tree/master/ratelimit/src/test/java/io/github/devlibx/easy/ratelimit/redis/RedisBasedRateLimitWithDynamoDbExample.java```

```yaml
rate_limit_factory:
  enabled: true
  rate_limiters:
    example-config-normal:
      enabled: true
      redis:
        host: localhost
        port: 6379
      rate: 1
      rate_interval: 1
      rate_interval_unit: SECONDS
      rate_limit_job_config:
        refresh-time-in-sec: 1            <<< refresh rate-limit every N seconds DDB 
        rate-limit-by-write: true         <<< if true, set write limit, if false set read limit 
        rate-limit-class: io.github.devlibx.easy.ratelimit.job.ddb.DynamoDbWriteRateLimitJob
        rate-limit-factor: 0.9            <<< Max rate limit allowed e.g. if DDB has 100 write unit per sec, then (0.9 * 100) = 90 per sec is allowed
        region: ap-south-1              
        enabled: true
        table: test                       <<< Name of the table to check the write/read units
```

```java
package io.github.devlibx.easy.ratelimit.redis;

import ch.qos.logback.classic.Level;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.LoggingHelper;
import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.yaml.YamlUtils;
import io.github.devlibx.easy.ratelimit.IRateLimiter;
import io.github.devlibx.easy.ratelimit.IRateLimiterFactory;
import io.github.devlibx.easy.ratelimit.RateLimiterFactoryConfig;
import io.github.devlibx.easy.ratelimit.impl.RateLimiterFactory;
import io.github.devlibx.easy.ratelimit.job.ddb.DynamoDbWriteRateLimitJob;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.Charset;

public class RedisBasedRateLimitWithDynamoDbExample {

    public static void main(String[] args) throws Exception {
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(DynamoDbWriteRateLimitJob.class).setLevel(Level.DEBUG);

        //  Setup 1 - read config from your yaml file
        String rateLimiterName = "example-config-normal";
        String testFilePath = new File(".").getAbsoluteFile().getAbsolutePath() + "/ratelimit/src/test/resources/example-with-ddb.yaml";
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

        // We will write to
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.AP_SOUTH_1).build();
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(rateLimiterFactoryConfig.getRateLimiters().get(rateLimiterName).getRateLimitJobConfig().getString("table"));

        for (int i = 0; i < 1_000_000; i++) {
            Data data = Data.builder().id("id_" + i).data("data_" + i).build();
            rateLimiterFactory.get(rateLimiterName).ifPresent(IRateLimiter::acquire);
            table.putItem(Item.fromJSON(JsonUtils.asJson(data)));
            System.out.println("Write done - " + i);
        }
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

    @lombok.Data
    @Builder
    private static class Data {
        private String id;
        private String data;
    }
}
```