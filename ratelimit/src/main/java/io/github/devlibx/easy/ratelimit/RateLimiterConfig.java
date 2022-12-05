package io.github.devlibx.easy.ratelimit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.redisson.config.Config;

import java.util.Objects;
import java.util.concurrent.TimeUnit;


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

    @Builder.Default
    private RateType rateType = RateType.OVERALL;

    @Builder.Default
    private int rate = 100;

    @Builder.Default
    private int rateInterval = 1;

    @Builder.Default
    private TimeUnit rateIntervalUnit = TimeUnit.SECONDS;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Redis {

        @Builder.Default
        private String host = "localhost";

        @Builder.Default
        private int port = 6379;

        @Builder.Default
        private String strategy = "single-server";

        @JsonIgnore
        public Config getRedissonConfig() {
            Config config = new Config();
            if (Objects.equals(strategy, "single-server")) {
                String redisAddress = String.format("redis://%s:%s", host, port);
                config.useSingleServer().setAddress(redisAddress);
            } else if (Objects.equals(strategy, "localhost")) {
                String redisAddress = String.format("redis://%s:%s", host, port);
                config.useSingleServer().setAddress(redisAddress);
            }
            return config;
        }

        @JsonIgnore
        public String uniqueKey() {
            return host + "-" + port;
        }
    }
}
