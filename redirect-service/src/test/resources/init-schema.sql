-- NOTE: manually mirrors link-service's V1__create_links_table.sql.
-- Keep in sync manually; no automated check exists yet.
CREATE TABLE tb_links (
                          id BIGSERIAL PRIMARY KEY,
                          code VARCHAR(50) NOT NULL UNIQUE,
                          original_url TEXT NOT NULL,
                          user_id VARCHAR(100) NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                          expires_at TIMESTAMP WITH TIME ZONE,
                          is_active BOOLEAN DEFAULT TRUE NOT NULL
);