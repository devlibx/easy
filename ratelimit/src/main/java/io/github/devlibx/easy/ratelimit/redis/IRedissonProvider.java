package io.github.devlibx.easy.ratelimit.redis;

import io.github.devlibx.easy.ratelimit.RateLimiterConfig.Redis;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public interface IRedissonProvider {
    /**
     * Provide a revision client
     */
    RedissonClient get(Redis redis);

    /**
     * Default revision client provider
     */
    class DefaultRedissonProvider implements IRedissonProvider {
        private final Lock lock = new ReentrantLock();
        private final Map<String, RedissonClient> redissonClientMap = new HashMap<>();

        @Override
        public RedissonClient get(Redis redis) {
            lock.lock();
            try {
                return redissonClientMap.computeIfAbsent(redis.uniqueKey(), key -> {
                    org.redisson.config.Config redissonConfig = redis.getRedissonConfig();
                    if (Objects.equals(redis.getVersion(), "v1")) {
                        return Redisson.create(redissonConfig);
                    } else if (Objects.equals(redis.getVersion(), "v2")) {
                        return RedissonExt.create(redissonConfig);
                    } else if (Objects.equals(redis.getVersion(), "v3")) {
                        return RedissonExt.create(redissonConfig);
                    } else {
                        return Redisson.create(redissonConfig);
                    }
                });
            } finally {
                lock.unlock();
            }
        }
    }
}
