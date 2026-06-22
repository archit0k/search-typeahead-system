package com.archit.searchtypeahead.dto;

import lombok.Builder;

@Builder
public record LatencyStatsDto(
        double averageMs,
        double p95Ms,
        long sampleCount
) {
}
