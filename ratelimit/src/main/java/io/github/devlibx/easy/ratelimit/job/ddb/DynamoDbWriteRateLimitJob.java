package io.github.devlibx.easy.ratelimit.job.ddb;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import io.github.devlibx.easy.ratelimit.IRateLimitJob;
import io.github.devlibx.easy.ratelimit.IRateLimiter;
import io.github.devlibx.easy.ratelimit.IRateLimiterFactory;
import io.github.devlibx.easy.ratelimit.RateLimiterConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class DynamoDbWriteRateLimitJob implements IRateLimitJob {
    private RateLimiterConfig rateLimiterConfig;
    private StringObjectMap config;
    private Table dynamoTable;
    private String tableName;
    private final AtomicBoolean keepRunning = new AtomicBoolean(true);

    @Override
    public void startRateLimitJob(RateLimiterConfig rateLimiterConfig) {
        try {
            internalStartRateLimitJob(rateLimiterConfig);

            new Thread(() -> {
                while (keepRunning.get()) {

                    // Wait for next run
                    try {
                        int sleep = rateLimiterConfig.getRateLimitJobConfig().getInt("refresh-time-in-sec", 5);
                        Thread.sleep(sleep * 1000L);
                    } catch (InterruptedException ignored) {
                    }

                    // Try to update the value
                    try {
                        updateRateLimit(rateLimiterConfig);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopRateLimitJob() {
        keepRunning.set(false);
    }

    public void internalStartRateLimitJob(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
        this.config = rateLimiterConfig.getRateLimitJobConfig();

        AmazonDynamoDB client;
        if (!Strings.isNullOrEmpty(config.getString("AWS_ACCESS_KEY_ID"))) {
            BasicSessionCredentials bc = new BasicSessionCredentials(
                    config.getString("AWS_ACCESS_KEY_ID"),
                    config.getString("AWS_SECRET_ACCESS_KEY"),
                    config.getString("AWS_SESSION_TOKEN")
            );
            client = AmazonDynamoDBClientBuilder
                    .standard()
                    .withCredentials(new AWSStaticCredentialsProvider(bc))
                    .withRegion(Regions.valueOf(config.getString("region", "AP_SOUTH_1")))
                    .build();
        } else {
            client = AmazonDynamoDBClientBuilder
                    .standard()
                    .withRegion(Regions.AP_SOUTH_1)
                    .build();
        }
        DynamoDB dynamoDB = new DynamoDB(client);
        tableName = config.getString("table");
        dynamoTable = dynamoDB.getTable(config.getString("table"));
    }

    private void updateRateLimit(RateLimiterConfig rateLimiterConfig) {
        ApplicationContext.getInstance(IRateLimiterFactory.class).get(rateLimiterConfig.getName()).ifPresent(rateLimiter -> {
            if (rateLimiterConfig.isEnabled()) {
                if (config.getBoolean("rate-limit-by-write", true)) {
                    setWriteLimit(rateLimiter);
                } else {
                    setReadLimit(rateLimiter);
                }
            }
        });
    }

    private void setWriteLimit(IRateLimiter rateLimiter) {
        try {
            TableDescription table_info = dynamoTable.describe();
            long value = table_info.getProvisionedThroughput().getWriteCapacityUnits();
            if (value > 0) {
                float rateLimitFactor = config.getFloat("rate-limit-factor") != null ? config.getFloat("rate-limit-factor") : 0.8f;
                long finalValue = (long) (value * rateLimitFactor);
                log.debug("set ratelimit for DDB write table={} with value={}, factor={}, rateLimitUsed={}", tableName, value, rateLimitFactor, finalValue);
                rateLimiter.trySetRate(value);
            } else {
                log.warn("(OnDemand table) will not set ratelimit for DDB write table={} with rateLimitUsed={}", tableName, rateLimiter.debug());
            }
        } catch (Exception e) {
            log.error("failed to setup write rate limiter: table={}", tableName, e);
        }
    }

    private void setReadLimit(IRateLimiter rateLimiter) {
        try {
            TableDescription table_info = dynamoTable.describe();
            long value = table_info.getProvisionedThroughput().getReadCapacityUnits();
            if (value > 0) {
                float rateLimitFactor = config.getFloat("rate-limit-factor") != null ? config.getFloat("rate-limit-factor") : 0.8f;
                long finalValue = (long) (value * rateLimitFactor);
                log.debug("set ratelimit for DDB read table={} with value={}, factor={}, rateLimitUsed={}", tableName, value, rateLimitFactor, finalValue);
                rateLimiter.trySetRate(value);
            } else {
                log.warn("(OnDemand table) will not set ratelimit for DDB read table={} with rateLimitUsed={}", tableName, rateLimiter.debug());
            }
        } catch (Exception e) {
            log.error("failed to setup read rate limiter: table={}", tableName, e);
        }
    }
}
