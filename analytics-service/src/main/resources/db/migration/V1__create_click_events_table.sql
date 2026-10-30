CREATE TABLE tb_click_events (
                                 id BIGSERIAL PRIMARY KEY,
                                 event_id VARCHAR(36) NOT NULL,
                                 code VARCHAR(50) NOT NULL,
                                 status VARCHAR(20) NOT NULL,
                                 target_url TEXT,
                                 failure_reason VARCHAR(50),
                                 client_ip VARCHAR(45),
                                 user_agent TEXT,
                                 referer TEXT,
                                 occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT uk_click_events_event_id UNIQUE (event_id)
);

CREATE INDEX idx_click_events_code ON tb_click_events (code);
CREATE INDEX idx_click_events_occurred_at ON tb_click_events (occurred_at);