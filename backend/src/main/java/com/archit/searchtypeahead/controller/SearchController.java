package com.archit.searchtypeahead.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.archit.searchtypeahead.dto.SearchRequest;
import com.archit.searchtypeahead.dto.SearchResponse;
import com.archit.searchtypeahead.service.SearchService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/search")
    public SearchResponse search(
            @Valid @RequestBody SearchRequest request
    ) {
        return searchService.recordSearch(request.term());
    }
}
