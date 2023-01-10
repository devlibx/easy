package io.github.devlibx.easy.ratelimit.redis;

import io.gitbub.devlibx.easy.helper.json.JsonUtils;
import io.github.devlibx.easy.ratelimit.RateLimiterConfig;
import lombok.Setter;
import org.joda.time.DateTime;
import org.redisson.RedissonRateLimiter;
import org.redisson.api.RFuture;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.client.codec.LongCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.client.protocol.RedisCommands;
import org.redisson.command.CommandAsyncExecutor;
import org.redisson.misc.RPromise;
import org.redisson.misc.RedissonPromise;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class is extended to provide support to allow changign the rate limit in the rate limiter
 */
public class RedissonRateLimiterExt extends RedissonRateLimiter {

    @Setter
    private RateLimiterConfig rateLimiterConfig;
    private final RedissonExt redissonExt;
    private final AtomicReference<String> currentTimestampString = new AtomicReference<>();

    public RedissonRateLimiterExt(CommandAsyncExecutor commandExecutor, String name, RedissonExt redissonExt) {
        super(commandExecutor, name);
        this.redissonExt = redissonExt;
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

    public void acquireExtV3(long permits) {

        long nowSec = DateTime.now().getMillis() / 1000;
        String prefix = rateLimiterConfig.getName() + "-" + rateLimiterConfig.getPrefix() + "-" + nowSec;
        if (rateLimiterConfig != null && rateLimiterConfig.getProperties().getBoolean("enable-acquire-optimization", false)) {
            long value = 0; //redissonExt.getAtomicLong(prefix).get();
            if (Objects.equals(currentTimestampString.get(), prefix) || redissonExt.getAtomicLong(prefix).isExists()) {
                value = redissonExt.getAtomicLong(prefix).addAndGet(-1 * permits);
                currentTimestampString.set(prefix);
            }
            if (value > 0) {
                if (rateLimiterConfig != null && rateLimiterConfig.getProperties().getBoolean("debug-acquire-optimization", false)) {
                    if (count.incrementAndGet() % rateLimiterConfig.getProperties().getInt("debug-acquire-optimization-percentage", 1) == 0) {
                        System.out.println("Got it using atomic counter " + value);
                    }
                }
                return;
            }
        }

        get(acquireAsyncExtV3(permits));
    }

    public RFuture<Void> acquireAsyncExtV3(long permits) {
        RPromise<Void> promise = new RedissonPromise<Void>();
        tryAcquireAsyncExtV3(permits, -1, null).onComplete((res, e) -> {
            if (e != null) {
                promise.tryFailure(e);
                return;
            }

            promise.trySuccess(null);
        });
        return promise;
    }

    public RFuture<Boolean> tryAcquireAsyncExtV3(long permits, long timeout, TimeUnit unit) {
        RPromise<Boolean> promise = new RedissonPromise<Boolean>();
        long timeoutInMillis = -1;
        if (timeout >= 0) {
            timeoutInMillis = unit.toMillis(timeout);
        }
        tryAcquireAsyncExtV3(permits, promise, timeoutInMillis);
        return promise;
    }

    // Only used in debug
    private final AtomicInteger count = new AtomicInteger();

    private void tryAcquireAsyncExtV3(long permits, RPromise<Boolean> promise, long timeoutInMillis) {
        long s = System.currentTimeMillis();
        RFuture<List<Object>> future = tryAcquireAsyncExtV3(RedisCommands.EVAL_LIST, permits);
        future.onComplete((_delay, e) -> {
            if (e != null) {
                promise.tryFailure(e);
                return;
            }

            // Debug
            if (rateLimiterConfig != null && rateLimiterConfig.getProperties().getBoolean("debug", false)) {
                if (count.incrementAndGet() % rateLimiterConfig.getProperties().getInt("debug-percentage", 100) == 0) {
                    System.out.println(JsonUtils.asJson(_delay));
                }
            }

            Long delay = Long.parseLong(_delay.get(1).toString());
            if (delay <= 0) {
                delay = null;
            }
            if (delay == null) {
                promise.trySuccess(true);
                return;
            }

            if (timeoutInMillis == -1) {
                commandExecutor.getConnectionManager().getGroup().schedule(() -> {
                    tryAcquireAsyncExtV3(permits, promise, timeoutInMillis);
                }, delay, TimeUnit.MILLISECONDS);
                return;
            }

            long el = System.currentTimeMillis() - s;
            long remains = timeoutInMillis - el;
            if (remains <= 0) {
                promise.trySuccess(false);
                return;
            }
            if (remains < delay) {
                commandExecutor.getConnectionManager().getGroup().schedule(() -> {
                    promise.trySuccess(false);
                }, remains, TimeUnit.MILLISECONDS);
            } else {
                long start = System.currentTimeMillis();
                commandExecutor.getConnectionManager().getGroup().schedule(() -> {
                    long elapsed = System.currentTimeMillis() - start;
                    if (remains <= elapsed) {
                        promise.trySuccess(false);
                        return;
                    }

                    tryAcquireAsyncExtV3(permits, promise, remains - elapsed);
                }, delay, TimeUnit.MILLISECONDS);
            }
        });
    }

    @SuppressWarnings("all")
    private <T> RFuture<T> tryAcquireAsyncExtV3(RedisCommand<T> command, Long value) {
        DateTime now = DateTime.now();
        return commandExecutor.evalWriteAsync(getName(), StringCodec.INSTANCE, command,
                rateLimiterConfig.getProperties().getString("script", script),
                Collections.EMPTY_LIST,
                new Long(now.getMillis() / 1000),
                new Long(now.minusSeconds(rateLimiterConfig.getProperties().getInt("buffer", 300)).getMillis() / 1000),
                rateLimiterConfig.getRate(),
                value,
                getName().replace("-", ""),
                rateLimiterConfig.getProperties().getInt("ttl", 300),
                now.getMillis(),
                getName() + "-" + rateLimiterConfig.getPrefix(),
                rateLimiterConfig != null && rateLimiterConfig.getProperties().getBoolean("debug", false)
        );
    }

    private static final String script = "local enableDebugLogging = true;\n" +
            "if ARGV[9] == 'false' then\n" +
            "    enableDebugLogging = false;\n" +
            "end\n" +
            "\n" +
            "-- Algo will run between lowest value to current value\n" +
            "local currentTimeParam = ARGV[1];\n" +
            "local lowestTimeParam = ARGV[2];\n" +
            "local currentTimeParamString = currentTimeParam .. \"\";\n" +
            "local lowestTimeParamString = lowestTimeParam .. \"\";\n" +
            "\n" +
            "-- What is per second rate, how many permits are required, what is the name of this sorted set, what is the TTL value\n" +
            "local rateParam = ARGV[3];\n" +
            "local permits = ARGV[4];\n" +
            "local zset = ARGV[8] .. '-' .. ARGV[5];\n" +
            "local ttlValue = ARGV[6];\n" +
            "local currentTime = ARGV[7];\n" +
            "local keyPrefix = ARGV[8];\n" +
            "\n" +
            "-- We have 2 data structure:\n" +
            "-- 1 - a sorted set of last N seconds (we make sure we only keep last N seconds keys here)\n" +
            "-- 2 - for each time time seconds a rate limit counter\n" +
            "\n" +
            "-- This is the redis key to get current time key\n" +
            "local redisCurrentTimeKey = keyPrefix .. '-' .. currentTimeParamString;\n" +
            "\n" +
            "-- This is the value to return\n" +
            "local value = -1\n" +
            "\n" +
            "local debug = ''\n" +
            "if enableDebugLogging  == true then\n" +
            "    debug = '[Set Name: ' .. zset .. ' Current Time: ' .. currentTimeParam .. ' currentTimeRedisKey:' .. redisCurrentTimeKey .. ']'\n" +
            "end\n" +
            "\n" +
            "-- If key does not exist then set the value to rate\n" +
            "if redis.call('GETEX', redisCurrentTimeKey) == false then\n" +
            "\n" +
            "    -- We did not have they key, set it with rate value and TTL\n" +
            "    redis.call('SET', redisCurrentTimeKey, rateParam, 'EX', ttlValue);\n" +
            "\n" +
            "    -- Add current value to the sorted list\n" +
            "    redis.call('ZADD', zset, currentTimeParam, redisCurrentTimeKey);\n" +
            "\n" +
            "    -- Make sure we flush old keys (to free up any old value)\n" +
            "    redis.call('ZREMRANGEBYSCORE', zset, 0, lowestTimeParam);\n" +
            "\n" +
            "    if enableDebugLogging  == true then\n" +
            "        debug = debug ..\n" +
            "                ' [key did not existed - create new key with ttl:' .. ttlValue ..\n" +
            "                ' Sorted key cleared:' .. 0 .. '-' .. lowestTimeParam\n" +
            "    end\n" +
            "end\n" +
            "\n" +
            "-- Decrement by requested permits\n" +
            "value = redis.call(\"DECRBY\", redisCurrentTimeKey, permits)\n" +
            "if enableDebugLogging  == true then\n" +
            "    if value > 0 then\n" +
            "        debug = debug .. \" value after decrement \" .. value\n" +
            "    else\n" +
            "        debug = debug .. \" value after decrement (-ve) \" .. value\n" +
            "    end\n" +
            "end\n" +
            "-- If we already consumed all limits, then try to get it from old tokesn\n" +
            "if value < 0 then\n" +
            "\n" +
            "    -- Get all the keys from last N sec\n" +
            "    local sortedSet = redis.call('ZRANGEBYSCORE', zset, '-inf', '+inf');\n" +
            "\n" +
            "    for i, v in pairs(sortedSet) do\n" +
            "\n" +
            "        value = redis.call(\"DECRBY\", v, permits)\n" +
            "\n" +
            "        if value > 0 then\n" +
            "            if enableDebugLogging  == true then\n" +
            "                debug = debug .. ' [found value from key ' .. v .. ' with value ' .. value\n" +
            "            end\n" +
            "            break\n" +
            "        else\n" +
            "            if v ~= redisCurrentTimeKey then\n" +
            "                redis.call('DEL', v)\n" +
            "                if enableDebugLogging  == true then\n" +
            "                    debug = debug .. ' DeleteFromZRange: ' .. v .. ','\n" +
            "                end\n" +
            "            end\n" +
            "        end\n" +
            "\n" +
            "    end\n" +
            "end\n" +
            "\n" +
            "local resultToReturn = -1\n" +
            "local debugToReturn = ''\n" +
            "local delay = 0\n" +
            "if enableDebugLogging  == true then\n" +
            "    if value >= 0 then\n" +
            "        resultToReturn = value\n" +
            "        debugToReturn = debug\n" +
            "    else\n" +
            "        resultToReturn = -1\n" +
            "        delay = ((currentTimeParam + 1) * 1000) - currentTime;\n" +
            "        if enableDebugLogging  == true then\n" +
            "            debugToReturn = debug .. ' Final value suppress to -1'\n" +
            "        end\n" +
            "    end\n" +
            "else\n" +
            "    if value >= 0 then\n" +
            "        resultToReturn = value\n" +
            "    else\n" +
            "        resultToReturn = -1\n" +
            "        delay = ((currentTimeParam + 1) * 1000) - currentTime;\n" +
            "    end\n" +
            "end\n" +
            "\n" +
            "-- Meta class\n" +
            "\n" +
            "return { resultToReturn .. '', delay .. '', debugToReturn }" +
            "";
}
