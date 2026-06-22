package com.archit.searchtypeahead.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "app.redis")
@Getter
@Setter
public class RedisClusterProperties {

    private int virtualNodes = 128;
    private Duration suggestionTtl = Duration.ofMinutes(10);
    private List<RedisNodeProperties> nodes = new ArrayList<>();

    @Getter
    @Setter
    public static class RedisNodeProperties {

        private String id;
        private String host;
        private int port;
    }
}
