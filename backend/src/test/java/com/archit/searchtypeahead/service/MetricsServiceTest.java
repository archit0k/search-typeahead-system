package com.archit.searchtypeahead.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MetricsServiceTest {

    @Test
    void computesHitRateAndLatencySummary() {

        MetricsService metricsService = new MetricsService();

        metricsService.recordSuggestionRequest();
        metricsService.recordSuggestionRequest();
        metricsService.recordCacheHit();
        metricsService.recordCacheMiss();
        metricsService.recordDbRead();
        metricsService.recordBatchWrite(7);
        metricsService.recordLatency("cacheHitLatencyMs", 3);
        metricsService.recordLatency("cacheHitLatencyMs", 5);
        metricsService.recordLatency("dbQueryLatencyMs", 12);

        var summary = metricsService.summary();

        assertEquals(2, summary.totalSuggestionRequests());
        assertEquals(50.0, summary.cacheHitRate());
        assertEquals(1, summary.dbReadCount());
        assertEquals(7, summary.flushedRows());
        assertTrue(summary.latency().containsKey("cacheHitLatencyMs"));
    }
}
