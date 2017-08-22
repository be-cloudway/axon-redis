## axon-redis [![Build Status](https://travis-ci.org/be-cloudway/axon-redis.svg?branch=master)](https://travis-ci.org/be-cloudway/axon-redis)

An [Axon Framework](https://github.com/AxonFramework/AxonFramework) Redis token store implementation.

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
