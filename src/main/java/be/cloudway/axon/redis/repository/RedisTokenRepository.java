package be.cloudway.axon.redis.repository;

import be.cloudway.axon.redis.RedisTokenEntry;

import java.time.Instant;

/**
 * Interface declaring specification for a Redis Token Repository with support for claim expiration.
 *
 * @author Michael Willemse
 */
public interface RedisTokenRepository {

    /**
     * Stores the token entry. Create a new token if it does not exists. Supports pure functional expiration timestamp
     * comparison.
     *
     * @param tokenEntry                The token entry that should be stored
     * @param expirationFromTimestamp   The timestamp from which an existing token entry claim would be regarded as expired.
     * @return                          If the token was successfully stored, return true, otherwise false.
     */
    boolean storeTokenEntry(RedisTokenEntry tokenEntry, Instant expirationFromTimestamp);

    /**
     *
     * @param processorName             The name of the process for which to store the token
     * @param segment                   The index of the segment for which to store the token
     * @param owner                     The current nodeId
     * @param timestamp                 The timestamp to be used when storing the token entry
     * @param expirationFromTimestamp   The timestamp from which an existing token entry claim would be regarded as expired.
     * @return                          RedisTokenEntry representation of the Redis stored token entry
     */
    RedisTokenEntry fetchTokenEntry(String processorName, int segment, String owner, Instant timestamp, Instant expirationFromTimestamp);

    /**
     * Releases the claim on the token entry when the node is currently holding the claim
     *
     * @param processorName The name of the process for which to store the token
     * @param segment       The index of the segment for which to store the token
     * @param owner         The current nodeId
     * @return              If the claim was successfully removed, return true, otherwise false.
     */
    boolean releaseClaim(String processorName, int segment, String owner);
}
