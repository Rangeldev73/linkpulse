package dev.rangel.linkservice.application.service;

import dev.rangel.linkservice.domain.model.Link;
import dev.rangel.linkservice.infrastructure.persistence.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LinkPersistenceService {

    private final LinkRepository linkRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Link saveIsolated(Link link) {
        return linkRepository.saveAndFlush(link);
    }
}