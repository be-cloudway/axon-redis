## axon-redis

An [Axon Framework](https://github.com/AxonFramework/AxonFramework) Redis token repository implementation.

### Usage

```java

    @Bean
    public JedisPool jedisPool() {
        return new JedisPool(HOST, PORT);
    }

    @Bean
    public TokenStore tokenStore(JedisPool jedisPool, XStreamSerializer xStreamSerializer) {
        return new RedisTokenStore(new DefaultRedisTokenRepository(jedisPool), xStreamSerializer);
    }
```