# Performance Report

## What Was Measured

- cache hit latency
- cache miss latency
- DB query latency
- cache hit rate
- number of aggregated batch writes
- number of flushed rows

## Measurement Source

The application exposes `GET /metrics/summary`, which aggregates live counters and recent latency samples from the running backend.

## Sample Local Run

Sample observation captured on June 22, 2026 after running local requests against the live backend:

```json
{
  "totalSearchRequests": 1,
  "totalSuggestionRequests": 3,
  "cacheHits": 1,
  "cacheMisses": 2,
  "cacheHitRate": 33.333333333333336,
  "dbReadCount": 2,
  "dbBatchWriteCount": 1,
  "flushedRows": 1,
  "lastFlushRows": 1
}
```

Observed latency sample from the same run:

- cache hit latency average: about `109 ms`
- DB query latency average: about `139.5 ms`
- cold cache miss latency average: about `643 ms`

## Batch Write Evidence

The write buffer aggregates repeated searches in memory and flushes them every 5 seconds.

Example:

- input requests: `service mesh`, `service mesh`, `service mesh`
- buffered update: `service mesh +3`
- DB flush: one aggregated upsert instead of three immediate writes

This directly reduces write amplification on PostgreSQL.

## Trade-offs

- cache hits are fast after a warm prefix lookup
- cold misses are slower because the database must be queried and the result cached
- batch writes reduce database pressure but can lose a few seconds of in-memory updates if the process crashes before flushing

## Suggested Demo Sequence

1. Open the UI at `http://localhost:8080`
2. Search for a prefix like `se`
3. Call `/cache/debug?prefix=se` and show the selected Redis node
4. Repeat the same prefix search and show the cache hit
5. Submit a new search term like `service mesh` multiple times
6. Show `/metrics/summary`
7. Show `/trending` and explain the recent-boost behavior
