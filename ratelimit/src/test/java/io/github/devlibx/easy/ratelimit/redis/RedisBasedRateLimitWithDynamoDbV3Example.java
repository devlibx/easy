package io.github.devlibx.easy.ratelimit.redis;

import ch.qos.logback.classic.Level;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.LoggingHelper;
import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.gitbub.devlibx.easy.helper.metrics.MetricsConfig;
import io.gitbub.devlibx.easy.helper.yaml.YamlUtils;
import io.github.devlibx.easy.metrics.statsd.StatsdMetrics;
import io.github.devlibx.easy.ratelimit.IRateLimiterFactory;
import io.github.devlibx.easy.ratelimit.RateLimiterFactoryConfig;
import io.github.devlibx.easy.ratelimit.impl.RateLimiterFactory;
import io.github.devlibx.easy.ratelimit.job.ddb.DynamoDbWriteRateLimitJob;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RedisBasedRateLimitWithDynamoDbV3Example {

    public static void main(String[] args) throws Exception {
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(DynamoDbWriteRateLimitJob.class).setLevel(Level.DEBUG);

        String host = System.getenv("statsd");
        MetricsConfig metricsConfig = null;
        if (true) {
            metricsConfig = MetricsConfig.builder()
                    .env("stage")
                    .host(host)
                    .port(80)
                    .prefix("p")
                    .serviceName("tests")
                    .pushInterval(100)
                    .enabled(true).build();
        }

        //  Setup 1 - read config from your yaml file
        String rateLimiterName = "example-config-normal";
        String testFilePath = new File(".").getAbsoluteFile().getAbsolutePath() + "/ratelimit/src/test/resources/example-with-ddb-v3.yaml";
        String content = FileUtils.readFileToString(new File(testFilePath), Charset.defaultCharset());
        RateLimiterFactoryConfig rateLimiterFactoryConfig = YamlUtils.readYamlFromString(content, Config.class).config;

        /*
        testFilePath = new File(".").getAbsoluteFile().getAbsolutePath() + "/ratelimit/src/test/resources/ratelimit.lua";
        String script = FileUtils.readFileToString(new File(testFilePath), Charset.defaultCharset());
        rateLimiterFactoryConfig.getRateLimiters().get(rateLimiterName)
                .getProperties()
                .put("script", script);
         */


        // Setup 2 - Start the rate limiter
        MetricsConfig metricsConfig1 = metricsConfig;
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(IRateLimiterFactory.class).to(RateLimiterFactory.class).in(Scopes.SINGLETON);
                bind(RateLimiterFactoryConfig.class).toInstance(rateLimiterFactoryConfig);
                if (metricsConfig1 != null) {
                    bind(IMetrics.class).to(StatsdMetrics.class).in(Scopes.SINGLETON);
                    bind(MetricsConfig.class).toInstance(metricsConfig1);
                } else {
                    bind(IMetrics.class).to(IMetrics.NoOpMetrics.class);
                }
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


        //Setup the reporter
        MetricRegistry metricRegistry = new MetricRegistry();
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(1, TimeUnit.HOURS);

        Lock lock = new ReentrantLock();
        AtomicInteger counter = new AtomicInteger();
        AtomicInteger totalCount = new AtomicInteger();
        AtomicReference<String> currentSecAttomic = new AtomicReference<>();
        Map<String, Boolean> shown = new HashMap<>();
        for (int j = 0; j < 100; j++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    AtomicInteger permits = new AtomicInteger();
                    for (int i = 0; i < 1_000_000; i++) {
                        try {
                            String currentSec = (DateTime.now().getMillis() / 1000) + "";

                            int val = counter.incrementAndGet();
                            totalCount.incrementAndGet();
                            Data data = Data.builder().id("id_" + val).data("data_" + val).build();
                            if (permits.decrementAndGet() <= 0) {
                                rateLimiterFactory.get(rateLimiterName).ifPresent(rateLimiter -> {
                                    rateLimiter.acquire(1);
                                    permits.set(1);
                                });
                            }
                            table.putItem(Item.fromJSON(JsonUtils.asJson(data)));
                            if (counter.incrementAndGet() % 1000 == 0) {
                                System.out.println("Write done - " + val);
                            }
                            // Thread.sleep(ThreadLocalRandom.current().nextInt(00, 300));
                            ApplicationContext.getInstance(IMetrics.class).inc("ddb_write_testing");

                            metricRegistry.counter("ddb").inc();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                long start = metricRegistry.counter("ddb").getCount();
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    long newCount = metricRegistry.counter("ddb").getCount();
                    System.out.println("Write per second = " + (newCount - start));
                    start = newCount;
                }
            }
        }).start();

        Thread.sleep(100000);
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
