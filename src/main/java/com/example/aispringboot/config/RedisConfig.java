package com.example.aispringboot.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 配置：String key + JSON value 序列化，并按缓存名定义差异化 TTL。
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /** 缓存名 -> TTL。未列出的缓存使用默认 TTL。 */
    private static final Map<String, Duration> CACHE_TTLS = Map.of(
            "analytics:overview", Duration.ofMinutes(5),   // 数据看板：允许短周期延迟
            "knowledge:article", Duration.ofMinutes(30),   // 文章详情：写操作时主动淘汰
            "knowledge:category", Duration.ofHours(1),     // 分类树：极少变化
            "user:info", Duration.ofMinutes(30)            // 用户信息：JWT 过滤器每请求读取
    );
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jacksonSerializer());
        template.setHashValueSerializer(jacksonSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = baseConfig();
        Map<String, RedisCacheConfiguration> perCacheConfig = new HashMap<>();
        CACHE_TTLS.forEach((name, ttl) -> perCacheConfig.put(name, baseConfig().entryTtl(ttl)));
        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig.entryTtl(DEFAULT_TTL))
                .withInitialCacheConfigurations(perCacheConfig)
                .transactionAware()
                .build();
    }

    private RedisCacheConfiguration baseConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .disableCachingNullValues()
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(jacksonSerializer()));
    }

    private Jackson2JsonRedisSerializer<Object> jacksonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new Jackson2JsonRedisSerializer<>(mapper, Object.class);
    }
}
