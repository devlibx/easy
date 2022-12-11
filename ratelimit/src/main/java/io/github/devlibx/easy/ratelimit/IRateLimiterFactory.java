package io.github.devlibx.easy.ratelimit;

import java.util.Optional;

public interface IRateLimiterFactory {

    /**
     * Start rate limit factory
     */
    void start();

    /**
     * Stop rate limit factory
     */
    void stop();

    /**
     * @param name get the ratelimit by name
     */
    Optional<IRateLimiter> get(String name);
}
