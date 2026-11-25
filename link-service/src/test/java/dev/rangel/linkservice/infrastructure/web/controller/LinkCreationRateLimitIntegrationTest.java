package dev.rangel.linkservice.infrastructure.web.controller;

import dev.rangel.linkservice.config.AbstractIntegrationTest;
import dev.rangel.linkservice.infrastructure.config.LinkCreationRateLimitProperties;
import dev.rangel.linkservice.infrastructure.persistence.repository.LinkRepository;
import dev.rangel.linkservice.utils.JwtTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LinkCreationRateLimitIntegrationTest extends AbstractIntegrationTest {

    private static final String PAYLOAD = "{\"originalUrl\": \"https://github.com\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTestUtils jwtTestUtils;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private LinkCreationRateLimitProperties properties;

    @AfterEach
    void tearDown() {
        linkRepository.deleteAll();
    }

    @Test
    @DisplayName("Returns 429 with Retry-After once a user exceeds the creation limit")
    void shouldReturn429AfterTheLimit() throws Exception {
        String token = jwtTestUtils.generateToken("user-a");

        for (int i = 0; i < properties.limit(); i++) {
            createLink(token).andExpect(status().isCreated());
        }

        createLink(token)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Rate Limit Exceeded"));
    }

    @Test
    @DisplayName("The limit is per user: one user exhausting it does not affect another")
    void shouldNotAffectOtherUsers() throws Exception {
        String userA = jwtTestUtils.generateToken("user-a");
        String userB = jwtTestUtils.generateToken("user-b");

        for (int i = 0; i < properties.limit(); i++) {
            createLink(userA).andExpect(status().isCreated());
        }
        createLink(userA).andExpect(status().isTooManyRequests());

        createLink(userB).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Only creation is limited: reads are never throttled")
    void shouldNotLimitReads() throws Exception {
        String token = jwtTestUtils.generateToken("user-a");

        for (int i = 0; i < properties.limit() + 3; i++) {
            mockMvc.perform(get("/api/v1/links/unknown-code").header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    private ResultActions createLink(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(PAYLOAD));
    }
}
