package com.archit.searchtypeahead.service;

import org.springframework.stereotype.Service;

import com.archit.searchtypeahead.cache.DistributedCacheService;
import com.archit.searchtypeahead.dto.CacheDebugResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CacheDebugService {

    private final DistributedCacheService distributedCacheService;

    public CacheDebugResponse debug(String prefix) {
        return distributedCacheService.debugPrefix(
                prefix == null ? "" : prefix.trim().toLowerCase()
        );
    }
}
