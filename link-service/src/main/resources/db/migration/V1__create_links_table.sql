CREATE TABLE tb_links (
                          id BIGSERIAL PRIMARY KEY,
                          code VARCHAR(50) NOT NULL,
                          original_url TEXT NOT NULL,
                          user_id VARCHAR(100) NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          expires_at TIMESTAMP WITH TIME ZONE,
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,

                          CONSTRAINT uk_links_code UNIQUE (code)
);

CREATE INDEX idx_links_user_id ON tb_links (user_id);