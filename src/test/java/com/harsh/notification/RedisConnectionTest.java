
package com.harsh.notification;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedisConnectionTest {

    @Test
    void shouldConnectToRedis() {
        RedisConnectionFactory factory =
                new LettuceConnectionFactory("localhost", 6379);

        ((LettuceConnectionFactory) factory).afterPropertiesSet();

        try (RedisConnection connection = factory.getConnection()) {
            assertEquals("PONG", connection.ping());
        } finally {
            ((LettuceConnectionFactory) factory).destroy();
        }
    }
}
