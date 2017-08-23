package be.cloudway.axon.redis;

import be.cloudway.axon.redis.repository.DefaultRedisTokenScripts;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultRedisTokenScriptsTest {

    @Test
    public void testFetchTokenSha1() {
        assertThat(DigestUtils.sha1Hex(DefaultRedisTokenScripts.FETCH_TOKEN_SCRIPT)).isEqualTo(DefaultRedisTokenScripts.FETCH_TOKEN_SHA1);
    }

    @Test
    public void testStoreTokenSha1() {
        assertThat(DigestUtils.sha1Hex(DefaultRedisTokenScripts.STORE_TOKEN_SCRIPT)).isEqualTo(DefaultRedisTokenScripts.STORE_TOKEN_SHA1);
    }

    @Test
    public void testReleaseTokenSha1() {
        assertThat(DigestUtils.sha1Hex(DefaultRedisTokenScripts.RELEASE_TOKEN_SCRIPT)).isEqualTo(DefaultRedisTokenScripts.RELEASE_TOKEN_SHA1);
    }
}
