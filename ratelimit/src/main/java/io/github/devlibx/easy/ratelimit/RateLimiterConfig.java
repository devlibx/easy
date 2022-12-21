package io.github.devlibx.easy.ratelimit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import io.gitbub.devlibx.easy.helper.map.StringObjectMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.redisson.config.Config;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("unchecked")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RateLimiterConfig {

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private Redis redis = new Redis();

    private String name;

    private String prefix;

    @Builder.Default
    private RateType rateType = RateType.OVERALL;

    @Builder.Default
    private int rate = 100;

    @Builder.Default
    private int rateInterval = 1;

    @Builder.Default
    private TimeUnit rateIntervalUnit = TimeUnit.SECONDS;

    @Builder.Default
    private StringObjectMap rateLimitJobConfig = new StringObjectMap();

    @Builder.Default
    private StringObjectMap properties = new StringObjectMap();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Redis {

        @Builder.Default
        private String version = "v1";

        @Builder.Default
        private String host = "localhost";

        @Builder.Default
        private int port = 6379;

        private String password;

        @Builder.Default
        private String strategy = "single-server";

        @Builder.Default
        private int timeout = 3000;

        @Builder.Default
        @JsonProperty("connect_timeout")
        private int connectTimeout = 10000;

        @Builder.Default
        @JsonProperty("idle_connection_timeout")
        private int idleConnectionTimeout = 10000;

        @Builder.Default
        @JsonProperty("ping_connection_interval")
        private int pingConnectionInterval = 30000;

        @JsonIgnore
        public Config getRedissonConfig() {
            Config config = new Config();
            if (Objects.equals(strategy, "cluster-server-ssl")) {
                String redisAddress = String.format("rediss://%s:%s", host, port);
                config.useClusterServers()
                        .setTimeout(timeout)
                        .setPassword(password)
                        .setConnectTimeout(connectTimeout)
                        .setIdleConnectionTimeout(idleConnectionTimeout)
                        .setPingConnectionInterval(pingConnectionInterval)
                        .addNodeAddress(redisAddress);
            } else if (Objects.equals(strategy, "cluster-server")) {
                String redisAddress = String.format("redis://%s:%s", host, port);
                config.useClusterServers()
                        .setTimeout(timeout)
                        .setConnectTimeout(connectTimeout)
                        .setIdleConnectionTimeout(idleConnectionTimeout)
                        .setPingConnectionInterval(pingConnectionInterval)
                        .addNodeAddress(redisAddress);
            } else if (Objects.equals(strategy, "single-server")) {
                String redisAddress = String.format("redis://%s:%s", host, port);
                config.useSingleServer()
                        .setTimeout(timeout)
                        .setConnectTimeout(connectTimeout)
                        .setIdleConnectionTimeout(idleConnectionTimeout)
                        .setPingConnectionInterval(pingConnectionInterval)
                        .setAddress(redisAddress);
            } else if (Objects.equals(strategy, "localhost")) {
                String redisAddress = String.format("redis://%s:%s", host, port);
                config.useSingleServer()
                        .setTimeout(timeout)
                        .setConnectTimeout(connectTimeout)
                        .setIdleConnectionTimeout(idleConnectionTimeout)
                        .setPingConnectionInterval(pingConnectionInterval)
                        .setAddress(redisAddress);
            }
            return config;
        }

        @JsonIgnore
        public String uniqueKey() {
            return host + "-" + port;
        }
    }

    @JsonIgnore
    public Optional<IRateLimitJob> getRateLimitJob() {
        if (rateLimitJobConfig == null
                || Strings.isNullOrEmpty(rateLimitJobConfig.getString("rate-limit-class"))
                || !rateLimitJobConfig.getBoolean("enabled", true)
        ) {
            return Optional.empty();
        }

        try {
            Class<IRateLimitJob> rateLimitJobClass = (Class<IRateLimitJob>) Class.forName(rateLimitJobConfig.getString("rate-limit-class"));
            return Optional.of(rateLimitJobClass.newInstance());
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
