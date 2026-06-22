package com.archit.searchtypeahead.service;

import org.springframework.stereotype.Service;

import com.archit.searchtypeahead.dto.SearchResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchWriteBufferService searchWriteBufferService;
    private final MetricsService metricsService;

    public SearchResponse recordSearch(String term) {

        String normalizedTerm = normalize(term);

        searchWriteBufferService.bufferSearch(normalizedTerm);
        metricsService.recordSearchRequest();

        return SearchResponse.builder()
                .message("Searched")
                .term(normalizedTerm)
                .build();
    }

    private String normalize(String term) {
        return term == null
                ? ""
                : term.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
