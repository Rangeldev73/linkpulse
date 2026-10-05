package dev.rangel.linkservice.application.service;

import dev.rangel.linkservice.domain.exception.LinkNotFoundException;
import dev.rangel.linkservice.domain.model.Link;
import dev.rangel.linkservice.infrastructure.persistence.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LinkQueryService {
    private final LinkRepository linkRepository;

    @Transactional(readOnly = true)
    public Link getByCode(String code, String userId) {
        Link link = linkRepository.findByCode(code)
                .orElseThrow(() -> new LinkNotFoundException(code));

        if (!link.getUserId().equals(userId)) {
            throw new LinkNotFoundException(code);
        }

        return link;
    }
}