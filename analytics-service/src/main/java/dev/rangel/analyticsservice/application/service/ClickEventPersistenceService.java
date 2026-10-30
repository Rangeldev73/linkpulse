package dev.rangel.analyticsservice.application.service;

import dev.rangel.analyticsservice.domain.model.ClickEventEntity;
import dev.rangel.analyticsservice.infrastructure.persistence.repository.ClickEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ClickEventPersistenceService {

    private final ClickEventRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveEventIsolated(ClickEventEntity entity) {
        repository.saveAndFlush(entity);
    }
}