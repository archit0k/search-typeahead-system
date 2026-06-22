package com.archit.searchtypeahead.dto;

import java.util.Map;

import lombok.Builder;

@Builder
public record MetricsSummaryResponse(
        long totalSearchRequests,
        long totalSuggestionRequests,
        long cacheHits,
        long cacheMisses,
        double cacheHitRate,
        long dbReadCount,
        long dbBatchWriteCount,
        long flushedRows,
        long lastFlushRows,
        Map<String, LatencyStatsDto> latency
) {
}
