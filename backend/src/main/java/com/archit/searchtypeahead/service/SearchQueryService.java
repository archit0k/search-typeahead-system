package com.archit.searchtypeahead.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.archit.searchtypeahead.entity.SearchQuery;
import com.archit.searchtypeahead.repository.SearchQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchQueryService {

    private final SearchQueryRepository repository;

    public SearchQuery save(String query) {

        return repository.findByQuery(query)
                .map(existing -> {
                    existing.setSearchCount(existing.getSearchCount() + 1);
                    existing.setLastSearchedAt(LocalDateTime.now());
                    return repository.save(existing);
                })
                .orElseGet(() ->
                        repository.save(
                                SearchQuery.builder()
                                        .query(query)
                                        .searchCount(1L)
                                        .lastSearchedAt(LocalDateTime.now())
                                        .build()
                        )
                );
    }
}