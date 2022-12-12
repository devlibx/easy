local enableDebugLogging = true;
if ARGV[9] == 'false' then
    enableDebugLogging = false;
end

-- Algo will run between lowest value to current value
local currentTimeParam = ARGV[1];
local lowestTimeParam = ARGV[2];
local currentTimeParamString = currentTimeParam .. "";
local lowestTimeParamString = lowestTimeParam .. "";

-- What is per second rate, how many permits are required, what is the name of this sorted set, what is the TTL value
local rateParam = ARGV[3];
local permits = ARGV[4];
local zset = ARGV[8] .. '-' .. ARGV[5];
local ttlValue = ARGV[6];
local currentTime = ARGV[7];
local keyPrefix = ARGV[8];

-- We have 2 data structure:
-- 1 - a sorted set of last N seconds (we make sure we only keep last N seconds keys here)
-- 2 - for each time time seconds a rate limit counter

-- This is the redis key to get current time key
local redisCurrentTimeKey = keyPrefix .. '-' .. currentTimeParamString;

-- This is the value to return
local value = -1

local debug = ''
if enableDebugLogging  == true then
    debug = '[Set Name: ' .. zset .. ' Current Time: ' .. currentTimeParam .. ' currentTimeRedisKey:' .. redisCurrentTimeKey .. ']'
end

-- If key does not exist then set the value to rate
if redis.call('GETEX', redisCurrentTimeKey) == false then

    -- We did not have they key, set it with rate value and TTL
    redis.call('SET', redisCurrentTimeKey, rateParam, 'EX', ttlValue);

    -- Add current value to the sorted list
    redis.call('ZADD', zset, currentTimeParam, redisCurrentTimeKey);

    -- Make sure we flush old keys (to free up any old value)
    redis.call('ZREMRANGEBYSCORE', zset, 0, lowestTimeParam);

    if enableDebugLogging  == true then
        debug = debug ..
                ' [key did not existed - create new key with ttl:' .. ttlValue ..
                ' Sorted key cleared:' .. 0 .. '-' .. lowestTimeParam
    end
end

-- Decrement by requested permits
value = redis.call("DECRBY", redisCurrentTimeKey, permits)
if enableDebugLogging  == true then
    if value > 0 then
        debug = debug .. " value after decrement " .. value
    else
        debug = debug .. " value after decrement (-ve) " .. value
    end
end
-- If we already consumed all limits, then try to get it from old tokesn
if value < 0 then

    -- Get all the keys from last N sec
    local sortedSet = redis.call('ZRANGEBYSCORE', zset, '-inf', '+inf');

    for i, v in pairs(sortedSet) do

        value = redis.call("DECRBY", v, permits)

        if value > 0 then
            if enableDebugLogging  == true then
                debug = debug .. ' [found value from key ' .. v .. ' with value ' .. value
            end
            break
        else
            if v ~= redisCurrentTimeKey then
                redis.call('DEL', v)
                if enableDebugLogging  == true then
                    debug = debug .. ' DeleteFromZRange: ' .. v .. ','
                end
            end
        end

    end
end

local resultToReturn = -1
local debugToReturn = ''
local delay = 0
if enableDebugLogging  == true then
    if value >= 0 then
        resultToReturn = value
        debugToReturn = debug
    else
        resultToReturn = -1
        delay = ((currentTimeParam + 1) * 1000) - currentTime;
        if enableDebugLogging  == true then
            debugToReturn = debug .. ' Final value suppress to -1'
        end
    end
else
    if value >= 0 then
        resultToReturn = value
    else
        resultToReturn = -1
        delay = ((currentTimeParam + 1) * 1000) - currentTime;
    end
end

-- Meta class

return { resultToReturn .. '', delay .. '', debugToReturn }