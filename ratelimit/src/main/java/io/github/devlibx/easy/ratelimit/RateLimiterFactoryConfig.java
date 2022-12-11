package io.github.devlibx.easy.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimiterFactoryConfig {

    @Builder.Default
    private boolean enabled = true;

    private String prefix;

    @Builder.Default
    private Map<String, RateLimiterConfig> rateLimiters = new HashMap<>();
}
