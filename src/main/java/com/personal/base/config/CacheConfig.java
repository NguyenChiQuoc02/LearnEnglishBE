package com.personal.base.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;

/**
 * Cache names used across the app: "provinces", "wards", "users".
 * Cleared on demand via CacheTestController for the admin cache-timing test page.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

  @Value("${cache.ttl.seconds:600}")
  private long ttlSeconds;

  /**
   * If Redis is unreachable, callers of a @Cacheable method (e.g. province/ward lookups used
   * by the registration and profile forms) should still fall back to the DB instead of getting
   * a 500 — so cache failures are logged and swallowed rather than rethrown.
   */
  @Bean
  public CacheErrorHandler cacheErrorHandler() {
    return new CacheErrorHandler() {
      @Override
      public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis cache GET failed for cache '{}', falling back to DB: {}", cache.getName(), exception.getMessage());
      }

      @Override
      public void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value) {
        log.warn("Redis cache PUT failed for cache '{}': {}", cache.getName(), exception.getMessage());
      }

      @Override
      public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis cache EVICT failed for cache '{}': {}", cache.getName(), exception.getMessage());
      }

      @Override
      public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Redis cache CLEAR failed for cache '{}': {}", cache.getName(), exception.getMessage());
      }
    };
  }

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    // Keep the default constructor so "@class" type metadata (needed to deserialize
    // cached List<SomeDto> back into the right type) stays enabled; only add LocalDate
    // support on top of it, since UserResponse.dateOfBirth is immutable with no default
    // constructor and Jackson can't reflect its way through that without a converter.
    GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
    serializer.configure(mapper -> {
      mapper.registerModule(localDateModule());
      mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    });

    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(ttlSeconds))
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

    return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .build();
  }

  private SimpleModule localDateModule() {
    SimpleModule module = new SimpleModule();
    module.addSerializer(LocalDate.class, new StdSerializer<>(LocalDate.class) {
      @Override
      public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.toString());
      }
    });
    module.addDeserializer(LocalDate.class, new StdDeserializer<>(LocalDate.class) {
      @Override
      public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return LocalDate.parse(p.getValueAsString());
      }
    });
    return module;
  }
}
