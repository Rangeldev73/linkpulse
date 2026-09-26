package dev.rangel.linkservice.application.service;

import dev.rangel.linkservice.domain.exception.AliasAlreadyExistsException;
import dev.rangel.linkservice.domain.exception.CodeGenerationException;
import dev.rangel.linkservice.domain.model.Link;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LinkCreationService {

    private final Base62CodeGenerator codeGenerator;
    private final LinkPersistenceService persistenceService;

    private static final int MAX_RETRIES = 3;
    private static final int INITIAL_CODE_LENGTH = 6;
    private static final int FALLBACK_CODE_LENGTH = 7;
    private static final String UK_CODE_CONSTRAINT = "uk_links_code";

    public Link createLink(String originalUrl, String userId, String customAlias, Instant expiresAt) {
        if (customAlias != null && !customAlias.isBlank()) {
            return createWithCustomAlias(originalUrl, userId, customAlias, expiresAt);
        }
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String generatedCode = codeGenerator.generate(INITIAL_CODE_LENGTH);
                return saveLink(generatedCode, originalUrl, userId, expiresAt);
            } catch (DataIntegrityViolationException e) {
                if (!isCodeConstraintViolation(e)) {
                    throw e;
                }
            }
        }
        try {
            String fallbackCode = codeGenerator.generate(FALLBACK_CODE_LENGTH);
            return saveLink(fallbackCode, originalUrl, userId, expiresAt);
        } catch (DataIntegrityViolationException e) {
            if (!isCodeConstraintViolation(e)) {
                throw e;
            }
            throw new CodeGenerationException("Could not generate a unique link code after multiple attempts.", e);
        }
    }

    private Link createWithCustomAlias(String originalUrl, String userId, String customAlias, Instant expiresAt) {
        try {
            return saveLink(customAlias, originalUrl, userId, expiresAt);
        } catch (DataIntegrityViolationException e) {
            if (isCodeConstraintViolation(e)) {
                throw new AliasAlreadyExistsException(customAlias);
            }
            throw e;
        }
    }

    private Link saveLink(String code, String originalUrl, String userId, Instant expiresAt) {
        Link link = Link.builder()
                .code(code)
                .originalUrl(originalUrl)
                .userId(userId)
                .expiresAt(expiresAt)
                .build();
        return persistenceService.saveIsolated(link);
    }

    private boolean isCodeConstraintViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException hibernateException) {
            String constraintName = hibernateException.getConstraintName();
            return constraintName != null && constraintName.toLowerCase().contains(UK_CODE_CONSTRAINT);
        }
        return false;
    }
}