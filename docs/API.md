# API Documentation

## `GET /health`

Returns a simple backend health message.

Example response:

```json
"Search Typeahead Backend Running"
```

## `GET /suggest?q=<prefix>`

Returns up to 10 suggestions that start with the given prefix.

Behavior:

- case-insensitive prefix matching
- top 10 results
- recency-aware ranking
- cached per prefix

Example:

```json
[
  {
    "term": "service mesh",
    "frequency": 4,
    "recentSearchCount": 3,
    "score": 18.72
  }
]
```

## `POST /search`

Records a search submission and returns the required dummy response.

Request:

```json
{
  "term": "service mesh"
}
```

Response:

```json
{
  "message": "Searched",
  "term": "service mesh"
}
```

## `GET /trending`

Returns the top trending searches using historical popularity plus recent activity.

## `GET /cache/debug?prefix=<prefix>`

Returns cache-routing and cache-state information.

Example:

```json
{
  "prefix": "se",
  "node": "redis-2",
  "host": "localhost",
  "port": 6380,
  "cacheKey": "suggest:se",
  "cacheHit": true,
  "ttlSeconds": 591
}
```

## `GET /metrics/summary`

Returns:

- total search requests
- total suggestion requests
- cache hits and misses
- cache hit rate
- DB read count
- batch write count
- flushed row count
- latency summaries including average and p95
