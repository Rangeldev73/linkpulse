package dev.rangel.linkservice.integration;

import dev.rangel.linkservice.application.service.Base62CodeGenerator;
import dev.rangel.linkservice.application.service.LinkCreationService;
import dev.rangel.linkservice.config.AbstractIntegrationTest;
import dev.rangel.linkservice.domain.exception.AliasAlreadyExistsException;
import dev.rangel.linkservice.domain.exception.CodeGenerationException;
import dev.rangel.linkservice.domain.model.Link;
import dev.rangel.linkservice.infrastructure.persistence.repository.LinkRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CreateLinkIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LinkCreationService linkService;

    @Autowired
    private LinkRepository linkRepository;

    @MockitoBean
    private Base62CodeGenerator codeGenerator;

    @BeforeEach
    void setUp() {
        linkRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Successful creation without alias should generate code, persist in Postgres, and allow retrieval by findByCode")
    void createLink_withoutAlias_shouldGenerateCodeAndPersistSuccessfully() {
        String originalUrl = "https://example.com/pg-test";
        String expectedCode = "abc1234";
        String userId = UUID.randomUUID().toString();

        given(codeGenerator.generate(anyInt())).willReturn(expectedCode);

        Link createdLink = linkService.createLink(originalUrl, userId, null, null);

        assertThat(createdLink).isNotNull();
        assertThat(createdLink.getCode()).isEqualTo(expectedCode);

        var foundInDb = linkRepository.findByCode(expectedCode);
        assertThat(foundInDb).isPresent();
        assertThat(foundInDb.get().getOriginalUrl()).isEqualTo(originalUrl);
        assertThat(foundInDb.get().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("2. Collision with pre-existing alias should throw AliasAlreadyExistsException")
    void createLink_whenAliasCollidesWithExistingCode_shouldThrowAliasAlreadyExistsException() {
        String existingCode = "custom-alias";
        String userId = UUID.randomUUID().toString();

        Link preExistingLink = Link.builder()
                .originalUrl("https://already-exists.com")
                .code(existingCode)
                .userId(userId)
                .build();
        linkRepository.saveAndFlush(preExistingLink);

        assertThatThrownBy(() -> linkService.createLink("https://new-link.com", userId, existingCode, null))
                .isInstanceOf(AliasAlreadyExistsException.class)
                .hasMessageContaining(existingCode);
    }

    @Test
    @DisplayName("3. Retry on random code collision should re-attempt against real Postgres and succeed on 3rd attempt")
    void createLink_whenCodeCollidesTwice_shouldRetryAndSucceedOnThirdAttempt() {
        String collidingCode = "collision-code";
        String successfulCode = "unique-code-3";
        String userId = UUID.randomUUID().toString();

        Link initialLink = Link.builder()
                .originalUrl("https://initial.com")
                .code(collidingCode)
                .userId(userId)
                .build();
        linkRepository.saveAndFlush(initialLink);

        given(codeGenerator.generate(anyInt()))
                .willReturn(collidingCode)
                .willReturn(collidingCode)
                .willReturn(successfulCode);

        Link createdLink = linkService.createLink("https://retry-test.com", userId, null, null);

        assertThat(createdLink.getCode()).isEqualTo(successfulCode);
        assertThat(linkRepository.findByCode(successfulCode)).isPresent();
        verify(codeGenerator, times(3)).generate(anyInt());
    }

    @Test
    @DisplayName("4. Unrelated constraint violation (e.g., null userId) should not be masked as CodeGenerationException")
    void createLink_whenUnrelatedConstraintViolated_shouldNotBeMaskedAsCodeGenerationException() {
        assertThatThrownBy(() -> linkService.createLink("https://unrelated-error.com", null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(CodeGenerationException.class)
                .isNotInstanceOf(AliasAlreadyExistsException.class);
    }
}