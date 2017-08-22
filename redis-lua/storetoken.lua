--[[ script to store token

storeToken:
  if hash with key exists
    if owner = null || owner = nodeId || expirationFromTimestamp > timestamp
      set owner
      set timestamp
      set token
      set tokenType
      return true
    else
     return false
    end
  else
    set processorName
    set segment
    set owner
    set timestamp
    set token
    set tokenType
    return true
  end

]]--

local processorNameSegment = KEYS[1]
local processorName = ARGV[1]
local segment = ARGV[2]
local owner = ARGV[3]
local timestamp = ARGV[4]
local expirationFromTimestamp = ARGV[5]
local token = ARGV[6]
local tokenType = ARGV[7]

local tokenEntry = {}

local rawTokenHash = redis.call('HGETALL', processorNameSegment)

for idx = 1, #rawTokenHash, 2 do
    tokenEntry[rawTokenHash[idx]] = rawTokenHash[idx + 1]
end

if tokenEntry.processorName == processorName and tokenEntry.segment == segment then
    if tokenEntry.owner == nil or tokenEntry.owner == owner or expirationFromTimestamp > tokenEntry.timestamp then
        tokenEntry.owner = owner
        tokenEntry.timestamp = timestamp
        tokenEntry.token = token
        tokenEntry.tokenType = tokenType
        redis.call('HMSET', processorNameSegment, 'owner', tokenEntry.owner, 'timestamp', tokenEntry.timestamp, 'token', tokenEntry.token, 'tokenType', tokenEntry.tokenType)
        return true
    else
        return nil
    end
else
    tokenEntry.processorName = processorName
    tokenEntry.segment = segment
    tokenEntry.owner = owner
    tokenEntry.timestamp = timestamp
    tokenEntry.token = token
    tokenEntry.tokenType = tokenType
    redis.call('HMSET', processorNameSegment, 'processorName', tokenEntry.processorName, 'segment', tokenEntry.segment, 'owner', tokenEntry.owner, 'timestamp', tokenEntry.timestamp, 'token', tokenEntry.token, 'tokenType', tokenEntry.tokenType)
    return true
end
