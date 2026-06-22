package com.archit.searchtypeahead.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.archit.searchtypeahead.dto.SuggestionDto;
import com.archit.searchtypeahead.service.TrendingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TrendingController {

    private final TrendingService trendingService;

    @GetMapping("/trending")
    public List<SuggestionDto> trending() {
        return trendingService.getTrendingSearches();
    }
}
