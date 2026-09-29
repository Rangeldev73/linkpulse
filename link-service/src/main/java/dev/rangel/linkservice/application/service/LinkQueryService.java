package dev.rangel.linkservice.application.service;

import org.springframework.transaction.annotation.Transactional;
import dev.rangel.linkservice.domain.exception.LinkNotFoundException;
import dev.rangel.linkservice.domain.model.Link;
import dev.rangel.linkservice.infrastructure.persistence.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;;

@Service
@RequiredArgsConstructor
public class LinkQueryService {
    private final LinkRepository linkRepository;

    @Transactional(readOnly = true)
    public Link getByCode(String code) {
        return linkRepository.findByCode(code)
                .orElseThrow(() -> new LinkNotFoundException(code));
    }
}