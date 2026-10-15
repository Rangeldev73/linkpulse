package dev.rangel.redirectservice.integration;

import dev.rangel.redirectservice.config.AbstractIntegrationTest;
import dev.rangel.redirectservice.config.MutableClock;
import dev.rangel.redirectservice.domain.repository.RedirectableLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RedirectCacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private RedirectableLinkRepository repository;

    @Autowired
    private MutableClock mutableClock;

    @BeforeEach
    void resetClock() {
        mutableClock.setInstant(Instant.now());
    }

    @Test
    @DisplayName("Cache miss followed by hit: should query database only on first request")
    void redirect_whenCalledTwice_shouldHitCacheOnSecondRequest() throws Exception {
        String code = "cache001";
        String targetUrl = "https://example.com/cache-test";
        insertLink(10L, code, targetUrl, Instant.now().plus(1, ChronoUnit.HOURS), true);

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, targetUrl));

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, targetUrl));

        verify(repository, times(1)).findByCode(code);
    }

    @Test
    @DisplayName("Negative caching: non-existent code should cache sentinel and avoid repeated DB hits")
    void redirect_whenCodeDoesNotExistTwice_shouldUseNegativeCache() throws Exception {
        String missingCode = "ghost999";

        mockMvc.perform(get("/{code}", missingCode))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/{code}", missingCode))
                .andExpect(status().isNotFound());

        verify(repository, times(1)).findByCode(missingCode);
    }

    @Test
    @DisplayName("Real-time expiration: cached link should still expire if expiresAt passes")
    void redirect_whenCachedLinkExpires_shouldEvaluateInRealTime() throws Exception {
        String code = "expirehot";
        String targetUrl = "https://example.com/expired";

        Instant now = Instant.now(mutableClock);
        mutableClock.setInstant(now);

        Instant futureExpiration = now.plus(2, ChronoUnit.SECONDS);
        insertLink(11L, code, targetUrl, futureExpiration, true);

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isFound());

        mutableClock.advance(java.time.Duration.ofSeconds(3));

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isNotFound());
    }

    private void insertLink(Long id, String code, String originalUrl, Instant expiresAt, boolean isActive) {
        Object dbExpiresAt = expiresAt != null
                ? Timestamp.from(expiresAt)
                : new SqlParameterValue(Types.TIMESTAMP_WITH_TIMEZONE, null);

        jdbcTemplate.update(
                "INSERT INTO tb_links (id, code, original_url, user_id, expires_at, is_active) VALUES (?, ?, ?, ?, ?, ?)",
                id, code, originalUrl, "user-123", dbExpiresAt, isActive
        );
    }
}