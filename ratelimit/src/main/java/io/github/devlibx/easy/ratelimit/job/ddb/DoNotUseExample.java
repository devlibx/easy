package io.github.devlibx.easy.ratelimit.job.ddb;

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
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.Charset;

public class DoNotUseExample {
    public static void main(String[] args) throws Exception {
        LoggingHelper.setupLogging();
        LoggingHelper.getLogger(DynamoDbWriteRateLimitJob.class).setLevel(Level.DEBUG);

        //  Setup 1 - read config from your yaml file
        String rateLimiterName = "example-config-normal";
        String testFilePath = args[0];
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
