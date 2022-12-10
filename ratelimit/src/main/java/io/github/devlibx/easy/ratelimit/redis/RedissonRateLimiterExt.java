package io.github.devlibx.easy.ratelimit.redis;

import org.redisson.RedissonRateLimiter;
import org.redisson.api.RFuture;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.command.CommandAsyncExecutor;

import java.util.Collections;

/**
 * This class is extended to provide support to allow changign the rate limit in the rate limiter
 */
public class RedissonRateLimiterExt extends RedissonRateLimiter {

    public RedissonRateLimiterExt(CommandAsyncExecutor commandExecutor, String name) {
        super(commandExecutor, name);
    }

    @Override
    public RFuture<Boolean> trySetRateAsync(RateType type, long rate, long rateInterval, RateIntervalUnit unit) {
        // What is diff compared to revision impl
        // Redisson uses "hsetnx" which will not allow you to update the rate limit
        // I have changed it to use "hset" instead
        return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
                "redis.call('hset', KEYS[1], 'rate', ARGV[1]);"
                        + "redis.call('hset', KEYS[1], 'interval', ARGV[2]);"
                        + " redis.call('hset', KEYS[1], 'type', ARGV[3]);"
                        + " return 1;",
                Collections.singletonList(getName()), rate, unit.toMillis(rateInterval), type.ordinal());
    }
}
