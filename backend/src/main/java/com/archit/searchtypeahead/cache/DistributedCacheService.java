package com.archit.searchtypeahead.cache;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.archit.searchtypeahead.config.RedisClusterProperties;
import com.archit.searchtypeahead.dto.CacheDebugResponse;
import com.archit.searchtypeahead.dto.SuggestionDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedCacheService {

    private static final TypeReference<List<SuggestionDto>> SUGGESTION_LIST_TYPE =
            new TypeReference<>() { };

    private final ConsistentHashRing consistentHashRing;
    private final ObjectMapper objectMapper;
    private final RedisClusterProperties redisClusterProperties;

    public CacheLookupResult getSuggestions(String prefix) {

        RedisNodeClient node = consistentHashRing.resolveNode(prefix);
        String cacheKey = suggestionKey(prefix);

        try {
            String cachedValue = node.redisTemplate()
                    .opsForValue()
                    .get(cacheKey);

            if (cachedValue == null) {
                return CacheLookupResult.miss(node, cacheKey);
            }

            List<SuggestionDto> suggestions = objectMapper.readValue(
                    cachedValue,
                    SUGGESTION_LIST_TYPE
            );

            return CacheLookupResult.hit(node, cacheKey, suggestions);
        } catch (Exception exception) {
            log.warn(
                    "Cache read failed for key {} on node {}",
                    cacheKey,
                    node.id(),
                    exception
            );
            return CacheLookupResult.miss(node, cacheKey);
        }
    }

    public void putSuggestions(String prefix, List<SuggestionDto> suggestions) {

        RedisNodeClient node = consistentHashRing.resolveNode(prefix);
        String cacheKey = suggestionKey(prefix);

        try {
            String payload = objectMapper.writeValueAsString(suggestions);
            node.redisTemplate()
                    .opsForValue()
                    .set(
                            cacheKey,
                            payload,
                            redisClusterProperties.getSuggestionTtl()
                    );
        } catch (Exception exception) {
            log.warn(
                    "Cache write failed for key {} on node {}",
                    cacheKey,
                    node.id(),
                    exception
            );
        }
    }

    public void evictSuggestionKeysForTerm(String term) {

        String normalizedTerm = term == null ? "" : term.trim().toLowerCase();

        for (int prefixLength = 1; prefixLength <= normalizedTerm.length(); prefixLength++) {
            String prefix = normalizedTerm.substring(0, prefixLength);
            RedisNodeClient node = consistentHashRing.resolveNode(prefix);

            try {
                node.redisTemplate().delete(suggestionKey(prefix));
            } catch (Exception exception) {
                log.warn(
                        "Cache eviction failed for prefix {} on node {}",
                        prefix,
                        node.id(),
                        exception
                );
            }
        }
    }

    public CacheDebugResponse debugPrefix(String prefix) {

        String normalizedPrefix = prefix == null ? "" : prefix.trim().toLowerCase();
        RedisNodeClient node = consistentHashRing.resolveNode(normalizedPrefix);
        String cacheKey = suggestionKey(normalizedPrefix);
        StringRedisTemplate redisTemplate = node.redisTemplate();

        Boolean exists = redisTemplate.hasKey(cacheKey);
        Long ttl = redisTemplate.getExpire(cacheKey);

        return CacheDebugResponse.builder()
                .prefix(normalizedPrefix)
                .node(node.id())
                .host(node.host())
                .port(node.port())
                .cacheKey(cacheKey)
                .cacheHit(Boolean.TRUE.equals(exists))
                .ttlSeconds(ttl == null ? null : ttl)
                .build();
    }

    public String suggestionKey(String prefix) {
        return "suggest:" + prefix;
    }

    public record CacheLookupResult(
            RedisNodeClient node,
            String cacheKey,
            boolean cacheHit,
            List<SuggestionDto> suggestions
    ) {

        public static CacheLookupResult hit(
                RedisNodeClient node,
                String cacheKey,
                List<SuggestionDto> suggestions
        ) {
            return new CacheLookupResult(node, cacheKey, true, suggestions);
        }

        public static CacheLookupResult miss(
                RedisNodeClient node,
                String cacheKey
        ) {
            return new CacheLookupResult(node, cacheKey, false, List.of());
        }
    }
}
