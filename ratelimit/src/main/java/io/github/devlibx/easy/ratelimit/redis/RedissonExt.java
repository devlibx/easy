package io.github.devlibx.easy.ratelimit.redis;

import org.redisson.Redisson;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedissonExt extends Redisson {

    protected RedissonExt(Config config) {
        super(config);
    }

    public static RedissonClient create(Config config) {
        RedissonExt redisson = new RedissonExt(config);
        if (config.isReferenceEnabled()) {
            redisson.enableRedissonReferenceSupport();
        }
        return redisson;
    }

    @Override
    public RRateLimiter getRateLimiter(String name) {
        return new RedissonRateLimiterExt(connectionManager.getCommandExecutor(), name);
    }
}
