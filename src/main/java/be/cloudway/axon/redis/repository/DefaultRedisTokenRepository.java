package be.cloudway.axon.redis.repository;

import be.cloudway.axon.redis.RedisTokenEntry;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisNoScriptException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static be.cloudway.axon.redis.repository.DefaultRedisTokenScripts.FETCH_TOKEN_SCRIPT;
import static be.cloudway.axon.redis.repository.DefaultRedisTokenScripts.FETCH_TOKEN_SHA1;
import static be.cloudway.axon.redis.repository.DefaultRedisTokenScripts.RELEASE_TOKEN_SCRIPT;
import static be.cloudway.axon.redis.repository.DefaultRedisTokenScripts.RELEASE_TOKEN_SHA1;
import static be.cloudway.axon.redis.repository.DefaultRedisTokenScripts.STORE_TOKEN_SCRIPT;
import static be.cloudway.axon.redis.repository.DefaultRedisTokenScripts.STORE_TOKEN_SHA1;

/**
 * Default implementation of the RedisTokenRepository. Depends on a JedisPool.
 *
 * @author Michael Willemse
 */
public class DefaultRedisTokenRepository implements RedisTokenRepository {

    private final JedisPool jedisPool;

    private static final Long TRUE = 1L;

    /**
     * Initialize the RedisTokenRepository with a {@code jedisPool}.
     * @param jedisPool Configured JedisPool instance
     */
    public DefaultRedisTokenRepository(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public RedisTokenEntry storeTokenEntry(RedisTokenEntry tokenEntry, Instant expirationFromTimestamp) {
        String redisOutput = (String) tryEvalSha1(STORE_TOKEN_SCRIPT, STORE_TOKEN_SHA1,
                getHashKeyList(tokenEntry.getProcessorName(), tokenEntry.getSegment()),
                Arrays.asList(
                        tokenEntry.getProcessorName(),
                        Integer.toString(tokenEntry.getSegment()),
                        tokenEntry.getOwner(),
                        Long.toString(tokenEntry.timestamp().toEpochMilli()),
                        Long.toString(expirationFromTimestamp.toEpochMilli()),
                        Base64.getEncoder().encodeToString(tokenEntry.getSerializedToken().getData()),
                        tokenEntry.getSerializedToken().getType().getName()));

        return mapRedisTokenEntry(redisOutput);
    }

    @Override
    public RedisTokenEntry fetchTokenEntry(String processorName, int segment, String owner, Instant currentTimestamp, Instant expirationFromTimestamp) {

        String redisOutput = (String) tryEvalSha1(FETCH_TOKEN_SCRIPT, FETCH_TOKEN_SHA1,
                getHashKeyList(processorName, segment),
                Arrays.asList(
                        processorName,
                        Integer.toString(segment),
                        owner,
                        Long.toString(currentTimestamp.toEpochMilli()),
                        Long.toString(expirationFromTimestamp.toEpochMilli())));

        return mapRedisTokenEntry(redisOutput);
    }

    @Override
    public boolean releaseClaim(String processorName, int segment, String owner) {
        Long value = (Long) tryEvalSha1(RELEASE_TOKEN_SCRIPT, RELEASE_TOKEN_SHA1,
                getHashKeyList(processorName, segment),
                Arrays.asList(
                        processorName,
                        Integer.toString(segment), owner));

        return Objects.equals(value, TRUE);
    }

    /**
     * Utility method returning a singleton list containing a String with a colon separated processorName and segment.
     *
     * @param processorName The name of the process for which to store the token
     * @param segment       The index of the segment for which to store the token
     * @return              Singleton list containing a String with a colon separated processorName and segment
     */
    private List<String> getHashKeyList(String processorName, int segment) {
        return Collections.singletonList(processorName + ":" + segment);
    }

    private Object tryEvalSha1(String script, String sha1, List<String> keys, List<String> args) {
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                return jedis.evalsha(sha1, keys, args);
            } catch (JedisNoScriptException e) {
                return jedis.eval(script, keys, args);
            }
        }
    }

    private RedisTokenEntry mapRedisTokenEntry(String redisOutput) {
        if (redisOutput != null) {
            JSONObject tokenEntry = new JSONObject(redisOutput);

            String tokenEntryProcessorName = null;
            int tokenEntrySegment = 0;
            String tokenEntryOwner = null;
            String tokenEntryTimestamp = null;
            byte[] tokenEntryToken = null;
            String tokenEntryTokenType = null;

            if (tokenEntry.has("processorName")) {
                tokenEntryProcessorName = tokenEntry.getString("processorName");
            }
            if (tokenEntry.has("segment")) {
                tokenEntrySegment = Integer.parseInt(tokenEntry.getString("segment"));
            }
            if (tokenEntry.has("owner")) {
                tokenEntryOwner = tokenEntry.getString("owner");
            }
            if (tokenEntry.has("timestamp")) {
                tokenEntryTimestamp = Instant.ofEpochMilli(Long.parseLong(tokenEntry.getString("timestamp"))).toString();
            }
            if (tokenEntry.has("token")) {
                tokenEntryToken = Base64.getDecoder().decode(tokenEntry.getString("token"));
            }
            if (tokenEntry.has("tokenType")) {
                tokenEntryTokenType = tokenEntry.getString("tokenType");
            }

            return new RedisTokenEntry(tokenEntryToken, tokenEntryTokenType, tokenEntryTimestamp,
                    tokenEntryOwner, tokenEntryProcessorName, tokenEntrySegment);
        } else {
            return null;
        }
    }
}
