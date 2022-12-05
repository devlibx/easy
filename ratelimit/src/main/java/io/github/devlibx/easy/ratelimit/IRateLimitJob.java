package io.github.devlibx.easy.ratelimit;

public interface IRateLimitJob {
    void startRateLimitJob(RateLimiterConfig rateLimiterConfig);

    void stopRateLimitJob();
}
