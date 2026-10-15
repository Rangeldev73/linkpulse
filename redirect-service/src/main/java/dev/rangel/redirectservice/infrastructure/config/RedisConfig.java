package dev.rangel.redirectservice.infrastructure.config;

import dev.rangel.redirectservice.application.dto.CachedLink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, CachedLink> cachedLinkRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, CachedLink> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(RedisSerializer.string());

        template.setValueSerializer(RedisSerializer.json());

        return template;
    }
}