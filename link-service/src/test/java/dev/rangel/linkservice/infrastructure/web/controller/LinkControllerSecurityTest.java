package dev.rangel.linkservice.infrastructure.web.controller;

import dev.rangel.linkservice.config.AbstractIntegrationTest;
import dev.rangel.linkservice.domain.model.Link;
import dev.rangel.linkservice.infrastructure.persistence.repository.LinkRepository;
import dev.rangel.linkservice.utils.JwtTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import java.time.Instant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LinkControllerSecurityTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTestUtils jwtTestUtils;

    @Autowired
    private LinkRepository linkRepository;

    @AfterEach
    void tearDown() {
        linkRepository.deleteAll();
    }

    @Test
    void shouldReturn401WhenTokenIsExpired() throws Exception {
        String token = jwtTestUtils.generateExpiredToken("user-123");

        mockMvc.perform(get("/api/v1/links/any-code")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void shouldReturn401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/links/any-code"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenTokenHasInvalidSignature() throws Exception {
        String token = jwtTestUtils.generateInvalidSignatureToken("user-123");

        mockMvc.perform(get("/api/v1/links/any-code")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn404WhenValidTokenBelongsToAnotherUser() throws Exception {
        Link link = Link.builder()
                .code("code123")
                .originalUrl("https://spring.io")
                .userId("owner-user")
                .createdAt(Instant.now())
                .build();
        linkRepository.save(link);

        String token = jwtTestUtils.generateToken("different-user");

        mockMvc.perform(get("/api/v1/links/code123")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()) // Testando a opacidade!
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void shouldReturn200WhenValidTokenBelongsToOwner() throws Exception {
        Link link = Link.builder()
                .code("mycode")
                .originalUrl("https://spring.io")
                .userId("owner-user")
                .createdAt(Instant.now())
                .build();
        linkRepository.save(link);

        String token = jwtTestUtils.generateToken("owner-user");

        mockMvc.perform(get("/api/v1/links/mycode")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://spring.io"));
    }

    @Test
    void shouldReturn201WhenCreatingLinkWithValidToken() throws Exception {
        String token = jwtTestUtils.generateToken("owner-user");
        String payload = """  
                {  
                    "originalUrl": "https://github.com"  
                }  
                """;

        mockMvc.perform(post("/api/v1/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").exists());
    }

    @Test
    void shouldReturn200ForActuatorHealthWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400WhenPayloadIsInvalidBehindSecurity() throws Exception {
        String token = jwtTestUtils.generateToken("owner-user");
        String invalidPayload = """  
                {  
                    "originalUrl": ""  
                }  
                """;

        mockMvc.perform(post("/api/v1/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request Payload"));
    }
}