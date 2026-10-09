TRUNCATE TABLE tb_links RESTART IDENTITY;

INSERT INTO tb_links (code, original_url, user_id, expires_at, is_active)
SELECT
    'hot-' || LPAD(i::text, 3, '0'),
    'https://example.com/hot/' || i,
    'k6-load-test-user',
    NOW() + INTERVAL '1 year',
    true
FROM generate_series(1, 50) AS i;

INSERT INTO tb_links (code, original_url, user_id, expires_at, is_active)
SELECT
    'cold-' || LPAD(i::text, 3, '0'),
    'https://example.com/cold/' || i,
    'k6-load-test-user',
    NOW() + INTERVAL '1 year',
    true
FROM generate_series(1, 950) AS i;