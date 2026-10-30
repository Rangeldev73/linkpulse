package dev.rangel.analyticsservice.infrastructure.persistence.repository;

import dev.rangel.analyticsservice.domain.model.ClickEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEventEntity,Long> {}