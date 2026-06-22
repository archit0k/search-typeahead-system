# Architecture

## Core Flow

1. The browser sends a prefix to `GET /suggest`.
2. The backend normalizes the prefix and uses `ConsistentHashRing` to choose one Redis node.
3. On a cache hit, the cached suggestion list is returned immediately.
4. On a cache miss, PostgreSQL is queried for top prefix matches.
5. Pending buffered increments and recent-window counts are merged into the ranking result.
6. The final top 10 suggestions are cached on the selected Redis node for 10 minutes.

## Search Submission Flow

1. The browser submits `POST /search`.
2. The backend normalizes the term and adds it to an in-memory aggregation map.
3. The term is also added to the recent search window.
4. All cached prefix keys for that term are evicted across the Redis ring.
5. A scheduled task flushes aggregated updates to PostgreSQL every 5 seconds.

## Recency Logic

The assignment requires recency-aware ranking, but the dataset contains very large historical frequencies, so raw counts would overpower recent activity.

The project uses:

`score = 0.7 * log10(frequency + 1) + 0.3 * recentSearchCount * 20`

Why this works:

- `log10(frequency + 1)` keeps long-term popularity meaningful without letting billion-scale counts dominate every result forever.
- `recentSearchCount` comes from a 15-minute rolling window.
- the recent boost fades naturally when the term stops being searched because older minute buckets expire.

## Batch Write Trade-off

Batching reduces database write pressure because repeated searches like:

- `search`
- `search`
- `search`

are stored in memory as one aggregated update such as `search +3`.

Trade-off:

- if the application crashes before the next flush, some buffered updates can be lost
- this is acceptable for the assignment because it clearly demonstrates write reduction with simple code

## Why This Design Fits The Assignment

- simple enough to explain in a viva
- clearly demonstrates distributed cache routing
- keeps PostgreSQL as the primary store
- adds a visible UI without introducing extra frontend build complexity
- uses practical batching and ranking logic without over-engineering
