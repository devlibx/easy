package io.github.devlibx.easy.ratelimit.redis;

import io.gitbub.devlibx.easy.helper.ApplicationContext;
import io.gitbub.devlibx.easy.helper.Safe;
import io.gitbub.devlibx.easy.helper.metrics.IMetrics;
import io.github.devlibx.easy.ratelimit.IRateLimiter;
import io.github.devlibx.easy.ratelimit.RateLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RFuture;
import org.redisson.api.RLock;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import javax.inject.Inject;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class RedisBasedRateLimiter implements IRateLimiter {
    private final RateLimiterConfig rateLimiterConfig;
    private final IMetrics metrics;
    private RedissonClient redissonClient;
    private RRateLimiter limiter;
    private final Lock limiterLock;

    @Inject
    public RedisBasedRateLimiter(RateLimiterConfig rateLimiterConfig, IMetrics metrics) {
        this.rateLimiterConfig = rateLimiterConfig;
        this.metrics = metrics;
        this.limiterLock = new ReentrantLock();
    }

    @Override
    public void start() {
        if (rateLimiterConfig.getRedis() != null) {

            // Get revision client provider
            IRedissonProvider redissonProvider;
            try {
                redissonProvider = ApplicationContext.getInstance(IRedissonProvider.class);
            } catch (Exception e) {
                redissonProvider = new IRedissonProvider.DefaultRedissonProvider();
            }

            // Create limiter
            redissonClient = redissonProvider.get(rateLimiterConfig.getRedis());

            // Delete a existing rate limiter if exists
            limiter = redissonClient.getRateLimiter(rateLimiterConfig.getName());
            if (limiter != null) {
                try {
                    limiter.delete();
                } catch (Exception ignored) {
                }
            }
            limiter = redissonClient.getRateLimiter(rateLimiterConfig.getName());
            applyRate();
        } else {
            throw new RuntimeException("redis property must be set to use redis based rate limiter");
        }
    }


    // Apply rate to limiter
    private boolean applyRate() {
        limiterLock.lock();
        try {
            return limiter.trySetRate(
                    org.redisson.api.RateType.valueOf(rateLimiterConfig.getRateType().name()),
                    rateLimiterConfig.getRate(),
                    rateLimiterConfig.getRateInterval(),
                    convert(rateLimiterConfig.getRateIntervalUnit())
            );
        } finally {
            limiterLock.unlock();
        }
    }

    @Override
    public boolean trySetRate(long rate) {

        // update the new rate
        rateLimiterConfig.setRate((int) rate);

        // Apply change
        limiterLock.lock();
        try {
            safeDeleteRateLimit();
            return true ? applyRate() : false;
        } finally {
            limiterLock.unlock();
        }
    }


    // update the rate limit
    private void safeDeleteRateLimit() {

        // Take lock to modify this rate limiter
        RLock redisLockToModifyRateLimit;
        try {
            redisLockToModifyRateLimit = redissonClient.getLock("ratelimit-update-lock-" + rateLimiterConfig.getName());
        } catch (Exception e) {
            redisLockToModifyRateLimit = new DummyRLock();
            log.error("Failed to create a ratelimit-update-lock-{} to safely update the rate", rateLimiterConfig.getName());
        }

        try {

            // Take a lock to update the rate limiter
            redisLockToModifyRateLimit.lock(10, TimeUnit.SECONDS);

            // if config is changed then apply this rate limit again
            org.redisson.api.RateLimiterConfig configFromRedis;
            try {
                configFromRedis = limiter.getConfig();
            } catch (Exception e) {
                log.warn("trying to update the ratelimit-update-lock-{}, and this error is expected 2-3 times at boot: error={}", rateLimiterConfig.getName(), e.getMessage());
                if (e.getCause() instanceof NumberFormatException) {
                    configFromRedis = new org.redisson.api.RateLimiterConfig(RateType.OVERALL, 0L, 0L);
                } else {
                    throw e;
                }
            }

            // Delete and create this limiter again
            if (configFromRedis.getRate() != rateLimiterConfig.getRate()) {
                try {
                    limiter.delete();
                } catch (Exception e) {
                    log.error("trying to update the ratelimit-update-lock-{} - this requires us to delete the old limit. Something is wrong, we could not delete it", rateLimiterConfig.getName(), e);
                }
                limiter = redissonClient.getRateLimiter(rateLimiterConfig.getName());
            }

        } finally {
            try {
                redisLockToModifyRateLimit.unlock();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void acquire() {
        limiterLock.lock();
        int retry = 10;
        try {
            if (limiter != null) {
                while (retry-- >= 0) {
                    try {
                        limiter.acquire();
                        return;
                    } catch (Exception e) {
                        if (e.getMessage().contains("ERR user_script:1: RateLimiter is not initialized script")) {
                            try {
                                Thread.sleep(5);
                                applyRate();
                            } catch (InterruptedException ignored) {
                            }
                        } else {
                            throw e;
                        }
                    }
                }
                log.error("failed to acquire lock: {}", rateLimiterConfig.getName());
            }
        } finally {
            limiterLock.unlock();
        }
    }

    @Override
    public void stop() {
        limiterLock.lock();
        try {
            if (redissonClient != null) {
                Safe.safe(() -> {
                    redissonClient.shutdown();
                    redissonClient = null;
                    limiter = null;
                }, "failed to stop redisson client: " + rateLimiterConfig);
            }
        } finally {
            limiterLock.unlock();
        }
    }

    private RateIntervalUnit convert(TimeUnit rateIntervalUnit) {
        RateIntervalUnit unit;
        if (rateIntervalUnit == TimeUnit.DAYS) {
            unit = RateIntervalUnit.DAYS;
        } else if (rateIntervalUnit == TimeUnit.HOURS) {
            unit = RateIntervalUnit.HOURS;
        } else if (rateIntervalUnit == TimeUnit.MINUTES) {
            unit = RateIntervalUnit.MINUTES;
        } else if (rateIntervalUnit == TimeUnit.SECONDS) {
            unit = RateIntervalUnit.SECONDS;
        } else if (rateIntervalUnit == TimeUnit.MILLISECONDS) {
            unit = RateIntervalUnit.MILLISECONDS;
        } else {
            unit = RateIntervalUnit.SECONDS;
        }
        return unit;
    }

    private void sleep(long l) {
        try {
            Thread.sleep(l);
        } catch (Exception ignored) {
        }
    }

    private static class DummyRLock implements RLock {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void lockInterruptibly(long leaseTime, TimeUnit unit) throws InterruptedException {

        }

        @Override
        public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public void lock(long leaseTime, TimeUnit unit) {
            Random random = new Random();
            try {
                Thread.sleep(1000 * random.nextInt(5));
            } catch (InterruptedException ignored) {
            }
        }

        @Override
        public boolean forceUnlock() {
            return false;
        }

        @Override
        public boolean isLocked() {
            return false;
        }

        @Override
        public boolean isHeldByThread(long threadId) {
            return false;
        }

        @Override
        public boolean isHeldByCurrentThread() {
            return false;
        }

        @Override
        public int getHoldCount() {
            return 0;
        }

        @Override
        public long remainTimeToLive() {
            return 0;
        }

        @Override
        public void lock() {

        }

        @Override
        public void lockInterruptibly() throws InterruptedException {

        }

        @Override
        public boolean tryLock() {
            return false;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public void unlock() {

        }

        @Override
        public Condition newCondition() {
            return null;
        }

        @Override
        public RFuture<Boolean> forceUnlockAsync() {
            return null;
        }

        @Override
        public RFuture<Void> unlockAsync() {
            return null;
        }

        @Override
        public RFuture<Void> unlockAsync(long threadId) {
            return null;
        }

        @Override
        public RFuture<Boolean> tryLockAsync() {
            return null;
        }

        @Override
        public RFuture<Void> lockAsync() {
            return null;
        }

        @Override
        public RFuture<Void> lockAsync(long threadId) {
            return null;
        }

        @Override
        public RFuture<Void> lockAsync(long leaseTime, TimeUnit unit) {
            return null;
        }

        @Override
        public RFuture<Void> lockAsync(long leaseTime, TimeUnit unit, long threadId) {
            return null;
        }

        @Override
        public RFuture<Boolean> tryLockAsync(long threadId) {
            return null;
        }

        @Override
        public RFuture<Boolean> tryLockAsync(long waitTime, TimeUnit unit) {
            return null;
        }

        @Override
        public RFuture<Boolean> tryLockAsync(long waitTime, long leaseTime, TimeUnit unit) {
            return null;
        }

        @Override
        public RFuture<Boolean> tryLockAsync(long waitTime, long leaseTime, TimeUnit unit, long threadId) {
            return null;
        }

        @Override
        public RFuture<Integer> getHoldCountAsync() {
            return null;
        }

        @Override
        public RFuture<Boolean> isLockedAsync() {
            return null;
        }

        @Override
        public RFuture<Long> remainTimeToLiveAsync() {
            return null;
        }
    }
}