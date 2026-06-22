package com.archit.searchtypeahead.service;

import org.springframework.stereotype.Service;

@Service
public class RankingService {

    public double score(long frequency, long recentCount) {

        double historicalScore = 0.7 * Math.log10(frequency + 1.0);
        double recentBoost = 0.3 * recentCount * 20.0;

        return roundToTwoDecimals(historicalScore + recentBoost);
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
