-- ClickHouse table backing the Event entity.
-- MergeTree is the workhorse engine; ORDER BY is mandatory and here doubles as the
-- primary sort key, which makes "recent events" range scans cheap.
CREATE TABLE IF NOT EXISTS events
(
    id         UUID,
    source     LowCardinality(String),
    severity   LowCardinality(String),
    message    String,
    created_at DateTime64(6, 'UTC')
)
ENGINE = MergeTree
ORDER BY (created_at, id);
