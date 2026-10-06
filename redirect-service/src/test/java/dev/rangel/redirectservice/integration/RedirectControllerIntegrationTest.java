package dev.rangel.redirectservice.integration;

import dev.rangel.redirectservice.config.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class RedirectControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("Should redirect with 302, Location header, and Cache-Control no-store for valid active link")
    void redirect_whenLinkIsValid_shouldReturn302WithLocationAndNoStore() throws Exception {
        String code = "valid123";
        String targetUrl = "https://example.com/success";

        insertLink(1L, code, targetUrl, "user-123", Instant.now(clock).plusSeconds(3600), true);

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, targetUrl))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
    }

    @Test
    @DisplayName("Should return opaque 404 Not Found for inactive link")
    void redirect_whenLinkIsInactive_shouldReturn404() throws Exception {
        String code = "inactive123";

        insertLink(2L, code, "https://example.com", "user-123", null, false);

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return opaque 404 Not Found for expired link")
    void redirect_whenLinkIsExpired_shouldReturn404() throws Exception {
        String code = "expired123";

        insertLink(3L, code, "https://example.com", "user-123", Instant.now(clock).minusSeconds(60), true);

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return opaque 404 Not Found for non-existent code")
    void redirect_whenCodeDoesNotExist_shouldReturn404() throws Exception {
        mockMvc.perform(get("/non-existent-code"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return opaque 404 Not Found for malformed URL in database")
    void redirect_whenLinkHasInvalidUrl_shouldReturn404() throws Exception {
        String code = "invalidurl123";
        String malformedUrl = "http://example.com/invalid space";

        insertLink(4L, code, malformedUrl, "user-123", Instant.now(clock).plusSeconds(3600), true);

        mockMvc.perform(get("/{code}", code))
                .andExpect(status().isNotFound());
    }

    private void insertLink(Long id, String code, String originalUrl, String userId, Instant expiresAt, boolean isActive) {
        Object dbExpiresAt = expiresAt != null
                ? Timestamp.from(expiresAt)
                : new SqlParameterValue(Types.TIMESTAMP_WITH_TIMEZONE, null);

        jdbcTemplate.update(
                "INSERT INTO tb_links (id, code, original_url, user_id, expires_at, is_active) VALUES (?, ?, ?, ?, ?, ?)",
                id, code, originalUrl, userId, dbExpiresAt, isActive
        );
    }
}