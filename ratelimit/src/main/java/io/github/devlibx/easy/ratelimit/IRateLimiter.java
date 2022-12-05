package io.github.devlibx.easy.ratelimit;


import java.util.concurrent.TimeUnit;

public interface IRateLimiter {

    /**
     * Start rate limiter
     */
    void start();

    /**
     * Stop rate limiter
     */
    void stop();

    /**
     * Initializes RateLimiter's state and stores config to Redis server.
     * Params:
     * mode – - rate mode rate – - rate rateInterval – - rate time interval rateIntervalUnit – - rate time interval unit
     * Returns:
     * true if rate was set and false otherwise
     */
    boolean trySetRate(long rate);

    /**
     * Acquires a permit from this RateLimiter, blocking until one is available.
     * Acquires a permit, if one is available and returns immediately, reducing the number of available permits by one.
     */
    void acquire();

    /**
     * Acquires a specified <code>permits</code> from this RateLimiter,
     * blocking until one is available.
     *
     * <p>Acquires the given number of permits, if they are available
     * and returns immediately, reducing the number of available permits
     * by the given amount.
     *
     * @param permits the number of permits to acquire
     */
    void acquire(long permits);

    /**
     * No op implementation
     */
    class NoOpRateLimiter implements IRateLimiter {

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public boolean trySetRate(long rate) {
            return false;
        }

        @Override
        public void acquire() {
        }

        @Override
        public void acquire(long permits) {
        }
    }
}


