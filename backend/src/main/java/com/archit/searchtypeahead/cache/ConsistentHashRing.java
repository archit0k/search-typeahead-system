package com.archit.searchtypeahead.cache;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import com.archit.searchtypeahead.config.RedisClusterProperties;

@Component
public class ConsistentHashRing {

    private final NavigableMap<Long, RedisNodeClient> ring = new TreeMap<>();

    public ConsistentHashRing(
            RedisClusterProperties properties,
            Collection<RedisNodeClient> redisNodeClients
    ) {

        if (redisNodeClients == null || redisNodeClients.isEmpty()) {
            throw new IllegalStateException(
                    "At least one Redis node is required for the hash ring");
        }

        for (RedisNodeClient node : redisNodeClients) {
            for (int replica = 0; replica < properties.getVirtualNodes(); replica++) {
                ring.put(
                        hash(node.id() + "#vn#" + replica),
                        node
                );
            }
        }
    }

    public RedisNodeClient resolveNode(String key) {

        if (ring.isEmpty()) {
            throw new IllegalStateException("Hash ring is empty");
        }

        long hash = hash(key);
        NavigableMap<Long, RedisNodeClient> tailMap = ring.tailMap(hash, true);

        Long selectedKey = tailMap.isEmpty()
                ? ring.firstKey()
                : tailMap.firstKey();

        return ring.get(selectedKey);
    }

    private long hash(String input) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8)
            );

            return ByteBuffer.wrap(hashBytes).getLong() & Long.MAX_VALUE;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash input", exception);
        }
    }
}
