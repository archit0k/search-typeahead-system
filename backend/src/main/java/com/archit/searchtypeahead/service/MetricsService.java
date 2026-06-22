package com.archit.searchtypeahead.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.archit.searchtypeahead.dto.LatencyStatsDto;
import com.archit.searchtypeahead.dto.MetricsSummaryResponse;

@Service
public class MetricsService {

    private static final int MAX_LATENCY_SAMPLES = 2_000;

    private final Map<String, ConcurrentLinkedDeque<Long>> latencySamples =
            new ConcurrentHashMap<>();

    private final AtomicLong totalSearchRequests = new AtomicLong();
    private final AtomicLong totalSuggestionRequests = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong dbReadCount = new AtomicLong();
    private final AtomicLong dbBatchWriteCount = new AtomicLong();
    private final AtomicLong flushedRows = new AtomicLong();
    private final AtomicLong lastFlushRows = new AtomicLong();

    public void recordSuggestionRequest() {
        totalSuggestionRequests.incrementAndGet();
    }

    public void recordSearchRequest() {
        totalSearchRequests.incrementAndGet();
    }

    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    public void recordDbRead() {
        dbReadCount.incrementAndGet();
    }

    public void recordBatchWrite(long rowsWritten) {
        dbBatchWriteCount.incrementAndGet();
        flushedRows.addAndGet(rowsWritten);
        lastFlushRows.set(rowsWritten);
    }

    public void recordLatency(String metricName, long durationMs) {

        ConcurrentLinkedDeque<Long> samples = latencySamples.computeIfAbsent(
                metricName,
                ignored -> new ConcurrentLinkedDeque<>()
        );

        samples.addLast(durationMs);

        while (samples.size() > MAX_LATENCY_SAMPLES) {
            samples.pollFirst();
        }
    }

    public MetricsSummaryResponse summary() {

        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long totalCacheLookups = hits + misses;
        double cacheHitRate = totalCacheLookups == 0
                ? 0
                : (hits * 100.0) / totalCacheLookups;

        Map<String, LatencyStatsDto> latencySummary = latencySamples.entrySet()
                .stream()
                .collect(
                        ConcurrentHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), buildLatencyStats(entry.getValue())),
                        ConcurrentHashMap::putAll
                );

        return MetricsSummaryResponse.builder()
                .totalSearchRequests(totalSearchRequests.get())
                .totalSuggestionRequests(totalSuggestionRequests.get())
                .cacheHits(hits)
                .cacheMisses(misses)
                .cacheHitRate(cacheHitRate)
                .dbReadCount(dbReadCount.get())
                .dbBatchWriteCount(dbBatchWriteCount.get())
                .flushedRows(flushedRows.get())
                .lastFlushRows(lastFlushRows.get())
                .latency(latencySummary)
                .build();
    }

    private LatencyStatsDto buildLatencyStats(
            ConcurrentLinkedDeque<Long> samples
    ) {

        if (samples.isEmpty()) {
            return LatencyStatsDto.builder()
                    .averageMs(0)
                    .p95Ms(0)
                    .sampleCount(0)
                    .build();
        }

        List<Long> snapshot = new ArrayList<>(samples);
        snapshot.sort(Long::compareTo);

        double average = snapshot.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        int p95Index = (int) Math.ceil(snapshot.size() * 0.95) - 1;
        long p95 = snapshot.get(Math.max(p95Index, 0));

        return LatencyStatsDto.builder()
                .averageMs(roundToTwoDecimals(average))
                .p95Ms(roundToTwoDecimals(p95))
                .sampleCount(snapshot.size())
                .build();
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
