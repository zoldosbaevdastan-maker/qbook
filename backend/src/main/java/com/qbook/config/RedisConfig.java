package com.qbook.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.url:redis://localhost:6379}")
    private String redisUrl;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Parse redis URL (supports redis:// and rediss://)
        String url = redisUrl.replace("rediss://", "").replace("redis://", "");
        String[] parts = url.split("@");

        if (parts.length == 2) {
            // Has credentials: user:password@host:port
            String[] credentials = parts[0].split(":");
            String[] hostPort = parts[1].split(":");
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(hostPort[0]);
            config.setPort(Integer.parseInt(hostPort[1]));
            if (credentials.length == 2) {
                config.setPassword(credentials[1]);
            }
            // TLS for Upstash
            if (redisUrl.startsWith("rediss://")) {
                LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
                factory.setUseSsl(true);
                return factory;
            }
            return new LettuceConnectionFactory(config);
        }

        // Simple: host:port
        String[] hostPort = url.split(":");
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(hostPort[0]);
        config.setPort(hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 6379);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
