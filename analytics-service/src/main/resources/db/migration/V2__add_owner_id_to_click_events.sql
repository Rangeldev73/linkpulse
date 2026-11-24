-- Nullable on purpose: events published before this change carry no owner.
ALTER TABLE tb_click_events ADD COLUMN owner_id VARCHAR(100);
