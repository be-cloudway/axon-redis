--[[ script to fetch token

  if hash with key exists
    if owner = null || owner = nodeId || expirationFromTimestamp (calculated on server -> pure function) > timestamp
      set owner
      set timestamp
      return hash
    else
     return null (throw exception in java code)
    end
  else
    set processorName
    set segment
    set owner
    set timestamp
    return hash
  end

]]--

local processorNameSegment = KEYS[1]
local processorName = ARGV[1]
local segment = ARGV[2]
local owner = ARGV[3]
local timestamp = ARGV[4]
local expirationFromTimestamp = ARGV[5]

local tokenEntry = {}

local rawTokenHash = redis.call('HGETALL', processorNameSegment)

for idx = 1, #rawTokenHash, 2 do
    tokenEntry[rawTokenHash[idx]] = rawTokenHash[idx + 1]
end

if tokenEntry.processorName == processorName and tokenEntry.segment == segment then
    if tokenEntry.owner == nil or tokenEntry.owner == owner or expirationFromTimestamp > tokenEntry.timestamp then
        tokenEntry.owner = owner
        tokenEntry.timestamp = timestamp
        redis.call('HMSET', processorNameSegment, 'owner', tokenEntry.owner, 'timestamp', tokenEntry.timestamp)
    end
else
    tokenEntry.processorName = processorName
    tokenEntry.segment = segment
    tokenEntry.owner = owner
    tokenEntry.timestamp = timestamp
    redis.call('HMSET', processorNameSegment, 'processorName', tokenEntry.processorName, 'segment', tokenEntry.segment, 'owner', tokenEntry.owner, 'timestamp', tokenEntry.timestamp)
end

return cjson.encode(tokenEntry)
