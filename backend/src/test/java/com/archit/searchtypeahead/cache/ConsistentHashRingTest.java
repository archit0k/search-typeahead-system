package com.archit.searchtypeahead.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.archit.searchtypeahead.config.RedisClusterProperties;

class ConsistentHashRingTest {

    @Test
    void resolvesPrefixesDeterministically() {

        RedisClusterProperties properties = new RedisClusterProperties();
        properties.setVirtualNodes(64);

        ConsistentHashRing ring = new ConsistentHashRing(
                properties,
                List.of(
                        new RedisNodeClient("redis-1", "localhost", 6379, null),
                        new RedisNodeClient("redis-2", "localhost", 6380, null),
                        new RedisNodeClient("redis-3", "localhost", 6381, null)
                )
        );

        RedisNodeClient firstLookup = ring.resolveNode("se");
        RedisNodeClient secondLookup = ring.resolveNode("se");
        RedisNodeClient thirdLookup = ring.resolveNode("java");

        assertNotNull(firstLookup);
        assertEquals(firstLookup.id(), secondLookup.id());
        assertNotNull(thirdLookup);
    }
}
