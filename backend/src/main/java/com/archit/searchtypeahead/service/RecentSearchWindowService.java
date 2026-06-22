package com.archit.searchtypeahead.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RecentSearchWindowService {

    private final Bucket[] buckets;
    private final int windowMinutes;

    public RecentSearchWindowService(
            @Value("${app.search.recent-window-minutes:15}") int windowMinutes
    ) {
        this.windowMinutes = windowMinutes;
        this.buckets = new Bucket[windowMinutes];

        Instant nowMinute = truncateToMinute(Instant.now());

        for (int index = 0; index < windowMinutes; index++) {
            buckets[index] = new Bucket(nowMinute.minus(index, ChronoUnit.MINUTES));
        }
    }

    public synchronized void record(String term) {
        rotateIfNeeded();
        buckets[0].counts.merge(term, 1L, Long::sum);
    }

    public synchronized long getRecentCount(String term) {
        rotateIfNeeded();
        long count = 0;

        for (Bucket bucket : buckets) {
            count += bucket.counts.getOrDefault(term, 0L);
        }

        return count;
    }

    public synchronized Map<String, Long> snapshot() {
        rotateIfNeeded();
        Map<String, Long> mergedCounts = new HashMap<>();

        for (Bucket bucket : buckets) {
            bucket.counts.forEach(
                    (term, count) -> mergedCounts.merge(term, count, Long::sum)
            );
        }

        return mergedCounts;
    }

    private void rotateIfNeeded() {

        Instant currentMinute = truncateToMinute(Instant.now());
        long minuteDifference = ChronoUnit.MINUTES.between(
                buckets[0].bucketMinute,
                currentMinute
        );

        if (minuteDifference <= 0) {
            return;
        }

        long rotations = Math.min(minuteDifference, windowMinutes);

        for (int rotation = 0; rotation < rotations; rotation++) {
            for (int index = buckets.length - 1; index > 0; index--) {
                buckets[index] = buckets[index - 1];
            }

            buckets[0] = new Bucket(currentMinute.minus(rotation, ChronoUnit.MINUTES));
        }
    }

    private Instant truncateToMinute(Instant instant) {
        return instant.truncatedTo(ChronoUnit.MINUTES);
    }

    private static class Bucket {

        private final Instant bucketMinute;
        private final Map<String, Long> counts = new HashMap<>();

        private Bucket(Instant bucketMinute) {
            this.bucketMinute = bucketMinute;
        }
    }
}
