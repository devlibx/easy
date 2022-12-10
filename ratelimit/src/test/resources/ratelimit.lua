local enableDebugLogging = true

-- Algo will run between lowest value to current value
local currentTimeParam = ARGV[1];
local lowestTimeParam = ARGV[2];
local currentTimeParamString = currentTimeParam .. "";
local lowestTimeParamString = lowestTimeParam .. "";

-- What is per second rate, how many permits are required, what is the name of this sorted set, what is the TTL value
local rateParam = ARGV[3];
local permits = ARGV[4];
local zset = ARGV[5];
local ttlValue = ARGV[6];

-- We have 2 data structure:
-- 1 - a sorted set of last N seconds (we make sure we only keep last N seconds keys here)
-- 2 - for each time time seconds a rate limit counter

-- This is the redis key to get current time key
local redisCurrentTimeKey = currentTimeParamString;

-- This is the value to return
local value = -1

local debug = ''
if enableDebugLogging then
    debug = '[Set Name: ' .. zset .. ' Current Time: ' .. currentTimeParam .. ' currentTimeRedisKey:' .. redisCurrentTimeKey .. ']'
end

-- If key does not exist then set the value to rate
if redis.call('GETEX', redisCurrentTimeKey) == false then

    -- We did not have they key, set it with rate value and TTL
    redis.call('SET', redisCurrentTimeKey, rateParam, 'EX', ttlValue);

    -- Add current value to the sorted list
    redis.call('ZADD', zset, currentTimeParam, currentTimeParamString);

    -- Make sure we flush old keys (to free up any old value)
    redis.call('ZREMRANGEBYSCORE', zset, 0, lowestTimeParam);

    if enableDebugLogging then
        debug = debug ..
                ' [key did not existed - create new key with ttl:' .. ttlValue ..
                ' Sorted key cleared:' .. 0 .. '-' .. lowestTimeParam
    end
end

-- Decrement by requested permits
value = redis.call("DECRBY", redisCurrentTimeKey, permits)

-- If we already consumed all limits, then try to get it from old tokesn
if value < 0 then

    -- Get all the keys from last N sec
    local sortedSet = redis.call('ZRANGEBYSCORE', zset, '-inf', '+inf');

    for i, v in pairs(sortedSet) do

        value = redis.call("DECRBY", v, permits)

        if value > 0 then
            debug = debug .. ' [found value from key ' .. v .. ' with value ' .. value
            break
        else
            redis.call('DEL', v)
        end

    end
end

if enableDebugLogging then
    if value >= 0 then
        return debug .. ' Final value - [' .. value .. ']'
    else
        value = -1
        return debug .. ' Final value suppress to 0 - [' .. value .. ']'
    end
else
    if value >= 0 then
        return value
    else
        return '..' - 1
    end
end