package io.github.devlibx.easy.ratelimit;

public enum RateType {
    /**
     * Total rate for all RateLimiter instances
     */
    OVERALL,

    /**
     * Total rate for all RateLimiter instances working with the same Redisson instance
     */
    PER_CLIENT
}
