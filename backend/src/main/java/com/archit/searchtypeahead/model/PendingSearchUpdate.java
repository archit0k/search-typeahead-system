package com.archit.searchtypeahead.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

public class PendingSearchUpdate {

    private final String term;
    private final AtomicLong increment = new AtomicLong();
    private volatile LocalDateTime lastSearchedAt = LocalDateTime.now();

    public PendingSearchUpdate(String term) {
        this.term = term;
    }

    public void increment() {
        increment.incrementAndGet();
        lastSearchedAt = LocalDateTime.now();
    }

    public String getTerm() {
        return term;
    }

    public long getIncrement() {
        return increment.get();
    }

    public LocalDateTime getLastSearchedAt() {
        return lastSearchedAt;
    }
}
