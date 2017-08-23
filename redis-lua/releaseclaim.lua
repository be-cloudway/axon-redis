--[[ script to release claim

releaseClaim:
  if hash with key exists for which owner = nodeId
    unset owner
    return true
  else
    return false
  end

]]--

local processorNameSegment = KEYS[1]
local processorName = ARGV[1]
local segment = ARGV[2]
local owner = ARGV[3]

local tokenEntry = {}

local rawTokenHash = redis.call('HGETALL', processorNameSegment)

for idx = 1, #rawTokenHash, 2 do
    tokenEntry[rawTokenHash[idx]] = rawTokenHash[idx + 1]
end

if tokenEntry.processorName == processorName and tokenEntry.segment == segment and tokenEntry.owner == owner then
    redis.call('HDEL', processorNameSegment, 'owner')
    return true
else
    return false
end
