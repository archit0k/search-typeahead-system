package com.archit.searchtypeahead.dto;

import lombok.Builder;

@Builder
public record CacheDebugResponse(
        String prefix,
        String node,
        String host,
        Integer port,
        String cacheKey,
        boolean cacheHit,
        Long ttlSeconds
) {
}
