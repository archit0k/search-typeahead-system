package com.archit.searchtypeahead.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.archit.searchtypeahead.dto.SuggestionDto;
import com.archit.searchtypeahead.entity.SearchTerm;
import com.archit.searchtypeahead.repository.SearchTermRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrendingService {

    private final SearchTermRepository searchTermRepository;
    private final SearchWriteBufferService searchWriteBufferService;
    private final RecentSearchWindowService recentSearchWindowService;
    private final RankingService rankingService;

    public List<SuggestionDto> getTrendingSearches() {

        List<SearchTerm> topTerms = searchTermRepository.findAllByOrderByFrequencyDesc(
                PageRequest.of(0, 100)
        );

        Map<String, Long> pendingSnapshot = searchWriteBufferService.pendingSnapshot();
        Map<String, SearchTerm> mergedTerms = new HashMap<>();
        topTerms.forEach(term -> mergedTerms.put(term.getTerm(), term));

        if (!pendingSnapshot.isEmpty()) {
            searchTermRepository.findByTermIn(pendingSnapshot.keySet())
                    .forEach(term -> mergedTerms.put(term.getTerm(), term));

            pendingSnapshot.forEach(
                    (term, increment) -> mergedTerms.putIfAbsent(
                            term,
                            SearchTerm.builder()
                                    .term(term)
                                    .frequency(0L)
                                    .build()
                    )
            );
        }

        List<SuggestionDto> trending = new ArrayList<>();

        for (SearchTerm searchTerm : mergedTerms.values()) {
            long pendingIncrement =
                    searchWriteBufferService.pendingIncrementForTerm(searchTerm.getTerm());
            long frequency = searchTerm.getFrequency() + pendingIncrement;
            long recentCount =
                    recentSearchWindowService.getRecentCount(searchTerm.getTerm());

            trending.add(
                    SuggestionDto.builder()
                            .term(searchTerm.getTerm())
                            .frequency(frequency)
                            .recentSearchCount(recentCount)
                            .score(rankingService.score(frequency, recentCount))
                            .build()
            );
        }

        return trending.stream()
                .sorted(
                        Comparator.comparingDouble(SuggestionDto::score)
                                .reversed()
                                .thenComparing(Comparator.comparingLong(SuggestionDto::frequency).reversed())
                                .thenComparing(SuggestionDto::term)
                )
                .limit(10)
                .toList();
    }
}
