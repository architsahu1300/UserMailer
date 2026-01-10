package com.archit.profilemail.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * =============================================================================
 * REDIS CONFIGURATION
 * =============================================================================
 * This class configures how Spring Boot connects to and uses Redis.
 * 
 * Key concepts:
 * - RedisTemplate: The main class for interacting with Redis
 * - Serializers: Convert Java objects to/from byte arrays for Redis storage
 * - Streams: Redis data structure we use for our job queue
 * 
 * Why we need custom configuration:
 * - Default serializer uses Java serialization (hard to debug, not portable)
 * - We want JSON serialization (human-readable, works across languages)
 * =============================================================================
 */
@Configuration
public class RedisConfig {

    /**
     * Stream name constant - used by both producer (API) and consumer (Worker)
     * 
     * In Redis, a Stream is like an append-only log:
     * - Messages are added with XADD command
     * - Each message gets a unique ID (timestamp-based)
     * - Consumers read with XREADGROUP command
     * - Messages stay until explicitly deleted (or trimmed)
     */
    public static final String CSV_JOBS_STREAM = "csv-jobs-stream";
    
    /**
     * Consumer group name - all workers join this group
     * 
     * Consumer groups allow:
     * - Multiple workers to share the load
     * - Each message goes to exactly ONE worker in the group
     * - Tracking which messages are processed (acknowledgment)
     * - Recovering messages from failed workers
     */
    public static final String CSV_JOBS_GROUP = "csv-workers";

    /**
     * Configure the RedisTemplate with JSON serialization
     * 
     * RedisTemplate is like JdbcTemplate but for Redis.
     * It provides methods for all Redis operations:
     * - opsForValue(): String operations (GET, SET)
     * - opsForList(): List operations (LPUSH, RPOP)
     * - opsForStream(): Stream operations (XADD, XREADGROUP)
     * - opsForHash(): Hash operations (HSET, HGET)
     * 
     * @param connectionFactory Provided by Spring Boot auto-configuration
     *                          (reads spring.data.redis.host and port from properties)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        
        // Set the connection factory (how to connect to Redis)
        template.setConnectionFactory(connectionFactory);
        
        // -------------------------------------------------------------------------
        // KEY SERIALIZER: How to convert Redis keys to bytes
        // -------------------------------------------------------------------------
        // Keys are always strings (like "csv-jobs-stream")
        // StringRedisSerializer converts String to UTF-8 bytes
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // -------------------------------------------------------------------------
        // VALUE SERIALIZER: How to convert Java objects to bytes
        // -------------------------------------------------------------------------
        // We use JSON serialization because:
        // 1. Human-readable (can inspect in Redis CLI)
        // 2. Language-agnostic (could read from Python, Node.js, etc.)
        // 3. Includes type info (knows which Java class to deserialize to)
        
        // Create ObjectMapper with type information
        // This adds "@class" field to JSON so we know the original Java type
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper);
        
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        // Initialize the template
        template.afterPropertiesSet();
        
        return template;
    }
}

