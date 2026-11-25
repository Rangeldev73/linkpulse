package dev.rangel.analyticsservice;

import dev.rangel.analyticsservice.domain.model.ClickEventEntity;
import dev.rangel.analyticsservice.infrastructure.persistence.repository.ClickEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ClickStatsSecurityTest extends BaseIntegrationTest {

    private static final String URL = "/api/v1/analytics/sec-001/clicks"
            + "?granularity=hour&from=2026-11-10T10:00:00Z&to=2026-11-10T12:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClickEventRepository eventRepository;

    @Value("${app.security.jwt.secret}")
    private String secret;

    private JwtTestUtils jwt;

    @BeforeEach
    void setUp() throws Exception {
        eventRepository.deleteAll();
        jwt = new JwtTestUtils(secret);
    }

    @Test
    @DisplayName("Returns 401 when no token is provided")
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("Returns 401 when the token is expired")
    void shouldReturn401WithExpiredToken() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt.generateExpiredToken("owner-1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Returns 401 when the token is signed with another key")
    void shouldReturn401WithInvalidSignature() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt.generateInvalidSignatureToken("owner-1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Each user sees only the clicks of their own links, even for the same code")
    void shouldScopeStatisticsToTheAuthenticatedOwner() throws Exception {
        save("owner-1", "2026-11-10T10:05:00Z");
        save("owner-1", "2026-11-10T10:35:00Z");
        save("owner-2", "2026-11-10T10:45:00Z");

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt.generateToken("owner-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buckets.length()").value(1))
                .andExpect(jsonPath("$.buckets[0].clicks").value(2));

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt.generateToken("owner-2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buckets[0].clicks").value(1));
    }

    @Test
    @DisplayName("A user with no clicks on that code gets an empty list, indistinguishable from a link nobody visited")
    void shouldReturnEmptyBucketsForStranger() throws Exception {
        save("owner-1", "2026-11-10T10:05:00Z");

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwt.generateToken("stranger")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buckets.length()").value(0));
    }

    private void save(String ownerId, String occurredAt) {
        eventRepository.save(ClickEventEntity.builder()
                .eventId(UUID.randomUUID().toString())
                .code("sec-001")
                .ownerId(ownerId)
                .status("SUCCESS")
                .occurredAt(Instant.parse(occurredAt))
                .build());
    }
}
