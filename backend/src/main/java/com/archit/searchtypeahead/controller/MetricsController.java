package com.archit.searchtypeahead.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.archit.searchtypeahead.dto.MetricsSummaryResponse;
import com.archit.searchtypeahead.service.MetricsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    @GetMapping("/metrics/summary")
    public MetricsSummaryResponse summary() {
        return metricsService.summary();
    }
}
