package com.archit.searchtypeahead.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.archit.searchtypeahead.cache.RedisNodeClient;

@Configuration
@EnableConfigurationProperties(RedisClusterProperties.class)
public class RedisConfiguration {

    @Bean
    public List<RedisNodeClient> redisNodeClients(
            RedisClusterProperties redisClusterProperties
    ) {
        return redisClusterProperties.getNodes()
                .stream()
                .map(this::buildRedisNodeClient)
                .toList();
    }

    private RedisNodeClient buildRedisNodeClient(
            RedisClusterProperties.RedisNodeProperties redisNodeProperties
    ) {

        RedisStandaloneConfiguration standaloneConfiguration =
                new RedisStandaloneConfiguration(
                        redisNodeProperties.getHost(),
                        redisNodeProperties.getPort()
                );

        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory(standaloneConfiguration);
        connectionFactory.afterPropertiesSet();

        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();

        return new RedisNodeClient(
                redisNodeProperties.getId(),
                redisNodeProperties.getHost(),
                redisNodeProperties.getPort(),
                redisTemplate
        );
    }
}
