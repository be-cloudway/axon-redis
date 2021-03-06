package be.cloudway.axon.redis;

import be.cloudway.axon.redis.repository.RedisTokenRepository;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.axonframework.eventhandling.tokenstore.UnableToClaimTokenException;
import org.axonframework.eventsourcing.eventstore.TrackingToken;
import org.axonframework.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.TemporalAmount;

import static java.lang.String.format;

/**
 * Redis implementation of the TokenStore. With assistance of the DefaultRedisTokenRepository fetch, store and
 * release token functionality is implemented within a single round trip by Redis Lua scripts.
 *
 * @author Michael Willemse
 */
public class RedisTokenStore implements TokenStore {

    private final RedisTokenRepository redisTokenRepository;
    private final Serializer serializer;
    private final TemporalAmount claimTimeout;
    private final String nodeId;
    private final Clock clock = Clock.systemUTC();

    private static final Logger logger = LoggerFactory.getLogger(RedisTokenStore.class);

    /**
     * Initialize the RedisTokenStore with a RedisTokenRepository and a Serializer. The claim timeout configured
     * at 10 seconds and the name of the node is set to a representational name of the Java VM.
     *
     * @param redisTokenRepository  Lower level Redis repository
     * @param serializer            The serializer to serialize tokens with
     */
    public RedisTokenStore(RedisTokenRepository redisTokenRepository, Serializer serializer) {
        this(redisTokenRepository, serializer, Duration.ofSeconds(10), ManagementFactory.getRuntimeMXBean().getName());
    }

    /**
     * Initialize the RedisTokenStore with a RedisTokenRepository, a Serializer. The given {@code claimTimeout} is used to 'steal' any claim
     * that has not been updated since that amount of time.
     *
     * @param redisTokenRepository  Lower level Redis repository
     * @param serializer            The serializer to serialize tokens with
     * @param claimTimeout          The timeout after which this process will force a claim
     * @param nodeId                The identifier to identify ownership of t he tokens
     */
    public RedisTokenStore(RedisTokenRepository redisTokenRepository, Serializer serializer,
                           TemporalAmount claimTimeout, String nodeId) {
        this.redisTokenRepository = redisTokenRepository;
        this.serializer = serializer;
        this.claimTimeout = claimTimeout;
        this.nodeId = nodeId;
    }

    @Override
    public void storeToken(TrackingToken token, String processorName, int segment) throws UnableToClaimTokenException {
        RedisTokenEntry redisTokenEntry = new RedisTokenEntry(token, serializer, processorName, segment);
        redisTokenEntry.claim(nodeId, claimTimeout);
        RedisTokenEntry redisTokenEntryResponse = redisTokenRepository.storeTokenEntry(redisTokenEntry,
                redisTokenEntry.timestamp().minus(claimTimeout));

        if(redisTokenEntryResponse != null) {
            if(!nodeId.equals(redisTokenEntryResponse.getOwner())) {
                throw new UnableToClaimTokenException(format("Unable to claim token '%s[%s]'. It is owned by '%s'",
                        processorName, segment, redisTokenEntryResponse.getOwner()));
            }
        } else {
            throw new UnableToClaimTokenException(format("Unable to claim token '%s[%s]'.", processorName, segment));
        }
    }

    @Override
    public TrackingToken fetchToken(String processorName, int segment) throws UnableToClaimTokenException {
        RedisTokenEntry redisTokenEntry = redisTokenRepository.fetchTokenEntry(processorName, segment, nodeId,
                clock.instant(), clock.instant().minus(claimTimeout));

        if(redisTokenEntry != null) {
            if(!nodeId.equals(redisTokenEntry.getOwner())) {
                throw new UnableToClaimTokenException(format("Unable to claim token '%s[%s]'. It is owned by '%s'",
                        processorName, segment, redisTokenEntry.getOwner()));
            }
            return redisTokenEntry.getToken(serializer);
        } else {
            throw new UnableToClaimTokenException(format("Unable to claim token '%s[%s]'.", processorName, segment));
        }
    }

    @Override
    public void releaseClaim(String processorName, int segment) {
        boolean result = redisTokenRepository.releaseClaim(processorName, segment, nodeId);
        if(!result) {
            logger.warn("Releasing claim of token {}/{} failed. It was not owned by {}", processorName, segment,
                    nodeId);
        }
    }
}
