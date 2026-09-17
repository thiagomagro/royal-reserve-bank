package com.royal.reserve.bank.account.api.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.royal.reserve.bank.account.api.dto.AccountResponse;
import com.royal.reserve.bank.account.api.serializer.CustomBigDecimalRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

/**
 * Configuration class for Redis.
 * The connection factory is auto-configured by Spring Boot from the {@code spring.data.redis.*} properties.
 */
@Configuration
public class RedisConfig {

    /**
     * Creates a Redis template for storing and retrieving account responses.
     *
     * @param connectionFactory The Redis connection factory.
     * @return The Redis template for account responses.
     */
    @Bean
    public RedisTemplate<String, List<AccountResponse>> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, List<AccountResponse>> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(createValueSerializer());

        return redisTemplate;
    }

    /**
     * Creates a custom serializer for BigDecimal values.
     *
     * @return The serializer for BigDecimal values.
     */
    private RedisSerializer<Object> createValueSerializer() {
        RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());
        return new CustomBigDecimalRedisSerializer(jsonSerializer);
    }

    /**
     * Creates and configures an instance of ObjectMapper for JSON serialization and deserialization.
     *
     * @return The configured ObjectMapper.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return objectMapper;
    }
}

