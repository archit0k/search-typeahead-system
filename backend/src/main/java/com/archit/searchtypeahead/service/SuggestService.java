package com.archit.searchtypeahead.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.archit.searchtypeahead.cache.DistributedCacheService;
import com.archit.searchtypeahead.dto.SuggestionDto;
import com.archit.searchtypeahead.entity.SearchTerm;
import com.archit.searchtypeahead.repository.SearchTermRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestService {

    private static final int SUGGESTION_LIMIT = 10;
    private static final int CANDIDATE_LIMIT = 50;

    private final SearchTermRepository repository;
    private final DistributedCacheService distributedCacheService;
    private final SearchWriteBufferService searchWriteBufferService;
    private final RecentSearchWindowService recentSearchWindowService;
    private final MetricsService metricsService;
    private final RankingService rankingService;

    public List<SuggestionDto> getSuggestions(String prefix) {

        metricsService.recordSuggestionRequest();

        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }

        String normalizedPrefix = normalize(prefix);
        long startedAt = System.nanoTime();

        DistributedCacheService.CacheLookupResult cacheLookupResult =
                distributedCacheService.getSuggestions(normalizedPrefix);

        if (cacheLookupResult.cacheHit()) {
            long durationMs = elapsedMs(startedAt);
            metricsService.recordCacheHit();
            metricsService.recordLatency("cacheHitLatencyMs", durationMs);

            log.info(
                    "Cache hit for prefix '{}' on node {} in {} ms",
                    normalizedPrefix,
                    cacheLookupResult.node().id(),
                    durationMs
            );

            return cacheLookupResult.suggestions();
        }

        metricsService.recordCacheMiss();

        long dbStartedAt = System.nanoTime();
        List<SearchTerm> candidates = repository
                .findByTermStartingWithIgnoreCaseOrderByFrequencyDesc(
                        normalizedPrefix,
                        PageRequest.of(0, CANDIDATE_LIMIT)
                );
        long dbDurationMs = elapsedMs(dbStartedAt);

        metricsService.recordDbRead();
        metricsService.recordLatency("dbQueryLatencyMs", dbDurationMs);

        List<SuggestionDto> suggestions = buildRankedSuggestions(
                normalizedPrefix,
                candidates
        );

        distributedCacheService.putSuggestions(normalizedPrefix, suggestions);

        long totalDurationMs = elapsedMs(startedAt);
        metricsService.recordLatency("cacheMissLatencyMs", totalDurationMs);

        log.info(
                "Cache miss for prefix '{}' on node {}. DB query {} ms, total {} ms",
                normalizedPrefix,
                cacheLookupResult.node().id(),
                dbDurationMs,
                totalDurationMs
        );

        return suggestions;
    }

    private List<SuggestionDto> buildRankedSuggestions(
            String prefix,
            List<SearchTerm> databaseCandidates
    ) {

        Map<String, SuggestionDto> suggestionsByTerm = databaseCandidates.stream()
                .map(this::fromEntity)
                .collect(Collectors.toMap(SuggestionDto::term, suggestion -> suggestion));

        for (SuggestionDto pendingSuggestion : searchWriteBufferService.pendingSuggestionsForPrefix(prefix)) {
            suggestionsByTerm.putIfAbsent(
                    pendingSuggestion.term(),
                    SuggestionDto.builder()
                            .term(pendingSuggestion.term())
                            .frequency(0)
                            .recentSearchCount(pendingSuggestion.recentSearchCount())
                            .score(0)
                            .build()
            );
        }

        List<SuggestionDto> mergedSuggestions = new ArrayList<>();

        for (SuggestionDto suggestion : suggestionsByTerm.values()) {
            long recentCount =
                    recentSearchWindowService.getRecentCount(suggestion.term());
            long pendingIncrement =
                    searchWriteBufferService.pendingIncrementForTerm(suggestion.term());
            long frequency = suggestion.frequency() + pendingIncrement;

            mergedSuggestions.add(
                    SuggestionDto.builder()
                            .term(suggestion.term())
                            .frequency(frequency)
                            .recentSearchCount(recentCount)
                            .score(rankingService.score(frequency, recentCount))
                            .build()
            );
        }

        return mergedSuggestions.stream()
                .sorted(suggestionComparator())
                .limit(SUGGESTION_LIMIT)
                .toList();
    }

    private SuggestionDto fromEntity(SearchTerm searchTerm) {

        long recentCount = recentSearchWindowService.getRecentCount(searchTerm.getTerm());

        return SuggestionDto.builder()
                .term(searchTerm.getTerm())
                .frequency(searchTerm.getFrequency())
                .recentSearchCount(recentCount)
                .score(rankingService.score(searchTerm.getFrequency(), recentCount))
                .build();
    }

    private Comparator<SuggestionDto> suggestionComparator() {
        return Comparator
                .comparingDouble(SuggestionDto::score)
                .reversed()
                .thenComparing(Comparator.comparingLong(SuggestionDto::frequency).reversed())
                .thenComparing(SuggestionDto::term);
    }

    private String normalize(String prefix) {
        return prefix.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
