package com.archit.searchtypeahead.cache;

import org.springframework.data.redis.core.StringRedisTemplate;

public record RedisNodeClient(
        String id,
        String host,
        int port,
        StringRedisTemplate redisTemplate
) {
}
