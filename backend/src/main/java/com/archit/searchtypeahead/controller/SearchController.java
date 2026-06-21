package com.archit.searchtypeahead.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.archit.searchtypeahead.entity.SearchQuery;
import com.archit.searchtypeahead.service.SearchQueryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchQueryService service;

    @PostMapping
    public SearchQuery save(
            @RequestParam String query
    ) {
        return service.save(query);
    }
}