-- Serves per-link statistics: filter by code, then range-scan by time.
-- The leading column (code) also covers lookups by code alone,
-- so the old single-column index is redundant and only slows down inserts.
CREATE INDEX idx_click_events_code_occurred_at ON tb_click_events (code, occurred_at);

DROP INDEX idx_click_events_code;
