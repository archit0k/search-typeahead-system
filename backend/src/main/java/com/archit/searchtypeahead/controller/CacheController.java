package com.archit.searchtypeahead.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.archit.searchtypeahead.dto.CacheDebugResponse;
import com.archit.searchtypeahead.service.CacheDebugService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CacheController {

    private final CacheDebugService cacheDebugService;

    @GetMapping("/cache/debug")
    public CacheDebugResponse debug(
            @RequestParam(required = false, defaultValue = "") String prefix
    ) {
        return cacheDebugService.debug(prefix);
    }
}
