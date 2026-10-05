package dev.rangel.linkservice.infrastructure.web.controller;

import dev.rangel.linkservice.application.dto.CreateLinkRequest;
import dev.rangel.linkservice.application.dto.LinkResponse;
import dev.rangel.linkservice.application.service.LinkCreationService;
import dev.rangel.linkservice.application.service.LinkQueryService;
import dev.rangel.linkservice.domain.model.Link;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/links")
public class LinkController {

    private final LinkCreationService linkCreationService;
    private final LinkQueryService linkQueryService;
    private final String baseUrl;

    public LinkController(
            LinkCreationService linkCreationService,
            LinkQueryService linkQueryService,
            @Value("${app.base-url}") String baseUrl
    ) {
        this.linkCreationService = linkCreationService;
        this.linkQueryService = linkQueryService;
        this.baseUrl = baseUrl;
    }

    @PostMapping
    public ResponseEntity<LinkResponse> createLink(
            @Valid @RequestBody CreateLinkRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();

        Link createdLink = linkCreationService.createLink(
                request.originalUrl(),
                userId,
                request.customAlias(),
                request.expiresAt()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{code}")
                .buildAndExpand(createdLink.getCode())
                .toUri();

        LinkResponse response = LinkResponse.from(createdLink, baseUrl);

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<LinkResponse> getLinkByCode(
            @PathVariable("code") String code,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        Link link = linkQueryService.getByCode(code, userId);
        return ResponseEntity.ok(LinkResponse.from(link, baseUrl));
    }
}