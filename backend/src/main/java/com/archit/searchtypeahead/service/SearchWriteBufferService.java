package com.archit.searchtypeahead.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.archit.searchtypeahead.cache.DistributedCacheService;
import com.archit.searchtypeahead.dto.SuggestionDto;
import com.archit.searchtypeahead.model.PendingSearchUpdate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchWriteBufferService {

    private final ConcurrentHashMap<String, PendingSearchUpdate> pendingUpdates =
            new ConcurrentHashMap<>();

    private final JdbcTemplate jdbcTemplate;
    private final DistributedCacheService distributedCacheService;
    private final RecentSearchWindowService recentSearchWindowService;
    private final MetricsService metricsService;

    @Value("${app.search.batch.flush-size:500}")
    private int flushSize;

    public void bufferSearch(String term) {
        pendingUpdates.computeIfAbsent(term, PendingSearchUpdate::new).increment();
        recentSearchWindowService.record(term);
        distributedCacheService.evictSuggestionKeysForTerm(term);

        if (pendingUpdates.size() >= flushSize) {
            flushPendingUpdates();
        }
    }

    public List<SuggestionDto> pendingSuggestionsForPrefix(String prefix) {
        return pendingUpdates.values()
                .stream()
                .filter(update -> update.getTerm().startsWith(prefix))
                .sorted(
                        Comparator.comparingLong(PendingSearchUpdate::getIncrement)
                                .reversed()
                                .thenComparing(PendingSearchUpdate::getTerm)
                )
                .map(
                        update -> SuggestionDto.builder()
                                .term(update.getTerm())
                                .frequency(update.getIncrement())
                                .recentSearchCount(recentSearchWindowService.getRecentCount(update.getTerm()))
                                .score(update.getIncrement())
                                .build()
                )
                .toList();
    }

    public long pendingIncrementForTerm(String term) {
        PendingSearchUpdate update = pendingUpdates.get(term);
        return update == null ? 0 : update.getIncrement();
    }

    public LocalDateTime pendingLastSearchedAt(String term) {
        PendingSearchUpdate update = pendingUpdates.get(term);
        return update == null ? null : update.getLastSearchedAt();
    }

    public Map<String, Long> pendingSnapshot() {
        return pendingUpdates.values()
                .stream()
                .collect(
                        java.util.HashMap::new,
                        (map, update) -> map.put(update.getTerm(), update.getIncrement()),
                        java.util.HashMap::putAll
                );
    }

    @Scheduled(fixedDelayString = "${app.search.batch.flush-interval-ms:5000}")
    @Transactional
    public void flushPendingUpdates() {

        if (pendingUpdates.isEmpty()) {
            return;
        }

        List<PendingSearchUpdate> updatesToFlush = new ArrayList<>();

        for (PendingSearchUpdate update : pendingUpdates.values()) {
            if (pendingUpdates.remove(update.getTerm(), update)) {
                updatesToFlush.add(update);
            }
        }

        if (updatesToFlush.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                """
                INSERT INTO search_terms (term, frequency, last_searched_at)
                VALUES (?, ?, ?)
                ON CONFLICT (term)
                DO UPDATE SET
                    frequency = search_terms.frequency + EXCLUDED.frequency,
                    last_searched_at = GREATEST(
                        search_terms.last_searched_at,
                        EXCLUDED.last_searched_at
                    )
                """,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(java.sql.PreparedStatement preparedStatement, int index)
                            throws java.sql.SQLException {

                        PendingSearchUpdate update = updatesToFlush.get(index);
                        preparedStatement.setString(1, update.getTerm());
                        preparedStatement.setLong(2, update.getIncrement());
                        preparedStatement.setObject(3, update.getLastSearchedAt());
                    }

                    @Override
                    public int getBatchSize() {
                        return updatesToFlush.size();
                    }
                }
        );

        metricsService.recordBatchWrite(updatesToFlush.size());

        log.info(
                "Flushed {} aggregated search updates to PostgreSQL",
                updatesToFlush.size()
        );
    }
}
